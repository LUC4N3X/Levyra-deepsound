package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartArtworkCropPlannerTest {
    private fun image(width: Int, height: Int, fill: Int, paint: (Int, Int) -> Int?): IntArray =
        IntArray(width * height) { index -> paint(index % width, index / width) ?: fill }

    private fun checker(x: Int, y: Int): Int = if ((x + y) % 2 == 0) 0xFFE04A2F.toInt() else 0xFF1C6DD0.toInt()

    @Test
    fun squareImagesKeepTheFullFrame() {
        val pixels = image(80, 80, 0xFF202020.toInt()) { x, y -> checker(x, y) }
        val crop = SmartArtworkCropPlanner.plan(pixels, 80, 80, allowZoom = true)
        assertEquals(0f, crop.left, 0.001f)
        assertEquals(0f, crop.top, 0.001f)
        assertEquals(1f, crop.width, 0.001f)
    }

    @Test
    fun followsTheSubjectInsteadOfTheCenter() {
        val pixels = image(96, 54, 0xFF101418.toInt()) { x, y -> if (x in 4..22 && y in 14..40) checker(x, y) else null }
        val crop = SmartArtworkCropPlanner.plan(pixels, 96, 54, allowZoom = false)
        assertEquals(54f / 96f, crop.width, 0.001f)
        assertTrue("crop must stay on the left subject: ${crop.left * 96f}", crop.left * 96f <= 6f)
    }

    @Test
    fun zoomDropsLetterboxBars() {
        val pixels = image(96, 72, 0xFF000000.toInt()) { x, y -> if (y in 9 until 63) checker(x, y) else null }
        val zoomed = SmartArtworkCropPlanner.plan(pixels, 96, 72, allowZoom = true)
        val top = zoomed.top * 72f
        val bottom = top + zoomed.height * 72f
        assertTrue("zoomed crop must sit inside the picture: $top..$bottom", top >= 8.5f && bottom <= 63.5f)
        val fixed = SmartArtworkCropPlanner.plan(pixels, 96, 72, allowZoom = false)
        assertEquals(72f / 96f, fixed.width, 0.001f)
    }

    @Test
    fun facesOutweighBusyBackgrounds() {
        val pixels = image(120, 60, 0xFF303030.toInt()) { x, y -> if (x >= 84) checker(x, y) else null }
        val face = ArtworkCropFace(centerX = 14f / 120f, centerY = 0.5f, radius = 12f / 120f)
        val crop = SmartArtworkCropPlanner.plan(pixels, 120, 60, allowZoom = false, faces = listOf(face))
        assertTrue("crop must keep the face: ${crop.left * 120f}", crop.left * 120f <= 2f)
    }

    @Test
    fun uniformImagesFallBackToTheCenter() {
        val pixels = image(100, 50, 0xFF777777.toInt()) { _, _ -> null }
        val crop = SmartArtworkCropPlanner.plan(pixels, 100, 50, allowZoom = false)
        assertEquals(0.25f, crop.left, 0.02f)
    }
}
