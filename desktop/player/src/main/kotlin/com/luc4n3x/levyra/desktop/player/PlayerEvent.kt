package com.luc4n3x.levyra.desktop.player

sealed interface PlayerEvent {
    data object Opening : PlayerEvent
    data class Buffering(val percent: Float) : PlayerEvent
    data object Playing : PlayerEvent
    data object Paused : PlayerEvent
    data object Stopped : PlayerEvent
    data object Finished : PlayerEvent
    data class Failed(val reason: String) : PlayerEvent
    data class TimeChanged(val positionMs: Long) : PlayerEvent
    data class LengthChanged(val durationMs: Long) : PlayerEvent
}

enum class PlaybackStatus {
    IDLE,
    OPENING,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    FAILED
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromName(value: String): RepeatMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OFF
    }
}
