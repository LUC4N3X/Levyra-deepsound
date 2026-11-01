package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LevyraColorModelsTest {
    @Test
    fun hslMatchesReferenceValuesForPrimaryColors() {
        val red = argbToHsv(0xFFFF0000.toInt()).toHsl()
        assertEquals(0f, red.hue, 0.01f)
        assertEquals(1f, red.saturation, 0.001f)
        assertEquals(0.5f, red.lightness, 0.001f)
        val teal = argbToHsv(0xFF008080.toInt()).toHsl()
        assertEquals(180f, teal.hue, 0.5f)
        assertEquals(1f, teal.saturation, 0.01f)
        assertEquals(0.251f, teal.lightness, 0.01f)
    }

    @Test
    fun hsvHslRoundTripIsLosslessAcrossTheGamut() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val argb = argbFromRgb(r, g, b)
            val hsv = argbToHsv(argb)
            val back = hsv.toHsl().toHsv()
            assertEquals("roundtrip $r $g $b", argb, back.toArgb())
            assertTrue(abs(back.hue - hsv.hue) < 0.01f || hsv.saturation == 0f)
        }
    }

    @Test
    fun degenerateLightnessKeepsHueAndRememberedSaturation() {
        val black = LevyraHslColor(210f, 0.8f, 0f)
        val hsl = black.toHsv().toHsl(fallbackSaturation = 0.8f)
        assertEquals(210f, hsl.hue, 0.01f)
        assertEquals(0.8f, hsl.saturation, 0.001f)
        assertEquals(0xFF000000.toInt(), black.toHsv().toArgb())
        assertEquals(0xFFFFFFFF.toInt(), LevyraHslColor(30f, 0.5f, 1f).toHsv().toArgb())
    }

    @Test
    fun hexInputAcceptsPrefixesAndShorthandAndRejectsGarbage() {
        assertEquals(0xFF12AB34.toInt(), parseLevyraHexColor("0x12ab34"))
        assertEquals("12AB34", sanitizeHexInput("0x12ab34zz"))
        assertEquals("FF00AA", expandLevyraHexShorthand("#f0a"))
        assertEquals(0xFFFF00AA.toInt(), resolveLevyraHexInput("f0a"))
        assertNull(resolveLevyraHexInput("12"))
        assertNull(resolveLevyraHexInput("GGGGGG"))
        assertNull(expandLevyraHexShorthand("1234"))
    }

    @Test
    fun contrastRatioMatchesWcagReference() {
        assertEquals(21f, levyraContrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01f)
        assertEquals(1f, levyraContrastRatio(0xFF777777.toInt(), 0xFF777777.toInt()), 0.001f)
        assertEquals(4.48f, levyraContrastRatio(0xFF777777.toInt(), 0xFFFFFFFF.toInt()), 0.02f)
    }

    @Test
    fun recentColorsAreDistinctNewestFirstAndBounded() {
        var recent = emptyList<Int>()
        (1..10).forEach { recent = rememberRecentColor(recent, argbFromRgb(it, 0, 0)) }
        recent = rememberRecentColor(recent, argbFromRgb(5, 0, 0))
        assertEquals(RecentColorLimit, recent.size)
        assertEquals(argbFromRgb(5, 0, 0), recent.first())
        assertEquals(recent.distinct(), recent)
        assertEquals(recent, LevyraRecentColors.decode(LevyraRecentColors.encode(recent)))
        assertEquals(emptyList<Int>(), LevyraRecentColors.decode("zz,"))
    }
}
