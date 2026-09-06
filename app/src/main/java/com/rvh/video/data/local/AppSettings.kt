package com.rvh.video.data.local

import android.content.Context

/**
 * Deliberately plain SharedPreferences rather than DataStore — with a
 * single boolean setting, DataStore's extra dependency and async-Flow
 * ceremony isn't worth it yet. Worth migrating if the settings list grows.
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Appearance mode: system, light, or dark. */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    /** Whether the music queue should automatically advance when a track ends. */
    var autoAdvanceEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ADVANCE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ADVANCE, value).apply()

    /** Default speed used when opening a player for the first time. */
    var defaultPlaybackSpeed: Float
        get() = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f).coerceIn(0.5f, 2.0f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_SPEED, value.coerceIn(0.5f, 2.0f)).apply()

    /** Preferred library ordering used by Movies and Music. */
    var librarySortMode: String
        get() = prefs.getString(KEY_LIBRARY_SORT, SORT_NEWEST) ?: SORT_NEWEST
        set(value) = prefs.edit().putString(KEY_LIBRARY_SORT, value).apply()

    /** Preferred movie grid density. */
    var movieGridColumns: Int
        get() = prefs.getInt(KEY_GRID_COLUMNS, 3).coerceIn(2, 4)
        set(value) = prefs.edit().putInt(KEY_GRID_COLUMNS, value.coerceIn(2, 4)).apply()

    /** Mirrors the "Auto floating play" toggle — gates whether Music Videos arms auto-enter PiP on playback. */
    var autoFloatingPlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FLOATING_PLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FLOATING_PLAY, value).apply()

    companion object {
        private const val PREFS_NAME = "rvh_settings"
        private const val KEY_AUTO_FLOATING_PLAY = "auto_floating_play"
        private const val KEY_AUTO_ADVANCE = "auto_advance"
        private const val KEY_DEFAULT_SPEED = "default_playback_speed"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LIBRARY_SORT = "library_sort"
        private const val KEY_GRID_COLUMNS = "movie_grid_columns"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val SORT_NEWEST = "newest"
        const val SORT_OLDEST = "oldest"
        const val SORT_NAME = "name"
        const val SORT_LONGEST = "longest"
    }
}
