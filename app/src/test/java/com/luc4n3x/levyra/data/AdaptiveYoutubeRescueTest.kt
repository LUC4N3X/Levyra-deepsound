package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiFailureKind
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiHealthState
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveYoutubeRescueTest {
    @Test
    fun byeDpiOffKeepsDirectFirstAndAvoidsUnneededPipedCall() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)
        var pipedCalls = 0
        val result = AdaptiveYoutubeRescue(health).resolve(
            byeDpiEnabled = false,
            direct = { "direct" },
            piped = { pipedCalls++; "piped" }
        )

        assertEquals("direct", result)
        assertEquals(0, pipedCalls)
        assertTrue(health.failures.isEmpty())
    }

    @Test
    fun byeDpiOffUsesPipedOnlyAfterExistingDirectFallbacksFail() = runBlocking {
        val order = mutableListOf<String>()
        val result = AdaptiveYoutubeRescue(FakeHealth(ByeDpiHealthState.HEALTHY)).resolve(
            byeDpiEnabled = false,
            direct = { order += "direct"; null },
            piped = { order += "piped"; "rescued" }
        )

        assertEquals("rescued", result)
        assertEquals(listOf("direct", "piped"), order)
    }

    @Test
    fun byeDpiOnUsesPipedFirstEvenWhenHealthy() = runBlocking {
        val order = mutableListOf<String>()
        val result = AdaptiveYoutubeRescue(FakeHealth(ByeDpiHealthState.HEALTHY)).resolve(
            byeDpiEnabled = true,
            direct = { order += "direct"; "direct" },
            piped = { order += "piped"; "rescued" }
        )

        assertEquals("rescued", result)
        assertEquals(listOf("piped"), order)
    }

    @Test
    fun degradedByeDpiAlsoUsesPipedFirst() = runBlocking {
        val order = mutableListOf<String>()
        val result = AdaptiveYoutubeRescue(FakeHealth(ByeDpiHealthState.DEGRADED)).resolve(
            byeDpiEnabled = true,
            direct = { order += "direct"; "direct" },
            piped = { order += "piped"; "rescued" }
        )

        assertEquals("rescued", result)
        assertEquals(listOf("piped"), order)
    }

    @Test
    fun pipedFailureFallsBackToOneBoundedByeDpiAttempt() = runBlocking {
        val order = mutableListOf<String>()
        val result = AdaptiveYoutubeRescue(FakeHealth(ByeDpiHealthState.HEALTHY)).resolve(
            byeDpiEnabled = true,
            direct = { order += "direct"; "direct" },
            piped = { order += "piped"; null }
        )

        assertEquals("direct", result)
        assertEquals(listOf("piped", "direct"), order)
    }

    @Test
    fun pipedFailureBoundsTheOnlyDirectAttempt() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.DEGRADED)
        val directCancelled = AtomicBoolean(false)
        val rescue = AdaptiveYoutubeRescue(
            health = health,
            resolutionBudgetMs = 25L,
            recoveryProbeBudgetMs = 25L
        )

        val result = rescue.resolve(
            byeDpiEnabled = true,
            direct = {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { directCancelled.set(true) }
                }
            },
            piped = { null }
        )

        assertNull(result)
        assertTrue(directCancelled.get())
        assertTrue(health.failures.contains(ByeDpiFailureKind.TIMEOUT))
    }

    @Test
    fun blockingDirectAttemptCannotOutliveItsBudget() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)
        val rescue = AdaptiveYoutubeRescue(
            health = health,
            resolutionBudgetMs = 50L,
            recoveryProbeBudgetMs = 50L
        )
        val startedAt = System.nanoTime()

        val result = rescue.resolve(
            byeDpiEnabled = true,
            direct = {
                Thread.sleep(2_000L)
                "late"
            },
            piped = { null }
        )

        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
        assertNull(result)
        assertTrue("elapsed=$elapsedMs", elapsedMs < 1_000L)
        assertTrue(health.failures.contains(ByeDpiFailureKind.TIMEOUT))
    }

    @Test
    fun pipedFailureNeverStartsDirectMoreThanOnce() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)
        val directCalls = AtomicInteger(0)
        val rescue = AdaptiveYoutubeRescue(
            health = health,
            resolutionBudgetMs = 25L,
            recoveryProbeBudgetMs = 25L
        )

        val result = rescue.resolve(
            byeDpiEnabled = true,
            direct = {
                directCalls.incrementAndGet()
                suspendCancellableCoroutine { }
            },
            piped = { null }
        )

        assertNull(result)
        assertEquals(1, directCalls.get())
    }

    @Test
    fun dueRecoveryProbeDoesNotRaceHealthyPipedResult() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.DEGRADED, probeDue = true)
        var directCalls = 0
        val result = AdaptiveYoutubeRescue(health).resolve(
            byeDpiEnabled = true,
            direct = { directCalls++; "direct" },
            piped = { "piped" }
        )

        assertEquals("piped", result)
        assertEquals(0, directCalls)
    }

    @Test
    fun cancellationDuringPipedResolutionIsNotConvertedIntoByeDpiFallback() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)
        var directCalls = 0
        val pipedCancelled = AtomicBoolean(false)
        val job = launch {
            AdaptiveYoutubeRescue(health).resolve(
                byeDpiEnabled = true,
                direct = { directCalls++; "direct" },
                piped = {
                    suspendCancellableCoroutine { continuation ->
                        continuation.invokeOnCancellation { pipedCancelled.set(true) }
                    }
                }
            )
        }
        yield()

        job.cancelAndJoin()

        assertTrue(pipedCancelled.get())
        assertEquals(0, directCalls)
        assertTrue(health.failures.isEmpty())
    }

    @Test
    fun fastDirectFallbackFeedsHealthAfterPipedMiss() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)
        val order = mutableListOf<String>()

        val result = AdaptiveYoutubeRescue(health).resolve(
            byeDpiEnabled = true,
            direct = { order += "direct"; delay(1); "direct" },
            piped = { order += "piped"; null }
        )

        assertEquals("direct", result)
        assertEquals(listOf("piped", "direct"), order)
        assertEquals(1, health.successes.size)
        assertFalse(health.successes.first() <= 0L)
    }

    private class FakeHealth(
        private var currentState: ByeDpiHealthState,
        private val probeDue: Boolean = false
    ) : ByeDpiHealthGateway {
        val successes = CopyOnWriteArrayList<Long>()
        val failures = CopyOnWriteArrayList<ByeDpiFailureKind>()

        override fun state(): ByeDpiHealthState = currentState

        override fun isProbeDue(): Boolean = probeDue

        override fun recordSuccess(latencyMs: Long) {
            successes += latencyMs
            currentState = ByeDpiHealthState.HEALTHY
        }

        override fun recordFailure(kind: ByeDpiFailureKind) {
            failures += kind
            currentState = if (kind == ByeDpiFailureKind.TIMEOUT) {
                ByeDpiHealthState.DEGRADED
            } else {
                ByeDpiHealthState.SUSPECT
            }
        }
    }
}
