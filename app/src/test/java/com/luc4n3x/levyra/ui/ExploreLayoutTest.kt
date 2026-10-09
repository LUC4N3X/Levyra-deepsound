package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
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
    fun discoveryContentPrecedesAdvancedMixControls() {
        val rows = buildExploreRows(zones(4), false, true, true)

        assertTrue(rows.indexOf(ExploreRow.FreshSpotlight) < rows.indexOf(ExploreRow.MixTools))
        assertTrue(rows.indexOf(ExploreRow.Samples) < rows.indexOf(ExploreRow.MixTools))
        assertEquals(ExploreRow.MixTools, rows.last())
    }

    @Test
    fun mixControlsRemainAvailableWhenDiscoveryIsEmpty() {
        val rows = buildExploreRows(emptyList(), false, false, false)

        assertTrue(rows.contains(ExploreRow.FreshEmpty))
        assertEquals(1, rows.count { it == ExploreRow.MixTools })
    }

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
        assertTrue(rows.subList(moodsIndex + 1, samplesIndex).any { row -> row is ExploreRow.MoodRail })
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

        assertTrue(rows.contains(ExploreRow.FreshSpotlight))
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
    fun moodRailKeepsTheCompleteCatalogInSourceOrder() {
        val rows = buildExploreRows(zones(5), isFreshLoading = false, hasFreshTracks = true, hasSamples = true)
        val rail = rows.filterIsInstance<ExploreRow.MoodRail>().single()

        assertEquals(listOf("zone-0", "zone-1", "zone-2", "zone-3", "zone-4"), rail.zones.map { it.id })
        assertEquals("explore-mood-rail", rail.key)
    }

    @Test
    fun duplicatedZonesNeverProduceDuplicatedLazyKeys() {
        val duplicated = zones(3) + zones(3)

        val rows = buildExploreRows(duplicated, isFreshLoading = false, hasFreshTracks = true, hasSamples = true)
        val keys = rows.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertEquals(3, rows.filterIsInstance<ExploreRow.MoodRail>().single().zones.size)
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

    @Test
    fun providerSectionsUseStructuralOrderAndStableParamsIdentity() {
        val categories = listOf(
            ExploreCategory("Focus", "mood-focus", "Moods & moments", 0),
            ExploreCategory("Focus duplicate", "mood-focus", "Moods & moments", 0),
            ExploreCategory("Pop", "genre-pop", "Genres", 1),
            ExploreCategory("", "malformed", "Genres", 1),
            ExploreCategory("Rock", "", "Genres", 1)
        )

        val sections = buildExploreCategorySections(categories)

        assertEquals(2, sections.size)
        assertEquals(ExploreCategoryPresentation.Atmospheric, sections[0].presentation)
        assertEquals(ExploreCategoryPresentation.Structured, sections[1].presentation)
        assertEquals(listOf("mood-focus", "genre-pop"), sections.flatMap { it.categories }.map { it.params })
    }

    @Test
    fun ungroupedProviderCategoriesRemainUsableWithoutTitleGuessing() {
        val sections = buildExploreCategorySections(
            listOf(ExploreCategory("未知", "opaque-params", section = "", sectionIndex = -1))
        )

        assertEquals(ExploreCategoryPresentation.Mixed, sections.single().presentation)
        assertEquals("opaque-params", sections.single().categories.single().params)
    }

    @Test
    fun fallbackGenresExcludeNonGenreEditorialShortcuts() {
        val template = zones(1).single()
        val fallbackIds = exploreFallbackGenres(
            listOf(
                template.copy(id = ExploreCatalog.NEW_RELEASES_ZONE_ID),
                template.copy(id = ExploreCatalog.LOCAL_WAVE_ZONE_ID),
                template.copy(id = "rap-drill")
            )
        ).map { it.id }

        assertFalse(ExploreCatalog.NEW_RELEASES_ZONE_ID in fallbackIds)
        assertFalse(ExploreCatalog.LOCAL_WAVE_ZONE_ID in fallbackIds)
        assertTrue("rap-drill" in fallbackIds)
    }

    @Test
    fun scopeRailAppearsOnlyWhenBothFeedsExist() {
        val local = List(3) { index -> track("local-$index") }
        val world = List(3) { index -> track("world-$index") }

        assertEquals(listOf(ExploreFreshScope.Local), exploreFreshScopes(local, emptyList()))
        assertEquals(listOf(ExploreFreshScope.World), exploreFreshScopes(emptyList(), world))
        assertEquals(
            listOf(ExploreFreshScope.Local, ExploreFreshScope.World),
            exploreFreshScopes(local, world)
        )
        assertTrue(exploreFreshScopes(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun worldScopeFallsBackToTheLocalFeedWhenNothingInternationalLoaded() {
        val local = List(6) { index -> track("local-$index") }

        val feed = exploreFreshFeed(ExploreFreshScope.World, local, emptyList())

        assertEquals(ExploreFreshScope.Local, feed.scope)
        assertEquals(listOf("local-0", "local-1", "local-2", "local-3", "local-4"), feed.spotlight.map { it.id })
        assertEquals(listOf("local-5"), feed.moment.map { it.id })
    }

    @Test
    fun spotlightAndMomentNeverRepeatTheSameTrack() {
        val source = List(30) { index -> track("id-$index") } + track("id-0")

        val feed = exploreFreshFeed(ExploreFreshScope.Local, source, emptyList())
        val ids = feed.spotlight.map { it.id } + feed.moment.map { it.id }

        assertEquals(ExploreSpotlightLimit, feed.spotlight.size)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ExploreMomentRowsPerPage * ExploreMomentPageLimit, feed.moment.size)
    }

    @Test
    fun momentPagesStayBoundedAndKeepFeedOrder() {
        val tracks = List(10) { index -> track("id-$index") }

        val pages = exploreMomentPages(tracks)

        assertEquals(3, pages.size)
        assertEquals(listOf("id-0", "id-1", "id-2", "id-3"), pages.first().map { it.id })
        assertEquals(listOf("id-8", "id-9"), pages.last().map { it.id })
        assertTrue(exploreMomentPages(List(40) { index -> track("x-$index") }).size <= ExploreMomentPageLimit)
    }

    @Test
    fun freshRowsFollowTheEditorialOrderAndDropEmptySections() {
        val rows = buildExploreRows(
            zones = zones(2),
            isFreshLoading = false,
            hasFreshTracks = true,
            hasSamples = false,
            hasFreshScopes = true,
            hasFreshMoment = true
        )

        val headerIndex = exploreAnchorIndex(rows, ExploreAnchor.Fresh)
        assertEquals(ExploreRow.FreshScopes, rows[headerIndex + 1])
        assertEquals(ExploreRow.FreshSpotlight, rows[headerIndex + 2])
        assertEquals(ExploreRow.FreshMoment, rows[headerIndex + 3])

        val minimal = buildExploreRows(zones(2), false, true, false)
        assertFalse(minimal.contains(ExploreRow.FreshScopes))
        assertFalse(minimal.contains(ExploreRow.FreshMoment))
        assertTrue(minimal.contains(ExploreRow.FreshSpotlight))
    }

    @Test
    fun releaseKindIsNeverGuessedFromTheTitle() {
        val trackOnAnAlbum = track("x").copy(title = "Le foglie di te", album = "AMATORE", albumType = "")
        val selfTitled = track("y").copy(title = "Vertigini", album = "Vertigini", albumType = "")

        assertEquals(ExploreReleaseKind.Release, exploreReleaseKind(trackOnAnAlbum))
        assertEquals(ExploreReleaseKind.Release, exploreReleaseKind(selfTitled))
    }

    @Test
    fun resolvedCollectionSizeSettlesSingleVersusAlbum() {
        val base = track("x").copy(title = "Le foglie di te", album = "AMATORE", albumType = "")

        assertEquals(ExploreReleaseKind.Single, exploreReleaseKind(base.copy(trackTotal = 1)))
        assertEquals(ExploreReleaseKind.Album, exploreReleaseKind(base.copy(trackTotal = 12)))
        assertEquals(ExploreReleaseKind.Release, exploreReleaseKind(base.copy(trackTotal = 0)))
    }

    @Test
    fun aReportedTypeStillOutranksTheCollectionSize() {
        val base = track("x").copy(title = "Le foglie di te", album = "AMATORE", trackTotal = 12)

        assertEquals(ExploreReleaseKind.Single, exploreReleaseKind(base.copy(albumType = "single")))
        assertEquals(ExploreReleaseKind.Album, exploreReleaseKind(base.copy(albumType = "album")))
    }

    @Test
    fun aSingleNamesTheTrackEvenWhenTheReleaseIsNamedDifferently() {
        val card = exploreReleaseCard(
            track("x").copy(
                title = "Le foglie di te",
                artist = "Jovanotti, Samurai Jay",
                album = "AMATORE",
                albumType = "single"
            )
        )

        assertEquals("Le foglie di te", card.title)
        assertEquals("Jovanotti, Samurai Jay", card.subtitle)
        assertFalse(card.opensAlbum)
    }

    @Test
    fun spotlightArtworkPrefersTheCoverOverAVideoStill() {
        val videoHero = track("hero").copy(
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music/cover/600x600bb.jpg",
            largeThumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg"
        )

        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/cover/600x600bb.jpg",
            exploreCoverArtworkTrack(videoHero).largeThumbnailUrl
        )
    }

    @Test
    fun spotlightArtworkIsLeftAloneWhenNoCoverIsAvailable() {
        val coverHero = track("cover").copy(
            thumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music/a/300x300bb.jpg",
            largeThumbnailUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music/a/1400x1400bb.jpg"
        )
        val videoOnly = track("video").copy(
            thumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
            largeThumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg"
        )

        assertEquals(coverHero, exploreCoverArtworkTrack(coverHero))
        assertEquals(videoOnly, exploreCoverArtworkTrack(videoOnly))
    }

    @Test
    fun oneAlbumNeverFillsTheSpotlightWithItsOwnTracks() {
        val album = List(4) { index ->
            track("album-$index").copy(title = "Track $index", artist = "Jova", album = "Il Disco")
        }
        val single = track("single").copy(title = "Solo", artist = "Other", album = "Solo")

        val feed = exploreFreshFeed(ExploreFreshScope.Local, album + single, emptyList())

        assertEquals(listOf("album-0", "single"), feed.spotlight.map { it.id })
        assertEquals(listOf("album-1", "album-2", "album-3"), feed.moment.map { it.id })
    }

    @Test
    fun tracksWithoutAnAlbumStayIndividualReleases() {
        val loose = List(3) { index -> track("loose-$index").copy(album = "  ") }

        val feed = exploreFreshFeed(ExploreFreshScope.Local, loose, emptyList())

        assertEquals(listOf("loose-0", "loose-1", "loose-2"), feed.spotlight.map { it.id })
        assertTrue(feed.moment.isEmpty())
    }

    @Test
    fun releaseKindComesFromTheTypeTheSourceReported() {
        val single = track("a").copy(title = "Vertigini", album = "Vertigini", albumType = "single")
        val album = track("b").copy(title = "Le foglie di te", album = "AMATORE", albumType = "album")
        val compilation = track("c").copy(title = "X", album = "Best of", albumType = "COMPILATION")

        assertEquals(ExploreReleaseKind.Single, exploreReleaseKind(single))
        assertEquals(ExploreReleaseKind.Album, exploreReleaseKind(album))
        assertEquals(ExploreReleaseKind.Album, exploreReleaseKind(compilation))
    }

    @Test
    fun anAlbumCardNamesTheAlbumAndOpensIt() {
        val card = exploreReleaseCard(
            track("x").copy(
                title = "Le foglie di te",
                artist = "Jovanotti, Samurai Jay",
                album = "AMATORE",
                albumArtist = "Jovanotti",
                albumType = "album"
            )
        )

        assertEquals("AMATORE", card.title)
        assertEquals("Jovanotti", card.subtitle)
        assertTrue(card.opensAlbum)
    }

    @Test
    fun aTrackCardNamesTheTrackAndNeverOpensAnAlbum() {
        val single = exploreReleaseCard(
            track("s").copy(title = "Vertigini", artist = "Fabri Fibra", album = "Vertigini", albumType = "single")
        )
        val unknown = exploreReleaseCard(
            track("u").copy(
                title = "Le foglie di te",
                artist = "Jovanotti",
                album = "AMATORE",
                albumType = "",
                trackTotal = 0
            )
        )

        assertEquals("Vertigini", single.title)
        assertEquals("Fabri Fibra", single.subtitle)
        assertFalse(single.opensAlbum)

        assertEquals("Le foglie di te", unknown.title)
        assertEquals("Jovanotti", unknown.subtitle)
        assertFalse(unknown.opensAlbum)
    }

    @Test
    fun anAlbumWithoutANameFallsBackToTheTrack() {
        val card = exploreReleaseCard(
            track("x").copy(title = "Senza disco", artist = "Tizio", album = "   ", albumType = "album")
        )

        assertEquals("Senza disco", card.title)
        assertFalse(card.opensAlbum)
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
