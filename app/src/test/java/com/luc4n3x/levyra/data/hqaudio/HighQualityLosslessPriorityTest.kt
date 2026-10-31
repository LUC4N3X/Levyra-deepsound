package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityLosslessPriorityTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun losslessCandidate(providerId: String, id: String) = candidate(id = id).copy(providerId = providerId)

    private fun flacStream(candidate: AlternativeTrackCandidate) = resolvedStream(candidate).copy(
        url = "https://tracks.monochrome.st/track/${candidate.providerTrackId}",
        tier = AudioQualityTier.LOSSLESS_HI_RES,
        mimeType = "audio/flac",
        container = "flac",
        codec = "flac",
        estimatedKbps = 3_200,
        bitDepth = 24,
        sampleRateHz = 96_000
    )

    private fun losslessProvider(id: String, outcome: suspend (AlternativeTrackCandidate) -> ProviderStreamOutcome) =
        FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(losslessCandidate(id, "$id-1"))) },
            streamOutcome = outcome,
            id = id,
            displayName = id,
            lossless = true,
            supportsLookup = false
        )

    private fun jioSaavn() = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
        lookupOutcome = { ProviderLookupOutcome.Found(candidate()) }
    )

    private fun resolver(
        mode: HighQualityAudioMode,
        vararg providers: HighQualityAudioProvider,
        losslessLaneBudgetMs: Long = HighQualityAudioResolver.LOSSLESS_LANE_BUDGET_MS
    ) = HighQualityAudioResolver(
        providers = providers.toList(),
        mappingStore = HighQualityMappingStore(InMemoryMappingStorage()),
        scope = scope,
        losslessLaneBudgetMs = losslessLaneBudgetMs
    ).apply { this.mode = mode }

    private fun HighQualityAudioResolver.resolveNow(): HighQualityResolution =
        runBlocking { await(begin("key", query()), 5_000L) }

    @Test
    fun flacProviderWinsOverJioSaavnWhenLosslessIsPreferred() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }

        val resolution = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn()).resolveNow()

        resolution as HighQualityResolution.Selected
        assertEquals("monochrome", resolution.stream.providerId)
        assertEquals(AudioQualityTier.LOSSLESS_HI_RES, resolution.stream.tier)
        assertTrue(monochrome.lookups.isEmpty())
    }

    @Test
    fun tidalWinsEvenWhenMonochromeIsReadyFirst() {
        val tidal = losslessProvider("tidal") { delay(120L); ProviderStreamOutcome.Resolved(flacStream(it)) }
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val result = resolver(HighQualityAudioMode.PREFER_LOSSLESS, tidal, monochrome, jioSaavn()).resolveNow()
        assertEquals("tidal", (result as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun failedTidalKeepsMonochromeFallback() {
        val tidal = losslessProvider("tidal") { ProviderStreamOutcome.Failed(ProviderFailure.FORBIDDEN) }
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val result = resolver(HighQualityAudioMode.PREFER_LOSSLESS, tidal, monochrome, jioSaavn()).resolveNow()
        assertEquals("monochrome", (result as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun temporaryPreferredProviderFailureIsNotNegativeCachedWhenOtherProvidersHaveNoMatch() {
        val preferred = losslessProvider("tidal") { ProviderStreamOutcome.Failed(ProviderFailure.HTTP_ERROR) }
        val resolver = resolver(HighQualityAudioMode.PREFER_LOSSLESS, preferred, FakeHighQualityProvider())
        val result = resolver.resolveNow()
        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, (result as HighQualityResolution.Fallback).reason)
        assertTrue(resolver.upgradePending("key"))
    }

    @Test
    fun stalledTidalIsBoundedAndFallsBack() {
        val tidal = losslessProvider("tidal") { awaitCancellation() }
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val result = resolver(HighQualityAudioMode.PREFER_LOSSLESS, tidal, monochrome, losslessLaneBudgetMs = 150L).resolveNow()
        assertEquals("monochrome", (result as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun readySecondLosslessSourceDoesNotWaitForAStalledFirstSource() = runBlocking {
        val monochrome = losslessProvider("monochrome") { awaitCancellation() }
        val arcod = losslessProvider("arcod") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val resolver = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, arcod, jioSaavn())

        val result = withTimeout(1_000L) { resolver.await(resolver.begin("race", query()), 2_000L) }

        assertEquals("arcod", (result as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun secondFlacProviderIsUsedWhenTheFirstIsRateLimited() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Failed(ProviderFailure.RATE_LIMITED) }
        val arcod = losslessProvider("arcod") { ProviderStreamOutcome.Resolved(flacStream(it)) }

        val resolution = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, arcod, jioSaavn()).resolveNow()

        assertEquals("arcod", (resolution as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun slowFlacCannotDelayJioSaavnPastTheDefaultLaneDeadline() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Failed(ProviderFailure.RATE_LIMITED) }
        val arcod = losslessProvider("arcod") {
            awaitCancellation()
            ProviderStreamOutcome.Resolved(flacStream(it))
        }

        val resolution = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, arcod, jioSaavn()).resolveNow()

        assertEquals("jiosaavn", (resolution as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun acceptedLosslessIdentityDoesNotNeedARedundantSearch() {
        val searched = candidate(album = "").copy(providerId = "monochrome")
        val monochrome = FakeHighQualityProvider(
            id = "monochrome", lossless = true, supportsLookup = false,
            searchOutcome = { ProviderSearchOutcome.Found(listOf(searched)) },
            streamOutcome = { ProviderStreamOutcome.Resolved(flacStream(it)) }
        )

        val resolution = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn()).resolveNow()

        assertEquals("monochrome", (resolution as HighQualityResolution.Selected).stream.providerId)
        assertEquals(1, monochrome.searches.size)
    }

    @Test
    fun jioSaavnRemainsTheFallbackWhenNoFlacSourceMatches() {
        val monochrome = FakeHighQualityProvider(id = "monochrome", lossless = true, supportsLookup = false)

        val resolution = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn()).resolveNow()

        resolution as HighQualityResolution.Selected
        assertEquals("jiosaavn", resolution.stream.providerId)
        assertEquals(AudioQualityTier.KBPS_320, resolution.stream.tier)
    }

    @Test
    fun stalledFlacProviderCannotHoldBackTheFallbackBeyondItsLaneBudget() {
        val stalled = losslessProvider("monochrome") { awaitCancellation() }
        val startedAt = System.nanoTime()

        val resolution = resolver(
            HighQualityAudioMode.PREFER_LOSSLESS,
            stalled,
            jioSaavn(),
            losslessLaneBudgetMs = 200L
        ).resolveNow()

        assertEquals("jiosaavn", (resolution as HighQualityResolution.Selected).stream.providerId)
        assertTrue((System.nanoTime() - startedAt) / 1_000_000L < 3_000L)
    }

    @Test
    fun selectedLosslessModeAllowsAResponsiveProviderToCompleteAfterTwoSeconds() {
        val monochrome = losslessProvider("monochrome") {
            delay(2_000L)
            ProviderStreamOutcome.Resolved(flacStream(it))
        }

        val resolution = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn()).resolveNow()

        assertEquals("monochrome", (resolution as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun flacProvidersAreSkippedOutsideLosslessMode() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }

        val resolution = resolver(HighQualityAudioMode.PREFER_320, monochrome, jioSaavn()).resolveNow()

        assertEquals("jiosaavn", (resolution as HighQualityResolution.Selected).stream.providerId)
        assertTrue(monochrome.searches.isEmpty())
    }

    @Test
    fun transientFlacFailureDoesNotNegativeCacheTheIdentity() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Failed(ProviderFailure.TIMEOUT) }
        val resolver = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, FakeHighQualityProvider())

        val first = resolver.resolveNow()

        assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, (first as HighQualityResolution.Fallback).reason)
        assertTrue(resolver.upgradePending("key"))
    }

    @Test
    fun temporaryFallbackDoesNotHideRecoveredLosslessForTheWholeStreamLifetime() = runBlocking {
        var now = 1_000_000L
        var recovered = false
        val monochrome = losslessProvider("monochrome") {
            if (recovered) ProviderStreamOutcome.Resolved(flacStream(it).copy(expiresAtMs = Long.MAX_VALUE))
            else ProviderStreamOutcome.Failed(ProviderFailure.RATE_LIMITED)
        }
        val jio = jioSaavn().apply {
            streamOutcome = { ProviderStreamOutcome.Resolved(resolvedStream(it, expiresAtMs = Long.MAX_VALUE)) }
        }
        val resolver = HighQualityAudioResolver(
            providers = listOf(monochrome, jio),
            mappingStore = HighQualityMappingStore(InMemoryMappingStorage()),
            scope = scope,
            clock = { now }
        ).apply { mode = HighQualityAudioMode.PREFER_LOSSLESS }
        val first = resolver.await(resolver.begin("key", query()), 3_000L) as HighQualityResolution.Selected
        assertEquals("jiosaavn", first.stream.providerId)
        assertEquals("jiosaavn", resolver.cachedSelection("key")!!.stream.providerId)

        recovered = true
        now += 90_001L

        assertNull(resolver.cachedSelection("key"))
        val retry = resolver.await(resolver.begin("key", query()), 3_000L) as HighQualityResolution.Selected
        assertEquals("monochrome", retry.stream.providerId)
    }

    @Test
    fun repeatedLaneTimeoutsOpenMonochromeCircuitButOneTimeoutDoesNot() = runBlocking {
        var requests = 0
        val monochrome = com.luc4n3x.levyra.data.hqaudio.lossless.MonochromeAudioProvider(
            exchange = ProviderHttpExchange {
                requests++
                awaitCancellation()
            }
        )
        val resolver = resolver(
            HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn(), losslessLaneBudgetMs = 100L
        )
        val first = resolver.await(resolver.begin("first", query()), 3_000L) as HighQualityResolution.Selected
        assertEquals("jiosaavn", first.stream.providerId)
        assertEquals("CLOSED", monochrome.health().single().state)
        assertEquals("LOOKUP_TIMEOUT", monochrome.health().single().lastFailure)

        for (key in listOf("second", "third")) {
            resolver.await(resolver.begin(key, query()), 3_000L)
        }
        assertEquals("OPEN", monochrome.health().single().state)

        val next = resolver.await(resolver.begin("next", query()), 3_000L) as HighQualityResolution.Selected
        assertEquals("jiosaavn", next.stream.providerId)
        assertTrue(requests in 1..3)
        val requestsAtOpen = requests
        resolver.await(resolver.begin("still-open", query()), 3_000L)
        assertEquals(requestsAtOpen, requests)
    }

    @Test
    fun cancellingAnObsoleteLookupDoesNotOpenMonochromeCircuit() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val monochrome = com.luc4n3x.levyra.data.hqaudio.lossless.MonochromeAudioProvider(
            exchange = ProviderHttpExchange {
                started.complete(Unit)
                awaitCancellation()
            }
        )
        val resolver = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn())
        val pending = resolver.begin("key", query())
        withTimeout(3_000L) { started.await() }

        resolver.mode = HighQualityAudioMode.PREFER_320
        pending.join()

        assertEquals("CLOSED", monochrome.health().single().state)
    }

    @Test
    fun choosingLosslessInvalidatesACached320Selection() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val resolver = resolver(HighQualityAudioMode.PREFER_320, monochrome, jioSaavn())
        assertEquals("jiosaavn", (resolver.resolveNow() as HighQualityResolution.Selected).stream.providerId)

        resolver.mode = HighQualityAudioMode.PREFER_LOSSLESS

        assertNull(resolver.cachedSelection("key"))
        assertEquals("monochrome", (resolver.resolveNow() as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun choosingLosslessClearsAMissFromThe320Provider() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val resolver = resolver(HighQualityAudioMode.PREFER_320, monochrome, FakeHighQualityProvider())
        assertEquals(HighQualityFallbackReason.NO_MATCH, (resolver.resolveNow() as HighQualityResolution.Fallback).reason)

        resolver.mode = HighQualityAudioMode.PREFER_LOSSLESS

        assertEquals("monochrome", (resolver.resolveNow() as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun choosingLosslessCancelsALookupStartedIn320Mode() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val jio = jioSaavn().apply { streamOutcome = { started.complete(Unit); awaitCancellation() } }
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val resolver = resolver(HighQualityAudioMode.PREFER_320, monochrome, jio)
        val pending = resolver.begin("key", query())
        withTimeout(3_000L) { started.await() }

        resolver.mode = HighQualityAudioMode.PREFER_LOSSLESS

        assertTrue(pending.isCancelled)
        val result = resolver.await(resolver.begin("key", query()), 3_000L)
        assertEquals("monochrome", (result as HighQualityResolution.Selected).stream.providerId)
    }

    @Test
    fun flacPlaybackFailureFallsThroughToTheNextProviderForTheSameTrack() {
        val monochrome = losslessProvider("monochrome") { ProviderStreamOutcome.Resolved(flacStream(it)) }
        val resolver = resolver(HighQualityAudioMode.PREFER_LOSSLESS, monochrome, jioSaavn())
        resolver.resolveNow()

        resolver.reportPlaybackFailure("key", "monochrome-1", "HTTP 429", "monochrome")

        assertNull(resolver.cachedSelection("key"))
        val retry = resolver.resolveNow()
        assertEquals("jiosaavn", (retry as HighQualityResolution.Selected).stream.providerId)
    }
}
