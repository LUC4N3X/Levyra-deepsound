package com.luc4n3x.levyra.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.domain.PlaylistCoverMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistProDaoTest {
    private lateinit var database: LevyraDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LevyraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.playlistDao()
        dao.upsertPlaylist(
            PlaylistEntity(
                id = PLAYLIST_ID,
                name = "Playlist Pro",
                coverUrl = CUSTOM_COVER,
                createdAt = 1L,
                updatedAt = 1L,
                coverMode = PlaylistCoverMode.CUSTOM.name
            )
        )
        dao.insertTracks((0..4).map { index -> track(('A'.code + index).toChar().toString(), index) })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bulkRemovalCompactsPositionsOnceAndKeepsCustomCover() = runBlocking {
        dao.removeTracksAndCompact(PLAYLIST_ID, setOf("B", "D"))

        val remaining = dao.tracksOf(PLAYLIST_ID)
        assertEquals(listOf("A", "C", "E"), remaining.map { it.trackId })
        assertEquals(listOf(0, 1, 2), remaining.map { it.position })
        assertCustomCover()
    }

    @Test
    fun automaticArtworkUpdatesAndReorderDoNotOverrideCustomCover() = runBlocking {
        dao.updateAutomaticCover(PLAYLIST_ID, "https://example.test/automatic.jpg", 2L)
        val reversed = dao.tracksOf(PLAYLIST_ID).reversed().mapIndexed { index, track ->
            track.copy(position = index)
        }
        dao.replaceTracks(PLAYLIST_ID, reversed)

        assertEquals(listOf("E", "D", "C", "B", "A"), dao.tracksOf(PLAYLIST_ID).map { it.trackId })
        assertCustomCover()
    }

    @Test
    fun resetRestoresAutomaticCoverState() = runBlocking {
        dao.resetCover(PLAYLIST_ID, "https://example.test/automatic.jpg", 3L)

        val playlist = requireNotNull(dao.playlist(PLAYLIST_ID))
        assertEquals(PlaylistCoverMode.AUTO.name, playlist.coverMode)
        assertEquals("https://example.test/automatic.jpg", playlist.coverUrl)
    }

    private suspend fun assertCustomCover() {
        val playlist = requireNotNull(dao.playlist(PLAYLIST_ID))
        assertEquals(PlaylistCoverMode.CUSTOM.name, playlist.coverMode)
        assertEquals(CUSTOM_COVER, playlist.coverUrl)
    }

    private fun track(id: String, position: Int): PlaylistTrackEntity = PlaylistTrackEntity(
        playlistId = PLAYLIST_ID,
        trackId = id,
        position = position,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        accentStart = 0,
        accentEnd = 0,
        youtubeLoudnessDb = null,
        youtubePerceptualLoudnessDb = null,
        isrc = "",
        upc = "",
        releaseDate = "",
        year = "",
        trackNumber = position + 1,
        discNumber = 1,
        explicit = false,
        albumBrowseId = "",
        artistBrowseIds = "",
        counterpartVideoId = "",
        videoType = "",
        metadataProvider = "",
        metadataConfidence = 0,
        canonicalAlbumUrl = "",
        addedAt = position.toLong()
    )

    private companion object {
        const val PLAYLIST_ID = "playlist-pro"
        const val CUSTOM_COVER = "file:///data/user/0/com.luc4n3x.levyra/files/playlist_covers/custom.jpg"
    }
}
