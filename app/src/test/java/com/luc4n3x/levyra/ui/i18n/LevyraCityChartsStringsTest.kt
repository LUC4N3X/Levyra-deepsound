package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraCityChartsStringsTest {
    private val dailyTop100Codes = listOf(
        "en", "it", "es", "fr", "de", "pt", "pt-BR", "nl", "pl", "ro", "el", "sv", "da", "cs", "sk",
        "hr", "sr", "sq", "bg", "hu", "fi", "et", "nb", "ca", "uk", "ru", "kk", "tr", "ar", "fa",
        "zh", "zh-Hant", "ja", "ko", "hi", "bn", "id", "ms", "vi", "th", "fil", "he"
    )

    @Test
    fun codeListMatchesDailyTop100() {
        assertEquals(42, dailyTop100Codes.size)
        assertEquals(dailyTop100LocalizationCodes(), dailyTop100Codes.toSet())
    }

    @Test
    fun englishAndItalianExactValues() {
        val en = cityChartsCopy("en")
        assertEquals("City charts", en.title)
        assertEquals("What each city is playing right now", en.subtitle)
        val italian = cityChartsCopy("it")
        assertEquals("Classifiche città", italian.title)
        assertEquals("Cosa ascolta ogni città in questo momento", italian.subtitle)
    }

    @Test
    fun unknownCodeFallsBackToEnglish() {
        assertEquals(cityChartsCopy("en"), cityChartsCopy("xx"))
    }

    @Test
    fun everyDailyTop100LanguageShipsOwnCityChartsCopy() {
        val english = cityChartsCopy("en")
        dailyTop100Codes.forEach { code ->
            val copy = cityChartsCopy(code)
            assertTrue(code, copy.title.isNotBlank() && copy.subtitle.isNotBlank())
            if (code != "en") {
                assertNotEquals(code, english.title, copy.title)
                assertNotEquals(code, english.subtitle, copy.subtitle)
            }
        }
    }
}
