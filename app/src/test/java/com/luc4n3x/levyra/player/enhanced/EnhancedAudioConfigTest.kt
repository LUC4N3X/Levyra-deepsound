package com.luc4n3x.levyra.player.enhanced

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedAudioConfigTest {

    @Test
    fun defaultValues_areActiveAndConservative() {
        val config = EnhancedAudioConfig()
        assertTrue(config.enabled)
        assertEquals(0.50f, config.adaptiveGainCap, 0.001f)
        assertEquals(0.15f, config.deficitThreshold, 0.001f)
        assertEquals(18_500f, config.cutoffFrequencyHz, 0.001f)
        assertEquals(0.08f, config.harmonicGain, 0.001f)
        assertEquals(0.35f, config.transientSensitivity, 0.001f)
        assertEquals(0.944f, config.truePeakCeilingLinear, 0.001f)
        assertTrue(config.protectStereo)
    }

    @Test
    fun normalization_boundsExtremeValues() {
        val extreme = EnhancedAudioConfig(
            adaptiveGainCap = 2.5f,
            deficitThreshold = 1.5f,
            cutoffFrequencyHz = 30_000f,
            harmonicGain = 1.0f,
            transientSensitivity = 5.0f,
            truePeakCeilingLinear = 1.5f
        ).normalized()

        assertEquals(1.0f, extreme.adaptiveGainCap, 0.001f)
        assertEquals(0.99f, extreme.deficitThreshold, 0.001f)
        assertEquals(22_000f, extreme.cutoffFrequencyHz, 0.001f)
        assertEquals(0.30f, extreme.harmonicGain, 0.001f)
        assertEquals(1.0f, extreme.transientSensitivity, 0.001f)
        assertEquals(1.0f, extreme.truePeakCeilingLinear, 0.001f)
    }

    @Test
    fun normalization_boundsNegativeValues() {
        val negative = EnhancedAudioConfig(
            adaptiveGainCap = -0.5f,
            deficitThreshold = -0.1f,
            cutoffFrequencyHz = 2_000f,
            harmonicGain = -0.2f,
            transientSensitivity = -1.0f,
            truePeakCeilingLinear = 0.2f
        ).normalized()

        assertEquals(0.0f, negative.adaptiveGainCap, 0.001f)
        assertEquals(0.01f, negative.deficitThreshold, 0.001f)
        assertEquals(10_000f, negative.cutoffFrequencyHz, 0.001f)
        assertEquals(0.0f, negative.harmonicGain, 0.001f)
        assertEquals(0.0f, negative.transientSensitivity, 0.001f)
        assertEquals(0.70f, negative.truePeakCeilingLinear, 0.001f)
    }
}
