package com.luc4n3x.levyra.data

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaybackConcurrencyTest {
    @Test
    fun ordinaryFailureIsReturnedAsResult() = runBlocking {
        val result = runCatchingPreservingCancellation<Int> {
            throw IOException("network")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun callerCancellationCannotBecomeSuccessfulResult() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val worker = async {
            runCatchingPreservingCancellation {
                entered.complete(Unit)
                withContext(NonCancellable) {
                    release.await()
                    42
                }
            }
        }

        entered.await()
        worker.cancel()
        release.complete(Unit)

        try {
            worker.await()
            fail("Cancellation must be propagated")
        } catch (_: kotlinx.coroutines.CancellationException) {
            assertTrue(worker.isCancelled)
        }
    }

    @Test
    fun sameKeyRunsOnlyOneSharedRequest() = runBlocking {
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val singleFlight = PlaybackSingleFlight<String, Int>(ownerScope)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val calls = AtomicInteger(0)

            val first = async {
                singleFlight.run("track") {
                    calls.incrementAndGet()
                    started.complete(Unit)
                    release.await()
                    7
                }
            }
            started.await()
            val second = async {
                singleFlight.run("track") {
                    calls.incrementAndGet()
                    99
                }
            }

            release.complete(Unit)

            assertEquals(7, first.await())
            assertEquals(7, second.await())
            assertEquals(1, calls.get())
            assertEquals(0, singleFlight.activeCount())
        } finally {
            ownerScope.cancel()
        }
    }

    @Test
    fun cancellingFirstWaiterDoesNotCancelSharedRequest() = runBlocking {
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val singleFlight = PlaybackSingleFlight<String, Int>(ownerScope)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val calls = AtomicInteger(0)

            val first = async {
                singleFlight.run("track") {
                    calls.incrementAndGet()
                    started.complete(Unit)
                    release.await()
                    21
                }
            }
            started.await()
            val second = async {
                singleFlight.run("track") {
                    calls.incrementAndGet()
                    84
                }
            }

            first.cancel()
            release.complete(Unit)

            try {
                first.await()
                fail("First waiter must stay cancelled")
            } catch (_: kotlinx.coroutines.CancellationException) {
                assertTrue(first.isCancelled)
            }
            assertEquals(21, second.await())
            assertEquals(1, calls.get())
            assertEquals(0, singleFlight.activeCount())
        } finally {
            ownerScope.cancel()
        }
    }

    @Test
    fun failedSharedRequestIsRemovedAndCanRetry() = runBlocking {
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val singleFlight = PlaybackSingleFlight<String, Int>(ownerScope)
            val calls = AtomicInteger(0)

            try {
                singleFlight.run("track") {
                    calls.incrementAndGet()
                    throw IOException("first attempt")
                }
                fail("First request should fail")
            } catch (_: IOException) {
                // Expected.
            }

            assertEquals(0, singleFlight.activeCount())
            val retried = singleFlight.run("track") {
                calls.incrementAndGet()
                12
            }

            assertEquals(12, retried)
            assertEquals(2, calls.get())
            assertEquals(0, singleFlight.activeCount())
        } finally {
            ownerScope.cancel()
        }
    }

    @Test
    fun differentKeysDoNotBlockEachOther() = runBlocking {
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val singleFlight = PlaybackSingleFlight<String, Int>(ownerScope)
            val firstStarted = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()

            val first = async {
                singleFlight.run("first") {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    1
                }
            }
            firstStarted.await()
            val second = async {
                singleFlight.run("second") { 2 }
            }

            assertEquals(2, second.await())
            assertFalse(first.isCompleted)
            releaseFirst.complete(Unit)
            assertEquals(1, first.await())
        } finally {
            ownerScope.cancel()
        }
    }
}
