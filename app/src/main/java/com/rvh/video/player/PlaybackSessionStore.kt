package com.rvh.video.player

import android.content.Context

/**
 * Persists only the tiny amount of information needed to reconstruct the
 * user's last playback session. The actual Media3 player remains in memory;
 * this store is deliberately dumb and safe to use from any screen.
 */
class PlaybackSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): PlaybackSnapshot = PlaybackSnapshot(
        uri = prefs.getString(KEY_URI, null),
        positionMs = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
        playbackSpeed = prefs.getFloat(KEY_SPEED, 1.0f).coerceIn(0.25f, 4.0f),
    )

    fun write(snapshot: PlaybackSnapshot) {
        prefs.edit()
            .putString(KEY_URI, snapshot.uri)
            .putLong(KEY_POSITION, snapshot.positionMs.coerceAtLeast(0L))
            .putFloat(KEY_SPEED, snapshot.playbackSpeed.coerceIn(0.25f, 4.0f))
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_URI).remove(KEY_POSITION).apply()
    }

    companion object {
        private const val PREFS = "rvh_playback_session"
        private const val KEY_URI = "uri"
        private const val KEY_POSITION = "position_ms"
        private const val KEY_SPEED = "speed"
    }
}
