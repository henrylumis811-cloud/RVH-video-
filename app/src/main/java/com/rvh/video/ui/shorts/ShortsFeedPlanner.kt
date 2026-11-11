package com.rvh.video.ui.shorts

import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.PlaybackHistoryEntity
import kotlin.random.Random

/**
 * Builds a fresh Shorts session without changing the user's library order.
 *
 * This is deliberately a local/session concern: Movies, Music and the library
 * keep their existing ordering, while Shorts gets a feed that feels fresh.
 */
object ShortsFeedPlanner {

    fun build(
        shorts: List<LocalVideoEntity>,
        history: List<PlaybackHistoryEntity>,
        seed: Long,
    ): List<LocalVideoEntity> {
        if (shorts.size < 2) return shorts

        val historyByUri = history.associateBy { it.videoUri }
        val random = Random(seed)
        val newestModified = shorts.maxOf { it.dateModifiedEpochSeconds }
        val oldestModified = shorts.minOf { it.dateModifiedEpochSeconds }
        val modifiedRange = (newestModified - oldestModified).coerceAtLeast(1L)
        val remaining = shorts.toMutableList()
        val result = ArrayList<LocalVideoEntity>(shorts.size)
        var previousFolder: String? = null

        while (remaining.isNotEmpty()) {
            val candidates = remaining.map { video ->
                val entry = historyByUri[video.uri]
                var score = random.nextDouble(0.0, 10.0)

                // Never-watched content is strongly preferred. Recently watched
                // content receives a cooldown penalty that fades with time.
                if (entry == null) {
                    score += 34.0
                } else {
                    val ageHours = ((System.currentTimeMillis() - entry.lastPlayedEpochMs) / 3_600_000.0)
                        .coerceAtLeast(0.0)
                    val cooldownPenalty = (24.0 - ageHours).coerceAtLeast(0.0) * 1.35
                    score -= cooldownPenalty
                    score -= entry.playCount.coerceAtMost(6) * 2.5
                    if (entry.completed) score -= 2.0
                }

                // Keep consecutive items from the same folder/source apart when possible.
                if (previousFolder != null && video.folderName == previousFolder) {
                    score -= 20.0
                }

                // Slightly favor newer material without making Shorts chronological again.
                val recency = (video.dateModifiedEpochSeconds - oldestModified).toDouble() / modifiedRange
                score += recency * 8.0
                video to score
            }

            val selected = candidates.maxBy { it.second }.first
            result += selected
            remaining.remove(selected)
            previousFolder = selected.folderName
        }

        return result
    }
}
