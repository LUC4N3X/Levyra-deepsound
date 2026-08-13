package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class AudioAndRegionLocalizationRegressionTest {
    @Test
    fun truePeakLimiterHasLocalizedCopyForEverySupportedLanguage() {
        val labels = LevyraLanguageCatalog.languages.associate { language ->
            language.code to LevyraStrings.forCode(language.code).truePeakLimiter
        }

        assertEquals(LevyraLanguageCatalog.languages.size, labels.size)
        assertTrue(labels.values.all(String::isNotBlank))
        assertEquals("Limitador true-peak", labels.getValue("pt"))
        assertEquals("トゥルーピークリミッター", labels.getValue("ja"))
        assertEquals("محدد الذروة الحقيقية", labels.getValue("ar"))
        assertEquals("מגביל שיא אמיתי", labels.getValue("he"))
        assertTrue(labels.values.toSet().size >= 20)
    }

    @Test
    fun portugueseDiscoveryDefaultsToPortugal() {
        val locale = LevyraContentLocales.forLanguage("pt-PT")
        val quickSearches = LevyraContentLocales.quickSearches("pt-PT")
        val artists = LevyraContentLocales.artistSuggestions("pt-PT")

        assertEquals("pt", locale.chartRegionId)
        assertEquals("pt", locale.chartCountry)
        assertEquals("PT", locale.gl)
        assertTrue(locale.localSectionTitle.contains("Portugal", ignoreCase = true))
        assertTrue(locale.homeQueries.any { it.contains("Portugal", ignoreCase = true) })
        assertFalse(locale.homeQueries.any { it.contains("Brasil", ignoreCase = true) })
        assertTrue(quickSearches.any { it.contains("Portugal", ignoreCase = true) })
        assertFalse(quickSearches.any { it.contains("Brasil", ignoreCase = true) })
        assertTrue(artists.contains("Dillaz"))
    }

    @Test
    fun changingUiLanguagePreservesSelectedContentRegion() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraViewModel.kt not found")
        val content = Files.readString(source)
        val start = content.indexOf("private fun applyLanguageContent")
        val end = content.indexOf("fun restartOnboarding", start)
        assertTrue(start >= 0 && end > start)
        val block = content.substring(start, end)

        assertTrue(block.contains("val selectedChartRegion = ChartsCatalog.regions"))
        assertTrue(block.contains("firstOrNull { it.id == _state.value.selectedChartId }"))
        assertTrue(block.contains("selectedChartId = selectedChartRegion.id"))
        assertFalse(block.contains("selectedChartId = defaultChartRegion.id"))
    }
}
