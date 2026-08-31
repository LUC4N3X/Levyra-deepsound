package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerVideoTransformTest {
    @Test
    fun `zoom is bounded between one and five times`() {
        assertEquals(1f, boundedPlayerVideoScale(1f, 0.2f), 0.001f)
        assertEquals(2f, boundedPlayerVideoScale(1f, 2f), 0.001f)
        assertEquals(PLAYER_VIDEO_MAX_SCALE, boundedPlayerVideoScale(4f, 3f), 0.001f)
    }

    @Test
    fun `invalid zoom input safely resets to one`() {
        assertEquals(1f, boundedPlayerVideoScale(Float.NaN, 2f), 0.001f)
        assertEquals(1f, boundedPlayerVideoScale(2f, Float.POSITIVE_INFINITY), 0.001f)
        assertEquals(1f, boundedPlayerVideoScale(2f, 0f), 0.001f)
    }

    @Test
    fun `pan is clamped to visible zoom bounds`() {
        assertEquals(500f, boundedPlayerVideoTranslation(900f, 1000f, 2f), 0.001f)
        assertEquals(-500f, boundedPlayerVideoTranslation(-900f, 1000f, 2f), 0.001f)
        assertEquals(240f, boundedPlayerVideoTranslation(240f, 1000f, 2f), 0.001f)
    }

    @Test
    fun `pan is reset when video is not zoomed`() {
        assertEquals(0f, boundedPlayerVideoTranslation(120f, 1000f, 1f), 0.001f)
        assertEquals(0f, boundedPlayerVideoTranslation(120f, 0f, 2f), 0.001f)
    }
}
