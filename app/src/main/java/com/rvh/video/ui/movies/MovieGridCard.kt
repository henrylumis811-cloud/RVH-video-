package com.rvh.video.ui.movies

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.components.RecategorizeMenu
import com.rvh.video.ui.components.formatDuration
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieGridCard(
    movie: LocalVideoEntity,
    onClick: () -> Unit,
    onRecategorize: (VideoCategory) -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCollection: () -> Unit = {},
    isWatchLater: Boolean = movie.isWatchLater,
    onToggleWatchLater: () -> Unit = {},
    onDetails: () -> Unit = {},
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f) // poster-style ratio, matches the mockup grid
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
        ) {
            // Built once per uri via remember, not on every recomposition —
            // a fresh ImageRequest object each frame gave Coil no stable
            // identity to key its cache against, which showed up as
            // flicker/re-decode during scroll instead of serving the
            // already-decoded frame back from cache.
            val request = remember(movie.uri, performanceMode) {
                ImageRequest.Builder(context)
                    .data(movie.uri)
                    .videoFrameMillis(1000) // 1s in — avoids grabbing a black/blank first frame
                    .crossfade(!performanceMode)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = movie.displayName,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Center play affordance — subtle glass circle, not a flat icon,
            // so it still reads as part of the glass language at rest.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .glassSurface(shape = RoundedCornerShape(50), blurRadius = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Progress telemetry: the Garage makes unfinished vehicles visibly
            // different without adding another database field.
            if (movie.durationMs > 0L && movie.resumePositionMs > 0L) {
                val progress = (movie.resumePositionMs.toFloat() / movie.durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .size(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AccentTeal)
                    )
                }
            }

            // Duration badge, bottom-left, matching the mockup.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .glassSurface(shape = RoundedCornerShape(6.dp), blurRadius = 8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(formatDuration(movie.durationMs), style = RvhType.Meta, color = Color.White)
            }

            androidx.compose.material3.IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Filled.Favorite, null, tint = if (movie.isFavorite) AccentTeal else Color.White, modifier = Modifier.size(20.dp))
            }

            RecategorizeMenu(
                expanded = menuExpanded,
                currentCategory = movie.effectiveCategory,
                onDismiss = { menuExpanded = false },
                onAddToCollection = onAddToCollection,
                isWatchLater = isWatchLater,
                onToggleWatchLater = onToggleWatchLater,
            onDetails = onDetails,
                onSelect = {
                    onRecategorize(it)
                    menuExpanded = false
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(12.dp), tint = Color(0xB80B0C0E), borderColor = Color(0x665B5140))
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = movie.displayName.substringBeforeLast('.'),
                style = RvhType.CardTitle,
                color = Color(0xFFE0B35A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${yearFromEpochSeconds(movie.dateModifiedEpochSeconds)}  •  ${formatDuration(movie.durationMs)}",
                style = RvhType.Meta,
                color = TextSecondary,
                maxLines = 1,
            )
        }
    }
}

private fun yearFromEpochSeconds(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).year.toString()
