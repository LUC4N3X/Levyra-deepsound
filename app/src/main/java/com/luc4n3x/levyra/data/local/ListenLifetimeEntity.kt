package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listen_lifetime_tracks")
data class ListenLifetimeTrackEntity(
    @PrimaryKey val trackKey: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val listenedMs: Long,
    val countedPlays: Int,
    val completedCount: Int,
    val eventCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)

@Entity(tableName = "listen_lifetime_artists")
data class ListenLifetimeArtistEntity(
    @PrimaryKey val artistKey: String,
    val name: String,
    val listenedMs: Long,
    val countedPlays: Int,
    val completedCount: Int,
    val eventCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)

data class ListenLifetimeLastPlayed(
    val trackKey: String,
    val lastPlayedAt: Long
)

data class ListenLifetimeTotals(
    val listenedMs: Long,
    val countedPlays: Int,
    val completedCount: Int,
    val eventCount: Int,
    val distinctTracks: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)
