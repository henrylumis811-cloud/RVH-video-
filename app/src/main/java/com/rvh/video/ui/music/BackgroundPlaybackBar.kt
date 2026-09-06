package com.rvh.video.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.player.PlayerManager
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface
import kotlinx.coroutines.delay

@Composable
fun BackgroundPlaybackBar(
    track: LocalVideoEntity,
    isPlaying: Boolean,
    playerManager: PlayerManager,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onEnterPip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember(track.uri) { mutableFloatStateOf(0f) }

    // Polled rather than event-driven — Media3 has no lightweight "position
    // changed" callback, and polling at 500ms is imperceptible for a
    // progress bar while costing negligible battery.
    LaunchedEffect(track.uri, isPlaying) {
        while (isPlaying) {
            val duration = playerManager.getOrCreate().duration.coerceAtLeast(1L)
            progress = (playerManager.currentPositionMs().toFloat() / duration).coerceIn(0f, 1f)
            delay(500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 20.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.displayName.substringBeforeLast('.'),
                    style = RvhType.CardTitle,
                    color = Color(0xFFE8D9B9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Background playback",
                    style = RvhType.Meta,
                    color = TextSecondary,
                )
            }

            IconButton(onClick = onEnterPip) {
                Icon(Icons.Filled.PictureInPicture, contentDescription = "Picture in picture", tint = Color(0xFFE0B35A))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = Color(0xFFE0B35A),
            trackColor = Color(0xFF8F887C).copy(alpha = 0.18f),
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color(0xFFE0B35A), modifier = Modifier.size(28.dp))
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(44.dp)
                    .background(Color(0xFFE0B35A), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color(0xFFE0B35A), modifier = Modifier.size(28.dp))
            }
        }
    }
}
