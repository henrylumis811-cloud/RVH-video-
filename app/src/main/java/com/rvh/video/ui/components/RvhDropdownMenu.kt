package com.rvh.video.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.AccentTeal

/** Shared dropdown surface for library and search controls. */
@Composable
fun RvhDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.border(1.dp, AccentTeal.copy(alpha = 0.22f), shape),
        shape = shape,
        containerColor = Color(0xF20B1117),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        content = content,
    )
}
