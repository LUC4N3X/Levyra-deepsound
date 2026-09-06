package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoZoomTest {

    private val surface = Size(1080f, 1920f)

    @Test
    fun scaleNeverGoesBelowOne() {
        assertEquals(PLAYER_VIDEO_MIN_SCALE, boundedPlayerVideoScale(1f, 0.2f), 0.0001f)
        assertEquals(PLAYER_VIDEO_MIN_SCALE, boundedPlayerVideoScale(1.4f, 0.1f), 0.0001f)
    }

    @Test
    fun scaleStopsAtFiveTimes() {
        assertEquals(5f, PLAYER_VIDEO_MAX_SCALE, 0.0001f)
        assertEquals(PLAYER_VIDEO_MAX_SCALE, boundedPlayerVideoScale(4.5f, 4f), 0.0001f)
        assertEquals(PLAYER_VIDEO_MAX_SCALE, boundedPlayerVideoScale(PLAYER_VIDEO_MAX_SCALE, 1.5f), 0.0001f)
    }

    @Test
    fun nonFiniteInputFallsBackToTheMinimumScale() {
        assertEquals(PLAYER_VIDEO_MIN_SCALE, boundedPlayerVideoScale(Float.NaN, 2f), 0.0001f)
        assertEquals(PLAYER_VIDEO_MIN_SCALE, boundedPlayerVideoScale(2f, Float.POSITIVE_INFINITY), 0.0001f)
        assertEquals(PLAYER_VIDEO_MIN_SCALE, boundedPlayerVideoScale(2f, -1f), 0.0001f)
    }

    @Test
    fun panIsDisabledAtOneTimes() {
        assertEquals(0f, boundedPlayerVideoOffset(500f, surface.width, 1f), 0.0001f)
    }

    @Test
    fun panStaysInsideTheScaledBounds() {
        val scale = 2f
        val maxOffset = surface.width * (scale - 1f) / 2f

        assertEquals(maxOffset, boundedPlayerVideoOffset(10_000f, surface.width, scale), 0.0001f)
        assertEquals(-maxOffset, boundedPlayerVideoOffset(-10_000f, surface.width, scale), 0.0001f)
        assertEquals(120f, boundedPlayerVideoOffset(120f, surface.width, scale), 0.0001f)
    }

    @Test
    fun repeatedPanningNeverEscapesThePlayerBounds() {
        var transform = PlayerVideoTransform().applyPlayerVideoGesture(3f, Offset.Zero, surface)
        repeat(40) {
            transform = transform.applyPlayerVideoGesture(1f, Offset(400f, 400f), surface)
        }

        val maxX = surface.width * (transform.scale - 1f) / 2f
        val maxY = surface.height * (transform.scale - 1f) / 2f
        assertTrue(transform.offsetX <= maxX + 0.001f)
        assertTrue(transform.offsetY <= maxY + 0.001f)
    }

    @Test
    fun zoomingBackOutResetsScaleAndPan() {
        val zoomed = PlayerVideoTransform()
            .applyPlayerVideoGesture(3f, Offset(300f, 300f), surface)
        assertTrue(zoomed.isZoomed)

        val restored = zoomed.applyPlayerVideoGesture(0.05f, Offset.Zero, surface)

        assertEquals(PlayerVideoTransform.None, restored)
        assertFalse(restored.isZoomed)
    }

    @Test
    fun offsetsShrinkWhenZoomingOut() {
        val zoomed = PlayerVideoTransform()
            .applyPlayerVideoGesture(4f, Offset(10_000f, 10_000f), surface)
        val zoomedOut = zoomed.applyPlayerVideoGesture(0.5f, Offset.Zero, surface)

        val maxX = surface.width * (zoomedOut.scale - 1f) / 2f
        assertTrue(zoomedOut.scale < zoomed.scale)
        assertTrue(zoomedOut.offsetX <= maxX + 0.001f)
    }

    @Test
    fun defaultTransformIsNeutral() {
        val none = PlayerVideoTransform.None

        assertEquals(PLAYER_VIDEO_MIN_SCALE, none.scale, 0.0001f)
        assertEquals(0f, none.offsetX, 0.0001f)
        assertEquals(0f, none.offsetY, 0.0001f)
        assertFalse(none.isZoomed)
    }

    @Test
    fun zeroSizedSurfaceNeverProducesOffsets() {
        val transform = PlayerVideoTransform().applyPlayerVideoGesture(3f, Offset(500f, 500f), Size.Zero)

        assertEquals(0f, transform.offsetX, 0.0001f)
        assertEquals(0f, transform.offsetY, 0.0001f)
    }
}
