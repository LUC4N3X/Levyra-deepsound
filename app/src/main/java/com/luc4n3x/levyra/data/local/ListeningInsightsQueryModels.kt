package com.luc4n3x.levyra.data.local

data class ListeningInsightsAggregateRow(
    val listenedMs: Long,
    val countedPlays: Long,
    val eventCount: Long,
    val completedCount: Long,
    val distinctTracks: Long,
    val distinctArtists: Long
)

data class ListeningInsightsDayRow(
    val dayKey: String,
    val listenedMs: Long,
    val countedPlays: Long
)

data class ListeningInsightsHourRow(
    val hour: Int,
    val listenedMs: Long
)

data class ListeningInsightsTopTrackRow(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val thumbnailUrl: String,
    val largeThumbnailUrl: String,
    val listenedMs: Long,
    val countedPlays: Long
)

data class ListeningInsightsTopArtistRow(
    val name: String,
    val thumbnailUrl: String,
    val listenedMs: Long,
    val countedPlays: Long,
    val trackCount: Long
)
