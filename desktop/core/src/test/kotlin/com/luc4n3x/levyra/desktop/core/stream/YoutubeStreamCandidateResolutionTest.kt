package com.luc4n3x.levyra.desktop.core.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeStreamCandidateResolutionTest {
    private val opus = AudioCandidate(url = "https://stream/opus", mimeType = "audio/webm", itag = 251)
    private val aac = AudioCandidate(url = "https://stream/aac", mimeType = "audio/mp4", itag = 140)

    @Test
    fun retriesNextCandidateAfterProbeFailure() {
        val probes = ArrayList<String>()

        val resolution = selectVerifiedCandidate(
            rankedCandidates = listOf(opus, aac),
            isRejected = { false },
            isFresh = { true },
            verify = { url ->
                probes += url
                url == aac.url
            }
        )

        assertEquals(aac, resolution.candidate)
        assertEquals(listOf(opus.url, aac.url), probes)
        assertNull(resolution.failure)
    }

    @Test
    fun reportsExpiredWhenEveryEligibleCandidateIsStale() {
        val resolution = selectVerifiedCandidate(
            rankedCandidates = listOf(opus, aac),
            isRejected = { false },
            isFresh = { false },
            verify = { error("expired candidates must not be probed") }
        )

        assertNull(resolution.candidate)
        assertEquals(CandidateResolutionFailure.STREAM_EXPIRED, resolution.failure)
    }

    @Test
    fun rejectedCandidateIsNotRetried() {
        val probes = ArrayList<String>()

        val resolution = selectVerifiedCandidate(
            rankedCandidates = listOf(opus),
            isRejected = { true },
            isFresh = { true },
            verify = { url -> probes += url; true }
        )

        assertNull(resolution.candidate)
        assertEquals(CandidateResolutionFailure.RESOLVE_FAILED, resolution.failure)
        assertEquals(emptyList<String>(), probes)
    }

    @Test
    fun reportsProbeFailureAfterAllFreshCandidatesFail() {
        val resolution = selectVerifiedCandidate(
            rankedCandidates = listOf(opus, aac),
            isRejected = { false },
            isFresh = { true },
            verify = { false }
        )

        assertNull(resolution.candidate)
        assertEquals(CandidateResolutionFailure.STREAM_PROBE_FAILED, resolution.failure)
    }
}
