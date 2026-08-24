package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFailureClassificationTest {
    @Test
    fun antiBotAndSignInChallengesTriggerLoginRequiredRecovery() {
        val messages = listOf(
            "Sign in to confirm you're not a bot",
            "Sign in to confirm you’re not a bot",
            "Please confirm you are not a bot",
            "Accedi per confermare di essere umano",
            "LOGIN_REQUIRED"
        )

        messages.forEach { message ->
            assertEquals(
                PlaybackFailureKind.LoginRequired,
                classifyPlaybackFailureReason(message)
            )
        }
    }

    @Test
    fun ageRestrictionIsTrackSpecificRatherThanGlobalLoginFailure() {
        assertEquals(
            PlaybackFailureKind.ContentRestricted,
            classifyPlaybackFailureReason("This video is age restricted")
        )
    }

    @Test
    fun responseCodeWordingIsClassifiedLikeHttpStatus() {
        assertEquals(PlaybackFailureKind.Forbidden, classifyPlaybackFailureReason("Response code: 403"))
        assertEquals(PlaybackFailureKind.RateLimited, classifyPlaybackFailureReason("Response code: 429"))
    }

    @Test
    fun unrelatedNumbersDoNotMasqueradeAsHttpStatuses() {
        assertEquals(
            PlaybackFailureKind.Unknown,
            classifyPlaybackFailureReason("playback position 403 seconds")
        )
        assertEquals(
            PlaybackFailureKind.Unknown,
            classifyPlaybackFailureReason("track token abc410xyz")
        )
        assertEquals(
            PlaybackFailureKind.Unknown,
            classifyPlaybackFailureReason("buffer contains 429 samples")
        )
    }

    @Test
    fun existingHttpRecoveryClassificationIsPreserved() {
        assertEquals(PlaybackFailureKind.Forbidden, classifyPlaybackFailureReason("HTTP 403"))
        assertEquals(PlaybackFailureKind.Gone, classifyPlaybackFailureReason("HTTP 410"))
        assertEquals(PlaybackFailureKind.RateLimited, classifyPlaybackFailureReason("HTTP 429"))
        assertEquals(PlaybackFailureKind.Signature, classifyPlaybackFailureReason("PO Token rejected"))
    }

    @Test
    fun notFoundStatusIsClassified() {
        assertEquals(PlaybackFailureKind.NotFound, classifyPlaybackFailureReason("HTTP 404"))
        assertEquals(PlaybackFailureKind.NotFound, classifyPlaybackFailureReason("status code: 404"))
        assertEquals(PlaybackFailureKind.NotFound, classifyPlaybackFailureReason("Resource not found"))
    }

    @Test
    fun rangeNotSatisfiableStatusIsClassified() {
        assertEquals(PlaybackFailureKind.RangeNotSatisfiable, classifyPlaybackFailureReason("HTTP 416"))
        assertEquals(
            PlaybackFailureKind.RangeNotSatisfiable,
            classifyPlaybackFailureReason("Range not satisfiable")
        )
    }

    @Test
    fun serverErrorStatusesAreClassified() {
        assertEquals(PlaybackFailureKind.ServerError, classifyPlaybackFailureReason("HTTP 500"))
        assertEquals(PlaybackFailureKind.ServerError, classifyPlaybackFailureReason("HTTP 502"))
        assertEquals(PlaybackFailureKind.ServerError, classifyPlaybackFailureReason("HTTP 503"))
        assertEquals(PlaybackFailureKind.ServerError, classifyPlaybackFailureReason("HTTP 504"))
        assertEquals(PlaybackFailureKind.ServerError, classifyPlaybackFailureReason("Bad gateway"))
        assertEquals(PlaybackFailureKind.ServerError, classifyPlaybackFailureReason("Service unavailable"))
    }

    @Test
    fun truncatedResponsesAreClassified() {
        assertEquals(PlaybackFailureKind.Truncated, classifyPlaybackFailureReason("Truncated response body"))
        assertEquals(
            PlaybackFailureKind.Truncated,
            classifyPlaybackFailureReason("Unexpected end of stream")
        )
        assertEquals(PlaybackFailureKind.Truncated, classifyPlaybackFailureReason("Connection reset by peer"))
        assertEquals(PlaybackFailureKind.Truncated, classifyPlaybackFailureReason("Premature end of content-length"))
    }

    @Test
    fun genericConnectionFailuresStillClassifyAsNetwork() {
        assertEquals(PlaybackFailureKind.Network, classifyPlaybackFailureReason("Connection refused"))
        assertEquals(PlaybackFailureKind.Network, classifyPlaybackFailureReason("Unknown host"))
    }

    @Test
    fun transientNetworkRecoveryRefreshesStreamWithoutPurgingCacheOrSecurity() {
        listOf(PlaybackFailureKind.Network, PlaybackFailureKind.Timeout).forEach { kind ->
            val plan = playbackRecoveryPlanFor(kind)
            assertTrue(plan.invalidateStream)
            assertTrue(plan.rotateClient)
            assertFalse(plan.rotateCodec)
            assertFalse(plan.refreshSecurity)
            assertFalse(plan.invalidateCache)
            assertEquals(45_000L, plan.quarantineMs)
        }
    }

    @Test
    fun expiredAndRejectedUrlsUseFreshResolutionWithBoundedQuarantine() {
        val expired = playbackRecoveryPlanFor(PlaybackFailureKind.ExpiredUrl)
        assertTrue(expired.invalidateStream)
        assertTrue(expired.rotateClient)
        assertFalse(expired.rotateCodec)
        assertFalse(expired.refreshSecurity)
        assertFalse(expired.invalidateCache)
        assertEquals(2L * 60L * 1000L, expired.quarantineMs)

        listOf(PlaybackFailureKind.Forbidden, PlaybackFailureKind.Gone).forEach { kind ->
            val plan = playbackRecoveryPlanFor(kind)
            assertTrue(plan.invalidateStream)
            assertTrue(plan.rotateClient)
            assertTrue(plan.refreshSecurity)
            assertFalse(plan.invalidateCache)
            assertEquals(10L * 60L * 1000L, plan.quarantineMs)
        }
    }

    @Test
    fun structuralMediaFailuresAreTerminal() {
        assertEquals(
            PlaybackFailureKind.UnsupportedFormat,
            classifyPlaybackFailureReason("Unsupported media format")
        )
        assertEquals(
            PlaybackFailureKind.MalformedContainer,
            classifyPlaybackFailureReason("UnrecognizedInputFormatException: malformed media")
        )
        assertTrue(isTerminalPlaybackFailure(PlaybackFailureKind.UnsupportedFormat))
        assertTrue(isTerminalPlaybackFailure(PlaybackFailureKind.MalformedContainer))
        assertFalse(isTerminalPlaybackFailure(PlaybackFailureKind.Network))
    }

    @Test
    fun structuralMediaFailuresDoNotEnterNetworkRecoveryLoops() {
        val unsupported = playbackRecoveryPlanFor(PlaybackFailureKind.UnsupportedFormat)
        assertTrue(unsupported.invalidateStream)
        assertTrue(unsupported.rotateCodec)
        assertFalse(unsupported.rotateClient)
        assertFalse(unsupported.refreshSecurity)
        assertEquals(0L, unsupported.quarantineMs)

        val malformed = playbackRecoveryPlanFor(PlaybackFailureKind.MalformedContainer)
        assertTrue(malformed.invalidateStream)
        assertTrue(malformed.invalidateCache)
        assertFalse(malformed.rotateCodec)
        assertFalse(malformed.rotateClient)
        assertFalse(malformed.refreshSecurity)
        assertEquals(0L, malformed.quarantineMs)
    }
}
