package com.luc4n3x.levyra.player.enhanced

import org.junit.Assert.assertEquals
import org.junit.Test

class EnhancedAudioConfigTest {
    @Test
    fun defaultValues_areConservative() {
        val config = EnhancedAudioConfig()
        assertEquals(0.15f, config.deficitThreshold, 0.001f)
        assertEquals(0.944f, config.truePeakCeilingLinear, 0.001f)
        assertEquals(5_000L, config.maxAllowedProcessingTimeUs)
    }

    @Test
    fun normalization_boundsExtremeValues() {
        val extreme = EnhancedAudioConfig(deficitThreshold = 5f, truePeakCeilingLinear = 1.5f).normalized()
        assertEquals(0.99f, extreme.deficitThreshold, 0.001f)
        assertEquals(1.0f, extreme.truePeakCeilingLinear, 0.001f)

        val negative = EnhancedAudioConfig(deficitThreshold = -1f, truePeakCeilingLinear = 0.1f).normalized()
        assertEquals(0.01f, negative.deficitThreshold, 0.001f)
        assertEquals(0.70f, negative.truePeakCeilingLinear, 0.001f)
    }
}
