package com.rvh.video.ui.movies
import com.rvh.video.ui.theme.glassPill
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import com.rvh.video.player.PlayerManager
import com.rvh.video.player.PipController
import com.rvh.video.RvhVideoApp
import com.rvh.video.ui.components.PlayerErrorOverlay
import com.rvh.video.ui.components.ScaleAdjustSheet
import com.rvh.video.ui.components.VideoGestureOverlay
import com.rvh.video.ui.components.ReimaginedPlayerControls
import com.rvh.video.ui.components.VideoScaleMode
import com.rvh.video.ui.components.nextPlaybackSpeed
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

private enum class MovieOrientationStage { Landscape, Portrait }

/**
 * Movies section requirement: "auto-rotation to full-screen horizontal
 * playback upon video launch." Back navigation is two-stage: the first
 * back press (system or in-app) drops from landscape to portrait but
 * stays on this screen; the second back press exits to the grid.
 */
@Composable
fun MoviePlayerScreen(
    uri: String,
    resumePositionMs: Long,
    onSavePosition: (positionMs: Long) -> Unit,
    onBack: () -> Unit,
    mediaTitle: String = "RVH Media",
    isInPip: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val playerManager = (context.applicationContext as RvhVideoApp).playerManager
    var stage by remember { mutableStateOf(MovieOrientationStage.Landscape) }
    var ignition by remember(uri) { mutableStateOf(true) }
    var pitLane by remember { mutableStateOf(false) }

    var locked by remember { mutableStateOf(false) }
    var rotationLocked by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(playerManager.savedPlaybackSpeed()) }
    var scaleMode by remember { mutableStateOf(VideoScaleMode.ORIGINAL) }
    var showScaleSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    val playbackState by playerManager.state.collectAsState()

    fun handleBack() {
        if (stage == MovieOrientationStage.Landscape) {
            stage = MovieOrientationStage.Portrait
        } else if (!pitLane) {
            pitLane = true
        }
    }

    // Locking the screen also has to disable the back gesture/button, the
    // same way a real video player's lock does — otherwise "lock" wouldn't
    // actually stop accidental touches from navigating away mid-lock.
    BackHandler(enabled = !locked) { handleBack() }

    // Orientation follows `stage` reactively only while rotation is locked
    // to the app's own two-stage model — toggling rotationLocked off hands
    // control back to the device's sensor/auto-rotate setting instead.
    LaunchedEffect(stage, rotationLocked) {
        activity?.requestedOrientation = if (!rotationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else when (stage) {
            MovieOrientationStage.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            MovieOrientationStage.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(playbackSpeed) {
        playerManager.getOrCreate().setPlaybackSpeed(playbackSpeed)
    }

    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onSavePosition(playerManager.currentPositionMs())
                playerManager.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onSavePosition(playerManager.currentPositionMs())
            playerManager.pause()
        }
    }

    LaunchedEffect(uri) {
        ignition = true
        kotlinx.coroutines.delay(900)
        val savedPosition = if (resumePositionMs > 0L) resumePositionMs else playerManager.savedPositionFor(uri)
        playerManager.play(uri, savedPosition)
        ignition = false
    }

    LaunchedEffect(pitLane) {
        if (pitLane) {
            playerManager.pause()
            kotlinx.coroutines.delay(650)
            onBack()
        }
    }

    LaunchedEffect(ignition, playbackState.isPlaying, showControls) {
        if (!ignition && showControls && playbackState.isPlaying) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    controllerAutoShow = false
                    player = playerManager.getOrCreate()
                }
            },
            update = { view ->
                view.useController = false
                view.resizeMode = scaleMode.resizeMode
                view.player = playerManager.getOrCreate()
            }
        )

        if (pitLane) {
            PitLaneOverlay(title = mediaTitle, modifier = Modifier.fillMaxSize())
        } else if (ignition) {
            IgnitionOverlay(
                title = mediaTitle,
                resumePositionMs = resumePositionMs,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!ignition && !locked) {
            VideoGestureOverlay(
                player = playerManager.getOrCreate(),
                onSingleTap = { showControls = !showControls },
                modifier = Modifier.fillMaxSize()
            )
        }

        playbackState.errorMessage?.let { message ->
            PlayerErrorOverlay(
                message = message,
                onRetry = { playerManager.retry() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (!ignition && !isInPip) {
            PlayerTelemetryHud(
                title = mediaTitle,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                speed = playbackState.playbackSpeed,
                isPlaying = playbackState.isPlaying,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 16.dp)
            )
        }

        if (!ignition && (showControls || locked)) {
            ReimaginedPlayerControls(
                title = mediaTitle,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                isPlaying = playbackState.isPlaying,
                playbackSpeed = playbackSpeed,
                locked = locked,
                rotationLocked = rotationLocked,
                onBack = { handleBack() },
                onPlayPause = { if (playbackState.isPlaying) playerManager.pause() else playerManager.resume() },
                onSeekBack = { playerManager.seekTo((playbackState.positionMs - 10_000L).coerceAtLeast(0L)) },
                onSeekForward = { playerManager.seekTo((playbackState.positionMs + 10_000L).coerceAtMost(playbackState.durationMs.coerceAtLeast(0L))) },
                onSeekTo = { playerManager.seekTo(it) },
                onLock = { locked = true; showControls = false },
                onUnlock = { locked = false; showControls = true },
                onSpeed = { playbackSpeed = nextPlaybackSpeed(playbackSpeed) },
                onResize = { showScaleSheet = true },
                onRotation = { rotationLocked = !rotationLocked },
                onPip = { activity?.let { PipController.enterManually(it) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }
    }


    if (showScaleSheet) {
        ScaleAdjustSheet(
            current = scaleMode,
            onSelect = { scaleMode = it },
            onDismiss = { showScaleSheet = false }
        )
    }
}



@Composable
private fun PlayerTelemetryHud(
    title: String,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier
            .glassPill()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPlaying) "OVERDRIVE" else "PAUSED",
                style = RvhType.Meta,
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${if (speed % 1f == 0f) speed.toInt() else speed}x",
                style = RvhType.Meta,
                color = Color.White,
            )
        }
        Text(
            text = title.ifBlank { "RVH Media" }.take(34),
            style = RvhType.Meta,
            color = TextSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatPlayerDuration(positionMs), style = RvhType.Meta, color = Color.White)
            Spacer(Modifier.width(7.dp))
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 3.dp)
                    .background(TextSecondary.copy(alpha = 0.28f), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentTeal, RoundedCornerShape(50))
                        .then(Modifier)
                )
            }
            Spacer(Modifier.width(7.dp))
            Text(formatPlayerDuration(durationMs), style = RvhType.Meta, color = TextSecondary)
        }
    }
}


