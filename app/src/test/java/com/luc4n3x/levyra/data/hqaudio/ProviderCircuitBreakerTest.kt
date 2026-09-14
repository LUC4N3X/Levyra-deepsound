package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker.Permit
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker.State
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderCircuitBreakerTest {
    private var clockMs = 1_000_000L
    private val breaker = ProviderCircuitBreaker("jiosaavn", { clockMs })

    private fun open() {
        repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD) { breaker.onFailure(breaker.acquire()) }
    }

    @Test
    fun opensAfterConsecutiveFailuresAndRejectsWhileOpen() {
        repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD - 1) { breaker.onFailure(breaker.acquire()) }
        assertEquals(State.CLOSED, breaker.currentState)
        breaker.onFailure(breaker.acquire())
        assertEquals(State.OPEN, breaker.currentState)
        assertEquals(Permit.REJECTED, breaker.acquire())
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS - 1
        assertEquals(Permit.REJECTED, breaker.acquire())
    }

    @Test
    fun successResetsTheConsecutiveFailureCount() {
        repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD - 1) { breaker.onFailure(breaker.acquire()) }
        breaker.onSuccess()
        repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD - 1) { breaker.onFailure(breaker.acquire()) }
        assertEquals(State.CLOSED, breaker.currentState)
    }

    @Test
    fun halfOpenAdmitsASingleConcurrentProbe() {
        open()
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        val callers = 16
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(callers)
        val permits = (1..callers).map { pool.submit<Permit> { start.await(); breaker.acquire() } }
        start.countDown()
        val granted = permits.map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdownNow()
        assertEquals(1, granted.count { it == Permit.PROBE })
        assertEquals(callers - 1, granted.count { it == Permit.REJECTED })
        assertEquals(State.HALF_OPEN, breaker.currentState)
    }

    @Test
    fun successfulProbeClosesTheCircuit() {
        open()
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        assertEquals(Permit.PROBE, breaker.acquire())
        breaker.onSuccess()
        assertEquals(State.CLOSED, breaker.currentState)
        assertEquals(Permit.NORMAL, breaker.acquire())
    }

    @Test
    fun failedProbeReopensWithABoundedLongerWindow() {
        open()
        var window = ProviderCircuitBreaker.DEFAULT_OPEN_MS
        repeat(8) {
            clockMs += window
            val probe = breaker.acquire()
            assertEquals(Permit.PROBE, probe)
            breaker.onFailure(probe)
            window = (window * 2).coerceAtMost(ProviderCircuitBreaker.MAX_OPEN_MS)
            clockMs += window - 1
            assertEquals(Permit.REJECTED, breaker.acquire())
            clockMs -= window - 1
        }
        assertEquals(ProviderCircuitBreaker.MAX_OPEN_MS, window)
    }

    @Test
    fun abandonedProbeLetsTheNextCallerProbe() {
        open()
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        val probe = breaker.acquire()
        assertEquals(Permit.REJECTED, breaker.acquire())
        breaker.release(probe)
        assertEquals(Permit.PROBE, breaker.acquire())
    }

    @Test
    fun lateFailuresDoNotExtendAnOpenCircuit() {
        val stale = breaker.acquire()
        open()
        breaker.onFailure(stale)
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        assertEquals(Permit.PROBE, breaker.acquire())
    }
}
