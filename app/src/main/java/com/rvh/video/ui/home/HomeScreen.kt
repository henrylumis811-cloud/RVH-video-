package com.rvh.video.ui.home
import com.rvh.video.ui.theme.Surface0

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.R
import com.rvh.video.ui.components.formatDuration
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.Surface2
import com.rvh.video.ui.theme.Surface3
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.TextTertiary
import com.rvh.video.ui.theme.glassSurface

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenMovie: (String) -> Unit,
    onOpenMedia: (String, VideoCategory) -> Unit,
    onOpenMovies: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenShorts: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onToggleFavorite: (LocalVideoEntity) -> Unit,
    onToggleWatchLater: (LocalVideoEntity) -> Unit,
) {
    val recent by viewModel.recentlyAdded.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val count by viewModel.libraryCount.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val watchLater by viewModel.watchLater.collectAsState()
    val playbackHistory by viewModel.playbackHistory.collectAsState()
    val forYou by viewModel.forYou.collectAsState()
    val folderGroups by viewModel.folderGroups.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(com.rvh.video.ui.theme.Surface0)) {
        // Legendary Step 21: RVH gets a cinematic automotive identity.
        // The supplied car artwork stays local, while a dark scrim keeps every
        // interactive surface readable on top of the high-contrast photograph.
        Image(
            painter = painterResource(R.drawable.rvh_hero_car),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.34f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f),
                        0.42f to Color.Black.copy(alpha = 0.28f),
                        0.72f to Color.Black.copy(alpha = 0.52f),
                        1f to Surface0.copy(alpha = 0.96f)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        item {
            val transition = rememberInfiniteTransition(label = "hero_motion")
            val heroScale by transition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.035f,
                animationSpec = infiniteRepeatable(
                    animation = tween(9000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "hero_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .scale(heroScale)
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.rvh_hero_car),
                    contentDescription = "RVH cinematic automotive background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.42f to Color.Black.copy(alpha = 0.08f),
                            0.68f to Color.Black.copy(alpha = 0.28f),
                            1f to Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.52f),
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.08f)
                        )
                    )
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("RVH • AUTOMOTIVE EDITION", style = RvhType.Meta, color = AccentTeal)
                    Text("Drive. Watch.\nRepeat.", style = RvhType.ScreenTitle, color = Color.White)
                    Text(
                        "$count items in your private library",
                        style = RvhType.Meta,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(AccentTeal)
                                .clickable(onClick = onOpenMovies)
                                .padding(horizontal = 18.dp, vertical = 11.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Text("Enter library", style = RvhType.CardTitle, color = Color.Black)
                            }
                        }
                        androidx.compose.material3.IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .size(44.dp)
                                .glassSurface(shape = RoundedCornerShape(50.dp))
                        ) {
                            Icon(Icons.Filled.Search, "Search library", tint = Color.White)
                        }
                        androidx.compose.material3.IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .glassSurface(shape = RoundedCornerShape(50.dp))
                        ) {
                            Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glassSurface(shape = RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Speed, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                            Text("RVH COCKPIT", style = RvhType.Meta, color = AccentTeal)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Your media at a glance", style = RvhType.CardTitle, color = Color.White)
                    }
                    Text(
                        if (continueWatching.isNotEmpty()) "READY TO RESUME" else "SYSTEM READY",
                        style = RvhType.Meta,
                        color = TextSecondary
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { CockpitMetricCard("LIBRARY", count.toString(), "items", Icons.Filled.MovieFilter) }
                    item { CockpitMetricCard("RESUME", continueWatching.size.toString(), "in progress", Icons.Filled.PlayCircle) }
                    item { CockpitMetricCard("FAVORITES", favorites.size.toString(), "saved", Icons.Filled.FavoriteBorder) }
                    item { CockpitMetricCard("WATCH LATER", watchLater.size.toString(), "queued", Icons.Filled.Schedule) }
                    item { CockpitMetricCard("HISTORY", playbackHistory.size.toString(), "played", Icons.Filled.History) }
                }
            }
        }

        item {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { CommandCard("Shorts", "Jump in", Icons.Filled.PlayCircle, onOpenShorts) }
                item { CommandCard("Movies", "Your cinema", Icons.Filled.MovieFilter, onOpenMovies) }
                item { CommandCard("Music", "Play something", Icons.Filled.MusicNote, onOpenMusic) }
                item { CommandCard("Collections", "Your shelves", Icons.Filled.FolderCopy, onOpenCollections) }

            }
        }

        // Legendary Step 45: Universal Quick Actions put the most useful
        // destinations and the smartest resume action within one tap.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glassSurface(shape = RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("UNIVERSAL QUICK ACTIONS", style = RvhType.Meta, color = AccentTeal)
                        Text("Everything important. One tap away.", style = RvhType.CardTitle, color = Color.White)
                    }
                    Text("READY", style = RvhType.Meta, color = TextSecondary)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (continueWatching.isNotEmpty()) {
                        item {
                            QuickActionChip(
                                label = "▶ RESUME",
                                detail = continueWatching.first().displayName.substringBeforeLast('.'),
                                onClick = { onOpenMovie(continueWatching.first().uri) }
                            )
                        }
                    }
                    item { QuickActionChip("MOVIES", "Open garage", onOpenMovies) }
                    item { QuickActionChip("COLLECTIONS", "Open shelves", onOpenCollections) }
                    item { QuickActionChip("SEARCH", "Find media", onOpenSearch) }
                    item { QuickActionChip("MUSIC", "Play videos", onOpenMusic) }
                }
            }
        }

        if (forYou.isNotEmpty()) {
            item { SectionHeader("For you", "Personalized on this device") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(forYou, key = { it.uri }) { video ->
                        RecentCard(video, { onOpenMedia(video.uri, video.effectiveCategory) })
                    }
                }
            }
        }

        if (favorites.isNotEmpty()) {
            item { SectionHeader("Your favorites") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(favorites, key = { it.uri }) { video ->
                        RecentCard(video, { onOpenMedia(video.uri, video.effectiveCategory) }, onFavorite = { onToggleFavorite(video) })
                    }
                }
            }
        }

        if (watchLater.isNotEmpty()) {
            item { SectionHeader("Watch Later") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(watchLater, key = { it.uri }) { video ->
                        RecentCard(video, { onOpenMedia(video.uri, video.effectiveCategory) })
                    }
                }
            }
        }

        if (playbackHistory.isNotEmpty()) {
            item { SectionHeader("Recently played") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(playbackHistory, key = { it.uri }) { video ->
                        RecentCard(video, { onOpenMedia(video.uri, video.effectiveCategory) })
                    }
                }
            }
        }

        if (folderGroups.isNotEmpty()) {
            item { SectionHeader("Explore your folders", "Grouped from your local library") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(folderGroups, key = { it.first }) { (folder, size) ->
                        Column(
                            Modifier
                                .width(190.dp)
                                .glassSurface(shape = RoundedCornerShape(18.dp))
                                .clickable { onOpenFolder(folder) }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Filled.FolderCopy, null, tint = AccentTeal, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(folder, style = RvhType.CardTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
                            Text("$size ${if (size == 1) "item" else "items"}", style = RvhType.Meta, color = TextSecondary)
                        }
                    }
                }
            }
        }

        if (continueWatching.isNotEmpty()) {
            item { SectionHeader("Continue watching") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(continueWatching, key = { it.uri }) { video -> ContinueCard(video, onClick = { onOpenMovie(video.uri) }) }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionHeader("Recently added") }
            item {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recent, key = { it.uri }) { video -> RecentCard(video, onClick = { onOpenMovie(video.uri) }) }
                }
            }
        }
        }
    }
}

