package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavLocalizationTest {
    @Test
    fun everySelectableLanguageHasItsOwnFloatingNavCopy() {
        val codes = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(42, codes.size)
        assertEquals(codes, floatingNavCopies.keys)
        val english = floatingNavCopies.getValue("en")
        floatingNavCopies.forEach { (code, copy) ->
            assertTrue(
                code,
                listOf(
                    copy.title, copy.subtitle, copy.side, copy.sideSubtitle, copy.left, copy.right, copy.handle,
                    copy.expand, copy.collapse, copy.moveUp, copy.moveDown, copy.switchSide
                ).all { it.isNotBlank() }
            )
            if (code != "en") {
                assertNotEquals(code, english.subtitle, copy.subtitle)
            }
            assertEquals(code, copy, LevyraStrings.forCode(code).floatingNav())
        }
    }

    @Test
    fun brazilianPortugueseIsWrittenSeparatelyFromEuropeanPortuguese() {
        val european = LevyraStrings.forCode("pt").floatingNav()
        val brazilian = LevyraStrings.forCode(LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE).floatingNav()
        assertEquals(floatingNavCopies.getValue("pt-BR"), brazilian)
        listOf(
            european.title to brazilian.title,
            european.subtitle to brazilian.subtitle,
            european.side to brazilian.side,
            european.sideSubtitle to brazilian.sideSubtitle,
            european.handle to brazilian.handle,
            european.expand to brazilian.expand,
            european.collapse to brazilian.collapse,
            european.moveUp to brazilian.moveUp,
            european.moveDown to brazilian.moveDown,
            european.switchSide to brazilian.switchSide
        ).forEach { (pt, br) -> assertNotEquals(pt, br) }
    }
}
