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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.RvhViewModelFactory
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
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
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    var sortExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // Matches Movies' search behavior — was previously just a decorative
    // icon with no field or filtering behind it at all.
    val filtered = remember(queue, query, sortMode) {
        val searched = if (query.isBlank()) queue else queue.filter { it.displayName.contains(query, ignoreCase = true) }
        when (sortMode) {
            "oldest" -> searched.sortedBy { it.dateModifiedEpochSeconds }
            "name" -> searched.sortedBy { it.displayName.lowercase() }
            "longest" -> searched.sortedByDescending { it.durationMs }
            else -> searched.sortedByDescending { it.dateModifiedEpochSeconds }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // was rendering under the status bar icons
                .padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RVH Music", style = RvhType.ScreenTitle, modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                Box {
                    IconButton(onClick = { sortExpanded = true }) { Icon(Icons.Filled.Sort, "Sort") }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        listOf("newest" to "Newest", "oldest" to "Oldest", "name" to "Name", "longest" to "Longest").forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSortMode(value); sortExpanded = false })
                        }
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
                            Text("Search", style = RvhType.Body, color = TextSecondary)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filtered, key = { it.uri }) { video ->
                    val index = queue.indexOf(video)
                    MusicVideoRow(
                        video = video,
                        isCurrent = index == currentIndex,
                        isPlaying = isPlaying,
                        onPlayPauseClick = {
                            if (index == currentIndex) viewModel.togglePlayPause()
                            else { viewModel.playAt(index); onOpenFullPlayer() }
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
