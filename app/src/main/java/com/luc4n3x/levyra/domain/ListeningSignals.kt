package com.luc4n3x.levyra.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ListeningSignalWeights(
    val completion: Int = 40,
    val repeat: Int = 26,
    val favorite: Int = 34,
    val playlist: Int = 22,
    val followedArtist: Int = 30,
    val artistAffinity: Int = 24,
    val skipPenalty: Int = 46,
    val artistSkipPenalty: Int = 28,
    val recencyBonus: Int = 12
) {
    companion object {
        val Default = ListeningSignalWeights()
    }
}

data class TrackListeningSignal(
    val key: String,
    val plays: Int,
    val countedPlays: Int,
    val completionRatio: Double,
    val skips: Int,
    val earlySkips: Int,
    val lastPlayedAt: Long
) {
    val skipRatio: Double
        get() = if (plays <= 0) 0.0 else skips.toDouble() / plays.toDouble()
}

data class ArtistListeningSignal(
    val key: String,
    val plays: Int,
    val countedPlays: Int,
    val listenedMs: Long,
    val completionRatio: Double,
    val skips: Int,
    val distinctTracks: Int
) {
    val skipRatio: Double
        get() = if (plays <= 0) 0.0 else skips.toDouble() / plays.toDouble()
}

data class ListeningSignalProfile(
    val tracks: Map<String, TrackListeningSignal> = emptyMap(),
    val artists: Map<String, ArtistListeningSignal> = emptyMap(),
    val favoriteKeys: Set<String> = emptySet(),
    val playlistKeys: Set<String> = emptySet(),
    val followedArtistKeys: Set<String> = emptySet(),
    val referenceNowMs: Long = 0L,
    val weights: ListeningSignalWeights = ListeningSignalWeights.Default
) {
    val hasSignal: Boolean
        get() = tracks.isNotEmpty() || artists.isNotEmpty() ||
            favoriteKeys.isNotEmpty() || playlistKeys.isNotEmpty() || followedArtistKeys.isNotEmpty()

    fun trackScore(track: Track): Int {
        val key = ListenIdentity.trackKey(track.id, track.title, track.artist)
        val signal = tracks[key]
        var score = 0
        if (signal != null) {
            score += (signal.completionRatio * weights.completion).toInt()
            score += min(signal.countedPlays, REPEAT_CAP) * weights.repeat / REPEAT_CAP
            score -= (signal.skipRatio * weights.skipPenalty).toInt()
            if (signal.earlySkips >= STRONG_NEGATIVE_SKIPS && signal.countedPlays == 0) {
                score -= weights.skipPenalty
            }
            score += recencyBonus(signal.lastPlayedAt)
        }
        if (key in favoriteKeys) score += weights.favorite
        if (key in playlistKeys) score += weights.playlist
        score += artistScore(track.artist)
        return score
    }

    fun artistScore(artist: String): Int {
        val names = ListeningSignalEngine.splitArtists(artist)
        if (names.isEmpty()) return 0
        return names.maxOf { name ->
            val key = ListenIdentity.artistKey(name)
            var score = 0
            if (key in followedArtistKeys) score += weights.followedArtist
            artists[key]?.let { signal ->
                score += (signal.completionRatio * weights.artistAffinity).toInt()
                score -= (signal.skipRatio * weights.artistSkipPenalty).toInt()
            }
            score
        }
    }

    fun isSuppressed(track: Track): Boolean {
        val key = ListenIdentity.trackKey(track.id, track.title, track.artist)
        if (key in favoriteKeys || key in playlistKeys) return false
        val signal = tracks[key] ?: return false
        if (signal.countedPlays > 0) return false
        return signal.earlySkips >= STRONG_NEGATIVE_SKIPS && signal.completionRatio <= SUPPRESSION_COMPLETION
    }

    fun isArtistSuppressed(artist: String): Boolean {
        val names = ListeningSignalEngine.splitArtists(artist)
        if (names.isEmpty()) return false
        if (names.any { ListenIdentity.artistKey(it) in followedArtistKeys }) return false
        return names.all { name ->
            val signal = artists[ListenIdentity.artistKey(name)] ?: return false
            signal.countedPlays == 0 &&
                signal.plays >= STRONG_NEGATIVE_ARTIST_PLAYS &&
                signal.skipRatio >= SUPPRESSION_ARTIST_SKIP_RATIO
        }
    }

    private fun recencyBonus(lastPlayedAt: Long): Int {
        if (lastPlayedAt <= 0L || referenceNowMs <= 0L) return 0
        val age = referenceNowMs - lastPlayedAt
        if (age < 0L) return 0
        if (age >= RECENCY_WINDOW_MS) return 0
        val remaining = (RECENCY_WINDOW_MS - age).toDouble() / RECENCY_WINDOW_MS.toDouble()
        return (remaining * weights.recencyBonus).toInt()
    }

    internal companion object {
        const val REPEAT_CAP = 6
        const val STRONG_NEGATIVE_SKIPS = 3
        const val STRONG_NEGATIVE_ARTIST_PLAYS = 4
        const val SUPPRESSION_COMPLETION = 0.25
        const val SUPPRESSION_ARTIST_SKIP_RATIO = 0.8
        const val RECENCY_WINDOW_MS = 21L * 24L * 60L * 60L * 1000L
    }
}

