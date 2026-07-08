package com.luc4n3x.levyra.feature.player.domain

import com.luc4n3x.levyra.domain.Track

sealed interface PlayerEffect {
    data class ResolveTrack(val track: Track, val isVideoMode: Boolean) : PlayerEffect

    data class StartPlayback(val track: Track) : PlayerEffect

    data object PausePlayback : PlayerEffect

    data object StopPlayback : PlayerEffect

    data class SeekTo(val positionMs: Long) : PlayerEffect

    data object SelectNextTrack : PlayerEffect

    data object SelectPreviousTrack : PlayerEffect

    data class LoadLyrics(val track: Track) : PlayerEffect

    data class PersistSnapshot(val track: Track?, val positionMs: Long, val isPlaying: Boolean) : PlayerEffect

    data class ReportPlaybackError(val message: String) : PlayerEffect
}
