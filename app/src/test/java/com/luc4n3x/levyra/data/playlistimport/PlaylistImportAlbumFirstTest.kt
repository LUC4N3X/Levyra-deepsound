package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportAlbumFirstTest {
    private fun track(id: String, title: String, artist: String, album: String, durationMs: Long, videoType: String = "MUSIC_VIDEO_TYPE_ATV") = Track(
        id = id, title = title, artist = artist, album = album, durationMs = durationMs, streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id", thumbnailUrl = "", largeThumbnailUrl = "", source = "test",
        moodTags = emptySet(), energy = 50, vocal = 50, replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0,
        videoType = videoType
    )

    private val single = track("single", "Full Capacity", "Infinity Ink", "Full Capacity", 215_000)
    private val albumVersion = track("album", "Full Capacity", "Infinity Ink", "House Of Infinity", 206_000)
    private val otherAlbumTrack = track("other", "Games", "Infinity Ink", "House Of Infinity", 240_000)

    private val repository = object : YoutubeMusicRepository(null) {
        override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
            SearchPage(listOf(single), "")

        override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> = listOf(single)
    }

    private class CountingAlbums(private val tracks: List<Track>) : ImportAlbumSource {
        var searches = 0
        var browses = 0

        override suspend fun albums(query: String, languageCode: String): List<AlbumHit> {
            searches++
            return listOf(
                AlbumHit("House Of Infinity", "Infinity Ink", "2024", "", query, browseId = "MPREb_house"),
                AlbumHit("House Of Infinity", "Someone Else", "2020", "", query, browseId = "MPREb_cover")
            )
        }

        override suspend fun tracks(album: AlbumHit, languageCode: String): List<Track> {
            browses++
            assertEquals("MPREb_house", album.browseId)
            return tracks
        }
    }

    @Test
    fun albumTracklistSupersedesASingleEditWithADifferentLength() = runBlocking {
        val albums = CountingAlbums(listOf(albumVersion, otherAlbumTrack))
        val catalog = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, albumSource = albums)
        val identity = ImportedTrackIdentity(0, "Full Capacity", listOf("Infinity Ink", "Ali Love", "Luca Cazal"),
            album = "House Of Infinity", durationMs = 205_714)
        val outcome = catalog.resolve(identity, false, ResolutionPreference.SMART).outcome
        assertEquals("album", outcome.selected?.candidate?.id)
        assertTrue(outcome.confidence.autoAccepted)

        val sibling = ImportedTrackIdentity(1, "Games", listOf("Infinity Ink", "Guest"), album = "House Of Infinity", durationMs = 240_100)
        assertEquals("other", catalog.resolve(sibling, false, ResolutionPreference.SMART).outcome.selected?.candidate?.id)
        assertEquals(1, albums.searches)
        assertEquals(1, albums.browses)
    }

    @Test
    fun withoutAnAlbumSourceTheExistingSearchFlowIsUnchanged() = runBlocking {
        val catalog = PlaylistImportCatalog(repository, { emptyList() }, { "en" })
        val identity = ImportedTrackIdentity(0, "Full Capacity", listOf("Infinity Ink", "Ali Love", "Luca Cazal"),
            album = "House Of Infinity", durationMs = 205_714)
        val outcome = catalog.resolve(identity, false, ResolutionPreference.SMART).outcome
        assertEquals(false, outcome.confidence.autoAccepted)
    }
}
