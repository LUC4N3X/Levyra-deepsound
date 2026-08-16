package com.luc4n3x.levyra.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePrecachePolicyTest {
    @Test
    fun transitionReuseRequiresSameQueuePairAndFreshEntry() {
        assertTrue(
            queuePrecacheMatchesTransition(
                preparedSourceIdentity = "id:a",
                preparedTargetIdentity = "id:b",
                currentSourceIdentity = "id:a",
                currentTargetIdentity = "id:b",
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 10_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            queuePrecacheMatchesTransition(
                preparedSourceIdentity = "id:a",
                preparedTargetIdentity = "id:b",
                currentSourceIdentity = "id:c",
                currentTargetIdentity = "id:b",
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 10_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            queuePrecacheMatchesTransition(
                preparedSourceIdentity = "id:a",
                preparedTargetIdentity = "id:b",
                currentSourceIdentity = "id:a",
                currentTargetIdentity = "id:b",
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 70_001L,
                maxAgeMs = 60_000L
            )
        )
    }

    @Test
    fun targetReuseSurvivesQueueAdvanceButNotDifferentTrack() {
        assertTrue(
            queuePrecacheMatchesTarget(
                preparedTargetIdentity = "id:b",
                requestedTargetIdentity = "id:b",
                preparedAtElapsedMs = 5_000L,
                nowElapsedMs = 20_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            queuePrecacheMatchesTarget(
                preparedTargetIdentity = "id:b",
                requestedTargetIdentity = "id:c",
                preparedAtElapsedMs = 5_000L,
                nowElapsedMs = 20_000L,
                maxAgeMs = 60_000L
            )
        )
    }

    @Test
    fun preparedTrackCanBeConsumedByOriginalOrResolvedId() {
        assertTrue(queuePrecacheMatchesTrackId("queue-id", "resolved-id", "queue-id"))
        assertTrue(queuePrecacheMatchesTrackId("queue-id", "resolved-id", "resolved-id"))
        assertFalse(queuePrecacheMatchesTrackId("queue-id", "resolved-id", "other"))
        assertFalse(queuePrecacheMatchesTrackId("queue-id", "resolved-id", ""))
    }
}
