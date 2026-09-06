package com.rvh.video.data.local

import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import com.rvh.video.data.model.VideoCollectionEntity
import com.rvh.video.data.model.CollectionItemEntity

class VideoRepository(private val dao: LocalVideoDao, private val collectionDao: CollectionDao? = null, private val historyDao: PlaybackHistoryDao? = null) {

    /**
     * Videos effectively in [category]: either heuristically classified there
     * (and not overridden elsewhere) or explicitly overridden into it.
     * This is the query every screen (Shorts/Movies/Music) actually uses.
     */
    fun observeCategory(category: VideoCategory): Flow<List<LocalVideoEntity>> =
        combine(
            dao.observeHeuristicAs(category),
            dao.observeOverriddenAs(category)
        ) { heuristic, overridden ->
            // heuristic query already excludes overridden rows (userOverrideCategory IS NULL),
            // so a simple concat can't double-count.
            (heuristic + overridden).sortedByDescending { it.dateModifiedEpochSeconds }
        }

    suspend fun saveResumePosition(uri: String, positionMs: Long) =
        dao.updateResumePosition(uri, positionMs)

    /** Long-press "Move to X" action. Passing null clears the override, reverting to the heuristic result. */
    suspend fun setCategoryOverride(uri: String, category: VideoCategory?) =
        dao.setOverride(uri, category)

    fun observeAll(): Flow<List<LocalVideoEntity>> = dao.observeAll()

    fun observeRecentlyAdded(limit: Int): Flow<List<LocalVideoEntity>> =
        dao.observeRecentlyAdded(limit)

    fun observeContinueWatching(limit: Int): Flow<List<LocalVideoEntity>> =
        dao.observeContinueWatching(limit)

    fun observeLibraryCount(): Flow<Int> = dao.observeLibraryCount()

    fun observeFolder(folder: String): Flow<List<LocalVideoEntity>> = dao.observeFolder(folder)

    suspend fun getByUri(uri: String): LocalVideoEntity? = dao.getByUri(uri)

    fun observeFavorites(): Flow<List<LocalVideoEntity>> = dao.observeFavorites()

    suspend fun setFavorite(uri: String, favorite: Boolean) = dao.setFavorite(uri, favorite)

    fun observeWatchLater(): Flow<List<LocalVideoEntity>> = dao.observeWatchLater()

    suspend fun setWatchLater(uri: String, watchLater: Boolean) = dao.setWatchLater(uri, watchLater)


    fun observeCollections(): Flow<List<VideoCollectionEntity>> = requireCollectionDao().observeCollections()

    suspend fun createCollection(name: String): Long = requireCollectionDao().create(VideoCollectionEntity(name = name.trim()))

    suspend fun renameCollection(id: Long, name: String) = requireCollectionDao().rename(id, name.trim())

    suspend fun deleteCollection(id: Long) = requireCollectionDao().delete(id)

    fun observeCollectionItems(id: Long): Flow<List<LocalVideoEntity>> = requireCollectionDao().observeItems(id)

    suspend fun addToCollection(collectionId: Long, videoUri: String) {
        val collection = requireCollectionDao()
        collection.addItem(CollectionItemEntity(collectionId, videoUri, collection.nextPosition(collectionId)))
    }

    suspend fun addFavoritesToCollection(collectionId: Long) {
        val collection = requireCollectionDao()
        dao.observeFavorites().first().forEach { video ->
            collection.addItem(CollectionItemEntity(collectionId, video.uri, collection.nextPosition(collectionId)))
        }
    }

    suspend fun removeFromCollection(collectionId: Long, videoUri: String) = requireCollectionDao().removeItem(collectionId, videoUri)

    suspend fun moveInCollection(collectionId: Long, videoUri: String, direction: Int) =
        requireCollectionDao().moveItem(collectionId, videoUri, direction)

    fun observePlaybackHistory(limit: Int = 12): Flow<List<LocalVideoEntity>> = requireHistoryDao().observeRecentVideos(limit)

    suspend fun recordPlayStart(uri: String) {
        val history = requireHistoryDao()
        val existing = history.get(uri)
        history.upsert(
            com.rvh.video.data.model.PlaybackHistoryEntity(
                videoUri = uri,
                lastPositionMs = existing?.lastPositionMs ?: 0L,
                lastPlayedEpochMs = System.currentTimeMillis(),
                playCount = (existing?.playCount ?: 0) + 1,
                completed = false,
            )
        )
    }

    suspend fun updatePlaybackHistory(uri: String, positionMs: Long, durationMs: Long, completed: Boolean) {
        val history = requireHistoryDao()
        val existing = history.get(uri) ?: return
        val isCompleted = completed || (durationMs > 0 && positionMs >= durationMs * 0.95)
        history.upsert(existing.copy(
            lastPositionMs = if (isCompleted) 0L else positionMs.coerceAtLeast(0L),
            lastPlayedEpochMs = System.currentTimeMillis(),
            completed = isCompleted,
        ))
    }

    suspend fun clearPlaybackHistory() = requireHistoryDao().clear()

    private fun requireCollectionDao(): CollectionDao = collectionDao ?: error("Collection DAO not configured")
    private fun requireHistoryDao(): PlaybackHistoryDao = historyDao ?: error("Playback history DAO not configured")
}
