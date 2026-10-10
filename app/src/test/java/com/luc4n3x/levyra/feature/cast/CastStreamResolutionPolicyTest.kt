package com.luc4n3x.levyra.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastStreamResolutionPolicyTest {

    @Test
    fun isStaleReturnsFalseWhenWellWithinTtl() {
        assertFalse(CastStreamResolutionPolicy.isStale(resolvedAtMs = 1_000L, ttlMs = 300_000L, nowMs = 1_500L))
    }

    @Test
    fun isStaleReturnsFalseAtExactZeroAge() {
        assertFalse(CastStreamResolutionPolicy.isStale(resolvedAtMs = 5_000L, ttlMs = 300_000L, nowMs = 5_000L))
    }

    @Test
    fun isStaleReturnsTrueWhenAgeEqualsTtl() {
        assertTrue(CastStreamResolutionPolicy.isStale(resolvedAtMs = 0L, ttlMs = 300_000L, nowMs = 300_000L))
    }

    @Test
    fun isStaleReturnsTrueWhenAgeExceedsTtl() {
        assertTrue(CastStreamResolutionPolicy.isStale(resolvedAtMs = 0L, ttlMs = 300_000L, nowMs = 300_001L))
    }

    @Test
    fun isStaleTreatsFutureResolvedTimestampAsStale() {
        assertTrue(CastStreamResolutionPolicy.isStale(resolvedAtMs = 10_000L, ttlMs = 300_000L, nowMs = 9_000L))
    }

    @Test
    fun isStaleTreatsNonPositiveTtlAsAlwaysStale() {
        assertTrue(CastStreamResolutionPolicy.isStale(resolvedAtMs = 1_000L, ttlMs = 0L, nowMs = 1_000L))
    }

    @Test
    fun itemsNeedingResolutionReturnsOnlyBoundedWindowAroundCurrentIndex() {
        val queueIds = (0 until 20).map { "track-$it" }

        val needed = CastStreamResolutionPolicy.itemsNeedingResolution(
            queueIds = queueIds,
            currentIndex = 10,
            alreadyResolvedIds = emptySet(),
            windowRadius = 2
        )

        assertEquals(listOf("track-8", "track-9", "track-10", "track-11", "track-12"), needed)
    }

    @Test
    fun itemsNeedingResolutionExcludesAlreadyResolvedIds() {
        val queueIds = (0 until 10).map { "track-$it" }

        val needed = CastStreamResolutionPolicy.itemsNeedingResolution(
            queueIds = queueIds,
            currentIndex = 5,
            alreadyResolvedIds = setOf("track-4", "track-6"),
            windowRadius = 2
        )

        assertEquals(listOf("track-3", "track-5", "track-7"), needed)
    }

    @Test
    fun itemsNeedingResolutionOnEmptyQueueReturnsEmptyList() {
        val needed = CastStreamResolutionPolicy.itemsNeedingResolution(
            queueIds = emptyList(),
            currentIndex = 0,
            alreadyResolvedIds = emptySet()
        )

        assertTrue(needed.isEmpty())
    }

    @Test
    fun itemsNeedingResolutionClampsWindowAtListBoundaries() {
        val queueIds = (0 until 5).map { "track-$it" }

        val needed = CastStreamResolutionPolicy.itemsNeedingResolution(
            queueIds = queueIds,
            currentIndex = 0,
            alreadyResolvedIds = emptySet(),
            windowRadius = 2
        )

        assertEquals(listOf("track-0", "track-1", "track-2"), needed)
    }
}
