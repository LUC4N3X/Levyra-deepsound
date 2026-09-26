package com.luc4n3x.levyra.data.network.byedpi

import java.util.concurrent.TimeUnit

internal class ByeDpiCircuitBreaker(
    private val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val probeLeaseMs: Long = DEFAULT_PROBE_LEASE_MS,
    private val clockMs: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) }
) {
    private val lock = Any()
    private var consecutiveFailures = 0
    private var nextAttemptAtMs = 0L

    fun isOpen(): Boolean = synchronized(lock) { consecutiveFailures >= failureThreshold }

    fun tryAcquire(): Boolean = synchronized(lock) {
        if (consecutiveFailures < failureThreshold) return true
        val now = clockMs()
        if (now < nextAttemptAtMs) return false
        nextAttemptAtMs = now + probeLeaseMs
        true
    }

    fun recordSuccess() = synchronized(lock) {
        consecutiveFailures = 0
        nextAttemptAtMs = 0L
    }

    fun recordFailure() = synchronized(lock) {
        consecutiveFailures++
        if (consecutiveFailures >= failureThreshold) {
            nextAttemptAtMs = clockMs() + cooldownMs
        }
    }

    private companion object {
        const val DEFAULT_FAILURE_THRESHOLD = 3
        const val DEFAULT_COOLDOWN_MS = 30_000L
        const val DEFAULT_PROBE_LEASE_MS = 8_000L
    }
}
