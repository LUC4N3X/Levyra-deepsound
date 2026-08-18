package com.luc4n3x.levyra.feature.cast

import com.luc4n3x.levyra.domain.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class NoOpCastBackend : RemotePlaybackBackend {
    override val id: String = "noop"
    override val available: Boolean = false

    private val _state = MutableStateFlow(RemotePlaybackState())
    override val state: StateFlow<RemotePlaybackState> = _state

    override fun devices(): Flow<List<RemoteDevice>> = flowOf(emptyList())

    override suspend fun connect(device: RemoteDevice) = Unit

    override suspend fun disconnect() = Unit

    override suspend fun load(handoff: CastHandoff) = Unit

    override suspend fun play() = Unit

    override suspend fun pause() = Unit

    override suspend fun stop() = Unit

    override suspend fun seekTo(positionMs: Long) = Unit

    override suspend fun skipToNext() = Unit

    override suspend fun skipToPrevious() = Unit

    override suspend fun setShuffle(enabled: Boolean) = Unit

    override suspend fun setRepeatMode(mode: RepeatMode) = Unit
}
