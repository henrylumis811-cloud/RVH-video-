package com.rvh.video.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val videoUri: String,
    val lastPositionMs: Long = 0L,
    val lastPlayedEpochMs: Long,
    val playCount: Int = 1,
    val completed: Boolean = false,
)
