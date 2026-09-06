package com.rvh.video.ui.components

import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Media3's PlayerView.resizeMode only supports FIT / FIXED_WIDTH /
 * FIXED_HEIGHT / FILL / ZOOM — there's no built-in "force a specific
 * target ratio regardless of the source" mode (that would need a custom
 * AspectRatioFrameLayout subclass overriding onMeasure, real custom View
 * work that isn't safely buildable without a device to actually test it
 * against). So "16:9" and "4:3" here are an honest best-fit mapping onto
 * what Media3 actually offers, not true artificial letterboxing to that
 * exact ratio — FIXED_WIDTH/FIXED_HEIGHT are the closest available
 * behaviors.
 */
enum class VideoScaleMode(val label: String, val resizeMode: Int) {
    ORIGINAL("Original", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FULL_SCREEN("Full Screen", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    RATIO_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    RATIO_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
}

/** Cycled by the speed button — tap advances to the next value, wrapping back to 1.0x after 2.0x. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

fun nextPlaybackSpeed(current: Float): Float {
    val index = PLAYBACK_SPEEDS.indexOf(current).let { if (it == -1) 2 else it } // defaults to the 1.0x slot if somehow off-list
    return PLAYBACK_SPEEDS[(index + 1) % PLAYBACK_SPEEDS.size]
}
