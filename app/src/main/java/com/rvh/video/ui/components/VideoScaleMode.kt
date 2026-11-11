package com.rvh.video.ui.components

import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Controls both the player content fit and, where requested, the actual
 * viewport geometry. 16:9 and 4:3 are real target viewports; they are not
 * approximations of Media3's FIXED_WIDTH/FIXED_HEIGHT modes.
 */
enum class VideoScaleMode(
    val label: String,
    val resizeMode: Int,
    val targetAspectRatio: Float? = null,
) {
    ORIGINAL("Original", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FULL_SCREEN("Full Screen", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    RATIO_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIT, 16f / 9f),
    RATIO_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIT, 4f / 3f),
}

/** Cycled by the speed button — tap advances to the next value, wrapping back to 1.0x after 2.0x. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

fun nextPlaybackSpeed(current: Float): Float {
    val index = PLAYBACK_SPEEDS.indexOf(current).let { if (it == -1) 2 else it } // defaults to the 1.0x slot if somehow off-list
    return PLAYBACK_SPEEDS[(index + 1) % PLAYBACK_SPEEDS.size]
}
