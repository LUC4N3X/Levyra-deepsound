package com.luc4n3x.levyra.domain

import java.util.Locale

/** A local listening event with the metadata needed to materialize a smart playlist. */
data class SmartPlaylistListen(
    val track: Track,
    val listenedMs: Long,
    val startedAt: Long
)

/** Builds deterministic, entirely local smart playlists from listening history. */
fun rankMostPlayedTracks(
    listens: List<SmartPlaylistListen>,
    limit: Int = 40
): List<Track> = listens
    .asSequence()
    .filter { it.listenedMs >= 0L && it.startedAt > 0L && it.track.title.isNotBlank() }
    .groupBy { smartPlaylistTrackKey(it.track) }
    .values
    .map { group ->
        val newest = group.maxBy { it.startedAt }
        RankedSmartTrack(
            track = newest.track.copy(streamUrl = "", videoStreamUrl = ""),
            listenedMs = group.sumOf { it.listenedMs.coerceAtLeast(0L) },
            playCount = group.size,
            lastPlayedAt = newest.startedAt
        )
    }
    .sortedWith(
        compareByDescending<RankedSmartTrack> { it.listenedMs }
            .thenByDescending { it.playCount }
            .thenByDescending { it.lastPlayedAt }
            .thenBy { it.track.title.lowercase(Locale.ROOT) }
            .thenBy { smartPlaylistTrackKey(it.track) }
    )
    .take(limit.coerceIn(1, MAX_SMART_PLAYLIST_SIZE))
    .map { it.track }

private data class RankedSmartTrack(
    val track: Track,
    val listenedMs: Long,
    val playCount: Int,
    val lastPlayedAt: Long
)

private fun smartPlaylistTrackKey(track: Track): String = track.id.trim().takeIf(String::isNotBlank)
    ?: "${track.title.trim().lowercase(Locale.ROOT)}|${track.artist.trim().lowercase(Locale.ROOT)}"

private const val MAX_SMART_PLAYLIST_SIZE = 100
