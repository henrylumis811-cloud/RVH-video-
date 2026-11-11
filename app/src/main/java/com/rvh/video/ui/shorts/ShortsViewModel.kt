package com.rvh.video.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortsViewModel(private val repository: VideoRepository) : ViewModel() {

    private val libraryShorts = repository
        .observeCategory(VideoCategory.SHORT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _shorts = MutableStateFlow<List<LocalVideoEntity>>(emptyList())
    val shorts: StateFlow<List<LocalVideoEntity>> = _shorts

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady

    private var sessionStarted = false
    private var lastLibrarySnapshot: List<LocalVideoEntity> = emptyList()

    init {
        viewModelScope.launch {
            libraryShorts.collect { library ->
                lastLibrarySnapshot = library
                if (!sessionStarted) {
                    _shorts.value = library
                } else {
                    // Keep the active feed stable while scanning/classification updates
                    // arrive. New media is picked up on the next Shorts session.
                    val knownUris = library.mapTo(HashSet()) { it.uri }
                    _shorts.value = _shorts.value.filter { it.uri in knownUris }
                }
            }
        }
    }

    /** Starts a fresh, intelligent Shorts session. */
    fun startNewSession() {
        sessionStarted = true
        _sessionReady.value = false
        viewModelScope.launch {
            val currentLibrary = runCatching {
                libraryShorts.first()
            }.getOrDefault(lastLibrarySnapshot)
            lastLibrarySnapshot = currentLibrary
            val history = runCatching { repository.getPlaybackHistoryEntries() }.getOrDefault(emptyList())
            _shorts.value = ShortsFeedPlanner.build(
                shorts = currentLibrary,
                history = history,
                seed = System.nanoTime(),
            )
            _sessionReady.value = true
        }
    }

    fun toggleFavorite(uri: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(uri, favorite) }
    }

    fun toggleWatchLater(uri: String, watchLater: Boolean) {
        viewModelScope.launch { repository.setWatchLater(uri, watchLater) }
    }

    fun recategorize(uri: String, target: VideoCategory) {
        viewModelScope.launch { repository.setCategoryOverride(uri, target) }
    }
}
