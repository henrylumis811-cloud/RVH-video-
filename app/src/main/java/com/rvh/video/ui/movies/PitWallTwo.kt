package com.rvh.video.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Legendary Step 56: multi-session handoff control for the Garage pit wall. */
@Composable
fun PitWallTwo(
    sessions: List<LocalVideoEntity>,
    onOpen: (LocalVideoEntity) -> Unit,
    onDetails: (LocalVideoEntity) -> Unit,
) {
    val lead = sessions.firstOrNull()
    Column(
        modifier = Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(18.dp), 15.dp).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("PIT WALL 2.0", style = RvhType.Meta, color = TextSecondary)
                Text("MULTI-SESSION CONTROL", style = RvhType.Body, color = Color.White)
            }
            Text("${sessions.size} LIVE", style = RvhType.Meta, color = Color.White)
        }
        if (lead != null) {
            Text("HANDOFF TARGET  •  ${lead.displayName.substringBeforeLast('.')}", style = RvhType.Meta, color = Color.White, maxLines = 1)
        }
        sessions.forEachIndexed { index, movie ->
            val pct = if (movie.durationMs > 0L) ((movie.resumePositionMs.coerceIn(0L, movie.durationMs) * 100L) / movie.durationMs).toInt() else 0
            Row(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = .055f), RoundedCornerShape(10.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. ${movie.displayName.substringBeforeLast('.')}", style = RvhType.Meta, color = Color.White, maxLines = 1)
                    Text("${pct}%  •  ${if (pct >= 90) "FINAL LAP" else "IN PROGRESS"}", style = RvhType.Meta, color = TextSecondary)
                }
                TextButton(onClick = { onOpen(movie) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(if (pct > 0) "RESUME" else "LAUNCH", style = RvhType.Meta, color = Color.White)
                }
                TextButton(onClick = { onDetails(movie) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp)) {
                    Text("INFO", style = RvhType.Meta, color = TextSecondary)
                }
            }
        }
        Text("SMART HANDOFF  •  HIGHEST PROGRESS FIRST  •  LOCAL ONLY", style = RvhType.Meta, color = TextSecondary)
    }
}
