package com.rvh.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.sp
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.RvhGoldSoft
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.glassSurface

/** Compact command surface for infrequent player actions. */
@Composable
fun PlayerCommandPopup(
    speedLabel: String,
    locked: Boolean,
    rotationLabel: String,
    scaleLabel: String,
    onDismiss: () -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onSpeed: () -> Unit,
    onResize: () -> Unit,
    onRotation: () -> Unit,
    onPip: () -> Unit,
    onTracks: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onQueueMode: (() -> Unit)? = null,
    queueModeLabel: String? = null,
    queueMode: String? = null,
    anchor: Offset? = null,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
    ) {
        val popupWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { 300.dp.roundToPx() }
        val popupHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 300.dp.roundToPx() }
        val paddingPx = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.roundToPx() }
        val rawX = anchor?.x?.toInt() ?: ((constraints.maxWidth - popupWidthPx) / 2)
        val rawY = anchor?.y?.toInt()?.minus(popupHeightPx / 2) ?: ((constraints.maxHeight - popupHeightPx) / 2)
        val popupX = rawX.coerceIn(paddingPx, (constraints.maxWidth - popupWidthPx - paddingPx).coerceAtLeast(paddingPx))
        val popupY = rawY.coerceIn(paddingPx, (constraints.maxHeight - popupHeightPx - paddingPx).coerceAtLeast(paddingPx))

        Column(
            modifier = Modifier
                .offset { IntOffset(popupX, popupY) }
                .width(300.dp)
                .glassSurface(
                    shape = RoundedCornerShape(24.dp),
                    tint = Color(0xE6111820),
                    borderColor = RvhGold.copy(alpha = 0.55f),
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "PLAYER COMMANDS",
                style = RvhType.Meta.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                color = RvhGold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                CommandTile(Icons.Filled.Speed, "SPEED", speedLabel, onSpeed)
                CommandTile(Icons.Filled.Lock, "LOCK", if (locked) "UNLOCK" else "TOUCH", if (locked) onUnlock else onLock)
                CommandTile(Icons.Filled.ScreenRotation, "ROTATE", rotationLabel, onRotation)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                CommandTile(Icons.Filled.AspectRatio, "RESIZE", scaleLabel, onResize)
                CommandTile(Icons.Filled.PictureInPicture, "PIP", "FLOAT", onPip)
                if (onTracks != null) CommandTile(Icons.Filled.Subtitles, "TRACKS", "AUDIO", onTracks)
            }
            if (onPrevious != null || onNext != null || onQueueMode != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    if (onPrevious != null) CommandTile(Icons.Filled.SkipPrevious, "PREV", "TRACK", onPrevious)
                    if (onNext != null) CommandTile(Icons.Filled.SkipNext, "NEXT", "TRACK", onNext)
                    if (onQueueMode != null) {
                        val icon = when (queueMode) {
                            "shuffle" -> Icons.Filled.Shuffle
                            "repeat_one" -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        }
                        CommandTile(icon, "QUEUE", queueModeLabel ?: "AUTO", onQueueMode)
                    }
                }
            }
            Text(
                "LONG PRESS TO OPEN  •  TAP OUTSIDE TO DISMISS",
                style = RvhType.Meta.copy(fontSize = 7.sp, letterSpacing = 1.1.sp),
                color = RvhGoldSoft.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CommandTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .size(width = 82.dp, height = 64.dp)
            .glassSurface(
                shape = RoundedCornerShape(15.dp),
                tint = Color(0xB8141B23),
                borderColor = RvhGold.copy(alpha = 0.25f),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = RvhGold, modifier = Modifier.size(19.dp))
        Text(label, style = RvhType.Meta.copy(fontSize = 7.sp, letterSpacing = .8.sp), color = RvhGoldSoft)
        Text(value, style = RvhType.Meta.copy(fontSize = 7.sp), color = Color.White)
    }
}
