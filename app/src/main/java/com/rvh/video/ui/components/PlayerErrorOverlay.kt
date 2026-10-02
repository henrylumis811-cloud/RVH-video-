package com.rvh.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.RvhType

@Composable
fun PlayerErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Playback error", style = RvhType.CardTitle, color = Color.White)
        Text(
            text = message.take(120),
            style = RvhType.Meta,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp),
        )
        IconButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = "Retry playback", tint = Color.White)
        }
    }
}
