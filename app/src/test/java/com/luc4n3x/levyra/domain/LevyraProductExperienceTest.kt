package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraProductExperienceTest {

    @Test
    fun primaryNavigationContainsOnlyCoreDestinations() {
        assertEquals(listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Library), LevyraPrimaryTabs)
        assertFalse(LevyraTab.Explore in LevyraPrimaryTabs)
        assertFalse(LevyraTab.Player in LevyraPrimaryTabs)
    }

    @Test
    fun videoDetectionAcceptsOnlyMediaTypeSignals() {
        assertTrue(track(counterpartVideoId = "video-id").isSearchVideo())
        assertTrue(track(videoType = "MUSIC_VIDEO_TYPE_OMV").isSearchVideo())
        assertTrue(track(source = "YouTube Music Video").isSearchVideo())
        assertFalse(track(videoUrl = "https://music.youtube.com/watch?v=ordinary-song").isSearchVideo())
    }

    @Test
    fun filtersExposeOnlyAvailableRichCategories() {
        assertEquals(
            listOf(SearchFilter.All, SearchFilter.Songs, SearchFilter.Videos, SearchFilter.Artists, SearchFilter.Playlists),
            searchFiltersFor(hasArtists = true, hasAlbums = false, hasVideos = true, hasPlaylists = true)
        )
    }

    @Test
    fun homeAlbumsRemoveBrowseIdEditionAndArtistChannelDuplicates() {
        val result = deduplicateHomeAlbums(
            listOf(
                album("Amatore", "Samurai Jay - Topic", "MPRE-first", "https://lh3.googleusercontent.com/cover=w544"),
                album("Amatore (Deluxe Edition)", "Samurai Jay", "MPRE-second", "https://lh3.googleusercontent.com/cover=w1200"),
                album("Un altro album", "Samurai Jay", "MPRE-third", "https://lh3.googleusercontent.com/other=w544")
            )
        )
        assertEquals(listOf("Amatore", "Un altro album"), result.map { it.title })
    }

    @Test
    fun homeAlbumsKeepSameTitleFromDifferentArtists() {
        val result = deduplicateHomeAlbums(
            listOf(
                album("Greatest Hits", "Artist One", "MPRE-one", "https://example.com/one.jpg"),
                album("Greatest Hits", "Artist Two", "MPRE-two", "https://example.com/two.jpg")
            )
        )

        assertEquals(listOf("Artist One", "Artist Two"), result.map { it.artist })
    }

    @Test
    fun playlistSearchIsAccentInsensitiveAndRanksExactMatchesFirst() {
        val result = filterPlaylistsForSearch(
            query = "estate",
            playlists = listOf(
                playlist("Mix estate", 30L),
                playlist("Èstate", 10L),
                playlist("Estate 2026", 20L),
                playlist("Allenamento", 40L)
            )
        )
        assertEquals(listOf("Èstate", "Estate 2026", "Mix estate"), result.map { it.name })
    }

    private fun album(title: String, artist: String, browseId: String, thumbnailUrl: String) = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = thumbnailUrl,
        query = "$title $artist",
        browseId = browseId
    )

    private fun playlist(name: String, updatedAt: Long) = Playlist(
        id = name,
        name = name,
        coverUrl = "",
        tracks = emptyList(),
        createdAt = 0L,
        updatedAt = updatedAt
    )

    private fun track(
        counterpartVideoId: String = "",
        videoType: String = "",
        videoUrl: String = "",
        source: String = "YouTube Music"
    ) = Track(
        id = "id",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "https://example.com/audio",
        videoUrl = videoUrl,
        thumbnailUrl = "https://example.com/thumb.jpg",
        largeThumbnailUrl = "https://example.com/large.jpg",
        source = source,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId,
        videoType = videoType
    )
}
