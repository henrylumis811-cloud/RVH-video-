package com.rvh.video.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val generationMutex = Mutex()
    private val inFlight = mutableSetOf<String>()

    fun fileFor(context: Context, uri: String, dateModifiedEpochSeconds: Long): File =
        File(context.cacheDir.resolve(DIRECTORY), key(uri, dateModifiedEpochSeconds) + ".jpg")

    suspend fun getOrCreate(
        context: Context,
        uri: String,
        dateModifiedEpochSeconds: Long,
        widthPx: Int,
        heightPx: Int,
    ): File? {
        val output = fileFor(context, uri, dateModifiedEpochSeconds)
        if (output.exists() && output.length() > 0L) return output

        val identity = output.absolutePath
        var waited = 0
        while (true) {
            if (output.exists() && output.length() > 0L) return output
            val acquired = generationMutex.withLock {
                if (output.exists() && output.length() > 0L) return@withLock true
                inFlight.add(identity)
            }
            if (acquired) break
            if (waited++ >= 20) return null
            kotlinx.coroutines.delay(100)
        }

        return try {
            output.parentFile?.mkdirs()
            val bitmap = extractRepresentativeFrame(context, uri, widthPx, heightPx)
                ?: return null
            try {
                if (output.exists()) output.delete()
                output.outputStream().use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                        return null
                    }
                }
                output.takeIf { it.exists() && it.length() > 0L }
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            null
        } finally {
            generationMutex.withLock { inFlight.remove(identity) }
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
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        targetWidth(widthPx, heightPx),
                        targetHeight(widthPx, heightPx),
                    )
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
