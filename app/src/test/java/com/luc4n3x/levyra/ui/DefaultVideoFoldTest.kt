package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultVideoFoldTest {
    @Test
    fun cardAtRestIsNotFolded() {
        assertEquals(0f, videoFoldPixels(itemOffset = 48, itemSize = 900, restingStart = 48))
        assertEquals(0f, videoFoldPixels(itemOffset = 1000, itemSize = 900, restingStart = 48))
    }

    @Test
    fun foldFollowsScrollPastRestingEdgeAndStopsAtCardWidth() {
        assertEquals(300f, videoFoldPixels(itemOffset = -252, itemSize = 900, restingStart = 48))
        assertEquals(900f, videoFoldPixels(itemOffset = -2000, itemSize = 900, restingStart = 48))
    }
}
