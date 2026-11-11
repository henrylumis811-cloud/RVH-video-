package com.rvh.video.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rvh.video.ui.theme.AccentTeal
import androidx.compose.ui.unit.dp

/** Subtle, low-cost placeholder shown while a canonical media thumbnail is prepared. */
@Composable
fun RvhArtworkLoading(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "artwork-loading")
    val progress by transition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450),
            repeatMode = RepeatMode.Restart,
        ),
        label = "artwork-shimmer",
    )
    val base = Color(0xFF151B22)
    val highlight = AccentTeal.copy(alpha = 0.09f)
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(base, Color(0xFF1A2731), base),
                    start = Offset(progress * 420f, 0f),
                    end = Offset(progress * 420f + 260f, 260f),
                ),
                RoundedCornerShape(12.dp),
            )
            .background(highlight.copy(alpha = (0.035f + (progress.coerceIn(0f, 1f) * 0.035f))), RoundedCornerShape(12.dp))
    )
}
