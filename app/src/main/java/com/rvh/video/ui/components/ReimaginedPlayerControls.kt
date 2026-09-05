package com.rvh.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.glassSurface

// RVH control language: warm champagne/metal against the black automotive scene.
// Deliberately no pure white and no green/teal control chrome.
private val ControlAccent = Color(0xFFE0B35A)
private val ControlAccentSoft = Color(0xFFB88D45)
private val ControlText = Color(0xFFE8D9B9)
private val ControlMuted = Color(0xFF8F887C)
private val ControlPanel = Color(0xE20A0B0D)
private val ControlPanelRaised = Color(0xD9161411)
private val ControlEdge = Color(0x805B5140)

@Composable
fun ReimaginedPlayerControls(
    title: String,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    locked: Boolean,
    rotationLocked: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onSpeed: () -> Unit,
    onResize: () -> Unit,
    onRotation: () -> Unit,
    onPip: () -> Unit = {},
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val speedLabel = if (playbackSpeed % 1f == 0f) "${playbackSpeed.toInt()}×" else "${playbackSpeed}×"

    if (locked) {
        IconButton(
            onClick = onUnlock,
            modifier = modifier.size(50.dp).glassSurface(
                shape = RoundedCornerShape(16.dp),
                tint = ControlPanel,
                borderColor = ControlEdge,
            ),
        ) {
            Icon(Icons.Filled.Lock, "Unlock controls", tint = ControlAccent, modifier = Modifier.size(23.dp))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .glassSurface(
                shape = RoundedCornerShape(26.dp),
                tint = ControlPanel,
                borderColor = ControlEdge,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header is intentionally minimal: only navigation, title and true PIP action.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ControlAccent, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title.ifBlank { "RVH MEDIA" }.uppercase(),
                    style = RvhType.CardTitle,
                    color = ControlText,
                    maxLines = 1,
                )
                Text(
                    "${formatPlayerTime(positionMs)}  /  ${formatPlayerTime(durationMs)}",
                    style = RvhType.Meta,
                    color = ControlMuted,
                    maxLines = 1,
                )
            }
            CommandIcon(Icons.Filled.PictureInPicture, "Picture in picture", onPip)
            Text(speedLabel, style = RvhType.Meta, color = ControlAccent, modifier = Modifier.padding(end = 4.dp))
        }

        Slider(
            value = progress,
            onValueChange = { fraction -> if (durationMs > 0L) onSeekTo((durationMs * fraction).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = ControlAccent,
                activeTrackColor = ControlAccent,
                inactiveTrackColor = ControlMuted.copy(alpha = 0.22f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onPrevious != null) ControlButtonContent(Icons.Filled.SkipPrevious, "Previous", onPrevious)
            ControlButtonContent(Icons.Filled.Replay10, "Back 10 seconds", onSeekBack)
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(66.dp)
                    .glassSurface(
                        shape = RoundedCornerShape(21.dp),
                        tint = ControlPanelRaised,
                        borderColor = ControlAccent.copy(alpha = 0.85f),
                    ),
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    tint = ControlAccent,
                    modifier = Modifier.size(35.dp),
                )
            }
            ControlButtonContent(Icons.Filled.Forward10, "Forward 10 seconds", onSeekForward)
            if (onNext != null) ControlButtonContent(Icons.Filled.SkipNext, "Next", onNext)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniControl(Icons.Filled.Speed, "SPEED", speedLabel, onSpeed, Modifier.weight(1f))
            MiniControl(Icons.Filled.Lock, "LOCK", "CONTROLS", onLock, Modifier.weight(1f))
            MiniControl(
                if (rotationLocked) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                "ROTATE",
                if (rotationLocked) "AUTO" else "FREE",
                onRotation,
                Modifier.weight(1f),
            )
            MiniControl(Icons.Filled.AspectRatio, "RESIZE", "FIT", onResize, Modifier.weight(1f))
            MiniControl(Icons.Filled.PictureInPicture, "PIP", "FLOAT", onPip, Modifier.weight(1f))
        }

        Text(
            if (isPlaying) "AUTO-HIDE  •  TAP VIDEO TO RECALL" else "PAUSED  •  TAP PLAY TO RESUME",
            style = RvhType.Meta.copy(fontSize = 8.sp, letterSpacing = 1.15.sp),
            color = ControlMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun CommandIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, description, tint = ControlAccent, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ControlButtonContent(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(50.dp)) {
        Icon(icon, description, tint = ControlAccentSoft, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun MiniControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .glassSurface(
                shape = RoundedCornerShape(14.dp),
                tint = Color(0x99111112),
                borderColor = ControlEdge,
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Icon(icon, null, tint = ControlAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(2.dp))
        Text(label, style = RvhType.Meta.copy(fontSize = 8.sp, letterSpacing = .8.sp), color = ControlMuted, maxLines = 1)
        Text(value, style = RvhType.Meta.copy(fontSize = 8.sp), color = ControlText, maxLines = 1)
    }
}

private fun formatPlayerTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
