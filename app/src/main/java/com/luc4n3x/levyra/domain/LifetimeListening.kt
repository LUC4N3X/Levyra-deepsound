package com.luc4n3x.levyra.domain

data class LifetimeArtist(
    val name: String,
    val countedPlays: Int,
    val listenedMs: Long
)

data class LifetimeListening(
    val totalListenMs: Long = 0L,
    val countedPlays: Int = 0,
    val completedCount: Int = 0,
    val eventCount: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val firstPlayedAt: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val tracks: List<PulseTrack> = emptyList(),
    val artists: List<LifetimeArtist> = emptyList()
) {
    val hasSignal: Boolean
        get() = eventCount > 0 || totalListenMs > 0L

    val totalMinutes: Long
        get() = totalListenMs / 60_000L
}
