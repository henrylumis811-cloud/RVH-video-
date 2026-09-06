package com.rvh.video.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.rvh.video.data.model.VideoCategory
import com.rvh.video.ui.theme.RvhType

@Composable
fun RecategorizeMenu(
    expanded: Boolean,
    currentCategory: VideoCategory,
    onDismiss: () -> Unit,
    onSelect: (VideoCategory) -> Unit,
    onAddToCollection: (() -> Unit)? = null,
    isWatchLater: Boolean = false,
    onToggleWatchLater: (() -> Unit)? = null,
    onDetails: (() -> Unit)? = null,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (onDetails != null) {
            DropdownMenuItem(text = { Text("View details", style = RvhType.Body, modifier = androidx.compose.ui.Modifier.padding(vertical = 2.dp)) }, onClick = { onDetails(); onDismiss() })
        }
        if (onAddToCollection != null) {
            DropdownMenuItem(
                text = { Text("Add to collection", style = RvhType.Body, modifier = androidx.compose.ui.Modifier.padding(vertical = 2.dp)) },
                onClick = { onAddToCollection(); onDismiss() }
            )
        }
        if (onToggleWatchLater != null) {
            DropdownMenuItem(
                text = { Text(if (isWatchLater) "Remove from Watch Later" else "Add to Watch Later", style = RvhType.Body, modifier = androidx.compose.ui.Modifier.padding(vertical = 2.dp)) },
                onClick = { onToggleWatchLater(); onDismiss() }
            )
        }
        VideoCategory.entries
            .filter { it != currentCategory }
            .forEach { category ->
                DropdownMenuItem(
                    text = { Text("Move to ${category.displayName()}", style = RvhType.Body, modifier = androidx.compose.ui.Modifier.padding(vertical = 2.dp)) },
                    onClick = { onSelect(category) }
                )
            }
    }
}

private fun VideoCategory.displayName(): String = when (this) {
    VideoCategory.SHORT -> "Shorts"
    VideoCategory.MOVIE -> "Movies"
    VideoCategory.MUSIC_VIDEO -> "Music Videos"
}
