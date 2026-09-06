package com.rvh.video.ui.components
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.rvh.video.ui.theme.RvhType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Double-tap the left/right half to seek -10s/+10s. Vertical swipe on the
 * right half adjusts system media volume (delegated to AudioManager with
 * FLAG_SHOW_UI, so Android's own volume overlay handles the visual — no
 * reason to build a custom one when the system already provides a
 * reliable, familiar one). Vertical swipe on the left half adjusts screen
 * brightness (no system overlay equivalent exists for that, so this shows
 * a small custom percentage flash instead).
 *
 * IMPORTANT interaction note: this overlay sits ON TOP of the AndroidView
 * PlayerView in z-order, which means it would swallow single taps that
 * are meant to toggle the PlayerView's own controller visibility unless
 * explicitly forwarded. `onSingleTap` exists for exactly that — callers
 * must wire it to toggle the PlayerView's controller themselves. This is
 * the trickiest part of the whole feature and genuinely needs on-device
 * verification: Compose-over-AndroidView touch arbitration isn't
 * something that can be fully confirmed without an actual touchscreen.
 */
@Composable
fun VideoGestureOverlay(
    player: Player?,
    onSingleTap: () -> Unit,
    onDoubleTapPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var seekFlashText by remember { mutableStateOf<String?>(null) }
    var seekDirection by remember { mutableStateOf(0) }
    var brightnessFlashPercent by remember { mutableStateOf<Int?>(null) }
    var volumeFlashPercent by remember { mutableStateOf<Int?>(null) }
    var brightnessDragAccumulator by remember { mutableFloatStateOf(0f) }
    var volumeDragAccumulator by remember { mutableFloatStateOf(0f) }
    var brightnessDragStart by remember { mutableFloatStateOf(0.5f) }
    var volumeDragStart by remember { mutableFloatStateOf(0f) }
    var brightnessGestureSide by remember { mutableStateOf<Boolean?>(null) }

    DisposableEffect(activity) {
        val initialBrightness = activity?.window?.attributes?.screenBrightness
        onDispose {
            if (activity != null && initialBrightness != null) {
                val attrs = activity.window.attributes
                attrs.screenBrightness = initialBrightness
                activity.window.attributes = attrs
            }
        }
    }

    LaunchedEffect(seekFlashText) {
        if (seekFlashText != null) {
            delay(500)
            seekFlashText = null
        }
    }
    LaunchedEffect(brightnessFlashPercent, volumeFlashPercent) {
        if (brightnessFlashPercent != null || volumeFlashPercent != null) {
            delay(650)
            brightnessFlashPercent = null
            volumeFlashPercent = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player) {
                detectDoubleTapSeek(
                    onSeek = { deltaMs ->
                        player?.let {
                            val target = (it.currentPosition + deltaMs).coerceIn(0L, it.duration.coerceAtLeast(0L))
                            it.seekTo(target)
                        }
                        seekDirection = if (deltaMs > 0) 1 else -1
                        seekFlashText = if (deltaMs > 0) "+10s" else "-10s"
                    },
                    onDoubleTapPlayPause = onDoubleTapPlayPause,
                    onSingleTap = onSingleTap,
                )
            }
    ) {
        // One coordinated vertical detector avoids pointer arbitration between
        // two full-screen gesture layers. The side is captured at drag start.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            brightnessGestureSide = offset.x <= size.width / 2f
                            if (brightnessGestureSide == true) {
                                val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                brightnessDragAccumulator = 0f
                                brightnessDragStart = if (windowBrightness >= 0f) {
                                    windowBrightness
                                } else {
                                    val system = runCatching {
                                        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                                    }.getOrDefault(128)
                                    (system / 255f).coerceIn(0.01f, 1f)
                                }
                            } else {
                                volumeDragAccumulator = 0f
                                volumeDragStart = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            }
                        },
                        onDragCancel = { brightnessGestureSide = null },
                        onDragEnd = { brightnessGestureSide = null },
                    ) { change, dragAmount ->
                        if (brightnessGestureSide == true) {
                            change.consume()
                            brightnessDragAccumulator -= dragAmount
                            val activityWindow = activity?.window ?: return@detectVerticalDragGestures
                            val delta = brightnessDragAccumulator / size.height.toFloat()
                            val newBrightness = (brightnessDragStart + delta).coerceIn(0.01f, 1f)
                            val attrs = activityWindow.attributes
                            attrs.screenBrightness = newBrightness
                            activityWindow.attributes = attrs
                            brightnessDragAccumulator = 0f
                            brightnessFlashPercent = (newBrightness * 100f).roundToInt()
                        } else {
                            change.consume()
                            volumeDragAccumulator -= dragAmount
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val deltaSteps = (volumeDragAccumulator / size.height.toFloat()) * maxVolume
                            val newVolume = (volumeDragStart + deltaSteps).roundToInt().coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                            volumeFlashPercent = ((newVolume.toFloat() / maxVolume) * 100f).roundToInt()
                        }
                    }
                }
        )

        seekFlashText?.let { text ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                SeekFlash(direction = seekDirection, text = text)
            }
        }

        brightnessFlashPercent?.let { percent ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            ) {
                FlashLabel("$percent%")
            }
        }

        volumeFlashPercent?.let { percent ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
            ) {
                FlashLabel("VOL  $percent%")
            }
        }
    }
}


private val ControlAccent = Color(0xFFE0B35A)
private val ControlPanel = Color(0xE20A0B0D)

@Composable
private fun SeekFlash(direction: Int, text: String) {
    Box(
        modifier = Modifier
            .background(ControlPanel, RoundedCornerShape(18.dp))
            .then(Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (direction > 0) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                contentDescription = null,
                tint = ControlAccent,
            )
            Text(text, style = RvhType.CardTitle, color = ControlAccent, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun FlashLabel(text: String) {
    Box(
        modifier = Modifier
            .background(ControlPanel, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, style = RvhType.CardTitle, color = ControlAccent)
    }
}

/**
 * Custom double-tap detector using Foundation's own detectTapGestures,
 * which already disambiguates single vs. double tap with standard timing
 * — this just adds the left/right-half branching on top of it.
 */
private suspend fun PointerInputScope.detectDoubleTapSeek(
    onSeek: (deltaMs: Long) -> Unit,
    onDoubleTapPlayPause: () -> Unit,
    onSingleTap: () -> Unit,
) {
    detectTapGestures(
        onTap = { onSingleTap() },
        onDoubleTap = { offset ->
            val third = size.width / 3f
            when {
                offset.x < third -> onSeek(-10_000L)
                offset.x > third * 2f -> onSeek(10_000L)
                else -> onDoubleTapPlayPause()
            }
        }
    )
}
