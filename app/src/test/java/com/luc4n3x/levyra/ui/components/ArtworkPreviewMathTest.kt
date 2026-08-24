package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkPreviewMathTest {

    @Test
    fun atBaseScaleOffsetIsZero() {
        val size = IntSize(1080, 1080)
        val (scale, offset) = clampZoomPan(1f, Offset(100f, 100f), size)
        assertEquals(1f, scale, 0.001f)
        assertEquals(Offset.Zero, offset)
    }

    @Test
    fun zoomIsClampedWithinBounds() {
        val size = IntSize(1080, 1080)
        val (minClamped, _) = clampZoomPan(0.5f, Offset.Zero, size)
        val (maxClamped, _) = clampZoomPan(10f, Offset.Zero, size)

        assertEquals(1f, minClamped, 0.001f)
        assertEquals(4.5f, maxClamped, 0.001f)
    }

    @Test
    fun panOffsetIsClampedToViewportBoundaries() {
        val size = IntSize(1000, 1000)
        // At 2x scale, max pan is (1000 * (2 - 1)) / 2 = 500f
        val (scale, offset) = clampZoomPan(2f, Offset(800f, -900f), size)

        assertEquals(2f, scale, 0.001f)
        assertEquals(500f, offset.x, 0.001f)
        assertEquals(-500f, offset.y, 0.001f)
    }
}
