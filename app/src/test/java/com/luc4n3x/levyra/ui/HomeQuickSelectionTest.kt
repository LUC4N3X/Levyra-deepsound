package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeQuickSelectionTest {
    @Test
    fun excludesDisabledHomeSources() {
        val personal = track("personal", "Personal")
        val quick = track("quick", "Quick")
        val favorite = track("favorite", "Favorite")
        val release = track("release", "Release")
        val resonance = track("resonance", "Resonance")

        val result = buildHomeQuickSelectionTracks(
            personalTracks = listOf(personal),
            quickPickTracks = listOf(quick),
            favoriteTracks = listOf(favorite),
            newReleaseTracks = listOf(release),
            resonanceTracks = listOf(resonance),
            showPersonalOrbit = false,
            showNewReleases = false,
            showResonance = false
        )

        assertEquals(listOf(quick, favorite), result)
    }

    @Test
    fun balancesEnabledSourcesBeforeTakingSecondItems() {
        val personal = listOf(track("p1", "P1"), track("p2", "P2"))
        val quick = listOf(track("q1", "Q1"), track("q2", "Q2"))
        val favorites = listOf(track("f1", "F1"), track("f2", "F2"))
        val releases = listOf(track("n1", "N1"), track("n2", "N2"))
        val resonance = listOf(track("r1", "R1"), track("r2", "R2"))

        val result = buildHomeQuickSelectionTracks(
            personalTracks = personal,
            quickPickTracks = quick,
            favoriteTracks = favorites,
            newReleaseTracks = releases,
            resonanceTracks = resonance,
            showPersonalOrbit = true,
            showNewReleases = true,
            showResonance = true,
            limit = 9
        )

        assertEquals(
            listOf("P1", "Q1", "F1", "N1", "R1", "P2", "Q2", "F2", "N2"),
            result.map(Track::title)
        )
    }

    @Test
    fun removesRecordingDuplicatesAndContinuesFilling() {
        val original = track("source-a", "Same song", artist = "Same artist")
        val duplicate = track("source-b", "Same Song", artist = "Same Artist")
        val fallback = track("source-c", "Different song", artist = "Other artist")

        val result = buildHomeQuickSelectionTracks(
            personalTracks = listOf(original),
            quickPickTracks = listOf(duplicate, fallback),
            favoriteTracks = emptyList(),
            newReleaseTracks = emptyList(),
            resonanceTracks = emptyList(),
            showPersonalOrbit = true,
            showNewReleases = true,
            showResonance = true,
            limit = 2
        )

        assertEquals(2, result.size)
        assertEquals(original, result.first())
        assertTrue(result.contains(fallback))
    }

    @Test
    fun respectsRequestedLimitAcrossSources() {
        val result = buildHomeQuickSelectionTracks(
            personalTracks = listOf(track("p1", "P1"), track("p2", "P2")),
            quickPickTracks = listOf(track("q1", "Q1"), track("q2", "Q2")),
            favoriteTracks = listOf(track("f1", "F1"), track("f2", "F2")),
            newReleaseTracks = listOf(track("n1", "N1")),
            resonanceTracks = listOf(track("r1", "R1")),
            showPersonalOrbit = true,
            showNewReleases = true,
            showResonance = true,
            limit = 3
        )

        assertEquals(listOf("P1", "Q1", "F1"), result.map(Track::title))
    }

    @Test
    fun returnsEmptyListForNonPositiveLimit() {
        val result = buildHomeQuickSelectionTracks(
            personalTracks = listOf(track("p1", "P1")),
            quickPickTracks = emptyList(),
            favoriteTracks = emptyList(),
            newReleaseTracks = emptyList(),
            resonanceTracks = emptyList(),
            showPersonalOrbit = true,
            showNewReleases = true,
            showResonance = true,
            limit = 0
        )

        assertTrue(result.isEmpty())
    }

    private fun track(id: String, title: String, artist: String = "Artist"): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://example.com/$id.jpg",
        largeThumbnailUrl = "https://example.com/$id-large.jpg",
        source = "Test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
