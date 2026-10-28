package com.luc4n3x.levyra.ui.selection

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSelectionLayoutTest {
    @Test
    fun `measured navigation and mini player are cleared without subtracting overlap`() {
        listOf(56, 124, 192, 268).forEach { chrome ->
            assertEquals(chrome, selectionBottomObstruction(chrome, 24, 0))
        }
    }

    @Test
    fun `keyboard replaces rather than adds to navigation clearance`() {
        assertEquals(320, selectionBottomObstruction(192, 24, 320))
    }

    @Test
    fun `system navigation remains safe without app chrome`() {
        assertEquals(48, selectionBottomObstruction(0, 48, 0))
    }

    @Test
    fun `detail overlays ignore root chrome but still clear the keyboard`() {
        assertEquals(300, selectionBottomObstruction(0, 24, 300))
    }
}
