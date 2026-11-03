package com.luc4n3x.levyra.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class AudioClientRecoveryTest {
    private fun absent() = YoutubePlayerRequestException(null, "empty", noPlayableStream = true)

    @Test
    fun childCancellationInMultipleNormalClientsSettlesTheWholeHedge() = runBlocking {
        val cancellation = CancellationException("client cancelled")
        try {
            withTimeout(1_000L) {
                hedgedAudioWithRecovery<String>(
                    listOf(listOf({ delay(20); throw cancellation }), listOf({ throw absent() })), 1,
                    recovery = { fail("cancelled resolution must not recover"); null })
            }
            fail("expected cancellation")
        } catch (actual: CancellationException) {
            assertTrue(generateSequence<Throwable>(actual) { it.cause }.any { it === cancellation })
        }
    }

    @Test
    fun recoveryWaitsForAllNormalClientsToFinish() = runBlocking {
        val finished = AtomicInteger()
        val attempts = List(3) { index ->
            listOf<suspend () -> String?>({
                delay((3 - index) * 10L)
                finished.incrementAndGet()
                throw absent()
            })
        }
        val result = hedgedAudioWithRecovery(attempts, 1, recovery = {
            assertEquals(3, finished.get())
            "low"
        })
        assertEquals("low", result)
    }

    @Test
    fun normalQualityWinnerNeverRunsRecovery() = runBlocking {
        val result = hedgedAudioWithRecovery(
            listOf(listOf<suspend () -> String?>({ "original" })), 1,
            recovery = { fail("quality must remain original"); null }
        )
        assertEquals("original", result)
    }

    @Test
    fun ambiguousResultsNetworkHttpAndRestrictionsSuppressRecovery() = runBlocking {
        val failures = listOf<Throwable>(IOException("offline"),
            YoutubePlayerRequestException(429, "HTTP 429"),
            YoutubePlayerRequestException(null, "age restriction"),
            IllegalStateException("decipher failed"))
        for (failure in failures) {
            assertNull(hedgedAudioWithRecovery<String>(
                listOf(listOf({ throw absent() }), listOf({ throw failure })), 1,
                recovery = { fail("must preserve failure boundary"); null }
            ))
        }
        assertNull(hedgedAudioWithRecovery<String>(listOf(listOf({ null })), 1,
            recovery = { fail("null is inconclusive"); null }))
    }

    @Test
    fun recoveryFailureDoesNotStartAnotherRound() = runBlocking {
        var calls = 0
        val failure = IOException("recovery offline")
        val errors = mutableListOf<Throwable>()
        assertNull(hedgedAudioWithRecovery<String>(listOf(listOf({ throw absent() })), 1,
            recovery = { calls++; throw failure }, onAttemptError = errors::add))
        assertEquals(1, calls)
        assertSame(failure, errors.last())
    }

    @Test
    fun cancellationPropagatesFromBothRounds() = runBlocking {
        for (cancelRecovery in listOf(false, true)) {
            val cancellation = CancellationException("test cancellation")
            val normal: suspend () -> String? = { if (cancelRecovery) throw absent() else throw cancellation }
            try {
                hedgedAudioWithRecovery(listOf(listOf(normal)), 1,
                    recovery = { throw cancellation })
                fail("expected cancellation")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }
    }

    @Test
    fun absentNormalClientsDoNotTriggerSpeculativeRecovery() = runBlocking {
        assertNull(hedgedAudioWithRecovery<String>(emptyList(), 1,
            recovery = { fail("no normal attempt"); null }))
    }
}