@Composable
private fun PitLaneOverlay(title: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pit-lane")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pit-pulse"
    )
    Box(modifier.background(Color.Black.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(96.dp).scale(pulse).glassSurface(shape = RoundedCornerShape(50), blurRadius = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("RVH", style = RvhType.ScreenTitle, color = AccentTeal, fontWeight = FontWeight.Bold)
            }
            Text("PIT LANE", style = RvhType.ScreenTitle, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Securing your session", style = RvhType.Body, color = TextSecondary)
            Text(title, style = RvhType.Meta, color = AccentTeal)
        }
    }
}

@Composable
private fun IgnitionOverlay(
    title: String,
    resumePositionMs: Long,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "ignition")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse"
    )
    Box(
        modifier = modifier.background(Surface0),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .scale(pulse)
                    .glassSurface(shape = RoundedCornerShape(50), blurRadius = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(38.dp))
            }
            Text("LAUNCH CONTROL  •  IGNITION", style = RvhType.Meta, color = AccentTeal, letterSpacing = 2.sp)
            Text(
                title.ifBlank { "RVH Media" }.take(52),
                style = RvhType.CardTitle,
                color = Color.White,
            )
            if (resumePositionMs > 0L) {
                Text(
                    "RESUMING FROM ${formatPlayerDuration(resumePositionMs)}",
                    style = RvhType.Meta,
                    color = TextSecondary,
                )
            } else {
                Text("PREPARING PLAYBACK", style = RvhType.Meta, color = TextSecondary)
            }
        }
    }
}

private fun formatPlayerDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val minutes = total / 60
    val seconds = total % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        "%d:%02d:%02d".format(hours, minutes % 60, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
