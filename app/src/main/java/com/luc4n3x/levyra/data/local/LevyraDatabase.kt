package com.luc4n3x.levyra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteTrackEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LevyraDatabase : RoomDatabase() {
    abstract fun favoriteTracksDao(): FavoriteTracksDao
    abstract fun downloadedTracksDao(): DownloadedTracksDao

    companion object {
        @Volatile private var instance: LevyraDatabase? = null

        fun get(context: Context): LevyraDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LevyraDatabase::class.java,
                    "levyra.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
