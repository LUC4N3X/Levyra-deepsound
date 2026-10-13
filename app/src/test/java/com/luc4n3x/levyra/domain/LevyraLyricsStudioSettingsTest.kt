package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraLyricsStudioSettingsTest {

    @Test
    fun defaultSettingsMatchesLevyraOriginal() {
        val settings = LevyraLyricsStudioSettings()
        assertEquals(LyricsStylePreset.LEVYRA_ORIGINAL, settings.stylePreset)
        assertEquals(LyricsBackgroundMode.ORIGINAL, settings.backgroundMode)
        assertEquals(LyricsAlignment.START, settings.alignment)
        assertEquals(LyricsTextSize.STANDARD, settings.textSize)
        assertEquals(LyricsLineSpacing.STANDARD, settings.lineSpacing)
        assertEquals(LyricsFontWeightPreset.BOLD, settings.fontWeight)
        assertEquals(LyricsActiveColorMode.WHITE, settings.activeColorMode)
        assertEquals(LyricsGlowEffect.OFF, settings.glowEffect)
        assertTrue(settings.progressiveKaraokeEnabled)
        assertTrue(settings.blurEnabled)
    }

    @Test
    fun applyPresetConfiguresDistinctStyles() {
        val defaultSettings = LevyraLyricsStudioSettings()

        val original = defaultSettings.applyPreset(LyricsStylePreset.LEVYRA_ORIGINAL)
        assertEquals(LyricsStylePreset.LEVYRA_ORIGINAL, original.stylePreset)
        assertEquals(LyricsBackgroundMode.ORIGINAL, original.backgroundMode)
        assertEquals(LyricsAlignment.START, original.alignment)

        val expressive = defaultSettings.applyPreset(LyricsStylePreset.EXPRESSIVE_KARAOKE)
        assertEquals(LyricsStylePreset.EXPRESSIVE_KARAOKE, expressive.stylePreset)
        assertEquals(LyricsTextSize.LARGE, expressive.textSize)
        assertEquals(LyricsFontWeightPreset.EXTRA_BOLD, expressive.fontWeight)
        assertEquals(LyricsGlowEffect.SUBTLE, expressive.glowEffect)
        assertTrue(expressive.progressiveKaraokeEnabled)

        val minimal = defaultSettings.applyPreset(LyricsStylePreset.MINIMAL)
        assertEquals(LyricsStylePreset.MINIMAL, minimal.stylePreset)
        assertEquals(LyricsBackgroundMode.AMOLED, minimal.backgroundMode)
        assertEquals(LyricsAlignment.CENTER, minimal.alignment)
        assertEquals(LyricsTextSize.STANDARD, minimal.textSize)
        assertEquals(LyricsFontWeightPreset.SEMI_BOLD, minimal.fontWeight)
        assertEquals(LyricsGlowEffect.OFF, minimal.glowEffect)
        assertFalse(minimal.progressiveKaraokeEnabled)
        assertFalse(minimal.blurEnabled)

        val focus = defaultSettings.applyPreset(LyricsStylePreset.FOCUS)
        assertEquals(LyricsStylePreset.FOCUS, focus.stylePreset)
        assertEquals(LyricsTextSize.LARGE, focus.textSize)
        assertEquals(LyricsFontWeightPreset.BLACK, focus.fontWeight)
        assertEquals(LyricsGlowEffect.SUBTLE, focus.glowEffect)
        assertEquals(0.22f, focus.inactiveOpacity, 0.01f)
        assertEquals(0.82f, focus.inactiveScale, 0.01f)

        val cinematic = defaultSettings.applyPreset(LyricsStylePreset.CINEMATIC)
        assertEquals(LyricsStylePreset.CINEMATIC, cinematic.stylePreset)
        assertEquals(LyricsBackgroundMode.CINEMATIC, cinematic.backgroundMode)
        assertEquals(LyricsTextSize.LARGE, cinematic.textSize)
        assertEquals(LyricsGlowEffect.VIBRANT, cinematic.glowEffect)
        assertTrue(cinematic.blurEnabled)
    }

    @Test
    fun jsonRoundTripPreservesAllFields() {
        val original = LevyraLyricsStudioSettings(
            stylePreset = LyricsStylePreset.CINEMATIC,
            fontPreset = LevyraFontPreset.Outfit,
            textSize = LyricsTextSize.LARGE,
            lineSpacing = LyricsLineSpacing.RELAXED,
            fontWeight = LyricsFontWeightPreset.EXTRA_BOLD,
            alignment = LyricsAlignment.CENTER,
            inactiveScale = 0.85f,
            activeColorMode = LyricsActiveColorMode.CUSTOM,
            customAccentColor = 0xFFE91E63.toInt(),
            inactiveOpacity = 0.35f,
            glowEffect = LyricsGlowEffect.VIBRANT,
            backgroundMode = LyricsBackgroundMode.CINEMATIC,
            progressiveKaraokeEnabled = true,
            blurEnabled = true
        )

        val json = original.encode()
        val decoded = LevyraLyricsStudioSettings.decode(json)

        assertEquals(original, decoded)
    }

    @Test
    fun decodingInvalidOrEmptyJsonReturnsDefaultSafely() {
        val emptyDecoded = LevyraLyricsStudioSettings.decode("")
        assertEquals(LevyraLyricsStudioSettings(), emptyDecoded)

        val nullDecoded = LevyraLyricsStudioSettings.decode(null)
        assertEquals(LevyraLyricsStudioSettings(), nullDecoded)

        val malformedDecoded = LevyraLyricsStudioSettings.decode("{invalid_json: 123}")
        assertEquals(LevyraLyricsStudioSettings(), malformedDecoded)

        val partialJson = """{"stylePreset":"MINIMAL","alignment":"CENTER"}"""
        val partialDecoded = LevyraLyricsStudioSettings.decode(partialJson)
        assertEquals(LyricsStylePreset.MINIMAL, partialDecoded.stylePreset)
        assertEquals(LyricsAlignment.CENTER, partialDecoded.alignment)
        assertEquals(LevyraLyricsStudioSettings().textSize, partialDecoded.textSize)
    }

    @Test
    fun normalizationClampsExtremeValues() {
        val outOfBounds = LevyraLyricsStudioSettings(
            inactiveScale = 5.0f,
            inactiveOpacity = -0.5f
        ).normalized()

        assertTrue(outOfBounds.inactiveScale in 0.70f..1.0f)
        assertTrue(outOfBounds.inactiveOpacity in 0.15f..0.90f)
    }

    @Test
    fun demoLyricsHasValidLinesAndWordTiming() {
        val lines = DemoLyrics
        assertTrue("Demo lyrics should not be empty", lines.isNotEmpty())
        lines.forEach { line ->
            assertTrue("Line text should not be empty", line.text.isNotBlank())
            assertTrue("Line startMs should be non-negative", line.startMs >= 0)
        }
        val wordTimedLines = lines.filter { !it.words.isNullOrEmpty() }
        assertTrue("Demo lyrics should include word-timed lines", wordTimedLines.isNotEmpty())
    }
}
