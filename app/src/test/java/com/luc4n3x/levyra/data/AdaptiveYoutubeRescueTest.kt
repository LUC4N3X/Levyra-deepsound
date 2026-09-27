package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiFailureKind
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiHealthState
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun degradedByeDpiTriesPipedImmediately() = runBlocking {
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
    fun degradedPipedFailureFallsBackToExistingResolverWithoutLooping() = runBlocking {
        val order = mutableListOf<String>()
        val result = AdaptiveYoutubeRescue(FakeHealth(ByeDpiHealthState.DEGRADED)).resolve(
            byeDpiEnabled = true,
            direct = { order += "direct"; "direct" },
            piped = { order += "piped"; null }
        )

        assertEquals("direct", result)
        assertEquals(listOf("piped", "direct"), order)
    }

    @Test
    fun dueRecoveryProbeRacesOnlyOneDirectPathWithPiped() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.DEGRADED, probeDue = true)
        val directCancelled = AtomicBoolean(false)
        val result = AdaptiveYoutubeRescue(health).resolve(
            byeDpiEnabled = true,
            direct = {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { directCancelled.set(true) }
                }
            },
            piped = { "piped" }
        )

        assertEquals("piped", result)
        assertTrue(directCancelled.get())
    }

    @Test
    fun cancellationDuringTrackChangeIsNotConvertedIntoFallback() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)
        var pipedCalls = 0
        val directCancelled = AtomicBoolean(false)
        val job = launch {
            AdaptiveYoutubeRescue(health).resolve(
                byeDpiEnabled = true,
                direct = {
                    suspendCancellableCoroutine { continuation ->
                        continuation.invokeOnCancellation { directCancelled.set(true) }
                    }
                },
                piped = { pipedCalls++; "piped" }
            )
        }
        yield()

        job.cancelAndJoin()

        assertTrue(directCancelled.get())
        assertEquals(0, pipedCalls)
        assertTrue(health.failures.isEmpty())
    }

    @Test
    fun successfulFastDirectResolutionFeedsHealth() = runBlocking {
        val health = FakeHealth(ByeDpiHealthState.HEALTHY)

        val result = AdaptiveYoutubeRescue(health).resolve(
            byeDpiEnabled = true,
            direct = { delay(1); "direct" },
            piped = { "piped" }
        )

        assertEquals("direct", result)
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
