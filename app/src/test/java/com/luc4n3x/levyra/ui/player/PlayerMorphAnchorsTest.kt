package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMorphAnchorsTest {

    @Test
    fun artworkAndFlightCrossfadeAtTheSameLandingWindow() {
        var expansion = 0f
        val anchors = PlayerMorphAnchors { expansion }
        anchors.update(PlayerMorphSlot.Mini, Rect(8f, 800f, 72f, 864f))
        anchors.update(PlayerMorphSlot.Full, Rect(40f, 160f, 360f, 480f))

        expansion = 0.80f
        assertEquals(1f, anchors.flightAlpha(), 0.0001f)
        assertEquals(0f, anchors.fullArtworkAlpha(morphActive = true, immersive = false), 0.0001f)

        expansion = 0.90f
        assertEquals(1f, anchors.flightAlpha(), 0.0001f)
        assertTrue(anchors.fullArtworkAlpha(morphActive = true, immersive = false) > 0f)

        expansion = 0.92f
        assertEquals(1f, anchors.flightAlpha(), 0.0001f)
        assertEquals(1f, anchors.fullArtworkAlpha(morphActive = true, immersive = false), 0.0001f)

        expansion = 0.95f
        assertEquals(1f, anchors.fullArtworkAlpha(morphActive = true, immersive = false), 0.0001f)
        assertTrue(anchors.flightAlpha() in 0f..1f)

        expansion = 0.98f
        assertEquals(0f, anchors.flightAlpha(), 0.0001f)
        assertEquals(1f, anchors.fullArtworkAlpha(morphActive = true, immersive = false), 0.0001f)
    }

    @Test
    fun missingAnchorsAndStaticRenderingNeverHideTheArtwork() {
        val anchors = PlayerMorphAnchors { 0.92f }
        assertEquals(1f, anchors.fullArtworkAlpha(morphActive = true, immersive = false), 0f)
        anchors.update(PlayerMorphSlot.Mini, Rect(0f, 0f, 64f, 64f))
        assertEquals(1f, anchors.fullArtworkAlpha(morphActive = true, immersive = false), 0f)
        assertEquals(1f, anchors.fullArtworkAlpha(morphActive = false, immersive = false), 0f)
        assertEquals(0f, anchors.fullArtworkAlpha(morphActive = false, immersive = true), 0f)
    }
}
