package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.player.SectionLoopState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSectionLoopTest {

    @Test
    fun valueDescribesEachStep() {
        assertNull(sectionLoopValue(SectionLoopState.Off, "tap to set end"))
        assertEquals("1:02 – tap to set end", sectionLoopValue(SectionLoopState.Marking("s", 62_400L), "tap to set end"))
        assertEquals("1:02 – 1:30", sectionLoopValue(SectionLoopState.Active("s", 62_400L, 90_100L), "tap to set end"))
        assertEquals("1:00:05", formatSectionLoopTime(3_605_000L))
    }
}
