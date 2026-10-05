package com.rvh.video.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.components.rememberRvhThumbnailModel
import com.rvh.video.ui.components.RecategorizeMenu
import com.rvh.video.ui.components.formatDuration
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieListCard(
    movie: LocalVideoEntity,
    onClick: () -> Unit,
    onRecategorize: (VideoCategory) -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCollection: () -> Unit = {},
    isWatchLater: Boolean = movie.isWatchLater,
    onToggleWatchLater: () -> Unit = {},
    onDetails: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val thumbnailModel = rememberRvhThumbnailModel(movie.uri, movie.dateModifiedEpochSeconds, 320, 200)
    val imageRequest = remember(movie.uri, movie.dateModifiedEpochSeconds, thumbnailModel) {
        ImageRequest.Builder(context).data(thumbnailModel)
            .size(320, 200)
            .memoryCacheKey("rvh-list:${movie.uri}:${movie.dateModifiedEpochSeconds}:canonical")
            .diskCacheKey("rvh-list:${movie.uri}:${movie.dateModifiedEpochSeconds}:canonical")
            .crossfade(false).build()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.width(128.dp).height(82.dp).clip(RoundedCornerShape(11.dp)).background(Surface1),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = movie.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(82.dp),
            )
            if (movie.durationMs > 0L && movie.resumePositionMs > 0L) {
                val progress = (movie.resumePositionMs.toFloat() / movie.durationMs).coerceIn(0f, 1f)
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(3.dp).background(RvhGold))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(movie.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${formatDuration(movie.durationMs)}  •  ${movie.folderName.ifBlank { "Movies" }}", style = RvhType.Meta, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RecategorizeMenu(
            expanded = menuExpanded,
            currentCategory = movie.effectiveCategory,
            onDismiss = { menuExpanded = false },
            onAddToCollection = { menuExpanded = false; onAddToCollection() },
            isFavorite = movie.isFavorite,
            onToggleFavorite = { onToggleFavorite() },
            isWatchLater = isWatchLater,
            onToggleWatchLater = onToggleWatchLater,
            onDetails = onDetails,
            onSelect = { onRecategorize(it); menuExpanded = false },
        )
    }
}
