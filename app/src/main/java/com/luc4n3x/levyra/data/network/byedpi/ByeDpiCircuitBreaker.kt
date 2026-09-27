package com.luc4n3x.levyra.data.network.byedpi

import java.util.concurrent.TimeUnit

internal enum class ByeDpiHealthState {
    HEALTHY,
    SUSPECT,
    DEGRADED,
    PROBE
}

internal enum class ByeDpiFailureKind {
    TIMEOUT,
    CONNECTION_RESET,
    HTTP_403,
    HTTP_429,
    HTTP_5XX,
    CONNECTION,
    RESOLUTION
}

internal object ByeDpiHealthPolicy {
    const val FAILURE_THRESHOLD = 2
    const val SLOW_THRESHOLD = 2
    const val RECOVERY_SUCCESS_THRESHOLD = 2
    const val COOLDOWN_MS = 30_000L
    const val PROBE_LEASE_MS = 8_000L
    const val SLOW_CONNECT_MS = 1_500L
    const val SLOW_RESOLUTION_MS = 2_500L
    const val RESOLUTION_BUDGET_MS = 3_500L
    const val RECOVERY_PROBE_BUDGET_MS = 2_800L
}

internal data class ByeDpiHealthSnapshot(
    val state: ByeDpiHealthState,
    val consecutiveSuccesses: Int,
    val consecutiveFailures: Int,
    val consecutiveSlowResponses: Int,
    val timeouts: Int,
    val connectionResets: Int,
    val http403: Int,
    val http429: Int,
    val http5xx: Int,
    val lastConnectLatencyMs: Long?,
    val lastResolutionLatencyMs: Long?,
    val lastSuccessAtMs: Long?,
    val lastFailureAtMs: Long?,
    val nextProbeAtMs: Long
)

