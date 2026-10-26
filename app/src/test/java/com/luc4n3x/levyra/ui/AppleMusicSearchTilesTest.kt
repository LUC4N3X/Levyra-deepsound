package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusicSearchTilesTest {
    @Test
    fun appleCuratorAbsorbsMappedAndEquivalentProviderCategories() {
        val tiles = appleSearchTiles(
            apple = listOf(curator("976439548", "Hip-Hop/Rap"), curator("1558256251", "Chill")),
            categories = listOf(
                ExploreCategory("Hip hop", "ggMPOg1uX1BmNzc2V2p0YXJ5"),
                ExploreCategory("Relax", "unmapped-relax"),
                ExploreCategory("Jazz", "unmapped-jazz")
            ),
            categoryArtwork = mapOf("unmapped-jazz" to "https://example.test/jazz.jpg"),
            zones = emptyList()
        )

        assertEquals(listOf("Hip-Hop/Rap", "Chill", "Jazz"), tiles.map { it.label })
        assertEquals(exploreCategoryDestination(ExploreCategory("Hip hop", "ggMPOg1uX1BmNzc2V2p0YXJ5")), tiles[0].destination)
        assertEquals("https://example.test/jazz.jpg", tiles[2].artworkUrl)
        assertTrue(tiles[0].toned)
    }

    @Test
    fun keepsEveryDistinctZoneAndDropsOnlyEquivalentsAndNewReleases() {
        val tiles = appleSearchTiles(
            apple = listOf(curator("1", "Pop")),
            categories = emptyList(),
            categoryArtwork = emptyMap(),
            zones = listOf(zone("pop", "Pop"), zone("lofi", "Lo-fi"), zone(ExploreCatalog.NEW_RELEASES_ZONE_ID, "Nuove uscite"), zone("rock", "Rock"))
        )

        assertEquals(listOf("Pop", "Lo-fi", "Rock"), tiles.map { it.label })
        assertEquals(exploreMoodDestination("lofi"), tiles[1].destination)
    }

    @Test
    fun localizedEquivalentsCollapseIntoTheAppleTile() {
        val tiles = appleSearchTiles(
            apple = listOf(curator("10", "Musica latina"), curator("11", "Musica di Natale"), curator("12", "R&B")),
            categories = listOf(ExploreCategory("Natale", "unmapped-natale"), ExploreCategory("R&B e soul", "unmapped-rnb")),
            categoryArtwork = emptyMap(),
            zones = listOf(zone("latino", "Latino"), zone("afro", "Afrobeats"))
        )

        assertEquals(listOf("Musica latina", "Musica di Natale", "R&B", "Afrobeats"), tiles.map { it.label })
    }

    @Test
    fun appleOnlyCuratorOpensByTitleWithoutProviderCategory() {
        val tile = appleSearchTiles(listOf(curator("1765343179", "Amapiano")), emptyList(), emptyMap(), emptyList()).single()
        val category = exploreCategoryDestinationValue(tile.destination)!!

        assertEquals("Amapiano", category.title)
        assertEquals("apple-curator:1765343179", category.params)
    }

    @Test
    fun columnsFollowApkWidthBuckets() {
        assertEquals(2, appleSearchColumns(411))
        assertEquals(3, appleSearchColumns(600))
        assertEquals(4, appleSearchColumns(800))
        assertEquals(6, appleSearchColumns(1280))
    }

    private fun curator(id: String, title: String) = AppleCatalogItem(
        id = id, kind = AppleCatalogItemKind.Other, title = title, subtitle = "",
        artworkTemplate = "https://example.test/$id/{w}x{h}{c}.{f}", backgroundColor = 0xFF3CBB7D.toInt(),
        url = "https://music.apple.com/it/curator/apple-music/$id"
    )

    private fun zone(id: String, label: String) = ExploreZone(id, label, "", label, 0xFF000000.toInt(), 0xFF111111.toInt())
}
