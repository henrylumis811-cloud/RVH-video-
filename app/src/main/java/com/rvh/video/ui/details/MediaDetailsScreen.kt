package com.rvh.video.ui.details
import com.rvh.video.ui.components.rememberRvhThumbnailModel

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.theme.PlayerControlBlack
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface
import java.time.Instant
import java.time.ZoneId

@Composable
fun MediaDetailsScreen(
    video: LocalVideoEntity?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onMoveTo: (VideoCategory) -> Unit,
    onResetCategory: () -> Unit,
) {
    if (video == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Media not found", style = RvhType.Body, color = TextSecondary)
        }
        return
    }

    val context = LocalContext.current
    var launching by remember(video.uri) { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(launching) {
        if (launching) {
            kotlinx.coroutines.delay(350)
            onPlay()
            launching = false
        }
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = PlayerControlBlack,
            title = { Text("Move media", color = Color.White, style = RvhType.CardTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose the library category for this file.", color = TextSecondary, style = RvhType.Body)
                    listOf(
                        VideoCategory.MOVIE to "Movies",
                        VideoCategory.MUSIC_VIDEO to "Music Videos",
                        VideoCategory.SHORT to "Shorts",
                    ).forEach { (category, label) ->
                        TextButton(onClick = { onMoveTo(category); showCategoryDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(label, color = if (video.effectiveCategory == category) RvhGold else Color.White)
                        }
                    }
                    if (video.userOverrideCategory != null) {
                        TextButton(onClick = { onResetCategory(); showCategoryDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Use automatic category", color = RvhGold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Close", color = RvhGold) } },
        )
    }

    fun share() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(video.uri))
            putExtra(Intent.EXTRA_TITLE, video.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share video"))
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("Details", style = RvhType.CardTitle, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = ::share) { Icon(Icons.Filled.IosShare, "Share", tint = RvhGold) }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val thumbnailModel = rememberRvhThumbnailModel(video.uri, video.dateModifiedEpochSeconds, 720, 405)
    val imageRequest = remember(video.uri, video.dateModifiedEpochSeconds, thumbnailModel) {
        ImageRequest.Builder(context)
            .data(thumbnailModel)
            .size(720, 405)
            .crossfade(false)
            .memoryCacheKey("rvh-details:${video.uri}:${video.dateModifiedEpochSeconds}:canonical")
            .diskCacheKey("rvh-details:${video.uri}:${video.dateModifiedEpochSeconds}:canonical")
            .build()
    }
            AsyncImage(
                model = imageRequest,
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(if (video.effectiveCategory == VideoCategory.SHORT) 9f / 16f else 16f / 9f)
                    .glassSurface(shape = RoundedCornerShape(18.dp), blurRadius = 12.dp)
            )

            Text(video.displayName.substringBeforeLast('.'), style = RvhType.ScreenTitle, color = Color.White)
            Text(categoryLabel(video.effectiveCategory), style = RvhType.Meta, color = RvhGold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { if (!launching) launching = true }, modifier = Modifier.weight(1f)) {
                    if (launching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.padding(3.dp))
                        Text("Opening")
                    } else {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.padding(3.dp))
                        Text(if (video.resumePositionMs > 0L) "Resume" else "Play")
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.background(Surface1, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Filled.Favorite, "Favorite", tint = if (video.isFavorite) RvhGold else TextSecondary)
                }
                IconButton(onClick = onToggleWatchLater, modifier = Modifier.background(Surface1, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Filled.Schedule, "Watch Later", tint = if (video.isWatchLater) RvhGold else TextSecondary)
                }
                IconButton(onClick = { showCategoryDialog = true }, modifier = Modifier.background(Surface1, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Filled.Category, "Move category", tint = RvhGold)
                }
            }

            Column(
                Modifier.fillMaxWidth().glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 10.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DetailRow("Duration", formatDuration(video.durationMs))
                DetailRow("Resolution", if (video.widthPx > 0 && video.heightPx > 0) "${video.widthPx} × ${video.heightPx}" else "Unknown")
                DetailRow("Folder", video.folderName.ifBlank { "Library" })
                DetailRow("Added", yearFromEpochSeconds(video.dateModifiedEpochSeconds))
                if (video.resumePositionMs > 0) DetailRow("Resume", "${formatDuration(video.resumePositionMs)} of ${formatDuration(video.durationMs)}")
            }
        }
    }
}

@Composable private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = RvhType.Meta, color = TextSecondary)
        Text(value, style = RvhType.Body, color = Color.White)
    }
}

private fun categoryLabel(category: VideoCategory) = when (category) {
    VideoCategory.MOVIE -> "Movie"
    VideoCategory.MUSIC_VIDEO -> "Music Video"
    VideoCategory.SHORT -> "Short"
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun yearFromEpochSeconds(epochSeconds: Long): String = Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).year.toString()
