package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraStartupCatalogTest {

    private val languages = listOf("it", "es", "fr", "de", "pt", "nl", "pl", "ro", "el", "sv", "da", "cs", "uk", "ru", "tr", "en")

    @Test
    fun startupSectionsNeverExposeLocalOrbitShelf() {
        languages.forEach { language ->
            val titles = LevyraStartupCatalog.homeSections(language).map { it.title }
            assertFalse(
                "Local orbit shelf still present for $language: $titles",
                titles.any { title ->
                    title.contains("orbit", ignoreCase = true) ||
                        title.contains("órbita", ignoreCase = true) ||
                        title.contains("orbita", ignoreCase = true)
                }
            )
        }
    }

    @Test
    fun italianStartupSectionsDropItaliaNellaTuaOrbita() {
        val titles = LevyraStartupCatalog.homeSections("it").map { it.title }

        assertFalse(titles.contains("Italia nella tua orbita"))
        assertEquals(listOf("Scelte rapide", "Energia immediata"), titles)
    }

    @Test
    fun startupSectionsKeepQuickAndEnergyForEveryLanguage() {
        languages.forEach { language ->
            val locale = LevyraContentLocales.forLanguage(language)
            val titles = LevyraStartupCatalog.homeSections(language).map { it.title }

            assertEquals("Unexpected shelves for $language", 2, titles.size)
            assertEquals(listOf(locale.quickSectionTitle, locale.energySectionTitle), titles)
        }
    }

    @Test
    fun startupSectionsNeverShipLocalSeedTracks() {
        val removedLocalSeeds = listOf("LA FINE DEL MONDO", "Tuta Gold", "Cenere", "Despacito", "Stefania", "Europapa", "Roller")

        languages.forEach { language ->
            val titles = LevyraStartupCatalog.homeSections(language).flatMap { section -> section.tracks.map { it.title } }
            removedLocalSeeds.forEach { removed ->
                assertFalse("$removed still seeded for $language", titles.contains(removed))
            }
        }
    }

    @Test
    fun repairTracksKeepsUnknownSeedsUntouched() {
        val unknown = LevyraStartupCatalog.homeSections("it").first().tracks.first().copy(
            title = "Brano non presente nel catalogo",
            artist = "Artista Sconosciuto"
        )

        val repaired = LevyraStartupCatalog.repairTracks(listOf(unknown), "it")

        assertEquals(listOf(unknown), repaired)
    }

    @Test
    fun chartSeedsStayStableAndDeduplicated() {
        val tracks = LevyraStartupCatalog.chartTracks("it")

        assertTrue(tracks.isNotEmpty())
        assertEquals(tracks.map { it.id }.distinct().size, tracks.size)
        assertTrue(tracks.all { it.id.startsWith("chart-seed-") })
    }

    @Test
    fun everySupportedLanguageNormalizesToAKnownLocale() {
        languages.forEach { language ->
            val normalized = LevyraLanguageCatalog.normalize(language)
            val locale = LevyraContentLocales.forLanguage(normalized)

            assertTrue("Blank quick shelf for $language", locale.quickSectionTitle.isNotBlank())
            assertTrue("Blank energy shelf for $language", locale.energySectionTitle.isNotBlank())
        }
    }
}
