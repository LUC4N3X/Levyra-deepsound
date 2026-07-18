package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class MotionArtworkEngine(context: Context) {
    private val appContext = context.applicationContext
    private val repository = MotionArtworkRepository(LevyraDatabase.get(appContext).motionArtworkDao())
    private val networkPolicy = MotionArtworkNetworkPolicy(appContext)
    private val urlVerifier = MotionArtworkUrlVerifier(appContext)
    private val providerFactories: Map<String, () -> MotionArtworkProvider> = mapOf(
        "apple-motion" to { AppleMotionArtworkProvider(appContext) },
        "tidal-video-cover" to { TidalVideoCoverProvider(appContext) }
    )
    private val runtimeLock = Any()
    private var activeEpoch = -1L
    private var activeProviders: List<MotionArtworkProvider> = emptyList()
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<MotionArtwork?>>()

    suspend fun resolve(track: Track): MotionArtwork? {
        if (!networkPolicy.canResolveCurrent()) return null
        val identityKey = MotionArtworkIdentityKey.create(track)
        val runtime = MotionArtworkRuntime.snapshot()
        when (val cached = repository.get(identityKey, runtime.epoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }

        val requestKey = "${runtime.epoch}:$identityKey"
        val (deferred, owner) = inFlightMutex.withLock {
            val existing = inFlight[requestKey]
            if (existing != null) {
                existing to false
            } else {
                CompletableDeferred<MotionArtwork?>().also { inFlight[requestKey] = it } to true
            }
        }

        if (owner) {
            try {
                deferred.complete(resolveFresh(track, identityKey, runtime.epoch, runtime.value))
            } catch (error: CancellationException) {
                deferred.completeExceptionally(error)
                throw error
            } catch (error: Throwable) {
                Timber.d(error, "Motion artwork resolution failed")
                deferred.complete(null)
            } finally {
                inFlightMutex.withLock { inFlight.remove(requestKey, deferred) }
            }
        }
        return deferred.await()
    }

    suspend fun prefetchNext(track: Track?) {
        if (track == null || !networkPolicy.canPrefetchNext()) return
        resolve(track)
    }

    private suspend fun resolveFresh(
        track: Track,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig
    ): MotionArtwork? {
        when (val cached = repository.get(identityKey, configEpoch)) {
            is MotionArtworkCacheResult.Hit -> return cached.artwork
            MotionArtworkCacheResult.Negative -> return null
            MotionArtworkCacheResult.Miss -> Unit
        }
        val identity = MotionTrackIdentity.from(track)
        val providers = providersFor(configEpoch, config)
        val providerRanks = config.providerOrder.withIndex().associate { it.value to it.index }
        val candidates = supervisorScope {
            providers.map { provider ->
                async {
                    try {
                        withTimeoutOrNull(config.requestTimeoutMs) { provider.find(identity) }.orEmpty()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Timber.d(error, "Motion provider %s failed", provider.id)
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }

        val ranked = candidates.mapNotNull { candidate ->
            val match = CanonicalTrackMatcher.match(identity, candidate)
            if (!match.accepted || match.score < config.minimumConfidence) null
            else RankedCandidate(candidate, match.score, providerRanks[candidate.provider] ?: Int.MAX_VALUE)
        }.sortedWith(
            compareByDescending<RankedCandidate> { it.confidence }
                .thenBy { it.providerRank }
        )

        var verified: RankedCandidate? = null
        for (rankedCandidate in ranked.take(3)) {
            if (urlVerifier.verify(rankedCandidate.candidate)) {
                verified = rankedCandidate
                break
            }
        }
        if (verified == null) {
            repository.saveNegative(
                identityKey = identityKey,
                configEpoch = configEpoch,
                expiresAt = System.currentTimeMillis() + config.negativeTtlMs
            )
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

    private data class RankedCandidate(
        val candidate: MotionArtworkCandidate,
        val confidence: Int,
        val providerRank: Int
    )
}
