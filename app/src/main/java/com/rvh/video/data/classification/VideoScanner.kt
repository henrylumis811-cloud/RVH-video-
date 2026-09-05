package com.rvh.video.data.classification

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.rvh.video.data.local.LocalVideoDao
import com.rvh.video.data.model.LocalVideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scans MediaStore's video collection, classifies anything new or changed,
 * and upserts into Room. Designed to run on app start and on-demand
 * (pull-to-refresh), not continuously — combined with the Room cache this
 * keeps repeat scans fast (large libraries only pay the
 * MediaMetadataRetriever cost once per file, not once per scan).
 */
class VideoScanner(
    private val context: Context,
    private val dao: LocalVideoDao,
) {

    /**
     * @param forceReclassify Skips the fingerprint short-circuit for every
     * file, re-running the classifier on the whole library rather than
     * just new/changed files. Needed because classifier logic changes
     * (like the rotation-correction fix) don't change a file's own
     * dateModified — a normal scan has no way to know a previously-scanned
     * file might classify differently now. Exposed as "Force full rescan"
     * in Profile, separate from the regular "Scan for new videos".
     */
    suspend fun scan(forceReclassify: Boolean = false) = withContext(Dispatchers.IO) {
        val fingerprints = dao.getScanFingerprints().associateBy { it.uri }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATA, // used only to derive folder name
            MediaStore.Video.Media.DATE_MODIFIED,
        )

        val toUpsert = mutableListOf<LocalVideoEntity>()
        val seenUris = mutableListOf<String>()

        val scanSucceeded = try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = android.content.ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                ).toString()
                seenUris += uri

                val dateModified = cursor.getLong(modifiedCol)
                val fingerprint = fingerprints[uri]

                // Skip re-classification if we've already classified this
                // exact file version — this is what keeps repeat scans cheap
                // and avoids any visible hitch on screens that observe the DB.
                // forceReclassify bypasses this entirely.
                if (!forceReclassify && fingerprint != null && fingerprint.classifiedAtEpochSeconds >= dateModified) {
                    continue
                }

                // Carry forward anything the user set manually — a
                // reclassification (forced or fingerprint-triggered) must
                // never silently wipe a long-press override or a movie's
                // resume position. Without this, upsertAll's REPLACE
                // conflict strategy would blow both away on every
                // reclassify, since the entity below is built fresh.
                val existing = dao.getByUri(uri)

                var w = cursor.getInt(widthCol)
                var h = cursor.getInt(heightCol)
                var durationMs = cursor.getLong(durationCol)

                // One retriever pass supplies rotation and also repairs
                // MediaStore metadata when OEMs report zero dimensions or
                // duration. Keeping these reads together avoids opening the
                // same media source twice for a single file.
                val retrieved = retrieveVideoMetadata(uri)
                if (w <= 0) w = retrieved?.widthPx ?: 0
                if (h <= 0) h = retrieved?.heightPx ?: 0
                if (durationMs <= 0) durationMs = retrieved?.durationMs ?: 0L

                // Some downloader apps store a landscape encoded frame with
                // a 90/270 degree rotation flag. Correct the effective
                // dimensions before classification so vertical media lands
                // in Shorts instead of Movies.
                val rotation = retrieved?.rotationDegrees ?: 0
                if (rotation == 90 || rotation == 270) {
                    val swap = w; w = h; h = swap
                }

                val folderName = File(cursor.getString(dataCol) ?: "").parentFile?.name.orEmpty()

                val category = VideoClassifier.classify(
                    RawVideoMetrics(widthPx = w, heightPx = h, durationMs = durationMs, folderName = folderName)
                )

                toUpsert += LocalVideoEntity(
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: uri,
                    durationMs = durationMs,
                    widthPx = w,
                    heightPx = h,
                    folderName = folderName,
                    dateModifiedEpochSeconds = dateModified,
                    heuristicCategory = category,
                    userOverrideCategory = existing?.userOverrideCategory,
                    classifiedAtEpochSeconds = System.currentTimeMillis() / 1000,
                    resumePositionMs = existing?.resumePositionMs ?: 0L,
                    musicQueuePosition = existing?.musicQueuePosition,
                    isFavorite = existing?.isFavorite ?: false,
                    isWatchLater = existing?.isWatchLater ?: false,
                )
            }
            true
        } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }

        if (!scanSucceeded) return@withContext

        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
        // Only prune after a successful MediaStore query. A failed query must
        // never be interpreted as "all media was deleted".
        dao.pruneMissing(seenUris)
    }

    private data class RetrievedVideoMetadata(
        val widthPx: Int,
        val heightPx: Int,
        val durationMs: Long,
        val rotationDegrees: Int,
    )

    private fun retrieveVideoMetadata(uriString: String): RetrievedVideoMetadata? =
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, android.net.Uri.parse(uriString))
                RetrievedVideoMetadata(
                    widthPx = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull() ?: 0,
                    heightPx = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull() ?: 0,
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L,
                    rotationDegrees = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        ?.toIntOrNull() ?: 0,
                )
            }
        } catch (_: Exception) {
            null
        }
}
