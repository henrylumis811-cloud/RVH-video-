package com.rvh.video.player

import android.content.Context
import com.rvh.video.data.local.AppSettings
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single point of ExoPlayer lifecycle management for app-level playback.
 *
 * The manager deliberately exposes a small immutable state surface so UI can
 * observe the player without maintaining a second, easily-diverging state
 * machine. It also remembers the last media URI/position across process death.
 */
class PlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val sessionStore = PlaybackSessionStore(appContext)
    // The manager is application-scoped. Keep its coroutine scope alive across
    // decoder release/recreation; release() only tears down the player resources.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var lastPersistedPositionMs = -1L
    private var lastPersistedUri: String? = null
    private var player: ExoPlayer? = null
    private var clearingPlayback = false
    private var queueUris: List<String> = emptyList()
    private var queueIndex: Int = -1
    private var queueShuffle = false
    private var queueRepeatOne = false
    private var queueAutoAdvance = true
    private var queueRestored = false
    private var trackPreferencesAppliedUri: String? = null
    private val listeners = LinkedHashSet<Player.Listener>()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val internalListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publish()
            if (!isPlaying && !clearingPlayback) persist(force = true)
        }
        override fun onTracksChanged(tracks: Tracks) {
            val uri = currentUri() ?: return
            if (trackPreferencesAppliedUri != uri) {
                trackPreferencesAppliedUri = uri
                applySavedTrackPreferences(uri, tracks)
            } else {
                persistSelectedTrackPreferences(uri, tracks)
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            publish()
            if (playbackState == Player.STATE_ENDED && !clearingPlayback && queueAutoAdvance && AppSettings(appContext).autoAdvanceEnabled) {
                advanceQueue()
            }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            publish()
            if (!clearingPlayback) persist(force = true)
        }
        override fun onPositionDiscontinuity(reason: Int) {
            publish()
            if (!clearingPlayback && reason == Player.DISCONTINUITY_REASON_SEEK) persist(force = true)
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = publish()
        override fun onPlayerError(error: PlaybackException) {
            // Keep the error actionable without exposing a potentially noisy
            // decoder/source exception string directly to the player UI.
            _state.value = _state.value.copy(
                errorMessage = friendlyPlaybackError(error)
            )
            publish()
            if (!clearingPlayback) persist(force = true)
        }
    }

    fun getOrCreate(): ExoPlayer {
        restoreQueueIfNeeded()
        return player ?: ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(
            DefaultLoadControl.Builder()
                // Keep enough data buffered for smooth playback without retaining
                // a large decoded/buffered window on memory-constrained phones.
                .setBufferDurationsMs(
                    8_000,
                    45_000,
                    1_500,
                    3_000,
                )
                .setBackBuffer(5_000, false)
                .build()
        )
        .build()
        .also { created ->
            created.addListener(internalListener)
            listeners.forEach(created::addListener)
            player = created
            startTicker()
            publish()
        }
    }

    fun play(uri: String, startPositionMs: Long = 0L) {
        clearQueueState()
        trackPreferencesAppliedUri = null
        val exoPlayer = getOrCreate()
        persist(force = true)
        val positionMs = startPositionMs.coerceAtLeast(0L)
        _state.value = _state.value.copy(uri = uri, errorMessage = null)
        exoPlayer.setMediaItem(MediaItem.fromUri(uri), positionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        publish()
        persist(force = true)
    }


    fun setQueue(
        uris: List<String>,
        startIndex: Int,
        startPositionMs: Long,
        shuffle: Boolean,
        repeatOne: Boolean,
        autoAdvance: Boolean,
    ) {
        val normalized = uris.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return
        queueUris = normalized
        queueIndex = startIndex.coerceIn(0, normalized.lastIndex)
        queueShuffle = shuffle
        queueRepeatOne = repeatOne
        queueAutoAdvance = autoAdvance
        queueRestored = true
        persistQueue()
        playQueueIndex(startPositionMs)
    }

    fun setQueueMode(shuffle: Boolean, repeatOne: Boolean, autoAdvance: Boolean) {
        queueShuffle = shuffle
        queueRepeatOne = repeatOne
        queueAutoAdvance = autoAdvance
        queueRestored = true
        persistQueue()
    }

    fun nextQueueItem(): Boolean {
        if (queueUris.isEmpty()) return false
        queueIndex = nextIndex() ?: return false
        persistQueue()
        playQueueIndex(0L)
        return true
    }

    fun previousQueueItem(): Boolean {
        if (queueUris.isEmpty()) return false
        val previous = if (queueIndex > 0) queueIndex - 1 else return false
        queueIndex = previous
        persistQueue()
        playQueueIndex(0L)
        return true
    }

    private fun advanceQueue() {
        if (queueUris.isEmpty()) return
        if (queueRepeatOne) {
            persistQueue()
            playQueueIndex(0L)
            return
        }
        val next = nextIndex() ?: run {
            queueAutoAdvance = false
            persistQueue()
            return
        }
        queueIndex = next
        persistQueue()
        playQueueIndex(0L)
    }

    private fun nextIndex(): Int? {
        if (queueUris.isEmpty()) return null
        if (queueShuffle) {
            val candidates = queueUris.indices.filter { it != queueIndex }
            return candidates.randomOrNull()
        }
        return (queueIndex + 1).takeIf { it < queueUris.size }
    }

    private fun playQueueIndex(startPositionMs: Long) {
        val uri = queueUris.getOrNull(queueIndex) ?: return
        trackPreferencesAppliedUri = null
        val exoPlayer = getOrCreate()
        persist(force = true)
        _state.value = _state.value.copy(uri = uri, errorMessage = null)
        exoPlayer.setMediaItem(MediaItem.fromUri(uri), startPositionMs.coerceAtLeast(0L))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        publish()
        persist(force = true)
    }

    private fun clearQueueState() {
        if (queueRestored || queueUris.isNotEmpty()) sessionStore.clearQueue()
        queueRestored = false
        queueUris = emptyList()
        queueIndex = -1
        queueShuffle = false
        queueRepeatOne = false
        queueAutoAdvance = true
        trackPreferencesAppliedUri = null
    }

    fun currentPositionMs(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs

    private fun applySavedTrackPreferences(uri: String, tracks: Tracks) {
        val preference = sessionStore.readTrackPreferences(uri) ?: return
        val exoPlayer = player ?: return
        if (!exoPlayer.availableCommands.contains(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return

        val builder = exoPlayer.trackSelectionParameters.buildUpon()
        var changed = false

        val audio = findTrack(tracks, C.TRACK_TYPE_AUDIO, preference.audioLanguage, preference.audioLabel)
        if (audio != null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .addOverride(TrackSelectionOverride(audio.group.mediaTrackGroup, audio.index))
            changed = true
        }

        if (preference.subtitlesDisabled) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            changed = true
        } else {
            val subtitle = findTrack(tracks, C.TRACK_TYPE_TEXT, preference.subtitleLanguage, preference.subtitleLabel)
            if (subtitle != null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .addOverride(TrackSelectionOverride(subtitle.group.mediaTrackGroup, subtitle.index))
                changed = true
            }
        }

        if (changed) exoPlayer.setTrackSelectionParameters(builder.build())
    }

    private fun persistSelectedTrackPreferences(uri: String, tracks: Tracks) {
        val audio = selectedTrack(tracks, C.TRACK_TYPE_AUDIO)
        val subtitle = selectedTrack(tracks, C.TRACK_TYPE_TEXT)
        val textDisabled = player?.trackSelectionParameters?.disabledTrackTypes?.contains(C.TRACK_TYPE_TEXT) == true
        if (audio == null && subtitle == null && !textDisabled) return
        sessionStore.writeTrackPreferences(
            uri = uri,
            audioLanguage = audio?.format?.language,
            audioLabel = audio?.format?.label,
            subtitleLanguage = subtitle?.format?.language,
            subtitleLabel = subtitle?.format?.label,
            subtitlesDisabled = textDisabled,
        )
    }

    private data class SelectedTrack(val group: Tracks.Group, val index: Int, val format: androidx.media3.common.Format)

    private fun selectedTrack(tracks: Tracks, type: Int): SelectedTrack? {
        for (group in tracks.groups) {
            if (group.type != type) continue
            for (index in 0 until group.length) {
                if (group.isTrackSelected(index)) return SelectedTrack(group, index, group.getTrackFormat(index))
            }
        }
        return null
    }

    private fun findTrack(tracks: Tracks, type: Int, language: String?, label: String?): SelectedTrack? {
        val normalizedLanguage = language?.trim()?.lowercase()
        val normalizedLabel = label?.trim()?.lowercase()
        var fallback: SelectedTrack? = null
        for (group in tracks.groups) {
            if (group.type != type) continue
            for (index in 0 until group.length) {
                if (!group.isTrackSupported(index)) continue
                val format = group.getTrackFormat(index)
                val candidate = SelectedTrack(group, index, format)
                if (normalizedLanguage != null && format.language?.lowercase() == normalizedLanguage &&
                    (normalizedLabel == null || format.label?.lowercase() == normalizedLabel)) return candidate
                if (fallback == null && normalizedLanguage != null && format.language?.lowercase() == normalizedLanguage) fallback = candidate
                if (fallback == null && normalizedLabel != null && format.label?.lowercase() == normalizedLabel) fallback = candidate
            }
        }
        return fallback
    }

    /** Position saved from the previous app process, but only for this exact media URI. */

    private fun restoreQueueIfNeeded() {
        if (queueRestored) return
        val snapshot = sessionStore.readQueue() ?: run {
            queueRestored = true
            return
        }
        queueUris = snapshot.uris
        queueIndex = snapshot.index.coerceIn(0, queueUris.lastIndex)
        queueShuffle = snapshot.shuffle
        queueRepeatOne = snapshot.repeatOne
        queueAutoAdvance = snapshot.autoAdvance
        queueRestored = true
    }

    private fun persistQueue() {
        if (queueUris.isEmpty()) {
            sessionStore.clearQueue()
            return
        }
        sessionStore.writeQueue(
            uris = queueUris,
            index = queueIndex,
            shuffle = queueShuffle,
            repeatOne = queueRepeatOne,
            autoAdvance = queueAutoAdvance,
        )
    }

    fun savedPositionFor(uri: String): Long {
        val saved = sessionStore.read()
        return if (saved.uri == uri) saved.positionMs else 0L
    }

    /** Playback speed saved from the previous app process. */
    fun savedPlaybackSpeed(): Float = sessionStore.read().playbackSpeed

    /** Snapshot used by the MediaSession resumption callback. */
    fun savedPlaybackSnapshot(): PlaybackSnapshot = sessionStore.read()

    fun currentUri(): String? = player?.currentMediaItem?.localConfiguration?.uri?.toString() ?: _state.value.uri

    fun pause() {
        player?.pause()
        publish()
        persist(force = true)
    }

    fun resume() {
        getOrCreate().play()
        publish()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
        publish()
        persist(force = true)
    }

    fun setPlaybackSpeed(speed: Float) {
        getOrCreate().setPlaybackSpeed(speed.coerceIn(0.25f, 4.0f))
        publish()
        persist(force = true)
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun stopAndClear() {
        val exoPlayer = player
        clearingPlayback = true
        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _state.value = PlaybackState()
            sessionStore.clear()
            clearQueueState()
            lastPersistedUri = null
            lastPersistedPositionMs = -1L
        } finally {
            clearingPlayback = false
        }
    }

    /**
     * Retry the current media item without losing the user's last position.
     * This is deliberately centralized so every playback surface gets the same
     * recovery behavior.
     */
    fun retry() {
        val exoPlayer = player ?: return
        val currentItem = exoPlayer.currentMediaItem ?: return
        val positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        val uri = currentItem.localConfiguration?.uri?.toString() ?: return

        _state.value = _state.value.copy(errorMessage = null, uri = uri)
        exoPlayer.setMediaItem(currentItem, positionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        publish()
        persist(force = true)
    }

    /** Called when the app no longer needs the decoder. State is persisted first. */
    fun release() {
        persist(force = true)
        player?.removeListener(internalListener)
        player?.release()
        player = null
        tickerJob?.cancel()
        tickerJob = null
        // Do not cancel the manager scope: this object is application-scoped and
        // may legitimately recreate a player later in the same process.
        publish()
    }

    fun addListener(listener: Player.Listener) {
        if (listeners.add(listener)) player?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        if (listeners.remove(listener)) player?.removeListener(listener)
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (player?.isPlaying == true) {
                    // Position is high-frequency UI state; disk persistence is not.
                    // Avoid SharedPreferences writes every second while preserving
                    // a recent resume point if the process disappears unexpectedly.
                    publish()
                    persistIfNeeded()
                }
            }
        }
    }

    private fun publish() {
        val p = player
        _state.value = PlaybackState(
            uri = p?.currentMediaItem?.localConfiguration?.uri?.toString() ?: _state.value.uri,
            positionMs = p?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs,
            durationMs = p?.duration?.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            isPlaying = p?.isPlaying == true,
            playbackState = p?.playbackState ?: Player.STATE_IDLE,
            playbackSpeed = p?.playbackParameters?.speed ?: _state.value.playbackSpeed,
            errorMessage = _state.value.errorMessage,
        )
    }

    private fun persistIfNeeded() {
        val uri = currentUri() ?: return
        val position = currentPositionMs()
        if (uri != lastPersistedUri || lastPersistedPositionMs < 0L ||
            kotlin.math.abs(position - lastPersistedPositionMs) >= PERSIST_POSITION_DELTA_MS
        ) {
            persist(force = false)
        }
    }

    private fun persist(force: Boolean = false) {
        val uri = currentUri() ?: return
        val position = currentPositionMs()
        if (!force && uri == lastPersistedUri &&
            lastPersistedPositionMs >= 0L &&
            kotlin.math.abs(position - lastPersistedPositionMs) < PERSIST_POSITION_DELTA_MS
        ) return

        val duration = player?.duration?.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        // Keep the session store consistent with the library's 95% completion
        // rule. A finished item should not reappear as a resumable session after
        // process death or Android-driven MediaSession resumption.
        if (duration > 0L && position >= (duration * 0.95f).toLong()) {
            sessionStore.clear()
            lastPersistedUri = null
            lastPersistedPositionMs = -1L
            return
        }

        val snapshot = PlaybackSnapshot(
            uri = uri,
            title = player?.currentMediaItem?.mediaMetadata?.title?.toString()
                ?.takeIf { it.isNotBlank() },
            positionMs = position,
            playbackSpeed = player?.playbackParameters?.speed ?: _state.value.playbackSpeed,
        )
        sessionStore.write(snapshot)
        lastPersistedUri = uri
        lastPersistedPositionMs = position
    }

    /** Lightweight, opt-in diagnostics for measuring real playback behavior without log spam. */
    fun snapshot(): PlaybackDiagnostics = PlaybackDiagnostics(
        uri = currentUri(),
        positionMs = currentPositionMs(),
        durationMs = player?.duration?.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
        playbackState = player?.playbackState ?: Player.STATE_IDLE,
        isPlaying = player?.isPlaying == true,
        bufferedPositionMs = player?.bufferedPosition?.coerceAtLeast(0L) ?: 0L,
        bufferedPercentage = player?.bufferedPercentage?.coerceIn(0, 100) ?: 0,
        playbackSpeed = player?.playbackParameters?.speed ?: _state.value.playbackSpeed,
        hasError = _state.value.errorMessage != null,
    )

    companion object {
        private const val PERSIST_POSITION_DELTA_MS = 10_000L
    }
}

data class PlaybackDiagnostics(
    val uri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackState: Int = Player.STATE_IDLE,
    val isPlaying: Boolean = false,
    val bufferedPositionMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val hasError: Boolean = false,
)

data class PlaybackState(
    val uri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null,
)
