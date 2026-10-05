package com.rvh.video.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.rvh.video.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI-side playback boundary. The actual ExoPlayer is owned exclusively by
 * PlaybackService; this client only talks to the MediaController.
 */
class PlaybackClient(private val sessionStore: PlaybackSessionStore) {
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionTickerJob: Job? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private var listener: Player.Listener? = null
    private val externalListeners = LinkedHashSet<Player.Listener>()

    fun bind(mediaController: MediaController) {
        unbind()
        controller = mediaController
        _isConnected.value = true
        externalListeners.forEach(mediaController::addListener)
        listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publish(player)
        }.also { mediaController.addListener(it) }
        publish(mediaController)
        startPositionTicker()
    }

    fun unbind() {
        positionTickerJob?.cancel()
        positionTickerJob = null
        val current = controller
        listener?.let { current?.removeListener(it) }
        listener = null
        controller = null
        _isConnected.value = false
        _state.value = PlaybackState()
    }

    /** Called by the MediaController connection listener when the service disappears. */
    fun onControllerDisconnected(disconnectedController: MediaController) {
        if (controller !== disconnectedController) return
        positionTickerJob?.cancel()
        positionTickerJob = null
        listener?.let { disconnectedController.removeListener(it) }
        listener = null
        controller = null
        _isConnected.value = false
        _state.value = PlaybackState()
    }

    fun player(): Player? = controller

    fun playQueue(
        uris: List<String>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
        shuffle: Boolean = false,
        repeatOne: Boolean = false,
        autoAdvance: Boolean = true,
    ) {
        val c = controller ?: return
        if (!c.availableSessionCommands.contains(PlaybackService.SET_PLAYBACK_QUEUE_COMMAND)) return
        val args = android.os.Bundle().apply {
            putStringArrayList(PlaybackService.EXTRA_QUEUE_URIS, ArrayList(uris))
            putInt(PlaybackService.EXTRA_QUEUE_START_INDEX, startIndex)
            putLong(PlaybackService.EXTRA_QUEUE_START_POSITION_MS, startPositionMs.coerceAtLeast(0L))
            putBoolean(PlaybackService.EXTRA_QUEUE_SHUFFLE, shuffle)
            putBoolean(PlaybackService.EXTRA_QUEUE_REPEAT_ONE, repeatOne)
            putBoolean(PlaybackService.EXTRA_QUEUE_AUTO_ADVANCE, autoAdvance)
        }
        c.sendCustomCommand(PlaybackService.SET_PLAYBACK_QUEUE_COMMAND, args)
        publish(c)
    }

    fun setQueueMode(shuffle: Boolean, repeatOne: Boolean, autoAdvance: Boolean) {
        val c = controller ?: return
        if (!c.availableSessionCommands.contains(PlaybackService.SET_PLAYBACK_QUEUE_MODE_COMMAND)) return
        val args = android.os.Bundle().apply {
            putBoolean(PlaybackService.EXTRA_QUEUE_SHUFFLE, shuffle)
            putBoolean(PlaybackService.EXTRA_QUEUE_REPEAT_ONE, repeatOne)
            putBoolean(PlaybackService.EXTRA_QUEUE_AUTO_ADVANCE, autoAdvance)
        }
        c.sendCustomCommand(PlaybackService.SET_PLAYBACK_QUEUE_MODE_COMMAND, args)
    }

    fun play(uri: String, startPositionMs: Long = 0L) {
        val c = controller ?: return
        val title = uri.substringAfterLast('/').substringBeforeLast('.').ifBlank { "RVH Video" }
        val item = MediaItem.Builder()
            .setMediaId(uri)
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()
        c.setMediaItem(item, startPositionMs.coerceAtLeast(0L))
        c.prepare()
        c.play()
        publish(c)
    }

    fun currentPositionMs(): Long = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs

    fun savedPositionFor(uri: String): Long {
        val saved = sessionStore.read()
        return if (saved.uri == uri) saved.positionMs else 0L
    }

    fun savedPlaybackSpeed(): Float = sessionStore.read().playbackSpeed

    fun currentUri(): String? = controller?.currentMediaItem?.localConfiguration?.uri?.toString() ?: _state.value.uri

    fun playNext() {
        val c = controller ?: return
        if (c.availableSessionCommands.contains(PlaybackService.NEXT_QUEUE_ITEM_COMMAND)) {
            c.sendCustomCommand(PlaybackService.NEXT_QUEUE_ITEM_COMMAND, android.os.Bundle.EMPTY)
        }
        publish(c)
    }

    fun playPrevious() {
        val c = controller ?: return
        if (c.availableSessionCommands.contains(PlaybackService.PREVIOUS_QUEUE_ITEM_COMMAND)) {
            c.sendCustomCommand(PlaybackService.PREVIOUS_QUEUE_ITEM_COMMAND, android.os.Bundle.EMPTY)
        }
        publish(c)
    }

    fun pause() { controller?.pause(); controller?.let(::publish) }
    fun resume() { controller?.play(); controller?.let(::publish) }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        controller?.let(::publish)
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackParameters(PlaybackParameters(speed.coerceIn(0.25f, 4.0f)))
        controller?.let(::publish)
    }

    fun isPlaying(): Boolean = controller?.isPlaying == true

    fun stopAndClear() {
        val c = controller
        if (c == null) {
            sessionStore.clear()
            _state.value = PlaybackState()
            return
        }
        if (c.availableSessionCommands.contains(PlaybackService.CLEAR_PLAYBACK_COMMAND)) {
            c.sendCustomCommand(PlaybackService.CLEAR_PLAYBACK_COMMAND, android.os.Bundle.EMPTY)
        } else {
            // Fallback for a transiently disconnected/legacy session. The normal
            // in-app path always uses the service-owned command above.
            c.stop()
            c.clearMediaItems()
            sessionStore.clear()
        }
        _state.value = PlaybackState()
    }

    fun retry() {
        val c = controller ?: return
        val item = c.currentMediaItem ?: return
        c.setMediaItem(item, c.currentPosition.coerceAtLeast(0L))
        c.prepare()
        c.play()
        publish(c)
    }

    fun addListener(listener: Player.Listener) {
        if (externalListeners.add(listener)) controller?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        if (externalListeners.remove(listener)) controller?.removeListener(listener)
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive) {
                val current = controller
                if (current?.isPlaying == true) {
                    // MediaController does not emit an event for every playback-position
                    // change. Keep the UI-facing state alive while playback advances so
                    // timelines, elapsed time and related HUDs remain live.
                    publish(current)
                    delay(250)
                } else {
                    delay(500)
                }
            }
        }
    }

    private fun publish(player: Player) {
        _state.value = PlaybackState(
            uri = player.currentMediaItem?.localConfiguration?.uri?.toString(),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            isPlaying = player.isPlaying,
            playbackState = player.playbackState,
            playbackSpeed = player.playbackParameters.speed,
            errorMessage = friendlyPlaybackError(player.playerError),
        )
    }
}
