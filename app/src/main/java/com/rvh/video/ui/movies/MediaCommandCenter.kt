package com.rvh.video.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

/** Legendary Step 50: operational command center for the current media target. */
@Composable
fun MediaCommandCenter(
    movie: LocalVideoEntity,
    mode: String,
    progressPercent: Int,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onWatchLater: () -> Unit,
    onDetails: () -> Unit,
    onFocus: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(20.dp), blurRadius = 18.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MEDIA COMMAND CENTER", style = RvhType.Meta, color = TextSecondary)
                Text(movie.displayName.substringBeforeLast('.'), style = RvhType.ScreenTitle, color = Color.White, maxLines = 1)
            }
            Text("${progressPercent.coerceIn(0, 100)}%", style = RvhType.Meta, color = Color.White)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("STATUS  •  $mode", style = RvhType.Meta, color = TextSecondary)
            Text(if (progressPercent > 0) "SESSION ACTIVE" else "READY TO LAUNCH", style = RvhType.Meta, color = TextSecondary)
        }
        LinearProgressIndicator(
            progress = { (progressPercent.coerceIn(0, 100) / 100f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            trackColor = Color.White.copy(alpha = 0.08f)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CenterButton(if (progressPercent > 0) "▶ RESUME" else "▶ LAUNCH", Modifier.weight(1.2f), onPlay)
            CenterButton(if (movie.isFavorite) "★ FAV" else "☆ FAV", Modifier.weight(.72f), onFavorite)
            CenterButton(if (movie.isWatchLater) "✓ LATER" else "＋ LATER", Modifier.weight(.82f), onWatchLater)
            CenterButton("INFO", Modifier.weight(.58f), onDetails)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CenterButton("🎯 ENTER FOCUS", Modifier.weight(1f), onFocus)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MISSION", style = RvhType.Meta, color = TextSecondary)
            Text(
                when {
                    progressPercent >= 90 -> "FINAL LAP"
                    progressPercent > 0 -> "IN PROGRESS"
                    movie.isWatchLater -> "QUEUED"
                    movie.isFavorite -> "QUALIFYING"
                    else -> "STANDBY"
                },
                style = RvhType.Meta,
                color = Color.White
            )
        }
        Text("COMMAND CENTER 2.0  •  ONE TARGET  •  FULL CONTROL", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun CenterButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(Color.White.copy(alpha = 0.075f), RoundedCornerShape(10.dp)),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 5.dp)
    ) {
        Text(label, style = RvhType.Meta, color = Color.White, maxLines = 1)
    }
}
