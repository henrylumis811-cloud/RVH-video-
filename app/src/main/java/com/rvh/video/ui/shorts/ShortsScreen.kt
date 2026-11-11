package com.rvh.video.ui.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.rvh.video.RvhViewModelFactory
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.player.ShortsPlayerPool
import com.rvh.video.data.local.AppSettings
import com.rvh.video.ui.components.RecategorizeMenu
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.DangerRed
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.TextPrimary
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.glassPill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    initialVideoUri: String? = null,
    viewModel: ShortsViewModel = viewModel(
        factory = RvhViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onAddToCollection: (LocalVideoEntity) -> Unit = {},
    onToggleWatchLater: (String, Boolean) -> Unit = { _, _ -> },
    onOpenDetails: (String) -> Unit = {},
) {
    val shorts by viewModel.shorts.collectAsStateWithLifecycle()
    val sessionReady by viewModel.sessionReady.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settings = remember { AppSettings(context.applicationContext) }
    val pool = remember { ShortsPlayerPool(context) }
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var showResumePrompt by remember { mutableStateOf(false) }
    var resumeTargetIndex by remember { mutableStateOf<Int?>(null) }

    // Every Shorts opening gets a fresh smart queue. The underlying library order
    // remains untouched, so this only affects the Shorts viewing session.
    LaunchedEffect(Unit) {
        viewModel.startNewSession()
    }

    // Offer a resume choice once per Shorts opening when a previous Short exists.
    LaunchedEffect(shorts, initialVideoUri, sessionReady) {
        if (sessionReady && shorts.isNotEmpty() && initialVideoUri == null && !showResumePrompt) {
            val savedUri = settings.lastShortUri
            val savedIndex = savedUri?.let { uri -> shorts.indexOfFirst { it.uri == uri } } ?: -1
            if (savedIndex > 0) {
                resumeTargetIndex = savedIndex
                showResumePrompt = true
            } else if (savedIndex == 0) {
                // Being at the first Short is still a valid saved position, but
                // restarting would produce the same result; no dialog is needed.
                resumeTargetIndex = 0
            }
        }
    }

    // When a short is launched from Home/For You, jump directly to that item
    // instead of opening the vertical feed at page zero.
    LaunchedEffect(shorts, initialVideoUri) {
        val targetIndex = initialVideoUri?.let { uri -> shorts.indexOfFirst { it.uri == uri } } ?: -1
        if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    var activeCommentSheetUri by remember { mutableStateOf<String?>(null) }

    // Pool follows the settled page, not every intermediate scroll frame —
    // building/tearing down players mid-fling would be wasted work.
    LaunchedEffect(pagerState, shorts) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { index ->
                pool.onPageSettled(index) { i -> shorts.getOrNull(i)?.uri }
                shorts.getOrNull(index)?.uri?.let { settings.lastShortUri = it }
            }
    }

    // Pause everything when the app backgrounds; release on screen teardown.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> pool.pauseAll()
                Lifecycle.Event.ON_RESUME -> pool.resumeAfterLifecycle()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pool.releaseAll()
        }
    }

    if (showResumePrompt && resumeTargetIndex != null) {
        Dialog(onDismissRequest = {
            // Dismissing behaves like Continue rather than trapping the user.
            val target = resumeTargetIndex ?: 0
            showResumePrompt = false
            coroutineScope.launch { pagerState.scrollToPage(target) }
        }) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = Surface0,
                tonalElevation = 8.dp,
                modifier = Modifier.padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Resume Shorts?", style = RvhType.ScreenTitle, color = TextPrimary)
                    Text(
                        "You stopped at Short ${(resumeTargetIndex ?: 0) + 1}. Continue from there or restart the feed?",
                        style = RvhType.Body,
                        color = TextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            settings.lastShortUri = shorts.firstOrNull()?.uri
                            showResumePrompt = false
                            coroutineScope.launch { pagerState.scrollToPage(0) }
                        }) {
                            Text("START OVER", color = TextSecondary)
                        }
                        TextButton(onClick = {
                            val target = resumeTargetIndex ?: 0
                            showResumePrompt = false
                            coroutineScope.launch { pagerState.scrollToPage(target) }
                        }) {
                            Text("CONTINUE", color = RvhGold)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (shorts.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = shorts[page]
                ShortPage(
                    video = video,
                    pool = pool,
                    pageIndex = page,
                    onCommentClick = { activeCommentSheetUri = video.uri },
                    onRecategorize = { viewModel.recategorize(video.uri, it) },
                    onToggleFavorite = { viewModel.toggleFavorite(video.uri, !video.isFavorite) },
                    onAddToCollection = { onAddToCollection(video) },
                    onToggleWatchLater = { onToggleWatchLater(video.uri, !video.isWatchLater) },
                    onOpenDetails = { onOpenDetails(video.uri) },
                    onShare = { shareVideo(context, video) },
                )
            }
        }

        activeCommentSheetUri?.let { uri ->
            ShortsCommentsPopup(videoUri = uri, onDismiss = { activeCommentSheetUri = null })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortPage(
    video: LocalVideoEntity,
    pool: ShortsPlayerPool,
    pageIndex: Int,
    onCommentClick: () -> Unit,
    onRecategorize: (VideoCategory) -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCollection: () -> Unit = {},
    onToggleWatchLater: () -> Unit = {},
    onOpenDetails: () -> Unit = {},
    onShare: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Tracks whether the user has manually paused this page — separate
    // from the pool's own playWhenReady state, since the pool doesn't
    // expose playback-state callbacks and this only needs to reflect
    // taps on THIS page, not every player-pool event.
    var isPlaying by remember(video.uri) { mutableStateOf(true) }
    var showPauseFlash by remember { mutableStateOf(false) }
    var progress by remember(video.uri) { mutableFloatStateOf(0f) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var seekBarExpanded by remember { mutableStateOf(false) }

    // Polls position while playing and not being manually dragged — same
    // pattern as BackgroundPlaybackBar's progress bar, since Media3 has no
    // lightweight "position changed" callback to subscribe to instead.
    LaunchedEffect(pageIndex, isPlaying, isDraggingSeek) {
        while (isPlaying && !isDraggingSeek) {
            val player = pool.playerFor(pageIndex)
            // Only the actively playing page needs a 5 Hz progress loop.
            // Neighbor players are preloaded/paused and should not trigger
            // needless Compose state writes while the user swipes.
            if (player?.isPlaying == true && player.duration > 0) {
                progress = (player.currentPosition.toFloat() / player.duration).coerceIn(0f, 1f)
            }
            delay(200)
        }
    }

    // Tap-to-pause flash icon auto-hides shortly after appearing, TikTok-style.
    LaunchedEffect(showPauseFlash) {
        if (showPauseFlash) {
            delay(500)
            showPauseFlash = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {
                    val player = pool.playerFor(pageIndex) ?: return@combinedClickable
                    isPlaying = !isPlaying
                    pool.setUserPlaybackIntent(pageIndex, isPlaying)
                    showPauseFlash = true
                },
                onLongClick = { menuExpanded = true }
            )
    ) {
        // Player surface for this page — pool.playerFor returns null until
        // onPageSettled has built one for this index (current ± radius).
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { PlayerView(it).apply { useController = false } },
            update = { it.player = pool.playerFor(pageIndex) }
        )

        // Brief center play/pause flash on tap, TikTok-style.
        AnimatedVisibility(
            visible = showPauseFlash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom gradient scrim so caption/actions stay legible over any video content.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        startY = 400f
                    )
                )
        )

        // Message/report pill, top-right — matches the mockup's floating glass icon.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding() // was sitting under the status bar icons since the video behind it is intentionally full-bleed
                .padding(16.dp)
                .size(44.dp)
                .glassPill(),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MailOutline, contentDescription = null, tint = Color.White)
        }

        // Right action rail: like, comment, share.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ActionRailItem(
                icon = Icons.Filled.Favorite,
                tint = if (video.isFavorite) DangerRed else Color.White,
                label = "FAVORITE",
                onClick = onToggleFavorite
            )
            ActionRailItem(
                icon = Icons.AutoMirrored.Filled.Comment,
                tint = Color.White,
                label = "COMMENTS",
                onClick = onCommentClick
            )
            ActionRailItem(
                icon = Icons.Filled.Share,
                tint = Color.White,
                label = "SHARE",
                onClick = onShare
            )
        }

        // Caption / creator row, bottom-left.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp, end = 90.dp)
        ) {
            Text(
                text = video.displayName.substringBeforeLast('.'),
                style = RvhType.CardTitle,
                color = Color.White,
            )
            Text(
                text = "${video.effectiveCategory.name.replace("_", " ")}  •  LOCAL SHORT",
                style = RvhType.Meta,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        // Small TikTok-style timeline: it sits above the system gesture area,
        // expands on touch/drag, and collapses again after interaction.
        val seekTrackHeight by animateDpAsState(
            targetValue = if (seekBarExpanded || isDraggingSeek) 7.dp else 3.dp,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
            label = "shortsSeekTrackHeight"
        )
        val seekFillFraction by animateFloatAsState(
            targetValue = progress,
            animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
            label = "shortsSeekProgress"
        )
        LaunchedEffect(seekBarExpanded, isDraggingSeek) {
            if (seekBarExpanded && !isDraggingSeek) {
                delay(1600)
                seekBarExpanded = false
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 10.dp)
                .height(42.dp)
                .pointerInput(pageIndex) {
                    detectDragGestures(
                        onDragStart = {
                            seekBarExpanded = true
                            isDraggingSeek = true
                        },
                        onDragEnd = {
                            isDraggingSeek = false
                            val player = pool.playerFor(pageIndex)
                            if (player != null && player.duration > 0) {
                                player.seekTo((progress * player.duration).toLong())
                            }
                        },
                        onDragCancel = { isDraggingSeek = false }
                    ) { change, _ ->
                        change.consume()
                        progress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                }
                .clickable { seekBarExpanded = true },
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(seekTrackHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (seekBarExpanded || isDraggingSeek) 0.45f else 0.3f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(seekFillFraction.coerceIn(0f, 1f))
                    .height(seekTrackHeight)
                    .clip(CircleShape)
                    .background(AccentTeal)
            )
        }

        RecategorizeMenu(
            expanded = menuExpanded,
            currentCategory = video.effectiveCategory,
            onDismiss = { menuExpanded = false },
            onAddToCollection = onAddToCollection,
            isWatchLater = video.isWatchLater,
            onToggleWatchLater = onToggleWatchLater,
            onDetails = onOpenDetails,
            onSelect = { onRecategorize(it); menuExpanded = false }
        )
    }
}

private fun shareVideo(context: android.content.Context, video: LocalVideoEntity) {
    val uri = runCatching { android.net.Uri.parse(video.uri) }.getOrNull() ?: return
    val title = video.displayName.substringBeforeLast('.')
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share video"))
    }.onFailure {
        Toast.makeText(context, "No app available to share this video", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ActionRailItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        }
        Text(label, style = RvhType.Stat, color = Color.White)
    }
}

