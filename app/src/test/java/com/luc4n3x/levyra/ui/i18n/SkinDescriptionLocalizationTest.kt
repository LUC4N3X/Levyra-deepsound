package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinDescriptionLocalizationTest {
    private val skinBodies: List<Pair<String, (String) -> String>> = listOf(
        "Default" to { code -> LevyraSkinStrings.forCode(code).defaultBody },
        "Nocturne" to { code -> LevyraSkinStrings.forCode(code).nocturneBody },
        "Prism" to { code -> LevyraPrismStrings.forCode(code).body },
        "Torque" to { code -> LevyraPrismStrings.forCode(code).torqueBody },
        "Ivory" to { code -> LevyraPrismStrings.forCode(code).ivoryBody },
        "Spotify" to { code -> LevyraPrismStrings.forCode(code).spotifyBody },
        "Apple" to { code -> LevyraPrismStrings.forCode(code).appleMusicBody },
        "YouTube Music" to { code -> LevyraPrismStrings.forCode(code).youTubeMusicBody },
        "Qobuz" to { code -> LevyraQobuzSkinStrings.forCode(code).skinBody },
        "Atelier" to { code -> LevyraAtelierStrings.forCode(code).skinBody },
        "Glacier" to { code -> LevyraMonoStrings.forCode(code).skinBody }
    )

    @Test
    fun everySkinDescriptionIsTranslatedForEverySelectableLanguage() {
        val missing = LevyraLanguageCatalog.selectableLanguages.map { it.code }.filter { it != "en" }.flatMap { code ->
            skinBodies.filter { (_, body) -> body(code) == body("en") }.map { (skin, _) -> "$skin/$code" }
        }
        assertTrue("Untranslated skin descriptions: $missing", missing.isEmpty())
    }

    @Test
    fun traditionalChineseDoesNotFallBackToSimplified() {
        val sameAsSimplified = skinBodies.filter { it.first in setOf("Qobuz", "Atelier", "Glacier") }
            .filter { (_, body) -> body("zh-Hant") == body("zh") }.map { it.first }
        assertTrue("zh-Hant falls back to zh: $sameAsSimplified", sameAsSimplified.isEmpty())
        val traditional = LevyraQobuzSkinStrings.forCode("zh-Hant")
        val simplified = LevyraQobuzSkinStrings.forCode("zh")
        assertEquals(simplified.discover, traditional.discover)
        assertEquals(simplified.queueTitle, traditional.queueTitle)
    }

    @Test
    fun atelierKeepsItsItalianCopyAndLocalizesRussianDescription() {
        assertTrue(LevyraAtelierStrings.forCode("it").shelf.isNotBlank())
        assertTrue(LevyraAtelierStrings.forCode("ru").skinBody.startsWith("Тёмная комната"))
        assertTrue(LevyraMonoStrings.forCode("ru").skinBody.startsWith("Тёмный минимализм"))
    }
}
