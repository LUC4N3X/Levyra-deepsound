package com.luc4n3x.levyra.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.PlaylistLayoutSlot
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevyraDatabaseMigration26To27Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate26To27KeepsExistingPlaylistsAndAddsAlbumTable() {
        helper.createDatabase(TEST_DB, 26).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt, hidden, coverMode) " +
                    "VALUES ('p27', 'Before albums', '', 100, 200, 0, 'AUTO')"
            )
            db.execSQL(
                "INSERT INTO playlist_tracks " +
                    "(entryId, playlistId, trackId, position, title, artist, album, durationMs, videoUrl, " +
                    "thumbnailUrl, largeThumbnailUrl, source, accentStart, accentEnd, " +
                    "youtubeLoudnessDb, youtubePerceptualLoudnessDb, isrc, upc, releaseDate, year, " +
                    "trackNumber, discNumber, explicit, albumBrowseId, artistBrowseIds, " +
                    "counterpartVideoId, videoType, metadataProvider, metadataConfidence, " +
                    "canonicalAlbumUrl, addedAt) " +
                    "VALUES ('e1', 'p27', 't1', 0, 'Kept track', 'Artist', 'Album', 200000, '', '', '', " +
                    "'yt', 0, 0, NULL, NULL, '', '', '', '', 0, 0, 0, '', '', '', '', '', 0, '', 150)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 27, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT entryId, title, position FROM playlist_tracks WHERE playlistId = 'p27'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("e1", cursor.getString(0))
            assertEquals("Kept track", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT COUNT(*) FROM playlist_albums").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("PRAGMA foreign_key_list('playlist_albums')").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("playlists", cursor.getString(cursor.getColumnIndexOrThrow("table")))
            assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
        }
    }

    @Test
    fun albumEntriesSurviveTrackEditsReorderAndDeduplication() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LevyraDatabase::class.java).build()
        try {
            val dao = database.playlistDao()
            dao.upsertPlaylist(PlaylistEntity("p", "Mixed", "", 1L, 1L))
            dao.insertTracks(listOf(track("a").toPlaylistTrackEntity("p", 0, 1L, "ea"), track("c").toPlaylistTrackEntity("p", 1, 1L, "ec")))
            val album = AlbumHit(title = "B", artist = "Band", year = "2020", thumbnailUrl = "", query = "", browseId = "MPREb")
            assertTrue(dao.addAlbum(album.toPlaylistAlbumEntity("p", "eb", 0, listOf(track("b1"), track("b2")), 2L)))
            assertFalse(dao.addAlbum(album.toPlaylistAlbumEntity("p", "eb2", 0, listOf(track("b1")), 3L)))

            assertTrue(
                dao.reorderItems(
                    "p",
                    listOf(PlaylistLayoutSlot("ea", false), PlaylistLayoutSlot("eb", true), PlaylistLayoutSlot("ec", false))
                )
            )
            assertEquals(listOf("ea", "eb", "ec"), layoutSlots(dao.tracksOf("p"), dao.albumsOf("p")).map { it.entryId })

            dao.removeTracksAndCompact("p", setOf("a"))
            assertEquals(listOf("eb", "ec"), layoutSlots(dao.tracksOf("p"), dao.albumsOf("p")).map { it.entryId })
            assertEquals(2, decodePlaylistAlbumTracks(dao.albumsOf("p").single().tracksJson).size)

            dao.removeEntries("p", emptySet(), setOf("eb"))
            assertTrue(dao.albumsOf("p").isEmpty())
            assertEquals(listOf("c"), dao.tracksOf("p").map { it.trackId })

            dao.deletePlaylist("p")
            assertTrue(dao.albumsOf("p").isEmpty())
        } finally {
            database.close()
        }
    }

    private fun track(id: String) = Track(
        id = id, title = "T$id", artist = "A", album = "Al", durationMs = 1000L, streamUrl = "",
        videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "yt", moodTags = emptySet(),
        energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    private companion object {
        const val TEST_DB = "migration-26-27-test"
    }
}
