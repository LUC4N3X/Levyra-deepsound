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

    private companion object {
        const val TEST_DB = "levyra-migration-test.db"
    }
}
