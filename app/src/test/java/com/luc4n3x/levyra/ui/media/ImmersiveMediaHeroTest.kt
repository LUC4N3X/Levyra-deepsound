package com.luc4n3x.levyra.ui.media

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ImmersiveMediaHeroTest {

    @Test
    fun portraitHeroUsesCompactViewportFraction() {
        assertEquals(336.dp, immersivePortraitHeroHeight(400.dp, 800.dp))
        assertEquals(252.dp, immersivePortraitHeroHeight(360.dp, 600.dp))
    }

    @Test
    fun portraitHeroDoesNotOverflowVeryNarrowViewport() {
        assertEquals(216.dp, immersivePortraitHeroHeight(200.dp, 900.dp))
    }

    @Test
    fun wideArtworkRemainsWithinRequestedRange() {
        assertEquals(240.dp, immersiveWideArtworkSize(600.dp, 400.dp))
        assertEquals(300.dp, immersiveWideArtworkSize(1200.dp, 900.dp))
    }
}
