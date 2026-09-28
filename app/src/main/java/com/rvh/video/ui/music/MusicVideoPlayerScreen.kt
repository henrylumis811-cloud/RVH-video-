package com.rvh.video.ui.music
import androidx.compose.ui.layout.boundsInWindow

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.rvh.video.player.PipController
import com.rvh.video.ui.components.PlayerErrorOverlay
import com.rvh.video.ui.components.ScaleAdjustSheet
import com.rvh.video.ui.components.VideoGestureOverlay
import com.rvh.video.ui.components.ReimaginedPlayerControls
import com.rvh.video.ui.components.VideoScaleFrame
import com.rvh.video.ui.components.VideoScaleMode
import com.rvh.video.ui.components.TrackSelectionSheet
import com.rvh.video.ui.components.nextPlaybackSpeed
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0

private enum class MusicOrientationStage { Landscape, Portrait }

/**
 * "Minimize" (back arrow, or the second stage of back) leaves playback
 * running and lets BackgroundPlaybackBar take over — that's what fulfills
 * "browsing other sections while playback continues" from the spec,
 * distinct from true system PiP (floating window over OTHER APPS / the
 * home screen). Back is two-stage, same pattern as MoviePlayerScreen:
 * first press un-rotates to portrait and stays here; second press exits
 * to the Music list.
 */
