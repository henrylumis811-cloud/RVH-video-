package com.rvh.video.ui.details

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
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
) {
    if (video == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Media not found", style = RvhType.Body, color = TextSecondary)
        }
        return
    }

    val context = LocalContext.current
    var launching by remember(video.uri) { mutableStateOf(false) }

    LaunchedEffect(launching) {
        if (launching) {
            kotlinx.coroutines.delay(550)
            onPlay()
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("Details", style = RvhType.CardTitle, color = Color.White)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(video.uri).videoFrameMillis(1000).crossfade(true).build(),
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(if (video.effectiveCategory == VideoCategory.SHORT) 9f / 16f else 16f / 9f)
                    .glassSurface(shape = RoundedCornerShape(18.dp), blurRadius = 12.dp)
            )
            Text(video.displayName.substringBeforeLast('.'), style = RvhType.ScreenTitle, color = Color.White)
            Text(categoryLabel(video.effectiveCategory), style = RvhType.Meta, color = AccentTeal)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { if (!launching) launching = true },
                    modifier = Modifier.weight(1f)
                ) {
                    if (launching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.padding(3.dp))
                        Text("Launch Control")
                    } else {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.padding(3.dp))
                        Text(if (video.resumePositionMs > 0L) "Resume" else "Play")
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.background(Surface1, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Filled.Favorite, "Favorite", tint = if (video.isFavorite) AccentTeal else TextSecondary)
                }
                IconButton(onClick = onToggleWatchLater, modifier = Modifier.background(Surface1, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Filled.Schedule, "Watch Later", tint = if (video.isWatchLater) AccentTeal else TextSecondary)
                }
            }

            Column(Modifier.fillMaxWidth().glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 10.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
