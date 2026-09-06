package com.rvh.video.player

/** Small, serializable description of the last global playback session. */
data class PlaybackSnapshot(
    val uri: String? = null,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
)
