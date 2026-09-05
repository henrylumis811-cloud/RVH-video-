package com.rvh.video.ui.shorts

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary

/**
 * NOTE on the blur: ExoPlayer/Media3's default output surface is a
 * SurfaceView, which composites directly to the display outside Compose's
 * layer tree, so no Compose-side approach can blur it (moot point now
 * that glassSurface itself doesn't attempt real blur either — see
 * Glass.kt).
 * A true "video blurred through the sheet" effect would require switching
 * the player output to a TextureView (so its frames join Compose's normal
 * layer tree) or capturing periodic bitmap snapshots — both add real
 * performance cost for a decorative effect on a screen that's already
 * running a video decoder. The mockup itself actually shows a solid light
 * card here, not a see-through blurred one, so a dimming scrim over the
 * video + this opaque frosted panel matches the reference without that
 * tradeoff.
 */
@Composable
fun ShortsCommentSheet(
    videoUri: String,
    onDismiss: () -> Unit,
) {
    val comments = remember(videoUri) { CommentGenerator.forVideo(videoUri) }
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sheetDrag"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)) // scrim over the shorts video behind
            .pointerInput(Unit) { detectVerticalDragGestures { _, _ -> } } // absorbs drags so they don't fall through to the pager
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(360.dp)
                .graphicsLayer { translationY = animatedOffset }
                .background(Color(0xFFF5F5F7), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 120f) onDismiss() else dragOffset = 0f
                        }
                    ) { change, amount ->
                        change.consume()
                        dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                    }
                }
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFFD0D0D5), RoundedCornerShape(2.dp))
            )

            Text(
                "Comments",
                style = RvhType.ScreenTitle,
                color = Color.Black,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(comments) { comment -> CommentRow(comment) }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: PlaceholderComment) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Letter-based fallback avatar, per the spec — no image asset needed.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(comment.avatarLetter.toString(), color = Color.White, style = RvhType.CardTitle)
        }

        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("${comment.username}: ") }
                    append(comment.text)
                },
                style = RvhType.Body,
                color = Color.Black,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("${comment.likes}", style = RvhType.Stat, color = TextSecondary)
                Text("${comment.minutesAgo}m", style = RvhType.Meta, color = TextSecondary)
            }
        }
    }
}
