package com.rvh.video.player

import androidx.media3.common.PlaybackException

/**
 * Converts Media3's technical decoder/source errors into concise user-facing
 * messages. Raw exception messages are intentionally kept out of the UI because
 * they vary by device/codec and often expose implementation details.
 */
fun friendlyPlaybackError(error: PlaybackException?): String? {
    if (error == null) return null
    val code = error.errorCodeName.uppercase()
    return when {
        "FILE_NOT_FOUND" in code || "NO_PERMISSION" in code ->
            "This file is no longer available or cannot be accessed."
        "PARSING" in code || "MALFORMED" in code ->
            "This media file could not be read."
        "DECODER" in code || "CODEC" in code ->
            "This device cannot play this media format."
        "NETWORK" in code || "IO_" in code ->
            "The media could not be loaded. Please try again."
        else ->
            "Playback could not start. Please try again."
    }
}
