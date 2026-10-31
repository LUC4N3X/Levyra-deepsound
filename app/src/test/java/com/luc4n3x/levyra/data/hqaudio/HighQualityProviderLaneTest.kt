package com.luc4n3x.levyra.data.hqaudio

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityProviderLaneTest {
    private fun provider(
        candidates: List<AlternativeTrackCandidate>,
        lossless: Boolean = true,
        outcome: suspend (AlternativeTrackCandidate) -> ProviderStreamOutcome
    ) = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(candidates) },
        streamOutcome = outcome,
        lossless = lossless,
        supportsLookup = false
    )

    private fun lane(provider: HighQualityAudioProvider) = HighQualityProviderLane(
        provider = provider,
        mappingStore = HighQualityMappingStore(InMemoryMappingStorage()),
        matcher = AlternativeTrackMatcher(),
        clock = System::currentTimeMillis,
        isTrackQuarantined = { false }
    )

    @Test
    fun unavailableReleaseTriesAnotherMatchedCopyOfTheSameRecording() = runBlocking {
        val provider = provider(listOf(candidate(id = "first"), candidate(id = "second"))) {
            if (it.providerTrackId == "first") ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
            else ProviderStreamOutcome.Resolved(resolvedStream(it, AudioQualityTier.LOSSLESS))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Selected

        assertEquals("second", result.stream.providerTrackId)
        assertEquals(listOf("first", "second"), provider.streamRequests.toList())
        assertEquals(1, provider.searches.size)
        assertTrue(provider.lookups.isEmpty())
    }

    @Test
    fun lossyLaneDoesNotRetryAnotherCopyAfterSelectedStreamIsUnavailable() = runBlocking {
        val provider = provider(listOf(candidate(id = "first"), candidate(id = "second")), lossless = false) {
            if (it.providerTrackId == "first") ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
            else ProviderStreamOutcome.Resolved(resolvedStream(it))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Fallback

        assertEquals(HighQualityFallbackReason.STREAM_UNAVAILABLE, result.reason)
        assertEquals(listOf("first"), provider.streamRequests.toList())
    }

    @Test
    fun candidateRetriesStopAtThreeStreamsWithoutSearchingAgain() = runBlocking {
        val provider = provider((1..6).map { candidate(id = "release-$it") }) {
            ProviderStreamOutcome.Unavailable(listOf(StreamRejection.UNSUPPORTED_CONTAINER))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Fallback

        assertEquals(HighQualityFallbackReason.STREAM_UNAVAILABLE, result.reason)
        assertEquals(listOf("release-1", "release-2", "release-3"), provider.streamRequests.toList())
        assertEquals(1, provider.searches.size)
    }

    @Test
    fun providerFailuresNeverFanOutAcrossReleaseCandidates() = runBlocking {
        for (failure in listOf(ProviderFailure.RATE_LIMITED, ProviderFailure.NETWORK, ProviderFailure.TIMEOUT)) {
            val provider = provider(listOf(candidate(id = "first"), candidate(id = "second"))) {
                ProviderStreamOutcome.Failed(failure)
            }

            val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Fallback

            assertEquals(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, result.reason)
            assertEquals(failure.name, result.detail)
            assertEquals(listOf("first"), provider.streamRequests.toList())
        }
    }

    @Test
    fun acceptedButDifferentRecordingIsNeverAStreamFallback() = runBlocking {
        val provider = provider(listOf(candidate(id = "exact"), candidate(id = "different", duration = 205))) {
            ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Fallback

        assertEquals(HighQualityFallbackReason.STREAM_UNAVAILABLE, result.reason)
        assertEquals(listOf("exact"), provider.streamRequests.toList())
    }

    @Test
    fun unavailableStreamDoesNotRelaxVersionArtistOrIsrcMatching() = runBlocking {
        val provider = provider(listOf(
            candidate(id = "exact", isrc = "USAA10000001"),
            candidate(id = "live", title = "Blinding Lights (Live)", isrc = "USAA10000001"),
            candidate(id = "cover", primary = listOf("Other Artist"), isrc = "USAA10000001"),
            candidate(id = "different-isrc", isrc = "USAA10000002")
        )) { ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA)) }

        lane(provider).resolve("identity", query(isrc = "USAA10000001"))

        assertEquals(listOf("exact"), provider.streamRequests.toList())
    }

    @Test
    fun retryRejectsConflictingCandidateIsrcWhenQueryIsrcIsUnknown() = runBlocking {
        val provider = provider(listOf(
            candidate(id = "first", isrc = "USAA10000001"),
            candidate(id = "different-isrc", isrc = "USAA10000002")
        )) {
            if (it.providerTrackId == "first") ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
            else ProviderStreamOutcome.Resolved(resolvedStream(it, AudioQualityTier.LOSSLESS))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Fallback

        assertEquals(HighQualityFallbackReason.STREAM_UNAVAILABLE, result.reason)
        assertEquals(listOf("first"), provider.streamRequests.toList())
    }

    @Test
    fun retryAcceptsEquivalentIsrcFormattingWhenQueryIsrcIsUnknown() = runBlocking {
        val provider = provider(listOf(
            candidate(id = "first", isrc = "USAA10000001"),
            candidate(id = "same-isrc", isrc = "us-aa1-00-00001")
        )) {
            if (it.providerTrackId == "first") ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
            else ProviderStreamOutcome.Resolved(resolvedStream(it, AudioQualityTier.LOSSLESS))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Selected

        assertEquals("same-isrc", result.stream.providerTrackId)
        assertEquals(listOf("first", "same-isrc"), provider.streamRequests.toList())
    }

    @Test
    fun ambiguousRecordingsNeverTriggerAStreamRequest() = runBlocking {
        val provider = provider(listOf(candidate(id = "short", duration = 197), candidate(id = "long", duration = 203))) {
            ProviderStreamOutcome.Resolved(resolvedStream(it, AudioQualityTier.LOSSLESS))
        }

        val result = lane(provider).resolve("identity", query()) as HighQualityResolution.Fallback

        assertEquals(HighQualityFallbackReason.AMBIGUOUS, result.reason)
        assertTrue(provider.streamRequests.isEmpty())
    }

    @Test
    fun cancellationDuringFirstStreamPreventsFurtherAttempts() = runBlocking {
        val provider = provider(listOf(candidate(id = "first"), candidate(id = "second"))) { awaitCancellation() }

        val result = withTimeoutOrNull(100L) { lane(provider).resolve("identity", query()) }

        assertNull(result)
        assertEquals(listOf("first"), provider.streamRequests.toList())
    }
}
