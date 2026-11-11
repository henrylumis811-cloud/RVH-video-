package com.rvh.video.ui.movies
import com.rvh.video.ui.theme.glassPill
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.graphics.Rect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
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
import com.rvh.video.player.PlaybackClient
import com.rvh.video.player.PipController
import com.rvh.video.RvhVideoApp
import com.rvh.video.ui.components.PlayerErrorOverlay
import com.rvh.video.ui.components.PlayerCommandPopup
import com.rvh.video.ui.components.ScaleAdjustSheet
import com.rvh.video.ui.components.VideoGestureOverlay
import com.rvh.video.ui.components.RotationMode
import com.rvh.video.ui.components.VideoScaleFrame
import com.rvh.video.ui.components.VideoScaleMode
import com.rvh.video.ui.components.TrackSelectionSheet
import com.rvh.video.ui.components.nextPlaybackSpeed
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.PlayerControlBlack
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

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

    // Full-screen playback owns the display: keep the panel awake and hide
    // Android's clock/battery/status chrome while the player is on screen.
    DisposableEffect(activity) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(activity) {
        onDispose {
            activity?.let { PipController.configureAutoEnter(it, enabled = false) }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackClient = (context.applicationContext as RvhVideoApp).playbackClient
    var ignition by remember(uri) { mutableStateOf(true) }
    var pitLane by remember { mutableStateOf(false) }
    var videoBounds by remember { mutableStateOf<Rect?>(null) }

    var locked by remember { mutableStateOf(false) }
    var rotationMode by remember { mutableStateOf(RotationMode.AUTO) }
    var playbackSpeed by remember { mutableStateOf(playbackClient.savedPlaybackSpeed()) }
    var scaleMode by remember { mutableStateOf(VideoScaleMode.ORIGINAL) }
    var showScaleSheet by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var showCommandPopup by remember { mutableStateOf(false) }
    var commandPopupAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var showTelemetry by remember { mutableStateOf(true) }
    var telemetryInteractionToken by remember { mutableStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    val playbackState by playbackClient.state.collectAsStateWithLifecycle()

    fun handleBack() {
        if (rotationMode == RotationMode.LANDSCAPE) {
            rotationMode = RotationMode.PORTRAIT
        } else if (!pitLane) {
            pitLane = true
        }
    }

    // Locking the screen also has to disable the back gesture/button, the
    // same way a real video player's lock does — otherwise "lock" wouldn't
    // actually stop accidental touches from navigating away mid-lock.
    BackHandler(enabled = !locked) { handleBack() }

    LaunchedEffect(rotationMode) {
        activity?.requestedOrientation = when (rotationMode) {
            RotationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            RotationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            RotationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    LaunchedEffect(Unit) {
        val enabled = com.rvh.video.data.local.AppSettings(context).autoFloatingPlayEnabled
        activity?.let { PipController.configureAutoEnter(it, enabled = enabled) }
    }

    LaunchedEffect(videoBounds) {
        val bounds = videoBounds ?: return@LaunchedEffect
        activity?.let { PipController.updateSourceRectHint(it, bounds) }
    }

    LaunchedEffect(playbackSpeed) {
        playbackClient.setPlaybackSpeed(playbackSpeed)
    }

    // Capture the host orientation before RVH starts controlling it. Keying this
    // effect to the Activity ensures a recreated Activity captures its own
    // original state instead of restoring a stale orientation from a prior host.
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun persistResumePosition() {
        val positionMs = playbackClient.currentPositionMs()
        val durationMs = playbackState.durationMs
        // Once a movie is effectively finished, do not leave the user with a
        // misleading "Continue" position a few seconds from the end. The
        // playback-history layer already uses the same 95% completion rule.
        val resumePosition = if (durationMs > 0L && positionMs >= (durationMs * 0.95f).toLong()) {
            0L
        } else {
            positionMs
        }
        onSavePosition(resumePosition)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Background/PiP playback is service-owned. Persist the current
                // position here, but never pause merely because the Activity
                // lost foreground focus. User navigation away from the player
                // is handled by the composition disposal path below.
                persistResumePosition()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            persistResumePosition()
            playbackClient.pause()
        }
    }

    LaunchedEffect(uri) {
        ignition = true
        kotlinx.coroutines.delay(900)
        if (playbackClient.currentUri() != uri) {
            val savedPosition = if (resumePositionMs > 0L) resumePositionMs else playbackClient.savedPositionFor(uri)
            playbackClient.play(uri, savedPosition)
        } else {
            playbackClient.resume()
        }
        ignition = false
    }

    LaunchedEffect(pitLane) {
        if (pitLane) {
            playbackClient.pause()
            kotlinx.coroutines.delay(650)
            onBack()
        }
    }

    LaunchedEffect(ignition, showTelemetry, telemetryInteractionToken, isSeeking) {
        if (!ignition && showTelemetry && !isSeeking) {
            kotlinx.coroutines.delay(3800)
            showTelemetry = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VideoScaleFrame(scaleMode, Modifier.fillMaxSize()) { playerModifier ->
            AndroidView(
                modifier = playerModifier.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    videoBounds = Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
                },
                factory = {
                    PlayerView(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        controllerAutoShow = false
                        player = playbackClient.player()
                    }
                },
                update = { view ->
                    view.useController = false
                    view.resizeMode = scaleMode.resizeMode
                    view.player = playbackClient.player()
                }
            )
        }

        if (pitLane) {
            PitLaneOverlay(title = mediaTitle, modifier = Modifier.fillMaxSize())
        } else if (ignition) {
            IgnitionOverlay(
                title = mediaTitle,
                resumePositionMs = resumePositionMs,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!ignition && !isInPip) {
            VideoGestureOverlay(
                player = playbackClient.player(),
                onSingleTap = { showTelemetry = !showTelemetry; telemetryInteractionToken++ },
                onDoubleTapPlayPause = { if (playbackState.isPlaying) playbackClient.pause() else playbackClient.resume() },
                onLongPress = { offset -> commandPopupAnchor = offset; showCommandPopup = true; showTelemetry = false },
                onSeekStart = { isSeeking = true; showTelemetry = true },
                onSeekEnd = { isSeeking = false; telemetryInteractionToken++ },
                gesturesEnabled = !locked,
                modifier = Modifier.fillMaxSize()
            )
        }

        playbackState.errorMessage?.let { message ->
            PlayerErrorOverlay(
                message = message,
                onRetry = { playbackClient.retry() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (!ignition && !isInPip && showTelemetry) {
            PlayerTelemetryHud(
                title = mediaTitle,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                speed = playbackState.playbackSpeed,
                isPlaying = playbackState.isPlaying,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
            )
        }

        if (!ignition && !isInPip && showCommandPopup) {
            PlayerCommandPopup(
                speedLabel = if (playbackSpeed % 1f == 0f) "${playbackSpeed.toInt()}×" else "${playbackSpeed}×",
                locked = locked,
                rotationLabel = rotationMode.name,
                scaleLabel = scaleMode.label,
                onDismiss = { showCommandPopup = false; commandPopupAnchor = null },
                onLock = { locked = true; showCommandPopup = false; commandPopupAnchor = null },
                onUnlock = { locked = false },
                onSpeed = { playbackSpeed = nextPlaybackSpeed(playbackSpeed) },
                onResize = { showScaleSheet = true },
                onRotation = {
                    rotationMode = when (rotationMode) {
                        RotationMode.AUTO -> RotationMode.PORTRAIT
                        RotationMode.PORTRAIT -> RotationMode.LANDSCAPE
                        RotationMode.LANDSCAPE -> RotationMode.AUTO
                    }
                },
                onPip = { activity?.let { PipController.enterManually(it) }; showCommandPopup = false; commandPopupAnchor = null },
                onTracks = { showTrackSheet = true },
                anchor = commandPopupAnchor,
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
    if (showTrackSheet) {
        TrackSelectionSheet(
            player = playbackClient.player(),
            onDismiss = { showTrackSheet = false },
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
            .background(PlayerControlBlack, RoundedCornerShape(14.dp))
            .border(1.dp, RvhGold.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
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
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(AccentTeal, RoundedCornerShape(50))
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
