package com.rvh.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.RvhGoldSoft
import com.rvh.video.ui.theme.PlayerControlBlack
import com.rvh.video.ui.theme.PlayerControlBlackRaised
import com.rvh.video.ui.theme.glassSurface

// RVH player controls use the same warm gold language as gestures and telemetry.
// The control surfaces stay black and translucent so the video remains visible.
private val ControlAccent = RvhGold
private val ControlAccentSoft = RvhGoldSoft
private val ControlText = Color(0xFFF1F5F9)
private val ControlMuted = Color(0xFF94A3B8)
private val ControlPanel = PlayerControlBlack
private val ControlPanelRaised = PlayerControlBlackRaised
private val ControlEdge = RvhGold.copy(alpha = 0.30f)

enum class RotationMode { AUTO, PORTRAIT, LANDSCAPE }

@Composable
fun ReimaginedPlayerControls(
    title: String,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    locked: Boolean,
    rotationMode: RotationMode,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onSpeed: () -> Unit,
    onResize: () -> Unit,
    scaleModeLabel: String = "FIT",
    onRotation: () -> Unit,
    onInteraction: () -> Unit = {},
    onPip: () -> Unit = {},
    onTracks: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onQueueMode: (() -> Unit)? = null,
    queueModeLabel: String? = null,
    queueMode: String? = null,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var scrubFraction by remember { mutableFloatStateOf(progress) }
    var isScrubbing by remember { androidx.compose.runtime.mutableStateOf(false) }
    val displayedProgress = if (isScrubbing) scrubFraction else progress
    val speedLabel = if (playbackSpeed % 1f == 0f) "${playbackSpeed.toInt()}×" else "${playbackSpeed}×"

    if (locked) {
        IconButton(
            onClick = { onInteraction(); onUnlock() },
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .glassSurface(
                shape = RoundedCornerShape(26.dp),
                tint = ControlPanel,
                borderColor = ControlEdge,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header is intentionally minimal: only navigation, title and true PIP action.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onInteraction(); onBack() }, modifier = Modifier.size(40.dp)) {
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
            CommandIcon(Icons.Filled.PictureInPicture, "Picture in picture", { onInteraction(); onPip() })
            Text(speedLabel, style = RvhType.Meta, color = ControlAccent, modifier = Modifier.padding(end = 4.dp))
        }

        Slider(
            value = displayedProgress.coerceIn(0f, 1f),
            onValueChange = { fraction ->
                onInteraction()
                isScrubbing = true
                scrubFraction = fraction
            },
            onValueChangeFinished = {
                onInteraction()
                if (durationMs > 0L) onSeekTo((durationMs * scrubFraction).toLong())
                isScrubbing = false
            },
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
            if (onPrevious != null) ControlButtonContent(Icons.Filled.SkipPrevious, "Previous", { onInteraction(); onPrevious() })
            ControlButtonContent(Icons.Filled.Replay10, "Back 10 seconds", { onInteraction(); onSeekBack() })
            IconButton(
                onClick = { onInteraction(); onPlayPause() },
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
            ControlButtonContent(Icons.Filled.Forward10, "Forward 10 seconds", { onInteraction(); onSeekForward() })
            if (onNext != null) ControlButtonContent(Icons.Filled.SkipNext, "Next", { onInteraction(); onNext() })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniControl(Icons.Filled.Speed, "SPEED", speedLabel, { onInteraction(); onSpeed() })
            MiniControl(Icons.Filled.Lock, "LOCK", "CONTROLS", { onInteraction(); onLock() })
            MiniControl(
                when (rotationMode) {
                    RotationMode.PORTRAIT -> Icons.Filled.ScreenRotation
                    RotationMode.LANDSCAPE -> Icons.Filled.ScreenRotation
                    RotationMode.AUTO -> Icons.Filled.ScreenRotation
                },
                "ROTATE",
                rotationMode.name,
                { onInteraction(); onRotation() },
            )
            MiniControl(Icons.Filled.AspectRatio, "RESIZE", scaleModeLabel.uppercase(), { onInteraction(); onResize() })
            MiniControl(Icons.Filled.PictureInPicture, "PIP", "FLOAT", { onInteraction(); onPip() })
            if (onTracks != null) MiniControl(Icons.Filled.Subtitles, "TRACKS", "AUDIO / TEXT", { onInteraction(); onTracks() })
            if (onQueueMode != null) {
                MiniControl(
                    when (queueMode) {
                        "shuffle" -> Icons.Filled.Shuffle
                        "repeat_one" -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    },
                    "QUEUE",
                    queueModeLabel ?: "AUTO",
                    { onInteraction(); onQueueMode() },
                )
            }
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
            .width(78.dp)
            .clickable(onClick = onClick)
            .glassSurface(
                shape = RoundedCornerShape(16.dp),
                tint = Color(0xB8111820),
                borderColor = ControlEdge,
            )
            .padding(horizontal = 5.dp, vertical = 7.dp),
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
