package com.luc4n3x.levyra.data.lore

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArtistLoreLanguageTest {
    @Test
    fun everyAppLanguageTriesItsOwnWikipediaEditionBeforeEnglish() {
        mapOf(
            "nb" to "no",
            "fil" to "tl",
            "zh-Hant" to "zh",
            "sk" to "sk",
            "hr" to "hr",
            "bg" to "bg",
            "hu" to "hu",
            "fi" to "fi",
            "et" to "et",
            "ca" to "ca",
            "fa" to "fa",
            "ms" to "ms",
            "it" to "it",
            "en" to "en"
        ).forEach { (appLanguage, edition) ->
            assertEquals(appLanguage, edition, ArtistLoreRepository.preferredLanguage(appLanguage))
        }
        LevyraLanguageCatalog.languages.filter { it.code != "en" }.forEach { language ->
            assertNotEquals(language.code, "en", ArtistLoreRepository.preferredLanguage(language.code))
        }
    }
}
