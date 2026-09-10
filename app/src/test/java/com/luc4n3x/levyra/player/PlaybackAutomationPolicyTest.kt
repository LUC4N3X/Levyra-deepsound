package com.luc4n3x.levyra.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAutomationPolicyTest {

    private fun resumeEligible(
        enabled: Boolean = true,
        pausedAt: Long? = 1_000L,
        now: Long = 2_000L,
        ready: Boolean = true,
        hasQueueItem: Boolean = true,
        alreadyPlaying: Boolean = false
    ): Boolean = PlaybackAutomationPolicy.shouldResumeOnRouteReconnect(
        enabled = enabled,
        pausedByRouteLossAtMs = pausedAt,
        nowMs = now,
        playerReady = ready,
        hasQueueItem = hasQueueItem,
        alreadyPlaying = alreadyPlaying
    )

    @Test
    fun routeReconnectResumesOnlyAfterARouteLossPause() {
        assertTrue(resumeEligible())
        assertFalse(resumeEligible(pausedAt = null))
    }

    @Test
    fun routeReconnectNeverResumesWhenDisabledOrAlreadyPlaying() {
        assertFalse(resumeEligible(enabled = false))
        assertFalse(resumeEligible(alreadyPlaying = true))
    }

    @Test
    fun routeReconnectRequiresReadyPlayerAndQueue() {
        assertFalse(resumeEligible(ready = false))
        assertFalse(resumeEligible(hasQueueItem = false))
    }

    @Test
    fun routeReconnectExpiresAfterTheResumeWindow() {
        val pausedAt = 1_000L
        val late = pausedAt + PlaybackAutomationPolicy.ROUTE_RESUME_WINDOW_MS + 1L

        assertTrue(resumeEligible(pausedAt = pausedAt, now = pausedAt + PlaybackAutomationPolicy.ROUTE_RESUME_WINDOW_MS))
        assertFalse(resumeEligible(pausedAt = pausedAt, now = late))
    }

    @Test
    fun mutePauseOnlyAppliesToPlayingPlayerAtZeroVolume() {
        assertTrue(PlaybackAutomationPolicy.shouldPauseForMute(enabled = true, streamVolume = 0, isPlaying = true))
        assertFalse(PlaybackAutomationPolicy.shouldPauseForMute(enabled = true, streamVolume = 0, isPlaying = false))
        assertFalse(PlaybackAutomationPolicy.shouldPauseForMute(enabled = true, streamVolume = 3, isPlaying = true))
        assertFalse(PlaybackAutomationPolicy.shouldPauseForMute(enabled = false, streamVolume = 0, isPlaying = true))
    }

    @Test
    fun autoDownloadSkipsUnlikesDuplicatesAndActiveDownloads() {
        fun decide(
            enabled: Boolean = true,
            becameFavorite: Boolean = true,
            trackId: String = "abc",
            downloaded: Set<String> = emptySet(),
            downloading: Set<String> = emptySet()
        ) = PlaybackAutomationPolicy.shouldAutoDownloadFavorite(
            enabled = enabled,
            becameFavorite = becameFavorite,
            trackId = trackId,
            downloadedTrackIds = downloaded,
            downloadingKeys = downloading,
            downloadKey = "key-$trackId"
        )

        assertTrue(decide())
        assertFalse(decide(enabled = false))
        assertFalse(decide(becameFavorite = false))
        assertFalse(decide(trackId = ""))
        assertFalse(decide(downloaded = setOf("abc")))
        assertFalse(decide(downloading = setOf("key-abc")))
    }

    @Test
    fun consecutiveFailureGuardStopsSkippingAfterTheBound() {
        val guard = ConsecutivePlaybackFailureGuard(maxSkips = 2)

        assertTrue(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))
        assertTrue(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))
        assertFalse(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))
    }

    @Test
    fun healthyPlaybackResetsTheFailureGuard() {
        val guard = ConsecutivePlaybackFailureGuard(maxSkips = 1)

        assertTrue(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))
        assertFalse(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))

        guard.onHealthyPlayback()

        assertTrue(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))
    }

    @Test
    fun disabledPreferenceAndEmptyQueueNeverConsumeSkipBudget() {
        val guard = ConsecutivePlaybackFailureGuard(maxSkips = 1)

        assertFalse(guard.shouldSkipAfterUnrecoverableError(enabled = false, hasQueueItem = true))
        assertFalse(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = false))

        assertTrue(guard.shouldSkipAfterUnrecoverableError(enabled = true, hasQueueItem = true))
    }
}
