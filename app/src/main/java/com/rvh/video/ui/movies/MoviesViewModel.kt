package com.rvh.video.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoviesViewModel(application: Application, private val repository: VideoRepository) : AndroidViewModel(application) {

    private val settings = com.rvh.video.data.local.AppSettings(application)
    val sortMode = kotlinx.coroutines.flow.MutableStateFlow(settings.librarySortMode)
    val gridColumns = kotlinx.coroutines.flow.MutableStateFlow(settings.movieGridColumns)

    val movies: StateFlow<List<LocalVideoEntity>> = repository
        .observeCategory(VideoCategory.MOVIE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSortMode(value: String) { settings.librarySortMode = value; sortMode.value = value }
    fun setGridColumns(value: Int) { settings.movieGridColumns = value; gridColumns.value = value.coerceIn(2, 4) }

    fun saveResumePosition(uri: String, positionMs: Long) {
        viewModelScope.launch { repository.saveResumePosition(uri, positionMs) }
    }

    fun toggleFavorite(uri: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(uri, favorite) }
    }

    fun toggleWatchLater(uri: String, watchLater: Boolean) {
        viewModelScope.launch { repository.setWatchLater(uri, watchLater) }
    }

    /** Long-press "Move to..." action from the grid context menu. */
    fun recategorize(uri: String, target: VideoCategory) {
        viewModelScope.launch { repository.setCategoryOverride(uri, target) }
    }
}
