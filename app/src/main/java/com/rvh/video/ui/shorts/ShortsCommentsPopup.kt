package com.rvh.video.ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.PlayerControlBlackRaised
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary

/**
 * RVH's Shorts comments popup.
 *
 * This is intentionally a custom Dialog rather than a Material bottom sheet:
 * comments are secondary player content, so they should feel like an RVH
 * overlay while leaving the Shorts player visually anchored behind the scrim.
 * The comments remain local preview data and are never presented as a real
 * network/social feed.
 */
@Composable
fun ShortsCommentsPopup(
    videoUri: String,
    onDismiss: () -> Unit,
) {
    val comments = remember(videoUri) { CommentGenerator.forVideo(videoUri) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 430.dp)
                    .fillMaxWidth(0.88f)
                    .height(430.dp)
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(26.dp),
                color = PlayerControlBlackRaised,
                tonalElevation = 10.dp,
                shadowElevation = 18.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                text = "COMMENTS",
                                style = RvhType.ScreenTitle,
                                color = Color.White,
                            )
                            Text(
                                text = "Local preview — nothing is published or synced.",
                                style = RvhType.Meta,
                                color = TextSecondary,
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close comments",
                                tint = TextSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AccentTeal.copy(alpha = 0.22f)),
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(comments) { comment -> CommentRow(comment) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: PlaceholderComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                comment.avatarLetter.toString(),
                color = Color.White,
                style = RvhType.CardTitle,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("${comment.username}: ")
                    }
                    append(comment.text)
                },
                style = RvhType.Body,
                color = Color.White,
            )
            Text(
                "Preview comment",
                style = RvhType.Meta,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
