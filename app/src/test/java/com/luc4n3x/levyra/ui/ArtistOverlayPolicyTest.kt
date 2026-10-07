package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistOverlayPolicyTest {

    @Test
    fun `artist error takes priority over a partial profile`() {
        assertTrue(shouldShowArtistError(hasError = true, hasProfile = true))
    }

    @Test
    fun `missing profile is an error after loading completes`() {
        assertTrue(shouldShowArtistError(hasError = false, hasProfile = false))
    }

    @Test
    fun `complete profile without error renders normally`() {
        assertFalse(shouldShowArtistError(hasError = false, hasProfile = true))
    }

    @Test
    fun `expanded artist popular songs are not capped at ten`() {
        val tracks = (1..25).map { index -> track("track-$index") }

        assertEquals(25, artistPopularTracksForDisplay(tracks).size)
        assertEquals(25, visibleArtistPopularTracks(tracks, expanded = true).size)
        assertEquals(ARTIST_POPULAR_COLLAPSED_COUNT, visibleArtistPopularTracks(tracks, expanded = false).size)
    }

    @Test
    fun `artist popular songs keep a finite defensive ceiling`() {
        val tracks = (1..120).map { index -> track("track-$index") }

        assertEquals(ARTIST_POPULAR_MAX_COUNT, artistPopularTracksForDisplay(tracks).size)
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
