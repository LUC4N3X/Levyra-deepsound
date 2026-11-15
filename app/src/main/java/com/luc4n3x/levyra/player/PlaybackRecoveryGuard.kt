package com.luc4n3x.levyra.player

internal data class PlaybackRecoveryRequest(val playbackGeneration: Long, val queueKey: String)

internal class PlaybackRecoveryGuard {
    private var playbackGeneration = 0L

    fun invalidate() {
        playbackGeneration++
    }

    fun capture(queueKey: String): PlaybackRecoveryRequest =
        PlaybackRecoveryRequest(playbackGeneration, queueKey)

    fun isCurrent(request: PlaybackRecoveryRequest, queueKey: String): Boolean =
        request.playbackGeneration == playbackGeneration && request.queueKey == queueKey
}
