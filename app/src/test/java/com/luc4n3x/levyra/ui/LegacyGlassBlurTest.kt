package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyGlassBlurTest {
    @Test
    fun uniformBackdropStaysUniform() {
        val color = 0xFF3A6EA5.toInt()
        val pixels = IntArray(40 * 30) { color }
        LegacyGlassBlur.blur(pixels, IntArray(pixels.size), 40, 30, radius = 6, saturation = 1f)
        assertTrue(pixels.all { it == color })
    }

    @Test
    fun sharpEdgeBecomesASmoothRamp() {
        val width = 60
        val pixels = IntArray(width * 10) { if (it % width < width / 2) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        LegacyGlassBlur.blur(pixels, IntArray(pixels.size), width, 10, radius = 4, saturation = 1f)
        val row = (0 until width).map { pixels[5 * width + it] and 0xFF }
        assertEquals(0, row.first())
        assertEquals(255, row.last())
        assertTrue(row.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(row[width / 2] in 60..200)
    }

    @Test
    fun saturationBoostKeepsGrayNeutral() {
        val gray = 0xFF808080.toInt()
        val pixels = IntArray(16) { gray }
        LegacyGlassBlur.blur(pixels, IntArray(16), 4, 4, radius = 1, saturation = 1.4f)
        assertTrue(pixels.all { it shr 16 and 0xFF == it and 0xFF })
    }
}
