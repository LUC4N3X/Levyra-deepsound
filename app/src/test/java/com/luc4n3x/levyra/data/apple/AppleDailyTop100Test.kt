package com.luc4n3x.levyra.data.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleDailyTop100Test {

    private fun playlist(id: String, title: String, url: String = "") =
        AppleCatalogItem(id = id, kind = AppleCatalogItemKind.Playlist, title = title, subtitle = "Curator", url = url)

    private fun page(vararg items: AppleCatalogItem) =
        AppleCatalogPage("it", listOf(AppleCatalogShelf("room", "", AppleCatalogShelfKind.Grid, items.toList())), 0L)

    private val global = playlist(AppleDailyTop100.GLOBAL_PLAYLIST_ID, "Top 100: Globale")
    private val italy = playlist("pl.737e067787df485a8062e2c4927d94db", "Top 100: Italia")
    private val usa = playlist("pl.606afcbb70264d2eb2b51d8dbcfa6a12", "Top 100: Stati Uniti")

    @Test
    fun localCountryLeadsThenGlobalThenNeighboursInPageOrder() {
        val charts = AppleDailyTop100.charts(page(global, italy, usa), "it")
        assertEquals(listOf(italy.id, global.id, usa.id), charts.map { it.playlistId })
        assertEquals(listOf(false, true, false), charts.map { it.global })
        assertEquals(listOf("Italia", "Globale", "Stati Uniti"), charts.map { it.label })
    }

    @Test
    fun countryAlreadyFirstKeepsGlobalSecond() {
        val germany = playlist("pl.c10a2c113db14685a0b09fa5834d8e8b", "Top 100: Deutschland")
        val charts = AppleDailyTop100.charts(page(germany, global, usa), "de")
        assertEquals(listOf(germany.id, global.id, usa.id), charts.map { it.playlistId })
    }

    @Test
    fun alphabeticalRoomStillLeadsWithStorefrontCountryGlobalAndMajorMarkets() {
        val anguilla = playlist("pl.48bbe91b5d944b0aa7b1e90a3889b6a7", "Top 100: Anguilla")
        val antigua = playlist("pl.cca0d50798424e4e871820a03719e841", "Top 100: Antigua e Barbuda")
        val saudi = playlist("pl.a5365fa3b6ec4a34994339ca100801ae", "Top 100: Arabia Saudita")
        val armenia = playlist("pl.42abb2144d594137a8fb4d37a9f35b42", "Top 100: Armenia")
        val uk = playlist("pl.c2273b7e89b44121b3093f67228918e7", "Top 100: UK")
        val charts = AppleDailyTop100.charts(page(anguilla, antigua, saudi, armenia, global, italy, usa, uk), "it")
        assertEquals(listOf(italy.id, global.id, usa.id, uk.id, saudi.id, anguilla.id, antigua.id, armenia.id), charts.map { it.playlistId })
    }

    @Test
    fun missingGlobalAndUnknownStorefrontFailSoft() {
        val charts = AppleDailyTop100.charts(page(usa, italy), "hr")
        assertEquals(listOf(usa.id, italy.id), charts.map { it.playlistId })
        assertTrue(charts.none { it.global })
    }

    @Test
    fun skipsNonChartsInvalidIdsAndDuplicatesAndCapsTheRail() {
        val city = playlist("pl.c16207854c324cd1822c2632215c1fb3", "Top 25: Milan")
        val album = AppleCatalogItem(id = "123456", kind = AppleCatalogItemKind.Album, title = "Top 100 Hits", subtitle = "")
        val invalid = playlist("not-a-playlist", "Top 100: Nowhere")
        val fromUrl = playlist("", "Top 100: Spagna", "https://music.apple.com/it/playlist/top-100-spagna/pl.0d656d7feae64198bc5fb1b02786ed75")
        val extra = (1..10).map { playlist("pl.%032d".format(it), "Top 100: Country $it") }
        val charts = AppleDailyTop100.charts(page(*(listOf(global, italy, italy, city, album, invalid, fromUrl) + extra).toTypedArray()), "it")
        assertEquals(8, charts.size)
        assertEquals(charts.map { it.playlistId }.distinct(), charts.map { it.playlistId })
        assertTrue(charts.any { it.playlistId == "pl.0d656d7feae64198bc5fb1b02786ed75" && it.label == "Spagna" })
        assertTrue(charts.none { it.label.contains("Milan") || it.label.contains("Nowhere") || it.label.contains("Hits") })
    }

    @Test
    fun emptyOrMissingPageYieldsNoCharts() {
        assertTrue(AppleDailyTop100.charts(null, "it").isEmpty())
        assertTrue(AppleDailyTop100.charts(page(), "it").isEmpty())
    }

    @Test
    fun labelsStripLocalizedChartPrefixes() {
        assertEquals("Italia", AppleDailyTop100.label("Top 100: Italia"))
        assertEquals("France", AppleDailyTop100.label("Le top 100 : France"))
        assertEquals("International", AppleDailyTop100.label("Le top 100 international"))
        assertEquals("США", AppleDailyTop100.label("Топ-100 США"))
        assertEquals("Южной Кореи", AppleDailyTop100.label("Топ-100 Южной Кореи"))
        assertEquals("日本", AppleDailyTop100.label("トップ100：日本"))
        assertEquals("Isole Cayman", AppleDailyTop100.label("Top 100: Isole Cayman "))
        assertEquals("Top 100", AppleDailyTop100.label("Top 100"))
    }

    @Test
    fun dailyTop100RoomIsAppleCatalogPath() {
        assertEquals("room/6504092645", AppleCatalogPath.DailyTop100.segment)
    }
}
