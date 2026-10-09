package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshCurrentsLocalizationTest {

    private val codes = LevyraLanguageCatalog.languages.map { it.code }

    @Test
    fun everySupportedLanguageResolvesTheFreshCurrentsBundle() {
        val english = freshCurrentsLocalizationEntries("en")
        assertEquals(freshCurrentsKeyOrder.toSet(), english.keys)
        codes.forEach { code ->
            val entries = freshCurrentsLocalizationEntries(code)
            assertEquals("Missing fresh currents keys for $code", freshCurrentsKeyOrder.toSet(), entries.keys)
            assertTrue("Blank fresh currents string for $code", entries.values.all(String::isNotBlank))
        }
    }

    @Test
    fun translatedLanguagesDoNotFallBackToEnglishCopy() {
        val english = freshCurrentsLocalizationEntries("en")
        freshCurrentsEntries.keys.filterNot { it == "en" }.forEach { code ->
            val entries = freshCurrentsEntries.getValue(code)
            assertFalse(
                "Fresh currents copy falls back to English for $code",
                entries.getValue("freshMomentTitle") == english.getValue("freshMomentTitle")
            )
        }
    }

    @Test
    fun exploreSectionKeepsItsNameWhileTheNewCopyIsAvailable() {
        val italian = LevyraStrings.forCode("it")

        assertEquals("Correnti fresche", italian.exploreFresh)
        assertEquals("I brani del momento", italian.freshMomentTitle)
        assertEquals("Nel mondo", italian.freshScopeWorld)
        codes.forEach { code ->
            val strings = LevyraStrings.forCode(code)
            assertTrue("$code lost the moment title", strings.freshMomentTitle.isNotBlank())
            assertTrue("$code lost the world scope", strings.freshScopeWorld.isNotBlank())
        }
    }
}
