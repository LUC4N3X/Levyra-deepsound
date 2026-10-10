package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedQueuePrefetchPolicyTest {
    private val eligible = AdvancedPrefetchConditions(
        windowSize = 10,
        videoMode = false,
        connected = true,
        unmetered = true,
        lowRam = false,
        powerConstrained = false,
        freeDiskBytes = 4L * 1024L * 1024L * 1024L
    )

    @Test
    fun eligibleConditionsAllowPrefetch() {
        assertTrue(advancedPrefetchAllowed(eligible))
    }

    @Test
    fun disabledWindowNeverPrefetches() {
        assertFalse(advancedPrefetchAllowed(eligible.copy(windowSize = 0)))
    }

    @Test
    fun meteredOrMissingNetworkBlocksPrefetch() {
        assertFalse(advancedPrefetchAllowed(eligible.copy(unmetered = false)))
        assertFalse(advancedPrefetchAllowed(eligible.copy(connected = false)))
    }

    @Test
    fun constrainedDevicesAndVideoModeBlockPrefetch() {
        assertFalse(advancedPrefetchAllowed(eligible.copy(videoMode = true)))
        assertFalse(advancedPrefetchAllowed(eligible.copy(lowRam = true)))
        assertFalse(advancedPrefetchAllowed(eligible.copy(powerConstrained = true)))
    }

    @Test
    fun lowFreeStorageBlocksPrefetch() {
        assertFalse(advancedPrefetchAllowed(eligible.copy(freeDiskBytes = ADVANCED_PREFETCH_MIN_FREE_DISK_BYTES - 1L)))
        assertTrue(advancedPrefetchAllowed(eligible.copy(freeDiskBytes = ADVANCED_PREFETCH_MIN_FREE_DISK_BYTES)))
    }

    @Test
    fun budgetUsesHalfOfTheExistingCacheLimit() {
        assertEquals(192L * 1024L * 1024L, advancedPrefetchBudgetBytes(384L * 1024L * 1024L))
        assertEquals(0L, advancedPrefetchBudgetBytes(0L))
    }

    @Test
    fun trackLimitIsCappedPerTrackAndByRemainingBudget() {
        val budget = 192L * 1024L * 1024L
        assertEquals(ADVANCED_PREFETCH_MAX_TRACK_BYTES, advancedPrefetchTrackLimit(0L, budget))
        assertEquals(10L * 1024L * 1024L, advancedPrefetchTrackLimit(budget - 10L * 1024L * 1024L, budget))
        assertEquals(0L, advancedPrefetchTrackLimit(budget - 512L * 1024L, budget))
        assertEquals(0L, advancedPrefetchTrackLimit(budget, budget))
    }

    @Test
    fun progressiveAudioAndMuxedStreamsAreEligibleButManifestsAreNot() {
        assertTrue(isProgressiveMediaMimeType("audio/webm"))
        assertTrue(isProgressiveMediaMimeType("audio/mp4"))
        assertTrue(isProgressiveMediaMimeType("video/mp4"))
        assertFalse(isProgressiveMediaMimeType("application/x-mpegURL"))
        assertFalse(isProgressiveMediaMimeType("application/dash+xml"))
        assertFalse(isProgressiveMediaMimeType(null))
    }
}
