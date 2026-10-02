package com.rvh.video.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rvh.video.ui.components.RecategorizeMenu
import com.rvh.video.ui.components.formatDuration
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicVideoGridCard(
    video: LocalVideoEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onRecategorize: (VideoCategory) -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCollection: () -> Unit = {},
    isWatchLater: Boolean = video.isWatchLater,
    onToggleWatchLater: () -> Unit = {},
    onDetails: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Surface1).combinedClickable(onClick = onPlayPauseClick, onLongClick = { menuExpanded = true })
        ) {
            AsyncImage(
                model = remember(video.uri, video.dateModifiedEpochSeconds) {
                    ImageRequest.Builder(context).data(video.uri).videoFrameMillis(1000)
                        .size(360, 210)
                        .memoryCacheKey("rvh-music-grid:${video.uri}:${video.dateModifiedEpochSeconds}")
                        .diskCacheKey("rvh-music-grid:${video.uri}:${video.dateModifiedEpochSeconds}")
                        .crossfade(false).build()
                },
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = if (isCurrent) AccentTeal else Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${formatDuration(video.durationMs)}  •  ${video.folderName.ifBlank { "Music Videos" }}", style = RvhType.Meta, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        RecategorizeMenu(
            expanded = menuExpanded,
            currentCategory = video.effectiveCategory,
            onDismiss = { menuExpanded = false },
            onAddToCollection = onAddToCollection,
            isFavorite = video.isFavorite,
            onToggleFavorite = onToggleFavorite,
            isWatchLater = isWatchLater,
            onToggleWatchLater = onToggleWatchLater,
            onDetails = onDetails,
            onSelect = { onRecategorize(it); menuExpanded = false },
        )
    }
}
