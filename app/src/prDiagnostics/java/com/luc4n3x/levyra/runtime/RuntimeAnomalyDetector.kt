package com.luc4n3x.levyra.runtime

import java.util.ArrayDeque
import kotlinx.serialization.Serializable

@Serializable
internal enum class AnomalyType {
    MEMORY_GROWTH,
    PLAYER_RECREATION_STORM,
    PREPARE_LOOP,
    BUFFERING_LOOP,
    RESOLVER_RETRY_STORM,
    FALLBACK_LOOP,
    NETWORK_RETRY_STORM,
    CACHE_CHURN,
    DSP_RECREATE_STORM,
    CANVAS_RESTART_LOOP,
    HOT_OPERATION_STORM
}

@Serializable
internal enum class AnomalySeverity {
    INFO,
    WARNING,
    CRITICAL
}

@Serializable
internal data class DiagnosticAnomaly(
    val schemaVersion: Int = 1,
    val timestampMs: Long,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val occurrenceCount: Int,
    val windowMs: Long,
    val operation: DiagnosticOperation? = null
)

internal data class AnomalyThresholds(
    val memoryMinimumSamples: Int = 5,
    val memoryMinimumSpanMs: Long = 60_000L,
    val memoryGrowthKb: Long = 65_536L,
    val playerRecreationCount: Int = 4,
    val prepareCount: Int = 12,
    val bufferingCount: Int = 8,
    val resolverRetryCount: Int = 8,
    val fallbackCount: Int = 6,
    val networkRetryCount: Int = 12,
    val cacheEvictionCount: Int = 20,
    val dspRecreateCount: Int = 6,
    val canvasRestartCount: Int = 6,
    val hotOperationCount: Int = 40,
    val shortWindowMs: Long = 60_000L,
    val mediumWindowMs: Long = 120_000L,
    val hotWindowMs: Long = 30_000L,
    val alertCooldownMs: Long = 60_000L
)

private const val MEMORY_SAMPLE_NOISE_TOLERANCE_KB = 4_096L

