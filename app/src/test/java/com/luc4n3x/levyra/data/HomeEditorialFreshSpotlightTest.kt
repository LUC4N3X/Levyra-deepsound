package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSpotlightKind
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HomeEditorialFreshSpotlightTest {
    @Test
    fun freshReleasesReplaceOrdinarySpotlightCandidatesWhenAvailable() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val releasedToday = track("fresh", releaseDate = "2026-08-30")
        val chart = track("chart")
        val quickPick = track("quick")

        val candidates = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = true,
            newReleaseTracks = listOf(releasedToday),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = listOf(quickPick),
            fallbackSections = emptyList(),
            chartTracks = listOf(chart),
            nowMillis = now
        )

        assertEquals(listOf("fresh"), candidates.map { it.track.id })
        assertTrue(candidates.all { it.kind == HomeSpotlightKind.ReleasedToday })
    }

    @Test
    fun ordinarySpotlightCandidatesRemainAsFallbackWithoutFreshReleases() {
        val chart = track("chart")
        val quickPick = track("quick")

        val candidates = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = true,
            newReleaseTracks = emptyList(),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = listOf(quickPick),
            fallbackSections = emptyList(),
            chartTracks = listOf(chart),
            nowMillis = 1_777_000_000_000L
        )

        assertTrue(candidates.isNotEmpty())
        assertEquals(HomeSpotlightKind.ChartTrending, candidates.first().kind)
    }

    private fun track(id: String, releaseDate: String = ""): Track {
        return Track(
            id = id,
            title = "Title $id",
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "https://example.com/$id.jpg",
            largeThumbnailUrl = "https://example.com/${id}_large.jpg",
            source = "test",
            moodTags = setOf("pop"),
            energy = 75,
            vocal = 70,
            replayScore = 80,
            cacheScore = 70,
            accentStart = 0xFF123456.toInt(),
            accentEnd = 0xFF654321.toInt(),
            releaseDate = releaseDate,
            metadataConfidence = 90
        )
    }
}
