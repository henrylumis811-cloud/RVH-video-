package com.rvh.video.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "videoUri"],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["collectionId", "position"]),
        Index(value = ["videoUri"]),
    ],
)
data class CollectionItemEntity(
    val collectionId: Long,
    val videoUri: String,
    val position: Int,
    val addedAtEpochMs: Long = System.currentTimeMillis(),
)
