package com.rvh.video.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import com.rvh.video.data.model.CollectionItemEntity
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM video_collections ORDER BY createdAtEpochMs DESC")
    fun observeCollections(): Flow<List<VideoCollectionEntity>>

    @Insert
    suspend fun create(collection: VideoCollectionEntity): Long

    @Transaction
    suspend fun createCollectionWithItem(name: String, videoUri: String): Long {
        val collectionId = create(VideoCollectionEntity(name = name.trim()))
        addItem(CollectionItemEntity(collectionId, videoUri, 0))
        return collectionId
    }

    @Query("UPDATE video_collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM video_collections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId")
    suspend fun deleteItems(collectionId: Long)

    @Transaction
    suspend fun deleteCollection(id: Long) {
        deleteItems(id)
        delete(id)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(item: CollectionItemEntity)

    @Transaction
    suspend fun addItemAtNextPosition(collectionId: Long, videoUri: String) {
        addItem(CollectionItemEntity(collectionId, videoUri, nextPosition(collectionId)))
    }

    @Transaction
    suspend fun addItemsAtNextPositions(collectionId: Long, videoUris: List<String>) {
        if (videoUris.isEmpty()) return
        var position = nextPosition(collectionId)
        val existing = getVideoUris(collectionId).toHashSet()
        for (uri in videoUris.distinct()) {
            if (existing.add(uri)) {
                addItem(CollectionItemEntity(collectionId, uri, position))
                position++
            }
        }
    }

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM collection_items WHERE collectionId = :collectionId")
    suspend fun nextPosition(collectionId: Long): Int

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM local_videos INNER JOIN collection_items ON local_videos.uri = collection_items.videoUri WHERE collection_items.collectionId = :collectionId ORDER BY collection_items.position ASC")
    fun observeItems(collectionId: Long): Flow<List<LocalVideoEntity>>

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND videoUri = :videoUri")
    suspend fun removeItem(collectionId: Long, videoUri: String)

    @Query("DELETE FROM collection_items WHERE videoUri = :videoUri")
    suspend fun removeVideoFromAll(videoUri: String)

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY position ASC")
    suspend fun getItems(collectionId: Long): List<CollectionItemEntity>

    @Query("SELECT videoUri FROM collection_items WHERE collectionId = :collectionId")
    suspend fun getVideoUris(collectionId: Long): List<String>

    @Query("UPDATE collection_items SET position = :position WHERE collectionId = :collectionId AND videoUri = :videoUri")
    suspend fun setPosition(collectionId: Long, videoUri: String, position: Int)

    @Transaction
    suspend fun moveItem(collectionId: Long, videoUri: String, direction: Int) {
        val items = getItems(collectionId)
        val currentIndex = items.indexOfFirst { it.videoUri == videoUri }
        val targetIndex = currentIndex + direction
        if (currentIndex < 0 || targetIndex !in items.indices) return
        val current = items[currentIndex]
        val target = items[targetIndex]
        setPosition(collectionId, current.videoUri, target.position)
        setPosition(collectionId, target.videoUri, current.position)
    }
}