@Composable
fun MusicVideoPlayerScreen(
    viewModel: MusicViewModel,
    isInPip: Boolean,
    onMinimize: () -> Unit,
    onRequestFloatingPlayer: () -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val musicQueueMode by viewModel.musicQueueMode.collectAsStateWithLifecycle()
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
    val track = currentIndex?.let { queue.getOrNull(it) }
    var stage by remember { mutableStateOf(MusicOrientationStage.Landscape) }
    var videoBounds by remember { mutableStateOf<Rect?>(null) }

    var locked by remember { mutableStateOf(false) }
    var rotationLocked by remember { mutableStateOf(false) }
    val playbackState by viewModel.playbackClient.state.collectAsStateWithLifecycle()
    var playbackSpeed by remember { mutableStateOf(viewModel.playbackClient.savedPlaybackSpeed()) }
    var scaleMode by remember { mutableStateOf(VideoScaleMode.ORIGINAL) }
    var showScaleSheet by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }

    fun handleBack() {
        if (stage == MusicOrientationStage.Landscape) {
            stage = MusicOrientationStage.Portrait
        } else {
            onMinimize()
        }
    }

    BackHandler(enabled = !locked) { handleBack() }

    LaunchedEffect(stage, rotationLocked) {
        activity?.requestedOrientation = if (!rotationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else when (stage) {
            MusicOrientationStage.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            MusicOrientationStage.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(playbackSpeed) {
        viewModel.playbackClient.setPlaybackSpeed(playbackSpeed)
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

    LaunchedEffect(Unit) {
        val enabled = com.rvh.video.data.local.AppSettings(context).autoFloatingPlayEnabled
        activity?.let { PipController.configureAutoEnter(it, enabled = enabled) }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.let { PipController.configureAutoEnter(it, enabled = false) }
        }
    }

    LaunchedEffect(videoBounds) {
        val bounds = videoBounds ?: return@LaunchedEffect
        activity?.let { PipController.updateSourceRectHint(it, bounds) }
    }

    LaunchedEffect(playbackState.isPlaying, showControls) {
        if (playbackState.isPlaying && showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        } else if (!playbackState.isPlaying && !isInPip) {
            showControls = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VideoScaleFrame(scaleMode, Modifier.fillMaxSize()) { playerModifier ->
            AndroidView(
                modifier = playerModifier.onGloballyPositioned { coordinates ->
                    val boundsInWindow = coordinates.boundsInWindow()
                    videoBounds = Rect(
                        boundsInWindow.left.toInt(),
                        boundsInWindow.top.toInt(),
                        boundsInWindow.right.toInt(),
                        boundsInWindow.bottom.toInt()
                    )
                },
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        controllerAutoShow = false
                        setShowPreviousButton(false)
                        setShowNextButton(false)
                        player = viewModel.playbackClient.player()
                    }
                },
                update = { view ->
                    view.player = viewModel.playbackClient.player()
                    view.useController = false
                    view.resizeMode = scaleMode.resizeMode
                }
            )
        }

        if (!locked && !isInPip) {
            VideoGestureOverlay(
                player = viewModel.playbackClient.player(),
                onSingleTap = { showControls = !showControls },
                onDoubleTapPlayPause = { if (playbackState.isPlaying) viewModel.playbackClient.pause() else viewModel.playbackClient.resume() },
                modifier = Modifier.fillMaxSize()
            )
        }

        playbackState.errorMessage?.let { message ->
            PlayerErrorOverlay(
                message = message,
                onRetry = { viewModel.playbackClient.retry() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (!isInPip) {
            // The telemetry HUD is permanent playback chrome. Transport controls
            // may disappear after interaction, but this identity/progress HUD
            // remains visible just like the Movie player.
            MusicTelemetryHud(
                title = track?.displayName?.substringBeforeLast('.') ?: "Music Video",
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                speed = playbackState.playbackSpeed,
                isPlaying = playbackState.isPlaying,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
            )

            // In portrait the video itself owns the entire screen instead of
            // being forced into a 16:9 strip at the top. Media3's resize mode
            // handles the actual source aspect ratio inside this full surface.
            if (showControls || locked) {
                ReimaginedPlayerControls(
                    title = track?.displayName?.substringBeforeLast('.') ?: "Music Video",
                    positionMs = playbackState.positionMs,
                    durationMs = playbackState.durationMs,
                    isPlaying = playbackState.isPlaying,
                    playbackSpeed = playbackSpeed,
                    locked = locked,
                    rotationLocked = rotationLocked,
                    onBack = { handleBack() },
                    onPlayPause = { if (playbackState.isPlaying) viewModel.playbackClient.pause() else viewModel.playbackClient.resume() },
                    onSeekBack = { viewModel.playbackClient.seekTo((playbackState.positionMs - 10_000L).coerceAtLeast(0L)) },
                    onSeekForward = { viewModel.playbackClient.seekTo((playbackState.positionMs + 10_000L).coerceAtMost(playbackState.durationMs.coerceAtLeast(0L))) },
                    onSeekTo = { viewModel.playbackClient.seekTo(it) },
                    onLock = { locked = true; showControls = false },
                    onUnlock = { locked = false; showControls = true },
                    onSpeed = { playbackSpeed = nextPlaybackSpeed(playbackSpeed) },
                    onResize = { showScaleSheet = true },
                    scaleModeLabel = scaleMode.label,
                    onRotation = {
                    if (rotationLocked) {
                        rotationLocked = false
                    } else {
                        val current = activity?.resources?.configuration?.orientation
                        stage = if (current == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                            MusicOrientationStage.Landscape
                        } else {
                            MusicOrientationStage.Portrait
                        }
                        rotationLocked = true
                    }
                },
                    onPip = { activity?.let { PipController.enterManually(it) } },
                onTracks = { showTrackSheet = true },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onQueueMode = { viewModel.cycleQueueMode() },
                    queueMode = musicQueueMode,
                    queueModeLabel = when (musicQueueMode) {
                        com.rvh.video.data.local.AppSettings.MUSIC_QUEUE_SHUFFLE -> "SHUFFLE"
                        com.rvh.video.data.local.AppSettings.MUSIC_QUEUE_REPEAT_ONE -> "ONE"
                        else -> "ORDER"
                    },
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
        if (showTrackSheet) {
            TrackSelectionSheet(
                player = viewModel.playbackClient.player(),
                onDismiss = { showTrackSheet = false },
            )
        }
    }
}

@Composable
private fun MusicTelemetryHud(
    title: String,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val accent = Color(0xFFE0B35A)
    val panel = Color(0xE20A0B0D)
    Column(
        modifier = modifier
            .background(panel, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isPlaying) "OVERDRIVE" else "PAUSED", style = RvhType.Meta, color = accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.width(10.dp))
            Text("${if (speed % 1f == 0f) speed.toInt() else speed}x", style = RvhType.Meta, color = Color(0xFFE8D9B9))
        }
        Text(title.ifBlank { "RVH Media" }.take(34), style = RvhType.Meta, color = Color(0xFF8F887C))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMusicDuration(positionMs), style = RvhType.Meta, color = Color(0xFFE8D9B9))
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(width = 90.dp, height = 3.dp).background(Color(0xFF4A453C), androidx.compose.foundation.shape.RoundedCornerShape(50))) {
                Box(Modifier.fillMaxWidth(progress).fillMaxSize().background(accent, androidx.compose.foundation.shape.RoundedCornerShape(50)))
            }
            Spacer(Modifier.width(7.dp))
            Text(formatMusicDuration(durationMs), style = RvhType.Meta, color = Color(0xFF8F887C))
        }
    }
}

private fun formatMusicDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

