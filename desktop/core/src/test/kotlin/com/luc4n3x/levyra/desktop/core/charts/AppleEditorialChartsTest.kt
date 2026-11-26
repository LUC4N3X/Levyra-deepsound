package com.luc4n3x.levyra.desktop.core.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleEditorialChartsTest {
    private fun playlist(id: String, title: String) =
        AppleEditorialItem(id, "playlist", title, "", "", "", 0L, "", false)

    @Test
    fun localCountryAndGlobalLeadTheDailyCharts() {
        val sections = listOf(
            AppleEditorialSection(
                "squareLockup",
                listOf(
                    playlist("pl.606afcbb70264d2eb2b51d8dbcfa6a12", "Top 100: Stati Uniti"),
                    playlist(AppleEditorialCharts.GLOBAL_PLAYLIST_ID, "Top 100: Globale"),
                    playlist("pl.737e067787df485a8062e2c4927d94db", "Top 100: Italia")
                )
            )
        )
        val charts = AppleEditorialCharts.dailyCharts(sections, "it")
        assertEquals(listOf("Italia", "Globale", "Stati Uniti"), charts.map { it.label })
        assertEquals(true, charts[1].global)
    }

    @Test
    fun cityChartsSkipCountryCharts() {
        val sections = listOf(
            AppleEditorialSection(
                "squareLockup",
                listOf(
                    playlist("pl.c16207854c324cd1822c2632215c1fb3", "Top 25: Milano"),
                    playlist("pl.737e067787df485a8062e2c4927d94db", "Top 100: Italia"),
                    playlist("pl.aaaaaaaaaaaaaaaa", "Acoustic Hits")
                )
            )
        )
        val cities = AppleEditorialCharts.cityCharts(sections)
        assertEquals(listOf("Milano"), cities.map { it.name })
        assertEquals("Top 25", cities.single().badge)
    }

    @Test
    fun parsesChartTitlesAndArtwork() {
        assertNull(AppleEditorialCharts.chartParts("Acoustic Hits"))
        assertEquals("Top 25" to "Roma", AppleEditorialCharts.chartParts("Top 25: Roma"))
        assertEquals("https://a/600x600bb.jpg", AppleEditorialCharts.artwork("https://a/{w}x{h}bb.{f}", 600))
        assertNull(AppleEditorialCharts.playlistId("123"))
    }
}
