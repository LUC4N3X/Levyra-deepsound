package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkVerificationPlanTest {
    @Test
    fun fullSpotifyBudgetStillKeepsAppleFallback() {
        val ranked = listOf(
            rankedCandidate("spotify-1", providerRank = 0),
            rankedCandidate("spotify-2", providerRank = 0),
            rankedCandidate("spotify-3", providerRank = 0),
            rankedCandidate("apple", providerRank = 1)
        )

        val plan = buildMotionArtworkVerificationPlan(ranked)

        assertEquals(listOf("spotify-1", "spotify-2", "spotify-3", "apple"), plan.candidates.map { it.candidate.url })
        assertTrue(plan.exhaustive)
    }

    @Test
    fun oversizedProviderSetRemainsBoundedWithoutDroppingFallbackProviders() {
        val ranked = listOf(
            rankedCandidate("spotify-1", providerRank = 0),
            rankedCandidate("spotify-2", providerRank = 0),
            rankedCandidate("spotify-3", providerRank = 0),
            rankedCandidate("spotify-4", providerRank = 0),
            rankedCandidate("apple", providerRank = 1),
            rankedCandidate("tidal", providerRank = 2)
        )

        val plan = buildMotionArtworkVerificationPlan(ranked)

        assertEquals(
            listOf("spotify-1", "spotify-2", "spotify-3", "apple", "tidal"),
            plan.candidates.map { it.candidate.url }
        )
        assertFalse(plan.exhaustive)
    }

    private fun rankedCandidate(url: String, providerRank: Int): MotionArtworkRankedCandidate =
        MotionArtworkRankedCandidate(
            candidate = MotionArtworkCandidate(
                provider = when (providerRank) {
                    0 -> "community-canvas"
                    1 -> "apple-motion"
                    else -> "tidal-video-cover"
                },
                scope = MotionArtworkScope.TRACK,
                identity = MotionTrackIdentity(
                    title = "Track",
                    artists = listOf("Artist"),
                    album = "Album",
                    durationMs = 180_000L,
                    isrc = "",
                    upc = "",
                    year = "",
                    trackId = url,
                    albumId = "album"
                ),
                url = url,
                mimeType = "video/mp4",
                expiresAtMs = Long.MAX_VALUE
            ),
            confidence = 100,
            providerRank = providerRank
        )
}
