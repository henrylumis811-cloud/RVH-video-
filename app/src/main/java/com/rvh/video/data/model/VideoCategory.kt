package com.rvh.video.data.model

/**
 * The three sections of the app. A video's category is either heuristically
 * assigned (classification engine) or user-overridden via long-press.
 */
enum class VideoCategory {
    SHORT,
    MOVIE,
    MUSIC_VIDEO
}
