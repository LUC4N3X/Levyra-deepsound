package com.luc4n3x.levyra.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPreviewTransformTest {

    @Test
    fun `fitted bounds for portrait viewport and square artwork clamps to square`() {
        val bounds = artworkPreviewFittedBounds(
            viewportWidth = 1080f,
            viewportHeight = 2400f,
            artworkWidth = 1080f,
            artworkHeight = 1080f
        )
        assertEquals(1080f, bounds.width)
        assertEquals(1080f, bounds.height)
    }

    @Test
    fun `at scale 1f both max offsets are zero`() {
        val bounds = artworkPreviewFittedBounds(1080f, 2400f, 1080f, 1080f)
        val maxX = artworkPreviewMaxOffset(bounds.width, 1080f, scale = 1f)
        val maxY = artworkPreviewMaxOffset(bounds.height, 2400f, scale = 1f)
        assertEquals(0f, maxX)
        assertEquals(0f, maxY)
    }

    @Test
    fun `at scale 2f maxX uses rendered bounds while viewport is taller than artwork`() {
        val bounds = artworkPreviewFittedBounds(1080f, 2400f, 1080f, 1080f)
        val maxX = artworkPreviewMaxOffset(bounds.width, 1080f, scale = 2f)
        val maxY = artworkPreviewMaxOffset(bounds.height, 2400f, scale = 2f)
        assertEquals(540f, maxX)
        assertEquals(0f, maxY)
    }

    @Test
    fun `at scale 4f maxX and maxY derive from rendered artwork bounds not viewport`() {
        val bounds = artworkPreviewFittedBounds(1080f, 2400f, 1080f, 1080f)
        val maxX = artworkPreviewMaxOffset(bounds.width, 1080f, scale = 4f)
        val maxY = artworkPreviewMaxOffset(bounds.height, 2400f, scale = 4f)
        assertEquals(1620f, maxX)
        assertEquals((4320f - 2400f) / 2f, maxY)
        assertEquals(960f, maxY)
    }

    @Test
    fun `clampScale clamps to 1f and 4_5f`() {
        assertEquals(1f, artworkPreviewClampScale(0.2f))
        assertEquals(1f, artworkPreviewClampScale(1f))
        assertEquals(4.5f, artworkPreviewClampScale(10f))
        assertEquals(2f, artworkPreviewClampScale(2f))
    }

    @Test
    fun `clampOffset clamps both axes symmetrically`() {
        val bounds = artworkPreviewFittedBounds(1080f, 2400f, 1080f, 1080f)
        val clamped = artworkPreviewClampOffset(
            offset = ArtworkPreviewOffset(x = 10_000f, y = -10_000f),
            bounds = bounds,
            viewportWidth = 1080f,
            viewportHeight = 2400f,
            scale = 4f
        )
        assertEquals(1620f, clamped.x)
        assertEquals(-960f, clamped.y)
    }

    @Test
    fun `degenerate zero-size input returns finite non-NaN values without crashing`() {
        val zeroBounds = artworkPreviewFittedBounds(0f, 0f, 0f, 0f)
        assertEquals(0f, zeroBounds.width)
        assertEquals(0f, zeroBounds.height)

        val zeroArtwork = artworkPreviewFittedBounds(1080f, 2400f, 0f, 0f)
        assertTrue(zeroArtwork.width.isFinite())
        assertTrue(zeroArtwork.height.isFinite())

        val maxOffset = artworkPreviewMaxOffset(renderedSize = 0f, viewportSize = 0f, scale = 1f)
        assertFalse(maxOffset.isNaN())
        assertTrue(maxOffset.isFinite())

        val clamped = artworkPreviewClampOffset(
            offset = ArtworkPreviewOffset(0f, 0f),
            bounds = zeroBounds,
            viewportWidth = 0f,
            viewportHeight = 0f,
            scale = 1f
        )
        assertFalse(clamped.x.isNaN())
        assertFalse(clamped.y.isNaN())
    }
}
