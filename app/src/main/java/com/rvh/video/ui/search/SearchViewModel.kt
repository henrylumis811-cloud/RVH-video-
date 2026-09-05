package com.rvh.video.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: VideoRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<VideoCategory?>(null)
    private val sort = MutableStateFlow(SortMode.NEWEST)

    val queryText: StateFlow<String> = query
    val selectedCategory: StateFlow<VideoCategory?> = category
    val sortMode: StateFlow<SortMode> = sort

    val results: StateFlow<List<LocalVideoEntity>> = combine(
        repository.observeAll(), query, category, sort
    ) { videos, q, selected, ordering ->
        videos.asSequence()
            .filter { selected == null || it.effectiveCategory == selected }
            .filter {
                q.isBlank() || it.displayName.contains(q.trim(), ignoreCase = true) ||
                    it.folderName.contains(q.trim(), ignoreCase = true)
            }
            .let { sequence ->
                when (ordering) {
                    SortMode.NEWEST -> sequence.sortedByDescending { it.dateModifiedEpochSeconds }
                    SortMode.OLDEST -> sequence.sortedBy { it.dateModifiedEpochSeconds }
                    SortMode.NAME -> sequence.sortedBy { it.displayName.lowercase() }
                    SortMode.LONGEST -> sequence.sortedByDescending { it.durationMs }
                }
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: VideoCategory?) { category.value = value }
    fun setSort(value: SortMode) { sort.value = value }

    fun toggleFavorite(uri: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(uri, favorite) }
    }
}

enum class SortMode { NEWEST, OLDEST, NAME, LONGEST }
