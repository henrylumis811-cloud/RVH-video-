package com.rvh.video.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaDetailsViewModel(private val repository: VideoRepository) : ViewModel() {
    private val _video = MutableStateFlow<LocalVideoEntity?>(null)
    val video: StateFlow<LocalVideoEntity?> = _video.asStateFlow()

    fun load(uri: String?) {
        if (uri == null) { _video.value = null; return }
        viewModelScope.launch { _video.value = repository.getByUri(uri) }
    }

    fun toggleFavorite() { _video.value?.let { v -> viewModelScope.launch { repository.setFavorite(v.uri, !v.isFavorite); _video.value = repository.getByUri(v.uri) } } }
    fun toggleWatchLater() { _video.value?.let { v -> viewModelScope.launch { repository.setWatchLater(v.uri, !v.isWatchLater); _video.value = repository.getByUri(v.uri) } } }
}
