package com.luc4n3x.levyra.data

import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkToneMapperTest {
    @Test
    fun `tone mapping preserves muted artwork chroma`() {
        val source = Hct.from(350.0, 7.0, 58.0).toInt()

        val mapped = Hct.fromInt(
            ArtworkToneMapper.withToneRange(
                argb = source,
                toneFloor = 48.0,
                toneCeiling = 72.0
            )
        )

        assertTrue(mapped.chroma < 12.0)
        assertTrue(hueDistance(mapped.hue, Hct.fromInt(source).hue) < 3.0)
    }

    @Test
    fun `tone mapping clamps bright artwork without hue drift`() {
        val source = Hct.from(215.0, 52.0, 92.0).toInt()

        val mapped = Hct.fromInt(
            ArtworkToneMapper.withToneRange(
                argb = source,
                toneFloor = 48.0,
                toneCeiling = 72.0
            )
        )

        assertTrue(mapped.tone in 70.0..73.0)
        assertTrue(hueDistance(mapped.hue, Hct.fromInt(source).hue) < 3.0)
    }

    @Test
    fun `companion keeps palette controlled for muted artwork`() {
        val source = Hct.from(24.0, 10.0, 60.0).toInt()
        val sourceHct = Hct.fromInt(source)
        val companion = Hct.fromInt(ArtworkToneMapper.companion(source))

        assertTrue(companion.chroma <= sourceHct.chroma + 2.0)
        assertTrue(companion.tone in 39.0..61.0)
        assertTrue(hueDistance(companion.hue, sourceHct.hue) >= 30.0)
    }

    @Test
    fun `tone mapping caps aggressive neon chroma`() {
        val neon = Hct.from(140.0, 110.0, 80.0).toInt()

        val mapped = Hct.fromInt(ArtworkToneMapper.withToneRange(neon, 48.0, 72.0))

        assertTrue(mapped.chroma <= ArtworkToneMapper.ChromaCeiling + 1.0)
    }

    @Test
    fun `artwork with a few colored pixels is treated as monochrome`() {
        assertTrue(ArtworkToneMapper.isMonochrome(chromaticWeight = 40f, totalWeight = 9216f))
        assertFalse(ArtworkToneMapper.isMonochrome(chromaticWeight = 2000f, totalWeight = 9216f))
        assertFalse(ArtworkToneMapper.isMonochrome(chromaticWeight = 0f, totalWeight = 0f))
    }

    @Test
    fun `neutral palette stays quiet and readable for black and white covers`() {
        listOf(0xFF050505.toInt(), 0xFFF4F4F4.toInt(), 0xFF6E6A66.toInt()).forEach { average ->
            val (start, end) = ArtworkToneMapper.neutral(average)
            val first = Hct.fromInt(start)
            val second = Hct.fromInt(end)

            assertTrue(first.chroma <= ArtworkToneMapper.NeutralChromaCeiling + 1.0)
            assertTrue(second.chroma <= ArtworkToneMapper.NeutralChromaCeiling + 1.0)
            assertTrue(first.tone in 45.0..65.0)
            assertTrue(second.tone in 33.0..first.tone)
        }
    }

    @Test
    fun `neutral palette is deterministic`() {
        assertEquals(ArtworkToneMapper.neutral(0xFF808080.toInt()), ArtworkToneMapper.neutral(0xFF808080.toInt()))
    }

    private fun hueDistance(first: Double, second: Double): Double {
        val distance = abs(first - second)
        return min(distance, 360.0 - distance)
    }
}
