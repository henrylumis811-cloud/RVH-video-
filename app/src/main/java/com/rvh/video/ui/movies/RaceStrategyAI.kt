package com.rvh.video.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface

@Composable
fun RaceStrategyAI(
    movies: List<LocalVideoEntity>,
    onOpenMovie: (String, Long) -> Unit,
) {
    val recommendations = movies
        .sortedByDescending { score(it, movies) }
        .take(3)
    val bestScore = recommendations.firstOrNull()?.let { score(it, movies) } ?: 0.0
    val confidence = bestScore.coerceIn(0.0, 100.0).toInt()

    Column(
        modifier = Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(14.dp), 12.dp).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("RACE STRATEGY AI 2.0", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
            Text(if (recommendations.isEmpty()) "STANDBY" else "$confidence% CONFIDENCE", style = RvhType.Meta, color = TextSecondary)
        }
        if (recommendations.isEmpty()) {
            Text("NO MEDIA AVAILABLE  •  SCAN LIBRARY", style = RvhType.Meta, color = TextSecondary)
        } else {
            recommendations.forEachIndexed { index, movie ->
                val scoreValue = score(movie, movies).coerceIn(0.0, 100.0).toInt()
                val reason = reasonFor(movie)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = if (index == 0) .09f else .045f), RoundedCornerShape(10.dp))
                        .clickable { onOpenMovie(movie.uri, movie.resumePositionMs) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${index + 1}", style = RvhType.CardTitle, color = Color.White)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(movie.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                        Text(reason, style = RvhType.Meta, color = TextSecondary, maxLines = 1)
                    }
                    Text("$scoreValue%", style = RvhType.Meta, color = Color.White)
                }
            }
            Text("ADAPTIVE TOP 3  •  TAP ANY RUN TO LAUNCH", style = RvhType.Meta, color = TextSecondary)
        }
    }
}

private fun reasonFor(movie: LocalVideoEntity): String = when {
    movie.resumePositionMs > 0L && movie.durationMs > 0L && movie.resumePositionMs < movie.durationMs -> "CONTINUE MOMENTUM"
    movie.isWatchLater -> "QUEUE INTENT"
    movie.isFavorite -> "PERSONAL PICK"
    else -> "FRESHNESS BOOST"
}

private fun score(movie: LocalVideoEntity, movies: List<LocalVideoEntity>): Double {
    val ratio = if (movie.durationMs > 0L) movie.resumePositionMs.toDouble() / movie.durationMs else 0.0
    val resume = if (ratio > 0.0 && ratio < 1.0) 55.0 + ratio * 25.0 else 0.0
    val later = if (movie.isWatchLater) 16.0 else 0.0
    val favorite = if (movie.isFavorite) 10.0 else 0.0
    val newest = if (movies.isEmpty()) 0.0 else {
        val maxModified = movies.maxOf { it.dateModifiedEpochSeconds }
        val age = (maxModified - movie.dateModifiedEpochSeconds).coerceAtLeast(0L)
        (8.0 - (age / 86400.0)).coerceIn(0.0, 8.0)
    }
    return resume + later + favorite + newest
}
