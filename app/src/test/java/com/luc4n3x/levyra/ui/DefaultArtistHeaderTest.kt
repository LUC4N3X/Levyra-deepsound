package com.luc4n3x.levyra.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultArtistHeaderTest {
    @Test
    fun heroHeightStaysWithinBoundsOnWideShortWindows() {
        assertEquals(288f, defaultArtistHeroHeight(800.dp, 360.dp, 1f).value, 0.01f)
    }

    @Test
    fun heroHeightFollowsTallCanvasOnPhones() {
        assertEquals(549.33f, defaultArtistHeroHeight(412.dp, 915.dp, 0.75f).value, 0.01f)
    }

    @Test
    fun countLabelMarksTruncatedLists() {
        assertEquals("38+ songs", artistCountLabel("38 songs", 38, more = true))
        assertEquals("38 songs", artistCountLabel("38 songs", 38, more = false))
    }
}
