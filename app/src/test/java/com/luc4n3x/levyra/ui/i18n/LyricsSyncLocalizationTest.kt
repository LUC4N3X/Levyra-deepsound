package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSyncLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, lyricsSyncLocalizationCodes())
        val english = lyricsSyncCopy("en")
        supported.forEach { code ->
            val copy = lyricsSyncCopy(code)
            val fields = listOf(
                copy.title, copy.hint, copy.earlierAction, copy.laterAction, copy.inSync,
                copy.trackScope, copy.trackScopeHint, copy.deviceScope, copy.deviceScopeHint,
                copy.manualLabel, copy.apply, copy.invalidInput, copy.advanced
            )
            assertTrue(code, fields.all(String::isNotBlank))
            assertTrue(code, copy.laterState("2.5 s").contains("2.5 s"))
            assertTrue(code, copy.earlierState("2.5 s").contains("2.5 s"))
            assertTrue(code, !copy.laterState("x").contains("{value}"))
            assertTrue(code, copy.invalidInput.contains("30"))
            assertNotEquals(code, copy.earlierAction, copy.laterAction)
            if (code != "en") assertNotEquals(code, english.hint, copy.hint)
        }
    }

    @Test
    fun italianCopyUsesPlainDirectionLabels() {
        val copy = lyricsSyncCopy("it")
        assertEquals("Anticipa testo", copy.earlierAction)
        assertEquals("Ritarda testo", copy.laterAction)
        assertEquals("Testo ritardato di 3.0 s", copy.laterState("3.0 s"))
    }
}
