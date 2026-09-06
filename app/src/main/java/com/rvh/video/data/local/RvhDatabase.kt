package com.rvh.video.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rvh.video.data.model.CollectionItemEntity
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.data.model.PlaybackHistoryEntity
import com.rvh.video.data.model.VideoCollectionEntity

@Database(
    entities = [LocalVideoEntity::class, VideoCollectionEntity::class, CollectionItemEntity::class, PlaybackHistoryEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(VideoCategoryConverters::class)
abstract class RvhDatabase : RoomDatabase() {
    abstract fun localVideoDao(): LocalVideoDao
    abstract fun collectionDao(): CollectionDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_videos ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS video_collections (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS collection_items (collectionId INTEGER NOT NULL, videoUri TEXT NOT NULL, position INTEGER NOT NULL, addedAtEpochMs INTEGER NOT NULL, PRIMARY KEY(collectionId, videoUri))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_items_collectionId ON collection_items(collectionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_items_videoUri ON collection_items(videoUri)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_videos ADD COLUMN isWatchLater INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS playback_history (videoUri TEXT NOT NULL, lastPositionMs INTEGER NOT NULL, lastPlayedEpochMs INTEGER NOT NULL, playCount INTEGER NOT NULL, completed INTEGER NOT NULL, PRIMARY KEY(videoUri))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_lastPlayedEpochMs ON playback_history(lastPlayedEpochMs)")
            }
        }
        @Volatile private var instance: RvhDatabase? = null

        fun get(context: Context): RvhDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RvhDatabase::class.java,
                    "rvh_video.db"
                ) .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build().also { instance = it }
            }
    }
}
