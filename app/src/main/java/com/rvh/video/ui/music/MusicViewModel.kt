package com.rvh.video.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.local.AppSettings
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.player.PlayerManager
import com.rvh.video.player.PlaybackSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application,
    private val repository: VideoRepository,
    val playerManager: PlayerManager,
) : AndroidViewModel(application) {

    val queue: StateFlow<List<LocalVideoEntity>> = repository
        .observeCategory(VideoCategory.MUSIC_VIDEO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentIndex = MutableStateFlow<Int?>(null)
    val currentIndex: StateFlow<Int?> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    private val settings = AppSettings(application)
    val sortMode = MutableStateFlow(settings.librarySortMode)
    private var restoredPositionMs: Long = 0L
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        playerManager.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && settings.autoAdvanceEnabled) playNext()
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                _isPlaying.value = isPlayingNow
            }
        })

        // Reconcile the music selection with the single global player. If a
        // movie takes ownership of the player, the music mini-player must not
        // keep advertising a stale track whose controls now affect the movie.
        viewModelScope.launch {
            playerManager.state.collect { playback ->
                _isPlaying.value = playback.isPlaying
                val uri = playback.uri ?: return@collect
                val index = queue.value.indexOfFirst { it.uri == uri }
                _currentIndex.value = index.takeIf { it >= 0 }
            }
        }

        // Restore the last music selection once the Room flow has delivered
        // its first real library snapshot. Restoration is intentionally
        // paused; the user must press play before media starts.
        viewModelScope.launch {
            val saved = PlaybackSessionStore(application).read()
            val restoredQueue = queue.filter { it.isNotEmpty() }.first()
            val restoredIndex = saved.uri?.let { uri -> restoredQueue.indexOfFirst { it.uri == uri } } ?: -1
            if (restoredIndex >= 0) {
                _currentIndex.value = restoredIndex
                restoredPositionMs = saved.positionMs
                _isPlaying.value = false
            }
        }
    }

    fun playAt(index: Int) {
        val track = queue.value.getOrNull(index) ?: return
        _currentIndex.value = index
        _isPlaying.value = true
        val startPosition = if (track.uri == playerManager.state.value.uri) restoredPositionMs else 0L
        restoredPositionMs = 0L
        playerManager.play(track.uri, startPosition)
    }

    fun togglePlayPause() {
        if (_currentIndex.value == null) {
            if (queue.value.isNotEmpty()) playAt(0)
            return
        }
        if (_isPlaying.value) playerManager.pause() else playerManager.resume()
    }

    fun playNext() {
        val idx = _currentIndex.value ?: return
        val next = idx + 1
        if (next < queue.value.size) {
            playAt(next)
        } else {
            _isPlaying.value = false
        }
    }

    fun playPrevious() {
        val idx = _currentIndex.value ?: return
        val prev = idx - 1
        if (prev >= 0) playAt(prev)
    }

    fun stopAndClearQueue() {
        playerManager.stopAndClear()
        _currentIndex.value = null
        _isPlaying.value = false
    }

    fun setSortMode(value: String) { settings.librarySortMode = value; sortMode.value = value }

    fun toggleFavorite(uri: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(uri, favorite) }
    }

    fun toggleWatchLater(uri: String, watchLater: Boolean) {
        viewModelScope.launch { repository.setWatchLater(uri, watchLater) }
    }

    fun recategorize(uri: String, target: VideoCategory) {
        viewModelScope.launch { repository.setCategoryOverride(uri, target) }
    }

    override fun onCleared() {
        // PlayerManager is application-scoped and intentionally survives
        // navigation/ViewModel recreation. The Application owns its release.
        super.onCleared()
    }
}
