package com.rvh.video.ui.music

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicVideoRow(
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onPlayPauseClick, onLongClick = { menuExpanded = true })
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(48.dp) // fixed compact thumbnail, matches the mockup's list rows
                .clip(RoundedCornerShape(8.dp))
                .background(Surface1)
        ) {
            val thumbnailModel = rememberRvhThumbnailModel(video.uri, video.dateModifiedEpochSeconds, 320, 180)
            val request = remember(video.uri, video.dateModifiedEpochSeconds, thumbnailModel) {
                ImageRequest.Builder(context)
                    .data(thumbnailModel)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = video.displayName.substringBeforeLast('.'),
                style = RvhType.CardTitle,
                color = if (isCurrent) AccentTeal else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = video.folderName.ifBlank { "Music Videos" },
                style = RvhType.Meta,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }


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
            onSelect = { onRecategorize(it); menuExpanded = false }
        )
    }
}
