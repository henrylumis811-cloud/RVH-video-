package com.rvh.video.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * One type scale, used everywhere. The mockup's "expensive" feel comes
 * partly from never inventing a new font size per-screen — every label
 * across Shorts/Movies/Music maps to one of these five roles.
 */
object RvhType {
    val ScreenTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val CardTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val Meta = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)      // timestamps, durations, years
    val Stat = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)     // like counts, comment counts
}

val AppTypography = Typography(
    titleLarge = RvhType.ScreenTitle,
    titleMedium = RvhType.CardTitle,
    bodyLarge = RvhType.Body,
    bodyMedium = RvhType.Body,
    labelSmall = RvhType.Meta,
    labelMedium = RvhType.Stat,
)
