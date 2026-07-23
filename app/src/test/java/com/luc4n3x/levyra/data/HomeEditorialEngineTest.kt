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
import java.util.TimeZone

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
            nowMillis = now
        )

        assertEquals("fresh", candidates.first().track.id)
        assertEquals(HomeSpotlightKind.ReleasedToday, candidates.first().kind)
    }

    @Test
    fun moodPreferenceScoreChangesSpotlightSelection() {
        val gym = track("gym", tags = setOf("gym"), energy = 96)
        val chill = track("chill", tags = setOf("chill"), energy = 42)
        val tracks = listOf(gym, chill)

        fun candidates(preferredId: String) = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = false,
            newReleaseTracks = emptyList(),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = tracks,
            fallbackSections = emptyList(),
            chartTracks = emptyList(),
            preferenceScore = { track -> if (track.id == preferredId) 6_000 else 0 },
            nowMillis = 1_753_276_800_000L
        )

        assertEquals("gym", candidates("gym").first().track.id)
        assertEquals("chill", candidates("chill").first().track.id)
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
            chartTracks = emptyList()
        )

        assertEquals(listOf("quick"), candidates.map { it.track.id })
    }


    @Test
    fun playingSpotlightRemainsAvailableAfterPlaybackStarts() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val spotlight = track("spotlight", releaseDate = "2026-07-23")

        val candidates = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = true,
            newReleaseTracks = listOf(spotlight),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = emptyList(),
            fallbackSections = emptyList(),
            chartTracks = emptyList(),
            nowMillis = now
        )

        assertTrue(candidates.any { it.track.id == "spotlight" })
    }


    @Test
    fun hiddenNewReleasesDoNotApplyFreshnessLabelsThroughOtherSources() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val freshQuickPick = track("freshQuick", releaseDate = "2026-07-23")

        val candidate = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = false,
            newReleaseTracks = listOf(freshQuickPick),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = listOf(freshQuickPick),
            fallbackSections = emptyList(),
            chartTracks = emptyList(),
            nowMillis = now
        ).first()

        assertEquals(HomeSpotlightKind.LevyraSelect, candidate.kind)
        assertEquals(null, candidate.releaseAgeDays)
    }

    @Test
    fun disabledNewReleasesDoNotProduceFreshOrEditorialCollections() {
        val releases = listOf(
            track("release1", releaseDate = "2026-07-23"),
            track("release2", releaseDate = "2026-07-22"),
            track("release3", releaseDate = "2026-07-21"),
            track("release4", releaseDate = "2026-07-20")
        )

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = emptyList(),
            newReleaseTracks = emptyList(),
            personalTracks = emptyList(),
            resonanceTracks = emptyList(),
            quickPickTracks = releases,
            chartTracks = emptyList(),
            favorites = emptyList(),
            libraryTracks = emptyList(),
            includeFresh = false
        )

        assertFalse(collections.any { it.kind == HomeCollectionKind.Fresh })
    }

    @Test
    fun releaseAgeUsesCalendarDaysAcrossDst() {
        val previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try {
            val now = Calendar.getInstance().apply {
                set(2026, Calendar.MARCH, 9, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val release = track("dst", releaseDate = "2026-03-08")

            val candidate = HomeEditorialEngine.buildSpotlightCandidates(
                showNewReleases = true,
                newReleaseTracks = listOf(release),
                showPersonalOrbit = false,
                personalTracks = emptyList(),
                showResonance = false,
                resonanceTracks = emptyList(),
                quickPickTracks = emptyList(),
                fallbackSections = emptyList(),
                chartTracks = emptyList(),
                nowMillis = now
            ).first()

            assertEquals(1, candidate.releaseAgeDays)
            assertEquals(HomeSpotlightKind.JustReleased, candidate.kind)
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun ordinaryAlbumMetadataDoesNotClaimNewAlbum() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val release = track("single", releaseDate = "2026-07-22").copy(
            albumBrowseId = "MPREb_single_container",
            trackNumber = 1
        )

        val candidate = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = true,
            newReleaseTracks = listOf(release),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = emptyList(),
            fallbackSections = emptyList(),
            chartTracks = emptyList(),
            nowMillis = now
        ).first()

        assertEquals(HomeSpotlightKind.JustReleased, candidate.kind)
    }

    @Test
    fun chartPresenceUsesGenericChartLabel() {
        val chart = track("chart")

        val candidate = HomeEditorialEngine.buildSpotlightCandidates(
            showNewReleases = false,
            newReleaseTracks = emptyList(),
            showPersonalOrbit = false,
            personalTracks = emptyList(),
            showResonance = false,
            resonanceTracks = emptyList(),
            quickPickTracks = emptyList(),
            fallbackSections = emptyList(),
            chartTracks = listOf(chart)
        ).first()

        assertEquals(HomeSpotlightKind.ChartTrending, candidate.kind)
    }


    @Test
    fun utcReleaseTimestampUsesUtcBeforeConvertingToLocalDate() {
        val previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        try {
            val now = Calendar.getInstance().apply {
                set(2026, Calendar.JULY, 22, 20, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val release = track("utc", releaseDate = "2026-07-23T00:30:00Z")

            val candidate = HomeEditorialEngine.buildSpotlightCandidates(
                showNewReleases = true,
                newReleaseTracks = listOf(release),
                showPersonalOrbit = false,
                personalTracks = emptyList(),
                showResonance = false,
                resonanceTracks = emptyList(),
                quickPickTracks = emptyList(),
                fallbackSections = emptyList(),
                chartTracks = emptyList(),
                nowMillis = now
            ).first()

            assertEquals(0, candidate.releaseAgeDays)
            assertEquals(HomeSpotlightKind.ReleasedToday, candidate.kind)
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun collectionTailRotatesAcrossCalendarDays() {
        val fresh = (1..4).map { track("fresh$it", releaseDate = "2026-07-23", tags = setOf("fresh")) }
        val local = (1..4).map { track("local$it", tags = setOf("local")) }
        val workout = (1..4).map { track("workout$it", tags = setOf("gym"), energy = 94) }
        val chill = (1..4).map { track("chill$it", tags = setOf("chill"), energy = 48) }
        val rap = (1..4).map { track("rap$it", tags = setOf("rap"), energy = 78) }
        val party = (1..4).map { track("party$it", tags = setOf("party"), energy = 90) }
        val focus = (1..4).map { track("focus$it", tags = setOf("focus"), energy = 74) }
        val pop = (1..4).map { track("pop$it", tags = setOf("pop"), energy = 76) }
        val editorialOne = (1..4).map { track("editorialA$it", tags = setOf("editorial"), energy = 76) }
        val editorialTwo = (1..4).map { track("editorialB$it", tags = setOf("editorial"), energy = 76) }
        val allTracks = fresh + local + workout + chill + rap + party + focus + pop
        val start = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val observedKinds = LinkedHashSet<HomeCollectionKind>()

        repeat(16) { dayOffset ->
            val now = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }.timeInMillis
            val collections = HomeEditorialEngine.buildCollections(
                homeSections = listOf(
                    HomeSection("Editorial A", editorialOne),
                    HomeSection("Editorial B", editorialTwo)
                ),
                newReleaseTracks = fresh,
                personalTracks = emptyList(),
                resonanceTracks = emptyList(),
                quickPickTracks = allTracks,
                chartTracks = emptyList(),
                favorites = emptyList(),
                libraryTracks = emptyList(),
                includeFresh = true,
                nowMillis = now
            )
            assertTrue(collections.size <= 7)
            observedKinds += collections.map { it.kind }
        }

        assertTrue(observedKinds.contains(HomeCollectionKind.Pop))
        assertTrue(observedKinds.contains(HomeCollectionKind.Editorial))
        assertTrue(observedKinds.contains(HomeCollectionKind.Discovery))
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
