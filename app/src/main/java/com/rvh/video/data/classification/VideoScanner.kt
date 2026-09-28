package com.rvh.video.data.classification

import androidx.room.withTransaction
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Log
import com.rvh.video.data.local.LocalVideoDao
import com.rvh.video.data.local.RvhDatabase
import com.rvh.video.data.local.ScanFingerprint
import com.rvh.video.data.model.LocalVideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    companion object {
        private const val TAG = "VideoScanner"
        // MediaPermissionGate and Profile can legitimately trigger scans close together.
        // A single process-wide lock prevents concurrent scans from racing on the same
        // MediaStore snapshot and pruning rows discovered by the other scan.
        private val scanMutex = Mutex()
    }

    /**
     * @param forceReclassify Skips the fingerprint short-circuit for every
     * file, re-running the classifier on the whole library rather than
     * just new/changed files. Needed because classifier logic changes
     * (like the rotation-correction fix) don't change a file's own
     * dateModified — a normal scan has no way to know a previously-scanned
     * file might classify differently now. Exposed as "Force full rescan"
     * in Profile, separate from the regular "Scan for new videos".
     */
    suspend fun scan(forceReclassify: Boolean = false) = scanMutex.withLock {
        withContext(Dispatchers.IO) {
        // Load the existing rows once. The previous implementation performed a
        // Room query for every changed file, which becomes expensive on large
        // libraries. The scan already needs a full fingerprint pass, so keeping
        // the existing entity lookup in the same read avoids N+1 database work.
        val existingVideos = dao.observeAll().first().associateBy { it.uri }
        val fingerprints = existingVideos.values.associateBy { it.uri }.mapValues { (_, video) ->
            ScanFingerprint(video.uri, video.dateModifiedEpochSeconds, video.classifiedAtEpochSeconds)
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            "orientation", // MediaStore exposes this on supported Android versions; avoids opening a retriever just for rotation.
            MediaStore.Video.Media.RELATIVE_PATH, // scoped-storage-safe folder metadata
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
                val orientationCol = cursor.getColumnIndex("orientation")
                val relativePathCol = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
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
                // resume position and other user-owned fields are carried in
                // the entity for new-row insertion. Existing rows are updated
                // field-by-field after the scan so newer user changes cannot
                // be overwritten by this scan's stale snapshot.
                val existing = existingVideos[uri]

                var w = cursor.getInt(widthCol)
                var h = cursor.getInt(heightCol)
                var durationMs = cursor.getLong(durationCol)

                // Prefer MediaStore's already-indexed rotation metadata.
                // Opening MediaMetadataRetriever for every file is expensive
                // on large libraries, and rotation is the only value we need
                // from it when the normal MediaStore metadata is complete.
                var rotation = if (orientationCol >= 0 && !cursor.isNull(orientationCol)) {
                    cursor.getInt(orientationCol)
                } else {
                    0
                }

                // Only pay the retriever cost when MediaStore cannot provide
                // complete metadata or a usable rotation value. This keeps
                // the initial scan substantially lighter on modern devices
                // while retaining the OEM/downloader compatibility fallback.
                if (w <= 0 || h <= 0 || durationMs <= 0L ||
                    (orientationCol < 0 || cursor.isNull(orientationCol))) {
                    val retrieved = retrieveVideoMetadata(uri)
                    if (w <= 0) w = retrieved?.widthPx ?: 0
                    if (h <= 0) h = retrieved?.heightPx ?: 0
                    if (durationMs <= 0) durationMs = retrieved?.durationMs ?: 0L
                    if (orientationCol < 0 || cursor.isNull(orientationCol)) {
                        rotation = retrieved?.rotationDegrees ?: 0
                    }
                }

                // Some downloader apps store a landscape encoded frame with
                // a 90/270 degree rotation flag. Correct the effective
                // dimensions before classification so vertical media lands
                // in Shorts instead of Movies.
                if (rotation == 90 || rotation == 270) {
                    val swap = w; w = h; h = swap
                }

                val relativePath = if (relativePathCol >= 0 && !cursor.isNull(relativePathCol)) {
                    cursor.getString(relativePathCol).trimEnd('/')
                } else {
                    ""
                }
                val folderName = relativePath.substringAfterLast('/').ifBlank {
                    // RELATIVE_PATH is expected on API 30+, but retain a harmless
                    // fallback for unusual OEM MediaStore implementations.
                    ""
                }
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
        } catch (security: SecurityException) {
            Log.w(TAG, "MediaStore scan was denied", security)
            false
        } catch (error: Exception) {
            Log.w(TAG, "MediaStore scan failed", error)
            false
        }

        if (!scanSucceeded) return@withContext

        if (toUpsert.isNotEmpty()) {
            // Insert new rows without touching anything that may already exist.
            // Existing rows are updated field-by-field below so scanner work can
            // never overwrite user state changed while this scan was running.
            dao.insertNewVideos(toUpsert.filterNot { existingVideos.containsKey(it.uri) })
            toUpsert.filter { existingVideos.containsKey(it.uri) }.forEach { video ->
                dao.updateScanMetadata(
                    uri = video.uri,
                    displayName = video.displayName,
                    durationMs = video.durationMs,
                    widthPx = video.widthPx,
                    heightPx = video.heightPx,
                    folderName = video.folderName,
                    dateModifiedEpochSeconds = video.dateModifiedEpochSeconds,
                    heuristicCategory = video.heuristicCategory,
                    classifiedAtEpochSeconds = video.classifiedAtEpochSeconds,
                )
            }
        }

        // Only prune after a successful MediaStore query. A failed query must
        // never be interpreted as "all media was deleted". Compute the missing
        // rows from the snapshot already loaded at the start of this scan, then
        // delete them in bounded batches. This avoids a giant NOT IN (...) query
        // exceeding SQLite's bind-variable limit on large video libraries.
        val seenUriSet = seenUris.toHashSet()
        val missingUris = existingVideos.keys.filterNot(seenUriSet::contains)
        val database = RvhDatabase.get(context)
        missingUris.chunked(500).forEach { batch ->
            // Media rows are the source of truth for whether user state is still
            // meaningful. Remove dependent collection/history state in the same
            // SQLite transaction so a deleted/replaced MediaStore item cannot
            // leave behind dangling references or resurrect stale play counts.
            database.withTransaction {
                dao.deleteByUris(batch)
                batch.forEach { uri ->
                    database.collectionDao().removeVideoFromAll(uri)
                    database.playbackHistoryDao().remove(uri)
                }
            }
        }
        }
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
