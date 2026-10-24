package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.VideoQualityLadder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraVideoDeviceTierTest {

    private val gib = 1024L * 1024L * 1024L

    @Test
    fun `phones with four gigabytes or less are budget video devices`() {
        assertTrue(isBudgetVideoDevice(lowRamDevice = false, totalMemoryBytes = 3L * gib))
        assertTrue(isBudgetVideoDevice(lowRamDevice = false, totalMemoryBytes = 4L * gib))
    }

    @Test
    fun `android go devices are always budget video devices`() {
        assertTrue(isBudgetVideoDevice(lowRamDevice = true, totalMemoryBytes = 8L * gib))
    }

    @Test
    fun `mid range and flagship phones keep the full ladder`() {
        assertFalse(isBudgetVideoDevice(lowRamDevice = false, totalMemoryBytes = 6L * gib))
        assertFalse(isBudgetVideoDevice(lowRamDevice = false, totalMemoryBytes = 12L * gib))
    }

    @Test
    fun `unknown memory size is not treated as budget`() {
        assertFalse(isBudgetVideoDevice(lowRamDevice = false, totalMemoryBytes = 0L))
    }

    @Test
    fun `budget devices are capped at 720p even on fast wifi with a 1080p screen`() {
        val height = VideoQualityLadder.autoTargetHeight(
            lowRam = isBudgetVideoDevice(lowRamDevice = false, totalMemoryBytes = 4L * gib),
            powerSave = false,
            unmetered = true,
            fastTransport = true,
            displayShortSidePx = 1080,
            hasHardwareAv1 = false
        )
        assertEquals(720, height)
    }

    @Test
    fun `measured decoder speed below the stream frame rate is not smooth`() {
        assertFalse(measuredFrameRateSustains(measuredUpperFps = 38.0, requestedFps = 60.0))
        assertTrue(measuredFrameRateSustains(measuredUpperFps = 58.0, requestedFps = 60.0))
        assertTrue(measuredFrameRateSustains(measuredUpperFps = 120.0, requestedFps = 60.0))
    }

    @Test
    fun `missing measurements keep the declared capability`() {
        assertTrue(measuredFrameRateSustains(measuredUpperFps = null, requestedFps = 60.0))
        assertTrue(measuredFrameRateSustains(measuredUpperFps = 10.0, requestedFps = 0.0))
    }
}
