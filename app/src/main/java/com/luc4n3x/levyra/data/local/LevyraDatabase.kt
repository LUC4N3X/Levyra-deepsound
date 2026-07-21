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
        LyricsCacheEntity::class,
        MotionArtworkEntity::class,
        PlaybackSourceMatchEntity::class,
        ArtistLoreEntity::class
    ],
    version = 12,
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
    abstract fun motionArtworkDao(): MotionArtworkDao
    abstract fun playbackSourceMatchDao(): PlaybackSourceMatchDao
    abstract fun artistLoreDao(): ArtistLoreDao

    companion object {
        @Volatile private var instance: LevyraDatabase? = null

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


        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite_tracks RENAME TO favorite_tracks_old")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorite_tracks (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        streamUrl TEXT NOT NULL,
                        videoUrl TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        largeThumbnailUrl TEXT NOT NULL,
                        source TEXT NOT NULL,
                        moodTags TEXT NOT NULL,
                        energy INTEGER NOT NULL,
                        vocal INTEGER NOT NULL,
                        replayScore INTEGER NOT NULL,
                        cacheScore INTEGER NOT NULL,
                        accentStart INTEGER NOT NULL,
                        accentEnd INTEGER NOT NULL,
                        youtubeLoudnessDb REAL,
                        youtubePerceptualLoudnessDb REAL,
                        isrc TEXT NOT NULL,
                        upc TEXT NOT NULL,
                        releaseDate TEXT NOT NULL,
                        year TEXT NOT NULL,
                        trackNumber INTEGER NOT NULL,
                        discNumber INTEGER NOT NULL,
                        explicit INTEGER NOT NULL,
                        albumBrowseId TEXT NOT NULL,
                        artistBrowseIds TEXT NOT NULL,
                        counterpartVideoId TEXT NOT NULL,
                        videoType TEXT NOT NULL,
                        metadataProvider TEXT NOT NULL,
                        metadataConfidence INTEGER NOT NULL,
                        canonicalAlbumUrl TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO favorite_tracks (
                        id, title, artist, album, durationMs, streamUrl, videoUrl,
                        thumbnailUrl, largeThumbnailUrl, source, moodTags, energy,
                        vocal, replayScore, cacheScore, accentStart, accentEnd,
                        youtubeLoudnessDb, youtubePerceptualLoudnessDb, isrc, upc,
                        releaseDate, year, trackNumber, discNumber, explicit,
                        albumBrowseId, artistBrowseIds, counterpartVideoId, videoType,
                        metadataProvider, metadataConfidence, canonicalAlbumUrl, createdAt
                    )
                    SELECT
                        id, title, artist, album, durationMs, streamUrl, videoUrl,
                        thumbnailUrl, largeThumbnailUrl, source, moodTags, energy,
                        vocal, replayScore, cacheScore, accentStart, accentEnd,
                        NULL, NULL, '', '', '', '', 0, 0, 0, '', '', '', '', '', 0, '', createdAt
                    FROM favorite_tracks_old
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE favorite_tracks_old")
                db.execSQL("ALTER TABLE playlist_tracks RENAME TO playlist_tracks_old")
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
                        youtubeLoudnessDb REAL,
                        youtubePerceptualLoudnessDb REAL,
                        isrc TEXT NOT NULL,
                        upc TEXT NOT NULL,
                        releaseDate TEXT NOT NULL,
                        year TEXT NOT NULL,
                        trackNumber INTEGER NOT NULL,
                        discNumber INTEGER NOT NULL,
                        explicit INTEGER NOT NULL,
                        albumBrowseId TEXT NOT NULL,
                        artistBrowseIds TEXT NOT NULL,
                        counterpartVideoId TEXT NOT NULL,
                        videoType TEXT NOT NULL,
                        metadataProvider TEXT NOT NULL,
                        metadataConfidence INTEGER NOT NULL,
                        canonicalAlbumUrl TEXT NOT NULL,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, trackId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO playlist_tracks (
                        playlistId, trackId, position, title, artist, album, durationMs,
                        videoUrl, thumbnailUrl, largeThumbnailUrl, source, accentStart,
                        accentEnd, youtubeLoudnessDb, youtubePerceptualLoudnessDb, isrc,
                        upc, releaseDate, year, trackNumber, discNumber, explicit,
                        albumBrowseId, artistBrowseIds, counterpartVideoId, videoType,
                        metadataProvider, metadataConfidence, canonicalAlbumUrl, addedAt
                    )
                    SELECT
                        playlistId, trackId, position, title, artist, album, durationMs,
                        videoUrl, thumbnailUrl, largeThumbnailUrl, source, accentStart,
                        accentEnd, NULL, NULL, '', '', '', '', 0, 0, 0, '', '', '', '', '', 0, '', addedAt
                    FROM playlist_tracks_old
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE playlist_tracks_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
            }
        }



        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE listen_events ADD COLUMN artistBrowseIds TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS motion_artwork (
                        identityKey TEXT NOT NULL PRIMARY KEY,
                        provider TEXT,
                        url TEXT,
                        mimeType TEXT,
                        width INTEGER,
                        height INTEGER,
                        confidence INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        lastVerifiedAt INTEGER NOT NULL,
                        configEpoch INTEGER NOT NULL,
                        negative INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_motion_artwork_expiresAt ON motion_artwork(expiresAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_motion_artwork_lastVerifiedAt ON motion_artwork(lastVerifiedAt)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloaded_tracks ADD COLUMN downloadPreset TEXT NOT NULL DEFAULT 'Legacy'")
                db.execSQL("ALTER TABLE downloaded_tracks ADD COLUMN downloadQuality TEXT NOT NULL DEFAULT 'Unknown'")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_downloaded_tracks_trackId_downloadPreset_downloadQuality " +
                        "ON downloaded_tracks(trackId, downloadPreset, downloadQuality)"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playback_source_matches (
                        matchKey TEXT NOT NULL PRIMARY KEY,
                        canonicalKey TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        audioQuality TEXT NOT NULL,
                        sourceVideoId TEXT NOT NULL,
                        sourceVideoUrl TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        manifestJson TEXT NOT NULL,
                        confidence INTEGER NOT NULL,
                        successCount INTEGER NOT NULL,
                        failureCount INTEGER NOT NULL,
                        blockedUntil INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastValidatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_source_matches_canonicalKey ON playback_source_matches(canonicalKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_source_matches_sourceVideoId ON playback_source_matches(sourceVideoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_source_matches_updatedAt ON playback_source_matches(updatedAt)")
            }
        }


        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS artist_lore (
                        cacheKey TEXT NOT NULL PRIMARY KEY,
                        artistKey TEXT NOT NULL,
                        browseId TEXT NOT NULL,
                        languageCode TEXT NOT NULL,
                        text TEXT NOT NULL,
                        description TEXT NOT NULL,
                        pageTitle TEXT NOT NULL,
                        pageId INTEGER NOT NULL,
                        entityId TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        originalImageUrl TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        confidence INTEGER NOT NULL,
                        negative INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastAccessedAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        staleUntil INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_lore_artistKey_languageCode ON artist_lore(artistKey, languageCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_lore_browseId_languageCode ON artist_lore(browseId, languageCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_lore_expiresAt ON artist_lore(expiresAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_lore_lastAccessedAt ON artist_lore(lastAccessedAt)")
            }
        }

        fun get(context: Context): LevyraDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LevyraDatabase::class.java,
                    "levyra.db"
                )

                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
