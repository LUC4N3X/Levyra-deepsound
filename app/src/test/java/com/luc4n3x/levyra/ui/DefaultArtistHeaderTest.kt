package com.luc4n3x.levyra.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultArtistHeaderTest {
    @Test
    fun heroHeightStaysWithinBoundsOnWideShortWindows() {
        assertEquals(223.2f, defaultArtistHeroHeight(800.dp, 360.dp, 1f).value, 0.01f)
    }

    @Test
    fun tallCanvasNeverStretchesHeroBeyondStaticHeight() {
        assertEquals(444.96f, defaultArtistHeroHeight(412.dp, 915.dp, 0.5625f).value, 0.01f)
        assertEquals(defaultArtistHeroHeight(412.dp, 915.dp, null).value, defaultArtistHeroHeight(412.dp, 915.dp, 0.75f).value, 0.01f)
    }

    @Test
    fun wideCanvasShortensHeroToItsAspect() {
        assertEquals(231.75f, defaultArtistHeroHeight(412.dp, 915.dp, 16f / 9f).value, 0.01f)
    }

    @Test
    fun countLabelMarksTruncatedLists() {
        assertEquals("38+ songs", artistCountLabel("38 songs", 38, more = true))
        assertEquals("38 songs", artistCountLabel("38 songs", 38, more = false))
    }
}
