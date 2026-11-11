package com.rvh.video.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaDetailsViewModel(private val repository: VideoRepository) : ViewModel() {
    private val uri = MutableStateFlow<String?>(null)

    val video: StateFlow<LocalVideoEntity?> = uri
        .flatMapLatest { value -> value?.let(repository::observeByUri) ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load(uri: String?) { this.uri.value = uri }

    fun toggleFavorite() {
        video.value?.let { v -> viewModelScope.launch { repository.setFavorite(v.uri, !v.isFavorite) } }
    }

    fun toggleWatchLater() {
        video.value?.let { v -> viewModelScope.launch { repository.setWatchLater(v.uri, !v.isWatchLater) } }
    }

    fun moveTo(category: VideoCategory) {
        video.value?.let { v -> viewModelScope.launch { repository.setCategoryOverride(v.uri, category) } }
    }

    fun resetCategory() {
        video.value?.let { v -> viewModelScope.launch { repository.setCategoryOverride(v.uri, null) } }
    }
}
