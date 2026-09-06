package com.rvh.video.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: VideoRepository) : ViewModel() {
    val recentlyAdded = repository.observeRecentlyAdded(12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val continueWatching = repository.observeContinueWatching(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchLater = repository.observeWatchLater()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackHistory = repository.observePlaybackHistory(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(uri: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(uri, favorite) }
    }

    fun toggleWatchLater(uri: String, watchLater: Boolean) {
        viewModelScope.launch { repository.setWatchLater(uri, watchLater) }
    }

    val libraryCount = repository.observeLibraryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * A lightweight on-device recommendation rail. It learns only from local
     * signals: favorites, Watch Later and recently played categories. No data
     * leaves the device and there is no network dependency.
     */
    val forYou = combine(
        repository.observeAll(),
        repository.observeFavorites(),
        repository.observeWatchLater(),
        repository.observePlaybackHistory(20)
    ) { all, favoriteItems, watchLaterItems, historyItems ->
        val favoriteCategories = favoriteItems.groupingBy { it.effectiveCategory }.eachCount()
        val watchedCategories = historyItems.groupingBy { it.effectiveCategory }.eachCount()
        val watchLaterUris = watchLaterItems.mapTo(hashSetOf()) { it.uri }
        val favoriteUris = favoriteItems.mapTo(hashSetOf()) { it.uri }
        val historyUris = historyItems.mapTo(hashSetOf()) { it.uri }

        all.asSequence()
            .filter { it.uri !in favoriteUris && it.uri !in watchLaterUris }
            .map { video ->
                val categoryScore = (favoriteCategories[video.effectiveCategory] ?: 0) * 12 +
                    (watchedCategories[video.effectiveCategory] ?: 0) * 6
                val resumeScore = if (video.resumePositionMs > 0) 18 else 0
                val unseenScore = if (video.uri !in historyUris) 8 else 0
                val freshnessScore = ((video.dateModifiedEpochSeconds / 86_400L) % 30L).toInt()
                video to (categoryScore + resumeScore + unseenScore + freshnessScore)
            }
            .sortedWith(compareByDescending<Pair<com.rvh.video.data.model.LocalVideoEntity, Int>> { it.second }
                .thenByDescending { it.first.dateModifiedEpochSeconds })
            .take(10)
            .map { it.first }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeFolder(folder: String): Flow<List<com.rvh.video.data.model.LocalVideoEntity>> =
        repository.observeAll().map { videos ->
            videos.filter {
                if (folder == "Unknown folder") it.folderName.isBlank() else it.folderName == folder
            }
        }

    val folderGroups = repository.observeAll()
        .map { videos ->
            videos.groupBy { it.folderName.ifBlank { "Unknown folder" } }
                .map { (folder, items) -> folder to items.size }
                .sortedByDescending { it.second }
                .take(8)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
