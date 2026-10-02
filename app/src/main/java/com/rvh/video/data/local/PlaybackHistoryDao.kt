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

    /** Atomically records a new play start without a read-modify-write race. */
    @Query("""
        INSERT INTO playback_history(videoUri, lastPositionMs, lastPlayedEpochMs, playCount, completed)
        VALUES(:uri, 0, :playedAtEpochMs, 1, 0)
        ON CONFLICT(videoUri) DO UPDATE SET
            lastPlayedEpochMs = excluded.lastPlayedEpochMs,
            playCount = playback_history.playCount + 1,
            completed = 0
    """)
    suspend fun recordPlayStart(uri: String, playedAtEpochMs: Long)

    /** Updates an existing history row without first reading it into memory. */
    @Query("UPDATE playback_history SET lastPositionMs = :positionMs, lastPlayedEpochMs = :playedAtEpochMs, completed = :completed WHERE videoUri = :uri")
    suspend fun updatePlaybackState(uri: String, positionMs: Long, playedAtEpochMs: Long, completed: Boolean)

    @Query("DELETE FROM playback_history")
    suspend fun clear()

    @Query("DELETE FROM playback_history WHERE videoUri = :uri")
    suspend fun remove(uri: String)

    @Query("SELECT local_videos.* FROM local_videos INNER JOIN playback_history ON local_videos.uri = playback_history.videoUri ORDER BY playback_history.lastPlayedEpochMs DESC LIMIT :limit")
    fun observeRecentVideos(limit: Int): Flow<List<LocalVideoEntity>>
}
