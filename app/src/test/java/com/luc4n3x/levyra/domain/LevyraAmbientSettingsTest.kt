package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraAmbientSettingsTest {

    @Test
    fun defaultsKeepTheStandbyScreenReadableAndSafe() {
        val settings = LevyraAmbientSettings()

        assertEquals(LevyraAmbientMode.Artwork, settings.mode)
        assertTrue(settings.showClock)
        assertTrue(settings.showTitle)
        assertTrue(settings.showProgress)
        assertTrue(settings.amoledBlack)
        assertTrue(settings.pixelShift)
    }

    @Test
    fun normalizationClampsBrightnessAndAutoDimWindow() {
        val tooDark = LevyraAmbientSettings(brightness = -2f, autoDimAfterSeconds = 1).normalized()
        val tooBright = LevyraAmbientSettings(brightness = 9f, autoDimAfterSeconds = 9_999).normalized()

        assertEquals(LevyraAmbientSettings.MIN_BRIGHTNESS, tooDark.brightness, 0.0001f)
        assertEquals(LevyraAmbientSettings.MIN_AUTO_DIM_SECONDS, tooDark.autoDimAfterSeconds)
        assertEquals(LevyraAmbientSettings.MAX_BRIGHTNESS, tooBright.brightness, 0.0001f)
        assertEquals(LevyraAmbientSettings.MAX_AUTO_DIM_SECONDS, tooBright.autoDimAfterSeconds)
        assertEquals(
            LevyraAmbientSettings.MAX_AUTO_DIM_SECONDS * 1_000L,
            tooBright.autoDimAfterMs
        )
    }

    @Test
    fun unknownStoredModeFallsBackToArtwork() {
        assertEquals(LevyraAmbientMode.Artwork, LevyraAmbientMode.from(null))
        assertEquals(LevyraAmbientMode.Artwork, LevyraAmbientMode.from("not-a-mode"))
        assertEquals(LevyraAmbientMode.Lyrics, LevyraAmbientMode.from("lyrics"))
        assertEquals(LevyraAmbientMode.Spotlight, LevyraAmbientMode.from("  SPOTLIGHT "))
        assertEquals(LevyraAmbientMode.Minimal, LevyraAmbientMode.from("minimal"))
    }

    @Test
    fun storedModeIdsStayStableForPersistence() {
        assertEquals(
            listOf("minimal", "artwork", "spotlight", "lyrics"),
            LevyraAmbientMode.entries.map { it.id }
        )
    }

    @Test
    fun artworkAndLyricUsageFollowTheSelectedMode() {
        val minimal = LevyraAmbientSettings(mode = LevyraAmbientMode.Minimal)
        val artwork = LevyraAmbientSettings(mode = LevyraAmbientMode.Artwork)
        val spotlight = LevyraAmbientSettings(mode = LevyraAmbientMode.Spotlight)
        val lyrics = LevyraAmbientSettings(mode = LevyraAmbientMode.Lyrics)

        assertFalse(minimal.usesArtwork)
        assertTrue(artwork.usesArtwork)
        assertTrue(spotlight.usesArtwork)
        assertFalse(lyrics.usesArtwork)

        assertFalse(minimal.usesLyricLine)
        assertTrue(artwork.usesLyricLine)
        assertTrue(lyrics.usesLyricLine)
        assertFalse(lyrics.copy(showLyrics = false).usesLyricLine)
    }

    @Test
    fun pixelShiftWalksABoundedPatternAndReturnsToTheOrigin() {
        val offsets = (0 until 12).map { ambientPixelShiftOffset(it) }

        offsets.forEach { (x, y) ->
            assertTrue(kotlin.math.abs(x) <= LevyraAmbientSettings.PIXEL_SHIFT_RANGE_DP)
            assertTrue(kotlin.math.abs(y) <= LevyraAmbientSettings.PIXEL_SHIFT_RANGE_DP)
        }
        assertEquals(0f to 0f, ambientPixelShiftOffset(0))
        assertEquals(offsets[0], offsets[6])
        assertNotEquals(offsets[0], offsets[1])
        assertEquals(0f to 0f, ambientPixelShiftOffset(-5))
        assertEquals(0f to 0f, ambientPixelShiftOffset(3, rangeDp = 0f))
    }
}
