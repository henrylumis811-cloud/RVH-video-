package com.rvh.video.data.local

import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.rvh.video.data.model.VideoCollectionEntity
import com.rvh.video.data.model.PlaybackHistoryEntity

class VideoRepository(private val dao: LocalVideoDao, private val collectionDao: CollectionDao? = null, private val historyDao: PlaybackHistoryDao? = null) {

    /**
     * Videos effectively in [category]. Room performs the override/heuristic
     * merge and ordering in SQL so category screens receive one stable stream
     * without an extra combine/concat/sort pass on the collector thread.
     */
    fun observeCategory(category: VideoCategory): Flow<List<LocalVideoEntity>> =
        dao.observeCategory(category)

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

    fun observeByUri(uri: String): Flow<LocalVideoEntity?> = dao.observeByUri(uri)

    fun observeFavorites(): Flow<List<LocalVideoEntity>> = dao.observeFavorites()

    suspend fun setFavorite(uri: String, favorite: Boolean) = dao.setFavorite(uri, favorite)

    fun observeWatchLater(): Flow<List<LocalVideoEntity>> = dao.observeWatchLater()

    suspend fun setWatchLater(uri: String, watchLater: Boolean) = dao.setWatchLater(uri, watchLater)


    fun observeCollections(): Flow<List<VideoCollectionEntity>> = requireCollectionDao().observeCollections()

    suspend fun createCollection(name: String): Long = requireCollectionDao().create(VideoCollectionEntity(name = name.trim()))

    suspend fun createCollectionWithVideo(name: String, videoUri: String): Long =
        requireCollectionDao().createCollectionWithItem(name, videoUri)

    suspend fun renameCollection(id: Long, name: String) = requireCollectionDao().rename(id, name.trim())

    suspend fun deleteCollection(id: Long) = requireCollectionDao().deleteCollection(id)

    fun observeCollectionItems(id: Long): Flow<List<LocalVideoEntity>> = requireCollectionDao().observeItems(id)

    suspend fun addToCollection(collectionId: Long, videoUri: String) =
        requireCollectionDao().addItemAtNextPosition(collectionId, videoUri)

    suspend fun addFavoritesToCollection(collectionId: Long) {
        val favoriteUris = dao.observeFavorites().first().map { it.uri }
        requireCollectionDao().addItemsAtNextPositions(collectionId, favoriteUris)
    }

    suspend fun removeFromCollection(collectionId: Long, videoUri: String) = requireCollectionDao().removeItem(collectionId, videoUri)

    suspend fun moveInCollection(collectionId: Long, videoUri: String, direction: Int) =
        requireCollectionDao().moveItem(collectionId, videoUri, direction)

    fun observePlaybackHistory(limit: Int = 12): Flow<List<LocalVideoEntity>> = requireHistoryDao().observeRecentVideos(limit)

    /** Snapshot of raw playback history used by session-level feed planning. */
    suspend fun getPlaybackHistoryEntries(limit: Int = 1000): List<PlaybackHistoryEntity> =
        requireHistoryDao().observeRecent(limit.coerceAtLeast(1)).first()

    suspend fun recordPlayStart(uri: String) {
        requireHistoryDao().recordPlayStart(uri, System.currentTimeMillis())
    }

    suspend fun updatePlaybackHistory(uri: String, positionMs: Long, durationMs: Long, completed: Boolean) {
        val isCompleted = completed || (durationMs > 0 && positionMs >= durationMs * 0.95)
        requireHistoryDao().updatePlaybackState(
            uri = uri,
            positionMs = if (isCompleted) 0L else positionMs.coerceAtLeast(0L),
            playedAtEpochMs = System.currentTimeMillis(),
            completed = isCompleted,
        )
    }

    suspend fun clearPlaybackHistory() = requireHistoryDao().clear()

    private fun requireCollectionDao(): CollectionDao = collectionDao ?: error("Collection DAO not configured")
    private fun requireHistoryDao(): PlaybackHistoryDao = historyDao ?: error("Playback history DAO not configured")
}
