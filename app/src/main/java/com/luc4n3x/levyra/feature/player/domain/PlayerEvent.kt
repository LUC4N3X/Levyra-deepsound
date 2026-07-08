package com.luc4n3x.levyra.feature.player.domain

import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track

sealed interface PlayerEvent {
    data class TrackRequested(
        val track: Track,
        val queue: List<Track> = emptyList(),
        val startPlaying: Boolean = true
    ) : PlayerEvent

    data object PlayClicked : PlayerEvent

    data object PauseClicked : PlayerEvent

    data object TogglePlayClicked : PlayerEvent

    data object StopClicked : PlayerEvent

    data object NextClicked : PlayerEvent

    data object PreviousClicked : PlayerEvent

    data class SeekRequested(val positionMs: Long) : PlayerEvent

    data class ProgressChanged(val positionMs: Long, val durationMs: Long) : PlayerEvent

    data object ResolveStarted : PlayerEvent

    data class ResolveSucceeded(val track: Track) : PlayerEvent

    data class ResolveFailed(val message: String) : PlayerEvent

    data object PlaybackStarted : PlayerEvent

    data object PlaybackPaused : PlayerEvent

    data object PlaybackCompleted : PlayerEvent

    data class QueueChanged(val queue: List<Track>) : PlayerEvent

    data class VideoModeChanged(val enabled: Boolean) : PlayerEvent

    data class RepeatModeChanged(val repeatMode: RepeatMode) : PlayerEvent

    data class ShuffleChanged(val enabled: Boolean) : PlayerEvent

    data object ErrorConsumed : PlayerEvent
}
