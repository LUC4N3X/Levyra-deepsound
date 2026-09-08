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
    fun startupHomeKeepsFullLegacySeedPoolWithoutEnergyShelf() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val tracks = LevyraStartupCatalog.homeSections(language.code)
                .flatMap { it.tracks }
                .distinctBy { it.title.lowercase() to it.artist.lowercase() }
            val titles = tracks.mapTo(mutableSetOf()) { it.title }

            assertEquals("Startup seed count changed for ${language.code}", 20, tracks.size)
            assertTrue("Midnight City missing for ${language.code}", "Midnight City" in titles)
            assertTrue("Starboy missing for ${language.code}", "Starboy" in titles)
            assertTrue("Believer missing for ${language.code}", "Believer" in titles)
        }
    }

    @Test
    fun repairHomeSectionsMovesPersistedLegacyEnergyTracksIntoQuickPicks() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val locale = LevyraContentLocales.forLanguage(language.code)
            val startupTracks = LevyraStartupCatalog.homeSections(language.code).flatMap { it.tracks }
            val quickTrack = startupTracks.first { it.title == "Bohemian Rhapsody" }
            val energyTrack = startupTracks.first { it.title == "Midnight City" }

            val repaired = LevyraStartupCatalog.repairHomeSections(
                listOf(
                    HomeSection(locale.quickSectionTitle, listOf(quickTrack)),
                    HomeSection(locale.energySectionTitle, listOf(energyTrack))
                ),
                language.code
            )

            assertFalse(
                "Legacy energy shelf still restored for ${language.code}",
                repaired.any { section ->
                    section.title.trim().equals(locale.energySectionTitle.trim(), ignoreCase = true)
                }
            )
            val quick = repaired.single { section ->
                section.title.trim().equals(locale.quickSectionTitle.trim(), ignoreCase = true)
            }
            assertEquals(
                setOf("Bohemian Rhapsody", "Midnight City"),
                quick.tracks.mapTo(mutableSetOf()) { it.title }
            )
        }
    }

    @Test
    fun repairHomeSectionsMigratesLegacyEnergyShelfAfterLanguageChange() {
        val allEnergyTitles = LevyraLanguageCatalog.languages
            .map { language -> LevyraContentLocales.forLanguage(language.code).energySectionTitle.trim() }
            .filter { it.isNotEmpty() }

        LevyraLanguageCatalog.languages.forEach { savedLanguage ->
            val targetLanguageCode = if (savedLanguage.code == "en") "it" else "en"
            val savedLocale = LevyraContentLocales.forLanguage(savedLanguage.code)
            val startupTracks = LevyraStartupCatalog.homeSections(savedLanguage.code).flatMap { it.tracks }
            val quickTrack = startupTracks.first { it.title == "Bohemian Rhapsody" }
            val energyTrack = startupTracks.first { it.title == "Midnight City" }

            val repaired = LevyraStartupCatalog.repairHomeSections(
                listOf(
                    HomeSection(savedLocale.quickSectionTitle, listOf(quickTrack)),
                    HomeSection(savedLocale.energySectionTitle, listOf(energyTrack))
                ),
                targetLanguageCode
            )

            assertFalse(
                "Legacy ${savedLanguage.code} energy shelf survived after switching to $targetLanguageCode",
                repaired.any { section ->
                    allEnergyTitles.any { title -> section.title.trim().equals(title, ignoreCase = true) }
                }
            )
            val quick = repaired.single { section ->
                section.title.trim().equals(savedLocale.quickSectionTitle.trim(), ignoreCase = true)
            }
            assertEquals(
                setOf("Bohemian Rhapsody", "Midnight City"),
                quick.tracks.mapTo(mutableSetOf()) { it.title }
            )
        }
    }

    @Test
    fun chartFallbackStillContainsTwentyStartupSeeds() {
        val tracks = LevyraStartupCatalog.chartTracks("en")
        val titles = tracks.mapTo(mutableSetOf()) { it.title }

        assertEquals(20, tracks.size)
        assertTrue("Midnight City" in titles)
        assertTrue("Starboy" in titles)
        assertTrue("Believer" in titles)
    }
}