@Composable
private fun QuickActionChip(label: String, detail: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.075f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, style = RvhType.Meta, color = Color.White, maxLines = 1)
        Text(detail, style = RvhType.Meta, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = RvhType.CardTitle, color = Color.White)
            if (subtitle != null) Text(subtitle, style = RvhType.Meta, color = TextTertiary)
        }
        Icon(Icons.Filled.ArrowForward, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CockpitMetricCard(
    label: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .height(94.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.20f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = AccentTeal, modifier = Modifier.size(17.dp))
            Text(label, style = RvhType.Meta, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(value, style = RvhType.ScreenTitle, color = Color.White)
            Text(detail, style = RvhType.Meta, color = TextTertiary, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun CommandCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(150.dp).height(104.dp).glassSurface(shape = RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = AccentTeal, modifier = Modifier.size(24.dp))
        Column {
            Text(title, style = RvhType.CardTitle, color = Color.White)
            Text(subtitle, style = RvhType.Meta, color = TextSecondary)
        }
    }
}

@Composable
private fun ContinueCard(video: LocalVideoEntity, onClick: () -> Unit) {
    Column(Modifier.width(220.dp).clickable(onClick = onClick)) {
        MediaThumb(video, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Spacer(Modifier.height(8.dp))
        Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        val progress = if (video.durationMs > 0) (video.resumePositionMs.toFloat() / video.durationMs).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(Surface2)) {
            Box(Modifier.fillMaxWidth(progress).height(3.dp).background(AccentTeal))
        }
        Spacer(Modifier.height(4.dp))
        Text("Continue from ${formatDuration(video.resumePositionMs)} of ${formatDuration(video.durationMs)}", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun RecentCard(video: LocalVideoEntity, onClick: () -> Unit, onFavorite: (() -> Unit)? = null) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box {
            MediaThumb(video, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            if (onFavorite != null) {
                androidx.compose.material3.IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Favorite, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(categoryLabel(video.effectiveCategory), style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun MediaThumb(video: LocalVideoEntity, modifier: Modifier) {
    val context = LocalContext.current
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Surface1)) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(video.uri).videoFrameMillis(1000).crossfade(true).build(),
            contentDescription = video.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (video.resumePositionMs > 0) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(34.dp).glassSurface(shape = RoundedCornerShape(50), blurRadius = 8.dp).padding(7.dp))
        }
    }
}

private fun categoryLabel(category: VideoCategory): String = when (category) {
    VideoCategory.MOVIE -> "Movie"
    VideoCategory.MUSIC_VIDEO -> "Music"
    VideoCategory.SHORT -> "Short"
}
