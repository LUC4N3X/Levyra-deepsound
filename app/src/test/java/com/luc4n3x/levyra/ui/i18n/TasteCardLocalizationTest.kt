package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.PulsePeriod
import com.luc4n3x.levyra.ui.library.TastePersona
import com.luc4n3x.levyra.ui.library.isExplorer
import com.luc4n3x.levyra.ui.library.tastePersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteCardLocalizationTest {
    @Test
    fun everySelectableLanguageShipsTasteCardCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(selectable, tasteCardLocalizationCodes())
        val english = tasteCardCopy("en")
        selectable.forEach { code ->
            val copy = tasteCardCopy(code)
            val values = listOf(copy.yourSound, copy.nightOwl, copy.earlyRiser, copy.daytime, copy.evening, copy.explorer, copy.loyal)
            assertTrue(code, values.all(String::isNotBlank))
            if (code != "en") assertNotEquals(code, english.explorer, copy.explorer)
        }
    }

    @Test
    fun brazilianPortugueseUsesItsOwnCopy() {
        assertEquals("Seu som", tasteCardCopy("pt-BR").yourSound)
        assertEquals("O teu som", tasteCardCopy("pt").yourSound)
    }

    @Test
    fun personaFollowsPeakHour() {
        assertEquals(TastePersona.NIGHT, tastePersona(23))
        assertEquals(TastePersona.NIGHT, tastePersona(2))
        assertEquals(TastePersona.MORNING, tastePersona(7))
        assertEquals(TastePersona.DAY, tastePersona(14))
        assertEquals(TastePersona.EVENING, tastePersona(19))
        assertNull(tastePersona(-1))
    }

    @Test
    fun explorerNeedsWideArtistSpread() {
        assertTrue(isExplorer(PulsePeriod(plays = 10, distinctArtists = 4)))
        assertFalse(isExplorer(PulsePeriod(plays = 10, distinctArtists = 3)))
        assertFalse(isExplorer(PulsePeriod(plays = 0, distinctArtists = 0)))
    }
}
