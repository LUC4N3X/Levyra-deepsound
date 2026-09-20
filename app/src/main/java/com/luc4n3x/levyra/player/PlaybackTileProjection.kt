package com.luc4n3x.levyra.player

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
    mediaItemCount: Int
): PlaybackTileProjection {
    val active = isPlaying || playWhenReady
    val resumable = mediaItemCount > 0
    return when {
        active -> PlaybackTileProjection(
            kind = PlaybackTileProjectionKind.Active,
            action = PlaybackTileAction.Pause,
            description = PlaybackTileDescription.Playing
        )
        resumable -> PlaybackTileProjection(
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
