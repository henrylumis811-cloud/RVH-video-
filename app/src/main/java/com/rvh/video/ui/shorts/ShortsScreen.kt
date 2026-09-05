package com.rvh.video.ui.shorts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.rvh.video.ui.components.RecategorizeMenu
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.DangerRed
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.glassPill
import kotlinx.coroutines.delay
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
    val shorts by viewModel.shorts.collectAsState()
    val context = LocalContext.current
    val pool = remember { ShortsPlayerPool(context) }
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    val lifecycleOwner = LocalLifecycleOwner.current

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
            }
    }

    // Pause everything when the app backgrounds; release on screen teardown.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) pool.pauseAll()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pool.releaseAll()
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
                )
            }
        }

        activeCommentSheetUri?.let { uri ->
            ShortsCommentSheet(videoUri = uri, onDismiss = { activeCommentSheetUri = null })
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
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val commentCount = remember(video.uri) { CommentGenerator.totalCountForVideo(video.uri) }

    // Tracks whether the user has manually paused this page — separate
    // from the pool's own playWhenReady state, since the pool doesn't
    // expose playback-state callbacks and this only needs to reflect
    // taps on THIS page, not every player-pool event.
    var isPlaying by remember(video.uri) { mutableStateOf(true) }
    var showPauseFlash by remember { mutableStateOf(false) }
    var progress by remember(video.uri) { mutableFloatStateOf(0f) }
    var isDraggingSeek by remember { mutableStateOf(false) }

    // Polls position while playing and not being manually dragged — same
    // pattern as BackgroundPlaybackBar's progress bar, since Media3 has no
    // lightweight "position changed" callback to subscribe to instead.
    LaunchedEffect(pageIndex, isPlaying, isDraggingSeek) {
        while (isPlaying && !isDraggingSeek) {
            val player = pool.playerFor(pageIndex)
            if (player != null && player.duration > 0) {
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
                    player.playWhenReady = isPlaying
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
                label = formatCount(commentCount * 8), // likes run higher than comments — simple derived scale, still seeded
                onClick = onToggleFavorite
            )
            ActionRailItem(
                icon = Icons.AutoMirrored.Filled.Comment,
                tint = Color.White,
                label = formatCount(commentCount),
                onClick = onCommentClick
            )
            ActionRailItem(
                icon = Icons.Filled.Share,
                tint = Color.White,
                label = formatCount(commentCount / 2),
                onClick = { /* platform share intent — no backend needed */ }
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
                text = remember(video.uri) { CommentGenerator.captionForVideo(video.uri) },
                style = RvhType.Body,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        // Persistent thin draggable timeline, TikTok-style — always
        // visible at the very bottom (unlike the Movies/Music players'
        // auto-hiding scrubber), drag anywhere along it to seek.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 2.dp)
                .height(20.dp) // generous touch target even though the visible bar is thin
                .pointerInput(pageIndex) {
                    detectDragGestures(
                        onDragStart = { isDraggingSeek = true },
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
                        val newProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        progress = newProgress
                    }
                },
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
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

private fun formatCount(count: Int): String = when {
    count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K"
    else -> count.toString()
}
