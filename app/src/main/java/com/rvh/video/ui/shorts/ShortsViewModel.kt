package com.rvh.video.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortsViewModel(private val repository: VideoRepository) : ViewModel() {

    val shorts: StateFlow<List<LocalVideoEntity>> = repository
        .observeCategory(VideoCategory.SHORT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
