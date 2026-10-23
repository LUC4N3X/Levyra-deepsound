package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinArtistSearchMappingTest {

    private fun track(id: String) = Track(
        id = id, title = "Song $id", artist = "Artist", album = "", durationMs = 0L, streamUrl = "", videoUrl = "",
        thumbnailUrl = "", largeThumbnailUrl = "", source = "", moodTags = emptySet(), energy = 0, vocal = 0,
        replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    private fun artist(name: String) = ArtistHit(name = name, subscribers = "", thumbnailUrl = "", accentStart = 0, accentEnd = 0, browseId = "UC_$name")

    @Test
    fun youTubeMusicListLeadsWithSongsThenInterleavesOtherKinds() {
        val entries = youTubeMusicSearchEntries(
            songs = (1..7).map { track("s$it") },
            artists = listOf(artist("A"), artist("B")),
            videos = listOf(track("v1"), track("v2"), track("v3")),
            playlists = listOf(PlaylistHit(title = "P", author = "x", thumbnailUrl = "", playlistId = "PL1")),
            albums = listOf(AlbumHit(title = "Al", artist = "Artist", year = "2024", thumbnailUrl = "", query = "", browseId = "MPRE1")),
            leadingSongs = 5
        )

        assertEquals(
            listOf(
                "song-s1", "song-s2", "song-s3", "song-s4", "song-s5",
                "artist-UC_A", "video-v1", "playlist-PL1", "album-MPRE1",
                "artist-UC_B", "video-v2", "video-v3",
                "song-s6", "song-s7"
            ),
            entries.map { it.key }
        )
    }

    @Test
    fun youTubeMusicListDropsDuplicateEntries() {
        val entries = youTubeMusicSearchEntries(listOf(track("s1"), track("s1")), emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(listOf("song-s1"), entries.map { it.key })
    }

    @Test
    fun youTubeMusicFilterKeepsOnlyTheSelectedKind() {
        val data = SearchResults(
            songs = listOf(track("s1")),
            artists = listOf(artist("A")),
            videos = listOf(track("v1")),
            playlists = listOf(PlaylistHit(title = "P", author = "x", thumbnailUrl = "", playlistId = "PL1"))
        )

        assertEquals(listOf("song-s1"), youTubeMusicFilteredEntries(SearchFilter.Songs, data.songs, data, null).map { it.key })
        assertEquals(listOf("video-v1"), youTubeMusicFilteredEntries(SearchFilter.Videos, data.songs, data, null).map { it.key })
        assertEquals(listOf("artist-UC_A"), youTubeMusicFilteredEntries(SearchFilter.Artists, data.songs, data, artist("A")).map { it.key })
        assertEquals(listOf("playlist-PL1"), youTubeMusicFilteredEntries(SearchFilter.Playlists, data.songs, data, null).map { it.key })
        assertTrue(youTubeMusicFilteredEntries(SearchFilter.Albums, data.songs, data, null).isEmpty())
        assertFalse(youTubeMusicFilteredEntries(SearchFilter.All, data.songs, data, artist("A")).any { it.key == "artist-UC_A" })
    }

    @Test
    fun appleReleaseTitleLosesTheReleaseSuffixAndKeepsTheCanonicalUrl() {
        val item = AppleCatalogItem(
            id = "222", kind = AppleCatalogItemKind.Album, title = "Training Season's Over - EP", subtitle = "2026",
            url = "https://music.apple.com/us/album/training-seasons-over-ep/222"
        )

        val hit = item.toArtistAlbumHit("Dua Lipa")

        assertEquals("Training Season's Over", hit.title)
        assertEquals("Dua Lipa", hit.artist)
        assertEquals("2026", hit.year)
        assertEquals("https://music.apple.com/us/album/training-seasons-over-ep/222", hit.canonicalUrl)
    }

    @Test
    fun appearsOnAlbumKeepsItsOwnCredit() {
        val item = AppleCatalogItem(id = "9", kind = AppleCatalogItemKind.Album, title = "Collab", subtitle = "Miley Cyrus")

        assertEquals("Miley Cyrus", item.toArtistAlbumHit("Dua Lipa").artist)
    }

    @Test
    fun videoTitlesMatchIgnoringAnnotationsAndAccents() {
        assertTrue(appleVideoTitleMatches("Training Season (Official Music Video)", "Training Season"))
        assertTrue(appleVideoTitleMatches("Dúa", "Dua"))
        assertFalse(appleVideoTitleMatches("Houdini", "Training Season"))
        assertFalse(appleVideoTitleMatches("Anything", "(Live)"))
    }
}
