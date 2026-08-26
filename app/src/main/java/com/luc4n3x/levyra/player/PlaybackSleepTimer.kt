package com.luc4n3x.levyra.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlaybackSleepTimerState {
    data object Disabled : PlaybackSleepTimerState
    data class Countdown(
        val totalMs: Long,
        val deadlineElapsedRealtimeMs: Long
    ) : PlaybackSleepTimerState

    data object EndOfTrack : PlaybackSleepTimerState
}

class PlaybackSleepTimer(
    private val scope: CoroutineScope,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val onExpired: () -> Unit = {}
) {
    private val _state = MutableStateFlow<PlaybackSleepTimerState>(PlaybackSleepTimerState.Disabled)
    val state: StateFlow<PlaybackSleepTimerState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun startCountdown(totalMs: Long) {
        require(totalMs > 0L) { "Sleep timer duration must be positive" }
        cancel()
        val deadline = elapsedRealtime() + totalMs
        _state.value = PlaybackSleepTimerState.Countdown(totalMs, deadline)
        val job = scope.launch {
            val remaining = deadline - elapsedRealtime()
            if (remaining > 0L) delay(remaining)
            if (_state.value == PlaybackSleepTimerState.Countdown(totalMs, deadline)) {
                _state.value = PlaybackSleepTimerState.Disabled
                onExpired()
            }
        }
        countdownJob = job
    }

    fun startEndOfTrack() {
        cancel()
        _state.value = PlaybackSleepTimerState.EndOfTrack
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = PlaybackSleepTimerState.Disabled
    }

    fun isEndOfTrackActive(): Boolean = _state.value is PlaybackSleepTimerState.EndOfTrack

    fun consumeEndOfTrackBoundary(): Boolean {
        if (_state.value !is PlaybackSleepTimerState.EndOfTrack) return false
        _state.value = PlaybackSleepTimerState.Disabled
        return true
    }
}
