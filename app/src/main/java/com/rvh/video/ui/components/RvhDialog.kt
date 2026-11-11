package com.rvh.video.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.TextSecondary

/** Shared RVH dialog surface for consistent contrast, shape, and accent treatment. */
@Composable
fun RvhDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = Color(0xF20A1118),
        titleContentColor = AccentTeal,
        textContentColor = TextSecondary,
        iconContentColor = AccentTeal,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
    )
}