internal class ByeDpiCircuitBreaker(
    private val failureThreshold: Int = ByeDpiHealthPolicy.FAILURE_THRESHOLD,
    private val slowThreshold: Int = ByeDpiHealthPolicy.SLOW_THRESHOLD,
    private val recoverySuccessThreshold: Int = ByeDpiHealthPolicy.RECOVERY_SUCCESS_THRESHOLD,
    private val cooldownMs: Long = ByeDpiHealthPolicy.COOLDOWN_MS,
    private val probeLeaseMs: Long = ByeDpiHealthPolicy.PROBE_LEASE_MS,
    private val slowConnectMs: Long = ByeDpiHealthPolicy.SLOW_CONNECT_MS,
    private val slowResolutionMs: Long = ByeDpiHealthPolicy.SLOW_RESOLUTION_MS,
    private val clockMs: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) }
) {
    private val lock = Any()
    private var state = ByeDpiHealthState.HEALTHY
    private var consecutiveSuccesses = 0
    private var consecutiveFailures = 0
    private var consecutiveSlowResponses = 0
    private var timeouts = 0
    private var connectionResets = 0
    private var http403 = 0
    private var http429 = 0
    private var http5xx = 0
    private var lastConnectLatencyMs: Long? = null
    private var lastResolutionLatencyMs: Long? = null
    private var lastSuccessAtMs: Long? = null
    private var lastFailureAtMs: Long? = null
    private var nextProbeAtMs = 0L

    fun isOpen(): Boolean = synchronized(lock) {
        state == ByeDpiHealthState.DEGRADED || state == ByeDpiHealthState.PROBE
    }

    fun isProbeDue(): Boolean = synchronized(lock) {
        state == ByeDpiHealthState.DEGRADED && clockMs() >= nextProbeAtMs
    }

    fun tryAcquire(): Boolean = synchronized(lock) {
        when (state) {
            ByeDpiHealthState.HEALTHY,
            ByeDpiHealthState.SUSPECT -> true

            ByeDpiHealthState.DEGRADED -> {
                val now = clockMs()
                if (now < nextProbeAtMs) {
                    false
                } else {
                    state = ByeDpiHealthState.PROBE
                    nextProbeAtMs = now + probeLeaseMs
                    true
                }
            }

            ByeDpiHealthState.PROBE -> {
                if (clockMs() < nextProbeAtMs) {
                    false
                } else {
                    enterDegradedLocked(clockMs())
                    false
                }
            }
        }
    }

    fun recordSuccess(connectLatencyMs: Long? = null, resolutionLatencyMs: Long? = null) = synchronized(lock) {
        val now = clockMs()
        connectLatencyMs?.let { lastConnectLatencyMs = it }
        resolutionLatencyMs?.let { lastResolutionLatencyMs = it }
        lastSuccessAtMs = now
        val slow = connectLatencyMs?.let { it >= slowConnectMs } == true ||
            resolutionLatencyMs?.let { it >= slowResolutionMs } == true
        if (slow) {
            consecutiveSuccesses = 0
            consecutiveFailures = 0
            consecutiveSlowResponses++
            if (consecutiveSlowResponses >= slowThreshold) {
                enterDegradedLocked(now)
            } else {
                state = ByeDpiHealthState.SUSPECT
            }
            return@synchronized
        }

        consecutiveFailures = 0
        consecutiveSlowResponses = 0
        if (resolutionLatencyMs == null) return@synchronized

        consecutiveSuccesses++
        state = when (state) {
            ByeDpiHealthState.HEALTHY -> ByeDpiHealthState.HEALTHY
            ByeDpiHealthState.SUSPECT,
            ByeDpiHealthState.PROBE,
            ByeDpiHealthState.DEGRADED -> if (consecutiveSuccesses >= recoverySuccessThreshold) {
                nextProbeAtMs = 0L
                ByeDpiHealthState.HEALTHY
            } else {
                ByeDpiHealthState.SUSPECT
            }
        }
    }

    fun recordFailure(kind: ByeDpiFailureKind = ByeDpiFailureKind.CONNECTION) = synchronized(lock) {
        val now = clockMs()
        lastFailureAtMs = now
        consecutiveSuccesses = 0
        consecutiveSlowResponses = 0
        consecutiveFailures++
        when (kind) {
            ByeDpiFailureKind.TIMEOUT -> timeouts++
            ByeDpiFailureKind.CONNECTION_RESET -> connectionResets++
            ByeDpiFailureKind.HTTP_403 -> http403++
            ByeDpiFailureKind.HTTP_429 -> http429++
            ByeDpiFailureKind.HTTP_5XX -> http5xx++
            ByeDpiFailureKind.CONNECTION,
            ByeDpiFailureKind.RESOLUTION -> Unit
        }
        val immediateDegrade = kind == ByeDpiFailureKind.TIMEOUT ||
            kind == ByeDpiFailureKind.HTTP_403 ||
            kind == ByeDpiFailureKind.HTTP_429
        if (state == ByeDpiHealthState.PROBE || immediateDegrade || consecutiveFailures >= failureThreshold) {
            enterDegradedLocked(now)
        } else {
            state = ByeDpiHealthState.SUSPECT
        }
    }

    fun snapshot(): ByeDpiHealthSnapshot = synchronized(lock) {
        ByeDpiHealthSnapshot(
            state = state,
            consecutiveSuccesses = consecutiveSuccesses,
            consecutiveFailures = consecutiveFailures,
            consecutiveSlowResponses = consecutiveSlowResponses,
            timeouts = timeouts,
            connectionResets = connectionResets,
            http403 = http403,
            http429 = http429,
            http5xx = http5xx,
            lastConnectLatencyMs = lastConnectLatencyMs,
            lastResolutionLatencyMs = lastResolutionLatencyMs,
            lastSuccessAtMs = lastSuccessAtMs,
            lastFailureAtMs = lastFailureAtMs,
            nextProbeAtMs = nextProbeAtMs
        )
    }

    fun reset() = synchronized(lock) {
        state = ByeDpiHealthState.HEALTHY
        consecutiveSuccesses = 0
        consecutiveFailures = 0
        consecutiveSlowResponses = 0
        timeouts = 0
        connectionResets = 0
        http403 = 0
        http429 = 0
        http5xx = 0
        lastConnectLatencyMs = null
        lastResolutionLatencyMs = null
        lastSuccessAtMs = null
        lastFailureAtMs = null
        nextProbeAtMs = 0L
    }

    private fun enterDegradedLocked(now: Long) {
        state = ByeDpiHealthState.DEGRADED
        nextProbeAtMs = now + cooldownMs
    }
}
