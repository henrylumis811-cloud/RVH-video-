package com.rvh.video.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.Surface2
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface
import com.rvh.video.ui.components.rememberRvhThumbnailModel
import com.rvh.video.ui.components.RecategorizeMenu

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenMedia: (LocalVideoEntity) -> Unit,
    onToggleFavorite: (LocalVideoEntity) -> Unit,
    onToggleWatchLater: (LocalVideoEntity) -> Unit,
    onAddToCollection: (LocalVideoEntity) -> Unit,
    onOpenDetails: (LocalVideoEntity) -> Unit,
    onRecategorize: (LocalVideoEntity, VideoCategory) -> Unit,
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sort by viewModel.sortMode.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    var sortExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("Search library", style = RvhType.ScreenTitle, color = Color.White, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { sortExpanded = true }) { Icon(Icons.Filled.Sort, "Sort", tint = AccentTeal) }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(text = { Text(sortLabel(mode)) }, onClick = { viewModel.setSort(mode); sortExpanded = false })
                    }
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().glassSurface(shape = RoundedCornerShape(12.dp), blurRadius = 12.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Search, null, tint = TextSecondary)
                BasicTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.submitQuery() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isBlank()) Text("Search names, folders, or categories…", color = TextSecondary)
                        inner()
                    }
                )
                if (query.isNotBlank()) {
                    IconButton(onClick = viewModel::clearQuery) {
                        Icon(Icons.Filled.Close, "Clear search", tint = TextSecondary)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("All", selected == null) { viewModel.setCategory(null) }
            FilterChip("Movies", selected == VideoCategory.MOVIE) { viewModel.setCategory(VideoCategory.MOVIE) }
            FilterChip("Music", selected == VideoCategory.MUSIC_VIDEO) { viewModel.setCategory(VideoCategory.MUSIC_VIDEO) }
            FilterChip("Shorts", selected == VideoCategory.SHORT) { viewModel.setCategory(VideoCategory.SHORT) }
        }

        if (query.isBlank() && recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.History, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                    Text("Recent searches", style = RvhType.Meta, color = TextSecondary)
                }
                androidx.compose.material3.TextButton(onClick = viewModel::clearRecentSearches) {
                    Text("Clear", color = TextSecondary)
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                items(recentSearches, key = { it }) { recent ->
                    androidx.compose.material3.AssistChip(
                        onClick = { viewModel.useRecentSearch(recent) },
                        label = { Text(recent, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Filled.History, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Search, null, tint = AccentTeal, modifier = Modifier.size(34.dp))
                    Text(
                        if (query.isBlank()) "Your library is empty" else "No media matches \"$query\"",
                        style = RvhType.CardTitle,
                        color = Color.White
                    )
                    Text(
                        if (query.isBlank()) "Scan your device to bring media into the Garage." else "Try a title, folder, or category name.",
                        style = RvhType.Meta,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Text("${results.size} result${if (results.size == 1) "" else "s"} · ${sortLabel(sort)}", style = RvhType.Meta, color = TextSecondary)

            LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(results, key = { it.uri }) { video ->
                SearchResultCard(
                    video,
                    onOpen = { onOpenMedia(video) },
                    onFavorite = { onToggleFavorite(video) },
                    onWatchLater = { onToggleWatchLater(video) },
                    onAddToCollection = { onAddToCollection(video) },
                    onDetails = { onOpenDetails(video) },
                    onRecategorize = { category -> onRecategorize(video, category) },
                )
            }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultCard(
    video: LocalVideoEntity,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onWatchLater: () -> Unit,
    onAddToCollection: () -> Unit,
    onDetails: () -> Unit,
    onRecategorize: (VideoCategory) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val thumbnailModel = rememberRvhThumbnailModel(video.uri, video.dateModifiedEpochSeconds, 360, 225)
    val imageRequest = remember(video.uri, video.dateModifiedEpochSeconds, thumbnailModel) {
        ImageRequest.Builder(context).data(thumbnailModel)
            .size(360, 225)
            .memoryCacheKey("rvh-search:${video.uri}:${video.dateModifiedEpochSeconds}:canonical")
            .diskCacheKey("rvh-search:${video.uri}:${video.dateModifiedEpochSeconds}:canonical")
            .crossfade(false)
            .build()
    }
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .glassSurface(shape = RoundedCornerShape(14.dp))
                .combinedClickable(onClick = onOpen, onLongClick = { menuExpanded = true })
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(Surface1),
            )
        }
        Spacer(Modifier.size(7.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.effectiveCategory.name.replace('_', ' '), style = RvhType.Meta, color = TextSecondary)
        }
        RecategorizeMenu(
            expanded = menuExpanded,
            currentCategory = video.effectiveCategory,
            onDismiss = { menuExpanded = false },
            onSelect = onRecategorize,
            onAddToCollection = onAddToCollection,
            isFavorite = video.isFavorite,
            onToggleFavorite = onFavorite,
            isWatchLater = video.isWatchLater,
            onToggleWatchLater = onWatchLater,
            onDetails = onDetails,
        )
    }
}

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.NEWEST -> "Newest"
    SortMode.OLDEST -> "Oldest"
    SortMode.NAME -> "Name"
    SortMode.LONGEST -> "Longest"
}
