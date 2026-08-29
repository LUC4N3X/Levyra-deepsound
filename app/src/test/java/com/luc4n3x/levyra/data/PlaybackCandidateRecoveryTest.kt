package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCandidateRecoveryTest {
    private val now = 1_000_000L

    @Test
    fun onlyTransientCandidateStatusesAreTreatedAsCandidateFailures() {
        listOf(
            PlaybackFailureKind.NotFound,
            PlaybackFailureKind.ServerError,
            PlaybackFailureKind.Truncated
        ).forEach { assertTrue(it.name, isCandidateLevelPlaybackFailure(it)) }

        listOf(
            PlaybackFailureKind.Forbidden,
            PlaybackFailureKind.Signature,
            PlaybackFailureKind.LoginRequired,
            PlaybackFailureKind.ExpiredUrl,
            PlaybackFailureKind.Decoder,
            PlaybackFailureKind.UnsupportedFormat,
            PlaybackFailureKind.Network,
            PlaybackFailureKind.Timeout,
            PlaybackFailureKind.RangeNotSatisfiable,
            PlaybackFailureKind.Unknown
        ).forEach { assertFalse(it.name, isCandidateLevelPlaybackFailure(it)) }
    }

    @Test
    fun http503OnOneAudioUrlPromotesTheNextAudioCandidate() {
        val manifest = audioManifest()

        val promoted = promoteAlternatePlaybackCandidate(
            manifest = manifest,
            isVideoMode = false,
            nowMs = now,
            isBlocked = { it == "https://a/opus-high" }
        )

        assertEquals("https://a/aac", promoted?.selectedAudioUrl)
        assertEquals("", promoted?.selectedVideoUrl)
        assertEquals(
            listOf("https://a/aac"),
            promoted?.streams?.filter { it.selected }?.map { it.url }
        )
    }

    @Test
    fun promotionStopsWhenEveryEquivalentCandidateIsQuarantined() {
        val manifest = audioManifest()

        val promoted = promoteAlternatePlaybackCandidate(
            manifest = manifest,
            isVideoMode = false,
            nowMs = now,
            isBlocked = { true }
        )

        assertNull(promoted)
    }

    @Test
    fun promotionIgnoresExpiredCandidates() {
        val manifest = audioManifest(alternativeExpiresAtMs = now - 1L)

        val promoted = promoteAlternatePlaybackCandidate(
            manifest = manifest,
            isVideoMode = false,
            nowMs = now,
            isBlocked = { it == "https://a/opus-high" }
        )

        assertNull(promoted)
    }

    @Test
    fun healthySelectionIsNeverPromoted() {
        assertNull(
            promoteAlternatePlaybackCandidate(
                manifest = audioManifest(),
                isVideoMode = false,
                nowMs = now,
                isBlocked = { false }
            )
        )
    }

    @Test
    fun videoFailurePromotesTheClosestHeightAndKeepsHealthyAudio() {
        val manifest = videoManifest()

        val promoted = promoteAlternatePlaybackCandidate(
            manifest = manifest,
            isVideoMode = true,
            nowMs = now,
            isBlocked = { it == "https://v/av1-1080" }
        )

        assertEquals("https://a/aac", promoted?.selectedAudioUrl)
        assertEquals("https://v/vp9-1080", promoted?.selectedVideoUrl)
        assertEquals(
            setOf("https://a/aac", "https://v/vp9-1080"),
            promoted?.streams?.filter { it.selected }?.map { it.url }?.toSet()
        )
    }

    @Test
    fun videoFailureFallsBackToALowerHeightWhenNoEqualHeightRemains() {
        val manifest = videoManifest()

        val promoted = promoteAlternatePlaybackCandidate(
            manifest = manifest,
            isVideoMode = true,
            nowMs = now,
            isBlocked = { it == "https://v/av1-1080" || it == "https://v/vp9-1080" }
        )

        assertEquals("https://v/avc-720", promoted?.selectedVideoUrl)
    }

    @Test
    fun promotionNeverReturnsTheSameSelectionTwice() {
        var blocked = setOf("https://a/opus-high")
        val first = promoteAlternatePlaybackCandidate(
            manifest = audioManifest(),
            isVideoMode = false,
            nowMs = now,
            isBlocked = { it in blocked }
        )
        blocked = blocked + first!!.selectedAudioUrl
        val second = promoteAlternatePlaybackCandidate(
            manifest = first,
            isVideoMode = false,
            nowMs = now,
            isBlocked = { it in blocked }
        )

        assertEquals("https://a/opus-low", second?.selectedAudioUrl)
        assertTrue(first.selectedAudioUrl != second?.selectedAudioUrl)
    }

    private fun audioManifest(alternativeExpiresAtMs: Long = now + 600_000L) = ResolvedPlaybackManifest(
        sourceVideoId = "abcdefghijk",
        provider = "test",
        resolvedAtMs = now,
        expiresAtMs = now + 600_000L,
        durationMs = 210_000L,
        selectedAudioUrl = "https://a/opus-high",
        selectedVideoUrl = "",
        streams = listOf(
            audio("https://a/opus-high", bitrate = 160_000, selected = true, expiresAtMs = now + 600_000L),
            audio("https://a/aac", bitrate = 128_000, expiresAtMs = alternativeExpiresAtMs),
            audio("https://a/opus-low", bitrate = 64_000, expiresAtMs = alternativeExpiresAtMs)
        )
    )

    private fun videoManifest() = ResolvedPlaybackManifest(
        sourceVideoId = "abcdefghijk",
        provider = "test",
        resolvedAtMs = now,
        expiresAtMs = now + 600_000L,
        durationMs = 210_000L,
        selectedAudioUrl = "https://a/aac",
        selectedVideoUrl = "https://v/av1-1080",
        streams = listOf(
            audio("https://a/aac", bitrate = 128_000, selected = true, expiresAtMs = now + 600_000L),
            video("https://v/av1-1080", height = 1080, selected = true),
            video("https://v/vp9-1080", height = 1080),
            video("https://v/avc-720", height = 720)
        )
    )

    private fun audio(
        url: String,
        bitrate: Int,
        selected: Boolean = false,
        expiresAtMs: Long
    ) = PlaybackStreamDescriptor(
        url = url,
        kind = PlaybackStreamKind.AUDIO,
        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
        averageBitrate = bitrate,
        expiresAtMs = expiresAtMs,
        selected = selected
    )

    private fun video(url: String, height: Int, selected: Boolean = false) = PlaybackStreamDescriptor(
        url = url,
        kind = PlaybackStreamKind.VIDEO,
        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
        height = height,
        averageBitrate = height * 1_000,
        expiresAtMs = now + 600_000L,
        selected = selected
    )
}
