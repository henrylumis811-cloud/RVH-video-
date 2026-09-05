package com.rvh.video.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(private val repository: VideoRepository) : ViewModel() {
    val collections: StateFlow<List<com.rvh.video.data.model.VideoCollectionEntity>> = repository
        .observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createCollection(name) }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameCollection(id, name) }
    }

    fun delete(id: Long) { viewModelScope.launch { repository.deleteCollection(id) } }

    fun addFavorites(id: Long) { viewModelScope.launch { repository.addFavoritesToCollection(id) } }

    fun addVideo(id: Long, uri: String) { viewModelScope.launch { repository.addToCollection(id, uri) } }

    fun remove(id: Long, uri: String) { viewModelScope.launch { repository.removeFromCollection(id, uri) } }

    fun move(id: Long, uri: String, direction: Int) {
        viewModelScope.launch { repository.moveInCollection(id, uri, direction) }
    }

    fun items(id: Long) = repository.observeCollectionItems(id)
}
