package com.rvh.video.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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

/** Legendary Step 55: live session control keeps the current target and fleet state actionable. */
@Composable
fun LiveSessionControl(
    movie: LocalVideoEntity,
    progressPercent: Int,
    activeSessions: Int,
    fleetProgress: Int,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onFocus: () -> Unit,
) {
    val progress = progressPercent.coerceIn(0, 100)
    val mission = when {
        progress >= 90 -> "FINAL LAP"
        progress > 0 -> "LIVE SESSION"
        movie.isWatchLater -> "QUEUED"
        movie.isFavorite -> "QUALIFYING"
        else -> "STANDBY"
    }
    Column(
        modifier = Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(18.dp), 16.dp).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("LIVE SESSION CONTROL", style = RvhType.Meta, color = TextSecondary)
                Text(movie.displayName.substringBeforeLast('.'), style = RvhType.Body, color = Color.White, maxLines = 1)
            }
            Text(mission, style = RvhType.Meta, color = Color.White)
        }
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            trackColor = Color.White.copy(alpha = .08f)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SessionButton(if (progress > 0) "▶ RESUME" else "▶ LAUNCH", Modifier.weight(1f), onPlay)
            SessionButton("INFO", Modifier.weight(.55f), onDetails)
            SessionButton("FOCUS", Modifier.weight(.62f), onFocus)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ACTIVE  ${activeSessions.coerceAtLeast(0)}", style = RvhType.Meta, color = TextSecondary)
            Text("FLEET  ${fleetProgress.coerceIn(0, 100)}%", style = RvhType.Meta, color = TextSecondary)
            Text("RVH LIVE", style = RvhType.Meta, color = Color.White)
        }
        Text("SESSION CONTROL  •  TARGET LOCKED  •  LOCAL TELEMETRY", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun SessionButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(Color.White.copy(alpha = .075f), RoundedCornerShape(10.dp)),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)
    ) { Text(label, style = RvhType.Meta, color = Color.White, maxLines = 1) }
}
