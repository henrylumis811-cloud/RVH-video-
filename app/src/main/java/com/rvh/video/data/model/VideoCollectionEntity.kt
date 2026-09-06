package com.rvh.video.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_collections")
data class VideoCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