internal class RuntimeAnomalyDetector(
    private val thresholds: AnomalyThresholds = AnomalyThresholds(),
    private val capacity: Int = ANOMALY_CAPACITY,
    private val historyCapacity: Int = RECORDER_CAPACITY
) {
    private val anomalies = ArrayDeque<DiagnosticAnomaly>(capacity)
    private val history = ArrayDeque<DiagnosticEvent>(historyCapacity)
    private val lastRaisedAt = mutableMapOf<Pair<AnomalyType, DiagnosticOperation?>, Long>()

    init {
        require(capacity > 0)
        require(historyCapacity > 0)
    }

    @Synchronized
    fun observe(event: DiagnosticEvent) {
        while (history.size >= historyCapacity) history.removeFirst()
        history.addLast(event)
        when (event) {
            is MemorySampleEvent -> detectMemoryGrowth(event)
            is PlayerEvent -> detectPlayer(event)
            is ResolverEvent -> detectResolver(event)
            is NetworkEvent -> detectNetwork(event)
            is CacheEvent -> detectCache(event)
            is DspEvent -> detectDsp(event)
            is CanvasEvent -> detectCanvas(event)
            is HotOperationEvent -> detectHotOperation(event)
            is PreflightEvent -> Unit
        }
    }

    @Synchronized
    fun snapshot(): List<DiagnosticAnomaly> = anomalies.toList()

    @Synchronized
    fun clear() {
        anomalies.clear()
        history.clear()
        lastRaisedAt.clear()
    }

    private fun detectMemoryGrowth(event: MemorySampleEvent) {
        val samples = history.filterIsInstance<MemorySampleEvent>()
            .filter { event.timestampMs - it.timestampMs <= thresholds.mediumWindowMs * 2 }
            .takeLast(thresholds.memoryMinimumSamples)
        if (samples.size < thresholds.memoryMinimumSamples) return
        val span = samples.last().timestampMs - samples.first().timestampMs
        val growth = samples.last().pssKb - samples.first().pssKb
        val monotonicWithinNoise = samples.zipWithNext().all { (previous, next) ->
            next.pssKb + MEMORY_SAMPLE_NOISE_TOLERANCE_KB >= previous.pssKb
        }
        if (span >= thresholds.memoryMinimumSpanMs && growth >= thresholds.memoryGrowthKb && monotonicWithinNoise) {
            raise(event.timestampMs, AnomalyType.MEMORY_GROWTH, AnomalySeverity.CRITICAL, samples.size, span)
        }
    }

    private fun detectPlayer(event: PlayerEvent) {
        when {
            event.action == PlayerAction.CREATED -> countAndRaise(
                event.timestampMs,
                thresholds.mediumWindowMs,
                thresholds.playerRecreationCount,
                AnomalyType.PLAYER_RECREATION_STORM,
                AnomalySeverity.CRITICAL
            ) { it is PlayerEvent && it.action == PlayerAction.CREATED }
            event.action == PlayerAction.PREPARE -> countAndRaise(
                event.timestampMs,
                thresholds.shortWindowMs,
                thresholds.prepareCount,
                AnomalyType.PREPARE_LOOP,
                AnomalySeverity.WARNING
            ) { it is PlayerEvent && it.action == PlayerAction.PREPARE }
            event.action == PlayerAction.STATE && event.state == DiagnosticPlayerState.BUFFERING -> countAndRaise(
                event.timestampMs,
                thresholds.mediumWindowMs,
                thresholds.bufferingCount,
                AnomalyType.BUFFERING_LOOP,
                AnomalySeverity.WARNING
            ) {
                it is PlayerEvent && it.action == PlayerAction.STATE &&
                    it.state == DiagnosticPlayerState.BUFFERING
            }
        }
    }

    private fun detectResolver(event: ResolverEvent) {
        if (event.outcome == DiagnosticOutcome.FAILURE || event.outcome == DiagnosticOutcome.TIMEOUT) {
            countAndRaise(
                event.timestampMs,
                thresholds.shortWindowMs,
                thresholds.resolverRetryCount,
                AnomalyType.RESOLVER_RETRY_STORM,
                AnomalySeverity.WARNING
            ) {
                it is ResolverEvent &&
                    (it.outcome == DiagnosticOutcome.FAILURE || it.outcome == DiagnosticOutcome.TIMEOUT)
            }
        }
        if (event.attempt > 1) {
            countAndRaise(
                event.timestampMs,
                thresholds.shortWindowMs,
                thresholds.fallbackCount,
                AnomalyType.FALLBACK_LOOP,
                AnomalySeverity.WARNING
            ) { it is ResolverEvent && it.attempt > 1 }
        }
    }

    private fun detectNetwork(event: NetworkEvent) {
        if (event.retry <= 0 || event.outcome == DiagnosticOutcome.SUCCESS || event.outcome == DiagnosticOutcome.CANCELLED) return
        countAndRaise(
            event.timestampMs,
            thresholds.shortWindowMs,
            thresholds.networkRetryCount,
            AnomalyType.NETWORK_RETRY_STORM,
            AnomalySeverity.WARNING
        ) {
            it is NetworkEvent && it.retry > 0 &&
                (it.outcome == DiagnosticOutcome.FAILURE || it.outcome == DiagnosticOutcome.TIMEOUT)
        }
    }

    private fun detectCache(event: CacheEvent) {
        if (event.action != CacheAction.EVICTION) return
        countAndRaise(
            event.timestampMs,
            thresholds.shortWindowMs,
            thresholds.cacheEvictionCount,
            AnomalyType.CACHE_CHURN,
            AnomalySeverity.WARNING
        ) { it is CacheEvent && it.action == CacheAction.EVICTION }
    }

    private fun detectDsp(event: DspEvent) {
        if (event.action != DspAction.RECREATED) return
        countAndRaise(
            event.timestampMs,
            thresholds.shortWindowMs,
            thresholds.dspRecreateCount,
            AnomalyType.DSP_RECREATE_STORM,
            AnomalySeverity.WARNING
        ) { it is DspEvent && it.action == DspAction.RECREATED }
    }

    private fun detectCanvas(event: CanvasEvent) {
        if (event.action != CanvasAction.RESTARTED && event.action != CanvasAction.FALLBACK) return
        countAndRaise(
            event.timestampMs,
            thresholds.shortWindowMs,
            thresholds.canvasRestartCount,
            AnomalyType.CANVAS_RESTART_LOOP,
            AnomalySeverity.WARNING
        ) {
            it is CanvasEvent && (
                it.action == CanvasAction.RESTARTED || it.action == CanvasAction.FALLBACK
                )
        }
    }

    private fun detectHotOperation(event: HotOperationEvent) {
        val count = history.count {
            it is HotOperationEvent && it.operation == event.operation &&
                event.timestampMs - it.timestampMs <= thresholds.hotWindowMs
        }
        if (count >= thresholds.hotOperationCount) {
            raise(
                event.timestampMs,
                AnomalyType.HOT_OPERATION_STORM,
                AnomalySeverity.INFO,
                count,
                thresholds.hotWindowMs,
                event.operation
            )
        }
    }

    private fun countAndRaise(
        nowMs: Long,
        windowMs: Long,
        threshold: Int,
        type: AnomalyType,
        severity: AnomalySeverity,
        predicate: (DiagnosticEvent) -> Boolean
    ) {
        val count = history.count { predicate(it) && nowMs - it.timestampMs <= windowMs }
        if (count >= threshold) raise(nowMs, type, severity, count, windowMs)
    }

    private fun raise(
        nowMs: Long,
        type: AnomalyType,
        severity: AnomalySeverity,
        count: Int,
        windowMs: Long,
        operation: DiagnosticOperation? = null
    ) {
        val key = type to operation
        val last = lastRaisedAt[key] ?: Long.MIN_VALUE
        if (last != Long.MIN_VALUE && nowMs - last < thresholds.alertCooldownMs) return
        lastRaisedAt[key] = nowMs
        while (anomalies.size >= capacity) anomalies.removeFirst()
        anomalies.addLast(DiagnosticAnomaly(timestampMs = nowMs, type = type, severity = severity, occurrenceCount = count, windowMs = windowMs, operation = operation))
    }
}
