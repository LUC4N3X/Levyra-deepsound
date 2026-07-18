package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkRuntimeTest {
    @Test
    fun normalizedConfigurationIsBoundedAndDeduplicated() {
        val normalized = MotionArtworkConfig(
            providerOrder = listOf("apple-motion", "apple-motion", "tidal-video-cover"),
            minimumConfidence = 500,
            requestTimeoutMs = 100L,
            positiveTtlMs = Long.MAX_VALUE,
            negativeTtlMs = 1L
        ).normalized()

        assertEquals(listOf("apple-motion", "tidal-video-cover"), normalized.providerOrder)
        assertEquals(100, normalized.minimumConfidence)
        assertEquals(2_500L, normalized.requestTimeoutMs)
        assertTrue(normalized.positiveTtlMs <= 7L * 24L * 60L * 60L * 1000L)
        assertEquals(10L * 60L * 1000L, normalized.negativeTtlMs)
    }

    @Test
    fun runtimeEpochChangesOnlyForARealConfigurationChange() {
        val initial = MotionArtworkRuntime.snapshot()
        try {
            val changedOrder = initial.value.providerOrder.reversed().ifEmpty { listOf("tidal-video-cover") }
            val changed = MotionArtworkRuntime.update(initial.value.copy(providerOrder = changedOrder))
            val repeated = MotionArtworkRuntime.update(changed.value)

            if (changed.value == initial.value) {
                assertEquals(initial.epoch, changed.epoch)
            } else {
                assertEquals(initial.epoch + 1L, changed.epoch)
            }
            assertEquals(changed.epoch, repeated.epoch)
        } finally {
            MotionArtworkRuntime.update(initial.value)
        }
    }
}
