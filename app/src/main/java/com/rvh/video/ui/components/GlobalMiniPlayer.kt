package com.rvh.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.player.PlayerManager
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface
import kotlinx.coroutines.delay

/**
 * One global media control surface for every section of RVH.
 * The player itself is application-scoped; this UI simply reflects its state.
 */
@Composable
fun GlobalMiniPlayer(
    media: LocalVideoEntity,
    playerManager: PlayerManager,
    isMusic: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback by playerManager.state.collectAsState()
    var progress by remember(media.uri) { mutableFloatStateOf(0f) }

    LaunchedEffect(media.uri, playback.isPlaying, playback.positionMs, playback.durationMs) {
        while (true) {
            val duration = playback.durationMs
            val position = playerManager.currentPositionMs()
            progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            if (!playback.isPlaying) break
            delay(400)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(18.dp), blurRadius = 20.dp)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = media.uri,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = media.displayName.substringBeforeLast('.'),
                    style = RvhType.CardTitle,
                    color = Color(0xFFE8D9B9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isMusic) "Music video" else "Movie",
                    style = RvhType.Meta,
                    color = TextSecondary,
                )
            }
            IconButton(onClick = onExpand) {
                Icon(Icons.Filled.Fullscreen, contentDescription = "Open player", tint = Color(0xFFE0B35A))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            color = Color(0xFFE0B35A),
            trackColor = Color(0xFF8F887C).copy(alpha = 0.18f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isMusic) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color(0xFFE0B35A))
                }
            }
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFE0B35A), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                    )
                }
            }
            if (isMusic) {
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color(0xFFE0B35A))
                }
            }
        }
    }
}

