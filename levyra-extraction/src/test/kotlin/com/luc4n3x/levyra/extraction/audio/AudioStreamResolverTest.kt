package com.luc4n3x.levyra.extraction.audio

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AudioStreamResolverTest {
    private class NoStream : IllegalStateException("no playable stream")

    @Test
    fun firstTierWinnerSkipsLaterTiers() = runBlocking {
        var laterCalls = 0
        val primary: suspend () -> String? = { "primary" }
        val backup: suspend () -> String? = { laterCalls++; "backup" }

        val result = AudioStreamResolver.resolve(listOf(listOf(primary), listOf(backup)), 1_000L)

        assertEquals("primary", stream(result))
        assertEquals(0, laterCalls)
    }

    @Test
    fun rejectedOrMissingCandidateDoesNotPreventALaterWinner() = runBlocking {
        var calls = 0
        val rejected: suspend () -> String? = { calls++; null }
        val backup: suspend () -> String? = { "verified backup" }

        val result = AudioStreamResolver.resolve(listOf(listOf(rejected), listOf(backup)), 0L)

        assertEquals("verified backup", stream(result))
        assertEquals(1, calls)
    }

    @Test
    fun ambiguousAndThrowingAttemptsRemainInconclusive() = runBlocking {
        val ambiguous: suspend () -> String? = { null }
        val failure = IOException("offline")
        val throwing: suspend () -> String? = { throw failure }
        val errors = mutableListOf<Throwable>()

        val result = AudioStreamResolver.resolve(
            listOf(listOf(ambiguous), listOf(throwing)), 0L,
            onAttemptError = errors::add
        )

        assertSame(AudioResolution.Inconclusive, result)
        assertEquals(listOf(failure), errors)
    }

    @Test
    fun emptyLadderIsInconclusiveAndDoesNotRecover() = runBlocking {
        val result = AudioStreamResolver.resolve<String>(
            listOf(emptyList()), 0L, recovery = { fail("no normal attempts"); null }
        )

        assertSame(AudioResolution.Inconclusive, result)
    }

    @Test
    fun backupCanWinBeforeASlowPrimaryAndAllLosingWorkIsJoined() = runBlocking {
        withTimeout(2_000L) {
            val started = CompletableDeferred<Unit>()
            val stopped = CompletableDeferred<Unit>()
            val slow: suspend () -> String? = {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    stopped.complete(Unit)
                }
            }
            val backup: suspend () -> String? = { started.await(); "backup" }

            val result = AudioStreamResolver.resolve(listOf(listOf(slow), listOf(backup)), 0L)

            assertEquals("backup", stream(result))
            assertTrue(stopped.isCompleted)
        }
    }

    @Test
    fun definitiveAbsenceIsDifferentFromAnUnavailableClient() = runBlocking {
        val absent: suspend () -> String? = { throw NoStream() }

        val result = AudioStreamResolver.resolve(
            listOf(listOf(absent), listOf(absent)), 0L,
            isNoPlayableStream = { it is NoStream }
        )

        assertSame(AudioResolution.NoPlayableStream, result)
    }

    @Test
    fun recoveryWaitsForEveryNormalAttemptAndRunsOnlyOnce() = runBlocking {
        val finished = AtomicInteger()
        var recoveryCalls = 0
        val attempts = List(3) {
            listOf<suspend () -> String?>({ yield(); finished.incrementAndGet(); throw NoStream() })
        }

        val result = AudioStreamResolver.resolve(
            attempts, 0L,
            recovery = {
                assertEquals(3, finished.get())
                recoveryCalls++
                "recovered"
            },
            isNoPlayableStream = { it is NoStream }
        )

        assertEquals("recovered", stream(result))
        assertEquals(1, recoveryCalls)
    }

    @Test
    fun normalWinnerPreservesQualityAndSkipsRecovery() = runBlocking {
        val result = AudioStreamResolver.resolve(
            listOf(listOf<suspend () -> String?>({ "original quality" })), 0L,
            recovery = { fail("normal success must preserve quality"); null }
        )

        assertEquals("original quality", stream(result))
    }

    @Test
    fun oneInconclusiveAttemptSuppressesSpeculativeRecovery() = runBlocking {
        val absent: suspend () -> String? = { throw NoStream() }
        for (attempt in listOf<suspend () -> String?>(
            { null }, { throw IOException("offline") }, { throw IllegalStateException("verification failed") }
        )) {
            val result = AudioStreamResolver.resolve(
                listOf(listOf(absent), listOf(attempt)), 0L,
                recovery = { fail("inconclusive failures must not trigger recovery"); null },
                isNoPlayableStream = { it is NoStream }
            )

            assertSame(AudioResolution.Inconclusive, result)
        }
    }

    @Test
    fun recoveryFailureDoesNotStartAnotherRound() = runBlocking {
        var calls = 0
        val error = IOException("recovery offline")
        val errors = mutableListOf<Throwable>()
        val result = AudioStreamResolver.resolve<String>(
            listOf(listOf({ throw NoStream() })), 0L,
            recovery = { calls++; throw error },
            isNoPlayableStream = { it is NoStream },
            onAttemptError = errors::add
        )

        assertSame(AudioResolution.Inconclusive, result)
        assertEquals(1, calls)
        assertSame(error, errors.last())
    }

    @Test
    fun definitiveRecoveryAbsenceIsReportedWithoutRetrying() = runBlocking {
        var calls = 0
        val result = AudioStreamResolver.resolve<String>(
            listOf(listOf({ throw NoStream() })), 0L,
            recovery = { calls++; throw NoStream() },
            isNoPlayableStream = { it is NoStream }
        )

        assertSame(AudioResolution.NoPlayableStream, result)
        assertEquals(1, calls)
    }

    @Test
    fun parentCancellationStopsAttemptsAndDoesNotRecover() = runBlocking {
        withTimeout(2_000L) {
            val started = CompletableDeferred<Unit>()
            val stopped = CompletableDeferred<Unit>()
            val pending: suspend () -> String? = {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    stopped.complete(Unit)
                }
            }
            val job = launch {
                AudioStreamResolver.resolve(
                    listOf(listOf(pending), listOf(pending)), 0L,
                    recovery = { fail("cancelled request must not recover"); null }
                )
            }
            started.await()
            job.cancelAndJoin()

            assertTrue(stopped.isCompleted)
        }
    }

    @Test
    fun cancellingOneResolutionDoesNotPoisonAnotherCaller() = runBlocking {
        withTimeout(2_000L) {
            val firstStarted = CompletableDeferred<Unit>()
            val secondStarted = CompletableDeferred<Unit>()
            val releaseSecond = CompletableDeferred<Unit>()
            val secondResult = CompletableDeferred<AudioResolution<String>>()
            val first = launch {
                AudioStreamResolver.resolve<String>(
                    listOf(listOf({ firstStarted.complete(Unit); awaitCancellation() })), 0L
                )
            }
            val second = launch {
                secondResult.complete(AudioStreamResolver.resolve(
                    listOf(listOf<suspend () -> String?>({
                        secondStarted.complete(Unit)
                        releaseSecond.await()
                        "second"
                    })), 0L
                ))
            }
            firstStarted.await()
            secondStarted.await()
            first.cancelAndJoin()
            assertTrue(second.isActive)
            releaseSecond.complete(Unit)

            assertEquals("second", stream(secondResult.await()))
            second.join()
        }
    }

    @Test
    fun childAndRecoveryCancellationPropagateInsteadOfBecomingAbsence() = runBlocking {
        for (cancelRecovery in listOf(false, true)) {
            val cancellation = CancellationException("cancelled")
            val normal: suspend () -> String? = { if (cancelRecovery) throw NoStream() else throw cancellation }
            try {
                AudioStreamResolver.resolve(
                    listOf(listOf(normal), listOf({ throw NoStream() })), 0L,
                    recovery = { throw cancellation },
                    isNoPlayableStream = { it is NoStream }
                )
                fail("expected cancellation")
            } catch (actual: CancellationException) {
                assertTrue(generateSequence<Throwable>(actual) { it.cause }.any { it === cancellation })
            }
        }
    }

    @Test
    fun cancellationDuringANonSuspendingAttemptCannotPublishItsResult() = runBlocking {
        val rejected = CompletableDeferred<Unit>()
        val job = launch {
            try {
                AudioStreamResolver.resolve(
                    listOf(listOf<suspend () -> String?>({ currentCoroutineContext().cancel(); "stale" })), 0L
                )
                fail("cancelled result must not be published")
            } catch (_: CancellationException) {
                rejected.complete(Unit)
            }
        }
        job.join()

        assertTrue(rejected.isCompleted)
    }

    @Test
    fun cancelledConcurrentClientCannotPublishItsResult() = runBlocking {
        withTimeout(2_000L) {
            val stale: suspend () -> String? = { currentCoroutineContext().cancel(); "stale" }
            val pending: suspend () -> String? = { awaitCancellation() }
            try {
                AudioStreamResolver.resolve(
                    listOf(listOf(stale, pending)), 0L,
                    recovery = { fail("cancelled hedge must not recover"); null }
                )
                fail("cancelled concurrent client must not publish its result")
            } catch (actual: CancellationException) {
                assertFalse(actual is kotlinx.coroutines.TimeoutCancellationException)
            }
        }
    }

    @Test
    fun cancelledConcurrentFailureCannotTriggerRecovery() = runBlocking {
        withTimeout(2_000L) {
            val cancelled: suspend () -> String? = { currentCoroutineContext().cancel(); throw NoStream() }
            val absent: suspend () -> String? = { throw NoStream() }
            try {
                AudioStreamResolver.resolve(
                    listOf(listOf(cancelled, absent)), 0L,
                    recovery = { fail("cancelled failure must not recover"); null },
                    isNoPlayableStream = { it is NoStream }
                )
                fail("expected cancellation")
            } catch (actual: CancellationException) {
                assertFalse(actual is kotlinx.coroutines.TimeoutCancellationException)
            }
        }
    }

    @Test
    fun cancellationFromErrorCallbackSettlesTheHedge() = runBlocking {
        withTimeout(2_000L) {
            var cancelAttempt: () -> Unit = {}
            val failing: suspend () -> String? = {
                val context = currentCoroutineContext()
                cancelAttempt = { context.cancel() }
                throw NoStream()
            }
            val pending: suspend () -> String? = { awaitCancellation() }
            try {
                AudioStreamResolver.resolve(
                    listOf(listOf(failing, pending)), 0L,
                    isNoPlayableStream = { it is NoStream },
                    onAttemptError = { cancelAttempt() }
                )
                fail("expected cancellation")
            } catch (actual: CancellationException) {
                assertFalse(actual is kotlinx.coroutines.TimeoutCancellationException)
            }
        }
    }

    @Test
    fun cancelledRecoveryFailureCannotPublishDefinitiveAbsence() = runBlocking {
        val rejected = CompletableDeferred<Unit>()
        launch {
            try {
                AudioStreamResolver.resolve<String>(
                    listOf(listOf({ throw NoStream() })), 0L,
                    recovery = { currentCoroutineContext().cancel(); throw NoStream() },
                    isNoPlayableStream = { it is NoStream }
                )
                fail("cancelled recovery must not publish absence")
            } catch (_: CancellationException) {
                rejected.complete(Unit)
            }
        }.join()

        assertTrue(rejected.isCompleted)
    }

    @Test
    fun oversizedPlansAreRejectedBeforeAnyAttemptStarts() = runBlocking {
        var calls = 0
        val attempt: suspend () -> String? = { calls++; "unexpected" }
        for (plan in listOf(
            listOf(List(AudioStreamResolver.MAX_NORMAL_ATTEMPTS + 1) { attempt }),
            List(AudioStreamResolver.MAX_NORMAL_ATTEMPTS + 1) { emptyList<suspend () -> String?>() }
        )) {
            try {
                AudioStreamResolver.resolve(plan, 0L)
                fail("expected bounded plan rejection")
            } catch (_: IllegalArgumentException) {
                assertEquals(0, calls)
            }
        }
    }

    @Test
    fun maximumPlanUsesEachAttemptOnceAndAtMostOneRecovery() = runBlocking {
        var normalCalls = 0
        var recoveryCalls = 0
        val result = AudioStreamResolver.resolve<String>(
            listOf(List(AudioStreamResolver.MAX_NORMAL_ATTEMPTS) {
                suspend { normalCalls++; throw NoStream() }
            }), 0L,
            recovery = { recoveryCalls++; null },
            isNoPlayableStream = { it is NoStream }
        )

        assertSame(AudioResolution.Inconclusive, result)
        assertEquals(AudioStreamResolver.MAX_NORMAL_ATTEMPTS, normalCalls)
        assertEquals(1, recoveryCalls)
    }

    @Test
    fun qualityGraceWaitsForABetterStreamAlreadyInFlight() = runBlocking {
        withTimeout(2_000L) {
            val fast: suspend () -> String? = { "low" }
            val better: suspend () -> String? = { delay(20L); "high" }

            val result = AudioStreamResolver.resolve(
                listOf(listOf(fast), listOf(better)), 0L,
                quality = quality(graceMs = 10_000L)
            )

            assertEquals("high", stream(result))
        }
    }

    @Test
    fun qualityGraceExpiresWithTheBestAvailableAndJoinsLosers() = runBlocking {
        withTimeout(2_000L) {
            val stopped = CompletableDeferred<Unit>()
            val pending: suspend () -> String? = {
                try {
                    awaitCancellation()
                } finally {
                    stopped.complete(Unit)
                }
            }

            val result = AudioStreamResolver.resolve(
                listOf(listOf<suspend () -> String?>({ "low" }), listOf(pending)), 0L,
                quality = quality(graceMs = 30L)
            )

            assertEquals("low", stream(result))
            assertTrue(stopped.isCompleted)
        }
    }

    @Test
    fun sufficientFirstStreamSkipsTheGrace() = runBlocking {
        withTimeout(2_000L) {
            val result = AudioStreamResolver.resolve(
                listOf(listOf<suspend () -> String?>({ "high" }), listOf({ awaitCancellation() })), 0L,
                quality = quality(graceMs = 60_000L)
            )

            assertEquals("high", stream(result))
        }
    }

    @Test
    fun qualityGraceDoesNotDispatchLaterTiers() = runBlocking {
        var laterCalls = 0
        val result = AudioStreamResolver.resolve(
            listOf(listOf<suspend () -> String?>({ "low" }), listOf({ laterCalls++; "high" })), 1_000L,
            quality = quality(graceMs = 30L)
        )

        assertEquals("low", stream(result))
        assertEquals(0, laterCalls)
    }

    @Test
    fun graceEndsWhenEveryStartedAttemptHasSettled() = runBlocking {
        val started = System.nanoTime()
        val result = withTimeout(5_000L) {
            AudioStreamResolver.resolve(
                listOf(listOf<suspend () -> String?>({ "low" }), listOf({ "never started" })), 1_000L,
                quality = quality(graceMs = 60_000L)
            )
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L

        assertEquals("low", stream(result))
        assertTrue("grace waited for an attempt that never started: $elapsedMs ms", elapsedMs < 2_000L)
    }

    @Test
    fun failureDuringGraceKeepsTheStreamAlreadyFound() = runBlocking {
        withTimeout(2_000L) {
            val result = AudioStreamResolver.resolve(
                listOf(listOf<suspend () -> String?>({ "low" }), listOf({ delay(10L); throw NoStream() })), 0L,
                isNoPlayableStream = { it is NoStream },
                quality = quality(graceMs = 60_000L)
            )

            assertEquals("low", stream(result))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeQualityGraceIsRejected() {
        quality(graceMs = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeHedgeBudgetIsRejected() = runBlocking {
        AudioStreamResolver.resolve<String>(emptyList(), -1L)
        Unit
    }

    @Test
    fun resultDiagnosticsDoNotExposeTheStreamPayload() {
        val result = AudioResolution.Resolved("synthetic-private-media-location")

        assertFalse(result.toString().contains("synthetic-private-media-location"))
    }

    private fun quality(graceMs: Long) = StreamQuality<String>(
        rank = { if (it == "high") 2 else 1 },
        sufficientRank = 2,
        graceMs = graceMs
    )

    private fun stream(result: AudioResolution<String>): String =
        (result as AudioResolution.Resolved).stream
}
