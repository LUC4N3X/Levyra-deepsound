package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerLocalizationTest {
    @Test
    fun everySelectableLanguageShipsItsOwnSleepTimerCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertTrue((selectable - sleepTimerLocalizationCodes()).toString(), sleepTimerLocalizationCodes().containsAll(selectable))
    }

    @Test
    fun sleepSheetUsesTranslatedCopy() {
        val english = LevyraStrings.forCode("en").systemPlayerCopy()
        val russian = LevyraStrings.forCode("ru").systemPlayerCopy()
        assertEquals("Выберите, когда остановить музыку", russian.sleepChooseDuration)
        assertEquals("мин", russian.sleepMinuteUnit)
        assertEquals("+15 мин", russian.sleepAddFifteen)
        assertNotEquals(english.sleepActive, russian.sleepActive)
        assertEquals("Escolha quando a música deve parar", LevyraStrings.forCode("pt-BR").systemPlayerCopy().sleepChooseDuration)
    }
}
