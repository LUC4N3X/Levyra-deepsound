package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.AudioQualityRequest
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
    private val providers: List<HighQualityAudioProvider>,
    private val mappingStore: HighQualityMappingStore,
    private val scope: CoroutineScope,
    matcher: AlternativeTrackMatcher = AlternativeTrackMatcher(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val lookupBudgetMs: Long = LOOKUP_BUDGET_MS
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
        require(providers.isNotEmpty()) { "At least one high-quality audio provider is required" }
    }

    private val lanes = providers.map { provider ->
        HighQualityProviderLane(provider, mappingStore, matcher, clock) { providerTrackId ->
            isQuarantined(quarantinedProviderTracks, providerTrackKey(provider.id, providerTrackId))
        }
    }
    private val streams = ConcurrentHashMap<String, HighQualityResolution.Selected>()
    private val quarantinedIdentities = ConcurrentHashMap<String, Long>()
    private val quarantinedProviderTracks = ConcurrentHashMap<String, Long>()
    private val unmatchedIdentities = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap<String, Deferred<HighQualityResolution>>()
    private val inFlightLock = Any()

    @Volatile
    var mode: HighQualityAudioMode = HighQualityAudioMode.OFF
        set(value) {
            field = value
            if (!value.enabled && !playbackRequest.losslessEnabled) streams.clear()
        }

    @Volatile
    var playbackRequest: AudioQualityRequest = AudioQualityRequest()
        private set

    val providerId: String
        get() = providers.first().id

    val providerName: String
        get() = providers.first().displayName

    fun providerHealth(): List<ProviderBackendHealth> = providers.flatMap(HighQualityAudioProvider::health)

    fun providerName(providerId: String): String =
        providers.firstOrNull { it.id == providerId }?.displayName ?: providerId

    fun configureLossless(enabled: Boolean, quality: AudioQualityPreference) {
        val updated = AudioQualityRequest(preference = quality, losslessEnabled = enabled)
        if (updated == playbackRequest) return
        playbackRequest = updated
        streams.clear()
        unmatchedIdentities.clear()
    }

    fun invalidateSelections() {
        streams.clear()
        unmatchedIdentities.clear()
        quarantinedIdentities.clear()
    }

    fun requestForDownload(quality: AudioQualityPreference): AudioQualityRequest = AudioQualityRequest(
        preference = quality,
        purpose = com.luc4n3x.levyra.domain.AudioStreamPurpose.DOWNLOAD,
        losslessEnabled = playbackRequest.losslessEnabled
    )

    fun isActive(request: AudioQualityRequest = playbackRequest): Boolean =
        mode.enabled || request.losslessEnabled

    fun cachedSelection(
        identityKey: String,
        request: AudioQualityRequest = playbackRequest
    ): HighQualityResolution.Selected? {
        val key = resolutionKey(identityKey, request)
        if (!isActive(request)) return null
        val cached = streams[key] ?: return null
        val usable = cached.stream.isFresh(clock(), STREAM_REFRESH_MARGIN_MS) &&
            !isQuarantined(
                quarantinedProviderTracks,
                providerTrackKey(cached.stream.providerId, cached.stream.providerTrackId)
            )
        if (!usable) {
            streams.remove(key, cached)
            return null
        }
        return cached
    }

    /** True while a provider stream could still be found for this identity and none is cached yet. */
    fun upgradePending(identityKey: String, request: AudioQualityRequest = playbackRequest): Boolean {
        val key = resolutionKey(identityKey, request)
        if (!isActive(request)) return false
        if (isQuarantined(quarantinedIdentities, identityKey)) return false
        if (isQuarantined(unmatchedIdentities, key)) return false
        if (cachedSelection(identityKey, request) != null) return false
        return synchronized(inFlightLock) {
            inFlight.containsKey(key) || inFlight.size < MAX_IN_FLIGHT_LOOKUPS
        }
    }

    fun begin(
        identityKey: String,
        query: AlternativeTrackQuery,
        request: AudioQualityRequest = playbackRequest
    ): Deferred<HighQualityResolution> {
        val key = resolutionKey(identityKey, request)
        if (!isActive(request)) return completed(HighQualityFallbackReason.DISABLED)
        if (isQuarantined(quarantinedIdentities, identityKey)) return completed(HighQualityFallbackReason.QUARANTINED)
        if (isQuarantined(unmatchedIdentities, key)) return completed(HighQualityFallbackReason.NO_MATCH)
        cachedSelection(identityKey, request)?.let { return CompletableDeferred(it) }
        val lookup = synchronized(inFlightLock) {
            inFlight[key]?.let { return it }
            if (inFlight.size >= MAX_IN_FLIGHT_LOOKUPS) return completed(HighQualityFallbackReason.BUSY)
            scope.async(start = CoroutineStart.LAZY) { lookup(identityKey, query, request) }
                .also { inFlight[key] = it }
        }
        lookup.invokeOnCompletion {
            synchronized(inFlightLock) {
                inFlight.remove(key, lookup)
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

    fun reportPlaybackFailure(
        identityKey: String,
        providerId: String,
        providerTrackId: String,
        streamUrl: String,
        reason: String
    ) {
        val until = clock() + FAILURE_QUARANTINE_MS
        streams.keys.removeAll { it.startsWith("$identityKey|") }
        val provider = providers.firstOrNull { it.id == providerId }
        provider?.reportStreamFailure(providerTrackId, streamUrl, reason)
        if (providerTrackId.isNotBlank() && provider?.quarantineTrackOnStreamFailure != false) {
            quarantine(quarantinedIdentities, identityKey, until)
            quarantine(quarantinedProviderTracks, providerTrackKey(providerId, providerTrackId), until)
            mappingStore.remove(identityKey, providerId)
        }
        HighQualityAudioDiagnostics.fallback(HighQualityFallbackReason.QUARANTINED, "playback failure: ${reason.take(80)}", providerTrackId)
    }

    fun reportPlaybackFailure(identityKey: String, providerId: String, providerTrackId: String, reason: String) {
        reportPlaybackFailure(identityKey, providerId, providerTrackId, "", reason)
    }

    fun reportPlaybackFailure(identityKey: String, providerTrackId: String, reason: String) {
        reportPlaybackFailure(identityKey, providers.first().id, providerTrackId, "", reason)
    }

    private suspend fun lookup(
        identityKey: String,
        query: AlternativeTrackQuery,
        request: AudioQualityRequest
    ): HighQualityResolution {
        var resolution: HighQualityResolution = HighQualityResolution.Fallback(HighQualityFallbackReason.NO_MATCH)
        var sawExecutedLane = false
        var allExecutedLanesDefinitiveMisses = true
        val startedNanos = System.nanoTime()
        val activeLanes = lanes.filter { lane ->
            lane.provider.isEnabled(request) &&
                (mode.enabled || request.losslessEnabled || !lane.provider.requiresHighQualityMode)
        }
        for (lane in activeLanes) {
            currentCoroutineContext().ensureActive()
            val elapsedMs = (System.nanoTime() - startedNanos).coerceAtLeast(0L) / 1_000_000L
            val remainingMs = (lookupBudgetMs - elapsedMs).coerceAtLeast(0L)
            if (remainingMs == 0L) {
                allExecutedLanesDefinitiveMisses = false
                resolution = HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, "overall budget")
                break
            }
            resolution = withTimeoutOrNull(minOf(remainingMs, lane.provider.resolutionTimeoutMs)) {
                lane.resolve(identityKey, query, request)
            } ?: HighQualityResolution.Fallback(HighQualityFallbackReason.TIMEOUT, lane.provider.id)
            sawExecutedLane = true
            if (resolution is HighQualityResolution.Selected) break
            if (
                resolution is HighQualityResolution.Fallback &&
                resolution.reason !in DEFINITIVE_MISSES
            ) {
                allExecutedLanesDefinitiveMisses = false
            }
        }
        val key = resolutionKey(identityKey, request)
        if (resolution is HighQualityResolution.Selected && isActive(request)) rememberStream(key, resolution)
        if (
            resolution is HighQualityResolution.Fallback &&
            resolution.reason in DEFINITIVE_MISSES &&
            sawExecutedLane &&
            allExecutedLanesDefinitiveMisses
        ) {
            quarantine(unmatchedIdentities, key, clock() + UNMATCHED_MEMORY_MS)
        }
        return resolution
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

    private fun resolutionKey(identityKey: String, request: AudioQualityRequest): String =
        "$identityKey|${request.cacheKey}"

    private fun providerTrackKey(providerId: String, providerTrackId: String): String =
        "$providerId|$providerTrackId"

    private fun completed(reason: HighQualityFallbackReason): Deferred<HighQualityResolution> =
        CompletableDeferred(HighQualityResolution.Fallback(reason))

    companion object {
        const val LOOKUP_BUDGET_MS = 8_000L
        const val STREAM_REFRESH_MARGIN_MS = 90_000L
        const val FAILURE_QUARANTINE_MS = 30L * 60L * 1_000L
        const val UNMATCHED_MEMORY_MS = 30L * 60L * 1_000L
        private val DEFINITIVE_MISSES = setOf(HighQualityFallbackReason.NO_MATCH, HighQualityFallbackReason.AMBIGUOUS)
        const val MAX_IN_FLIGHT_LOOKUPS = 4
        const val MAX_CACHED_STREAMS = 64
        const val MAX_QUARANTINE_ENTRIES = 256
    }
}
