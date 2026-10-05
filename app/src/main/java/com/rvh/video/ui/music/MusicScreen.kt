package com.rvh.video.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.RvhViewModelFactory
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.TextPrimary
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

@Composable
fun MusicScreen(
    viewModel: MusicViewModel = viewModel(
        factory = RvhViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onOpenFullPlayer: () -> Unit,
    onAddToCollection: (LocalVideoEntity) -> Unit = {},
    onToggleWatchLater: (String, Boolean) -> Unit = { _, _ -> },
    onOpenDetails: (String) -> Unit = {},
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    var sortExpanded by remember { mutableStateOf(false) }
    var displayExpanded by remember { mutableStateOf(false) }

    val filtered by viewModel.filteredQueue.collectAsStateWithLifecycle()
    val queueIndex = remember(queue) { queue.withIndex().associate { it.value.uri to it.index } }

    // Keep the query in the ViewModel so filtering/sorting is performed off the
    // main thread and remains stable across recomposition.


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // was rendering under the status bar icons
                .padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RVH Music", style = RvhType.ScreenTitle, color = TextPrimary, modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                Box {
                    IconButton(onClick = { sortExpanded = true }) { Icon(Icons.Filled.Sort, "Sort", tint = TextPrimary) }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        listOf("newest" to "Newest", "oldest" to "Oldest", "name" to "Name", "longest" to "Longest").forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSortMode(value); sortExpanded = false })
                        }
                    }
                }
                Box {
                    IconButton(onClick = { displayExpanded = true }) {
                        Icon(if (displayMode == com.rvh.video.data.local.AppSettings.DISPLAY_GRID) Icons.Filled.ViewModule else Icons.Filled.ViewList, "Display type", tint = TextPrimary)
                    }
                    DropdownMenu(expanded = displayExpanded, onDismissRequest = { displayExpanded = false }) {
                        DropdownMenuItem(text = { Text("List") }, onClick = { viewModel.setDisplayMode(com.rvh.video.data.local.AppSettings.DISPLAY_LIST); displayExpanded = false })
                        DropdownMenuItem(text = { Text("Grid") }, onClick = { viewModel.setDisplayMode(com.rvh.video.data.local.AppSettings.DISPLAY_GRID); displayExpanded = false })
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search names, folders, or categories…", style = RvhType.Body, color = TextSecondary)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                }
            }

            if (query.isNotBlank()) {
                Text(
                    "${filtered.size} result${if (filtered.size == 1) "" else "s"}",
                    style = RvhType.Meta,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (displayMode == com.rvh.video.data.local.AppSettings.DISPLAY_LIST) {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp)) {
                    items(filtered, key = { it.uri }) { video ->
                        val index = queueIndex[video.uri] ?: -1
                        MusicVideoRow(
                            video = video,
                            isCurrent = index == currentIndex,
                            isPlaying = isPlaying,
                            onPlayPauseClick = {
                                if (index == currentIndex && viewModel.isActivePlayback(video.uri)) {
                                    viewModel.togglePlayPause()
                                } else {
                                    viewModel.playAt(index)
                                    onOpenFullPlayer()
                                }
                            },
                            onRecategorize = { viewModel.recategorize(video.uri, it) },
                            onToggleFavorite = { viewModel.toggleFavorite(video.uri, !video.isFavorite) },
                            onAddToCollection = { onAddToCollection(video) },
                            isWatchLater = video.isWatchLater,
                            onToggleWatchLater = { onToggleWatchLater(video.uri, !video.isWatchLater) },
                            onDetails = { onOpenDetails(video.uri) },
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    modifier = Modifier.weight(1f),
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(filtered, key = { it.uri }) { video ->
                        val index = queueIndex[video.uri] ?: -1
                        MusicVideoGridCard(
                            video = video,
                            isCurrent = index == currentIndex,
                            isPlaying = isPlaying,
                            onPlayPauseClick = {
                                if (index == currentIndex && viewModel.isActivePlayback(video.uri)) {
                                    viewModel.togglePlayPause()
                                } else {
                                    viewModel.playAt(index)
                                    onOpenFullPlayer()
                                }
                            },
                            onRecategorize = { viewModel.recategorize(video.uri, it) },
                            onToggleFavorite = { viewModel.toggleFavorite(video.uri, !video.isFavorite) },
                            onAddToCollection = { onAddToCollection(video) },
                            isWatchLater = video.isWatchLater,
                            onToggleWatchLater = { onToggleWatchLater(video.uri, !video.isWatchLater) },
                            onDetails = { onOpenDetails(video.uri) },
                        )
                    }
                }
            }
        }
    }
}
