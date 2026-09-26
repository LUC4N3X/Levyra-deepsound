package com.luc4n3x.levyra.data.network.byedpi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ByeDpiCircuitBreakerTest {
    private var nowMs = 0L
    private lateinit var breaker: ByeDpiCircuitBreaker

    @Before
    fun setUp() {
        nowMs = 1_000L
        breaker = ByeDpiCircuitBreaker(
            failureThreshold = 3,
            cooldownMs = 30_000L,
            probeLeaseMs = 8_000L,
            clockMs = { nowMs }
        )
    }

    @Test
    fun staysClosedBelowThreshold() {
        breaker.recordFailure()
        breaker.recordFailure()

        assertFalse(breaker.isOpen())
        assertTrue(breaker.tryAcquire())
    }

    @Test
    fun opensAfterConsecutiveFailuresAndRejectsDuringCooldown() {
        repeat(3) { breaker.recordFailure() }

        assertTrue(breaker.isOpen())
        assertFalse(breaker.tryAcquire())
        nowMs += 29_999L
        assertFalse(breaker.tryAcquire())
    }

    @Test
    fun allowsSingleProbeAfterCooldownThenRejectsUntilLeaseExpires() {
        repeat(3) { breaker.recordFailure() }
        nowMs += 30_000L

        assertTrue(breaker.tryAcquire())
        assertFalse(breaker.tryAcquire())
        nowMs += 7_999L
        assertFalse(breaker.tryAcquire())
        nowMs += 1L
        assertTrue(breaker.tryAcquire())
    }

    @Test
    fun probeSuccessClosesBreakerWithoutWaitingForAnotherCooldown() {
        repeat(3) { breaker.recordFailure() }
        nowMs += 30_000L
        assertTrue(breaker.tryAcquire())

        breaker.recordSuccess()

        assertFalse(breaker.isOpen())
        assertTrue(breaker.tryAcquire())
        assertTrue(breaker.tryAcquire())
    }

    @Test
    fun probeFailureReopensForFullCooldown() {
        repeat(3) { breaker.recordFailure() }
        nowMs += 30_000L
        assertTrue(breaker.tryAcquire())

        breaker.recordFailure()

        assertTrue(breaker.isOpen())
        nowMs += 29_999L
        assertFalse(breaker.tryAcquire())
        nowMs += 1L
        assertTrue(breaker.tryAcquire())
    }

    @Test
    fun successResetsConsecutiveFailureCount() {
        breaker.recordFailure()
        breaker.recordFailure()
        breaker.recordSuccess()
        breaker.recordFailure()
        breaker.recordFailure()

        assertFalse(breaker.isOpen())
        breaker.recordFailure()
        assertTrue(breaker.isOpen())
    }
}
