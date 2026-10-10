package com.luc4n3x.levyra.player

import androidx.media3.common.Player

enum class PlaybackTileProjectionKind {
    Active,
    Inactive
}

enum class PlaybackTileAction {
    Pause,
    Resume,
    OpenApp
}

enum class PlaybackTileDescription {
    Playing,
    Paused,
    Idle
}

data class PlaybackTileProjection(
    val kind: PlaybackTileProjectionKind,
    val action: PlaybackTileAction,
    val description: PlaybackTileDescription
)

fun playbackTileProjection(
    isPlaying: Boolean,
    playWhenReady: Boolean,
    playbackState: Int = Player.STATE_IDLE,
    mediaItemCount: Int
): PlaybackTileProjection {
    val hasItems = mediaItemCount > 0
    val isEnded = playbackState == Player.STATE_ENDED
    val isBufferingWithPlayIntent = playWhenReady && playbackState == Player.STATE_BUFFERING
    val isActivelyPlaying = isPlaying && !isEnded
    val active = (isActivelyPlaying || isBufferingWithPlayIntent) && hasItems
    val isPausedResumable = hasItems && !active && !isEnded && playbackState != Player.STATE_IDLE

    return when {
        active -> PlaybackTileProjection(
            kind = PlaybackTileProjectionKind.Active,
            action = PlaybackTileAction.Pause,
            description = PlaybackTileDescription.Playing
        )
        isPausedResumable -> PlaybackTileProjection(
            kind = PlaybackTileProjectionKind.Inactive,
            action = PlaybackTileAction.Resume,
            description = PlaybackTileDescription.Paused
        )
        else -> PlaybackTileProjection(
            kind = PlaybackTileProjectionKind.Inactive,
            action = PlaybackTileAction.OpenApp,
            description = PlaybackTileDescription.Idle
        )
    }
}

fun playbackTileProjection(
    isPlaying: Boolean,
    playWhenReady: Boolean,
    mediaItemCount: Int
): PlaybackTileProjection = playbackTileProjection(
    isPlaying = isPlaying,
    playWhenReady = playWhenReady,
    playbackState = if (isPlaying || playWhenReady) Player.STATE_READY else Player.STATE_IDLE,
    mediaItemCount = mediaItemCount
)
