package com.luc4n3x.levyra.domain

import java.util.concurrent.TimeUnit

object ForgottenFavorites {
    const val DEFAULT_THRESHOLD_DAYS = 30L
    const val DISPLAY_LIMIT = 20

    fun select(
        favorites: List<Track>,
        lastPlayedByKey: Map<String, Long>,
        nowMs: Long = System.currentTimeMillis(),
        thresholdDays: Long = DEFAULT_THRESHOLD_DAYS,
        limit: Int = DISPLAY_LIMIT
    ): List<Track> {
        if (favorites.isEmpty() || lastPlayedByKey.isEmpty() || limit <= 0) return emptyList()
        val cutoff = nowMs - TimeUnit.DAYS.toMillis(thresholdDays.coerceAtLeast(1L))
        if (cutoff <= 0L) return emptyList()

        val seen = HashSet<String>(favorites.size)
        val candidates = ArrayList<Candidate>()
        favorites.forEach { track ->
            if (!isPlayableFavorite(track)) return@forEach
            val key = ListenIdentity.trackKey(track.id, track.title, track.artist)
            if (key.isEmpty() || !seen.add(key)) return@forEach
            val lastPlayedAt = lastPlayedByKey[key] ?: return@forEach
            if (lastPlayedAt !in 1..cutoff) return@forEach
            candidates += Candidate(track, lastPlayedAt)
        }
        if (candidates.isEmpty()) return emptyList()

        candidates.sortWith(
            compareBy<Candidate> { it.lastPlayedAt }
                .thenBy { it.track.title.lowercase() }
        )
        return candidates.take(limit).map { it.track }
    }

    fun listeningKeys(favorites: List<Track>): List<String> =
        favorites.asSequence()
            .filter(::isPlayableFavorite)
            .map { ListenIdentity.trackKey(it.id, it.title, it.artist) }
            .filter(String::isNotEmpty)
            .distinct()
            .toList()

    private fun isPlayableFavorite(track: Track): Boolean =
        track.title.isNotBlank() && (track.id.isNotBlank() || track.videoUrl.isNotBlank())

    private data class Candidate(val track: Track, val lastPlayedAt: Long)
}
