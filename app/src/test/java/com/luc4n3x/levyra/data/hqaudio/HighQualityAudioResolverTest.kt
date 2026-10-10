package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityAudioResolverTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val storage = InMemoryMappingStorage()
    private val identity = "playback-key-v2|track:blinding-lights"

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun resolver(
        provider: FakeHighQualityProvider,
        budgetMs: Long = 5_000L,
        mode: HighQualityAudioMode = HighQualityAudioMode.AUTOMATIC
    ) = HighQualityAudioResolver(
        provider = provider,
        mappingStore = HighQualityMappingStore(storage),
        scope = scope,
        lookupBudgetMs = budgetMs
    ).apply { this.mode = mode }

    private fun HighQualityAudioResolver.resolveNow(query: AlternativeTrackQuery = query()): HighQualityResolution =
        runBlocking { await(begin(identity, query), 5_000L) }

    private fun exactProvider() = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
        lookupOutcome = { ProviderLookupOutcome.Found(candidate()) }
    )

    @Test
    fun disabledModeDoesNotContactProvider() {
        val provider = exactProvider()
        val result = resolver(provider, mode = HighQualityAudioMode.OFF).resolveNow()
        assertEquals(HighQualityFallbackReason.DISABLED, (result as HighQualityResolution.Fallback).reason)
        assertTrue(provider.searches.isEmpty())
    }

    @Test
    fun exactMatchIsSelectedAndPersisted() {
        val provider = exactProvider()
        val result = resolver(provider).resolveNow()
        assertTrue(result is HighQualityResolution.Selected)
        assertEquals(AlternativeMatchVerdict.EXACT, (result as HighQualityResolution.Selected).evaluation.verdict)
        assertEquals(AudioQualityTier.KBPS_320, result.stream.tier)
        assertEquals(1, provider.searches.size)
        assertEquals(1, storage.values.size)
    }

    @Test
    fun repeatedPlaybackUsesInMemoryStreamWithoutNewRequests() {
        val provider = exactProvider()
        val resolver = resolver(provider)
        resolver.resolveNow()
        val second = resolver.resolveNow()
        assertTrue(second is HighQualityResolution.Selected)
        assertEquals(1, provider.searches.size)
        assertEquals(1, provider.streamRequests.size)
    }

    @Test
    fun cacheHitRefreshesStoredMappingWithoutSearching() {
        resolver(exactProvider()).resolveNow()
        val provider = exactProvider()
        val result = resolver(provider).resolveNow()
        assertTrue(result is HighQualityResolution.Selected)
        assertTrue(provider.searches.isEmpty())
        assertEquals(listOf("pW-kkdqr"), provider.lookups.toList())
    }

    @Test
    fun staleMappingWithChangedProviderMetadataIsReplaced() {
        resolver(exactProvider()).resolveNow()
        val provider = exactProvider().apply {
            lookupOutcome = { ProviderLookupOutcome.Found(if (lookups.size == 1) candidate(duration = 260) else candidate()) }
        }
        val result = resolver(provider).resolveNow()
        assertTrue(result is HighQualityResolution.Selected)
        assertEquals(2, provider.lookups.size)
        assertTrue(provider.searches.isNotEmpty())
    }

    @Test
    fun selectedSearchCandidateIsHydratedBeforeItsStreamIsResolved() {
        val resolvedWith = CopyOnWriteArrayList<AlternativeTrackCandidate>()
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate().copy(offers320 = null))) },
            lookupOutcome = { ProviderLookupOutcome.Found(candidate(offers320 = true, isrc = "USUG11904206")) },
            streamOutcome = {
                resolvedWith += it
                ProviderStreamOutcome.Resolved(resolvedStream(it))
            }
        )
        val result = resolver(provider).resolveNow()
        assertTrue(result is HighQualityResolution.Selected)
        assertEquals(listOf("pW-kkdqr"), provider.lookups.toList())
        assertEquals(true, resolvedWith.single().offers320)
        assertEquals("USUG11904206", resolvedWith.single().isrc)
    }

    @Test
    fun detailsThatContradictTheIdentityNeverReachStreamResolution() {
        val conflicting = listOf(
            candidate(title = "Blinding Lights (Remix)"),
            candidate(title = "Blinding Lights (Live)"),
            candidate(primary = listOf("Someone Else")),
            candidate(duration = 260),
            candidate(isrc = "GBAYE0000001"),
            candidate(explicit = true),
            candidate(id = "other-id")
        )
        conflicting.forEach { details ->
            val provider = FakeHighQualityProvider(
                searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
                lookupOutcome = { ProviderLookupOutcome.Found(details) }
            )
            val result = resolver(provider).resolveNow(query(isrc = "USUG11904206", explicit = false))
            assertEquals(HighQualityFallbackReason.NO_MATCH, (result as HighQualityResolution.Fallback).reason)
            assertEquals("HYDRATION_CONFLICT", result.detail)
            assertTrue(provider.streamRequests.isEmpty())
            assertTrue(storage.values.isEmpty())
        }
    }

    @Test
    fun detailsMissingOrRestrictedNeverReachStreamResolution() {
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
            lookupOutcome = { ProviderLookupOutcome.Missing }
        )
        val result = resolver(provider).resolveNow()
        assertEquals(HighQualityFallbackReason.NO_MATCH, (result as HighQualityResolution.Fallback).reason)
        assertTrue(provider.streamRequests.isEmpty())
    }

    @Test
    fun temporaryDetailsFailureKeepsTheSafeSearchCandidate() {
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
            lookupOutcome = { ProviderLookupOutcome.Failed(ProviderFailure.TIMEOUT) }
        )
        val result = resolver(provider).resolveNow()
        assertEquals(AudioQualityTier.KBPS_320, (result as HighQualityResolution.Selected).stream.tier)
        assertEquals(listOf("pW-kkdqr"), provider.streamRequests.toList())
    }

    @Test
    fun slowDetailsAreBoundedAndKeepTheSafeSearchCandidate() {
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
            lookupOutcome = {
                delay(30_000L)
                ProviderLookupOutcome.Found(candidate(title = "Blinding Lights (Remix)"))
            }
        )
        val startedAt = System.nanoTime()
        val result = resolver(provider).resolveNow()
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
        assertTrue(result is HighQualityResolution.Selected)
        assertTrue(elapsedMs < HighQualityProviderLane.HYDRATION_TIMEOUT_MS + 2_000L)
    }

    @Test
    fun hydratedMetadataIsWhatTheStoredMappingRemembers() {
        val first = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate(duration = 201))) },
            lookupOutcome = { ProviderLookupOutcome.Found(candidate(duration = 200)) }
        )
        resolver(first).resolveNow()
        val second = exactProvider()
        assertTrue(resolver(second).resolveNow() is HighQualityResolution.Selected)
        assertTrue(second.searches.isEmpty())
        assertEquals(1, second.lookups.size)
    }

    @Test
    fun disappearedProviderTrackTriggersFreshSearch() {
        resolver(exactProvider()).resolveNow()
        val provider = exactProvider().apply { lookupOutcome = { ProviderLookupOutcome.Missing } }
        resolver(provider).resolveNow()
        assertTrue(provider.searches.isNotEmpty())
    }

    @Test
    fun providerLookupFailureKeepsMappingAndFallsBack() {
        resolver(exactProvider()).resolveNow()
        val stored = storage.values.toMap()
        val provider = exactProvider().apply { lookupOutcome = { ProviderLookupOutcome.Failed(ProviderFailure.TIMEOUT) } }
        val result = resolver(provider).resolveNow()
        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, (result as HighQualityResolution.Fallback).reason)
        assertEquals(stored, storage.values.toMap())
        assertTrue(provider.searches.isEmpty())
        assertTrue(provider.streamRequests.isEmpty())
        val recovered = exactProvider()
        assertTrue(resolver(recovered).resolveNow() is HighQualityResolution.Selected)
        assertTrue(recovered.searches.isEmpty())
    }

    @Test
    fun missingProviderTrackInvalidatesMapping() {
        resolver(exactProvider()).resolveNow()
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.NETWORK) },
            lookupOutcome = { ProviderLookupOutcome.Missing }
        )
        resolver(provider).resolveNow()
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun retainedMappingStillExpiresWithItsOriginalTtl() {
        var nowMs = 1_800_000_000_000L
        fun timedResolver(provider: FakeHighQualityProvider) = HighQualityAudioResolver(
            provider = provider,
            mappingStore = HighQualityMappingStore(storage, clock = { nowMs }),
            scope = scope,
            clock = { nowMs },
            lookupBudgetMs = 5_000L
        ).apply { mode = HighQualityAudioMode.AUTOMATIC }
        timedResolver(exactProvider()).resolveNow()
        val failing = exactProvider().apply { lookupOutcome = { ProviderLookupOutcome.Failed(ProviderFailure.TIMEOUT) } }
        nowMs += 24L * 60L * 60L * 1_000L
        timedResolver(failing).resolveNow()
        assertEquals(1, storage.values.size)
        nowMs += HighQualityMappingStore.DEFAULT_TTL_MS
        val expired = exactProvider().apply { lookupOutcome = { ProviderLookupOutcome.Failed(ProviderFailure.TIMEOUT) } }
        assertTrue(timedResolver(expired).resolveNow() is HighQualityResolution.Selected)
        assertEquals(1, expired.lookups.size)
        assertTrue(expired.searches.isNotEmpty())
    }

    @Test
    fun streamValidationFailureInvalidatesMapping() {
        resolver(exactProvider()).resolveNow()
        val provider = exactProvider().apply {
            streamOutcome = { ProviderStreamOutcome.Unavailable(listOf(StreamRejection.HTTP_STATUS)) }
        }
        val result = resolver(provider).resolveNow()
        assertEquals(HighQualityFallbackReason.STREAM_UNAVAILABLE, (result as HighQualityResolution.Fallback).reason)
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun incompleteArtistCreditFallsBackWithoutPersistence() {
        val provider = FakeHighQualityProvider(
            searchOutcome = {
                ProviderSearchOutcome.Found(listOf(candidate(title = "Starboy", album = "Starboy", duration = 202)))
            }
        )
        val result = resolver(provider).resolveNow(query(title = "Starboy", artist = "The Weeknd, Daft Punk", album = ""))
        assertEquals(HighQualityFallbackReason.NO_MATCH, (result as HighQualityResolution.Fallback).reason)
        assertTrue(provider.streamRequests.isEmpty())
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun ambiguousSearchFallsBackWithoutStreamRequest() {
        val provider = FakeHighQualityProvider(
            searchOutcome = {
                ProviderSearchOutcome.Found(listOf(candidate(id = "a", duration = 197), candidate(id = "b", duration = 203)))
            }
        )
        val result = resolver(provider).resolveNow(query(durationMs = 200_000L))
        assertEquals(HighQualityFallbackReason.AMBIGUOUS, (result as HighQualityResolution.Fallback).reason)
        assertTrue(provider.streamRequests.isEmpty())
    }

    @Test
    fun noMatchUsesEveryPlannedSearchPass() {
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate(title = "Blinding Lights (Remix)"))) }
        )
        val result = resolver(provider).resolveNow()
        assertEquals(HighQualityFallbackReason.NO_MATCH, (result as HighQualityResolution.Fallback).reason)
        assertEquals(AlternativeSearchPlan.queries(query()).size, provider.searches.size)
    }

    @Test
    fun providerFailureFallsBack() {
        val provider = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.FORBIDDEN) })
        val result = resolver(provider).resolveNow()
        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, (result as HighQualityResolution.Fallback).reason)
        assertEquals("FORBIDDEN", result.detail)
    }

    @Test
    fun slowProviderIsBoundedByLookupBudget() {
        val provider = FakeHighQualityProvider(searchOutcome = {
            delay(10_000L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val result = resolver(provider, budgetMs = 150L).resolveNow()
        assertEquals(HighQualityFallbackReason.TIMEOUT, (result as HighQualityResolution.Fallback).reason)
    }

    @Test
    fun awaitingPastCallerBudgetReportsNotReady() {
        val provider = FakeHighQualityProvider(searchOutcome = {
            delay(2_000L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val resolver = resolver(provider)
        val result = runBlocking { resolver.await(resolver.begin(identity, query()), 50L) }
        assertEquals(HighQualityFallbackReason.NOT_READY, (result as HighQualityResolution.Fallback).reason)
    }

    @Test
    fun concurrentRequestsShareOneLookup() {
        val provider = FakeHighQualityProvider(searchOutcome = {
            delay(100L)
            ProviderSearchOutcome.Found(listOf(candidate()))
        })
        val resolver = resolver(provider)
        val first = resolver.begin(identity, query())
        val second = resolver.begin(identity, query())
        assertTrue(first === second)
        runBlocking { resolver.await(first, 5_000L) }
        assertEquals(1, provider.searches.size)
    }

    @Test
    fun playbackFailureQuarantinesAlternative() {
        val provider = exactProvider()
        val resolver = resolver(provider)
        resolver.resolveNow()
        resolver.reportPlaybackFailure(identity, "pW-kkdqr", "HTTP 403")
        assertTrue(storage.values.isEmpty())
        assertNull(resolver.cachedSelection(identity))
        val result = resolver.resolveNow()
        assertEquals(HighQualityFallbackReason.QUARANTINED, (result as HighQualityResolution.Fallback).reason)
    }

    @Test
    fun turningModeOffDropsCachedStreams() {
        val resolver = resolver(exactProvider())
        resolver.resolveNow()
        resolver.mode = HighQualityAudioMode.OFF
        resolver.mode = HighQualityAudioMode.AUTOMATIC
        assertNull(resolver.cachedSelection(identity))
    }
    @Test
    fun inFlightLimitRejectsAdditionalDistinctLookup() {
        val provider = FakeHighQualityProvider(searchOutcome = {
            delay(5_000L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val resolver = resolver(provider)
        val admitted = (0 until HighQualityAudioResolver.MAX_IN_FLIGHT_LOOKUPS)
            .map { index -> resolver.begin("$identity-$index", query()) }
        val overflow = runBlocking {
            resolver.await(resolver.begin("$identity-overflow", query()), 100L)
        }
        assertEquals(HighQualityFallbackReason.BUSY, (overflow as HighQualityResolution.Fallback).reason)
        admitted.forEach { it.cancel() }
    }

    @Test
    fun saturatedLookupPoolDoesNotBlockCachedPlaybackForAnotherIdentity() {
        val provider = FakeHighQualityProvider(searchOutcome = {
            delay(5_000L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val resolver = resolver(provider)
        val admitted = (0 until HighQualityAudioResolver.MAX_IN_FLIGHT_LOOKUPS)
            .map { index -> resolver.begin("$identity-$index", query()) }

        assertTrue(resolver.upgradePending("$identity-0"))
        assertTrue(!resolver.upgradePending("$identity-overflow"))

        admitted.forEach { it.cancel() }
    }

    @Test
    fun exactIsrcMatchIsDecisiveEvenOnACompilationAlbum() {
        val provider = FakeHighQualityProvider(
            searchOutcome = {
                ProviderSearchOutcome.Found(listOf(candidate(album = "Greatest Hits 2020", isrc = "USUG11904206")))
            },
            lookupOutcome = { ProviderLookupOutcome.Found(candidate(album = "Greatest Hits 2020", isrc = "USUG11904206")) }
        )
        val result = resolver(provider).resolveNow(query(isrc = "USUG11904206"))
        assertEquals(100, (result as HighQualityResolution.Selected).evaluation.confidence)
        assertEquals(1, provider.searches.size)
    }

    @Test
    fun conflictingIsrcIsNeverSelected() {
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate(isrc = "GBAYE0000001"))) }
        )
        val result = resolver(provider).resolveNow(query(isrc = "USUG11904206"))
        assertEquals(HighQualityFallbackReason.NO_MATCH, (result as HighQualityResolution.Fallback).reason)
        assertTrue(provider.streamRequests.isEmpty())
    }

    @Test
    fun cancellingTheLookupStopsTheProviderAndAllowsAFreshLookup() {
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        val provider = FakeHighQualityProvider(searchOutcome = {
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                cancelled.set(true)
            }
        })
        val resolver = resolver(provider)
        val pending = resolver.begin(identity, query())
        runBlocking {
            delay(100L)
            pending.cancel()
            pending.join()
        }
        assertTrue(cancelled.get())
        val retry = resolver.begin(identity, query())
        assertTrue(retry !== pending)
        retry.cancel()
    }

    @Test
    fun noMatchIsRememberedSoRepeatedPlaybackDoesNotSearchAgain() {
        val provider = FakeHighQualityProvider()
        val resolver = resolver(provider)
        assertTrue(resolver.upgradePending(identity))
        val first = resolver.resolveNow()
        val searchesAfterFirst = provider.searches.size
        val second = resolver.resolveNow()
        assertEquals(HighQualityFallbackReason.NO_MATCH, (first as HighQualityResolution.Fallback).reason)
        assertEquals(HighQualityFallbackReason.NO_MATCH, (second as HighQualityResolution.Fallback).reason)
        assertEquals(searchesAfterFirst, provider.searches.size)
        assertTrue(!resolver.upgradePending(identity))
    }

    @Test
    fun upgradeStaysPendingUntilAStreamIsCached() {
        val resolver = resolver(exactProvider())
        assertTrue(resolver.upgradePending(identity))
        resolver.resolveNow()
        assertTrue(!resolver.upgradePending(identity))
    }

    @Test
    fun disabledModeNeverWaitsForAnUpgrade() {
        val resolver = resolver(exactProvider(), mode = HighQualityAudioMode.OFF)
        assertTrue(!resolver.upgradePending(identity))
    }
}
