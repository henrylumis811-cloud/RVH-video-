package com.rvh.video.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per local video file, indexed via MediaStore.
 *
 * Deliberately denormalized (classification + resume state + metadata
 * all on one row) rather than split into related tables: every screen
 * that reads this (Shorts pager, Movies grid, Music list) needs all of
 * it at once, and this is a read-heavy, write-light table — a handful
 * of fields updated occasionally (resume position, override) is cheaper
 * than joins on every scroll frame.
 */
@Entity(tableName = "local_videos")
data class LocalVideoEntity(
    /** MediaStore content URI string — stable identity for a file. */
    @PrimaryKey val uri: String,

    val displayName: String,
    val durationMs: Long,
    val widthPx: Int,
    val heightPx: Int,
    val folderName: String,
    val dateModifiedEpochSeconds: Long,

    /** Result of the heuristic classifier, cached so we don't re-run MediaMetadataRetriever every scan. */
    val heuristicCategory: VideoCategory,

    /**
     * Null unless the user long-pressed and manually recategorized this file.
     * Always takes precedence over heuristicCategory when non-null.
     */
    val userOverrideCategory: VideoCategory? = null,

    /** Last-modified timestamp this classification was computed against — if the file changes, we reclassify. */
    val classifiedAtEpochSeconds: Long,

    /** Resume-from-last-position, used by Movies (and optionally Music). Null/0 = start from beginning. */
    val resumePositionMs: Long = 0L,

    /** Manual ordering within the Music Videos queue, if the user reorders it. Null = natural/scan order. */
    val musicQueuePosition: Int? = null,

    /** User-curated favorite flag. */
    val isFavorite: Boolean = false,

    /** User-curated Watch Later flag. */
    val isWatchLater: Boolean = false,
) {
    /** The category actually used by the UI: override wins if present. */
    val effectiveCategory: VideoCategory
        get() = userOverrideCategory ?: heuristicCategory
}
