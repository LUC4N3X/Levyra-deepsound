package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TorqueCoverHeightTest {
    @Test
    fun coverKeepsPreferredSizeWhenEverythingFits() {
        assertEquals(400, torqueCoverHeightPx(viewportPx = 1000, preferredPx = 400, minPx = 160, restPx = 500))
    }

    @Test
    fun coverShrinksSoThePlayerFitsTheScreen() {
        assertEquals(330, torqueCoverHeightPx(viewportPx = 904, preferredPx = 406, minPx = 160, restPx = 574))
    }

    @Test
    fun coverNeverDropsBelowMinimum() {
        assertEquals(160, torqueCoverHeightPx(viewportPx = 500, preferredPx = 406, minPx = 160, restPx = 600))
    }

    @Test
    fun minimumNeverExceedsPreferred() {
        assertEquals(120, torqueCoverHeightPx(viewportPx = 300, preferredPx = 120, minPx = 160, restPx = 400))
    }
}
