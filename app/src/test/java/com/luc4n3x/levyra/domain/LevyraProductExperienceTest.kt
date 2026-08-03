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
    fun videoDetectionAcceptsOnlyMediaTypeSignals() {
        assertTrue(track(counterpartVideoId = "video-id").isSearchVideo())
        assertTrue(track(videoType = "MUSIC_VIDEO_TYPE_OMV").isSearchVideo())
        assertTrue(track(source = "YouTube Music Video").isSearchVideo())
        assertFalse(
            track(
                videoUrl = "https://music.youtube.com/watch?v=ordinary-song"
            ).isSearchVideo()
        )
    }

    @Test
    fun filtersExposeOnlyStableAvailableCategories() {
        assertEquals(
            listOf(
                SearchFilter.All,
                SearchFilter.Songs,
                SearchFilter.Artists
            ),
            searchFiltersFor(
                hasArtists = true,
                hasAlbums = false
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
