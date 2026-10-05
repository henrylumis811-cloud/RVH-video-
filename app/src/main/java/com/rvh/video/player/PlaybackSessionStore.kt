package com.rvh.video.player

import android.content.Context
import org.json.JSONArray

/**
 * Persists only the tiny amount of information needed to reconstruct the
 * user's last playback session. The actual Media3 player remains in memory;
 * this store is deliberately dumb and safe to use from any screen.
 */
class PlaybackSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): PlaybackSnapshot = PlaybackSnapshot(
        uri = prefs.getString(KEY_URI, null),
        title = prefs.getString(KEY_TITLE, null),
        positionMs = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
        playbackSpeed = prefs.getFloat(KEY_SPEED, 1.0f).coerceIn(0.25f, 4.0f),
    )

    fun write(snapshot: PlaybackSnapshot) {
        prefs.edit()
            .putString(KEY_URI, snapshot.uri)
            .putString(KEY_TITLE, snapshot.title)
            .putLong(KEY_POSITION, snapshot.positionMs.coerceAtLeast(0L))
            .putFloat(KEY_SPEED, snapshot.playbackSpeed.coerceIn(0.25f, 4.0f))
            .apply()
    }

    fun writeQueue(
        uris: List<String>,
        index: Int,
        shuffle: Boolean,
        repeatOne: Boolean,
        autoAdvance: Boolean,
    ) {
        val array = JSONArray()
        uris.distinct().take(MAX_QUEUE_ITEMS).forEach(array::put)
        prefs.edit()
            .putString(KEY_QUEUE_URIS, array.toString())
            .putInt(KEY_QUEUE_INDEX, index.coerceIn(0, (array.length() - 1).coerceAtLeast(0)))
            .putBoolean(KEY_QUEUE_SHUFFLE, shuffle)
            .putBoolean(KEY_QUEUE_REPEAT_ONE, repeatOne)
            .putBoolean(KEY_QUEUE_AUTO_ADVANCE, autoAdvance)
            .apply()
    }

    fun readQueue(): PlaybackQueueSnapshot? = runCatching {
        val raw = prefs.getString(KEY_QUEUE_URIS, null) ?: return@runCatching null
        val array = JSONArray(raw)
        val uris = buildList {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }.distinct()
        if (uris.isEmpty()) return@runCatching null
        PlaybackQueueSnapshot(
            uris = uris,
            index = prefs.getInt(KEY_QUEUE_INDEX, 0).coerceIn(0, uris.lastIndex),
            shuffle = prefs.getBoolean(KEY_QUEUE_SHUFFLE, false),
            repeatOne = prefs.getBoolean(KEY_QUEUE_REPEAT_ONE, false),
            autoAdvance = prefs.getBoolean(KEY_QUEUE_AUTO_ADVANCE, true),
        )
    }.getOrNull()

    fun writeTrackPreferences(
        uri: String,
        audioLanguage: String?,
        audioLabel: String?,
        subtitleLanguage: String?,
        subtitleLabel: String?,
        subtitlesDisabled: Boolean,
    ) {
        prefs.edit()
            .putString(KEY_TRACK_URI, uri)
            .putString(KEY_AUDIO_LANGUAGE, audioLanguage)
            .putString(KEY_AUDIO_LABEL, audioLabel)
            .putString(KEY_SUBTITLE_LANGUAGE, subtitleLanguage)
            .putString(KEY_SUBTITLE_LABEL, subtitleLabel)
            .putBoolean(KEY_SUBTITLES_DISABLED, subtitlesDisabled)
            .apply()
    }

    fun readTrackPreferences(uri: String): TrackPreferenceSnapshot? {
        if (prefs.getString(KEY_TRACK_URI, null) != uri) return null
        return TrackPreferenceSnapshot(
            audioLanguage = prefs.getString(KEY_AUDIO_LANGUAGE, null),
            audioLabel = prefs.getString(KEY_AUDIO_LABEL, null),
            subtitleLanguage = prefs.getString(KEY_SUBTITLE_LANGUAGE, null),
            subtitleLabel = prefs.getString(KEY_SUBTITLE_LABEL, null),
            subtitlesDisabled = prefs.getBoolean(KEY_SUBTITLES_DISABLED, false),
        )
    }

    private fun clearTrackPreferences() {
        prefs.edit()
            .remove(KEY_TRACK_URI)
            .remove(KEY_AUDIO_LANGUAGE)
            .remove(KEY_AUDIO_LABEL)
            .remove(KEY_SUBTITLE_LANGUAGE)
            .remove(KEY_SUBTITLE_LABEL)
            .remove(KEY_SUBTITLES_DISABLED)
            .apply()
    }

    fun clearQueue() {
        prefs.edit()
            .remove(KEY_QUEUE_URIS)
            .remove(KEY_QUEUE_INDEX)
            .remove(KEY_QUEUE_SHUFFLE)
            .remove(KEY_QUEUE_REPEAT_ONE)
            .remove(KEY_QUEUE_AUTO_ADVANCE)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_URI)
            .remove(KEY_TITLE)
            .remove(KEY_POSITION)
            .remove(KEY_SPEED)
            .remove(KEY_QUEUE_URIS)
            .remove(KEY_QUEUE_INDEX)
            .remove(KEY_QUEUE_SHUFFLE)
            .remove(KEY_QUEUE_REPEAT_ONE)
            .remove(KEY_QUEUE_AUTO_ADVANCE)
            .apply()
        clearTrackPreferences()
    }

    companion object {
        private const val PREFS = "rvh_playback_session"
        private const val KEY_URI = "uri"
        private const val KEY_TITLE = "title"
        private const val KEY_POSITION = "position_ms"
        private const val KEY_SPEED = "speed"
        private const val KEY_QUEUE_URIS = "queue_uris"
        private const val KEY_QUEUE_INDEX = "queue_index"
        private const val KEY_QUEUE_SHUFFLE = "queue_shuffle"
        private const val KEY_QUEUE_REPEAT_ONE = "queue_repeat_one"
        private const val KEY_QUEUE_AUTO_ADVANCE = "queue_auto_advance"
        private const val KEY_TRACK_URI = "track_uri"
        private const val KEY_AUDIO_LANGUAGE = "track_audio_language"
        private const val KEY_AUDIO_LABEL = "track_audio_label"
        private const val KEY_SUBTITLE_LANGUAGE = "track_subtitle_language"
        private const val KEY_SUBTITLE_LABEL = "track_subtitle_label"
        private const val KEY_SUBTITLES_DISABLED = "track_subtitles_disabled"
        private const val MAX_QUEUE_ITEMS = 1000
    }
}

data class TrackPreferenceSnapshot(
    val audioLanguage: String?,
    val audioLabel: String?,
    val subtitleLanguage: String?,
    val subtitleLabel: String?,
    val subtitlesDisabled: Boolean,
)

data class PlaybackQueueSnapshot(
    val uris: List<String>,
    val index: Int,
    val shuffle: Boolean,
    val repeatOne: Boolean,
    val autoAdvance: Boolean,
)
