package com.luc4n3x.levyra.data

import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.math.min
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

    private fun hueDistance(first: Double, second: Double): Double {
        val distance = abs(first - second)
        return min(distance, 360.0 - distance)
    }
}
