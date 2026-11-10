package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultVideoFoldTest {
    @Test
    fun cardAtRestIsNotFolded() {
        assertEquals(0f, videoFoldPixels(itemOffset = 0, itemSize = 900))
        assertEquals(0f, videoFoldPixels(itemOffset = 1000, itemSize = 900))
    }

    @Test
    fun foldFollowsScrollPastRestingEdgeAndStopsAtCardWidth() {
        assertEquals(300f, videoFoldPixels(itemOffset = -300, itemSize = 900))
        assertEquals(900f, videoFoldPixels(itemOffset = -2000, itemSize = 900))
    }
}
