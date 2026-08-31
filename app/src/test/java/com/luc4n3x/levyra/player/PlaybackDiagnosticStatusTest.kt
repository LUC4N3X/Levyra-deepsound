package com.luc4n3x.levyra.player

import androidx.media3.common.Player
import com.luc4n3x.levyra.data.PlaybackStrategyCircuit
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDiagnosticStatusTest {
    private val now = 2_000_000L

    @Test
    fun recentFailureMarksFallbackHistory() {
        assertEquals(
            PlaybackDiagnosticStatus.FALLBACK_HISTORY,
            playbackDiagnosticStatus(
                errorCode = "",
                playbackState = Player.STATE_READY,
                strategies = listOf(strategy(lastFailureAtMs = now - 60_000L)),
                nowMs = now
            )
        )
    }

    @Test
    fun staleFailureDoesNotMarkHealthyPlaybackAsFallback() {
        assertEquals(
            PlaybackDiagnosticStatus.HEALTHY,
            playbackDiagnosticStatus(
                errorCode = "",
                playbackState = Player.STATE_READY,
                strategies = listOf(strategy(lastFailureAtMs = now - 31L * 60L * 1_000L)),
                nowMs = now
            )
        )
    }

    @Test
    fun openCircuitMarksFallbackRegardlessOfFailureAge() {
        assertEquals(
            PlaybackDiagnosticStatus.FALLBACK_HISTORY,
            playbackDiagnosticStatus(
                errorCode = "",
                playbackState = Player.STATE_READY,
                strategies = listOf(
                    strategy(
                        circuit = PlaybackStrategyCircuit.OPEN,
                        lastFailureAtMs = now - 60L * 60L * 1_000L
                    )
                ),
                nowMs = now
            )
        )
    }

    @Test
    fun playerErrorAndIdleHavePriority() {
        assertEquals(
            PlaybackDiagnosticStatus.ERROR,
            playbackDiagnosticStatus(
                errorCode = "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED",
                playbackState = Player.STATE_READY,
                strategies = emptyList(),
                nowMs = now
            )
        )
        assertEquals(
            PlaybackDiagnosticStatus.IDLE,
            playbackDiagnosticStatus(
                errorCode = "",
                playbackState = Player.STATE_IDLE,
                strategies = emptyList(),
                nowMs = now
            )
        )
    }

    private fun strategy(
        circuit: PlaybackStrategyCircuit = PlaybackStrategyCircuit.CLOSED,
        lastFailureAtMs: Long
    ) = PlaybackDiagnosticStrategy(
        name = "TEST",
        successes = 1,
        failures = 1,
        consecutiveFailures = 1,
        averageLatencyMs = 100L,
        circuit = circuit,
        lastFailure = "Timeout",
        lastFailureAtMs = lastFailureAtMs
    )
}
