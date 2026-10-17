package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityDeeperPagesTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val identity = "playback-key-v2|track:blinding-lights"
    private val plan = AlternativeSearchPlan.queries(query())
    private val now = AtomicLong(1_000_000L)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun resolve(
        provider: FakeHighQualityProvider,
        budgetMs: Long = 5_000L,
        query: AlternativeTrackQuery = query(),
        quarantinedTrack: String? = null
    ): HighQualityResolution {
        val resolver = HighQualityAudioResolver(
            provider = provider,
            mappingStore = HighQualityMappingStore(InMemoryMappingStorage()),
            scope = scope,
            clock = now::get,
            lookupBudgetMs = budgetMs
        ).apply { mode = HighQualityAudioMode.AUTOMATIC }
        quarantinedTrack?.let { resolver.reportPlaybackFailure("playback-key-v2|track:other", it, "test") }
        return runBlocking { resolver.await(resolver.begin(identity, query), 5_000L) }
    }

    @Test
    fun transliteratedTitlesOnTheFirstPageEarnAnotherPage() {
        val kunFayaKun = query(title = "Kun Faya Kun", artist = "A.R. Rahman", album = "Rockstar", durationMs = 471_000L)
        val compilation = candidate(id = "compiled", title = "Kun Faaya Kun", primary = listOf("A.R. Rahman"), album = "Best Of A.R. Rahman", duration = 471)
            .copy(language = "hindi")
        val original = candidate(id = "csaEsVWV", title = "Kun Faaya Kun", primary = listOf("A.R. Rahman"), album = "Rockstar", duration = 472)
            .copy(language = "hindi")
        val provider = FakeHighQualityProvider(
            pageOutcome = { _, page ->
                if (page == 2) ProviderSearchOutcome.Found(listOf(original)) else ProviderSearchOutcome.Found(listOf(compilation), hasMore = true)
            },
            lookupOutcome = { ProviderLookupOutcome.Found(original) }
        )
        val result = resolve(provider, query = kunFayaKun)
        assertEquals("csaEsVWV", (result as HighQualityResolution.Selected).stream.providerTrackId)
    }

    @Test
    fun secondPagesOfEveryQueryComeBeforeAnyThirdPage() {
        var serial = 0
        val provider = provider { text, page ->
            if (text == plan[1] && page == 2) {
                ProviderSearchOutcome.Found(listOf(candidate()))
            } else {
                serial++
                ProviderSearchOutcome.Found(listOf(candidate(id = "cover-$serial", primary = listOf("Cover Band"), album = "Hits")), hasMore = true)
            }
        }
        val result = resolve(provider)
        assertEquals("pW-kkdqr", (result as HighQualityResolution.Selected).stream.providerTrackId)
        assertEquals(listOf(plan[0] to 2, plan[1] to 2), deeperPages(provider))
    }

    @Test
    fun highMatchOnTheFirstQuerySecondPageStillChecksPeerPages() {
        val high = candidate(id = "high", album = "After Hours (Deluxe)", duration = 201)
        val exact = candidate(id = "exact")
        val provider = FakeHighQualityProvider(
            pageOutcome = { text, page ->
                when {
                    page == 2 && text == plan[0] -> ProviderSearchOutcome.Found(listOf(high))
                    page == 2 && text == plan[1] -> ProviderSearchOutcome.Found(listOf(exact))
                    else -> coverFlood
                }
            },
            lookupOutcome = { id -> ProviderLookupOutcome.Found(if (id == "exact") exact else high) }
        )
        val result = resolve(provider)
        assertEquals("exact", (result as HighQualityResolution.Selected).stream.providerTrackId)
        assertTrue(plan[1] to 2 in provider.pages)
    }

    @Test
    fun acceptedDeeperMatchSurvivesAFailingPeerPage() {
        val high = candidate(id = "high", album = "After Hours (Deluxe)", duration = 201)
        val provider = FakeHighQualityProvider(
            pageOutcome = { text, page ->
                when {
                    page == 2 && text == plan[0] -> ProviderSearchOutcome.Found(listOf(high))
                    page == 2 && text == plan[1] -> ProviderSearchOutcome.Failed(ProviderFailure.TIMEOUT)
                    else -> coverFlood
                }
            },
            lookupOutcome = { ProviderLookupOutcome.Found(high) }
        )
        val result = resolve(provider)
        assertEquals("high", (result as HighQualityResolution.Selected).stream.providerTrackId)
        assertTrue(plan[1] to 2 in provider.pages)
    }

    @Test
    fun filteredRowsWithTheExpectedTitleKeepPaginationAlive() {
        val provider = provider { text, page ->
            when {
                text == plan.first() && page == 2 -> ProviderSearchOutcome.Found(listOf(candidate()))
                else -> ProviderSearchOutcome.Found(
                    listOf(candidate(id = "other", title = "Save Your Tears")),
                    hasMore = true,
                    unplayableRows = listOf(UnplayableSearchRow("Blinding Lights", "english"))
                )
            }
        }
        assertTrue(resolve(provider) is HighQualityResolution.Selected)
    }

    @Test
    fun laterQueryWithOverlappingFirstPageStillEarnsAnotherPage() {
        val provider = provider { text, page ->
            when {
                text == plan.last() && page == 2 -> ProviderSearchOutcome.Found(listOf(candidate()))
                text == plan.first() -> ProviderSearchOutcome.Found(listOf(candidate(id = "cover", primary = listOf("Cover Band"), album = "Hits")), hasMore = false)
                else -> coverFlood
            }
        }
        val result = resolve(provider)
        assertEquals("pW-kkdqr", (result as HighQualityResolution.Selected).stream.providerTrackId)
        assertTrue(plan.last() to 2 in provider.pages)
    }

    @Test
    fun quarantinedFirstPageStillEarnsAnotherPage() {
        val provider = provider { text, page ->
            when {
                text == plan.first() && page == 2 -> ProviderSearchOutcome.Found(listOf(candidate()))
                else -> ProviderSearchOutcome.Found(listOf(candidate(id = "broken")), hasMore = true)
            }
        }
        val result = resolve(provider, quarantinedTrack = "broken")
        assertEquals("pW-kkdqr", (result as HighQualityResolution.Selected).stream.providerTrackId)
    }

    private fun provider(pages: suspend (String, Int) -> ProviderSearchOutcome) = FakeHighQualityProvider(
        pageOutcome = pages,
        lookupOutcome = { ProviderLookupOutcome.Found(candidate()) }
    )

    private val coverFlood = ProviderSearchOutcome.Found(
        listOf(candidate(id = "cover", primary = listOf("Cover Band"), album = "Workout Hits")),
        hasMore = true
    )

    private fun deeperPages(provider: FakeHighQualityProvider) = provider.pages.filter { it.second > 1 }

    @Test
    fun decisiveFirstPageNeverRequestsAnotherPage() {
        val provider = provider { _, _ -> ProviderSearchOutcome.Found(listOf(candidate()), hasMore = true) }
        assertTrue(resolve(provider) is HighQualityResolution.Selected)
        assertEquals(listOf(plan.first() to 1), provider.pages)
    }

    @Test
    fun recordingOnlyOnTheSecondPageIsFound() {
        val provider = provider { text, page ->
            if (text == plan.first() && page == 2) ProviderSearchOutcome.Found(listOf(candidate())) else coverFlood
        }
        val result = resolve(provider)
        assertEquals("pW-kkdqr", (result as HighQualityResolution.Selected).stream.providerTrackId)
        assertEquals(plan.map { it to 1 }, provider.pages.take(plan.size))
        assertEquals(listOf(plan.first() to 2), deeperPages(provider))
    }

    @Test
    fun exhaustedPaginationStopsAfterThePlannedQueries() {
        val provider = provider { _, _ -> ProviderSearchOutcome.Found(emptyList(), hasMore = false) }
        assertEquals(HighQualityFallbackReason.NO_MATCH, (resolve(provider) as HighQualityResolution.Fallback).reason)
        assertTrue(deeperPages(provider).isEmpty())
    }

    @Test
    fun repeatedPageEndsThatQueryPagination() {
        val provider = provider { _, _ -> coverFlood }
        resolve(provider)
        val firstQueryPages = provider.pages.filter { it.first == plan.first() }.map { it.second }
        assertEquals(listOf(1, 2), firstQueryPages)
    }

    @Test
    fun deeperPagesAreBoundedPerLookup() {
        var serial = 0
        val provider = provider { _, _ ->
            serial++
            ProviderSearchOutcome.Found(
                listOf(candidate(id = "cover-$serial", primary = listOf("Cover Band $serial"), album = "Hits $serial")),
                hasMore = true
            )
        }
        assertEquals(HighQualityFallbackReason.NO_MATCH, (resolve(provider) as HighQualityResolution.Fallback).reason)
        assertEquals(HighQualityProviderLane.MAX_EXTRA_PAGES, deeperPages(provider).size)
        assertTrue(provider.pages.all { it.second <= HighQualityProviderLane.MAX_SEARCH_PAGE })
    }

    @Test
    fun unrelatedTitlesDoNotEarnAnotherPage() {
        val provider = provider { _, _ ->
            ProviderSearchOutcome.Found(listOf(candidate(id = "other", title = "Save Your Tears")), hasMore = true)
        }
        resolve(provider)
        assertTrue(deeperPages(provider).isEmpty())
    }

    @Test
    fun pageWithOnlyFilteredEntriesStillEarnsAnotherPage() {
        val provider = provider { text, page ->
            when {
                text == plan.first() && page == 2 -> ProviderSearchOutcome.Found(listOf(candidate()))
                else -> ProviderSearchOutcome.Found(emptyList(), hasMore = true)
            }
        }
        assertTrue(resolve(provider) is HighQualityResolution.Selected)
    }

    @Test
    fun secondPageFailureIsNotRememberedAsNoMatch() {
        val provider = provider { _, page ->
            if (page == 2) ProviderSearchOutcome.Failed(ProviderFailure.TIMEOUT) else coverFlood
        }
        val result = resolve(provider) as HighQualityResolution.Fallback
        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, result.reason)
        assertEquals("TIMEOUT", result.detail)
    }

    @Test
    fun malformedSecondPageFallsBackAsUnavailable() {
        val provider = provider { _, page ->
            if (page == 2) ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE) else coverFlood
        }
        val result = resolve(provider) as HighQualityResolution.Fallback
        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, result.reason)
        assertEquals("MALFORMED_RESPONSE", result.detail)
    }

    @Test
    fun ambiguousFirstPagesAreNotExpanded() {
        val provider = provider { _, _ ->
            ProviderSearchOutcome.Found(
                listOf(candidate(id = "a", duration = 197), candidate(id = "b", duration = 203)),
                hasMore = true
            )
        }
        assertEquals(HighQualityFallbackReason.AMBIGUOUS, (resolve(provider) as HighQualityResolution.Fallback).reason)
        assertTrue(deeperPages(provider).isEmpty())
    }

    @Test
    fun slowLookupSkipsDeeperPages() {
        val provider = provider { _, _ ->
            now.addAndGet(HighQualityProviderLane.DEEPER_PAGE_WINDOW_MS)
            coverFlood
        }
        resolve(provider)
        assertTrue(deeperPages(provider).isEmpty())
    }

    @Test
    fun hangingSecondPageIsBoundedByTheLookupBudget() {
        val provider = provider { _, page ->
            if (page == 2) delay(10_000L)
            coverFlood
        }
        val result = resolve(provider, budgetMs = 300L) as HighQualityResolution.Fallback
        assertEquals(HighQualityFallbackReason.TIMEOUT, result.reason)
    }
}
