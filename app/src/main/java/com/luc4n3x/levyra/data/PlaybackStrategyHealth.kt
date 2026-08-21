package com.luc4n3x.levyra.data

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal enum class PlaybackStrategyCircuit { CLOSED, HALF_OPEN, OPEN }

internal data class PlaybackStrategyStats(
    val successes: Int = 0,
    val failures: Int = 0,
    val consecutiveFailures: Int = 0,
    val averageLatencyMs: Long = Long.MAX_VALUE,
    val quarantineUntilMs: Long = 0L,
    val lastFailureKind: PlaybackFailureKind? = null,
    val lastFailureAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    val score: Double
        get() {
            val total = successes + failures
            if (total <= 0) return NEUTRAL_SCORE
            val successRate = successes.toDouble() / total.toDouble()
            val latencyPenalty = if (averageLatencyMs == Long.MAX_VALUE) {
                0.0
            } else {
                (averageLatencyMs.toDouble() / MAX_LATENCY_FOR_SCORE_MS.toDouble())
                    .coerceIn(0.0, 1.0) * LATENCY_PENALTY_WEIGHT
            }
            val consecutivePenalty =
                consecutiveFailures.coerceIn(0, MAX_CONSECUTIVE_FOR_SCORE).toDouble() * CONSECUTIVE_PENALTY_WEIGHT
            return (successRate * SUCCESS_WEIGHT - latencyPenalty - consecutivePenalty).coerceIn(0.0, 100.0)
        }

    fun circuitAt(nowMs: Long): PlaybackStrategyCircuit = when {
        quarantineUntilMs <= 0L -> PlaybackStrategyCircuit.CLOSED
        nowMs < quarantineUntilMs -> PlaybackStrategyCircuit.OPEN
        else -> PlaybackStrategyCircuit.HALF_OPEN
    }

    private companion object {
        const val NEUTRAL_SCORE = 50.0
        const val SUCCESS_WEIGHT = 100.0
        const val LATENCY_PENALTY_WEIGHT = 20.0
        const val MAX_LATENCY_FOR_SCORE_MS = 8_000L
        const val CONSECUTIVE_PENALTY_WEIGHT = 8.0
        const val MAX_CONSECUTIVE_FOR_SCORE = 5
    }
}

private const val SCORE_GAP_THRESHOLD = 25.0
private const val HARD_FAILURE_STREAK_THRESHOLD = 3
private const val BASE_QUARANTINE_MS = 5L * 60L * 1_000L
private const val MAX_QUARANTINE_MS = 30L * 60L * 1_000L

private val hardPlaybackFailureKinds = setOf(
    PlaybackFailureKind.Forbidden,
    PlaybackFailureKind.Gone,
    PlaybackFailureKind.RateLimited,
    PlaybackFailureKind.LoginRequired,
    PlaybackFailureKind.Signature
)

private data class RankedPlaybackStrategy<T>(
    val index: Int,
    val strategy: T,
    val circuit: PlaybackStrategyCircuit,
    val score: Double
)

internal fun <T : Enum<T>> orderPlaybackStrategiesByHealth(
    strategies: List<T>,
    statsFor: (String) -> PlaybackStrategyStats?,
    nowMs: Long
): List<T> {
    if (strategies.size <= 1) return strategies
    if (statsFor(strategies.first().name) == null) return strategies
    val hasHealthData = strategies.any { statsFor(it.name) != null }
    if (!hasHealthData) return strategies

    val ranked = strategies.mapIndexed { index, strategy ->
        val stats = statsFor(strategy.name) ?: PlaybackStrategyStats()
        RankedPlaybackStrategy(index, strategy, stats.circuitAt(nowMs), stats.score)
    }

    val comparator = Comparator<RankedPlaybackStrategy<T>> { a, b ->
        val circuitCompare = a.circuit.ordinal.compareTo(b.circuit.ordinal)
        if (circuitCompare != 0) return@Comparator circuitCompare
        val healthierGap = a.score - b.score
        when {
            healthierGap >= SCORE_GAP_THRESHOLD -> -1
            -healthierGap >= SCORE_GAP_THRESHOLD -> 1
            else -> a.index.compareTo(b.index)
        }
    }

    return ranked.sortedWith(comparator).map { it.strategy }
}

