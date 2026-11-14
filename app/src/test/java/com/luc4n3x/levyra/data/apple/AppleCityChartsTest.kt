package com.luc4n3x.levyra.data.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleCityChartsTest {

    private fun playlist(id: String, title: String) =
        AppleCatalogItem(id = id, kind = AppleCatalogItemKind.Playlist, title = title, subtitle = "Apple Music")

    private fun shelf(vararg items: AppleCatalogItem) =
        AppleCatalogShelf("room", "", AppleCatalogShelfKind.Grid, items.toList())

    private fun page(vararg shelves: AppleCatalogShelf) = AppleCatalogPage("it", shelves.toList(), 0L)

    private val milano = playlist("pl.aaaaaaaa", "Top 25: Milano")
    private val roma = playlist("pl.bbbbbbbb", "Top 25: Roma")

    @Test
    fun keepsCityCharts() {
        assertEquals(
            listOf(AppleCityChart("pl.aaaaaaaa", "Top 25", "Milano"), AppleCityChart("pl.bbbbbbbb", "Top 25", "Roma")),
            AppleCityCharts.charts(page(shelf(milano, roma)))
        )
    }

    @Test
    fun excludesCountryCharts() {
        val italy = playlist("pl.737e067787df485a8062e2c4927d94db", "Top 100: Italia")
        val world = playlist("pl.ffffffffffffffff", "Top 100: Mondo")
        val knownCountry = playlist("pl.737e067787df485a8062e2c4927d94db", "Top 25: Italia")
        assertEquals(emptyList<AppleCityChart>(), AppleCityCharts.charts(page(shelf(italy, world, knownCountry))))
    }

    @Test
    fun removesDuplicatesKeepsOrder() {
        assertEquals(
            listOf("pl.bbbbbbbb", "pl.aaaaaaaa"),
            AppleCityCharts.charts(page(shelf(roma, milano), shelf(roma))).map { it.playlistId }
        )
    }

    @Test
    fun capsAtThirty() {
        val items = (1..40).map { playlist("pl.city%08d".format(it), "Top 25: City $it") }
        assertEquals(30, AppleCityCharts.charts(page(shelf(*items.toTypedArray()))).size)
    }

    @Test
    fun noCityShelvesGivesEmptyList() {
        assertEquals(emptyList<AppleCityChart>(), AppleCityCharts.charts(null))
        val other = playlist("pl.cccccccc", "Chill Mix")
        assertEquals(emptyList<AppleCityChart>(), AppleCityCharts.charts(page(shelf(other))))
    }

    @Test
    fun recognisesCountryChartIds() {
        assertTrue(AppleDailyTop100.isCountryChart(AppleDailyTop100.GLOBAL_PLAYLIST_ID))
        assertTrue(AppleDailyTop100.isCountryChart("pl.737e067787df485a8062e2c4927d94db"))
        assertFalse(AppleDailyTop100.isCountryChart("pl.aaaaaaaa"))
    }
}
