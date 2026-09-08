package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraStartupCatalogTest {
    @Test
    fun homeSectionsDoNotExposeLegacyEnergyShelfInAnyLanguage() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val locale = LevyraContentLocales.forLanguage(language.code)
            val sections = LevyraStartupCatalog.homeSections(language.code)

            assertFalse(
                "Legacy energy shelf still visible for ${language.code}",
                sections.any { section ->
                    section.title.trim().equals(locale.energySectionTitle.trim(), ignoreCase = true)
                }
            )
        }
    }

    @Test
    fun repairHomeSectionsDropsPersistedLegacyEnergyShelf() {
        val locale = LevyraContentLocales.forLanguage("en")
        val chartTracks = LevyraStartupCatalog.chartTracks("en")
        val quickTrack = chartTracks.first { it.title == "Bohemian Rhapsody" }
        val energyTrack = chartTracks.first { it.title == "Midnight City" }

        val repaired = LevyraStartupCatalog.repairHomeSections(
            listOf(
                HomeSection(locale.quickSectionTitle, listOf(quickTrack)),
                HomeSection(locale.energySectionTitle, listOf(energyTrack))
            ),
            "en"
        )

        assertEquals(listOf(locale.quickSectionTitle), repaired.map { it.title })
    }

    @Test
    fun chartFallbackKeepsLegacyFocusSeedsWithoutRenderingTheirShelf() {
        val titles = LevyraStartupCatalog.chartTracks("en").mapTo(mutableSetOf()) { it.title }

        assertTrue("Midnight City" in titles)
        assertTrue("Starboy" in titles)
        assertTrue("Believer" in titles)
    }
}
