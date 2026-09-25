package com.luc4n3x.levyra.ui.theme

import com.luc4n3x.levyra.domain.LevyraVisualPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraVisualPerformanceTest {

    @Test
    fun fullKeepsEveryEffectEvenOnConstrainedDevices() {
        val capabilities = resolveVisualCapabilities(
            selected = LevyraVisualPerformance.Full,
            animationsEnabled = true,
            lowRamDevice = true,
            powerSaveMode = true
        )
        assertEquals(LevyraVisualCapabilities.Full, capabilities)
    }

    @Test
    fun compositionDefaultMatchesFull() {
        assertEquals(
            LevyraVisualCapabilities.Full,
            resolveVisualCapabilities(LevyraVisualPerformance.Full, true, false, false)
        )
    }

    @Test
    fun smoothDropsDecorativeCostButNeverDependsOnDeviceSignals() {
        listOf(false, true).forEach { lowRam ->
            listOf(false, true).forEach { powerSave ->
                val capabilities = resolveVisualCapabilities(LevyraVisualPerformance.Smooth, true, lowRam, powerSave)
                assertFalse(capabilities.decorativeMotion)
                assertFalse(capabilities.depthTransitions)
                assertFalse(capabilities.microMotion)
                assertFalse(capabilities.heavyBlur)
            }
        }
    }

    @Test
    fun autoResolvesFromStableDeviceSignalsOnly() {
        assertEquals(
            LevyraVisualPerformance.Full,
            resolveVisualPerformance(LevyraVisualPerformance.Auto, lowRamDevice = false, powerSaveMode = false)
        )
        assertEquals(
            LevyraVisualPerformance.Smooth,
            resolveVisualPerformance(LevyraVisualPerformance.Auto, lowRamDevice = true, powerSaveMode = false)
        )
        assertEquals(
            LevyraVisualPerformance.Smooth,
            resolveVisualPerformance(LevyraVisualPerformance.Auto, lowRamDevice = false, powerSaveMode = true)
        )
    }

    @Test
    fun autoIsDeterministicForTheSameSignals() {
        val first = resolveVisualCapabilities(LevyraVisualPerformance.Auto, true, false, true)
        repeat(20) {
            assertEquals(first, resolveVisualCapabilities(LevyraVisualPerformance.Auto, true, false, true))
        }
    }

    @Test
    fun reducedMotionTurnsOffMotionButKeepsFullMaterial() {
        val capabilities = resolveVisualCapabilities(LevyraVisualPerformance.Full, false, false, false)
        assertFalse(capabilities.decorativeMotion)
        assertFalse(capabilities.depthTransitions)
        assertFalse(capabilities.microMotion)
        assertTrue(capabilities.heavyBlur)
    }
}
