package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TidalVideoCoverProviderTest {
    @Test
    fun trackQueriesPreferAlbumArtistTitleThenUseBoundedFallback() {
        val queries = tidalSearchQueries(identity(album = "After Hours"), "TRACKS")

        assertEquals(listOf("After Hours The Weeknd Blinding Lights", "Blinding Lights The Weeknd"), queries)
    }

    @Test
    fun rejectedTrackOrReleaseCandidatesKeepBothVariantsAndAlbumFallbackAvailable() {
        val requested = identity(album = "After Hours")
        val wrongTrack = albumCandidate(title = "Save Your Tears", album = "After Hours")
        val wrongRelease = albumCandidate(album = "Dawn FM")

        assertEquals(2, tidalSearchQueries(requested, "TRACKS").size)
        assertTrue(shouldSearchTidalAlbumsAfterTracks(requested, listOf(wrongTrack), DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE))
        assertTrue(shouldSearchTidalAlbumsAfterTracks(requested, listOf(wrongRelease), DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE))
        assertFalse(shouldSearchTidalAlbumsAfterTracks(requested, listOf(albumCandidate()), DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE))
    }

    @Test
    fun structurallyAcceptedCandidateBelowConfiguredThresholdKeepsAlbumFallbackAvailable() {
        val requested = identity(album = "After Hours The Original Motion Picture Soundtrack")
        val candidate = albumCandidate(album = "After Hours The Original Motion Picture Soundtrack Official").copy(
            identity = identity("After Hours The Original Motion Picture Soundtrack Official").copy(
                title = "",
                durationMs = 0L
            )
        )

        val match = CanonicalTrackMatcher.match(requested, candidate)

        assertTrue(match.accepted)
        assertTrue(match.score in 70..83)
        assertTrue(shouldSearchTidalAlbumsAfterTracks(requested, listOf(candidate), 84))
        assertFalse(shouldSearchTidalAlbumsAfterTracks(requested, listOf(candidate), 70))
    }

    @Test
    fun albumHydrationBudgetIsDeduplicatedAndBounded() {
        val budget = TidalAlbumHydrationBudget()

        assertTrue(budget.reserve("first"))
        assertFalse(budget.reserve("first"))
        assertTrue(budget.reserve("second"))
        assertTrue(budget.reserve("third"))
        assertFalse(budget.reserve("fourth"))
    }

    @Test
    fun albumQueriesRemainBoundedAndIgnoreBlankValues() {
        assertEquals(listOf("After Hours The Weeknd", "The Weeknd After Hours"), tidalSearchQueries(identity(), "ALBUMS"))
        assertTrue(tidalSearchQueries(identity(album = ""), "ALBUMS").isNotEmpty())
    }

    private fun identity(album: String = "After Hours") = MotionTrackIdentity(
        title = "Blinding Lights",
        artists = listOf("The Weeknd"),
        album = album,
        durationMs = 200_000L,
        isrc = "",
        upc = "",
        year = "",
        trackId = "track",
        albumId = "album"
    )

    private fun albumCandidate(
        title: String = "Blinding Lights",
        album: String = "After Hours"
    ) = MotionArtworkCandidate(
        provider = "tidal-video-cover",
        scope = MotionArtworkScope.ALBUM,
        identity = identity(album).copy(title = title),
        url = "https://example.invalid/video.mp4",
        mimeType = "video/mp4",
        expiresAtMs = Long.MAX_VALUE
    )
}
