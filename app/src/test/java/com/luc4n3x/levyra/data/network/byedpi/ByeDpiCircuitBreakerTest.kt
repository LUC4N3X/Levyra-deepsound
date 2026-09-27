package com.luc4n3x.levyra.data.network.byedpi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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
            slowThreshold = 2,
            recoverySuccessThreshold = 2,
            cooldownMs = 30_000L,
            probeLeaseMs = 8_000L,
            slowConnectMs = 1_500L,
            slowResolutionMs = 2_500L,
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
    fun allowsSingleProbeAfterCooldownThenRequiresAnotherCooldownWhenLeaseExpires() {
        repeat(3) { breaker.recordFailure() }
        nowMs += 30_000L

        assertTrue(breaker.tryAcquire())
        assertFalse(breaker.tryAcquire())
        nowMs += 7_999L
        assertFalse(breaker.tryAcquire())
        nowMs += 1L
        assertFalse(breaker.tryAcquire())
        nowMs += 30_000L
        assertTrue(breaker.tryAcquire())
    }

    @Test
    fun probeNeedsTwoFastSuccessesBeforeReturningHealthy() {
        repeat(3) { breaker.recordFailure() }
        nowMs += 30_000L
        assertTrue(breaker.tryAcquire())

        breaker.recordSuccess(resolutionLatencyMs = 200L)

        assertFalse(breaker.isOpen())
        assertEquals(ByeDpiHealthState.SUSPECT, breaker.snapshot().state)
        assertTrue(breaker.tryAcquire())
        breaker.recordSuccess(resolutionLatencyMs = 200L)

        assertEquals(ByeDpiHealthState.HEALTHY, breaker.snapshot().state)
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
        breaker.recordSuccess(resolutionLatencyMs = 200L)
        breaker.recordFailure()
        breaker.recordFailure()

        assertFalse(breaker.isOpen())
        breaker.recordFailure()
        assertTrue(breaker.isOpen())
    }

    @Test
    fun slowTechnicalSuccessBecomesSuspectThenDegraded() {
        breaker.recordSuccess(resolutionLatencyMs = 2_500L)

        assertEquals(ByeDpiHealthState.SUSPECT, breaker.snapshot().state)
        assertFalse(breaker.isOpen())

        breaker.recordSuccess(connectLatencyMs = 1_800L)

        assertEquals(ByeDpiHealthState.DEGRADED, breaker.snapshot().state)
        assertTrue(breaker.isOpen())
        assertEquals(2, breaker.snapshot().consecutiveSlowResponses)
    }

    @Test
    fun timeoutImmediatelyEntersDegradedAndIsCounted() {
        breaker.recordFailure(ByeDpiFailureKind.TIMEOUT)

        assertEquals(ByeDpiHealthState.DEGRADED, breaker.snapshot().state)
        assertEquals(1, breaker.snapshot().timeouts)
        assertFalse(breaker.tryAcquire())
    }

    @Test
    fun connectionResetAndHttpFailuresAreRecorded() {
        breaker.recordFailure(ByeDpiFailureKind.CONNECTION_RESET)
        breaker.recordFailure(ByeDpiFailureKind.HTTP_5XX)

        val snapshot = breaker.snapshot()
        assertEquals(1, snapshot.connectionResets)
        assertEquals(1, snapshot.http5xx)
        assertFalse(breaker.isOpen())

        breaker.recordFailure(ByeDpiFailureKind.CONNECTION)

        assertTrue(breaker.isOpen())
    }

    @Test
    fun oneFastSuccessDoesNotOscillateBackToHealthy() {
        breaker.recordFailure(ByeDpiFailureKind.TIMEOUT)
        nowMs += 30_000L
        assertTrue(breaker.tryAcquire())

        breaker.recordSuccess(resolutionLatencyMs = 200L)

        assertEquals(ByeDpiHealthState.SUSPECT, breaker.snapshot().state)
        breaker.recordFailure(ByeDpiFailureKind.CONNECTION)
        assertEquals(ByeDpiHealthState.SUSPECT, breaker.snapshot().state)
    }
}
