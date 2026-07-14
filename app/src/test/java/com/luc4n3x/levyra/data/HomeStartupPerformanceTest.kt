package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStartupPerformanceTest {
    @Test
    fun lowRamPlanKeepsBackgroundWorkSmall() {
        val plan = HomeStartupWorkPolicy.create(lowRam = true, powerConstrained = true)
        assertTrue(plan.priorityArtworkCount <= 2)
        assertTrue(plan.refreshedArtworkCount <= 4)
        assertEquals(1, plan.chartEnrichmentConcurrency)
        assertEquals(0, plan.chartWarmCount)
        assertTrue(plan.releaseRadarArtistCount <= 2)
        assertTrue(plan.releasesPerArtist <= 4)
        assertTrue(plan.idleWindowMs >= 650L)
    }

    @Test
    fun normalPlanStillPreloadsUsefulArtwork() {
        val plan = HomeStartupWorkPolicy.create(lowRam = false, powerConstrained = false)
        assertTrue(plan.priorityArtworkCount >= 6)
        assertTrue(plan.refreshedArtworkCount >= 10)
        assertEquals(2, plan.chartEnrichmentConcurrency)
        assertTrue(plan.chartWarmCount >= 1)
        assertTrue(plan.releaseRadarArtistCount >= 6)
    }

    @Test
    fun playbackWarmupRemainsImmediateOnOlderDevices() {
        val plan = StartupPlaybackWarmPolicy.create(
            lowRam = true,
            powerConstrained = true,
            preferredConcurrency = 2
        )
        assertTrue(plan.delayMs <= 180L)
        assertEquals(1, plan.trackCount)
        assertEquals(1, plan.concurrency)
    }

    @Test
    fun playbackWarmupKeepsParallelCapacityOnModernDevices() {
        val plan = StartupPlaybackWarmPolicy.create(
            lowRam = false,
            powerConstrained = false,
            preferredConcurrency = 2
        )
        assertTrue(plan.delayMs <= 100L)
        assertEquals(3, plan.trackCount)
        assertEquals(2, plan.concurrency)
    }

    @Test
    fun interactionGateRequiresAStableIdleWindow() {
        var now = 1_000L
        val gate = HomeInteractionGate { now }
        gate.update(true)
        assertTrue(gate.remainingIdleMs(500L) > 0L)
        gate.update(false)
        now += 300L
        assertEquals(200L, gate.remainingIdleMs(500L))
        now += 200L
        assertEquals(0L, gate.remainingIdleMs(500L))
    }
}
