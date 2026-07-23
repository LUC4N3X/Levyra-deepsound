package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeCollectionKind
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.HomeSpotlightKind
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HomeEditorialEngineTest {
    @Test
    fun releasedTodayWinsSpotlightSelection() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fresh = track("fresh", releaseDate = "2026-07-23")
        val chart = track("chart")

        val candidates = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = true,
            newReleaseTracks = listOf(fresh),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = emptyList(),
            fallbackSections = emptyList(),
            chartTracks = listOf(chart),
            currentTrackId = null,
            nowMillis = now
        )

        assertEquals("fresh", candidates.first().track.id)
        assertEquals(HomeSpotlightKind.ReleasedToday, candidates.first().kind)
    }

    @Test
    fun hiddenSectionsDoNotFeedSpotlight() {
        val candidates = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = false,
            newReleaseTracks = listOf(track("release")),
            showPersonalOrbit = false,
            personalTracks = listOf(track("orbit")),
            showResonance = false,
            resonanceTracks = listOf(track("resonance")),
            quickPickTracks = listOf(track("quick")),
            fallbackSections = emptyList(),
            chartTracks = emptyList(),
            currentTrackId = null
        )

        assertEquals(listOf("quick"), candidates.map { it.track.id })
    }

    @Test
    fun collectionsRequirePlayableVarietyAndBuildThemedGroups() {
        val tracks = listOf(
            track("gym1", tags = setOf("gym", "energy"), energy = 94),
            track("gym2", tags = setOf("workout"), energy = 91),
            track("gym3", tags = setOf("running"), energy = 90),
            track("gym4", tags = setOf("energy"), energy = 89),
            track("chill1", tags = setOf("chill"), energy = 52),
            track("chill2", tags = setOf("relax"), energy = 48),
            track("chill3", tags = setOf("lofi"), energy = 44),
            track("chill4", tags = setOf("calm"), energy = 46)
        )

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = listOf(HomeSection("Editorial", tracks)),
            newReleaseTracks = emptyList(),
            personalTracks = emptyList(),
            resonanceTracks = emptyList(),
            quickPickTracks = tracks,
            chartTracks = emptyList(),
            favorites = emptyList(),
            libraryTracks = emptyList()
        )

        assertTrue(collections.any { it.kind == HomeCollectionKind.Workout })
        assertTrue(collections.any { it.kind == HomeCollectionKind.Chill })
        assertFalse(collections.any { it.tracks.size < 4 })
    }

    private fun track(
        id: String,
        releaseDate: String = "",
        tags: Set<String> = setOf("pop"),
        energy: Int = 75
    ): Track {
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
            moodTags = tags,
            energy = energy,
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
