package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.selects.select
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class HighQualityBackgroundLookup : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<HighQualityBackgroundLookup>
}

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
    private val providers: List<HighQualityAudioProvider>,
    private val mappingStore: HighQualityMappingStore,
    private val scope: CoroutineScope,
    matcher: AlternativeTrackMatcher = AlternativeTrackMatcher(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val lookupBudgetMs: Long = LOOKUP_BUDGET_MS,
    private val losslessLaneBudgetMs: Long = LOSSLESS_LANE_BUDGET_MS
) {
    constructor(
        provider: HighQualityAudioProvider,
        mappingStore: HighQualityMappingStore,
        scope: CoroutineScope,
        matcher: AlternativeTrackMatcher = AlternativeTrackMatcher(),
        clock: () -> Long = System::currentTimeMillis,
        lookupBudgetMs: Long = LOOKUP_BUDGET_MS
    ) : this(listOf(provider), mappingStore, scope, matcher, clock, lookupBudgetMs)

    init {
        require(providers.isNotEmpty()) { "At least one provider is required" }
    }

    private val lanes = providers.map { provider ->
        HighQualityProviderLane(provider, mappingStore, matcher, clock) { providerTrackId ->
            isQuarantined(quarantinedProviderTracks, trackKey(provider.id, providerTrackId))
        }
    }
    private val streams = ConcurrentHashMap<String, HighQualityResolution.Selected>()
    private val quarantinedIdentities = ConcurrentHashMap<String, Long>()
    private val quarantinedProviderTracks = ConcurrentHashMap<String, Long>()
    private val unmatchedIdentities = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap<String, Deferred<HighQualityResolution>>()
    private val inFlightLock = Any()
    private val backgroundLookups = ConcurrentHashMap.newKeySet<String>()
    private val queuedBackground = ConcurrentHashMap.newKeySet<String>()
    private val backgroundGate = Semaphore(MAX_BACKGROUND_LOOKUPS)
    private val backgroundStarts = ArrayDeque<Long>()

    @Volatile
    var qobuzEnabled: Boolean = true
        set(value) {
            synchronized(inFlightLock) {
                if (field == value) return
                field = value
                inFlight.values.forEach { it.cancel() }
                inFlight.clear()
                backgroundLookups.clear()
                queuedBackground.clear()
                streams.entries.removeIf { it.value.stream.providerId == RetiredAudioProviders.QOBUZ_ID }
                if (value) unmatchedIdentities.clear()
            }
        }

    @Volatile
    var mode: HighQualityAudioMode = HighQualityAudioMode.OFF
        set(value) {
            synchronized(inFlightLock) {
                val losslessChanged = field.lossless != value.lossless
                field = value
                if (!value.enabled || losslessChanged) {
                    streams.clear()
                    inFlight.values.forEach { it.cancel() }
                    inFlight.clear()
                    backgroundLookups.clear()
                    queuedBackground.clear()
                }
                if (losslessChanged) unmatchedIdentities.clear()
            }
        }

    fun providerName(providerId: String): String =
        providers.firstOrNull { it.id == providerId }?.displayName ?: providers.first().displayName

    fun providerHealth(): List<ProviderBackendHealth> = providers.flatMap { it.health() }

    fun cachedSelection(identityKey: String): HighQualityResolution.Selected? {
        if (!mode.enabled || isQuarantined(quarantinedIdentities, identityKey)) return null
        val cached = streams[identityKey] ?: return null
        val usable = cached.stream.isFresh(clock(), STREAM_REFRESH_MARGIN_MS) &&
            (mode.lossless || !cached.stream.tier.lossless) &&
            !isQuarantined(quarantinedProviderTracks, trackKey(cached.stream.providerId, cached.stream.providerTrackId))
        if (!usable) {
            streams.remove(identityKey, cached)
            return null
        }
        return cached
    }

    /** True while a provider stream could still be found for this identity and none is cached yet. */
    fun upgradePending(identityKey: String): Boolean {
        if (!mode.enabled) return false
        if (isQuarantined(quarantinedIdentities, identityKey)) return false
        if (isQuarantined(unmatchedIdentities, identityKey)) return false
        if (cachedSelection(identityKey) != null) return false
        return synchronized(inFlightLock) {
            inFlight.containsKey(identityKey) || inFlight.size - backgroundLookups.size < MAX_IN_FLIGHT_LOOKUPS
        }
    }

    fun begin(
        identityKey: String,
        query: AlternativeTrackQuery,
        background: Boolean = false
    ): Deferred<HighQualityResolution> {
        if (!mode.enabled) return completed(HighQualityFallbackReason.DISABLED)
        if (isQuarantined(quarantinedIdentities, identityKey)) return completed(HighQualityFallbackReason.QUARANTINED)
        if (isQuarantined(unmatchedIdentities, identityKey)) return completed(HighQualityFallbackReason.NO_MATCH)
        cachedSelection(identityKey)?.let { return CompletableDeferred(it) }
        val queued = background && mode.lossless
        val lookup = synchronized(inFlightLock) {
            inFlight[identityKey]?.let { existing ->
                if (queued || !queuedBackground.remove(identityKey)) return existing
                existing.cancel()
                inFlight.remove(identityKey, existing)
                backgroundLookups.remove(identityKey)
            }
            val busy = if (queued) {
                inFlight.size >= MAX_IN_FLIGHT_LOOKUPS
            } else {
                inFlight.size - backgroundLookups.size >= MAX_IN_FLIGHT_LOOKUPS
            }
            if (busy) return completed(HighQualityFallbackReason.BUSY)
            if (queued && !admitBackgroundLookup()) return completed(HighQualityFallbackReason.BUSY)
            val created = if (queued) {
                backgroundLookups += identityKey
                queuedBackground += identityKey
                scope.async(start = CoroutineStart.LAZY) {
                    backgroundGate.withPermit {
                        queuedBackground.remove(identityKey)
                        lookup(identityKey, query)
                    }
                }
            } else {
                scope.async(start = CoroutineStart.LAZY) { lookup(identityKey, query) }
            }
            created.also { inFlight[identityKey] = it }
        }
        lookup.invokeOnCompletion {
            synchronized(inFlightLock) {
                if (inFlight.remove(identityKey, lookup) && queued) {
                    backgroundLookups.remove(identityKey)
                    queuedBackground.remove(identityKey)
                }
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

    suspend fun resolveLossy(identityKey: String, query: AlternativeTrackQuery): HighQualityResolution {
        if (!mode.enabled) return HighQualityResolution.Fallback(HighQualityFallbackReason.DISABLED)
        if (isQuarantined(quarantinedIdentities, identityKey)) return HighQualityResolution.Fallback(HighQualityFallbackReason.QUARANTINED)
        cachedSelection(identityKey)?.takeIf { !it.stream.tier.lossless }?.let { return it }
        val lossy = lanes.filter { !it.provider.lossless }
        if (lossy.isEmpty()) return HighQualityResolution.Fallback(HighQualityFallbackReason.DISABLED)
        return withTimeoutOrNull(lookupBudgetMs) { resolveByPriority(identityKey, query, lossy) }
            ?: HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "lossy lookup budget")
    }

    fun reportPlaybackFailure(
        identityKey: String,
        providerTrackId: String,
        reason: String,
        providerId: String = providers.first().id
    ) {
        val until = clock() + FAILURE_QUARANTINE_MS
        streams.remove(identityKey)
        val failedLossless = providers.firstOrNull { it.id == providerId }?.lossless == true
        if (!failedLossless || providerTrackId.isBlank()) quarantine(quarantinedIdentities, identityKey, until)
        if (providerTrackId.isNotBlank()) quarantine(quarantinedProviderTracks, trackKey(providerId, providerTrackId), until)
        mappingStore.remove(identityKey, providerId)
        HighQualityAudioDiagnostics.fallback(HighQualityFallbackReason.QUARANTINED, "playback failure: ${reason.take(80)}", providerTrackId)
    }

    private suspend fun lookup(identityKey: String, query: AlternativeTrackQuery): HighQualityResolution {
        val active = lanes.filter { (mode.lossless || !it.provider.lossless) &&
            (qobuzEnabled || it.provider.id != RetiredAudioProviders.QOBUZ_ID) }
        if (active.isEmpty()) return HighQualityResolution.Fallback(HighQualityFallbackReason.DISABLED)
        val resolution = withTimeoutOrNull(lookupBudgetMs) { resolveByPriority(identityKey, query, active) }
            ?: HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "lookup budget")
        val lookupContext = currentCoroutineContext()
        synchronized(inFlightLock) {
            lookupContext.ensureActive()
            if (resolution is HighQualityResolution.Selected && mode.enabled) rememberStream(identityKey, resolution)
            if (resolution is HighQualityResolution.Fallback && resolution.reason in DEFINITIVE_MISSES) {
                quarantine(unmatchedIdentities, identityKey, clock() + UNMATCHED_MEMORY_MS)
            }
        }
        return resolution
    }

    private suspend fun resolveByPriority(
        identityKey: String,
        query: AlternativeTrackQuery,
        active: List<HighQualityProviderLane>
    ): HighQualityResolution {
        val qobuz = active.firstOrNull { it.provider.id == RetiredAudioProviders.QOBUZ_ID && it.provider.lossless }
        if (qobuz != null) return resolveWithQobuz(identityKey, query, qobuz, active.filter { it !== qobuz })
        val preferred = active.firstOrNull { it.provider.id == "tidal" && it.provider.lossless }
        return if (preferred != null) resolvePreferred(identityKey, query, preferred, active.filter { it !== preferred })
            else resolveRacingProviders(identityKey, query, active)
    }

    private suspend fun resolveWithQobuz(
        identityKey: String,
        query: AlternativeTrackQuery,
        qobuz: HighQualityProviderLane,
        original: List<HighQualityProviderLane>
    ): HighQualityResolution = coroutineScope {
        val optional = async {
            val result = withTimeoutOrNull(RetiredAudioProviders.QOBUZ_LOOKUP_BUDGET_MS) { qobuz.resolve(identityKey, query) }
                ?: run {
                    qobuz.provider.reportLookupTimeout()
                    HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "qobuz lane budget")
                }
            if (result is HighQualityResolution.Fallback) {
                HighQualityAudioDiagnostics.fallback(result.reason, "qobuz: ${result.detail}", query.title)
            }
            result
        }
        if (original.isEmpty()) return@coroutineScope optional.await()
        val fallback = async { resolveByPriority(identityKey, query, original) }
        fun HighQualityResolution.verifiedQobuz() = this is HighQualityResolution.Selected && stream.tier.lossless
        fun combine(optionalResult: HighQualityResolution, existing: HighQualityResolution): HighQualityResolution = when {
            optionalResult.verifiedQobuz() -> optionalResult
            existing is HighQualityResolution.Fallback && existing.reason in DEFINITIVE_MISSES &&
                optionalResult is HighQualityResolution.Fallback && optionalResult.reason !in DEFINITIVE_MISSES -> optionalResult
            else -> existing
        }
        try {
            select {
                optional.onAwait { if (it.verifiedQobuz()) it else combine(it, fallback.await()) }
                fallback.onAwait { existing ->
                    if (optional.isCompleted || existing !is HighQualityResolution.Selected) {
                        combine(optional.await(), existing)
                    } else existing
                }
            }
        } finally {
            optional.cancel()
            fallback.cancel()
        }
    }

    private suspend fun resolvePreferred(
        identityKey: String,
        query: AlternativeTrackQuery,
        preferred: HighQualityProviderLane,
        others: List<HighQualityProviderLane>
    ): HighQualityResolution = coroutineScope {
        val fallback = if (others.isNotEmpty()) async { resolveByPriority(identityKey, query, others) } else null
        try {
            val tidal = resolveLane(preferred, identityKey, query)
            if (tidal is HighQualityResolution.Selected && tidal.stream.tier.lossless) tidal
                else fallback?.await() ?: tidal
        } finally {
            fallback?.cancel()
        }
    }

    private suspend fun resolveRacingProviders(
        identityKey: String,
        query: AlternativeTrackQuery,
        active: List<HighQualityProviderLane>
    ): HighQualityResolution = coroutineScope {
        val results = Channel<Pair<Boolean, HighQualityResolution>>(active.size)
        val attempts = active.map { lane -> async { results.send(lane.provider.lossless to resolveLane(lane, identityKey, query)) } }
        val fallbacks = ArrayList<HighQualityResolution.Fallback>(active.size)
        var lossy: HighQualityResolution.Selected? = null
        var remainingLossless = active.count { it.provider.lossless }
        try {
            repeat(active.size) {
                val (lossless, resolution) = results.receive()
                if (lossless) remainingLossless--
                when (resolution) {
                    is HighQualityResolution.Selected -> {
                        if (resolution.stream.tier.lossless || remainingLossless == 0) return@coroutineScope resolution
                        lossy = resolution
                    }
                    is HighQualityResolution.Fallback -> fallbacks += resolution
                }
                if (remainingLossless == 0) lossy?.let { return@coroutineScope it }
            }
            lossy ?: fallbacks.firstOrNull { it.reason !in DEFINITIVE_MISSES } ?: fallbacks.first()
        } finally {
            attempts.forEach { it.cancel() }
        }
    }

    private suspend fun resolveLane(
        lane: HighQualityProviderLane,
        identityKey: String,
        query: AlternativeTrackQuery
    ): HighQualityResolution {
        if (!lane.provider.lossless) return lane.resolve(identityKey, query)
        val resolution = withTimeoutOrNull(losslessLaneBudgetMs) { lane.resolve(identityKey, query) }
            ?: run {
                currentCoroutineContext().ensureActive()
                lane.provider.reportLookupTimeout()
                HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "${lane.provider.id} lane budget")
            }
        if (resolution is HighQualityResolution.Fallback) {
            HighQualityAudioDiagnostics.fallback(resolution.reason, "${lane.provider.id}: ${resolution.detail}", query.title)
        }
        return resolution
    }

    private fun trackKey(providerId: String, providerTrackId: String): String = "$providerId:$providerTrackId"

    private fun rememberStream(identityKey: String, selection: HighQualityResolution.Selected) {
        if (streams.size >= MAX_CACHED_STREAMS) {
            val now = clock()
            streams.entries.removeIf { !it.value.stream.isFresh(now, STREAM_REFRESH_MARGIN_MS) }
            if (streams.size >= MAX_CACHED_STREAMS) {
                streams.entries.minByOrNull { it.value.stream.expiresAtMs }?.let { streams.remove(it.key, it.value) }
            }
        }
        streams[identityKey] = if (mode.lossless && !selection.stream.tier.lossless) {
            selection.copy(stream = selection.stream.copy(
                expiresAtMs = minOf(selection.stream.expiresAtMs, clock() + STREAM_REFRESH_MARGIN_MS + LOSSLESS_FALLBACK_RETRY_MS)
            ))
        } else {
            selection
        }
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

    private fun admitBackgroundLookup(): Boolean {
        val now = clock()
        while (backgroundStarts.isNotEmpty() && now - backgroundStarts.first() >= BACKGROUND_WINDOW_MS) {
            backgroundStarts.removeFirst()
        }
        if (backgroundStarts.size >= MAX_BACKGROUND_LOOKUPS_PER_WINDOW) return false
        backgroundStarts.addLast(now)
        return true
    }

    private fun completed(reason: HighQualityFallbackReason): Deferred<HighQualityResolution> =
        CompletableDeferred(HighQualityResolution.Fallback(reason))

    companion object {
        const val LOOKUP_BUDGET_MS = 8_000L
        const val LOSSLESS_LANE_BUDGET_MS = 4_000L
        const val STREAM_REFRESH_MARGIN_MS = 90_000L
        const val LOSSLESS_FALLBACK_RETRY_MS = 90_000L
        const val FAILURE_QUARANTINE_MS = 30L * 60L * 1_000L
        const val UNMATCHED_MEMORY_MS = 30L * 60L * 1_000L
        private val DEFINITIVE_MISSES = setOf(HighQualityFallbackReason.NO_MATCH, HighQualityFallbackReason.AMBIGUOUS)
        const val MAX_IN_FLIGHT_LOOKUPS = 4
        const val MAX_BACKGROUND_LOOKUPS = 1
        const val MAX_BACKGROUND_LOOKUPS_PER_WINDOW = 6
        const val BACKGROUND_WINDOW_MS = 60_000L
        const val MAX_CACHED_STREAMS = 64
        const val MAX_QUARANTINE_ENTRIES = 256
    }
}
