package com.luc4n3x.levyra.player

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryGuardTest {
    @Test
    fun delayedResolutionCannotReplaceTheNextTrack() = runBlocking {
        val guard = PlaybackRecoveryGuard()
        var queueKey = "space:first"
        var playing = "first"
        val resolved = CompletableDeferred<String>()
        val request = guard.capture(queueKey)
        val recovery = launch(start = CoroutineStart.UNDISPATCHED) {
            val track = resolved.await()
            if (guard.isCurrent(request, queueKey)) playing = track
        }

        queueKey = "space:second"
        guard.invalidate()
        playing = "second"
        resolved.complete("first")
        recovery.join()

        assertEquals("second", playing)
    }

    @Test
    fun pauseAndResumeCannotReviveAnEarlierRecovery() = runBlocking {
        val guard = PlaybackRecoveryGuard()
        var positionMs = 12_000L
        val resolved = CompletableDeferred<Unit>()
        val request = guard.capture("space:first")
        val recovery = launch(start = CoroutineStart.UNDISPATCHED) {
            resolved.await()
            if (guard.isCurrent(request, "space:first")) positionMs = 4_000L
        }

        guard.invalidate()
        positionMs = 25_000L
        resolved.complete(Unit)
        recovery.join()

        assertEquals(25_000L, positionMs)
        assertTrue(guard.isCurrent(guard.capture("space:first"), "space:first"))
    }

    @Test
    fun queueSelectionInvalidatesRecoveryBeforeThePlayerChanges() {
        val guard = PlaybackRecoveryGuard()
        val request = guard.capture("space:first")

        assertFalse(guard.isCurrent(request, "space:second"))
        assertFalse(guard.isCurrent(request, "other-space:first"))
    }

    @Test
    fun replacementOfTheSameTrackInvalidatesItsOldRequest() {
        val guard = PlaybackRecoveryGuard()
        val request = guard.capture("space:first")
        guard.invalidate()

        assertFalse(guard.isCurrent(request, "space:first"))
    }

    @Test
    fun transientNetworkRecoveryStillPublishesForUnchangedPlayback() = runBlocking {
        val guard = PlaybackRecoveryGuard()
        val resolved = CompletableDeferred<String>()
        val request = guard.capture("space:first")
        var stream = "expired"
        val recovery = launch(start = CoroutineStart.UNDISPATCHED) {
            val refreshed = resolved.await()
            if (guard.isCurrent(request, "space:first")) stream = refreshed
        }

        resolved.complete("refreshed")
        recovery.join()

        assertEquals("refreshed", stream)
    }

    @Test
    fun pauseDuringSkipSurvivesLaterRetries() {
        val guard = PlaybackRecoveryGuard()
        val playIntent = guard.capture("space:first")

        guard.invalidate()
        val retry = guard.capture("space:second")

        assertFalse(guard.isCurrentGeneration(playIntent))
        assertTrue(guard.isCurrentGeneration(retry))
    }
}
