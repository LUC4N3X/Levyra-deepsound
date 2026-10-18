package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.PlayerColorMode
import com.luc4n3x.levyra.ui.i18n.ColorPickerText
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.colorPickerCompleteLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.colorPickerCopy
import com.luc4n3x.levyra.ui.i18n.colorPickerLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LevyraAdvancedColorPickerTest {

    @Test
    fun primaryAndSecondaryColorsConvertAccuratelyBetweenArgbAndHsv() {
        val cases = listOf(
            0xFFFF0000.toInt() to LevyraHsvColor(0f, 1f, 1f),
            0xFFFFFF00.toInt() to LevyraHsvColor(60f, 1f, 1f),
            0xFF00FF00.toInt() to LevyraHsvColor(120f, 1f, 1f),
            0xFF00FFFF.toInt() to LevyraHsvColor(180f, 1f, 1f),
            0xFF0000FF.toInt() to LevyraHsvColor(240f, 1f, 1f),
            0xFFFF00FF.toInt() to LevyraHsvColor(300f, 1f, 1f)
        )
        for ((expectedArgb, expectedHsv) in cases) {
            assertEquals(expectedArgb, expectedHsv.toArgb())
            val actualHsv = argbToHsv(expectedArgb)
            assertEquals(expectedHsv.hue, actualHsv.hue, 0.01f)
            assertEquals(expectedHsv.saturation, actualHsv.saturation, 0.001f)
            assertEquals(expectedHsv.value, actualHsv.value, 0.001f)
        }
    }

    @Test
    fun arbitraryColorsRoundTripThroughHsvWithinOneBitPerChannel() {
        val samples = listOf(
            PlayerColorMode.DEFAULT_ACCENT,
            0xFF0A84FF.toInt(),
            0xFF30D158.toInt(),
            0xFFFF375F.toInt(),
            0xFF5BD6C0.toInt(),
            0xFFFFC857.toInt(),
            0xFF131A2B.toInt(),
            0xFFF6F2EA.toInt(),
            0xFF808080.toInt(),
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt()
        )
        for (color in samples) {
            val roundTripped = argbToHsv(color).toArgb()
            val (r1, g1, b1) = rgbChannelsOf(color)
            val (r2, g2, b2) = rgbChannelsOf(roundTripped)
            assertTrue("Red drift for ${formatLevyraHexColor(color)}", abs(r1 - r2) <= 1)
            assertTrue("Green drift for ${formatLevyraHexColor(color)}", abs(g1 - g2) <= 1)
            assertTrue("Blue drift for ${formatLevyraHexColor(color)}", abs(b1 - b2) <= 1)
        }
    }

    @Test
    fun achromaticColorsPreserveExistingHue() {
        val black = argbToHsv(0xFF000000.toInt(), preserveHue = 228f)
        assertEquals(228f, black.hue, 0.001f)
        assertEquals(0f, black.saturation, 0.001f)
        assertEquals(0f, black.value, 0.001f)
        assertEquals(0xFF000000.toInt(), black.toArgb())

        val white = argbToHsv(0xFFFFFFFF.toInt(), preserveHue = 145f)
        assertEquals(145f, white.hue, 0.001f)
        assertEquals(0f, white.saturation, 0.001f)
        assertEquals(1f, white.value, 0.001f)
        assertEquals(0xFFFFFFFF.toInt(), white.toArgb())

        val grey = argbToHsv(0xFF808080.toInt(), preserveHue = 312f)
        assertEquals(312f, grey.hue, 0.001f)
        assertEquals(0f, grey.saturation, 0.001f)
        assertEquals(128f / 255f, grey.value, 0.005f)
    }

    @Test
    fun palettePointAndHueBarMappingClampBoundsAndPreserveOrthogonalChannels() {
        val initial = LevyraHsvColor(hue = 210f, saturation = 0.5f, value = 0.8f)

        val topLeft = initial.withPalettePoint(-0.5f, -0.2f)
        assertEquals(210f, topLeft.hue, 0.001f)
        assertEquals(0f, topLeft.saturation, 0.001f)
        assertEquals(1f, topLeft.value, 0.001f)
        assertEquals(0xFFFFFFFF.toInt(), topLeft.toArgb())

        val bottomRight = initial.withPalettePoint(1.8f, 1.4f)
        assertEquals(210f, bottomRight.hue, 0.001f)
        assertEquals(1f, bottomRight.saturation, 0.001f)
        assertEquals(0f, bottomRight.value, 0.001f)
        assertEquals(0xFF000000.toInt(), bottomRight.toArgb())

        val hueShifted = initial.withHueFraction(0.5f)
        assertEquals(180f, hueShifted.hue, 0.001f)
        assertEquals(0.5f, hueShifted.saturation, 0.001f)
        assertEquals(0.8f, hueShifted.value, 0.001f)
    }

    @Test
    fun touchAndIndicatorCoordinatesMatchExactlyAcrossPaletteAndHueBar() {
        val paletteIntSize = IntSize(300, 220)
        val paletteCanvasSize = Size(300f, 220f)
        val paletteInsetPx = 28f
        val baseHsv = LevyraHsvColor(hue = 195f, saturation = 0.5f, value = 0.5f)

        val interiorPoints = listOf(
            Offset(28f, 28f),
            Offset(92.5f, 64.25f),
            Offset(150f, 110f),
            Offset(241.75f, 178.5f),
            Offset(272f, 192f)
        )
        for (finger in interiorPoints) {
            val (normX, normY) = paletteNormalizedPoint(finger, paletteIntSize, paletteInsetPx)
            val updated = baseHsv.withPalettePoint(normX, normY)
            val indicatorCenter = paletteIndicatorCenter(updated, paletteCanvasSize, paletteInsetPx)
            assertEquals(finger.x, indicatorCenter.x, 0.01f)
            assertEquals(finger.y, indicatorCenter.y, 0.01f)
        }

        val (topLeftX, topLeftY) = paletteNormalizedPoint(Offset(0f, 0f), paletteIntSize, paletteInsetPx)
        assertEquals(0f, topLeftX, 0.0001f)
        assertEquals(0f, topLeftY, 0.0001f)
        val topLeftCenter = paletteIndicatorCenter(
            baseHsv.withPalettePoint(topLeftX, topLeftY),
            paletteCanvasSize,
            paletteInsetPx
        )
        assertEquals(paletteInsetPx, topLeftCenter.x, 0.01f)
        assertEquals(paletteInsetPx, topLeftCenter.y, 0.01f)

        val (bottomRightX, bottomRightY) = paletteNormalizedPoint(Offset(300f, 220f), paletteIntSize, paletteInsetPx)
        assertEquals(1f, bottomRightX, 0.0001f)
        assertEquals(1f, bottomRightY, 0.0001f)
        val bottomRightCenter = paletteIndicatorCenter(
            baseHsv.withPalettePoint(bottomRightX, bottomRightY),
            paletteCanvasSize,
            paletteInsetPx
        )
        assertEquals(300f - paletteInsetPx, bottomRightCenter.x, 0.01f)
        assertEquals(220f - paletteInsetPx, bottomRightCenter.y, 0.01f)

        val hueHeight = 220
        val hueInsetYPx = 24f
        val interiorHueYs = listOf(24f, 68.5f, 110f, 163.25f, 196f)
        for (fingerY in interiorHueYs) {
            val fraction = hueBarNormalizedFraction(fingerY, hueHeight, hueInsetYPx)
            val updated = baseHsv.withHueFraction(fraction)
            val centerY = hueBarIndicatorCenterY(updated, hueHeight.toFloat(), hueInsetYPx)
            assertEquals(fingerY, centerY, 0.01f)
        }
        assertEquals(0f, hueBarNormalizedFraction(0f, hueHeight, hueInsetYPx), 0.0001f)
        assertEquals(1f, hueBarNormalizedFraction(220f, hueHeight, hueInsetYPx), 0.0001f)
    }

    @Test
    fun hexValidationAndSanitizationHandleIncompleteAndInvalidInputSafely() {
        assertEquals("7C8CFF", sanitizeHexInput("#7c8cff"))
        assertEquals("7C8", sanitizeHexInput("#7c8-g!z"))
        assertEquals("ABCDEF", sanitizeHexInput("#abcdef12345"))

        assertEquals(0xFF7C8CFF.toInt(), parseLevyraHexColor("#7c8cff"))
        assertEquals(0xFF7C8CFF.toInt(), parseLevyraHexColor("7C8CFF"))
        assertNull(parseLevyraHexColor(""))
        assertNull(parseLevyraHexColor("7C8"))
        assertNull(parseLevyraHexColor("7C8CFZ"))
        assertNull(parseLevyraHexColor("1234567"))

        assertEquals("00FF7F", formatLevyraHexColor(0x0000FF7F))
    }

    @Test
    fun colorPickerCopyCoversEverySupportedLanguageWithoutBlankEntries() {
        val expected = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, colorPickerLocalizationCodes())
        assertEquals(expected, colorPickerCompleteLocalizationCodes())
        assertEquals(expected, com.luc4n3x.levyra.ui.i18n.playerColorLocalizationCodes())

        for (language in LevyraLanguageCatalog.languages) {
            val copy = LevyraStrings.forCode(language.code).colorPickerCopy
            for (key in ColorPickerText.entries) {
                val value = copy[key]
                assertNotNull("Missing $key for ${language.code}", value)
                assertTrue("Blank $key for ${language.code}", value.isNotBlank())
            }
        }

        assertEquals("调色板", com.luc4n3x.levyra.ui.i18n.colorPickerCopyForCode("zh-Hans")[ColorPickerText.OpenPicker])
        assertEquals("לוח צבעים", com.luc4n3x.levyra.ui.i18n.colorPickerCopyForCode("iw")[ColorPickerText.OpenPicker])
        assertEquals("Palet warna", com.luc4n3x.levyra.ui.i18n.colorPickerCopyForCode("in")[ColorPickerText.OpenPicker])
        assertEquals("Palette ng kulay", com.luc4n3x.levyra.ui.i18n.colorPickerCopyForCode("tl")[ColorPickerText.OpenPicker])
        assertEquals("Fargepalett", com.luc4n3x.levyra.ui.i18n.colorPickerCopyForCode("no")[ColorPickerText.OpenPicker])
    }
}
