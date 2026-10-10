package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePrefetchPolicyTest {
    @Test
    fun unmeteredNetworkPrimesThreeUpcomingTracks() {
        assertEquals(
            3,
            queuePrefetchPrimeDepth(
                resolveCount = 3,
                videoMode = false,
                lowRam = false,
                powerConstrained = false,
                unmetered = true
            )
        )
    }

    @Test
    fun meteredNetworkPrimesTwoUpcomingTracks() {
        assertEquals(
            2,
            queuePrefetchPrimeDepth(
                resolveCount = 2,
                videoMode = false,
                lowRam = false,
                powerConstrained = false,
                unmetered = false
            )
        )
    }

    @Test
    fun constrainedDevicesAndVideoModeStayOnTheImmediateNextTrack() {
        assertEquals(
            1,
            queuePrefetchPrimeDepth(
                resolveCount = 3,
                videoMode = false,
                lowRam = false,
                powerConstrained = true,
                unmetered = true
            )
        )
        assertEquals(
            1,
            queuePrefetchPrimeDepth(
                resolveCount = 3,
                videoMode = false,
                lowRam = true,
                powerConstrained = false,
                unmetered = true
            )
        )
        assertEquals(
            1,
            queuePrefetchPrimeDepth(
                resolveCount = 3,
                videoMode = true,
                lowRam = false,
                powerConstrained = false,
                unmetered = true
            )
        )
    }

    @Test
    fun primeDepthNeverExceedsTheResolvedCandidateCount() {
        assertEquals(
            1,
            queuePrefetchPrimeDepth(
                resolveCount = 1,
                videoMode = false,
                lowRam = false,
                powerConstrained = false,
                unmetered = true
            )
        )
        assertEquals(
            0,
            queuePrefetchPrimeDepth(
                resolveCount = 0,
                videoMode = false,
                lowRam = false,
                powerConstrained = false,
                unmetered = true
            )
        )
    }

    @Test
    fun byteBudgetDecreasesWithQueueDistance() {
        val base = 448L * 1024L
        val first = queuePrefetchPrimeBytes(0, base)
        val second = queuePrefetchPrimeBytes(1, base)
        val third = queuePrefetchPrimeBytes(2, base)
        assertEquals(base, first)
        assertTrue(second < first)
        assertTrue(third < second)
        assertTrue(third > 0L)
    }

    @Test
    fun farQueueDistancesNeverDownloadMoreThanTheNearestTrack() {
        val base = 160L * 1024L
        assertEquals(base, queuePrefetchPrimeBytes(0, base))
        assertTrue(queuePrefetchPrimeBytes(1, base) <= base)
        assertTrue(queuePrefetchPrimeBytes(5, base) <= base)
        assertEquals(queuePrefetchPrimeBytes(2, base), queuePrefetchPrimeBytes(7, base))
    }

    @Test
    fun invalidInputsRequestNoBytes() {
        assertEquals(0L, queuePrefetchPrimeBytes(-1, 448L * 1024L))
        assertEquals(0L, queuePrefetchPrimeBytes(0, 0L))
    }
}
