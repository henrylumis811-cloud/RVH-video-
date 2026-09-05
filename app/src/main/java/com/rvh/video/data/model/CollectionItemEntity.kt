package com.rvh.video.data.model

import androidx.room.Entity

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "videoUri"],
)
data class CollectionItemEntity(
    val collectionId: Long,
    val videoUri: String,
    val position: Int,
    val addedAtEpochMs: Long = System.currentTimeMillis(),
)
