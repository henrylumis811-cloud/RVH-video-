package com.rvh.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.theme.PlayerControlBlack
import com.rvh.video.ui.theme.RvhGold
import com.rvh.video.ui.theme.RvhGoldSoft
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary

/**
 * RVH's media action popup. This is deliberately a real centered Dialog,
 * rather than a platform DropdownMenu, so long-press actions look and behave
 * consistently in grid/list screens and are not clipped by lazy containers.
 */
@Composable
fun RecategorizeMenu(
    expanded: Boolean,
    currentCategory: VideoCategory,
    onDismiss: () -> Unit,
    onSelect: (VideoCategory) -> Unit,
    onAddToCollection: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    isWatchLater: Boolean = false,
    onToggleWatchLater: (() -> Unit)? = null,
    onDetails: (() -> Unit)? = null,
) {
    if (!expanded) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(PlayerControlBlack, RoundedCornerShape(24.dp))
                .border(1.dp, RvhGold.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MEDIA ACTIONS", style = RvhType.Meta, color = RvhGold, letterSpacing = 1.8.sp)
                        Text("Choose an action", style = RvhType.Body, color = TextSecondary)
                    }
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onDismiss)
                            .padding(8.dp),
                    )
                }

                Spacer(Modifier.height(5.dp))
                if (onDetails != null) ActionRow(Icons.Filled.Info, "View details") { onDetails(); onDismiss() }
                if (onToggleFavorite != null) ActionRow(Icons.Filled.Favorite, if (isFavorite) "Remove from Favorites" else "Add to Favorites") { onToggleFavorite(); onDismiss() }
                if (onAddToCollection != null) ActionRow(Icons.Filled.FolderCopy, "Add to collection") { onAddToCollection(); onDismiss() }
                if (onToggleWatchLater != null) {
                    ActionRow(
                        Icons.Filled.Schedule,
                        if (isWatchLater) "Remove from Watch Later" else "Add to Watch Later",
                        { onToggleWatchLater(); onDismiss() },
                    )
                }

                if (VideoCategory.entries.any { it != currentCategory }) {
                    Spacer(Modifier.height(4.dp))
                    Text("MOVE TO", style = RvhType.Meta, color = RvhGoldSoft, letterSpacing = 1.4.sp)
                    VideoCategory.entries
                        .filter { it != currentCategory }
                        .forEach { category ->
                            ActionRow(
                                Icons.Filled.Category,
                                "Move to ${category.displayName()}",
                            ) {
                                onSelect(category)
                                onDismiss()
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.045f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RvhGold, modifier = Modifier.size(20.dp))
        Text(label, style = RvhType.Body, color = Color.White, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun VideoCategory.displayName(): String = when (this) {
    VideoCategory.SHORT -> "Shorts"
    VideoCategory.MOVIE -> "Movies"
    VideoCategory.MUSIC_VIDEO -> "Music Videos"
}
