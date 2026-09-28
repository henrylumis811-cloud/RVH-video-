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
     * Returns the effective category directly from Room. A manual override wins;
     * otherwise the classifier result is used. Keeping the merge and ordering in
     * SQL avoids combining two full lists and sorting them again on the collector
     * thread every time either source changes.
     */
    @Query("""
        SELECT * FROM local_videos
        WHERE userOverrideCategory = :category
           OR (userOverrideCategory IS NULL AND heuristicCategory = :category)
        ORDER BY dateModifiedEpochSeconds DESC
    """)
    fun observeCategory(category: VideoCategory): Flow<List<LocalVideoEntity>>

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

    /** Inserts only genuinely new media rows. Existing rows are updated through
     * updateScanMetadata so a long-running scan can never replace newer
     * user-owned state such as resume position, favorites, or queue position. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewVideos(videos: List<LocalVideoEntity>)

    /** Updates only fields owned by the MediaStore scanner/classifier. */
    @Query("""
        UPDATE local_videos SET
            displayName = :displayName,
            durationMs = :durationMs,
            widthPx = :widthPx,
            heightPx = :heightPx,
            folderName = :folderName,
            dateModifiedEpochSeconds = :dateModifiedEpochSeconds,
            heuristicCategory = :heuristicCategory,
            classifiedAtEpochSeconds = :classifiedAtEpochSeconds
        WHERE uri = :uri
    """)
    suspend fun updateScanMetadata(
        uri: String,
        displayName: String,
        durationMs: Long,
        widthPx: Int,
        heightPx: Int,
        folderName: String,
        dateModifiedEpochSeconds: Long,
        heuristicCategory: VideoCategory,
        classifiedAtEpochSeconds: Long,
    )

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

    /** Deletes only rows known to be absent from a successful MediaStore snapshot.
     * Callers batch this operation so large libraries never exceed SQLite's bind-variable limit.
     */
    @Query("DELETE FROM local_videos WHERE uri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)
}

/** Lightweight projection used to decide which files need re-classification during a scan. */
data class ScanFingerprint(
    val uri: String,
    val dateModifiedEpochSeconds: Long,
    val classifiedAtEpochSeconds: Long,
)
