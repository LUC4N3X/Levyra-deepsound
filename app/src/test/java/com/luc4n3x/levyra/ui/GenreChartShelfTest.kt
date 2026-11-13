package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.lastfm.LastFmGenreTags
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreChartShelfTest {
    @Test
    fun shelfOffersAtLeastTenPlayableGenreChartsInEveryLanguage() {
        LevyraLanguageCatalog.selectableLanguages.forEach { language ->
            val zones = ExploreCatalog.getZones(LevyraStrings.forCode(language.code))
            val chartZones = exploreGenreChartZones(zones, language.code)
            assertTrue(language.code, chartZones.size >= 10)
            assertEquals(language.code, chartZones.size, chartZones.map { it.id }.toSet().size)
            chartZones.forEach { zone ->
                assertTrue("${language.code}:${zone.id}", LastFmGenreTags.forZone(zone.id).isNotEmpty())
                assertTrue("${language.code}:${zone.id}", zone.label.isNotBlank())
            }
        }
    }

    @Test
    fun chartDestinationRoundTripsZoneId() {
        assertEquals("chart-jazz", exploreGenreChartDestinationId(exploreGenreChartDestination("chart-jazz")))
        assertNull(exploreGenreChartDestinationId(exploreMoodDestination("chart-jazz")))
        assertNull(exploreGenreChartDestinationId(null))
    }
}
