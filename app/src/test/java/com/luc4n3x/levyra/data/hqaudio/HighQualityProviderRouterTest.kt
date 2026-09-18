package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityProviderRouterTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val storage = InMemoryMappingStorage()
    private val identity = "playback-key-v2|track:blinding-lights"
    private var nowMs = System.currentTimeMillis()

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun jioSaavn() = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
        lookupOutcome = { ProviderLookupOutcome.Found(candidate()) }
    )

    private fun qobuzCandidate(id: String = "9001") = candidate(id = id, providerId = "qobuz", offers320 = true)

    private fun qobuz(streamOf: (AlternativeTrackCandidate) -> ProviderStreamOutcome = {
        ProviderStreamOutcome.Resolved(losslessStream(it))
    }) = FakeHighQualityProvider(
        id = "qobuz",
        displayName = "Qobuz",
        losslessCapable = true,
        supportsLookup = false,
        searchOutcome = { ProviderSearchOutcome.Found(listOf(qobuzCandidate())) },
        streamOutcome = { streamOf(it) }
    )

    private fun router(
        vararg providers: FakeHighQualityProvider,
        mode: HighQualityAudioMode = HighQualityAudioMode.AUTOMATIC,
        lookupBudgetMs: Long = 5_000L,
        providerBudgetMs: Long = 3_000L,
        hedgeDelayMs: Long = HighQualityAudioResolver.HEDGE_DELAY_MS
    ) = HighQualityAudioResolver(
        providers = providers.toList(),
        mappingStore = HighQualityMappingStore(storage, clock = { nowMs }),
        scope = scope,
        clock = { nowMs },
        lookupBudgetMs = lookupBudgetMs,
        providerBudgetMs = providerBudgetMs,
        hedgeDelayMs = hedgeDelayMs
    ).apply { this.mode = mode }

    private fun HighQualityAudioResolver.resolveNow(query: AlternativeTrackQuery = query()): HighQualityResolution =
        runBlocking { await(begin(identity, query), 8_000L) }

    private fun HighQualityResolution.selectedProvider(): String =
        (this as HighQualityResolution.Selected).stream.providerId

    @Test
    fun jioSaavnHitIsSelectedWithoutContactingQobuz() {
        val jio = jioSaavn()
        val qobuz = qobuz()
        assertEquals("jiosaavn", router(jio, qobuz).resolveNow().selectedProvider())
        assertTrue(qobuz.searches.isEmpty())
    }

    @Test
    fun jioSaavnMissFallsThroughToQobuz() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val result = router(jio, qobuz()).resolveNow()
        assertEquals("qobuz", result.selectedProvider())
        assertTrue((result as HighQualityResolution.Selected).stream.quality.lossless)
    }

    @Test
    fun jioSaavnStreamFailureFallsThroughToQobuz() {
        val jio = jioSaavn().apply { streamOutcome = { ProviderStreamOutcome.Unavailable(listOf(StreamRejection.HTTP_STATUS)) } }
        assertEquals("qobuz", router(jio, qobuz()).resolveNow().selectedProvider())
    }

    @Test
    fun jioSaavnOutageFallsThroughToQobuz() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.HTTP_ERROR) })
        assertEquals("qobuz", router(jio, qobuz()).resolveNow().selectedProvider())
    }

    @Test
    fun jioSaavnTimeoutFallsThroughToQobuz() {
        val jio = FakeHighQualityProvider(searchOutcome = {
            delay(10_000L)
            ProviderSearchOutcome.Found(listOf(candidate()))
        })
        val result = router(jio, qobuz(), providerBudgetMs = 200L, hedgeDelayMs = 0L).resolveNow()
        assertEquals("qobuz", result.selectedProvider())
    }

    @Test
    fun circuitOpenProviderIsSkippedImmediately() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.CIRCUIT_OPEN) })
        val started = System.nanoTime()
        val result = router(jio, qobuz()).resolveNow()
        assertEquals("qobuz", result.selectedProvider())
        assertTrue((System.nanoTime() - started) / 1_000_000L < 1_000L)
    }

    @Test
    fun bothProvidersFailingReturnsCombinedFallback() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz().apply { searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.CIRCUIT_OPEN) } }
        val result = router(jio, qobuz).resolveNow() as HighQualityResolution.Fallback
        assertEquals(HighQualityFallbackReason.NO_MATCH, result.reason)
        assertTrue(result.detail.contains("jiosaavn:NO_MATCH"))
        assertTrue(result.detail.contains("qobuz:PROVIDER_UNAVAILABLE/CIRCUIT_OPEN"))
    }

    @Test
    fun providerRecoversOnLaterLookup() {
        var healthy = false
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz().apply {
            searchOutcome = {
                if (healthy) ProviderSearchOutcome.Found(listOf(qobuzCandidate())) else ProviderSearchOutcome.Failed(ProviderFailure.NETWORK)
            }
        }
        val router = router(jio, qobuz)
        assertTrue(router.resolveNow() is HighQualityResolution.Fallback)
        healthy = true
        assertEquals("qobuz", router.resolveNow().selectedProvider())
    }

    @Test
    fun qobuzPlaybackFailureDoesNotQuarantineTheSongForJioSaavn() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val router = router(jio, qobuz())
        assertEquals("qobuz", router.resolveNow().selectedProvider())
        router.reportPlaybackFailure(identity, "qobuz", "9001", "HTTP 403")
        assertNull(router.cachedSelection(identity))
        jio.searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) }
        assertEquals("jiosaavn", router.resolveNow().selectedProvider())
    }

    @Test
    fun quarantinedProviderIsSkippedButOthersStillRun() {
        val jio = jioSaavn()
        val qobuz = qobuz()
        val router = router(jio, qobuz, mode = HighQualityAudioMode.PREFER_320)
        assertEquals("qobuz", router.resolveNow().selectedProvider())
        router.reportPlaybackFailure(identity, "qobuz", "9001", "decoder")
        val searchesBefore = qobuz.searches.size
        assertEquals("jiosaavn", router.resolveNow().selectedProvider())
        assertEquals(searchesBefore, qobuz.searches.size)
    }

    @Test
    fun onlyTheFailingProvidersMappingIsInvalidated() {
        router(jioSaavn()).resolveNow()
        router(qobuz()).resolveNow()
        assertEquals(2, storage.values.size)
        val result = router(
            jioSaavn(),
            qobuz(streamOf = { ProviderStreamOutcome.Unavailable(listOf(StreamRejection.HTTP_STATUS)) }),
            mode = HighQualityAudioMode.PREFER_320
        ).resolveNow()
        assertEquals("jiosaavn", result.selectedProvider())
        assertEquals(1, storage.values.size)
        assertTrue(storage.values.keys.single().startsWith("hq-v2:jiosaavn:"))
    }

    @Test
    fun maximumQualityPrefersLosslessEvenWhenJioSaavnIsFaster() {
        val jio = jioSaavn()
        val qobuz = qobuz().apply {
            searchOutcome = {
                delay(150L)
                ProviderSearchOutcome.Found(listOf(qobuzCandidate()))
            }
        }
        val result = router(jio, qobuz, mode = HighQualityAudioMode.PREFER_320).resolveNow()
        assertEquals("qobuz", result.selectedProvider())
        assertEquals(listOf(HighQualityPreference.MAXIMUM), qobuz.preferences.toList())
    }

    @Test
    fun deadPreferredProviderOnlyDelaysAReadyStreamByTheUpgradeGrace() {
        val qobuzFinished = AtomicBoolean(false)
        val qobuz = qobuz().apply {
            searchOutcome = {
                delay(2_500L)
                qobuzFinished.set(true)
                ProviderSearchOutcome.Failed(ProviderFailure.TIMEOUT)
            }
        }
        val started = System.nanoTime()
        val result = router(jioSaavn(), qobuz, mode = HighQualityAudioMode.PREFER_320).resolveNow()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        assertEquals("jiosaavn", result.selectedProvider())
        assertTrue("elapsed=$elapsedMs", elapsedMs < HighQualityAudioResolver.UPGRADE_GRACE_MS + 800L)
        runBlocking { delay(2_000L) }
        assertTrue(qobuzFinished.get())
    }

    @Test
    fun maximumQualityFallsBackToJioSaavnWhenQobuzFails() {
        val qobuz = qobuz().apply { searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.FORBIDDEN) } }
        assertEquals("jiosaavn", router(jioSaavn(), qobuz, mode = HighQualityAudioMode.PREFER_320).resolveNow().selectedProvider())
    }

    @Test
    fun automaticModeUpgradesWeakJioSaavnStreamToLossless() {
        val jio = jioSaavn().apply {
            streamOutcome = { ProviderStreamOutcome.Resolved(resolvedStream(it, AudioQualityTier.KBPS_160)) }
        }
        val result = router(jio, qobuz()).resolveNow()
        assertEquals("qobuz", result.selectedProvider())
    }

    @Test
    fun automaticModeKeepsWeakJioSaavnStreamWhenQobuzCannotHelp() {
        val jio = jioSaavn().apply {
            streamOutcome = { ProviderStreamOutcome.Resolved(resolvedStream(it, AudioQualityTier.KBPS_160)) }
        }
        val qobuz = qobuz().apply { searchOutcome = { ProviderSearchOutcome.Found(emptyList()) } }
        val result = router(jio, qobuz).resolveNow() as HighQualityResolution.Selected
        assertEquals("jiosaavn", result.stream.providerId)
        assertEquals(160, result.stream.quality.effectiveKbps)
    }

    @Test
    fun automaticModeUsesBalancedPreference() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz()
        router(jio, qobuz).resolveNow()
        assertEquals(listOf(HighQualityPreference.BALANCED), qobuz.preferences.toList())
    }

    @Test
    fun slowJioSaavnHedgesIntoQobuzWithoutWaitingForItsFullBudget() {
        val jio = FakeHighQualityProvider(searchOutcome = {
            delay(10_000L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val qobuz = qobuz()
        val started = System.nanoTime()
        val result = router(jio, qobuz, providerBudgetMs = 1_500L, hedgeDelayMs = 100L).resolveNow()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        assertEquals("qobuz", result.selectedProvider())
        assertTrue("elapsed=$elapsedMs", elapsedMs < 2_500L)
    }

    @Test
    fun wholeRouteIsBoundedByLookupBudget() {
        val slow: suspend (String) -> ProviderSearchOutcome = {
            delay(10_000L)
            ProviderSearchOutcome.Found(emptyList())
        }
        val started = System.nanoTime()
        val result = router(
            FakeHighQualityProvider(searchOutcome = slow),
            qobuz().apply { searchOutcome = slow },
            lookupBudgetMs = 300L
        ).resolveNow()
        assertEquals(HighQualityFallbackReason.TIMEOUT, (result as HighQualityResolution.Fallback).reason)
        assertTrue((System.nanoTime() - started) / 1_000_000L < 2_000L)
    }

    @Test
    fun duplicateLookupsForTheSameTrackShareOneRoute() {
        val jio = FakeHighQualityProvider(searchOutcome = {
            delay(100L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val qobuz = qobuz()
        val router = router(jio, qobuz)
        val first = router.begin(identity, query())
        val second = router.begin(identity, query())
        assertTrue(first === second)
        runBlocking { router.await(first, 5_000L) }
        assertEquals(1, qobuz.searches.size)
        assertEquals(AlternativeSearchPlan.queries(query()).size, jio.searches.size)
    }

    @Test
    fun cancellingTheLookupCancelsEveryProviderLane() {
        val jioCancelled = AtomicBoolean(false)
        val qobuzCancelled = AtomicBoolean(false)
        fun hanging(flag: AtomicBoolean): suspend (String) -> ProviderSearchOutcome = {
            try {
                awaitCancellation()
            } finally {
                flag.set(true)
            }
        }
        val router = router(
            FakeHighQualityProvider(searchOutcome = hanging(jioCancelled)),
            qobuz().apply { searchOutcome = hanging(qobuzCancelled) },
            mode = HighQualityAudioMode.PREFER_320
        )
        val pending = router.begin(identity, query())
        runBlocking {
            delay(100L)
            pending.cancel()
            pending.join()
            delay(50L)
        }
        assertTrue(jioCancelled.get())
        assertTrue(qobuzCancelled.get())
        val retry = router.begin(identity, query())
        assertFalse(retry === pending)
        retry.cancel()
    }

    @Test
    fun deadProviderIsCalledOncePerLookup() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz().apply { searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.NETWORK) } }
        router(jio, qobuz).resolveNow()
        assertEquals(1, qobuz.searches.size)
    }

    @Test
    fun cachedQobuzMappingIsReusedWithoutSearching() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        router(jio, qobuz()).resolveNow()
        val nextJio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val nextQobuz = qobuz()
        assertEquals("qobuz", router(nextJio, nextQobuz).resolveNow().selectedProvider())
        assertTrue(nextQobuz.searches.isEmpty())
        assertEquals(listOf("9001"), nextQobuz.streamRequests.toList())
    }

    @Test
    fun staleQobuzMappingIsDroppedWhenItsStreamDisappears() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        router(jio, qobuz()).resolveNow()
        val gone = qobuz(streamOf = { ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA)) })
        router(FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) }), gone).resolveNow()
        assertTrue(storage.values.keys.none { it.startsWith("hq-v2:qobuz:") })
        val fresh = qobuz()
        router(FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) }), fresh).resolveNow()
        assertTrue(fresh.searches.isNotEmpty())
    }

    @Test
    fun wrongVersionOnQobuzIsNeverSelected() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz().apply {
            searchOutcome = {
                ProviderSearchOutcome.Found(
                    listOf(
                        candidate(id = "live", providerId = "qobuz", title = "Blinding Lights (Live)"),
                        candidate(id = "remix", providerId = "qobuz", title = "Blinding Lights (Major Lazer Remix)"),
                        candidate(id = "remaster", providerId = "qobuz", title = "Blinding Lights", album = "After Hours (Remastered)"),
                        candidate(id = "long", providerId = "qobuz", duration = 230)
                    )
                )
            }
        }
        val result = router(jio, qobuz).resolveNow()
        assertTrue(result is HighQualityResolution.Fallback)
        assertTrue(qobuz.streamRequests.isEmpty())
    }

    @Test
    fun ambiguousQobuzResultsAreRejected() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz().apply {
            searchOutcome = {
                ProviderSearchOutcome.Found(
                    listOf(
                        candidate(id = "a", providerId = "qobuz", duration = 200),
                        candidate(id = "b", providerId = "qobuz", duration = 202)
                    )
                )
            }
        }
        val result = router(jio, qobuz).resolveNow(query(durationMs = 201_000L)) as HighQualityResolution.Fallback
        assertTrue(result.detail.contains("qobuz:AMBIGUOUS"))
        assertTrue(qobuz.streamRequests.isEmpty())
    }

    @Test
    fun exactIsrcOnQobuzIsDecisiveOnFirstPass() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz().apply {
            searchOutcome = {
                ProviderSearchOutcome.Found(
                    listOf(candidate(id = "isrc", providerId = "qobuz", album = "The Highlights", isrc = "USUG11904206"))
                )
            }
        }
        val result = router(jio, qobuz).resolveNow(query(isrc = "USUG11904206"))
        assertEquals(100, (result as HighQualityResolution.Selected).evaluation.confidence)
        assertEquals(1, qobuz.searches.size)
    }

    @Test
    fun manualPinResolvesAmbiguousTrackAndStaysProviderScoped() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz()
        val router = router(jio, qobuz)
        val pinned = candidate(id = "pinned", providerId = "qobuz", album = "Some Compilation")
        assertTrue(router.pinManualMatch(identity, query(), pinned))
        val result = router.resolveNow()
        assertEquals("pinned", (result as HighQualityResolution.Selected).stream.providerTrackId)
        assertTrue(qobuz.searches.isEmpty())
        assertTrue(storage.values.keys.all { it.startsWith("hq-v2:qobuz:") })
    }

    @Test
    fun manualPinCannotForceDifferentRecording() {
        val router = router(jioSaavn(), qobuz())
        assertFalse(router.pinManualMatch(identity, query(), candidate(id = "live", providerId = "qobuz", title = "Blinding Lights (Live)")))
        assertFalse(router.pinManualMatch(identity, query(), candidate(id = "long", providerId = "qobuz", duration = 260)))
        assertFalse(router.pinManualMatch(identity, query(), candidate(id = "x", providerId = "unknown")))
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun cachedStreamExpiresAndIsNotServed() {
        val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) })
        val qobuz = qobuz(streamOf = { ProviderStreamOutcome.Resolved(losslessStream(it, expiresAtMs = nowMs + 600_000L)) })
        val router = router(jio, qobuz)
        router.resolveNow()
        assertNotNull(router.cachedSelection(identity))
        nowMs += 600_000L - HighQualityAudioResolver.STREAM_REFRESH_MARGIN_MS
        assertNull(router.cachedSelection(identity))
    }

    @Test
    fun switchingModeDropsStreamsChosenUnderAnotherPreference() {
        val router = router(jioSaavn(), qobuz())
        router.resolveNow()
        assertNotNull(router.cachedSelection(identity))
        router.mode = HighQualityAudioMode.PREFER_320
        assertNull(router.cachedSelection(identity))
    }
}
