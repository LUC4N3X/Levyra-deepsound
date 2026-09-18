package com.luc4n3x.levyra.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevyraDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate15To16KeepsUserDataAndAddsLifetimeTables() {
        helper.createDatabase(TEST_DB, 15).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) " +
                    "VALUES ('p1', 'Road trip', '', 100, 100)"
            )
            db.execSQL(
                "INSERT INTO listen_events " +
                    "(trackId, title, artist, album, durationMs, videoUrl, thumbnailUrl, " +
                    "largeThumbnailUrl, source, listenedMs, completed, startedAt, artistBrowseIds) " +
                    "VALUES ('t1', 'Song', 'Artist', 'Album', 200000, '', '', '', 'yt', 45000, 1, 1000, '')"
            )
            db.execSQL(
                "INSERT INTO favorite_tracks (id, title, artist, album, durationMs, streamUrl, " +
                    "videoUrl, thumbnailUrl, largeThumbnailUrl, source, moodTags, energy, vocal, " +
                    "replayScore, cacheScore, accentStart, accentEnd, youtubeLoudnessDb, " +
                    "youtubePerceptualLoudnessDb, isrc, upc, releaseDate, year, trackNumber, " +
                    "discNumber, explicit, albumBrowseId, artistBrowseIds, counterpartVideoId, " +
                    "videoType, metadataProvider, metadataConfidence, canonicalAlbumUrl, createdAt) " +
                    "VALUES ('f1', 'Kept', 'Artist', 'Album', 200000, '', '', '', '', 'yt', '', 50, 50, " +
                    "50, 50, 0, 0, 0.0, 0.0, '', '', '', 0, 0, 0, 0, '', '', '', '', '', 0.0, '', 10)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 16, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Road trip", cursor.getString(0))
        }
        migrated.query("SELECT listenedMs, completed FROM listen_events WHERE trackId = 't1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(45000L, cursor.getLong(0))
            assertEquals(1, cursor.getInt(1))
        }
        migrated.query("SELECT title FROM favorite_tracks WHERE id = 'f1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Kept", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM listen_lifetime_tracks").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM listen_lifetime_artists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrate16To17KeepsUserDataAndAddsRecognitionHistory() {
        helper.createDatabase(TEST_DB, 16).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) " +
                    "VALUES ('p2', 'Recognition saves', '', 200, 200)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 17, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Recognition saves", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM recognition_history").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("PRAGMA index_list('recognition_history')").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "index_recognition_history_recognizedAt") found = true
            }
            assertTrue(found)
        }
        migrated.close()
    }

    @Test
    fun migrate17To18KeepsUserDataAndAddsFollowedArtists() {
        helper.createDatabase(TEST_DB, 17).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) " +
                    "VALUES ('p3', 'Followed releases', '', 300, 300)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 18, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p3'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Followed releases", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM followed_artists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrate18To19KeepsUserDataAndAddsOrganizationTables() {
        helper.createDatabase(TEST_DB, 18).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) " +
                    "VALUES ('p4', 'Gym mix', 'cover', 400, 400)"
            )
            db.execSQL(
                "INSERT INTO playlist_tracks " +
                    "(playlistId, trackId, position, title, artist, album, durationMs, videoUrl, " +
                    "thumbnailUrl, largeThumbnailUrl, source, accentStart, accentEnd, " +
                    "youtubeLoudnessDb, youtubePerceptualLoudnessDb, isrc, upc, releaseDate, year, " +
                    "trackNumber, discNumber, explicit, albumBrowseId, artistBrowseIds, " +
                    "counterpartVideoId, videoType, metadataProvider, metadataConfidence, " +
                    "canonicalAlbumUrl, addedAt) " +
                    "VALUES ('p4', 't4', 0, 'Kept track', 'Artist', 'Album', 200000, '', '', '', " +
                    "'yt', 0, 0, NULL, NULL, '', '', '', '', 0, 0, 0, '', '', '', '', '', 0, '', 400)"
            )
            db.execSQL(
                "INSERT INTO followed_artists (artistKey, browseId, name, thumbnailUrl, followedAt) " +
                    "VALUES ('UC1', 'UC1', 'Artist', '', 400)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 19, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name, coverUrl, hidden FROM playlists WHERE id = 'p4'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Gym mix", cursor.getString(0))
            assertEquals("cover", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
        }
        migrated.query("SELECT title FROM playlist_tracks WHERE playlistId = 'p4'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Kept track", cursor.getString(0))
        }
        migrated.query("SELECT name FROM followed_artists WHERE artistKey = 'UC1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Artist", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM playlist_tags").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM playlist_tag_links").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM excluded_artists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrate19To20KeepsUserDataAndAddsRecommendationFeedback() {
        helper.createDatabase(TEST_DB, 19).use { db ->
            db.execSQL(
                "INSERT INTO excluded_artists (artistKey, browseId, name, excludedAt) " +
                    "VALUES ('UC2', 'UC2', 'Excluded artist', 500)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 20, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM excluded_artists WHERE artistKey = 'UC2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Excluded artist", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM recommendation_feedback").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrate20To21KeepsPlaylistsAndDefaultsExistingCoversToAutomatic() {
        helper.createDatabase(TEST_DB, 20).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt, hidden) " +
                    "VALUES ('p5', 'Existing mix', 'https://example.test/cover.jpg', 600, 700, 0)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 21, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name, coverUrl, coverMode FROM playlists WHERE id = 'p5'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Existing mix", cursor.getString(0))
            assertEquals("https://example.test/cover.jpg", cursor.getString(1))
            assertEquals("AUTO", cursor.getString(2))
        }
        migrated.close()
    }

    @Test
    fun migrate21To22MovesTheExistingQueueIntoTheDefaultQueueSpace() {
        helper.createDatabase(TEST_DB, 21).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt, hidden, coverMode) " +
                    "VALUES ('p6', 'Kept playlist', '', 10, 20, 0, 'AUTO')"
            )
            db.execSQL(
                "INSERT INTO playback_queue_items (position, payload, identity) " +
                    "VALUES (0, '{\"id\":\"a\"}', 'yt:a'), (1, '{\"id\":\"b\"}', 'yt:b')"
            )
            db.execSQL(
                "INSERT INTO playback_queue_state (singletonId, currentIndex, positionMs, shuffleEnabled, " +
                    "shuffleOrder, shuffleCursor, history, repeatMode, radioEnabled, generation, updatedAt) " +
                    "VALUES (1, 1, 45000, 0, '', -1, '0', 'All', 1, 7, 1234)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 23, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p6'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Kept playlist", cursor.getString(0))
        }
        migrated.query("SELECT id, isActive, trackCount FROM queue_spaces").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(DEFAULT_QUEUE_SPACE_ID, cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(2, cursor.getInt(2))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT spaceId, position, identity FROM playback_queue_items ORDER BY position ASC")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(DEFAULT_QUEUE_SPACE_ID, cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals("yt:a", cursor.getString(2))
                assertTrue(cursor.moveToNext())
                assertEquals("yt:b", cursor.getString(2))
                assertEquals(2, cursor.count)
            }
        migrated.query(
            "SELECT spaceId, currentIndex, positionMs, repeatMode, radioEnabled, generation FROM playback_queue_state"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(DEFAULT_QUEUE_SPACE_ID, cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(45_000L, cursor.getLong(2))
            assertEquals("All", cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(7L, cursor.getLong(5))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT COUNT(*) FROM local_media").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrate21To22WithoutAnyQueueStillCreatesTheDefaultQueueSpace() {
        helper.createDatabase(TEST_DB, 21).use { db ->
            db.execSQL(
                "INSERT INTO favorite_tracks (id, title, artist, album, durationMs, streamUrl, " +
                    "videoUrl, thumbnailUrl, largeThumbnailUrl, source, moodTags, energy, vocal, " +
                    "replayScore, cacheScore, accentStart, accentEnd, youtubeLoudnessDb, " +
                    "youtubePerceptualLoudnessDb, isrc, upc, releaseDate, year, trackNumber, " +
                    "discNumber, explicit, albumBrowseId, artistBrowseIds, counterpartVideoId, " +
                    "videoType, metadataProvider, metadataConfidence, canonicalAlbumUrl, createdAt) " +
                    "VALUES ('f9', 'Kept favorite', 'Artist', 'Album', 200000, '', '', '', '', 'yt', '', 50, 50, " +
                    "50, 50, 0, 0, 0.0, 0.0, '', '', '', 0, 0, 0, 0, '', '', '', '', '', 0.0, '', 10)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 22, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT title FROM favorite_tracks WHERE id = 'f9'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Kept favorite", cursor.getString(0))
        }
        migrated.query("SELECT id, trackCount FROM queue_spaces").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(DEFAULT_QUEUE_SPACE_ID, cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        migrated.query("SELECT COUNT(*) FROM playback_queue_state").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migrate22To23KeepsLocalMediaAndAddsDeepTagColumns() {
        helper.createDatabase(TEST_DB, 22).use { db ->
            db.execSQL(
                "INSERT INTO local_media (" +
                    "identityKey, contentUri, volumeName, mediaStoreId, filePath, relativePath, displayName, " +
                    "folderKey, folderName, title, artist, album, albumArtist, genre, year, trackNumber, " +
                    "discNumber, durationMs, mimeType, bitrate, sizeBytes, dateAddedMs, dateModifiedMs, albumId, " +
                    "albumKey, artistKey, contentFingerprint, levyraTrackId, isLevyraDownload, available, " +
                    "missingSince, lastSeenAt) VALUES (" +
                    "'ms:external_primary:10', 'content://media/external_primary/audio/media/10', " +
                    "'external_primary', 10, '/storage/emulated/0/Music/test.m4a', 'Music/', 'test.m4a', " +
                    "'external_primary:music/', 'Music', 'Kept local song', 'Artist', 'Album', 'Artist', " +
                    "'Ambient', 2026, 3, 1, 180000, 'audio/mp4', 256000, 4000000, 1, 2, 9, " +
                    "'artist:artist|album', 'artist', '4000000:180:kept local song', '', 0, 1, 0, 2)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 23, true, *LevyraDatabase.MIGRATIONS)

        migrated.query(
            "SELECT title, composer, lyricist, comment, copyright, customTags, fullTagSearchText " +
                "FROM local_media WHERE identityKey = 'ms:external_primary:10'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Kept local song", cursor.getString(0))
            assertEquals("", cursor.getString(1))
            assertEquals("", cursor.getString(2))
            assertEquals("", cursor.getString(3))
            assertEquals("", cursor.getString(4))
            assertEquals("", cursor.getString(5))
            assertEquals("", cursor.getString(6))
        }
        migrated.close()
    }

    @Test
    fun migrateFrom15To23KeepsTheWholeUpgradePathValid() {
        helper.createDatabase(TEST_DB, 15).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) " +
                    "VALUES ('p7', 'Long path', '', 1, 2)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 22, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p7'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Long path", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM queue_spaces").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DB = "levyra-migration-test.db"
    }
}
