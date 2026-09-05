package com.rvh.video.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.rvh.video.player.PlayerManager
import com.rvh.video.ui.theme.GlassBorder

private val MINI_PLAYER_WIDTH = 140.dp
private val MINI_PLAYER_HEIGHT = 79.dp // 16:9 at that width

/**
 * A single ExoPlayer instance can only render to one output surface at a
 * time — attaching it to this small PlayerView automatically detaches it
 * from wherever it was previously (MusicVideoPlayerScreen's full-size
 * PlayerView), which is standard, supported ExoPlayer behavior and is
 * exactly the handoff this needs: playback continues uninterrupted, just
 * redirected to a smaller surface.
 *
 * Draggable so it can be repositioned out of the way of whatever's
 * underneath; starts wherever its parent positions it (bottom-right by
 * default, see the call site in MainActivity).
 */
@Composable
fun FloatingMiniVideoPlayer(
    playerManager: PlayerManager,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }
            .size(MINI_PLAYER_WIDTH, MINI_PLAYER_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    useController = false // deliberately no scrubber here — tap Expand for real controls
                }
            },
            update = { view -> view.player = playerManager.getOrCreate() }
        )

        IconButton(
            onClick = onExpand,
            modifier = Modifier.align(Alignment.Center).size(32.dp)
        ) {
            Icon(Icons.Filled.OpenInFull, contentDescription = "Expand", tint = Color.White)
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(2.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
