package com.luc4n3x.levyra.feature.jam

data class JamPlaybackSnapshot(
    val queue: List<JamTrack> = emptyList(),
    val currentIndex: Int = -1,
    val currentMediaId: String = "",
    val positionMs: Long = 0L,
    val playWhenReady: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0
)

interface JamPlayerBridge {
    fun snapshot(): JamPlaybackSnapshot

    suspend fun applyRemoteState(state: JamSessionState)

    suspend fun applyAction(action: JamAction)
}
