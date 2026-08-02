package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekbarGeometryTest {

    @Test
    fun `progress fraction is zero when duration is unknown`() {
        assertEquals(0f, seekbarProgressFraction(5_000L, 0L), 0f)
        assertEquals(0f, seekbarProgressFraction(5_000L, -1L), 0f)
    }

    @Test
    fun `progress fraction never leaves the unit range`() {
        assertEquals(0.5f, seekbarProgressFraction(30_000L, 60_000L), 0.0001f)
        assertEquals(1f, seekbarProgressFraction(90_000L, 60_000L), 0f)
        assertEquals(0f, seekbarProgressFraction(-4_000L, 60_000L), 0f)
    }

    @Test
    fun `touch fraction is clamped to the visible track`() {
        assertEquals(0f, seekbarFractionAt(-40f, 200f), 0f)
        assertEquals(1f, seekbarFractionAt(400f, 200f), 0f)
        assertEquals(0.25f, seekbarFractionAt(50f, 200f), 0.0001f)
    }

    @Test
    fun `touch fraction is zero without a measured width`() {
        assertEquals(0f, seekbarFractionAt(120f, 0f), 0f)
    }

    @Test
    fun `handle stays fully inside the track at both ends`() {
        val width = 300f
        val handle = 12f
        assertEquals(6f, seekbarHandleCenterX(0f, width, handle), 0.0001f)
        assertEquals(294f, seekbarHandleCenterX(1f, width, handle), 0.0001f)
        assertEquals(150f, seekbarHandleCenterX(0.5f, width, handle), 0.0001f)
    }

    @Test
    fun `handle center survives a track narrower than the handle`() {
        val center = seekbarHandleCenterX(0.5f, 8f, 40f)
        assertTrue(center in 0f..8f)
    }

    @Test
    fun `tooltip stays within the track bounds`() {
        val width = 400f
        val tooltip = 80f
        assertEquals(0f, seekbarTooltipOffsetX(0f, width, tooltip), 0.0001f)
        assertEquals(320f, seekbarTooltipOffsetX(1f, width, tooltip), 0.0001f)
        assertEquals(160f, seekbarTooltipOffsetX(0.5f, width, tooltip), 0.0001f)
    }

    @Test
    fun `tooltip wider than the track collapses to the start`() {
        assertEquals(0f, seekbarTooltipOffsetX(0.5f, 40f, 90f), 0f)
    }

    @Test
    fun `wave sample count stays bounded`() {
        assertEquals(0, seekbarWaveSampleCount(0f))
        assertEquals(1, seekbarWaveSampleCount(1f))
        assertTrue(seekbarWaveSampleCount(100_000f) <= 900)
    }

    @Test
    fun `wave amplitude tapers to zero at both edges`() {
        val activeWidth = 200f
        val taper = 20f
        assertEquals(0f, seekbarWaveTaper(0f, activeWidth, taper), 0.0001f)
        assertEquals(0f, seekbarWaveTaper(activeWidth, activeWidth, taper), 0.0001f)
        assertEquals(1f, seekbarWaveTaper(100f, activeWidth, taper), 0.0001f)
    }

    @Test
    fun `wave offset is flat without amplitude or wavelength`() {
        assertEquals(0f, seekbarWaveOffset(10f, 0f, 30f, 0f), 0f)
        assertEquals(0f, seekbarWaveOffset(10f, 4f, 0f, 0f), 0f)
    }

    @Test
    fun `wave offset follows the sine of the travelled distance`() {
        val wavelength = 40f
        val amplitude = 3f
        assertEquals(0f, seekbarWaveOffset(0f, amplitude, wavelength, 0f), 0.0001f)
        assertEquals(amplitude, seekbarWaveOffset(wavelength / 4f, amplitude, wavelength, 0f), 0.0001f)
        assertEquals(-amplitude, seekbarWaveOffset(3f * wavelength / 4f, amplitude, wavelength, 0f), 0.0001f)
    }

    @Test
    fun `seek millis is clamped to the track duration`() {
        assertEquals(0L, seekbarSeekMillis(0.5f, 0L))
        assertEquals(30_000L, seekbarSeekMillis(0.5f, 60_000L))
        assertEquals(60_000L, seekbarSeekMillis(4f, 60_000L))
        assertEquals(0L, seekbarSeekMillis(-2f, 60_000L))
    }

    @Test
    fun `scrub label switches to hours only for long tracks`() {
        assertEquals("00:00", formatSeekbarMillis(-5_000L))
        assertEquals("03:07", formatSeekbarMillis(187_000L))
        assertEquals("1:00:05", formatSeekbarMillis(3_605_000L))
    }
}
