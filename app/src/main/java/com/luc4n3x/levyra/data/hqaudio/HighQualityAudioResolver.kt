package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

enum class HighQualityFallbackReason {
    DISABLED,
    QUARANTINED,
    BUSY,
    NO_MATCH,
    AMBIGUOUS,
    PROVIDER_UNAVAILABLE,
    STREAM_UNAVAILABLE,
    TIMEOUT,
    NOT_READY,
    QUALITY_NOT_HIGHER
}

sealed interface HighQualityResolution {
    data class Selected(
        val stream: ResolvedHighQualityStream,
        val evaluation: AlternativeMatchEvaluation
    ) : HighQualityResolution

    data class Fallback(
        val reason: HighQualityFallbackReason,
        val detail: String = ""
    ) : HighQualityResolution
}

class HighQualityAudioResolver(
    providers: List<HighQualityAudioProvider>,
    private val mappingStore: HighQualityMappingStore,
    private val scope: CoroutineScope,
    private val matcher: AlternativeTrackMatcher = AlternativeTrackMatcher(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val lookupBudgetMs: Long = LOOKUP_BUDGET_MS,
    private val providerBudgetMs: Long = PROVIDER_BUDGET_MS,
    private val hedgeDelayMs: Long = HEDGE_DELAY_MS,
    private val upgradeGraceMs: Long = UPGRADE_GRACE_MS
) {
    private val lanes = providers
        .distinctBy { it.id }
        .map { HighQualityProviderLane(it, mappingStore, matcher, clock, ::isProviderTrackQuarantined) }
    private val streams = ConcurrentHashMap<String, HighQualityResolution.Selected>()
    private val quarantinedIdentities = ConcurrentHashMap<String, Long>()
    private val quarantinedProviderTracks = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap<String, Deferred<HighQualityResolution>>()
    private val inFlightLock = Any()

    @Volatile
    var mode: HighQualityAudioMode = HighQualityAudioMode.OFF
        set(value) {
            val changed = field != value
            field = value
            if (changed || !value.enabled) streams.clear()
        }

    val preference: HighQualityPreference
        get() = if (mode == HighQualityAudioMode.PREFER_320) HighQualityPreference.MAXIMUM else HighQualityPreference.BALANCED

    fun providerName(providerId: String): String =
        lanes.firstOrNull { it.provider.id == providerId }?.provider?.displayName ?: providerId

    fun providerHealth(): List<ProviderBackendHealth> = lanes.flatMap { it.provider.health() }

    fun cachedSelection(identityKey: String): HighQualityResolution.Selected? {
        if (!mode.enabled) return null
        val cached = streams[identityKey] ?: return null
        val stream = cached.stream
        val usable = stream.isFresh(clock(), STREAM_REFRESH_MARGIN_MS) &&
            !isQuarantined(quarantinedIdentities, providerKey(stream.providerId, identityKey)) &&
            !isProviderTrackQuarantined(stream.providerId, stream.providerTrackId)
        if (!usable) {
            streams.remove(identityKey, cached)
            return null
        }
        return cached
    }

    fun begin(identityKey: String, query: AlternativeTrackQuery): Deferred<HighQualityResolution> {
        if (!mode.enabled) return completed(HighQualityFallbackReason.DISABLED)
        if (eligibleLanes(identityKey, preference).isEmpty()) return completed(HighQualityFallbackReason.QUARANTINED)
        cachedSelection(identityKey)?.let { return CompletableDeferred(it) }
        val lookup = synchronized(inFlightLock) {
            inFlight[identityKey]?.let { return it }
            if (inFlight.size >= MAX_IN_FLIGHT_LOOKUPS) return completed(HighQualityFallbackReason.BUSY)
            val requestedPreference = preference
            scope.async(start = CoroutineStart.LAZY) { route(identityKey, query, requestedPreference) }
                .also { inFlight[identityKey] = it }
        }
        lookup.invokeOnCompletion {
            synchronized(inFlightLock) {
                inFlight.remove(identityKey, lookup)
            }
        }
        lookup.start()
        return lookup
    }

    suspend fun await(pending: Deferred<HighQualityResolution>, timeoutMs: Long): HighQualityResolution = try {
        when {
            pending.isCompleted -> pending.await()
            timeoutMs <= 0L -> HighQualityResolution.Fallback(HighQualityFallbackReason.NOT_READY)
            else -> withTimeoutOrNull(timeoutMs) { pending.await() }
                ?: HighQualityResolution.Fallback(HighQualityFallbackReason.NOT_READY, "waited ${timeoutMs}ms")
        }
    } catch (error: CancellationException) {
        currentCoroutineContext().ensureActive()
        HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "lookup cancelled")
    }

    fun reportPlaybackFailure(identityKey: String, providerId: String, providerTrackId: String, reason: String) {
        val until = clock() + FAILURE_QUARANTINE_MS
        streams.remove(identityKey)
        quarantine(quarantinedIdentities, providerKey(providerId, identityKey), until)
        if (providerTrackId.isNotBlank()) quarantine(quarantinedProviderTracks, providerKey(providerId, providerTrackId), until)
        mappingStore.remove(identityKey, providerId)
        HighQualityAudioDiagnostics.fallback(
            HighQualityFallbackReason.QUARANTINED,
            "$providerId playback failure: ${reason.take(80)}",
            providerTrackId
        )
    }

    fun pinManualMatch(identityKey: String, query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): Boolean {
        val lane = lanes.firstOrNull { it.provider.id == candidate.providerId } ?: return false
        val mapping = lane.pinnedMapping(query, candidate) ?: return false
        if (!mappingStore.save(identityKey, mapping)) return false
        streams.remove(identityKey)
        quarantinedIdentities.remove(providerKey(candidate.providerId, identityKey))
        quarantinedProviderTracks.remove(providerKey(candidate.providerId, candidate.providerTrackId))
        HighQualityAudioDiagnostics.manualPin(candidate.providerId, candidate.providerTrackId)
        return true
    }

    private suspend fun route(
        identityKey: String,
        query: AlternativeTrackQuery,
        preference: HighQualityPreference
    ): HighQualityResolution {
        val candidates = eligibleLanes(identityKey, preference)
        if (candidates.isEmpty()) return HighQualityResolution.Fallback(HighQualityFallbackReason.QUARANTINED)
        val run = RouteRun(identityKey, query, preference, candidates, TimeSource.Monotonic.markNow() + lookupBudgetMs.milliseconds)
        if (preference == HighQualityPreference.MAXIMUM || hedgeDelayMs <= 0L) candidates.indices.forEach(run::launch)
        try {
            for (index in candidates.indices) {
                val result = awaitLane(run, index)
                if (result == null) {
                    run.expired = true
                    break
                }
                if (run.record(candidates[index].provider.id, result)) break
            }
        } catch (error: CancellationException) {
            run.cancelAll()
            throw error
        }
        return run.outcome()
    }

    private suspend fun awaitLane(run: RouteRun, index: Int): HighQualityResolution? {
        if (run.started.size <= index) run.launch(index)
        val remaining = -run.deadline.elapsedNow().inWholeMilliseconds
        val hedgeNext = index + 1 < run.candidates.size && run.started.size == index + 1 && hedgeDelayMs in 1 until remaining
        if (!hedgeNext) return awaitPreferredLane(run.started[index], run.strongResultReady, run.deadline)
        withTimeoutOrNull(hedgeDelayMs) { run.started[index].await() }?.let { return it }
        HighQualityAudioDiagnostics.routeHedge(run.candidates[index].provider.id, run.candidates[index + 1].provider.id, hedgeDelayMs)
        run.launch(index + 1)
        return awaitPreferredLane(run.started[index], run.strongResultReady, run.deadline)
    }

    private suspend fun awaitPreferredLane(
        pending: Deferred<HighQualityResolution>,
        strongResultReady: Deferred<Unit>,
        deadline: TimeMark
    ): HighQualityResolution? {
        val remaining = -deadline.elapsedNow().inWholeMilliseconds
        if (remaining <= 0L) return if (pending.isCompleted) pending.await() else null
        val first = withTimeoutOrNull(remaining) {
            select<HighQualityResolution?> {
                pending.onAwait { it }
                strongResultReady.onAwait { null }
            }
        }
        if (first != null) return first
        if (pending.isCompleted) return pending.await()
        val left = -deadline.elapsedNow().inWholeMilliseconds
        if (left <= 0L) return null
        return withTimeoutOrNull(minOf(upgradeGraceMs, left)) { pending.await() }
            ?: HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "upgrade grace")
    }

    private inner class RouteRun(
        val identityKey: String,
        val query: AlternativeTrackQuery,
        val preference: HighQualityPreference,
        val candidates: List<HighQualityProviderLane>,
        val deadline: TimeMark
    ) {
        val started = ArrayList<Deferred<HighQualityResolution>>(candidates.size)
        val strongResultReady = CompletableDeferred<Unit>()
        private val failures = ArrayList<Pair<String, HighQualityResolution.Fallback>>(candidates.size)
        private var best: HighQualityResolution.Selected? = null
        var expired = false

        fun launch(index: Int) {
            val lane = candidates[index]
            HighQualityAudioDiagnostics.routeAttempt(lane.provider.id, index + 1, candidates.size, preference)
            started += scope.async {
                val result = withTimeoutOrNull(providerBudgetMs) { lane.resolve(identityKey, query, preference) }
                    ?: HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "provider budget")
                if (result is HighQualityResolution.Selected && HighQualityTierPolicy.isStrong(result.stream.quality)) {
                    strongResultReady.complete(Unit)
                }
                result
            }
        }

        fun record(providerId: String, result: HighQualityResolution): Boolean = when (result) {
            is HighQualityResolution.Selected -> {
                HighQualityAudioDiagnostics.routeOutcome(providerId, "SELECTED", result.stream.quality.label)
                val current = best
                if (current == null || result.stream.quality.rank > current.stream.quality.rank) best = result
                HighQualityTierPolicy.isStrong(result.stream.quality)
            }
            is HighQualityResolution.Fallback -> {
                HighQualityAudioDiagnostics.routeOutcome(providerId, result.reason.name, result.detail)
                failures += providerId to result
                false
            }
        }

        fun cancelAll() {
            started.forEach { it.cancel() }
        }

        fun outcome(): HighQualityResolution {
            val chosen = best
            return when {
                chosen != null -> {
                    if (mode.enabled) rememberStream(identityKey, chosen)
                    chosen
                }
                expired && failures.isEmpty() ->
                    HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "lookup budget")
                failures.size == 1 && !expired -> failures.single().second
                else -> combinedFallback(failures, expired)
            }
        }
    }

    private fun combinedFallback(
        failures: List<Pair<String, HighQualityResolution.Fallback>>,
        expired: Boolean
    ): HighQualityResolution.Fallback {
        val detail = buildList {
            failures.forEach { (providerId, fallback) ->
                add("$providerId:${fallback.reason.name}${fallback.detail.takeIf(String::isNotBlank)?.let { "/$it" }.orEmpty()}")
            }
            if (expired) add("lookup budget")
        }.joinToString(";")
        val reason = if (expired) HighQualityFallbackReason.TIMEOUT else failures.first().second.reason
        return HighQualityResolution.Fallback(reason, detail)
    }

    private fun eligibleLanes(identityKey: String, preference: HighQualityPreference): List<HighQualityProviderLane> {
        val ordered = if (preference == HighQualityPreference.MAXIMUM) {
            lanes.sortedByDescending { it.provider.losslessCapable }
        } else {
            lanes
        }
        return ordered.filterNot { isQuarantined(quarantinedIdentities, providerKey(it.provider.id, identityKey)) }
    }

    private fun isProviderTrackQuarantined(providerId: String, providerTrackId: String): Boolean =
        isQuarantined(quarantinedProviderTracks, providerKey(providerId, providerTrackId))

    private fun providerKey(providerId: String, value: String): String = "$providerId\u0000$value"

    private fun rememberStream(identityKey: String, selection: HighQualityResolution.Selected) {
        if (streams.size >= MAX_CACHED_STREAMS) {
            val now = clock()
            streams.entries.removeIf { !it.value.stream.isFresh(now, STREAM_REFRESH_MARGIN_MS) }
            if (streams.size >= MAX_CACHED_STREAMS) {
                streams.entries.minByOrNull { it.value.stream.expiresAtMs }?.let { streams.remove(it.key, it.value) }
            }
        }
        streams[identityKey] = selection
    }

    private fun quarantine(entries: ConcurrentHashMap<String, Long>, key: String, untilMs: Long) {
        if (entries.size >= MAX_QUARANTINE_ENTRIES) {
            val now = clock()
            entries.entries.removeIf { it.value <= now }
            if (entries.size >= MAX_QUARANTINE_ENTRIES) {
                entries.entries.minByOrNull { it.value }?.let { entries.remove(it.key, it.value) }
            }
        }
        entries[key] = untilMs
    }

    private fun isQuarantined(entries: ConcurrentHashMap<String, Long>, key: String): Boolean {
        val until = entries[key] ?: return false
        if (until > clock()) return true
        entries.remove(key, until)
        return false
    }

    private fun completed(reason: HighQualityFallbackReason): Deferred<HighQualityResolution> =
        CompletableDeferred(HighQualityResolution.Fallback(reason))

    companion object {
        const val LOOKUP_BUDGET_MS = 10_000L
        const val PROVIDER_BUDGET_MS = 6_500L
        const val HEDGE_DELAY_MS = 2_000L
        const val UPGRADE_GRACE_MS = 1_200L
        const val STREAM_REFRESH_MARGIN_MS = 90_000L
        const val FAILURE_QUARANTINE_MS = 30L * 60L * 1_000L
        const val MAX_IN_FLIGHT_LOOKUPS = 4
        const val MAX_CACHED_STREAMS = 64
        const val MAX_QUARANTINE_ENTRIES = 256
    }
}
