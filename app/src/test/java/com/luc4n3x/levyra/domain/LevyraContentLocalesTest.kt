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
    fun artistSuggestionMatchingUsesNormalizedLanguageAndPrimaryArtist() {
        assertTrue(LevyraContentLocales.isArtistSuggestionForLanguage("Bad Bunny feat. Feid", "es-MX"))
        assertTrue(LevyraContentLocales.isArtistSuggestionForLanguage("周杰伦", "zh-CN"))
        assertTrue(LevyraContentLocales.isArtistSuggestionForLanguage("MiyaGi & Andy Panda", "ru-RU"))
        assertFalse(LevyraContentLocales.isArtistSuggestionForLanguage("Sfera Ebbasta", "fr-FR"))
    }
}
