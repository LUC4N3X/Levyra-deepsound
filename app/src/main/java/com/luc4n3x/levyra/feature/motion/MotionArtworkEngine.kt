package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.ChartOfficialArtworkResolver
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Locale

class MotionArtworkEngine(context: Context) {
    private val appContext = context.applicationContext
    private val repository = MotionArtworkRepository(LevyraDatabase.get(appContext).motionArtworkDao())
    private val networkPolicy = MotionArtworkNetworkPolicy(appContext)
    private val urlVerifier = MotionArtworkUrlVerifier(appContext)
    private val metadataResolver = ChartOfficialArtworkResolver(appContext)
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

    fun close() {
        lookupScope.cancel()
    }

    suspend fun resolve(
        track: Track,
        source: LevyraCanvasSource = LevyraCanvasSource.Auto
    ): MotionArtwork? {
        if (!networkPolicy.canResolveCurrent()) return null
        val lookupTrack = prepareLookupTrack(track)
        val identityKey = motionArtworkCacheKey(MotionArtworkIdentityKey.create(lookupTrack), source)
        val runtime = MotionArtworkRuntime.snapshot()
        when (val cached = repository.get(identityKey, runtime.epoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }

        return shared("${runtime.epoch}:$identityKey") {
            resolveFresh(lookupTrack, identityKey, runtime.epoch, runtime.value, source)
        }
    }

    fun resolveProgressive(
        track: Track,
        source: LevyraCanvasSource = LevyraCanvasSource.Auto
    ): Flow<MotionArtwork> = flow {
        if (!networkPolicy.canResolveCurrent()) return@flow
        val lookupTrack = prepareLookupTrack(track)
        val identityKey = motionArtworkCacheKey(MotionArtworkIdentityKey.create(lookupTrack), source)
        val runtime = MotionArtworkRuntime.snapshot()
        when (val cached = repository.get(identityKey, runtime.epoch)) {
            is MotionArtworkCacheResult.Hit -> {
                emit(cached.artwork)
                return@flow
            }
            MotionArtworkCacheResult.Negative -> return@flow
            MotionArtworkCacheResult.Miss -> Unit
        }
        emitAll(resolveFreshProgressive(lookupTrack, identityKey, runtime.epoch, runtime.value, source))
    }.flowOn(Dispatchers.IO)

    private suspend fun prepareLookupTrack(track: Track): Track {
        if (track.isrc.isNotBlank() && !isUnusableMotionAlbum(track.album)) return track
        val country = Locale.getDefault().country
            .trim()
            .uppercase(Locale.ROOT)
            .takeIf { it.length == 2 }
            ?: DEFAULT_MOTION_METADATA_COUNTRY
        return try {
            metadataResolver.enrich(listOf(track), country).firstOrNull() ?: track
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Motion metadata enrichment failed for %s / %s", track.title, track.artist)
            track
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
        val identityKey = motionArtworkCacheKey(
            MotionArtworkIdentityKey.forArtist(clean, artistBrowseId),
            LevyraCanvasSource.Apple
        )
        when (val cached = repository.get(identityKey, runtime.epoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }
        Timber.d("Artist motion engine resolve start artist=%s source=%s", clean, source)
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
        val outcome = try {
            withTimeoutOrNull(ARTIST_MOTION_REQUEST_TIMEOUT_MS) {
                ArtistMotionLookup(artistMotionProvider.findArtistMotion(artistName))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Artist motion lookup failed for %s", artistName)
            null
        }
        if (outcome == null) {
            lookupFailed = true
            Timber.d("Artist motion lookup timed out after %dms for %s", ARTIST_MOTION_REQUEST_TIMEOUT_MS, artistName)
        }
        val candidate = outcome?.candidate
        if (candidate == null && outcome != null) {
            Timber.d("Artist motion provider returned conclusive miss for %s", artistName)
        }

        val verified = candidate?.takeIf { found ->
            when (val result = urlVerifier.verify(found)) {
                MotionArtworkVerificationResult.Verified -> {
                    Timber.d("Artist motion verifier VERIFIED provider=%s artist=%s", found.provider, artistName)
                    true
                }
                MotionArtworkVerificationResult.Invalid -> {
                    Timber.d("Artist motion verifier INVALID provider=%s artist=%s", found.provider, artistName)
                    false
                }
                is MotionArtworkVerificationResult.Failed -> {
                    lookupFailed = true
                    Timber.d(result.cause, "Artist motion verifier FAILED provider=%s artist=%s", found.provider, artistName)
                    false
                }
            }
        }

        if (verified == null) {
            if (!lookupFailed) {
                Timber.d("Artist motion saving negative cache artist=%s", artistName)
                repository.saveNegative(
                    identityKey = identityKey,
                    configEpoch = configEpoch,
                    expiresAt = System.currentTimeMillis() + config.negativeTtlMs
                )
            } else {
                Timber.d("Artist motion transient failure; negative cache skipped artist=%s", artistName)
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
        Timber.d("Artist motion resolved provider=%s artist=%s", artwork.provider, artistName)
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
    ): MotionArtwork? = resolveFreshProgressive(track, identityKey, configEpoch, config, source).lastOrNull()

    private fun resolveFreshProgressive(
        track: Track,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig,
        source: LevyraCanvasSource
    ): Flow<MotionArtwork> = flow {
        when (val cached = repository.get(identityKey, configEpoch)) {
            is MotionArtworkCacheResult.Hit -> {
                emit(cached.artwork)
                return@flow
            }
            MotionArtworkCacheResult.Negative -> return@flow
            MotionArtworkCacheResult.Miss -> Unit
        }
        val identity = MotionTrackIdentity.from(track)
        val providerOrder = motionArtworkProviderOrder(config.providerOrder, source)
        val providers = providersFor(configEpoch, config).filter { it.id in providerOrder }
        val providerRanks = providerOrder.withIndex().associate { it.value to it.index }
        val forcedSource = source != LevyraCanvasSource.Auto

        var providerFailed = providers.isEmpty()
        var verifierFailed = false
        var verificationExhaustive = true
        var publishedArtwork: MotionArtwork? = null
        var publishedCandidate: MotionArtworkRankedCandidate? = null
        var upgradesUsed = 0

        supervisorScope {
            val lookupOutcomes = Channel<IndexedMotionProviderOutcome>(Channel.UNLIMITED)
            val lookups = providers.mapIndexed { index, provider ->
                launch {
                    lookupOutcomes.send(
                        IndexedMotionProviderOutcome(index, runProviderLookup(provider, identity, config))
                    )
                }
            }
            launch {
                lookups.joinAll()
                lookupOutcomes.close()
            }
            val pendingOutcomes = mutableListOf<IndexedMotionProviderOutcome>()
            for (lookup in lookupOutcomes) {
                pendingOutcomes.add(lookup)
                while (true) {
                    val next = lookupOutcomes.tryReceive().getOrNull() ?: break
                    pendingOutcomes.add(next)
                }
                pendingOutcomes.sortBy { providerRanks[providers[it.index].id] ?: Int.MAX_VALUE }
                while (pendingOutcomes.isNotEmpty()) {
                    val currentLookup = pendingOutcomes.removeAt(0)
                    val provider = providers[currentLookup.index]
                    val outcome = currentLookup.outcome
                    val summary = when (outcome) {
                        is MotionArtworkProviderResult.Found -> "found=${outcome.candidates.size}"
                        MotionArtworkProviderResult.NoMatch -> "noMatch"
                        is MotionArtworkProviderResult.Failed -> "failed"
                    }
                    Timber.d(
                        "motion provider %s -> %s for %s / %s (album=%s)",
                        provider.id,
                        summary,
                        identity.title,
                        identity.artists.joinToString(),
                        identity.album
                    )
                    if (outcome is MotionArtworkProviderResult.Failed) providerFailed = true
                    val candidates = (outcome as? MotionArtworkProviderResult.Found)?.candidates.orEmpty()
                    if (candidates.isEmpty()) continue
                    val providerRank = providerRanks[provider.id] ?: Int.MAX_VALUE
                    if (
                        !shouldPublishMotionUpgrade(
                            publishedProviderRank = publishedCandidate?.providerRank,
                            candidateProviderRank = providerRank,
                            forcedSource = forcedSource,
                            upgradesUsed = upgradesUsed
                        )
                    ) {
                        verificationExhaustive = false
                        continue
                    }
                    val ranked = rankMotionCandidates(identity, candidates, config, providerRanks)
                    val verificationPlan = buildMotionArtworkVerificationPlan(ranked)
                    if (!verificationPlan.exhaustive) verificationExhaustive = false
                    var selected: MotionArtworkRankedCandidate? = null
                    for ((index, rankedCandidate) in verificationPlan.candidates.withIndex()) {
                        val candidate = rankedCandidate.candidate
                        when (val result = urlVerifier.verify(candidate)) {
                            MotionArtworkVerificationResult.Verified -> {
                                Timber.d(
                                    "motion verifier VERIFIED provider=%s scope=%s title=%s",
                                    candidate.provider,
                                    candidate.scope,
                                    identity.title
                                )
                                selected = rankedCandidate
                            }
                            MotionArtworkVerificationResult.Invalid -> {
                                Timber.d(
                                    "motion verifier INVALID provider=%s scope=%s title=%s",
                                    candidate.provider,
                                    candidate.scope,
                                    identity.title
                                )
                            }
                            is MotionArtworkVerificationResult.Failed -> {
                                verifierFailed = true
                                Timber.d(
                                    result.cause,
                                    "motion verifier FAILED provider=%s scope=%s title=%s",
                                    candidate.provider,
                                    candidate.scope,
                                    identity.title
                                )
                            }
                        }
                        if (selected != null) {
                            if (index != verificationPlan.candidates.lastIndex) verificationExhaustive = false
                            break
                        }
                    }
                    val accepted = selected ?: continue
                    if (publishedCandidate != null) upgradesUsed++
                    publishedCandidate = accepted
                    val artwork = motionArtworkFrom(accepted, identityKey, configEpoch, config)
                    publishedArtwork = artwork
                    repository.save(artwork)
                    emit(artwork)
                }
            }
        }

        val stabilized = publishedArtwork
        if (stabilized == null) {
            val conclusive = !providerFailed && !verifierFailed && verificationExhaustive
            if (conclusive) {
                Timber.d("motion resolve conclusive miss; saving negative cache title=%s source=%s", identity.title, source)
                repository.saveNegative(
                    identityKey = identityKey,
                    configEpoch = configEpoch,
                    expiresAt = System.currentTimeMillis() + config.negativeTtlMs
                )
            } else {
                Timber.d(
                    "motion resolve transient/non-exhaustive miss title=%s source=%s providerFailed=%b verifierFailed=%b exhaustive=%b",
                    identity.title,
                    source,
                    providerFailed,
                    verifierFailed,
                    verificationExhaustive
                )
            }
            repository.cleanup(configEpoch)
            return@flow
        }

        repository.save(stabilized)
        repository.cleanup(configEpoch)
        Timber.d(
            "motion resolved provider=%s scope=%s title=%s",
            stabilized.provider,
            publishedCandidate?.candidate?.scope,
            identity.title
        )
    }

    private suspend fun runProviderLookup(
        provider: MotionArtworkProvider,
        identity: MotionTrackIdentity,
        config: MotionArtworkConfig
    ): MotionArtworkProviderResult {
        val timeoutMs = motionArtworkProviderTimeoutMs(provider.id, config.requestTimeoutMs)
        return try {
            withTimeoutOrNull(timeoutMs) { provider.find(identity) } ?: run {
                Timber.d(
                    "Motion provider %s TIMEOUT after %dms for %s / %s",
                    provider.id,
                    timeoutMs,
                    identity.title,
                    identity.artists.joinToString()
                )
                MotionArtworkProviderResult.Failed()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Motion provider %s failed", provider.id)
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private fun rankMotionCandidates(
        identity: MotionTrackIdentity,
        candidates: List<MotionArtworkCandidate>,
        config: MotionArtworkConfig,
        providerRanks: Map<String, Int>
    ): List<MotionArtworkRankedCandidate> = candidates
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

    private fun motionArtworkFrom(
        ranked: MotionArtworkRankedCandidate,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig
    ): MotionArtwork {
        val now = System.currentTimeMillis()
        val candidate = ranked.candidate
        return MotionArtwork(
            identityKey = identityKey,
            provider = candidate.provider,
            url = candidate.url,
            mimeType = candidate.mimeType,
            width = candidate.width,
            height = candidate.height,
            confidence = ranked.confidence,
            expiresAtMs = minOf(candidate.expiresAtMs, now + config.positiveTtlMs),
            lastVerifiedAtMs = now,
            configEpoch = configEpoch
        )
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

private data class IndexedMotionProviderOutcome(
    val index: Int,
    val outcome: MotionArtworkProviderResult
)

private const val ARTIST_MOTION_CONFIDENCE = 100
private const val ARTIST_MOTION_REQUEST_TIMEOUT_MS = 45_000L
private const val APPLE_MOTION_PLAYER_REQUEST_TIMEOUT_MS = 25_000L
private const val DEFAULT_MOTION_METADATA_COUNTRY = "IT"

internal fun motionArtworkProviderTimeoutMs(providerId: String, configuredTimeoutMs: Long): Long =
    if (providerId == "apple-motion") {
        maxOf(configuredTimeoutMs, APPLE_MOTION_PLAYER_REQUEST_TIMEOUT_MS)
    } else {
        configuredTimeoutMs
    }

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

internal const val MAX_MOTION_ARTWORK_UPGRADES = 1

internal fun shouldPublishMotionUpgrade(
    publishedProviderRank: Int?,
    candidateProviderRank: Int,
    forcedSource: Boolean,
    upgradesUsed: Int,
    maxUpgrades: Int = MAX_MOTION_ARTWORK_UPGRADES
): Boolean {
    if (publishedProviderRank == null) return true
    if (forcedSource) return false
    if (upgradesUsed >= maxUpgrades) return false
    return candidateProviderRank < publishedProviderRank
}
