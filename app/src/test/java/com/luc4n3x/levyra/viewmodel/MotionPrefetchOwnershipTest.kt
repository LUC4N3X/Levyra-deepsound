package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPrefetchOwnershipTest {

    @Test
    fun prefetchContinuesWhenItsTrackBecomesCurrentDuringTheDelay() {
        assertTrue(shouldContinueMotionPrefetch(activeKey = "next", currentKey = "current", nextKey = "next", queueChanged = true))
    }

    @Test
    fun prefetchContinuesForUnchangedQueue() {
        assertTrue(shouldContinueMotionPrefetch(activeKey = "current", currentKey = "current", nextKey = "next", queueChanged = false))
    }

    @Test
    fun prefetchStopsWhenQueueChangedUnderCurrentTrack() {
        assertFalse(shouldContinueMotionPrefetch(activeKey = "current", currentKey = "current", nextKey = "next", queueChanged = true))
    }

    @Test
    fun prefetchStopsForUnrelatedTrack() {
        assertFalse(shouldContinueMotionPrefetch(activeKey = "other", currentKey = "current", nextKey = "next", queueChanged = false))
    }
}
