package com.luc4n3x.levyra.viewmodel

import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CancellationSafeRunCatchingTest {
    @Test
    fun cancellationIsRethrown() {
        try {
            runCatching<Int> { throw CancellationException("cancelled") }
            fail("CancellationException must be propagated")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    @Test
    fun ordinaryFailureRemainsAResult() {
        val result = runCatching<Int> { throw IOException("network") }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
