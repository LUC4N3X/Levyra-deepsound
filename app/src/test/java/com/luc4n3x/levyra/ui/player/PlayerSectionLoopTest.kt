package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.player.SectionLoopState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.sectionLoopCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class PlayerSectionLoopTest {

    @Test
    fun valueDescribesEachStep() {
        assertNull(sectionLoopValue(SectionLoopState.Off, "tap to set end"))
        assertEquals("1:02 – tap to set end", sectionLoopValue(SectionLoopState.Marking("s", 62_400L), "tap to set end"))
        assertEquals("1:02 – 1:30", sectionLoopValue(SectionLoopState.Active("s", 62_400L, 90_100L), "tap to set end"))
        assertEquals("1:00:05", formatSectionLoopTime(3_605_000L))
    }

    @Test
    fun timeStaysAsciiUnderAnArabicDigitLocale() {
        val previous = Locale.getDefault(Locale.Category.FORMAT)
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-u-nu-arab"))
        try {
            assertEquals("1:02", formatSectionLoopTime(62_400L))
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous)
        }
    }

    @Test
    fun everySupportedLanguageHasItsOwnLabels() {
        val english = LevyraStrings.forCode("en").sectionLoopCopy()
        LevyraLanguageCatalog.languages.map { it.code }.filter { it != "en" }.forEach { code ->
            val copy = LevyraStrings.forCode(code).sectionLoopCopy()
            assertNotEquals(code, english.action, copy.action)
            assertNotEquals(code, english.setEnd, copy.setEnd)
        }
    }
}