internal fun playbackStrategyStatsAfterSuccess(
    current: PlaybackStrategyStats?,
    nowMs: Long,
    latencyMs: Long
): PlaybackStrategyStats {
    val base = current ?: PlaybackStrategyStats()
    val open = base.circuitAt(nowMs) == PlaybackStrategyCircuit.OPEN
    return base.copy(
        successes = base.successes + 1,
        consecutiveFailures = if (open) base.consecutiveFailures else 0,
        averageLatencyMs = blendPlaybackLatency(base.averageLatencyMs, latencyMs),
        quarantineUntilMs = if (open) base.quarantineUntilMs else 0L,
        updatedAtMs = nowMs
    )
}

internal fun playbackStrategyStatsAfterFailure(
    current: PlaybackStrategyStats?,
    nowMs: Long,
    latencyMs: Long?,
    kind: PlaybackFailureKind
): PlaybackStrategyStats {
    val base = current ?: PlaybackStrategyStats()
    val consecutiveFailures = base.consecutiveFailures + 1
    val blendedLatency = if (latencyMs != null) {
        blendPlaybackLatency(base.averageLatencyMs, latencyMs)
    } else {
        base.averageLatencyMs
    }
    val quarantineUntilMs = if (
        consecutiveFailures >= HARD_FAILURE_STREAK_THRESHOLD && kind in hardPlaybackFailureKinds
    ) {
        nowMs + playbackQuarantineCooldownMs(consecutiveFailures)
    } else {
        base.quarantineUntilMs
    }
    return base.copy(
        failures = base.failures + 1,
        consecutiveFailures = consecutiveFailures,
        averageLatencyMs = blendedLatency,
        quarantineUntilMs = quarantineUntilMs,
        lastFailureKind = kind,
        lastFailureAtMs = nowMs,
        updatedAtMs = nowMs
    )
}

internal fun playbackStrategyStatsAfterRuntimeFailure(
    current: PlaybackStrategyStats?,
    nowMs: Long,
    kind: PlaybackFailureKind
): PlaybackStrategyStats {
    val updated = playbackStrategyStatsAfterFailure(current, nowMs, latencyMs = null, kind = kind)
    if (kind !in hardPlaybackFailureKinds) return updated
    val forcedStreak = maxOf(updated.consecutiveFailures, HARD_FAILURE_STREAK_THRESHOLD)
    return updated.copy(
        consecutiveFailures = forcedStreak,
        quarantineUntilMs = maxOf(
            updated.quarantineUntilMs,
            nowMs + playbackQuarantineCooldownMs(forcedStreak)
        )
    )
}

private fun playbackQuarantineCooldownMs(consecutiveFailures: Int): Long {
    val overflow = (consecutiveFailures - HARD_FAILURE_STREAK_THRESHOLD).coerceIn(0, 6)
    val cooldown = BASE_QUARANTINE_MS * (1L shl overflow)
    return cooldown.coerceAtMost(MAX_QUARANTINE_MS)
}

private fun blendPlaybackLatency(previousMs: Long, sampleMs: Long): Long {
    val boundedSample = sampleMs.coerceAtLeast(1L)
    if (previousMs == Long.MAX_VALUE) return boundedSample
    return ((previousMs * 3) + boundedSample) / 4
}

internal fun parsePlaybackStrategyHealthSnapshot(raw: String?): Map<String, PlaybackStrategyStats> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        val root = JSONObject(raw)
        val result = LinkedHashMap<String, PlaybackStrategyStats>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = root.optJSONObject(key) ?: continue
            result[key] = PlaybackStrategyStats(
                successes = entry.optInt("successes", 0),
                failures = entry.optInt("failures", 0),
                consecutiveFailures = entry.optInt("consecutiveFailures", 0),
                averageLatencyMs = entry.optLong("averageLatencyMs", Long.MAX_VALUE),
                quarantineUntilMs = entry.optLong("quarantineUntilMs", 0L),
                lastFailureKind = entry.optString("lastFailureKind", "").let { name ->
                    if (name.isBlank()) null else runCatching { PlaybackFailureKind.valueOf(name) }.getOrNull()
                },
                lastFailureAtMs = entry.optLong("lastFailureAtMs", 0L),
                updatedAtMs = entry.optLong("updatedAtMs", 0L)
            )
        }
        result as Map<String, PlaybackStrategyStats>
    }.getOrDefault(emptyMap())
}

