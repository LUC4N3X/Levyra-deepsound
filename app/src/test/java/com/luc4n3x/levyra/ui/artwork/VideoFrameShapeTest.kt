package com.luc4n3x.levyra.ui.artwork

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VideoFrameShapeTest {

    private val width = 120
    private val height = 90

    @Test
    fun artTrackFrameWithSolidSideBarsIsPillarboxed() {
        val random = Random(7)
        val pixels = frame { x, _ ->
            if (x < 26 || x >= 94) jitter(0x1E4D55, random, 6) else randomColor(random)
        }
        assertTrue(isPillarboxedVideoFrame(pixels, width, height))
    }

    @Test
    fun fullBleedVideoFrameIsNotPillarboxed() {
        val random = Random(11)
        val pixels = frame { x, y ->
            jitter(rgb(x * 2 % 256, y * 3 % 256, (x + y) % 256), random, 30)
        }
        assertFalse(isPillarboxedVideoFrame(pixels, width, height))
    }

    @Test
    fun solidColorFrameIsNotPillarboxed() {
        val pixels = frame { _, _ -> rgb(12, 12, 16) }
        assertFalse(isPillarboxedVideoFrame(pixels, width, height))
    }

    @Test
    fun framesWithDifferentSideColorsAreNotPillarboxed() {
        val random = Random(3)
        val pixels = frame { x, _ ->
            when {
                x < 26 -> rgb(200, 20, 20)
                x >= 94 -> rgb(20, 20, 200)
                else -> randomColor(random)
            }
        }
        assertFalse(isPillarboxedVideoFrame(pixels, width, height))
    }

    @Test
    fun undersizedInputIsIgnored() {
        assertFalse(isPillarboxedVideoFrame(IntArray(4), 2, 2))
        assertFalse(isPillarboxedVideoFrame(IntArray(10), width, height))
    }

    private fun frame(color: (Int, Int) -> Int): IntArray =
        IntArray(width * height) { index -> color(index % width, index / width) }

    private fun jitter(base: Int, random: Random, amount: Int): Int {
        fun channel(value: Int) = (value + random.nextInt(-amount, amount + 1)).coerceIn(0, 255)
        return rgb(channel((base shr 16) and 0xFF), channel((base shr 8) and 0xFF), channel(base and 0xFF))
    }

    private fun randomColor(random: Random): Int =
        rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
