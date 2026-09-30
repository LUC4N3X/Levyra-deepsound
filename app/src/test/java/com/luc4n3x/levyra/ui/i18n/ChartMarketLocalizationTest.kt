package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMarketLocalizationTest {

    private val codes = LevyraLanguageCatalog.languages.map { it.code }

    @Test
    fun everySupportedLanguageHasATranslatedChartMarketBundle() {
        val english = chartMarketLocalizationEntries("en")
        assertEquals(chartMarketKeys, english.keys)
        codes.forEach { code ->
            val entries = chartMarketLocalizationEntries(code)
            assertEquals("Missing chart market keys for $code", chartMarketKeys, entries.keys)
            assertTrue("Blank chart market string for $code", entries.values.all(String::isNotBlank))
            if (code != "en") assertFalse("Chart market strings fall back to English for $code", entries == english)
        }
    }

    @Test
    fun placeholdersArePreservedInEveryLanguage() {
        codes.forEach { code ->
            val entries = chartMarketLocalizationEntries(code)
            val title = entries.getValue("chartMarketTitle")
            assertTrue("$code lost {count}", title.contains("{count}"))
            assertTrue("$code lost {country}", title.contains("{country}"))
            assertTrue("$code lost {query}", entries.getValue("chartMarketNoResults").contains("{query}"))
        }
    }

    @Test
    fun formattedCopyFillsEveryPlaceholder() {
        codes.forEach { code ->
            val strings = LevyraStrings.forCode(code)
            val title = strings.chartMarketTitle(50, "Italia")
            val empty = strings.chartMarketNoResults("xyz")
            assertFalse(title.contains('{'))
            assertTrue(title.contains("50") && title.contains("Italia"))
            assertFalse(empty.contains('{'))
            assertTrue(empty.contains("xyz"))
        }
        assertEquals("Top 50 Italia", LevyraStrings.forCode("it").chartMarketTitle(50, "Italia"))
        assertEquals("Scegli una classifica", LevyraStrings.forCode("it").chartMarketSheetTitle)
        assertEquals("Cerca paese", LevyraStrings.forCode("it").chartMarketSearchHint)
    }
}
