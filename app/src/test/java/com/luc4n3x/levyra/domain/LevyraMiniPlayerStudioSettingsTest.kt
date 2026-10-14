package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.ui.formatMiniHexColor
import com.luc4n3x.levyra.ui.i18n.miniPlayerStudioCompleteLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.miniPlayerStudioLocalizationCodes
import com.luc4n3x.levyra.ui.parseMiniHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraMiniPlayerStudioSettingsTest {

    @Test
    fun defaultSettingsKeepOriginalStyle() {
        val settings = LevyraMiniPlayerStudioSettings()
        assertEquals(MiniPlayerStyle.Original, settings.style)
        assertTrue(settings.tunings.isEmpty())
        assertTrue(LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Original).options.isEmpty())
    }

    @Test
    fun missingOrCorruptStorageFallsBackToOriginal() {
        assertEquals(LevyraMiniPlayerStudioSettings(), LevyraMiniPlayerStudioSettings.decode(null))
        assertEquals(LevyraMiniPlayerStudioSettings(), LevyraMiniPlayerStudioSettings.decode(""))
        assertEquals(LevyraMiniPlayerStudioSettings(), LevyraMiniPlayerStudioSettings.decode("{not json"))
    }

    @Test
    fun unknownStyleAndValuesFallBackSafely() {
        val decoded = LevyraMiniPlayerStudioSettings.decode(
            """{"version":9,"style":"holographic","tunings":{"floating":{"height":999,"progress":"spiral","buttons":"neon"},"mystery":{}}}"""
        )
        assertEquals(MiniPlayerStyle.Original, decoded.style)
        val floating = decoded.tuningFor(MiniPlayerStyle.Floating)
        val spec = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating)
        assertEquals(spec.height.last, floating.heightDp)
        assertEquals(spec.defaults.progress, floating.progress)
        assertEquals(spec.defaults.buttons, floating.buttons)
    }

    @Test
    fun everyStyleDefaultIsAlreadyNormalized() {
        MiniPlayerStyle.entries.forEach { style ->
            val defaults = LevyraMiniPlayerStudioSettings.spec(style).defaults
            assertEquals(style.name, defaults, defaults.normalized(style))
        }
    }

    @Test
    fun customizationRoundTripsThroughStorage() {
        val settings = LevyraMiniPlayerStudioSettings()
            .withStyle(MiniPlayerStyle.LiquidGlass)
            .updateTuning(MiniPlayerStyle.LiquidGlass) {
                it.copy(opacity = 70, cornerDp = 20, colorMode = MiniColorMode.Custom, customColor = 0xFF112233.toInt())
            }
        val decoded = LevyraMiniPlayerStudioSettings.decode(settings.encode())
        assertEquals(settings, decoded)
        assertEquals(70, decoded.activeTuning().opacity)
    }

    @Test
    fun originalIsNeverCustomized() {
        val settings = LevyraMiniPlayerStudioSettings().updateTuning(MiniPlayerStyle.Original) { it.copy(heightDp = 80) }
        assertTrue(settings.tunings.isEmpty())
    }

    @Test
    fun tuningMatchingDefaultsIsDropped() {
        val style = MiniPlayerStyle.Floating
        val settings = LevyraMiniPlayerStudioSettings()
            .updateTuning(style) { it.copy(insetDp = 30) }
            .updateTuning(style) { it.copy(insetDp = LevyraMiniPlayerStudioSettings.spec(style).defaults.insetDp) }
        assertTrue(settings.tunings.isEmpty())
    }

    @Test
    fun valuesAreClampedToSafeLimits() {
        val style = MiniPlayerStyle.UltraCompact
        val tuning = LevyraMiniPlayerStudioSettings.spec(style).defaults
            .copy(heightDp = 10, artworkDp = 200, cornerDp = 200, controlScale = 999)
            .normalized(style)
        assertEquals(48, tuning.heightDp)
        assertTrue(tuning.artworkDp <= tuning.heightDp - 12)
        assertTrue(tuning.cornerDp <= tuning.heightDp / 2)
        assertEquals(MiniPlayerTuning.CONTROL_SCALE_RANGE.last, tuning.controlScale)
    }

    @Test
    fun restoringDefaultClearsEverything() {
        val customized = LevyraMiniPlayerStudioSettings()
            .withStyle(MiniPlayerStyle.ArtworkFocus)
            .updateTuning(MiniPlayerStyle.ArtworkFocus) { it.copy(heightDp = 84) }
        assertEquals(LevyraMiniPlayerStudioSettings(), LevyraMiniPlayerStudioSettings.decode(LevyraMiniPlayerStudioSettings().encode()))
        assertTrue(customized != LevyraMiniPlayerStudioSettings())
    }

    @Test
    fun hexColorsParseStrictly() {
        assertEquals(0xFF7C8CFF.toInt(), parseMiniHexColor("#7c8cff"))
        assertEquals(0xFF000000.toInt(), parseMiniHexColor("000000"))
        assertNull(parseMiniHexColor("12345"))
        assertNull(parseMiniHexColor("GGGGGG"))
        assertEquals("7C8CFF", formatMiniHexColor(0xFF7C8CFF.toInt()))
    }

    @Test
    fun miniPlayerStudioCopyCoversEveryLanguage() {
        val expected = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, miniPlayerStudioLocalizationCodes())
        assertEquals(expected, miniPlayerStudioCompleteLocalizationCodes())
    }
}
