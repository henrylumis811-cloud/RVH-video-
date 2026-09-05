package com.rvh.video.ui.components

/** "2h 15m" / "8m" style formatting, matching the mockup's duration badges. */
fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
