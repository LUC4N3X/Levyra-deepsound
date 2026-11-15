package com.luc4n3x.levyra.player

internal data class PlaybackRecoveryRequest(val playbackGeneration: Long, val queueKey: String)

internal class PlaybackRecoveryGuard {
    private var playbackGeneration = 0L

    fun invalidate() {
        playbackGeneration++
    }

    fun capture(queueKey: String): PlaybackRecoveryRequest =
        PlaybackRecoveryRequest(playbackGeneration, queueKey)

    fun isCurrentGeneration(request: PlaybackRecoveryRequest): Boolean =
        request.playbackGeneration == playbackGeneration

    fun isCurrent(request: PlaybackRecoveryRequest, queueKey: String): Boolean =
        isCurrentGeneration(request) && request.queueKey == queueKey
}
