package com.rvh.video.data.local

import android.content.Context
import android.graphics.Bitmap
import android.os.CancellationSignal
import android.util.Size
import android.media.MediaMetadataRetriever
import android.os.Build
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Deferred
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import java.security.MessageDigest
import kotlin.math.max

/**
 * Canonical on-device thumbnail store.
 *
 * A video gets one thumbnail keyed by URI + MediaStore DATE_MODIFIED. The UI
 * can therefore share the same generated image across Movies, Music, Search,
 * Home and Collections instead of asking Coil to decode the source video for
 * every surface.
 */
object VideoThumbnailStore {
    private const val DIRECTORY = "rvh_video_thumbnails"
    private const val MAX_EDGE = 640
    private const val JPEG_QUALITY = 86
    private const val BLACK_LUMA = 0.075
    private const val MAX_STORE_BYTES = 256L * 1024L * 1024L

    // Thumbnail extraction is shared across all UI surfaces and deliberately
    // bounded: opening a large library must not launch one decoder per card.
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generationSlots = Semaphore(2)
    private val inFlight = ConcurrentHashMap<String, Deferred<File?>>()

    fun fileFor(context: Context, uri: String, dateModifiedEpochSeconds: Long): File =
        File(context.filesDir.resolve(DIRECTORY), key(uri, dateModifiedEpochSeconds) + ".jpg")

    suspend fun getOrCreate(
        context: Context,
        uri: String,
        dateModifiedEpochSeconds: Long,
        widthPx: Int,
        heightPx: Int,
    ): File? {
        val appContext = context.applicationContext
        val output = fileFor(appContext, uri, dateModifiedEpochSeconds)
        if (output.isUsable()) return output

        // Preserve already-generated thumbnails from versions that stored them
        // in cacheDir, while moving future canonical files to durable app storage.
        val legacy = File(
            appContext.cacheDir.resolve(DIRECTORY),
            key(uri, dateModifiedEpochSeconds) + ".jpg",
        )
        if (legacy.isUsable()) {
            output.parentFile?.mkdirs()
            if (legacy.copyTo(output, overwrite = true).isUsable()) return output
        }

        val identity = output.absolutePath
        val task = inFlight.computeIfAbsent(identity) {
            workerScope.async(start = CoroutineStart.LAZY) {
                generationSlots.withPermit {
                    if (output.isUsable()) return@withPermit output
                    createThumbnail(appContext, uri, output, widthPx, heightPx)
                }
            }.also { deferred ->
                deferred.invokeOnCompletion { inFlight.remove(identity, deferred) }
            }
        }
        task.start()
        return task.await()
    }

