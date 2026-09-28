package com.rvh.video.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.local.AppSettings
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.player.PlaybackClient
import com.rvh.video.player.PlaybackSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application,
    private val repository: VideoRepository,
    val playbackClient: PlaybackClient,
) : AndroidViewModel(application) {

    val queue: StateFlow<List<LocalVideoEntity>> = repository
        .observeCategory(VideoCategory.MUSIC_VIDEO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentIndex = MutableStateFlow<Int?>(null)
    val currentIndex: StateFlow<Int?> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    private val settings = AppSettings(application)
    val musicQueueMode = MutableStateFlow(settings.musicQueueMode)
    val sortMode = MutableStateFlow(settings.librarySortMode)
    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    /** Filter/sort stays off the main thread so large music libraries never
     * make typing or scrolling compete with list rendering. */
    val filteredQueue: StateFlow<List<LocalVideoEntity>> = combine(queue, query, sortMode) { items, q, ordering ->
        val searched = if (q.isBlank()) items else items.filter {
            it.displayName.contains(q.trim(), ignoreCase = true)
        }
        when (ordering) {
            "oldest" -> searched.sortedBy { it.dateModifiedEpochSeconds }
            "name" -> searched.sortedBy { it.displayName.lowercase() }
            "longest" -> searched.sortedByDescending { it.durationMs }
            else -> searched.sortedByDescending { it.dateModifiedEpochSeconds }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var restoredPositionMs: Long = 0L
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        playbackClient.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                _isPlaying.value = isPlayingNow
            }
        })

        // Reconcile the music selection with the single global player. If a
        // movie takes ownership of the player, the music mini-player must not
        // keep advertising a stale track whose controls now affect the movie.
        viewModelScope.launch {
            playbackClient.state.collect { playback ->
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
        val startPosition = if (track.uri == playbackClient.state.value.uri) restoredPositionMs else 0L
        restoredPositionMs = 0L
        val mode = musicQueueMode.value
        playbackClient.playQueue(
            uris = queue.value.map(LocalVideoEntity::uri),
            startIndex = index,
            startPositionMs = startPosition,
            shuffle = mode == AppSettings.MUSIC_QUEUE_SHUFFLE,
            repeatOne = mode == AppSettings.MUSIC_QUEUE_REPEAT_ONE,
            autoAdvance = settings.autoAdvanceEnabled,
        )
    }

    /**
     * Synchronizes the UI selection with media that is already owned by the
     * MediaSession. Unlike playAt(), this never replaces/restarts the player.
     */
    fun selectExistingPlayback(index: Int) {
        if (queue.value.getOrNull(index) == null) return
        _currentIndex.value = index
        _isPlaying.value = playbackClient.isPlaying()
        restoredPositionMs = 0L
    }

    fun togglePlayPause() {
        if (_currentIndex.value == null) {
            if (queue.value.isNotEmpty()) playAt(0)
            return
        }
        if (_isPlaying.value) playbackClient.pause() else playbackClient.resume()
    }

    fun playNext() {
        playbackClient.playNext()
    }

    fun cycleQueueMode() {
        val next = when (musicQueueMode.value) {
            AppSettings.MUSIC_QUEUE_SEQUENTIAL -> AppSettings.MUSIC_QUEUE_SHUFFLE
            AppSettings.MUSIC_QUEUE_SHUFFLE -> AppSettings.MUSIC_QUEUE_REPEAT_ONE
            else -> AppSettings.MUSIC_QUEUE_SEQUENTIAL
        }
        settings.musicQueueMode = next
        musicQueueMode.value = next
        playbackClient.setQueueMode(
            shuffle = next == AppSettings.MUSIC_QUEUE_SHUFFLE,
            repeatOne = next == AppSettings.MUSIC_QUEUE_REPEAT_ONE,
            autoAdvance = settings.autoAdvanceEnabled,
        )
    }

    fun playPrevious() {
        playbackClient.playPrevious()
    }

    fun stopAndClearQueue() {
        playbackClient.stopAndClear()
        _currentIndex.value = null
        _isPlaying.value = false
    }

    fun setSortMode(value: String) { settings.librarySortMode = value; sortMode.value = value }

    fun setQuery(value: String) { query.value = value }

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
        // PlaybackClient is application-scoped and intentionally survives
        // navigation/ViewModel recreation. The Application owns its release.
        super.onCleared()
    }
}
