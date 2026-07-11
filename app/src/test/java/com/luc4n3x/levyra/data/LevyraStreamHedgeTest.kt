package com.luc4n3x.levyra.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class LevyraStreamHedgeTest {
    @Test
    fun firstTierWinnerSkipsLaterTiers() = runBlocking {
        val laterCalls = AtomicInteger(0)
        val primary: suspend () -> String? = { "primary" }
        val backup: suspend () -> String? = {
            laterCalls.incrementAndGet()
            "backup"
        }

        val result = hedgedFirst(listOf(listOf(primary), listOf(backup)), hedgeBudgetMs = 1_000L)

        assertEquals("primary", result)
        assertEquals(0, laterCalls.get())
    }

    @Test
    fun fallsThroughToNextTierWhenEarlierFails() = runBlocking {
        val primaryCalls = AtomicInteger(0)
        val primary: suspend () -> String? = {
            primaryCalls.incrementAndGet()
            null
        }
        val backup: suspend () -> String? = { "backup" }

        val result = hedgedFirst(listOf(listOf(primary), listOf(backup)), hedgeBudgetMs = 25L)

        assertEquals("backup", result)
        assertEquals(1, primaryCalls.get())
    }

    @Test
    fun returnsNullWhenEveryAttemptFails() = runBlocking {
        val failing: suspend () -> String? = { null }
        val throwing: suspend () -> String? = { throw IllegalStateException("boom") }
        val captured = AtomicInteger(0)

        val result = hedgedFirst(
            listOf(listOf(failing), listOf(throwing)),
            hedgeBudgetMs = 20L
        ) { captured.incrementAndGet() }

        assertNull(result)
        assertEquals(1, captured.get())
    }

    @Test
    fun emptyLadderReturnsNull() = runBlocking {
        val result = hedgedFirst<String>(emptyList(), hedgeBudgetMs = 10L)
        assertNull(result)
    }

    @Test
    fun playbackExtractorStartsInsideLowLatencyBudget() {
        assertTrue(LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = false, preferMp4Audio = false) <= 40L)
    }

    @Test
    fun playbackInnerTubeFallbackStartsInsideTapLatencyBudget() {
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
