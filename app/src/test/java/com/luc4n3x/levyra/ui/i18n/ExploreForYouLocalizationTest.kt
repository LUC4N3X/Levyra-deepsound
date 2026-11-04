package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreForYouLocalizationTest {
    @Test
    fun everySelectableLanguageShipsForYouCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(selectable, exploreForYouLocalizationCodes())
        val english = exploreForYouCopy("en")
        selectable.forEach { code ->
            val copy = exploreForYouCopy(code)
            assertTrue(code, copy.title.isNotBlank() && copy.subtitle.isNotBlank())
            if (code != "en") assertNotEquals(code, english.subtitle, copy.subtitle)
        }
    }

    @Test
    fun italianAndBrazilianWording() {
        assertEquals("Per te", exploreForYouCopy("it").title)
        assertEquals("Para você", exploreForYouCopy("pt-BR").title)
    }
}
