package com.rvh.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleAdjustSheet(
    current: VideoScaleMode,
    onSelect: (VideoScaleMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xE6081417),
        scrimColor = Color(0xCC000000),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = AccentTeal.copy(alpha = 0.72f)) },
    ) {
        Text(
            "Scale adjust",
            style = RvhType.ScreenTitle,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        VideoScaleMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode); onDismiss() }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    mode.label,
                    style = RvhType.Body,
                    color = if (mode == current) AccentTeal else Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (mode == current) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = AccentTeal)
                }
            }
        }
    }
}
