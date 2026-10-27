package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockMorphLocalizationTest {
    @Test
    fun everySupportedLanguageHasItsOwnDockMorphCopy() {
        val codes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(41, codes.size)
        assertEquals(codes, dockMorphCopies.keys)
        val english = dockMorphCopies.getValue("en")
        dockMorphCopies.forEach { (code, copy) ->
            assertTrue(code, listOf(copy.title, copy.subtitle, copy.tide, copy.merge, copy.mergeSubtitle, copy.off).all { it.isNotBlank() })
            if (code != "en") {
                assertNotEquals(code, english.subtitle, copy.subtitle)
                assertNotEquals(code, english.mergeSubtitle, copy.mergeSubtitle)
            }
            assertEquals(code, copy, LevyraStrings.forCode(code).dockMorph())
        }
    }
}
