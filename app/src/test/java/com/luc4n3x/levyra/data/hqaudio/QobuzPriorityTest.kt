package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class QobuzPriorityTest {
    private fun provider(id: String, outcome: suspend (AlternativeTrackCandidate) -> ProviderStreamOutcome) = FakeHighQualityProvider(
        id = id, lossless = true, supportsLookup = false,
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate(id = "123").copy(providerId = id))) },
        streamOutcome = outcome
    )
    private fun stream(c: AlternativeTrackCandidate) = ProviderStreamOutcome.Resolved(resolvedStream(c, AudioQualityTier.LOSSLESS).copy(codec = "flac"))

    @Test fun qobuzWinsOnlyAfterVerificationAndLeavesTidalFallbackIntact() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            for (failure in listOf<ProviderStreamOutcome?>(null, ProviderStreamOutcome.Failed(ProviderFailure.FORBIDDEN), ProviderStreamOutcome.Failed(ProviderFailure.RATE_LIMITED), ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NOT_AUDIO)))) {
                val qobuz = provider("qobuz") { failure ?: stream(it) }
                val tidal = provider("tidal") { delay(50); stream(it) }
                val resolver = HighQualityAudioResolver(listOf(qobuz, tidal), HighQualityMappingStore(InMemoryMappingStorage()), scope).apply { mode = HighQualityAudioMode.PREFER_LOSSLESS }
                val result = resolver.await(resolver.begin("key", query()), 3_000) as HighQualityResolution.Selected
                assertEquals(if (failure == null) "qobuz" else "tidal", result.stream.providerId)
                if (failure == null) {
                    resolver.reportPlaybackFailure("key", "123", "synthetic", "qobuz")
                    assertEquals("tidal", (resolver.await(resolver.begin("key", query()), 3_000) as HighQualityResolution.Selected).stream.providerId)
                }
            }
        } finally { scope.cancel() }
    }

    @Test fun stalledQobuzDoesNotDelayReadyTidalOrConsumeItsBudget() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val qobuz = provider("qobuz") { awaitCancellation() }
            val tidal = provider("tidal") { stream(it) }
            val resolver = HighQualityAudioResolver(listOf(qobuz, tidal), HighQualityMappingStore(InMemoryMappingStorage()), scope).apply { mode = HighQualityAudioMode.PREFER_LOSSLESS }
            val result = withTimeout(500) { resolver.begin("key", query()).await() }
            assertEquals("tidal", (result as HighQualityResolution.Selected).stream.providerId)
        } finally { scope.cancel() }
    }

    @Test fun disabledQobuzAnd320ModeMakeNoQobuzRequests() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            for (mode in listOf(HighQualityAudioMode.PREFER_LOSSLESS, HighQualityAudioMode.PREFER_320)) {
                val qobuz = provider("qobuz") { error("Disabled provider invoked") }
                val tidal = provider("tidal") { stream(it) }
                val jio = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) }, supportsLookup = false)
                val resolver = HighQualityAudioResolver(listOf(qobuz, tidal, jio), HighQualityMappingStore(InMemoryMappingStorage()), scope).apply {
                    this.mode = mode; qobuzEnabled = false
                }
                val result = resolver.begin("key", query()).await() as HighQualityResolution.Selected
                assertEquals(if (mode.lossless) "tidal" else "jiosaavn", result.stream.providerId)
                assertTrue(qobuz.searches.isEmpty())
            }
        } finally { scope.cancel() }
    }

    @Test fun disablingQobuzEvictsItsSelectionWithoutQuarantiningTheTrack() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val resolver = HighQualityAudioResolver(listOf(provider("qobuz") { stream(it) }, provider("tidal") { delay(50); stream(it) }), HighQualityMappingStore(InMemoryMappingStorage()), scope).apply { mode = HighQualityAudioMode.PREFER_LOSSLESS }
            assertEquals("qobuz", (resolver.begin("key", query()).await() as HighQualityResolution.Selected).stream.providerId)
            resolver.qobuzEnabled = false
            assertNull(resolver.cachedSelection("key"))
            assertEquals("tidal", (resolver.begin("key", query()).await() as HighQualityResolution.Selected).stream.providerId)
        } finally { scope.cancel() }
    }
}
