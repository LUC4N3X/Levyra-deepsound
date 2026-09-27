package com.luc4n3x.levyra.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStabilitySignalsTest {

    private class FakeClock(var value: Long = 0L) {
        fun advance(deltaMs: Long) {
            value += deltaMs
        }
    }

    private fun signals(clock: FakeClock) = PlaybackStabilitySignals(nowMs = clock::value)

    private fun PlaybackStabilitySignals.startPlayback() {
        onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        onPlaybackStateChanged(Player.STATE_READY)
    }

    private fun PlaybackStabilitySignals.rebuffer() {
        onPlaybackStateChanged(Player.STATE_BUFFERING)
        onPlaybackStateChanged(Player.STATE_READY)
    }

    @Test
    fun startsInNormalProfile() {
        val signals = signals(FakeClock())
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun singleIsolatedRebufferDoesNotEscalate() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun repeatedRebufferEscalatesToStable() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun rebufferOutsideWindowDoesNotAccumulate() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(200_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun seekDoesNotCountAsRebuffer() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.onDiscontinuity()
        signals.onPlaybackStateChanged(Player.STATE_BUFFERING)
        signals.onPlaybackStateChanged(Player.STATE_READY)
        signals.onDiscontinuity()
        signals.onPlaybackStateChanged(Player.STATE_BUFFERING)
        signals.onPlaybackStateChanged(Player.STATE_READY)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun trackTransitionBufferingDoesNotCountAsRebuffer() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        repeat(5) {
            signals.onDiscontinuity()
            signals.onPlaybackStateChanged(Player.STATE_BUFFERING)
            signals.onPlaybackStateChanged(Player.STATE_READY)
        }
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun repeatedLoadFailureEscalatesToStable() {
        val signals = signals(FakeClock())
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun cancelledLoadDoesNotCountAsFailure() {
        val signals = signals(FakeClock())
        repeat(5) { signals.onLoadOutcome(failed = true, wasCanceled = true) }
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun successfulLoadResetsFailureStreak() {
        val signals = signals(FakeClock())
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = false, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun staysStableBeforeMinimumDwellElapses() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
        clock.advance(5_000L)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun recoversToNormalAfterDwellWithNoFurtherSignals() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
        clock.advance(130_000L)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun newSignalDuringDwellExtendsStableWindow() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        clock.advance(115_000L)
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        clock.advance(10_000L)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun noOscillationAcrossManyShortIntervalChecks() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        var sawNormalAfterEscalation = false
        repeat(20) {
            clock.advance(5_000L)
            if (signals.requestedProfile() == PlaybackStabilityProfile.Normal) sawNormalAfterEscalation = true
        }
        assertEquals(false, sawNormalAfterEscalation)
    }
}
