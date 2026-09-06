package com.rvh.video.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var player: ExoPlayer? = null
    private val listeners = LinkedHashSet<Player.Listener>()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val internalListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) = publish()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = publish()
        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(errorMessage = error.message ?: "Playback error")
            persist()
        }
    }

    fun getOrCreate(): ExoPlayer = player ?: ExoPlayer.Builder(appContext)
        .build()
        .also { created ->
            created.addListener(internalListener)
            listeners.forEach(created::addListener)
            player = created
            startTicker()
            publish()
        }

    fun play(uri: String, startPositionMs: Long = 0L) {
        val exoPlayer = getOrCreate()
        exoPlayer.setMediaItem(MediaItem.fromUri(uri), startPositionMs.coerceAtLeast(0L))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _state.value = _state.value.copy(uri = uri, errorMessage = null)
        persist()
    }

    fun currentPositionMs(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs

    /** Position saved from the previous app process, but only for this exact media URI. */
    fun savedPositionFor(uri: String): Long {
        val saved = sessionStore.read()
        return if (saved.uri == uri) saved.positionMs else 0L
    }

    /** Playback speed saved from the previous app process. */
    fun savedPlaybackSpeed(): Float = sessionStore.read().playbackSpeed

    fun currentUri(): String? = player?.currentMediaItem?.localConfiguration?.uri?.toString() ?: _state.value.uri

    fun pause() {
        player?.pause()
        publish()
        persist()
    }

    fun resume() {
        getOrCreate().play()
        publish()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
        publish()
        persist()
    }

    fun setPlaybackSpeed(speed: Float) {
        getOrCreate().setPlaybackSpeed(speed.coerceIn(0.25f, 4.0f))
        publish()
        persist()
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun stopAndClear() {
        player?.stop()
        _state.value = PlaybackState()
        sessionStore.clear()
    }

    fun retry() {
        player?.prepare()
        player?.play()
        publish()
    }

    /** Called when the app no longer needs the decoder. State is persisted first. */
    fun release() {
        persist()
        player?.removeListener(internalListener)
        player?.release()
        player = null
        tickerJob?.cancel()
        tickerJob = null
        scope.cancel()
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
            while (true) {
                delay(1000)
                if (player?.isPlaying == true) {
                    publish()
                    persist()
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

    private fun persist() {
        val snapshot = PlaybackSnapshot(
            uri = currentUri(),
            positionMs = currentPositionMs(),
            playbackSpeed = player?.playbackParameters?.speed ?: _state.value.playbackSpeed,
        )
        if (snapshot.uri != null) sessionStore.write(snapshot)
    }
}

data class PlaybackState(
    val uri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null,
)
