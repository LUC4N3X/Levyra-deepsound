package com.luc4n3x.levyra.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResolveBudgetTest {
    @Test
    fun spotifyMatchingCannotOutlivePlaybackBudget() = runBlocking {
        val startedAt = System.nanoTime()
        val failure = runCatching {
            withinPlaybackResolveBudget(timeoutMs = 50L) {
                delay(5_000L)
            }
        }.exceptionOrNull()
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L

        assertTrue(failure is PlaybackBlockedException)
        assertTrue("Playback timeout must stop matching promptly, took ${elapsedMs}ms", elapsedMs < 1_000L)
    }
}
