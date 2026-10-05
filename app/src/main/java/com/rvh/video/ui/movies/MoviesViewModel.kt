package com.rvh.video.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The complete derived library state used by MoviesScreen.
 *
 * Expensive filtering/sorting/strategy selection happens off the main thread so
 * the composable can concentrate on rendering rather than repeatedly walking the
 * entire library during recomposition.
 */
data class MoviesLibraryState(
    val allMovies: List<LocalVideoEntity> = emptyList(),
    val filteredMovies: List<LocalVideoEntity> = emptyList(),
    val inProgress: List<LocalVideoEntity> = emptyList(),
    val strategyMovie: LocalVideoEntity? = null,
    val readyCount: Int = 0,
    val runningCount: Int = 0,
    val favoriteCount: Int = 0,
    val fleetProgress: Int = 0,
    val smartQueue: List<LocalVideoEntity> = emptyList(),
)

class MoviesViewModel(application: Application, private val repository: VideoRepository) : AndroidViewModel(application) {

    private val settings = com.rvh.video.data.local.AppSettings(application)
    val sortMode = MutableStateFlow(settings.movieSortMode)
    val gridColumns = MutableStateFlow(settings.movieGridColumns)
    val displayMode = MutableStateFlow(settings.movieDisplayMode)

    val movies: StateFlow<List<LocalVideoEntity>> = repository
        .observeCategory(VideoCategory.MOVIE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow("all")

    val libraryState: StateFlow<MoviesLibraryState> = combine(
        movies,
        query,
        sortMode,
        filter,
    ) { allMovies, searchQuery, sort, selectedFilter ->
        MoviesDerivationInput(allMovies, searchQuery, sort, selectedFilter)
    }
        .mapLatest { input ->
            withContext(Dispatchers.Default) {
                deriveLibraryState(input)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoviesLibraryState())

    fun setSearchQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: String) {
        filter.value = value
    }

    fun setSortMode(value: String) {
        settings.movieSortMode = value
        sortMode.value = value
    }

    fun setGridColumns(value: Int) {
        settings.movieGridColumns = value
        gridColumns.value = value.coerceIn(2, 4)
    }

    fun setDisplayMode(value: String) {
        settings.movieDisplayMode = value
        displayMode.value = value
    }

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

    private data class MoviesDerivationInput(
        val movies: List<LocalVideoEntity>,
        val query: String,
        val sortMode: String,
        val filter: String,
    )

    private fun deriveLibraryState(input: MoviesDerivationInput): MoviesLibraryState {
        val all = input.movies
        val searched = if (input.query.isBlank()) {
            all
        } else {
            all.filter { it.displayName.contains(input.query, ignoreCase = true) }
        }

        val narrowed = when (input.filter) {
            "resume" -> searched.filter { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
            "favorites" -> searched.filter(LocalVideoEntity::isFavorite)
            "later" -> searched.filter(LocalVideoEntity::isWatchLater)
            else -> searched
        }

        val filtered = when (input.sortMode) {
            "oldest" -> narrowed.sortedBy(LocalVideoEntity::dateModifiedEpochSeconds)
            "name" -> narrowed.sortedBy { it.displayName.lowercase() }
            "longest" -> narrowed.sortedByDescending(LocalVideoEntity::durationMs)
            else -> narrowed.sortedByDescending(LocalVideoEntity::dateModifiedEpochSeconds)
        }

        val inProgress = all.filter {
            it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs
        }

        val strategyMovie = inProgress.maxByOrNull {
            it.resumePositionMs.toDouble() / it.durationMs.coerceAtLeast(1L)
        } ?: all.firstOrNull(LocalVideoEntity::isWatchLater)
            ?: all.firstOrNull(LocalVideoEntity::isFavorite)
            ?: all.maxByOrNull(LocalVideoEntity::dateModifiedEpochSeconds)

        val smartQueue = all.asSequence()
            .filter { it.uri != strategyMovie?.uri }
            .sortedWith(
                compareByDescending<LocalVideoEntity> {
                    when {
                        it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs -> 4
                        it.isWatchLater -> 3
                        it.isFavorite -> 2
                        else -> 1
                    }
                }.thenByDescending { it.dateModifiedEpochSeconds }
            )
            .take(3)
            .toList()

        val playable = all.filter { it.durationMs > 0L }
        val fleetProgress = if (playable.isEmpty()) {
            0
        } else {
            (playable.sumOf {
                it.resumePositionMs.coerceIn(0L, it.durationMs) * 100L / it.durationMs
            } / playable.size).toInt()
        }

        return MoviesLibraryState(
            allMovies = all,
            filteredMovies = filtered,
            inProgress = inProgress,
            strategyMovie = strategyMovie,
            readyCount = all.count { it.resumePositionMs <= 0L },
            runningCount = inProgress.size,
            favoriteCount = all.count(LocalVideoEntity::isFavorite),
            fleetProgress = fleetProgress,
            smartQueue = smartQueue,
        )
    }
}
