package com.rvh.video.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.rvh.video.data.local.AppSettings
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val repository: VideoRepository,
    private val settings: AppSettings,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<VideoCategory?>(null)
    private val sort = MutableStateFlow(SortMode.NEWEST)
    private val _recentSearches = MutableStateFlow(settings.recentSearches)

    val queryText: StateFlow<String> = query
    val selectedCategory: StateFlow<VideoCategory?> = category
    val sortMode: StateFlow<SortMode> = sort
    val recentSearches: StateFlow<List<String>> = _recentSearches

    /**
     * Search is intentionally local-only. A short debounce prevents expensive
     * re-ranking on every keystroke while keeping the UI feeling immediate.
     * Matching uses title/folder/category tokens and ranks useful matches before
     * applying the user's selected ordering as a deterministic tie-breaker.
     */
    val results: StateFlow<List<LocalVideoEntity>> = combine(
        repository.observeAll(),
        query.debounce(180),
        category,
        sort
    ) { videos, rawQuery, selected, ordering ->
        val normalizedQuery = normalize(rawQuery)
        val tokens = normalizedQuery.split(' ').filter { it.isNotEmpty() }

        videos.asSequence()
            .filter { selected == null || it.effectiveCategory == selected }
            .mapNotNull { video ->
                if (tokens.isEmpty()) {
                    video to 0
                } else {
                    val score = matchScore(video, normalizedQuery, tokens)
                    if (score > 0) video to score else null
                }
            }
            .let { sequence ->
                if (tokens.isEmpty()) {
                    applyOrdering(sequence.map { it.first }, ordering)
                } else {
                    sequence
                        .sortedWith(Comparator { left, right ->
                            val scoreComparison = right.second.compareTo(left.second)
                            if (scoreComparison != 0) scoreComparison
                            else compareForOrdering(left.first, right.first, ordering)
                        })
                        .map { it.first }
                        .toList()
                }
            }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { query.value = value }

    fun submitQuery() {
        val clean = normalize(query.value)
        if (clean.isEmpty()) return
        settings.rememberSearch(clean)
        _recentSearches.value = settings.recentSearches
    }

    fun useRecentSearch(value: String) {
        query.value = value
        submitQuery()
    }

    fun clearQuery() { query.value = "" }

    fun toggleFavorite(uri: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(uri, favorite) }
    }

    fun toggleWatchLater(uri: String, watchLater: Boolean) {
        viewModelScope.launch { repository.setWatchLater(uri, watchLater) }
    }

    fun recategorize(uri: String, category: VideoCategory) {
        viewModelScope.launch { repository.setCategoryOverride(uri, category) }
    }

    fun clearRecentSearches() {
        settings.clearRecentSearches()
        _recentSearches.value = emptyList()
    }

    fun setCategory(value: VideoCategory?) { category.value = value }
    fun setSort(value: SortMode) { sort.value = value }

    private fun matchScore(video: LocalVideoEntity, query: String, tokens: List<String>): Int {
        val title = normalize(video.displayName.substringBeforeLast('.'))
        val folder = normalize(video.folderName)
        val categoryName = normalize(video.effectiveCategory.name.replace('_', ' '))
        val searchable = "$title $folder $categoryName"

        if (!tokens.all { token -> searchable.contains(token) }) return 0

        var score = 10
        when {
            title == query -> score += 120
            title.startsWith(query) -> score += 80
            title.contains(query) -> score += 45
        }
        if (folder == query) score += 55
        else if (folder.startsWith(query)) score += 30
        else if (folder.contains(query)) score += 15
        if (categoryName == query) score += 25
        score += tokens.sumOf { token ->
            when {
                title.split(' ').any { it == token } -> 12
                folder.split(' ').any { it == token } -> 6
                else -> 0
            }
        }
        return score
    }

    private fun applyOrdering(videos: Sequence<LocalVideoEntity>, ordering: SortMode): List<LocalVideoEntity> =
        videos.sortedWith { left, right -> compareForOrdering(left, right, ordering) }.toList()

    private fun compareForOrdering(left: LocalVideoEntity, right: LocalVideoEntity, ordering: SortMode): Int = when (ordering) {
        SortMode.NEWEST -> right.dateModifiedEpochSeconds.compareTo(left.dateModifiedEpochSeconds)
        SortMode.OLDEST -> left.dateModifiedEpochSeconds.compareTo(right.dateModifiedEpochSeconds)
        SortMode.NAME -> compareValues(left.displayName.lowercase(), right.displayName.lowercase())
        SortMode.LONGEST -> right.durationMs.compareTo(left.durationMs)
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
}

enum class SortMode { NEWEST, OLDEST, NAME, LONGEST }
