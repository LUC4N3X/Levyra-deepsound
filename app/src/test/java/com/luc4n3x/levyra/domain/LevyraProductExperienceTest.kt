package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraProductExperienceTest {

    @Test
    fun primaryNavigationContainsOnlyCoreDestinations() {
        assertEquals(
            listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Library),
            LevyraPrimaryTabs
        )
        assertFalse(LevyraTab.Explore in LevyraPrimaryTabs)
        assertFalse(LevyraTab.Player in LevyraPrimaryTabs)
    }

    @Test
    fun videoDetectionAcceptsAllSupportedProviderSignals() {
        assertTrue(track(counterpartVideoId = "video-id").isSearchVideo())
        assertTrue(track(videoType = "MUSIC_VIDEO_TYPE_OMV").isSearchVideo())
        assertTrue(track(videoUrl = "https://music.youtube.com/watch?v=id").isSearchVideo())
        assertFalse(track().isSearchVideo())
    }

    @Test
    fun filtersExposeOnlyAvailableRichCategories() {
        assertEquals(
            listOf(
                SearchFilter.All,
                SearchFilter.Songs,
                SearchFilter.Videos,
                SearchFilter.Artists,
                SearchFilter.Playlists
            ),
            searchFiltersFor(
                hasArtists = true,
                hasAlbums = false,
                hasVideos = true,
                hasPlaylists = true
            )
        )
    }

    @Test
    fun playlistSearchIsAccentInsensitiveAndRanksExactMatchesFirst() {
        val result = filterPlaylistsForSearch(
            query = "estate",
            playlists = listOf(
                playlist("Mix estate", updatedAt = 30L),
                playlist("Èstate", updatedAt = 10L),
                playlist("Estate 2026", updatedAt = 20L),
                playlist("Allenamento", updatedAt = 40L)
            )
        )

        assertEquals(listOf("Èstate", "Estate 2026", "Mix estate"), result.map { it.name })
    }

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
        videoUrl: String = ""
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
        source = "YouTube Music",
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
