package com.rvh.video.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE videoUri = :uri LIMIT 1")
    suspend fun get(uri: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clear()

    @Query("DELETE FROM playback_history WHERE videoUri = :uri")
    suspend fun remove(uri: String)

    @Query("SELECT local_videos.* FROM local_videos INNER JOIN playback_history ON local_videos.uri = playback_history.videoUri ORDER BY playback_history.lastPlayedEpochMs DESC LIMIT :limit")
    fun observeRecentVideos(limit: Int): Flow<List<LocalVideoEntity>>
}
