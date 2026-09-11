package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.HighQualityAudioMode
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
            lookupOutcome = { ProviderLookupOutcome.Found(candidate(duration = 260)) }
        }
        val result = resolver(provider).resolveNow()
        assertTrue(result is HighQualityResolution.Selected)
        assertEquals(1, provider.lookups.size)
        assertTrue(provider.searches.isNotEmpty())
    }

    @Test
    fun disappearedProviderTrackTriggersFreshSearch() {
        resolver(exactProvider()).resolveNow()
        val provider = exactProvider().apply { lookupOutcome = { ProviderLookupOutcome.Missing } }
        resolver(provider).resolveNow()
        assertTrue(provider.searches.isNotEmpty())
    }

    @Test
    fun providerLookupFailureInvalidatesMappingAndFallsBack() {
        resolver(exactProvider()).resolveNow()
        val provider = exactProvider().apply { lookupOutcome = { ProviderLookupOutcome.Failed(ProviderFailure.TIMEOUT) } }
        val result = resolver(provider).resolveNow()
        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, (result as HighQualityResolution.Fallback).reason)
        assertTrue(storage.values.isEmpty())
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
                ProviderSearchOutcome.Found(listOf(candidate(id = "a", duration = 200), candidate(id = "b", duration = 202)))
            }
        )
        val result = resolver(provider).resolveNow(query(durationMs = 201_000L))
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

}
