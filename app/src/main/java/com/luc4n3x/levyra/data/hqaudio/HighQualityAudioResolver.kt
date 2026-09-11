package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val provider: HighQualityAudioProvider,
    private val mappingStore: HighQualityMappingStore,
    private val scope: CoroutineScope,
    private val matcher: AlternativeTrackMatcher = AlternativeTrackMatcher(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val lookupBudgetMs: Long = LOOKUP_BUDGET_MS
) {
    private val streams = ConcurrentHashMap<String, HighQualityResolution.Selected>()
    private val quarantinedIdentities = ConcurrentHashMap<String, Long>()
    private val quarantinedProviderTracks = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap<String, Deferred<HighQualityResolution>>()
    private val inFlightLock = Any()

    @Volatile
    var mode: HighQualityAudioMode = HighQualityAudioMode.OFF
        set(value) {
            field = value
            if (!value.enabled) streams.clear()
        }

    val providerId: String
        get() = provider.id

    val providerName: String
        get() = provider.displayName

    fun cachedSelection(identityKey: String): HighQualityResolution.Selected? {
        if (!mode.enabled || isQuarantined(quarantinedIdentities, identityKey)) return null
        val cached = streams[identityKey] ?: return null
        val usable = cached.stream.isFresh(clock(), STREAM_REFRESH_MARGIN_MS) &&
            !isQuarantined(quarantinedProviderTracks, cached.stream.providerTrackId)
        if (!usable) {
            streams.remove(identityKey, cached)
            return null
        }
        return cached
    }

    fun begin(identityKey: String, query: AlternativeTrackQuery): Deferred<HighQualityResolution> {
        if (!mode.enabled) return completed(HighQualityFallbackReason.DISABLED)
        if (isQuarantined(quarantinedIdentities, identityKey)) return completed(HighQualityFallbackReason.QUARANTINED)
        cachedSelection(identityKey)?.let { return CompletableDeferred(it) }
        val lookup = synchronized(inFlightLock) {
            inFlight[identityKey]?.let { return it }
            if (inFlight.size >= MAX_IN_FLIGHT_LOOKUPS) return completed(HighQualityFallbackReason.BUSY)
            scope.async(start = CoroutineStart.LAZY) {
                withTimeoutOrNull(lookupBudgetMs) { lookup(identityKey, query) }
                    ?: HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "lookup budget")
            }.also { inFlight[identityKey] = it }
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

    fun reportPlaybackFailure(identityKey: String, providerTrackId: String, reason: String) {
        val until = clock() + FAILURE_QUARANTINE_MS
        streams.remove(identityKey)
        quarantine(quarantinedIdentities, identityKey, until)
        if (providerTrackId.isNotBlank()) quarantine(quarantinedProviderTracks, providerTrackId, until)
        mappingStore.remove(identityKey)
        HighQualityAudioDiagnostics.fallback(HighQualityFallbackReason.QUARANTINED, "playback failure: ${reason.take(80)}", providerTrackId)
    }

    private suspend fun lookup(identityKey: String, query: AlternativeTrackQuery): HighQualityResolution = try {
        val queryFingerprint = AlternativeTrackFingerprint.of(query)
        val stored = mappingStore.load(identityKey, queryFingerprint)
        val refreshed = stored?.let { refreshStoredMapping(identityKey, query, it) }
        refreshed ?: searchAndResolve(identityKey, query, queryFingerprint)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, error.javaClass.simpleName)
    }

    private suspend fun refreshStoredMapping(
        identityKey: String,
        query: AlternativeTrackQuery,
        mapping: StoredAlternativeMapping
    ): HighQualityResolution? {
        if (mapping.providerId != provider.id || isQuarantined(quarantinedProviderTracks, mapping.providerTrackId)) {
            mappingStore.remove(identityKey)
            return null
        }
        return when (val outcome = provider.lookup(mapping.providerTrackId)) {
            is ProviderLookupOutcome.Found -> {
                val evaluation = matcher.evaluate(query, outcome.candidate)
                val unchanged = AlternativeTrackFingerprint.of(outcome.candidate) == mapping.candidateFingerprint
                if (!unchanged || !evaluation.accepted) {
                    HighQualityAudioDiagnostics.staleMapping(
                        provider.id,
                        mapping.providerTrackId,
                        if (!unchanged) "metadata changed" else "${evaluation.rejection}"
                    )
                    mappingStore.remove(identityKey)
                    null
                } else {
                    HighQualityAudioDiagnostics.cacheHit(provider.id, mapping.providerTrackId)
                    resolveStream(identityKey, evaluation, mapping.queryFingerprint)
                }
            }
            ProviderLookupOutcome.Missing -> {
                HighQualityAudioDiagnostics.staleMapping(provider.id, mapping.providerTrackId, "provider track missing")
                mappingStore.remove(identityKey)
                null
            }
            is ProviderLookupOutcome.Failed -> {
                mappingStore.remove(identityKey)
                HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
            }
        }
    }

    private suspend fun searchAndResolve(
        identityKey: String,
        query: AlternativeTrackQuery,
        queryFingerprint: String
    ): HighQualityResolution {
        val collected = LinkedHashMap<String, AlternativeTrackCandidate>()
        var selection: AlternativeMatchSelection = AlternativeMatchSelection.Rejected(MatchRejection.NO_CANDIDATES, emptyList())
        for ((index, text) in AlternativeSearchPlan.queries(query).withIndex()) {
            currentCoroutineContext().ensureActive()
            when (val outcome = provider.search(text)) {
                is ProviderSearchOutcome.Failed ->
                    return HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
                is ProviderSearchOutcome.Found -> {
                    HighQualityAudioDiagnostics.search(provider.id, index + 1, text, outcome.candidates.size)
                    outcome.candidates
                        .filterNot { isQuarantined(quarantinedProviderTracks, it.providerTrackId) }
                        .forEach { collected.putIfAbsent(it.providerTrackId, it) }
                }
            }
            selection = matcher.select(query, collected.values.toList())
            val decisive = when (val current = selection) {
                is AlternativeMatchSelection.Accepted ->
                    current.evaluation.verdict == AlternativeMatchVerdict.EXACT || index >= 1
                is AlternativeMatchSelection.Rejected -> current.reason == MatchRejection.AMBIGUOUS
            }
            if (decisive) break
        }
        return when (val finalSelection = selection) {
            is AlternativeMatchSelection.Rejected -> {
                HighQualityAudioDiagnostics.matchRejected(query, finalSelection)
                val reason = if (finalSelection.reason == MatchRejection.AMBIGUOUS) {
                    HighQualityFallbackReason.AMBIGUOUS
                } else {
                    HighQualityFallbackReason.NO_MATCH
                }
                HighQualityResolution.Fallback(reason, finalSelection.reason.name)
            }
            is AlternativeMatchSelection.Accepted -> {
                HighQualityAudioDiagnostics.matchAccepted(query, finalSelection.evaluation)
                resolveStream(identityKey, finalSelection.evaluation, queryFingerprint)
            }
        }
    }

    private suspend fun resolveStream(
        identityKey: String,
        evaluation: AlternativeMatchEvaluation,
        queryFingerprint: String
    ): HighQualityResolution = when (val outcome = provider.resolveStream(evaluation.candidate)) {
        is ProviderStreamOutcome.Resolved -> {
            val selection = HighQualityResolution.Selected(outcome.stream, evaluation)
            if (mode.enabled) rememberStream(identityKey, selection)
            mappingStore.save(
                identityKey,
                StoredAlternativeMapping(
                    providerId = provider.id,
                    providerTrackId = evaluation.candidate.providerTrackId,
                    queryFingerprint = queryFingerprint,
                    candidateFingerprint = AlternativeTrackFingerprint.of(evaluation.candidate),
                    verdict = evaluation.verdict,
                    confidence = evaluation.confidence,
                    storedAtMs = clock()
                )
            )
            selection
        }
        is ProviderStreamOutcome.Unavailable -> {
            mappingStore.remove(identityKey)
            HighQualityResolution.Fallback(
                HighQualityFallbackReason.STREAM_UNAVAILABLE,
                outcome.rejections.joinToString(",") { it.name }
            )
        }
        is ProviderStreamOutcome.Failed ->
            HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
    }

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
        const val LOOKUP_BUDGET_MS = 8_000L
        const val STREAM_REFRESH_MARGIN_MS = 90_000L
        const val FAILURE_QUARANTINE_MS = 30L * 60L * 1_000L
        const val MAX_IN_FLIGHT_LOOKUPS = 4
        const val MAX_CACHED_STREAMS = 64
        const val MAX_QUARANTINE_ENTRIES = 256
    }
}