    private suspend fun createThumbnail(
        context: Context,
        uriString: String,
        output: File,
        widthPx: Int,
        heightPx: Int,
    ): File? = withContext(Dispatchers.IO) {
        try {
            output.parentFile?.mkdirs()

            // MediaStore's system-generated thumbnail is substantially cheaper
            // than opening a retriever and decoding/scoring five separate frames.
            val bitmap = try {
                context.contentResolver.loadThumbnail(
                    Uri.parse(uriString),
                    Size(targetWidth(widthPx, heightPx), targetHeight(widthPx, heightPx)),
                    CancellationSignal(),
                )
            } catch (_: Exception) {
                null
            } ?: extractRepresentativeFrame(context, uriString, widthPx, heightPx)
                ?: return@withContext null

            val temporary = File(output.parentFile, output.nameWithoutExtension + ".tmp")
            try {
                temporary.outputStream().use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                        return@withContext null
                    }
                }
                if (output.exists()) output.delete()
                if (!temporary.renameTo(output)) {
                    temporary.copyTo(output, overwrite = true)
                    temporary.delete()
                }
                output.takeIf { it.isUsable() }?.also {
                    it.setLastModified(System.currentTimeMillis())
                    pruneIfNeeded(it.parentFile, it)
                }
            } finally {
                bitmap.recycle()
                if (temporary.exists()) temporary.delete()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun File.isUsable(): Boolean = isFile && length() > 0L

    // Keep the durable cache bounded without putting thumbnail generation back
    // on the critical path of every library-card request.
    private fun pruneIfNeeded(directory: File?, keep: File) {
        val files = directory?.listFiles { file -> file.isFile && file.extension == "jpg" }
            ?: return
        var totalBytes = files.sumOf { it.length() }
        if (totalBytes <= MAX_STORE_BYTES) return
        for (file in files.asSequence().filter { it != keep }.sortedBy { it.lastModified() }) {
            if (totalBytes <= MAX_STORE_BYTES) break
            val size = file.length()
            if (file.delete()) totalBytes -= size
        }
    }

    private fun extractRepresentativeFrame(
        context: Context,
        uriString: String,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap? {
        val durationUs = MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, Uri.parse(uriString))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.times(1000L) ?: 0L
        }
        if (durationUs <= 0L) return null

        // Do not assume that the opening seconds contain useful artwork.
        // Candidates are spread through the early/middle portion of the file;
        // the scoring pass prefers a non-black, non-flat frame.
        val candidates = buildList {
            add((durationUs * 0.08).toLong())
            add((durationUs * 0.16).toLong())
            add((durationUs * 0.30).toLong())
            add((durationUs * 0.50).toLong())
            add((durationUs * 0.70).toLong())
        }.distinct().map { it.coerceIn(0L, max(0L, durationUs - 1L)) }

        var best: Bitmap? = null
        var bestScore = Double.NEGATIVE_INFINITY

        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, Uri.parse(uriString))
            for (timeUs in candidates) {
                val bitmap = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        // getScaledFrameAtTime was added in API 27.
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetWidth(widthPx, heightPx),
                            targetHeight(widthPx, heightPx),
                        )
                    } else {
                        // Android 8.0 (API 26) only has getFrameAtTime.
                        // Scale its result immediately to keep cached thumbnails bounded.
                        retriever.getFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        )?.let { source ->
                            val scaled = Bitmap.createScaledBitmap(
                                source,
                                targetWidth(widthPx, heightPx),
                                targetHeight(widthPx, heightPx),
                                true,
                            )
                            if (scaled !== source) source.recycle()
                            scaled
                        }
                    }
                } catch (_: Exception) {
                    null
                } ?: continue

                val score = frameScore(bitmap)
                if (score > bestScore) {
                    best?.recycle()
                    best = bitmap
                    bestScore = score
                } else {
                    bitmap.recycle()
                }
            }
        }
        return best
    }

    private fun targetWidth(widthPx: Int, heightPx: Int): Int =
        if (widthPx >= heightPx) MAX_EDGE else max(1, (MAX_EDGE * widthPx.toFloat() / heightPx).toInt())

    private fun targetHeight(widthPx: Int, heightPx: Int): Int =
        if (heightPx > widthPx) MAX_EDGE else max(1, (MAX_EDGE * heightPx.toFloat() / widthPx.coerceAtLeast(1)).toInt())

    private fun frameScore(bitmap: Bitmap): Double {
        val sampleW = minOf(bitmap.width, 32)
        val sampleH = minOf(bitmap.height, 32)
        val pixels = IntArray(sampleW * sampleH)
        bitmap.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        var sum = 0.0
        var sumSq = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0
            val g = (pixel shr 8 and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0
            val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
            sum += luma
            sumSq += luma * luma
        }
        val mean = sum / pixels.size
        val variance = (sumSq / pixels.size - mean * mean).coerceAtLeast(0.0)
        // Brightness alone would reject legitimate dark scenes; variance rewards
        // frames that contain actual visual structure instead of a flat black card.
        val darknessPenalty = if (mean < BLACK_LUMA) 1.5 else 0.0
        return mean + variance * 3.0 - darknessPenalty
    }

    private fun key(uri: String, dateModifiedEpochSeconds: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$uri:$dateModifiedEpochSeconds".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