object ListeningSignalEngine {

    const val EARLY_SKIP_RATIO = 0.2
    const val SKIP_RATIO = 0.5

    private val separatorPattern = Regex(
        """\s*(?:feat\.?|featuring|ft\.?|&|,|;|\+|/|\bx\b|\bvs\.?\b)\s*""",
        RegexOption.IGNORE_CASE
    )

    fun splitArtists(artist: String): List<String> = artist
        .split(separatorPattern)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }

    fun build(
        events: List<ListenEvent>,
        favorites: List<Track> = emptyList(),
        playlistTracks: List<Track> = emptyList(),
        followedArtists: List<String> = emptyList(),
        nowMs: Long = System.currentTimeMillis(),
        weights: ListeningSignalWeights = ListeningSignalWeights.Default
    ): ListeningSignalProfile {
        val trackAccumulators = LinkedHashMap<String, TrackAccumulator>()
        val artistAccumulators = LinkedHashMap<String, ArtistAccumulator>()

        events.forEach { event ->
            val ratio = completionRatio(event)
            val counted = ListenPlayPolicy.isCountedPlay(event)
            val skipped = !event.completed && !counted && ratio <= SKIP_RATIO
            val earlySkipped = !event.completed && !counted && ratio <= EARLY_SKIP_RATIO

            val trackKey = ListenIdentity.trackKey(event)
            val track = trackAccumulators.getOrPut(trackKey) { TrackAccumulator(trackKey) }
            track.accept(ratio, counted, skipped, earlySkipped, event.startedAt)

            splitArtists(event.artist).forEach { name ->
                val artistKey = ListenIdentity.artistKey(name)
                if (artistKey.isBlank()) return@forEach
                val artist = artistAccumulators.getOrPut(artistKey) { ArtistAccumulator(artistKey) }
                artist.accept(ratio, counted, skipped, event.listenedMs, trackKey)
            }
        }

        return ListeningSignalProfile(
            tracks = trackAccumulators.mapValues { it.value.toSignal() },
            artists = artistAccumulators.mapValues { it.value.toSignal() },
            favoriteKeys = favorites.mapTo(LinkedHashSet()) { ListenIdentity.trackKey(it.id, it.title, it.artist) },
            playlistKeys = playlistTracks.mapTo(LinkedHashSet()) { ListenIdentity.trackKey(it.id, it.title, it.artist) },
            followedArtistKeys = followedArtists
                .flatMap(::splitArtists)
                .mapTo(LinkedHashSet(), ListenIdentity::artistKey)
                .filterTo(LinkedHashSet(), String::isNotBlank),
            referenceNowMs = nowMs,
            weights = weights
        )
    }

    fun completionRatio(event: ListenEvent): Double {
        if (event.completed) return 1.0
        if (event.trackDurationMs <= 0L) return if (event.listenedMs >= ListenPlayPolicy.COUNTED_PLAY_MS) 1.0 else 0.0
        return min(1.0, max(0.0, event.listenedMs.toDouble() / event.trackDurationMs.toDouble()))
    }

    private class TrackAccumulator(val key: String) {
        var plays = 0
        var counted = 0
        var skips = 0
        var earlySkips = 0
        var ratioTotal = 0.0
        var lastPlayedAt = 0L

        fun accept(ratio: Double, counted: Boolean, skipped: Boolean, earlySkipped: Boolean, startedAt: Long) {
            plays += 1
            ratioTotal += ratio
            if (counted) this.counted += 1
            if (skipped) skips += 1
            if (earlySkipped) earlySkips += 1
            if (startedAt > lastPlayedAt) lastPlayedAt = startedAt
        }

        fun toSignal(): TrackListeningSignal = TrackListeningSignal(
            key = key,
            plays = plays,
            countedPlays = counted,
            completionRatio = if (plays <= 0) 0.0 else ratioTotal / plays.toDouble(),
            skips = skips,
            earlySkips = earlySkips,
            lastPlayedAt = lastPlayedAt
        )
    }

    private class ArtistAccumulator(val key: String) {
        var plays = 0
        var counted = 0
        var skips = 0
        var listenedMs = 0L
        var ratioTotal = 0.0
        val trackKeys = LinkedHashSet<String>()

        fun accept(ratio: Double, counted: Boolean, skipped: Boolean, listenedMs: Long, trackKey: String) {
            plays += 1
            ratioTotal += ratio
            if (counted) this.counted += 1
            if (skipped) skips += 1
            this.listenedMs += max(0L, listenedMs)
            trackKeys += trackKey
        }

        fun toSignal(): ArtistListeningSignal = ArtistListeningSignal(
            key = key,
            plays = plays,
            countedPlays = counted,
            listenedMs = listenedMs,
            completionRatio = if (plays <= 0) 0.0 else ratioTotal / plays.toDouble(),
            skips = skips,
            distinctTracks = trackKeys.size
        )
    }
}

