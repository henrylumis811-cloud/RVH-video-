package com.rvh.video.data.classification

import com.rvh.video.data.model.VideoCategory
import kotlin.math.abs

/** Raw signals pulled from MediaStore + MediaMetadataRetriever for one file, before classification. */
data class RawVideoMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val durationMs: Long,
    val folderName: String,
)

/**
 * Layered heuristic classifier. Pure and deterministic: same input always
 * gives the same category, which is what lets the Room cache
 * (classifiedAtEpochSeconds) safely skip re-running this until a file changes.
 *
 * IMPORTANT: widthPx/heightPx here are expected to already be
 * rotation-corrected (display orientation), not raw encoded dimensions —
 * that correction happens in VideoScanner before this is called. Some
 * third-party downloader apps (TikTok savers, etc.) store the encoded
 * frame as landscape with a separate rotation flag rather than
 * pre-rotating the video, so a naive raw-dimension check misses them.
 *
 * Order of signals:
 *   1. Aspect ratio (post-rotation-correction) -> catches Shorts outright
 *   2. Duration safety net -> catches vertical downloader content where
 *      the rotation flag itself is missing or wrong (not just unread) —
 *      no width/height signal can save those, so a short duration on an
 *      otherwise-ambiguous horizontal-reported video is treated as a
 *      likely mis-tagged Short rather than immediately falling to Movies.
 *   3. Duration range        -> splits the rest into Movie vs Music Video
 *   4. Folder name           -> tiebreaker only, nudges genuinely ambiguous durations
 *
 * Ambiguous horizontal videos default to MOVIE (a misplaced movie is less
 * disruptive than a movie interrupting a music-video autoplay queue).
 *
 * Known tradeoff: a genuinely short, genuinely horizontal home clip (say,
 * a 20-second landscape recording) will also fall into the duration
 * safety net and land in Shorts. That's an accepted cost of closing the
 * TikTok-bleed gap — long-press still overrides it either way.
 */
object VideoClassifier {

    private const val VERTICAL_ASPECT_THRESHOLD = 1.0 // height/width > this => vertical
    private val MUSIC_VIDEO_DURATION_RANGE = 60_000L..7 * 60_000L // 1–7 minutes
    private const val MOVIE_MIN_DURATION_MS = 40 * 60_000L // 40 minutes
    private const val SHORT_DURATION_SAFETY_NET_MS = 3 * 60_000L // under 3 min, on an otherwise-ambiguous file, reads as a likely Short

    private val musicFolderHints = listOf("music video", "music_video", "mv", "clips")
    private val movieFolderHints = listOf("movie", "movies", "films")

    fun classify(metrics: RawVideoMetrics): VideoCategory {
        if (metrics.widthPx <= 0 || metrics.heightPx <= 0) {
            // Corrupt/unreadable metadata — safest bucket is Movies, matches the
            // "ambiguous defaults to Movies" rule rather than inventing a 4th case.
            return VideoCategory.MOVIE
        }

        val aspect = metrics.heightPx.toDouble() / metrics.widthPx.toDouble()
        if (aspect > VERTICAL_ASPECT_THRESHOLD) {
            return VideoCategory.SHORT
        }

        // Horizontal (post-rotation-correction) from here — but a video
        // that's short AND has no folder signal pointing to Movies is
        // more likely a mis-tagged downloaded Short (missing/wrong
        // rotation flag) than a genuine short film — see class doc.
        val folder = metrics.folderName.lowercase()
        val folderSaysMusic = musicFolderHints.any { folder.contains(it) }
        val folderSaysMovie = movieFolderHints.any { folder.contains(it) }

        if (metrics.durationMs in 1 until SHORT_DURATION_SAFETY_NET_MS && !folderSaysMovie && !folderSaysMusic) {
            return VideoCategory.SHORT
        }

        val durationSaysMusic = metrics.durationMs in MUSIC_VIDEO_DURATION_RANGE
        val durationSaysMovie = metrics.durationMs >= MOVIE_MIN_DURATION_MS

        return when {
            // Clear signals agree — easy cases.
            durationSaysMovie -> VideoCategory.MOVIE
            durationSaysMusic && !folderSaysMovie -> VideoCategory.MUSIC_VIDEO

            // Folder breaks the tie for anything in the awkward middle
            // (e.g. 8-20 minute videos that are too long for a typical music
            // video and too short to confidently call a movie).
            folderSaysMusic -> VideoCategory.MUSIC_VIDEO
            folderSaysMovie -> VideoCategory.MOVIE

            // No signal agrees on anything — default per the design decision.
            else -> VideoCategory.MOVIE
        }
    }

    /** Exposed for potential future use (e.g. showing "why" in a debug view) — not required by the UI. */
    fun aspectRatioOf(metrics: RawVideoMetrics): Double =
        if (metrics.widthPx == 0) 0.0 else abs(metrics.heightPx.toDouble() / metrics.widthPx.toDouble())
}
