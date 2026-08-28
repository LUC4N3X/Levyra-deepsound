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

    private companion object {
        const val TEST_DB = "levyra-migration-test.db"
    }
}
