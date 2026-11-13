package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedDecadeCollectionsLocalizationTest {

    private val requiredCodes = listOf(
        "en", "it", "es", "fr", "de", "pt", "pt-BR", "nl", "pl", "ro", "el", "sv", "da", "cs", "uk", "ru", "nb", "ca",
        "kk", "tr", "ar", "fa", "zh", "zh-Hant", "ja", "ko", "hi", "bn", "id", "ms", "vi", "th", "fil", "he", "hu",
        "fi", "bg", "hr", "sr", "sk", "et", "sq"
    )

    @Test
    fun `every required language has its own short collection copy`() {
        assertEquals(42, requiredCodes.toSet().size)
        assertEquals(requiredCodes.toSet(), curatedCollectionsLocalizationCodes())
        val english = curatedCollectionsCopy("en")
        requiredCodes.forEach { code ->
            val copy = curatedCollectionsCopy(code)
            listOf(copy.decadesTitle, copy.decadesSubtitle, copy.everydayTitle, copy.everydaySubtitle).forEach { value ->
                assertTrue("$code $value", value.isNotBlank() && value == value.trim())
                assertTrue("$code $value", value.codePointCount(0, value.length) <= 32)
            }
            assertEquals(code, 4, setOf(copy.decadesTitle, copy.decadesSubtitle, copy.everydayTitle, copy.everydaySubtitle).size)
            if (code != "en") {
                assertNotEquals(code, english.decadesSubtitle, copy.decadesSubtitle)
                assertNotEquals(code, english.everydaySubtitle, copy.everydaySubtitle)
            }
        }
    }
}
