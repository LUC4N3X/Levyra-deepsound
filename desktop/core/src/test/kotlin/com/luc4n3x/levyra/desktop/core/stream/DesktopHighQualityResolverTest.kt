package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.data.hqaudio.*
import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopHighQualityResolverTest {
    @Test fun refreshRenewsTheUrlWithoutQuarantiningTheMatchOrDiscardingItsIdentity() = runTest {
        var requests = 0
        val provider = FakeHighQualityProvider(
            searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
            lookupOutcome = { ProviderLookupOutcome.Found(candidate()) },
            streamOutcome = { ProviderStreamOutcome.Resolved(resolvedStream(it).copy(url = "https://aac.saavncdn.com/song.mp4?request=${++requests}")) }
        )
        val shared = HighQualityAudioResolver(provider, HighQualityMappingStore(InMemoryMappingStorage()), backgroundScope)
        val original = OriginalResolver()
        val adapter = DesktopHighQualityResolver(original, shared, { HighQualityAudioMode.DEFAULT }, true, System::currentTimeMillis)
        val track = Track("original-id", "Blinding Lights", "The Weeknd", "After Hours", "", durationMs = 200_000)
        val first = adapter.resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO)
        adapter.refresh(track)
        val renewed = adapter.resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO)
        assertNotEquals(first.url, renewed.url)
        assertEquals(first.sourceIdentity, renewed.sourceIdentity)
        assertEquals(first.label, renewed.label)
        assertEquals(2, requests)
        assertEquals(1, provider.searches.size)
        assertEquals(1, original.refreshes)
        assertEquals(0, original.failures)
        assertEquals(0, original.resolutions)
    }

    @Test fun genuinePlaybackFailureStillQuarantinesTheFailedProviderTrack() = runTest {
        val provider = exactProvider()
        val shared = HighQualityAudioResolver(provider, HighQualityMappingStore(InMemoryMappingStorage()), backgroundScope)
        val original = OriginalResolver()
        val adapter = DesktopHighQualityResolver(original, shared, { HighQualityAudioMode.DEFAULT }, false, System::currentTimeMillis)
        val track = Track("original-id", "Blinding Lights", "The Weeknd", "After Hours", "", durationMs = 200_000)
        adapter.resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO)
        adapter.invalidate(track)
        val failure = runCatching { adapter.resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO) }.exceptionOrNull()
        assertTrue(failure is StreamResolutionException)
        assertEquals(1, provider.streamRequests.size)
        assertEquals(1, original.failures)
        assertEquals(1, original.resolutions)
    }

    @Test fun refreshingAnInFlightLookupCancelsItBeforeTheNewSelectionIsCached() = runTest {
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val provider = exactProvider().apply {
            streamOutcome = {
                val request = ++requests
                if (request == 1) release.await()
                ProviderStreamOutcome.Resolved(resolvedStream(it).copy(url = "https://aac.saavncdn.com/song.mp4?request=$request"))
            }
        }
        val shared = HighQualityAudioResolver(provider, HighQualityMappingStore(InMemoryMappingStorage()), backgroundScope)
            .apply { mode = HighQualityAudioMode.DEFAULT }
        val obsolete = shared.begin("one", query())
        runCurrent()
        assertEquals(1, requests)
        shared.refreshStream("one")
        val renewed = shared.await(shared.begin("one", query()), 1_200L) as HighQualityResolution.Selected
        release.complete(Unit)
        runCurrent()
        assertTrue(obsolete.isCancelled)
        assertTrue(renewed.stream.url.endsWith("request=2"))
        assertEquals(renewed, shared.cachedSelection("one"))
    }

    private fun exactProvider() = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
        lookupOutcome = { ProviderLookupOutcome.Found(candidate()) }
    )

    private class OriginalResolver : StreamResolver {
        var resolutions = 0
        var refreshes = 0
        var failures = 0
        override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
            resolutions++
            throw StreamResolutionException("Original unavailable")
        }
        override fun refresh(track: Track) { refreshes++ }
        override fun invalidate(track: Track) { failures++ }
    }
}
