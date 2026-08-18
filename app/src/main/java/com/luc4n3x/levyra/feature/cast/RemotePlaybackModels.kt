package com.luc4n3x.levyra.feature.cast

import com.luc4n3x.levyra.domain.RepeatMode

data class RemoteDevice(
    val id: String,
    val name: String
)

enum class RemotePlaybackAvailability {
    Unavailable,
    Idle,
    Connecting,
    Connected
}

data class RemotePlaybackState(
    val availability: RemotePlaybackAvailability = RemotePlaybackAvailability.Unavailable,
    val connected: Boolean = false,
    val deviceName: String? = null,
    val queueIds: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off
)
