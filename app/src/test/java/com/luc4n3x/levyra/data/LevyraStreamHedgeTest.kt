package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraStreamHedgeTest {
    @Test
    fun playbackExtractorIsDelayedBehindAttestedResolver() {
        val innerTubeDelay = LevyraResolverLatency.innerTubeFallbackDelayMs(
            isVideoMode = false,
            preferMp4Audio = false
        )
        val extractorDelay = LevyraResolverLatency.extractorHedgeDelayMs(
            isVideoMode = false,
            preferMp4Audio = false
        )

        assertEquals(0L, innerTubeDelay)
        assertEquals(600L, extractorDelay)
        assertTrue(extractorDelay > innerTubeDelay)
    }

    @Test
    fun playbackInnerTubeStartsInsideTapLatencyBudget() {
        assertTrue(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = false) <= 120L)
    }

    @Test
    fun innerTubeProfilesFanOutInsideLowLatencyBudget() {
        assertTrue(LevyraResolverLatency.INNER_TUBE_HEDGE_BUDGET_MS <= 150L)
    }

    @Test
    fun offlineMp4ExtractorStartsImmediately() {
        assertEquals(0L, LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = false, preferMp4Audio = true))
    }
}
