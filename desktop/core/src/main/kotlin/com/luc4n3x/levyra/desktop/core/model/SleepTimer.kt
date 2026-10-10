package com.luc4n3x.levyra.desktop.core.model

enum class SleepTimerMode {
    OFF,
    DURATION,
    END_OF_TRACK
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val requestedMinutes: Int = 0,
    val deadlineMs: Long = 0L
) {
    val active: Boolean get() = mode != SleepTimerMode.OFF

    fun remainingMs(nowMs: Long): Long = when (mode) {
        SleepTimerMode.DURATION -> (deadlineMs - nowMs).coerceAtLeast(0L)
        else -> 0L
    }

    fun remainingMinutes(nowMs: Long): Int = when (mode) {
        SleepTimerMode.DURATION -> {
            val remaining = remainingMs(nowMs)
            if (remaining <= 0L) 0 else ((remaining + MINUTE_MS - 1L) / MINUTE_MS).toInt()
        }

        else -> 0
    }

    fun expired(nowMs: Long): Boolean = mode == SleepTimerMode.DURATION && nowMs >= deadlineMs

    companion object {
        const val MINUTE_MS = 60_000L

        val PRESET_MINUTES = listOf(15, 30, 45, 60, 90)

        fun forMinutes(minutes: Int, nowMs: Long): SleepTimerState {
            val safeMinutes = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
            return SleepTimerState(
                mode = SleepTimerMode.DURATION,
                requestedMinutes = safeMinutes,
                deadlineMs = nowMs + safeMinutes * MINUTE_MS
            )
        }

        fun endOfTrack(): SleepTimerState = SleepTimerState(mode = SleepTimerMode.END_OF_TRACK)

        private const val MIN_MINUTES = 1
        private const val MAX_MINUTES = 600
    }
}
