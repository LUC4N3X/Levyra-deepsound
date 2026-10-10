package com.luc4n3x.levyra.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VideoFrameShapeTest {

    private val width = 160
    private val height = 90

    @Test
    fun artTrackFrameIsSquareContentBetweenSideBands() {
        val random = Random(7)
        val pixels = frame { x, _ ->
            if (x < 35 || x >= 125) jitter(0x1E4D55, random, 6) else randomColor(random)
        }
        val fit = measureVideoFrameFit(pixels, width, height)
        assertTrue(fit.squareContent)
        assertTrue(fit.zoom > 1.7f)
    }

    @Test
    fun windowboxedFourByThreeVideoZoomsToFillTheCard() {
        val random = Random(5)
        val pixels = frame { x, y ->
            val pillar = x < 28 || x >= 132
            val letterbox = y < 6 || y >= 84
            if (pillar || letterbox) jitter(0x000000, random, 4) else randomColor(random)
        }
        val fit = measureVideoFrameFit(pixels, width, height)
        assertFalse(fit.squareContent)
        assertTrue("zoom ${fit.zoom}", fit.zoom in 1.5f..1.65f)
    }

    @Test
    fun fullBleedVideoFrameKeepsItsScale() {
        val random = Random(11)
        val pixels = frame { x, y ->
            jitter(rgb(x * 2 % 256, y * 3 % 256, (x + y) % 256), random, 30)
        }
        val fit = measureVideoFrameFit(pixels, width, height)
        assertEquals(1f, fit.zoom)
        assertFalse(fit.squareContent)
    }

    @Test
    fun solidColorFrameKeepsItsScale() {
        val fit = measureVideoFrameFit(frame { _, _ -> rgb(12, 12, 16) }, width, height)
        assertEquals(1f, fit.zoom)
    }

    @Test
    fun framesWithDifferentSideColorsKeepTheirScale() {
        val random = Random(3)
        val pixels = frame { x, _ ->
            when {
                x < 35 -> rgb(200, 20, 20)
                x >= 125 -> rgb(20, 20, 200)
                else -> randomColor(random)
            }
        }
        assertEquals(1f, measureVideoFrameFit(pixels, width, height).zoom)
    }

    @Test
    fun undersizedInputIsIgnored() {
        assertEquals(1f, measureVideoFrameFit(IntArray(4), 2, 2).zoom)
        assertEquals(1f, measureVideoFrameFit(IntArray(10), width, height).zoom)
    }

    private fun frame(color: (Int, Int) -> Int): IntArray =
        IntArray(width * height) { index -> color(index % width, index / width) }

    private fun jitter(base: Int, random: Random, amount: Int): Int {
        fun channel(value: Int) = (value + random.nextInt(-amount, amount + 1)).coerceIn(0, 255)
        return rgb(channel(base shr 16 and 0xFF), channel(base shr 8 and 0xFF), channel(base and 0xFF))
    }

    private fun randomColor(random: Random): Int =
        rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        0xFF shl 24 or (red shl 16) or (green shl 8) or blue
}
