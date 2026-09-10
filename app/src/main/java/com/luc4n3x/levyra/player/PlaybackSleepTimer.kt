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
        val deadlineElapsedRealtimeMs: Long,
        val fadeMs: Long = 0L
    ) : PlaybackSleepTimerState

    data object EndOfTrack : PlaybackSleepTimerState
}

const val SLEEP_FADE_STEP_MS = 250L

fun sleepFadeVolume(remainingMs: Long, fadeMs: Long): Float {
    if (fadeMs <= 0L) return 1f
    if (remainingMs <= 0L) return 0f
    if (remainingMs >= fadeMs) return 1f
    return (remainingMs.toDouble() / fadeMs.toDouble()).toFloat().coerceIn(0f, 1f)
}

class PlaybackSleepTimer(
    private val scope: CoroutineScope,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val onFadeVolume: (Float) -> Unit = {},
    private val onExpired: () -> Unit = {}
) {
    private val _state = MutableStateFlow<PlaybackSleepTimerState>(PlaybackSleepTimerState.Disabled)
    val state: StateFlow<PlaybackSleepTimerState> = _state.asStateFlow()

    private val fadeLock = Any()
    private var countdownJob: Job? = null
    private var fadeActive = false

    fun startCountdown(totalMs: Long, fadeMs: Long = 0L) {
        require(totalMs > 0L) { "Sleep timer duration must be positive" }
        cancel()
        val effectiveFadeMs = fadeMs.coerceIn(0L, totalMs)
        val deadline = elapsedRealtime() + totalMs
        val target = PlaybackSleepTimerState.Countdown(totalMs, deadline, effectiveFadeMs)
        _state.value = target
        val job = scope.launch {
            val untilFade = deadline - effectiveFadeMs - elapsedRealtime()
            if (untilFade > 0L) delay(untilFade)
            if (effectiveFadeMs > 0L && _state.value == target) {
                synchronized(fadeLock) {
                    if (_state.value == target) fadeActive = true
                }
                while (_state.value == target) {
                    val remaining = deadline - elapsedRealtime()
                    if (remaining <= 0L) break
                    val emitted = synchronized(fadeLock) {
                        if (!fadeActive || _state.value != target) {
                            false
                        } else {
                            onFadeVolume(sleepFadeVolume(remaining, effectiveFadeMs))
                            true
                        }
                    }
                    if (!emitted) break
                    delay(minOf(SLEEP_FADE_STEP_MS, remaining))
                }
            }
            val remaining = deadline - elapsedRealtime()
            if (remaining > 0L) delay(remaining)
            if (_state.value == target) {
                _state.value = PlaybackSleepTimerState.Disabled
                releaseFade()
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
        releaseFade()
    }

    fun isEndOfTrackActive(): Boolean = _state.value is PlaybackSleepTimerState.EndOfTrack

    fun consumeEndOfTrackBoundary(): Boolean {
        if (_state.value !is PlaybackSleepTimerState.EndOfTrack) return false
        _state.value = PlaybackSleepTimerState.Disabled
        return true
    }

    private fun releaseFade() {
        synchronized(fadeLock) {
            if (!fadeActive) return
            fadeActive = false
            onFadeVolume(1f)
        }
    }
}
