package com.rvh.video.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.TextPrimary
import com.rvh.video.ui.theme.glassSurface

private val SettingsAccent = AccentTeal
@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = RvhType.Meta,
        color = SettingsAccent,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 16.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SettingsAccent)
        Text(
            text = label,
            style = RvhType.Body,
            color = TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
        trailing?.invoke()
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        icon = icon,
        label = label,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = SettingsAccent, checkedThumbColor = Color(0xFF17130C), uncheckedTrackColor = Color(0xFF2A2926), uncheckedThumbColor = Color(0xFF8F887C))
            )
        }
    )
}

@Composable
fun RvhAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = Color(0xE6081417),
        iconContentColor = SettingsAccent,
        titleContentColor = SettingsAccent,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
    )
}
