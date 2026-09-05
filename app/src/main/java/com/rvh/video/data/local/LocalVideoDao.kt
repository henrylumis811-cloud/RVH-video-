package com.rvh.video.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.VideoCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalVideoDao {

    /**
     * Effective category has to be computed in Kotlin (COALESCE would work in
     * raw SQL too, but keeping the "override wins" rule in one place — the
     * entity's effectiveCategory getter — avoids the rule drifting between
     * SQL and Kotlin as the app grows). So we fetch by BOTH possible sources
     * and filter in the repository layer instead of a single SQL WHERE.
     */
    @Query("SELECT * FROM local_videos WHERE userOverrideCategory = :category")
    fun observeOverriddenAs(category: VideoCategory): Flow<List<LocalVideoEntity>>

    @Query("SELECT * FROM local_videos WHERE userOverrideCategory IS NULL AND heuristicCategory = :category")
    fun observeHeuristicAs(category: VideoCategory): Flow<List<LocalVideoEntity>>

    @Query("SELECT * FROM local_videos ORDER BY dateModifiedEpochSeconds DESC")
    fun observeAll(): Flow<List<LocalVideoEntity>>

    @Query("SELECT * FROM local_videos WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): LocalVideoEntity?

    @Query("SELECT uri, dateModifiedEpochSeconds, classifiedAtEpochSeconds FROM local_videos")
    suspend fun getScanFingerprints(): List<ScanFingerprint>

    @Query("SELECT * FROM local_videos ORDER BY dateModifiedEpochSeconds DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<LocalVideoEntity>>

    @Query("SELECT * FROM local_videos WHERE resumePositionMs > 0 ORDER BY dateModifiedEpochSeconds DESC LIMIT :limit")
    fun observeContinueWatching(limit: Int): Flow<List<LocalVideoEntity>>

    @Query("SELECT COUNT(*) FROM local_videos")
    fun observeLibraryCount(): Flow<Int>

    @Query("SELECT * FROM local_videos WHERE folderName = :folder ORDER BY dateModifiedEpochSeconds DESC")
    fun observeFolder(folder: String): Flow<List<LocalVideoEntity>>

    @Query("SELECT * FROM local_videos WHERE isFavorite = 1 ORDER BY dateModifiedEpochSeconds DESC")
    fun observeFavorites(): Flow<List<LocalVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(videos: List<LocalVideoEntity>)

    @Update
    suspend fun update(video: LocalVideoEntity)

    @Query("UPDATE local_videos SET resumePositionMs = :positionMs WHERE uri = :uri")
    suspend fun updateResumePosition(uri: String, positionMs: Long)

    @Query("UPDATE local_videos SET userOverrideCategory = :category WHERE uri = :uri")
    suspend fun setOverride(uri: String, category: VideoCategory?)

    @Query("UPDATE local_videos SET isFavorite = :favorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, favorite: Boolean)

    @Query("SELECT * FROM local_videos WHERE isWatchLater = 1 ORDER BY dateModifiedEpochSeconds DESC")
    fun observeWatchLater(): Flow<List<LocalVideoEntity>>

    @Query("UPDATE local_videos SET isWatchLater = :watchLater WHERE uri = :uri")
    suspend fun setWatchLater(uri: String, watchLater: Boolean)

    @Query("DELETE FROM local_videos WHERE uri NOT IN (:stillPresentUris)")
    suspend fun pruneMissing(stillPresentUris: List<String>)
}

/** Lightweight projection used to decide which files need re-classification during a scan. */
data class ScanFingerprint(
    val uri: String,
    val dateModifiedEpochSeconds: Long,
    val classifiedAtEpochSeconds: Long,
)
