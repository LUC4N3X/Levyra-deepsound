package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.searchBrowseCopy
import com.luc4n3x.levyra.ui.i18n.searchBrowseLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBrowseTest {
    private fun zone(id: String, label: String) = ExploreZone(id, label, "", "$label query", 0, 0)

    private val zones = listOf(
        zone(ExploreCatalog.NEW_RELEASES_ZONE_ID, "New releases"),
        zone(ExploreCatalog.LOCAL_WAVE_ZONE_ID, "Italy Wave"),
        zone("rap-drill", "Rap & Drill"),
        zone("elettronica", "Electronic"),
        zone("lofi-chill", "Lo-fi & Chill")
    )

    private val categories = listOf(
        ExploreCategory("Chill", "p-chill", "Moods & moments", 0),
        ExploreCategory("Workout", "p-workout", "Moods & moments", 0),
        ExploreCategory("Sleep", "p-sleep", "Moods & moments", 0),
        ExploreCategory("Electronic", "p-electronic", "Genres", 1),
        ExploreCategory("Jazz", "p-jazz", "Genres", 1),
        ExploreCategory("Jazz duplicate", "p-jazz", "Genres", 1),
        ExploreCategory("Summer", "p-summer", "Seasonal", 2)
    )

    private fun LevyraSearchBrowseSection.labels(): List<String> = tiles.map { tile ->
        when (tile) {
            is LevyraSearchBrowseTile.Zone -> tile.zone.label
            is LevyraSearchBrowseTile.Category -> tile.category.title
        }
    }

    @Test
    fun newReleasesAndLocalWaveLeadTheFirstCategorySection() {
        val first = levyraSearchBrowseSections(zones, categories).first()

        assertEquals(LevyraSearchBrowseSectionKind.Moods, first.kind)
        assertEquals("Moods & moments", first.providerTitle)
        assertEquals(listOf("New releases", "Italy Wave", "Chill", "Workout", "Sleep"), first.labels())
    }

    @Test
    fun oneGridKeepsFeaturedFirstThenMoodsGenresAndPicks() {
        val tiles = levyraSearchBrowseTiles(levyraSearchBrowseSections(zones, categories))
        val labels = tiles.map { tile ->
            when (tile) {
                is LevyraSearchBrowseTile.Zone -> tile.zone.label
                is LevyraSearchBrowseTile.Category -> tile.category.title
            }
        }

        assertEquals(
            listOf("New releases", "Italy Wave", "Chill", "Workout", "Sleep", "Electronic", "Jazz", "Summer", "Rap & Drill", "Lo-fi & Chill"),
            labels
        )
        assertEquals(tiles.size, tiles.map { it.key }.toSet().size)
    }

    @Test
    fun everyProviderCategoryAppearsGroupedInProviderOrder() {
        val sections = levyraSearchBrowseSections(zones, categories)
        val provider = sections.filter { section -> section.kind != LevyraSearchBrowseSectionKind.Picks }

        assertEquals(listOf("Moods & moments", "Genres", "Seasonal"), provider.map { it.providerTitle })
        assertEquals(
            listOf(LevyraSearchBrowseSectionKind.Moods, LevyraSearchBrowseSectionKind.Genres, LevyraSearchBrowseSectionKind.Genres),
            provider.map { it.kind }
        )
        assertEquals(listOf("Chill", "Workout", "Sleep"), provider[0].tiles.filterIsInstance<LevyraSearchBrowseTile.Category>().map { it.category.title })
        assertEquals(listOf("Electronic", "Jazz"), provider[1].labels())
        assertEquals(listOf("Summer"), provider[2].labels())
    }

    @Test
    fun levyraPicksComeLastAndSkipZonesAlreadyCoveredByTheProvider() {
        val last = levyraSearchBrowseSections(zones, categories).last()

        assertEquals(LevyraSearchBrowseSectionKind.Picks, last.kind)
        assertEquals(listOf("Rap & Drill", "Lo-fi & Chill"), last.labels())
    }

    @Test
    fun withoutProviderCategoriesLevyraZonesStillFillTheGrid() {
        val sections = levyraSearchBrowseSections(zones, emptyList())

        assertEquals(listOf(LevyraSearchBrowseSectionKind.Picks), sections.map { it.kind })
        assertEquals(listOf("New releases", "Italy Wave", "Rap & Drill", "Electronic", "Lo-fi & Chill"), sections.single().labels())
    }

    @Test
    fun ungroupedProviderCategoriesFormOneMixedSection() {
        val loose = listOf(ExploreCategory("Focus", "p-focus"), ExploreCategory("Party", "p-party"))
        val mixed = levyraSearchBrowseSections(zones, loose).single { it.kind == LevyraSearchBrowseSectionKind.Mixed }

        assertEquals(listOf("New releases", "Italy Wave", "Focus", "Party"), mixed.labels())
    }

    @Test
    fun onlyNewReleasesAndLocalWaveWhenEveryOtherZoneIsCovered() {
        val featuredOnly = zones.take(2)
        val sections = levyraSearchBrowseSections(featuredOnly, emptyList())

        assertEquals(listOf("New releases", "Italy Wave"), sections.single().labels())
        assertTrue(levyraSearchBrowseSections(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun tileAndSectionKeysAreUnique() {
        val sections = levyraSearchBrowseSections(zones, categories)
        val tileKeys = sections.flatMap { section -> section.tiles.map { it.key } }

        assertEquals(tileKeys.size, tileKeys.toSet().size)
        assertEquals(sections.size, sections.map { it.key }.toSet().size)
    }

    @Test
    fun picksTitleIsLocalizedForEveryLanguage() {
        searchBrowseLocalizationCodes().forEach { code ->
            assertTrue(code, searchBrowseCopy(code).levyraPicks.isNotBlank())
        }
    }

    private fun release(id: String, album: String, cover: String) = Track(
        id = id, title = album, artist = "Artist $id", album = album, durationMs = 180_000L,
        streamUrl = "", videoUrl = "", thumbnailUrl = cover, largeThumbnailUrl = cover, source = "test",
        moodTags = emptySet(), energy = 50, vocal = 50, replayScore = 50, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    @Test
    fun newReleasesUsesTheWorldCoverAndLocalWaveTheLocalOne() {
        val covers = searchBrowseZoneCovers(
            localFresh = listOf(release("l1", "Local album", "https://img/local.jpg")),
            worldFresh = listOf(release("w1", "World album", "https://img/world.jpg"))
        )

        assertEquals("https://img/world.jpg", covers[ExploreCatalog.NEW_RELEASES_ZONE_ID])
        assertEquals("https://img/local.jpg", covers[ExploreCatalog.LOCAL_WAVE_ZONE_ID])
    }

    @Test
    fun localOnlyReleasesGiveEachFeaturedTileADifferentCover() {
        val covers = searchBrowseZoneCovers(
            localFresh = listOf(release("l1", "First", "https://img/1.jpg"), release("l2", "Second", "https://img/2.jpg")),
            worldFresh = emptyList()
        )

        assertEquals("https://img/1.jpg", covers[ExploreCatalog.NEW_RELEASES_ZONE_ID])
        assertEquals("https://img/2.jpg", covers[ExploreCatalog.LOCAL_WAVE_ZONE_ID])
    }

    @Test
    fun missingCoversLeaveTheZoneArtworkFallbackInCharge() {
        assertTrue(searchBrowseZoneCovers(emptyList(), emptyList()).isEmpty())
        assertTrue(searchBrowseZoneCovers(listOf(release("l1", "Blank", "")), emptyList()).isEmpty())
    }
}
