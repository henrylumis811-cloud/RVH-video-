package com.rvh.video.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

@Composable
fun FolderScreen(
    folder: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenMedia: (LocalVideoEntity) -> Unit,
) {
    val videos by viewModel.observeFolder(folder).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().background(Color.Transparent)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(folder, style = RvhType.ScreenTitle, color = Color.White, maxLines = 1)
                Text("${videos.size} ${if (videos.size == 1) "video" else "videos"}", style = RvhType.Meta, color = TextSecondary)
            }
            Icon(Icons.Filled.FolderCopy, null, tint = AccentTeal, modifier = Modifier.size(26.dp))
        }

        if (videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FolderCopy, null, tint = AccentTeal, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No videos found", style = RvhType.CardTitle, color = Color.White)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos, key = { video: LocalVideoEntity -> video.uri }) { video ->
                    FolderVideoRow(video = video, onClick = { onOpenMedia(video) })
                }
            }
        }
    }
}

@Composable
private fun FolderVideoRow(video: LocalVideoEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(18.dp), blurRadius = 14.dp)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(video.uri).videoFrameMillis(1000).crossfade(true).build(),
            contentDescription = video.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(120.dp).height(76.dp).clip(RoundedCornerShape(12.dp))
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 2)
            Text(
                when (video.effectiveCategory.name) {
                    "MUSIC_VIDEO" -> "Music video"
                    "SHORT" -> "Short"
                    else -> "Movie"
                },
                style = RvhType.Meta,
                color = TextSecondary
            )
        }
        Icon(Icons.Filled.PlayArrow, "Open", tint = AccentTeal, modifier = Modifier.size(30.dp))
    }
}
