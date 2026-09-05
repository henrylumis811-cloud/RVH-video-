package com.rvh.video.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.Surface2
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenMedia: (LocalVideoEntity) -> Unit,
    onToggleFavorite: (LocalVideoEntity) -> Unit,
) {
    val results by viewModel.results.collectAsState()
    val query by viewModel.queryText.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val sort by viewModel.sortMode.collectAsState()
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
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isBlank()) Text("Search names or folders…", color = TextSecondary)
                        inner()
                    }
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("All", selected == null) { viewModel.setCategory(null) }
            FilterChip("Movies", selected == VideoCategory.MOVIE) { viewModel.setCategory(VideoCategory.MOVIE) }
            FilterChip("Music", selected == VideoCategory.MUSIC_VIDEO) { viewModel.setCategory(VideoCategory.MUSIC_VIDEO) }
            FilterChip("Shorts", selected == VideoCategory.SHORT) { viewModel.setCategory(VideoCategory.SHORT) }
        }

        Text("${results.size} result${if (results.size == 1) "" else "s"} · ${sortLabel(sort)}", style = RvhType.Meta, color = TextSecondary)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(results, key = { it.uri }) { video ->
                SearchResultCard(video, onOpen = { onOpenMedia(video) }, onFavorite = { onToggleFavorite(video) })
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

@Composable
private fun SearchResultCard(video: LocalVideoEntity, onOpen: () -> Unit, onFavorite: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).glassSurface(shape = RoundedCornerShape(14.dp)).clickable(onClick = onOpen)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(video.uri).videoFrameMillis(1000).crossfade(true).build(),
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(Surface1),
            )
            IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Favorite, "Favorite", tint = if (video.isFavorite) AccentTeal else Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.size(7.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.effectiveCategory.name.replace('_', ' '), style = RvhType.Meta, color = TextSecondary)
        }
        // A small dedicated play surface avoids making the favorite button part of the click target.
        androidx.compose.material3.TextButton(onClick = onOpen) { Text("Open", color = AccentTeal) }
    }
}

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.NEWEST -> "Newest"
    SortMode.OLDEST -> "Oldest"
    SortMode.NAME -> "Name"
    SortMode.LONGEST -> "Longest"
}