object ListeningSignalRanker {

    const val DEFAULT_ARTIST_RUN_LIMIT = 2

    fun rank(
        candidates: List<Track>,
        profile: ListeningSignalProfile,
        limit: Int = candidates.size,
        contextArtist: String = "",
        artistRunLimit: Int = DEFAULT_ARTIST_RUN_LIMIT,
        dropSuppressed: Boolean = true
    ): List<Track> {
        if (candidates.isEmpty()) return emptyList()
        val max = limit.coerceAtLeast(1)
        if (!profile.hasSignal) return candidates.take(max)

        val contextKeys = ListeningSignalEngine.splitArtists(contextArtist)
            .mapTo(LinkedHashSet(), ListenIdentity::artistKey)
            .filterTo(LinkedHashSet(), String::isNotBlank)

        val pool = if (!dropSuppressed) {
            candidates
        } else {
            candidates.filterNot { candidate ->
                if (matchesContext(candidate, contextKeys)) return@filterNot false
                profile.isSuppressed(candidate) || profile.isArtistSuppressed(candidate.artist)
            }.ifEmpty { candidates }
        }

        val scored = pool.mapIndexed { index, candidate ->
            ScoredCandidate(
                track = candidate,
                score = profile.trackScore(candidate) + if (matchesContext(candidate, contextKeys)) CONTEXT_BONUS else 0,
                originalIndex = index
            )
        }.sortedWith(compareByDescending<ScoredCandidate> { it.score }.thenBy { it.originalIndex })

        return diversify(scored, max, artistRunLimit, contextKeys)
    }

    private fun diversify(
        scored: List<ScoredCandidate>,
        limit: Int,
        artistRunLimit: Int,
        contextKeys: Set<String>
    ): List<Track> {
        val runCap = artistRunLimit.coerceAtLeast(1)
        val selected = ArrayList<Track>(min(limit, scored.size))
        val pending = scored.toMutableList()
        var previousArtistKey = ""
        var consecutiveArtistCount = 0

        while (selected.size < limit && pending.isNotEmpty()) {
            val eligibleIndex = pending.indexOfFirst { candidate ->
                val key = primaryArtistKey(candidate.track.artist)
                key.isBlank() ||
                    key in contextKeys ||
                    key != previousArtistKey ||
                    consecutiveArtistCount < runCap
            }
            val index = if (eligibleIndex >= 0) eligibleIndex else 0
            val candidate = pending.removeAt(index)
            selected += candidate.track

            val key = primaryArtistKey(candidate.track.artist)
            if (key.isBlank()) {
                previousArtistKey = ""
                consecutiveArtistCount = 0
            } else if (key == previousArtistKey) {
                consecutiveArtistCount += 1
            } else {
                previousArtistKey = key
                consecutiveArtistCount = 1
            }
        }
        return selected
    }

    private fun matchesContext(track: Track, contextKeys: Set<String>): Boolean {
        if (contextKeys.isEmpty()) return false
        return ListeningSignalEngine.splitArtists(track.artist)
            .any { ListenIdentity.artistKey(it) in contextKeys }
    }

    private fun primaryArtistKey(artist: String): String =
        ListeningSignalEngine.splitArtists(artist).firstOrNull()?.let(ListenIdentity::artistKey).orEmpty()

    private data class ScoredCandidate(
        val track: Track,
        val score: Int,
        val originalIndex: Int
    )

    private const val CONTEXT_BONUS = 240
}
