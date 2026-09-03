package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreLayoutTest {
    @Test
    fun shortcutAnchorsResolveToTheirHeaders() {
        val rows = buildExploreRows(
            zones = zones(3),
            isFreshLoading = false,
            hasFreshTracks = true,
            hasSamples = true
        )

        assertEquals(ExploreShortcut.entries.map { it.anchor }.toSet(), exploreAvailableAnchors(rows))
        ExploreShortcut.entries.forEach { shortcut ->
            val index = exploreAnchorIndex(rows, shortcut.anchor)
            assertTrue("Missing header for ${shortcut.name}", index >= 0)
            assertEquals(ExploreRow.Header(shortcut.anchor), rows[index])
        }
    }

    @Test
    fun genresStayAheadOfSamplesInTheMainDiscoveryFlow() {
        val rows = buildExploreRows(
            zones = zones(4),
            isFreshLoading = false,
            hasFreshTracks = true,
            hasSamples = true
        )

        val moodsIndex = exploreAnchorIndex(rows, ExploreAnchor.Moods)
        val samplesIndex = exploreAnchorIndex(rows, ExploreAnchor.Samples)

        assertTrue(moodsIndex >= 0)
        assertTrue(samplesIndex > moodsIndex)
        assertTrue(rows.subList(moodsIndex + 1, samplesIndex).any { row -> row is ExploreRow.MoodPair })
    }

    @Test
    fun newReleasesShortcutTargetsAnExistingCatalogZone() {
        assertEquals(ExploreCatalog.NEW_RELEASES_ZONE_ID, ExploreShortcut.NewReleases.zoneId)
        assertNull(ExploreShortcut.Samples.zoneId)
        assertNull(ExploreShortcut.Moods.zoneId)
    }

    @Test
    fun cachedTracksStayVisibleWhileTheNextZoneLoads() {
        val rows = buildExploreRows(
            zones = zones(2),
            isFreshLoading = true,
            hasFreshTracks = true,
            hasSamples = false
        )

        assertTrue(rows.contains(ExploreRow.FreshCarousel))
        assertFalse(rows.contains(ExploreRow.FreshLoading))
    }

    @Test
    fun emptyZoneFallsBackToTheEmptyStateInsteadOfTheSpinner() {
        val loading = buildExploreRows(zones(2), isFreshLoading = true, hasFreshTracks = false, hasSamples = false)
        val settled = buildExploreRows(zones(2), isFreshLoading = false, hasFreshTracks = false, hasSamples = false)

        assertTrue(loading.contains(ExploreRow.FreshLoading))
        assertFalse(loading.contains(ExploreRow.FreshEmpty))
        assertTrue(settled.contains(ExploreRow.FreshEmpty))
        assertFalse(settled.contains(ExploreRow.FreshLoading))
    }

    @Test
    fun samplesSectionDisappearsWhenThereIsNothingToShow() {
        val rows = buildExploreRows(zones(2), isFreshLoading = false, hasFreshTracks = true, hasSamples = false)

        assertFalse(rows.contains(ExploreRow.Samples))
        assertEquals(-1, exploreAnchorIndex(rows, ExploreAnchor.Samples))
    }

    @Test
    fun availableAnchorsMatchOnlyRenderedSections() {
        val rows = buildExploreRows(
            zones = emptyList(),
            isFreshLoading = false,
            hasFreshTracks = true,
            hasSamples = false
        )

        assertEquals(setOf(ExploreAnchor.Fresh), exploreAvailableAnchors(rows))
    }

    @Test
    fun moodRowsPairZonesAndKeepTheLastOddZone() {
        val rows = buildExploreRows(zones(5), isFreshLoading = false, hasFreshTracks = true, hasSamples = true)
        val pairs = rows.filterIsInstance<ExploreRow.MoodPair>()

        assertEquals(3, pairs.size)
        assertEquals(listOf("zone-0", "zone-2", "zone-4"), pairs.map { it.leading.id })
        assertEquals(listOf("zone-1", "zone-3", null), pairs.map { it.trailing?.id })
    }

    @Test
    fun duplicatedZonesNeverProduceDuplicatedLazyKeys() {
        val duplicated = zones(3) + zones(3)

        val rows = buildExploreRows(duplicated, isFreshLoading = false, hasFreshTracks = true, hasSamples = true)
        val keys = rows.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertEquals(3, rows.filterIsInstance<ExploreRow.MoodPair>().sumOf { pair ->
            if (pair.trailing == null) 1 else 2
        })
    }

    @Test
    fun missingMoodCatalogStillRendersTheRestOfTheScreen() {
        val rows = buildExploreRows(emptyList(), isFreshLoading = false, hasFreshTracks = true, hasSamples = true)

        assertEquals(-1, exploreAnchorIndex(rows, ExploreAnchor.Moods))
        assertTrue(rows.contains(ExploreRow.Shortcuts))
        assertNotNull(rows.firstOrNull { it is ExploreRow.Header && it.anchor == ExploreAnchor.Fresh })
    }

    @Test
    fun samplesKeepSourceOrderAndDropArtworklessEntries() {
        val videos = listOf(
            track("a"),
            track("b", thumbnailUrl = "", largeThumbnailUrl = ""),
            track("c", thumbnailUrl = ""),
            track("a")
        )

        assertEquals(listOf("a", "c"), exploreSampleTracks(videos).map { it.id })
    }

    @Test
    fun samplesRejectOrdinaryMusicVideos() {
        val ordinary = track("ordinary").copy(
            source = "YouTube Music",
            videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
            videoType = ""
        )

        assertTrue(exploreSampleTracks(listOf(ordinary)).isEmpty())
    }

    @Test
    fun samplesDefaultToPreviewBoundAndKeepTheImmersiveFeedBounded() {
        val videos = List(ExploreImmersiveSampleLimit + 4) { index -> track("id-$index") }

        assertEquals(ExploreSampleLimit, exploreSampleTracks(videos).size)
        assertEquals(
            ExploreImmersiveSampleLimit,
            exploreSampleTracks(videos, limit = ExploreImmersiveSampleLimit).size
        )
        assertEquals(3, exploreSampleTracks(videos, limit = 3).size)
        assertTrue(exploreSampleTracks(videos, limit = 0).isEmpty())
        assertTrue(exploreSampleTracks(videos, limit = -1).isEmpty())
    }

    private fun zones(count: Int): List<ExploreZone> = List(count) { index ->
        ExploreZone(
            id = "zone-$index",
            label = "Zone $index",
            emoji = "🎧",
            query = "query $index",
            accentStart = 0xFF00E5FF.toInt(),
            accentEnd = 0xFF2979FF.toInt()
        )
    }

    private fun track(
        id: String,
        thumbnailUrl: String = "https://levyra.test/$id.jpg",
        largeThumbnailUrl: String = "https://levyra.test/$id-large.jpg"
    ): Track = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "Album $id",
        durationMs = 190_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/shorts/$id",
        thumbnailUrl = thumbnailUrl,
        largeThumbnailUrl = largeThumbnailUrl,
        source = "YouTube Shorts",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF2979FF.toInt(),
        videoType = "SHORTS"
    )
}
