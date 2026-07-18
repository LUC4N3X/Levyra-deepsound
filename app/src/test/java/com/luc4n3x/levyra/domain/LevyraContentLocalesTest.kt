package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraContentLocalesTest {
    @Test
    fun everySupportedLanguageHasDistinctArtistSuggestions() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val suggestions = LevyraContentLocales.artistSuggestions(language.code)
            assertTrue(suggestions.size >= 10)
            assertEquals(suggestions.size, suggestions.map(::artistIdentityKey).distinct().size)
            assertTrue(suggestions.all { it.isNotBlank() })
        }
    }

    @Test
    fun nonItalianLanguagesDoNotReuseItalianSuggestions() {
        val italian = LevyraContentLocales.artistSuggestions("it").map(::artistIdentityKey).toSet()
        LevyraLanguageCatalog.languages
            .filterNot { it.code == "it" }
            .forEach { language ->
                val localized = LevyraContentLocales.artistSuggestions(language.code).map(::artistIdentityKey).toSet()
                assertTrue(italian.intersect(localized).isEmpty())
            }
    }


    @Test
    fun everySupportedLanguageResolvesToAnExplicitChartRegion() {
        val regionIds = ChartsCatalog.regions.map { it.id }.toSet()
        LevyraLanguageCatalog.languages.forEach { language ->
            val locale = LevyraContentLocales.forLanguage(language.code)
            assertTrue("Missing chart region for ${language.code}: ${locale.chartRegionId}", locale.chartRegionId in regionIds)
            assertEquals(locale.chartRegionId, ChartsCatalog.defaultRegionForLanguage(language.code).id)
        }
    }

    @Test
    fun majorAsianLanguagesUseLocalizedDiscoveryData() {
        assertEquals("jp", LevyraContentLocales.forLanguage("ja-JP").chartRegionId)
        assertEquals("kr", LevyraContentLocales.forLanguage("ko-KR").chartRegionId)
        assertEquals("in", LevyraContentLocales.forLanguage("hi-IN").chartRegionId)
        assertEquals("id", LevyraContentLocales.forLanguage("in-ID").chartRegionId)
        assertEquals("vn", LevyraContentLocales.forLanguage("vi-VN").chartRegionId)
        assertEquals("th", LevyraContentLocales.forLanguage("th-TH").chartRegionId)
        assertEquals("ph", LevyraContentLocales.forLanguage("fil-PH").chartRegionId)
        assertEquals("ph", LevyraContentLocales.forLanguage("tl-PH").chartRegionId)
        assertEquals("il", LevyraContentLocales.forLanguage("he-IL").chartRegionId)
        assertEquals("il", LevyraContentLocales.forLanguage("iw-IL").chartRegionId)
        assertTrue(LevyraContentLocales.artistSuggestions("ja").contains("YOASOBI"))
        assertTrue(LevyraContentLocales.artistSuggestions("ko").contains("BTS"))
        assertTrue(LevyraContentLocales.artistSuggestions("vi").contains("Sơn Tùng M-TP"))
        assertTrue(LevyraContentLocales.artistSuggestions("fil").contains("Cup of Joe"))
        assertTrue(LevyraContentLocales.artistSuggestions("he").contains("נועה קירל"))
    }

    @Test
    fun artistSuggestionMatchingUsesNormalizedLanguageAndPrimaryArtist() {
        assertTrue(LevyraContentLocales.isArtistSuggestionForLanguage("Bad Bunny feat. Feid", "es-MX"))
        assertTrue(LevyraContentLocales.isArtistSuggestionForLanguage("周杰伦", "zh-CN"))
        assertTrue(LevyraContentLocales.isArtistSuggestionForLanguage("MiyaGi & Andy Panda", "ru-RU"))
        assertFalse(LevyraContentLocales.isArtistSuggestionForLanguage("Sfera Ebbasta", "fr-FR"))
    }
}
