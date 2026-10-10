package com.luc4n3x.levyra.domain

object ResumePlaybackPolicy {
    enum class Decision {
        RESUME,
        ALREADY_PLAYING,
        NOTHING_TO_RESUME,
        LIVE_RADIO,
        REMOTE_PLAYBACK
    }

    fun decide(
        playing: Boolean,
        hasRemotePlayback: Boolean,
        currentTrackIsLiveRadio: Boolean,
        restoredTrackAvailable: Boolean
    ): Decision = when {
        hasRemotePlayback -> Decision.REMOTE_PLAYBACK
        currentTrackIsLiveRadio -> Decision.LIVE_RADIO
        playing -> Decision.ALREADY_PLAYING
        restoredTrackAvailable -> Decision.RESUME
        else -> Decision.NOTHING_TO_RESUME
    }
}
