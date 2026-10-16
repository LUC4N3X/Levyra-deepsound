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
    fun meteredNetworkPrefetchesOnlyWhenTheUserAllowsMobileData() {
        val mobile = eligible.copy(unmetered = false)
        assertFalse(advancedPrefetchAllowed(mobile))
        assertTrue(advancedPrefetchAllowed(mobile.copy(mobileDataAllowed = true)))
        assertFalse(advancedPrefetchAllowed(mobile.copy(mobileDataAllowed = true, connected = false)))
        assertFalse(advancedPrefetchAllowed(mobile.copy(mobileDataAllowed = true, lowRam = true)))
    }

    @Test
    fun networkGateRespectsMobileDataChoiceAndDataSaver() {
        assertTrue(advancedPrefetchNetworkAllowed(true, true, unmetered = true, mobileDataAllowed = false, dataSaverOn = false))
        assertFalse(advancedPrefetchNetworkAllowed(true, true, unmetered = false, mobileDataAllowed = false, dataSaverOn = false))
        assertTrue(advancedPrefetchNetworkAllowed(true, true, unmetered = false, mobileDataAllowed = true, dataSaverOn = false))
        assertFalse(advancedPrefetchNetworkAllowed(true, true, unmetered = false, mobileDataAllowed = true, dataSaverOn = true))
        assertFalse(advancedPrefetchNetworkAllowed(true, validated = false, unmetered = true, mobileDataAllowed = true, dataSaverOn = false))
    }

    @Test
    fun automaticQueueGrowsToTheSelectedPreloadWindow() {
        assertEquals(RADIO_TAIL_MIN_UPCOMING, radioTailTargetUpcoming(0))
        assertEquals(5, radioTailTargetUpcoming(5))
        assertEquals(15, radioTailTargetUpcoming(15))
        assertEquals(20, radioTailFetchLimit(targetUpcoming = 15, currentUpcoming = 0))
        assertEquals(5, radioTailFetchLimit(targetUpcoming = 3, currentUpcoming = 3))
        assertEquals(30, radioTailFetchLimit(targetUpcoming = 40, currentUpcoming = 0))
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
