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
class LevyraDatabaseMigration23To24Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate23To24PreservesTracksAndMatchesRoomSchema() {
        helper.createDatabase(TEST_DB, 23).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt, hidden, coverMode) " +
                    "VALUES ('p24', 'Migration mix', '', 100, 200, 0, 'AUTO')"
            )
            db.execSQL(
                "INSERT INTO playlist_tracks " +
                    "(playlistId, trackId, position, title, artist, album, durationMs, videoUrl, " +
                    "thumbnailUrl, largeThumbnailUrl, source, accentStart, accentEnd, " +
                    "youtubeLoudnessDb, youtubePerceptualLoudnessDb, isrc, upc, releaseDate, year, " +
                    "trackNumber, discNumber, explicit, albumBrowseId, artistBrowseIds, " +
                    "counterpartVideoId, videoType, metadataProvider, metadataConfidence, " +
                    "canonicalAlbumUrl, addedAt) " +
                    "VALUES ('p24', 't24', 0, 'Kept track', 'Artist', 'Album', 200000, '', '', '', " +
                    "'yt', 0, 0, NULL, NULL, '', '', '', '', 0, 0, 0, '', '', '', '', '', 0, '', 150)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 24, true, *LevyraDatabase.MIGRATIONS)

        migrated.query(
            "SELECT entryId, playlistId, trackId, position, title, addedAt FROM playlist_tracks WHERE playlistId = 'p24'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("p24:t24", cursor.getString(0))
            assertEquals("p24", cursor.getString(1))
            assertEquals("t24", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals("Kept track", cursor.getString(4))
            assertEquals(150L, cursor.getLong(5))
            assertEquals(1, cursor.count)
        }

        migrated.query("PRAGMA foreign_key_list('playlist_tracks')").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("playlists", cursor.getString(cursor.getColumnIndexOrThrow("table")))
            assertEquals("playlistId", cursor.getString(cursor.getColumnIndexOrThrow("from")))
            assertEquals("id", cursor.getString(cursor.getColumnIndexOrThrow("to")))
            assertEquals("NO ACTION", cursor.getString(cursor.getColumnIndexOrThrow("on_update")))
            assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
        }

        val indexNames = mutableSetOf<String>()
        migrated.query("PRAGMA index_list('playlist_tracks')").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) indexNames += cursor.getString(nameColumn)
        }
        assertTrue("missing playlistId index", "index_playlist_tracks_playlistId" in indexNames)
        assertTrue(
            "missing playlistId/trackId index",
            "index_playlist_tracks_playlistId_trackId" in indexNames
        )

        migrated.close()
    }

    private companion object {
        const val TEST_DB = "levyra-migration-23-24-test.db"
    }
}