internal fun playbackStrategyStatsToJson(stats: PlaybackStrategyStats): JSONObject = JSONObject()
    .put("successes", stats.successes)
    .put("failures", stats.failures)
    .put("consecutiveFailures", stats.consecutiveFailures)
    .put("averageLatencyMs", stats.averageLatencyMs)
    .put("quarantineUntilMs", stats.quarantineUntilMs)
    .put("lastFailureKind", stats.lastFailureKind?.name.orEmpty())
    .put("lastFailureAtMs", stats.lastFailureAtMs)
    .put("updatedAtMs", stats.updatedAtMs)

internal class PlaybackStrategyHealthStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val stats = ConcurrentHashMap<String, PlaybackStrategyStats>()
    private val persistenceScheduled = AtomicBoolean(false)
    private val persistenceExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "levyra-playback-strategy-health").apply { isDaemon = true }
    }

    init {
        stats.putAll(parsePlaybackStrategyHealthSnapshot(prefs.getString(KEY_HEALTH, null)))
    }

    fun <T : Enum<T>> order(mode: String, strategies: List<T>, nowMs: Long = System.currentTimeMillis()): List<T> {
        return orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { strategyName -> stats[keyFor(mode, strategyName)] },
            nowMs = nowMs
        )
    }

    fun recordSuccess(mode: String, strategy: String, latencyMs: Long) {
        val key = keyFor(mode, strategy)
        stats[key] = playbackStrategyStatsAfterSuccess(stats[key], System.currentTimeMillis(), latencyMs)
        trimToCapacity()
        schedulePersist()
    }

    fun recordFailure(mode: String, strategy: String, latencyMs: Long?, kind: PlaybackFailureKind) {
        val key = keyFor(mode, strategy)
        stats[key] = playbackStrategyStatsAfterFailure(stats[key], System.currentTimeMillis(), latencyMs, kind)
        trimToCapacity()
        schedulePersist()
    }

    fun recordRuntimeFailure(mode: String, strategy: String, kind: PlaybackFailureKind) {
        val key = keyFor(mode, strategy)
        stats[key] = playbackStrategyStatsAfterRuntimeFailure(stats[key], System.currentTimeMillis(), kind)
        trimToCapacity()
        schedulePersist()
    }

    fun snapshot(): Map<String, JSONObject> {
        return stats.mapValues { (_, value) -> playbackStrategyStatsToJson(value) }
    }

    private fun trimToCapacity() {
        val overflow = stats.size - MAX_ENTRIES
        if (overflow <= 0) return
        stats.entries
            .sortedBy { it.value.updatedAtMs }
            .take(overflow)
            .forEach { stats.remove(it.key) }
    }

    private fun schedulePersist() {
        if (!persistenceScheduled.compareAndSet(false, true)) return
        persistenceExecutor.schedule(
            {
                persistenceScheduled.set(false)
                persistSnapshot()
            },
            PERSIST_DEBOUNCE_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun persistSnapshot() {
        val root = JSONObject()
        stats.forEach { (key, value) -> root.put(key, playbackStrategyStatsToJson(value)) }
        prefs.edit().putString(KEY_HEALTH, root.toString()).apply()
    }

    private fun keyFor(mode: String, strategy: String): String = "$mode::$strategy"

    private companion object {
        const val PREFS_NAME = "levyra_playback_strategy_health"
        const val KEY_HEALTH = "health"
        const val MAX_ENTRIES = 32
        const val PERSIST_DEBOUNCE_MS = 1_500L
    }
}
