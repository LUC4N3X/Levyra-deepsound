package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class MotionArtworkEngine(context: Context) {
    private val appContext = context.applicationContext
    private val repository = MotionArtworkRepository(LevyraDatabase.get(appContext).motionArtworkDao())
    private val networkPolicy = MotionArtworkNetworkPolicy(appContext)
    private val urlVerifier = MotionArtworkUrlVerifier(appContext)
    private val providerFactories: Map<String, () -> MotionArtworkProvider> = mapOf(
        "community-canvas" to { CommunityCanvasProvider(appContext) },
        "apple-motion" to { AppleMotionArtworkProvider(appContext) },
        "tidal-video-cover" to { TidalVideoCoverProvider(appContext) }
    )
    private val runtimeLock = Any()
    private var activeEpoch = -1L
    private var activeProviders: List<MotionArtworkProvider> = emptyList()
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<MotionArtwork?>>()
    private val lookupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val artistMotionProvider by lazy { AppleMotionArtworkProvider(appContext) }

    suspend fun resolve(
        track: Track,
        source: LevyraCanvasSource = LevyraCanvasSource.Auto
    ): MotionArtwork? {
        if (!networkPolicy.canResolveCurrent()) return null
        val identityKey = motionArtworkCacheKey(MotionArtworkIdentityKey.create(track), source)
        val runtime = MotionArtworkRuntime.snapshot()
        when (val cached = repository.get(identityKey, runtime.epoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }

        return shared("${runtime.epoch}:$identityKey") {
            resolveFresh(track, identityKey, runtime.epoch, runtime.value, source)
        }
    }

    suspend fun resolveArtist(
        artistName: String,
        artistBrowseId: String,
        source: LevyraCanvasSource = LevyraCanvasSource.Auto
    ): MotionArtwork? {
        val clean = artistName.trim()
        if (clean.length < 2) return null
        if (!networkPolicy.canResolveCurrent()) return null
        val runtime = MotionArtworkRuntime.snapshot()
        val config = runtime.value
        if (ARTIST_MOTION_PROVIDER !in motionArtworkProviderOrder(config.providerOrder, source)) return null
        val identityKey = motionArtworkCacheKey(
            MotionArtworkIdentityKey.forArtist(clean, artistBrowseId),
            source
        )
        when (val cached = repository.get(identityKey, runtime.epoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }
        return shared("${runtime.epoch}:$identityKey") {
            resolveArtistFresh(clean, identityKey, runtime.epoch, config)
        }
    }

    private suspend fun resolveArtistFresh(
        artistName: String,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig
    ): MotionArtwork? {
        when (val cached = repository.get(identityKey, configEpoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }
        var lookupFailed = false
        // A null outcome is inconclusive; ArtistMotionLookup(null) is a conclusive miss.
        val outcome = try {
            withTimeoutOrNull(config.requestTimeoutMs) {
                ArtistMotionLookup(artistMotionProvider.findArtistMotion(artistName))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Artist motion lookup failed for %s", artistName)
            null
        }
        if (outcome == null) lookupFailed = true
        val candidate = outcome?.candidate

        val verified = candidate?.takeIf { found ->
            when (urlVerifier.verify(found)) {
                MotionArtworkVerificationResult.Verified -> true
                MotionArtworkVerificationResult.Invalid -> false
                is MotionArtworkVerificationResult.Failed -> {
                    lookupFailed = true
                    false
                }
            }
        }

        if (verified == null) {
            if (!lookupFailed) {
                repository.saveNegative(
                    identityKey = identityKey,
                    configEpoch = configEpoch,
                    expiresAt = System.currentTimeMillis() + config.negativeTtlMs
                )
            }
            repository.cleanup(configEpoch)
            return null
        }

        val now = System.currentTimeMillis()
        val artwork = MotionArtwork(
            identityKey = identityKey,
            provider = verified.provider,
            url = verified.url,
            mimeType = verified.mimeType,
            width = verified.width,
            height = verified.height,
            confidence = ARTIST_MOTION_CONFIDENCE,
            expiresAtMs = minOf(verified.expiresAtMs, now + config.positiveTtlMs),
            lastVerifiedAtMs = now,
            configEpoch = configEpoch
        )
        repository.save(artwork)
        repository.cleanup(configEpoch)
        return artwork
    }

    private suspend fun shared(
        requestKey: String,
        block: suspend () -> MotionArtwork?
    ): MotionArtwork? {
        val deferred = inFlightMutex.withLock {
            inFlight[requestKey] ?: CompletableDeferred<MotionArtwork?>().also { pending ->
                inFlight[requestKey] = pending
                lookupScope.launch {
                    try {
                        pending.complete(block())
                    } catch (error: CancellationException) {
                        pending.completeExceptionally(error)
                    } catch (error: Throwable) {
                        Timber.d(error, "Motion artwork resolution failed")
                        pending.complete(null)
                    } finally {
                        withContext(NonCancellable) {
                            inFlightMutex.withLock {
                                inFlight.remove(requestKey, pending)
                            }
                        }
                    }
                }
            }
        }
        return deferred.await()
    }

    suspend fun prefetchNext(
        track: Track?,
        source: LevyraCanvasSource = LevyraCanvasSource.Auto
    ) {
        if (track == null || !networkPolicy.canPrefetchNext()) return
        resolve(track, source)
    }

    private suspend fun resolveFresh(
        track: Track,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig,
        source: LevyraCanvasSource
    ): MotionArtwork? {
        when (val cached = repository.get(identityKey, configEpoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }
        val identity = MotionTrackIdentity.from(track)
        val providerOrder = motionArtworkProviderOrder(config.providerOrder, source)
        val providers = providersFor(configEpoch, config).filter { it.id in providerOrder }
        val providerRanks = providerOrder.withIndex().associate { it.value to it.index }
        val outcomes = supervisorScope {
            providers.map { provider ->
                async {
                    try {
                        withTimeoutOrNull(config.requestTimeoutMs) {
                            provider.find(identity)
                        } ?: MotionArtworkProviderResult.Failed()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Timber.d(error, "Motion provider %s failed", provider.id)
                        MotionArtworkProviderResult.Failed(error)
                    }
                }
            }.awaitAll()
        }
        val providerFailed = providers.isEmpty() || outcomes.any { it is MotionArtworkProviderResult.Failed }
        val candidates = outcomes.flatMap { outcome ->
            when (outcome) {
                is MotionArtworkProviderResult.Found -> outcome.candidates
                MotionArtworkProviderResult.NoMatch,
                is MotionArtworkProviderResult.Failed -> emptyList()
            }
        }

        outcomes.forEachIndexed { index, outcome ->
            val provider = providers.getOrNull(index)?.id ?: "unknown"
            val summary = when (outcome) {
                is MotionArtworkProviderResult.Found -> "found=${outcome.candidates.size}"
                MotionArtworkProviderResult.NoMatch -> "noMatch"
                is MotionArtworkProviderResult.Failed -> "failed"
            }
            Timber.d(
                "motion provider %s -> %s for %s / %s (album=%s)",
                provider,
                summary,
                identity.title,
                identity.artists.joinToString(),
                identity.album
            )
        }

        val ranked = candidates
            .map { candidate -> candidate to CanonicalTrackMatcher.match(identity, candidate) }
            .onEach { (candidate, match) ->
                Timber.d(
                    "motion candidate %s scope=%s score=%d accepted=%b minimum=%d album=%s",
                    candidate.provider,
                    candidate.scope,
                    match.score,
                    match.accepted,
                    config.minimumConfidence,
                    candidate.identity.album
                )
            }
            .mapNotNull { (candidate, match) ->
                if (!match.accepted || match.score < config.minimumConfidence) null
                else MotionArtworkRankedCandidate(
                    candidate,
                    match.score,
                    providerRanks[candidate.provider] ?: Int.MAX_VALUE
                )
            }
            .sortedWith(
            compareBy<MotionArtworkRankedCandidate> { it.providerRank }
                .thenByDescending { it.confidence }
        )

        var verified: MotionArtworkRankedCandidate? = null
        var verifierFailed = false
        val verificationPlan = buildMotionArtworkVerificationPlan(ranked)
        for (rankedCandidate in verificationPlan.candidates) {
            when (urlVerifier.verify(rankedCandidate.candidate)) {
                MotionArtworkVerificationResult.Verified -> {
                    verified = rankedCandidate
                    break
                }
                MotionArtworkVerificationResult.Invalid -> Unit
                is MotionArtworkVerificationResult.Failed -> verifierFailed = true
            }
        }
        if (verified == null) {
            val conclusive = !providerFailed && !verifierFailed && verificationPlan.exhaustive
            if (conclusive) {
                repository.saveNegative(
                    identityKey = identityKey,
                    configEpoch = configEpoch,
                    expiresAt = System.currentTimeMillis() + config.negativeTtlMs
                )
            }
            repository.cleanup(configEpoch)
            return null
        }

        val now = System.currentTimeMillis()
        val candidate = verified.candidate
        val artwork = MotionArtwork(
            identityKey = identityKey,
            provider = candidate.provider,
            url = candidate.url,
            mimeType = candidate.mimeType,
            width = candidate.width,
            height = candidate.height,
            confidence = verified.confidence,
            expiresAtMs = minOf(candidate.expiresAtMs, now + config.positiveTtlMs),
            lastVerifiedAtMs = now,
            configEpoch = configEpoch
        )
        repository.save(artwork)
        repository.cleanup(configEpoch)
        return artwork
    }

    private fun providersFor(epoch: Long, config: MotionArtworkConfig): List<MotionArtworkProvider> = synchronized(runtimeLock) {
        if (activeEpoch != epoch) {
            activeProviders = config.providerOrder.mapNotNull { providerFactories[it]?.invoke() }
            activeEpoch = epoch
        }
        activeProviders
    }

}

private class ArtistMotionLookup(val candidate: MotionArtworkCandidate?)

private const val ARTIST_MOTION_PROVIDER = "apple-motion"
private const val ARTIST_MOTION_CONFIDENCE = 100

internal fun motionArtworkCacheKey(identityKey: String, source: LevyraCanvasSource): String =
    if (source == LevyraCanvasSource.Auto) identityKey else "$identityKey#${source.name.lowercase()}"

internal fun motionArtworkProviderOrder(
    configuredOrder: List<String>,
    source: LevyraCanvasSource
): List<String> {
    val forced = when (source) {
        LevyraCanvasSource.Auto -> return configuredOrder
        LevyraCanvasSource.Community -> "community-canvas"
        LevyraCanvasSource.Apple -> "apple-motion"
        LevyraCanvasSource.Tidal -> "tidal-video-cover"
    }
    return configuredOrder.filter { it == forced }
}

internal data class MotionArtworkRankedCandidate(
    val candidate: MotionArtworkCandidate,
    val confidence: Int,
    val providerRank: Int
)

internal data class MotionArtworkVerificationPlan(
    val candidates: List<MotionArtworkRankedCandidate>,
    val exhaustive: Boolean
)

internal fun buildMotionArtworkVerificationPlan(
    ranked: List<MotionArtworkRankedCandidate>,
    maxCandidatesPerProvider: Int = 3
): MotionArtworkVerificationPlan {
    require(maxCandidatesPerProvider > 0)
    val selectedPerProvider = mutableMapOf<Int, Int>()
    val candidates = ranked.filter { candidate ->
        val selected = selectedPerProvider[candidate.providerRank] ?: 0
        if (selected >= maxCandidatesPerProvider) {
            false
        } else {
            selectedPerProvider[candidate.providerRank] = selected + 1
            true
        }
    }
    return MotionArtworkVerificationPlan(
        candidates = candidates,
        exhaustive = candidates.size == ranked.size
    )
}
