package com.rvh.video.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.glassPill

/**
 * Speed / Lock / Scale adjust / Rotation-lock, matching the reference
 * player row. Hidden entirely by the caller while `locked` is true —
 * only the separate unlock button (below) stays visible then, mirroring
 * how a real screen-lock hides every other control.
 */
@Composable
fun PlayerTopControls(
    playbackSpeed: Float,
    onSpeedClick: () -> Unit,
    onLockClick: () -> Unit,
    onScaleAdjustClick: () -> Unit,
    rotationLocked: Boolean,
    onRotationToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.glassPill(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSpeedClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = "Playback speed", tint = Color(0xFFE0B35A), modifier = Modifier.size(20.dp))
                Text(
                    text = "${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt().toString() else playbackSpeed.toString()}x",
                    style = RvhType.Meta,
                    color = Color(0xFFE8D9B9),
                    modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                )
            }
        }
        IconButton(onClick = onLockClick) {
            Icon(Icons.Filled.LockOpen, contentDescription = "Lock controls", tint = Color(0xFFE0B35A))
        }
        IconButton(onClick = onScaleAdjustClick) {
            Icon(Icons.Filled.AspectRatio, contentDescription = "Scale adjust", tint = Color(0xFFE0B35A))
        }
        IconButton(onClick = onRotationToggle) {
            Icon(
                imageVector = if (rotationLocked) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                contentDescription = if (rotationLocked) "Rotation locked" else "Rotation follows device",
                tint = Color(0xFFE0B35A)
            )
        }
    }
}

/** Shown by itself, replacing every other control, whenever the player is locked. */
@Composable
fun PlayerUnlockButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp).glassPill()
    ) {
        Icon(Icons.Filled.Lock, contentDescription = "Unlock controls", tint = Color(0xFFE0B35A))
    }
}
