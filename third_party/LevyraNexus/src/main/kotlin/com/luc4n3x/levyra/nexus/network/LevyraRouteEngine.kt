package com.luc4n3x.levyra.nexus.network

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class LevyraTransport {
    OKHTTP,
    CRONET,
    SYSTEM
}

enum class LevyraAddressFamily {
    SYSTEM,
    IPV4,
    IPV6
}

enum class LevyraRouteFailure {
    TIMEOUT,
    CONNECTION,
    TLS,
    RATE_LIMIT,
    ACCESS_DENIED,
    NOT_FOUND,
    INVALID_RESPONSE,
    CANCELLED,
    UNKNOWN
}

data class LevyraRoute(
    val id: String,
    val host: String,
    val transport: LevyraTransport = LevyraTransport.SYSTEM,
    val addressFamily: LevyraAddressFamily = LevyraAddressFamily.SYSTEM,
    val priority: Int = 0
) {
    val key: String
        get() = buildString {
            append(host.trim().lowercase(Locale.ROOT))
            append('|')
            append(transport.name)
            append('|')
            append(addressFamily.name)
            append('|')
            append(id.trim())
        }
}

data class LevyraRouteHealth(
    val successes: Int = 0,
    val failures: Int = 0,
    val denials: Int = 0,
    val consecutiveFailures: Int = 0,
    val consecutiveDenials: Int = 0,
    val averageLatencyMs: Long = Long.MAX_VALUE,
    val blockedUntilMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    fun score(nowMs: Long): Double {
        val ageDays = if (updatedAtMs <= 0L) 30.0 else ((nowMs - updatedAtMs).coerceAtLeast(0L) / DAY_MS.toDouble())
        val weightedFailures = failures.toDouble() + denials * 0.25
        val reliability = (successes + 1.0) / (successes + weightedFailures + 2.0)
        val latencyBonus = if (averageLatencyMs == Long.MAX_VALUE) 0.0 else 1_500.0 / averageLatencyMs.coerceAtLeast(50L)
        val recencyBonus = (3.0 - ageDays.coerceAtMost(3.0)) * 0.75
        return reliability * 100.0 + latencyBonus + recencyBonus - consecutiveFailures * 12.0 - consecutiveDenials * 4.0
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}

class LevyraRouteEngine(
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()
    private val health = LinkedHashMap<String, LevyraRouteHealth>()

    fun order(routes: List<LevyraRoute>, limit: Int = routes.size): List<LevyraRoute> {
        if (routes.isEmpty() || limit <= 0) return emptyList()
        val now = clock()
        return synchronized(lock) {
            routes.asSequence()
                .filter { (health[it.key]?.blockedUntilMs ?: 0L) <= now }
                .sortedWith(
                    compareByDescending<LevyraRoute> { health[it.key]?.score(now) ?: DEFAULT_SCORE }
                        .thenBy { it.priority }
                        .thenBy { it.id }
                )
                .take(limit.coerceAtMost(routes.size))
                .toList()
        }
    }

    fun race(routes: List<LevyraRoute>, maxRoutes: Int = 2): List<LevyraRoute> {
        if (maxRoutes <= 0) return emptyList()
        val ordered = order(routes)
        if (ordered.size <= 1) return ordered
        val selected = ArrayList<LevyraRoute>(min(maxRoutes, ordered.size))
        val usedFamilies = LinkedHashSet<LevyraAddressFamily>()
        for (route in ordered) {
            if (route.addressFamily == LevyraAddressFamily.SYSTEM || usedFamilies.add(route.addressFamily)) {
                selected += route
                if (selected.size >= maxRoutes) return selected
            }
        }
        for (route in ordered) {
            if (route !in selected) selected += route
            if (selected.size >= maxRoutes) break
        }
        return selected
    }

    fun recordSuccess(route: LevyraRoute, latencyMs: Long) {
        val now = clock()
        synchronized(lock) {
            val previous = health[route.key] ?: LevyraRouteHealth()
            health[route.key] = previous.copy(
                successes = (previous.successes + 1).coerceAtMost(MAX_SAMPLES),
                consecutiveFailures = 0,
                consecutiveDenials = 0,
                averageLatencyMs = updatedAverage(previous.averageLatencyMs, latencyMs),
                blockedUntilMs = previous.blockedUntilMs.takeIf { it > now } ?: 0L,
                updatedAtMs = now
            )
        }
    }

    fun recordFailure(
        route: LevyraRoute,
        failure: LevyraRouteFailure,
        latencyMs: Long? = null,
        recoveredByFallback: Boolean = false
    ) {
        if (failure == LevyraRouteFailure.CANCELLED) return
        val now = clock()
        synchronized(lock) {
            val previous = health[route.key] ?: LevyraRouteHealth()
            if (failure == LevyraRouteFailure.ACCESS_DENIED || failure == LevyraRouteFailure.NOT_FOUND) {
                val consecutive = if (recoveredByFallback) 0 else (previous.consecutiveDenials + 1).coerceAtMost(MAX_CONSECUTIVE)
                health[route.key] = previous.copy(
                    denials = (previous.denials + 1).coerceAtMost(MAX_SAMPLES),
                    consecutiveDenials = consecutive,
                    averageLatencyMs = updatedAverage(previous.averageLatencyMs, latencyMs),
                    blockedUntilMs = if (consecutive >= DENIAL_BLOCK_THRESHOLD) {
                        max(previous.blockedUntilMs, now + DENIAL_BLOCK_MS)
                    } else {
                        previous.blockedUntilMs
                    },
                    updatedAtMs = now
                )
                return
            }

            val consecutive = (previous.consecutiveFailures + 1).coerceAtMost(MAX_CONSECUTIVE)
            val blockMs = when (failure) {
                LevyraRouteFailure.RATE_LIMIT -> HARD_BLOCK_MS
                LevyraRouteFailure.TLS -> if (consecutive >= 2) LONG_BLOCK_MS else SHORT_BLOCK_MS
                LevyraRouteFailure.TIMEOUT,
                LevyraRouteFailure.CONNECTION -> when {
                    consecutive >= 5 -> LONG_BLOCK_MS
                    consecutive >= 2 -> SHORT_BLOCK_MS
                    else -> 0L
                }
                LevyraRouteFailure.INVALID_RESPONSE,
                LevyraRouteFailure.UNKNOWN -> if (consecutive >= 3) SHORT_BLOCK_MS else 0L
            }
            health[route.key] = previous.copy(
                failures = (previous.failures + 1).coerceAtMost(MAX_SAMPLES),
                consecutiveFailures = consecutive,
                averageLatencyMs = updatedAverage(previous.averageLatencyMs, latencyMs),
                blockedUntilMs = max(previous.blockedUntilMs, now + blockMs),
                updatedAtMs = now
            )
        }
    }

    fun snapshot(): Map<String, LevyraRouteHealth> = synchronized(lock) { health.toMap() }

    fun resetVolatileState() {
        synchronized(lock) {
            health.replaceAll { _, value ->
                value.copy(
                    successes = value.successes / 2,
                    failures = value.failures / 2,
                    denials = value.denials / 2,
                    consecutiveFailures = 0,
                    consecutiveDenials = 0,
                    blockedUntilMs = 0L,
                    averageLatencyMs = Long.MAX_VALUE
                )
            }
        }
    }

    fun clear() {
        synchronized(lock) { health.clear() }
    }

    private fun updatedAverage(previous: Long, latencyMs: Long?): Long = when {
        latencyMs == null || latencyMs <= 0L -> previous
        previous == Long.MAX_VALUE -> latencyMs
        else -> ((previous * 7L) + latencyMs) / 8L
    }

    private companion object {
        const val DEFAULT_SCORE = 50.0
        const val MAX_SAMPLES = 10_000
        const val MAX_CONSECUTIVE = 20
        const val DENIAL_BLOCK_THRESHOLD = 3
        const val DENIAL_BLOCK_MS = 20_000L
        const val SHORT_BLOCK_MS = 25_000L
        const val LONG_BLOCK_MS = 2L * 60L * 1_000L
        const val HARD_BLOCK_MS = 10L * 60L * 1_000L
    }
}
