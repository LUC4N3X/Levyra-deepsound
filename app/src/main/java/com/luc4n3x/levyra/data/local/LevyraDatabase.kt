package com.luc4n3x.levyra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteTrackEntity::class,
        DownloadEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        ListenEventEntity::class,
        PlaybackQueueItemEntity::class,
        PlaybackQueueStateEntity::class,
        OfflineDownloadTaskEntity::class,
        LyricsCacheEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class LevyraDatabase : RoomDatabase() {
    abstract fun favoriteTracksDao(): FavoriteTracksDao
    abstract fun downloadedTracksDao(): DownloadedTracksDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun listenEventsDao(): ListenEventsDao
    abstract fun playbackQueueDao(): PlaybackQueueDao
    abstract fun offlineDownloadTasksDao(): OfflineDownloadTasksDao
    abstract fun lyricsCacheDao(): LyricsCacheDao

    companion object {
        @Volatile private var instance: LevyraDatabase? = null

        /**
         * Migrazione 1 -> 2: aggiunge le tabelle delle playlist SENZA toccare i dati esistenti
         * (preferiti e download restano intatti). È il motivo per cui un aggiornamento dell'app
         * non fa più "ricominciare da capo".
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlists (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        coverUrl TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_tracks (
                        playlistId TEXT NOT NULL,
                        trackId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        videoUrl TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        largeThumbnailUrl TEXT NOT NULL,
                        source TEXT NOT NULL,
                        accentStart INTEGER NOT NULL,
                        accentEnd INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, trackId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listen_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trackId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        videoUrl TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        largeThumbnailUrl TEXT NOT NULL,
                        source TEXT NOT NULL,
                        listenedMs INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listen_events_startedAt ON listen_events(startedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listen_events_trackId ON listen_events(trackId)")
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playback_queue_items (
                        position INTEGER NOT NULL PRIMARY KEY,
                        payload TEXT NOT NULL,
                        identity TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playback_queue_state (
                        singletonId INTEGER NOT NULL PRIMARY KEY,
                        currentIndex INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        shuffleEnabled INTEGER NOT NULL,
                        shuffleOrder TEXT NOT NULL,
                        shuffleCursor INTEGER NOT NULL,
                        history TEXT NOT NULL,
                        repeatMode TEXT NOT NULL,
                        radioEnabled INTEGER NOT NULL,
                        generation INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_download_tasks (
                        taskKey TEXT NOT NULL PRIMARY KEY,
                        trackId TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        state TEXT NOT NULL,
                        progress INTEGER NOT NULL,
                        workId TEXT NOT NULL,
                        error TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }


        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS lyrics_cache (
                        cacheKey TEXT NOT NULL PRIMARY KEY,
                        titleKey TEXT NOT NULL,
                        artistKey TEXT NOT NULL,
                        durationBucket INTEGER NOT NULL,
                        videoId TEXT NOT NULL,
                        languageCode TEXT NOT NULL,
                        translate INTEGER NOT NULL,
                        synced INTEGER NOT NULL,
                        provider TEXT NOT NULL,
                        confidence INTEGER NOT NULL,
                        payload TEXT NOT NULL,
                        negative INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastAccessedAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lyrics_cache_expiresAt ON lyrics_cache(expiresAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lyrics_cache_lastAccessedAt ON lyrics_cache(lastAccessedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lyrics_cache_titleKey_artistKey_durationBucket_languageCode_translate ON lyrics_cache(titleKey, artistKey, durationBucket, languageCode, translate)")
            }
        }

        fun get(context: Context): LevyraDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LevyraDatabase::class.java,
                    "levyra.db"
                )
                    // Migrazioni versionate: i dati utente sopravvivono agli aggiornamenti.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
