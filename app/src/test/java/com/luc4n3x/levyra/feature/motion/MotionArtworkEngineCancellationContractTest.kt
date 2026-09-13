package com.luc4n3x.levyra.feature.motion

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkEngineCancellationContractTest {
    @Test
    fun networkPolicyCancellationStopsPendingProviderJobs() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val lookup = launch {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }

        started.await()
        cancelMotionArtworkLookups(listOf(lookup))

        withTimeout(1_000L) { cancelled.await() }
        lookup.join()
        assertTrue(lookup.isCancelled)
    }
}
