package com.rvh.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType

/** RVH-styled display/resize picker; deliberately not a system bottom sheet. */
@Composable
fun ScaleAdjustSheet(
    current: VideoScaleMode,
    onSelect: (VideoScaleMode) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xF20A1118), RoundedCornerShape(26.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("DISPLAY MODE", style = RvhType.Meta, color = AccentTeal)
            Text("Resize video", style = RvhType.ScreenTitle, color = Color.White)
            Text("Choose how the source fits the player.", style = RvhType.Body, color = Color(0xFF94A3B8))
            VideoScaleMode.entries.forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (mode == current) AccentTeal.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.035f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(mode); onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.label, style = RvhType.CardTitle, color = if (mode == current) AccentTeal else Color.White)
                        Text(
                            when (mode) {
                                VideoScaleMode.ORIGINAL -> "Preserve the source aspect ratio"
                                VideoScaleMode.FULL_SCREEN -> "Fill the screen and crop as needed"
                                VideoScaleMode.RATIO_16_9 -> "Fit inside a 16:9 frame"
                                VideoScaleMode.RATIO_4_3 -> "Fit inside a 4:3 frame"
                            },
                            style = RvhType.Meta,
                            color = Color(0xFF94A3B8),
                        )
                    }
                    if (mode == current) Icon(Icons.Filled.Check, null, tint = AccentTeal, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
