package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFailureClassificationTest {
    @Test
    fun antiBotAndSignInChallengesTriggerForbiddenRecovery() {
        val messages = listOf(
            "Sign in to confirm you're not a bot",
            "Sign in to confirm you’re not a bot",
            "Please confirm you are not a bot",
            "Accedi per confermare di essere umano"
        )

        messages.forEach { message ->
            assertEquals(
                PlaybackFailureKind.Forbidden,
                classifyPlaybackFailureReason(message)
            )
        }
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
}
