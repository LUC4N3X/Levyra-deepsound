package com.luc4n3x.levyra.feature.cast

import com.luc4n3x.levyra.domain.RepeatMode
import androidx.media3.common.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RemotePlaybackBackend {
    val id: String
    val available: Boolean
    val state: StateFlow<RemotePlaybackState>

    fun devices(): Flow<List<RemoteDevice>>

    suspend fun connect(device: RemoteDevice)
    suspend fun disconnect()

    suspend fun load(handoff: CastHandoff)

    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun seekTo(positionMs: Long)
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun setShuffle(enabled: Boolean)
    suspend fun setRepeatMode(mode: RepeatMode)

    fun attachLocalPlayer(localPlayer: Player): Player
    fun release()
}
