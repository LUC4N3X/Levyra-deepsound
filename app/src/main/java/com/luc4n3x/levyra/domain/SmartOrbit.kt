package com.luc4n3x.levyra.domain

import kotlin.math.min

data class SmartOrbitCandidate(
    val track: Track,
    val seedKeys: List<String>,
    val lastSeenAt: Long
) {
    val key: String = SmartOrbitEngine.trackKey(track)

    val coOccurrence: Int
        get() = seedKeys.size
}

data class SmartOrbitPool(val candidates: List<SmartOrbitCandidate> = emptyList()) {
    val isEmpty: Boolean
        get() = candidates.isEmpty()

    val bonusScores: Map<String, Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        candidates.associate { it.key to SmartOrbitEngine.coOccurrenceBonus(it.coOccurrence) }
    }

    private val keys: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        candidates.mapTo(HashSet(candidates.size * 2)) { it.key }
    }

    fun contains(track: Track): Boolean = SmartOrbitEngine.trackKey(track) in keys

    companion object {
        val Empty = SmartOrbitPool()
    }
}

object SmartOrbitEngine {
    const val MAX_CANDIDATES = 160
    const val MAX_SEEDS_PER_CANDIDATE = 8
    const val RELATED_PER_SEED = 12
    const val DISCOVERY_SLOTS = 5
    const val MIN_CO_OCCURRENCE = 2
    const val CO_OCCURRENCE_STEP = 48
    const val CO_OCCURRENCE_CAP = 4
    const val CANDIDATE_TTL_MS = 30L * 24L * 60L * 60L * 1000L

    fun trackKey(track: Track): String = ListenIdentity.trackKey(track.id, track.title, track.artist)

    fun coOccurrenceBonus(coOccurrence: Int): Int =
        min(coOccurrence.coerceAtLeast(0), CO_OCCURRENCE_CAP) * CO_OCCURRENCE_STEP

    fun accumulate(
        pool: SmartOrbitPool,
        seed: Track,
        related: List<Track>,
        nowMs: Long
    ): SmartOrbitPool {
        val seedKey = trackKey(seed)
        if (seedKey.isBlank() || seedKey == "|") return prune(pool, nowMs)
        val byKey = LinkedHashMap<String, SmartOrbitCandidate>(pool.candidates.size + RELATED_PER_SEED)
        pool.candidates.forEach { byKey[it.key] = it }
        val seedTitle = LevyraPersonalOrbit.musicTitleKey(seed)
        related.asSequence()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .filterNot { LevyraPersonalOrbit.sameRecording(it, seed) }
            .filterNot { seedTitle.isNotBlank() && LevyraPersonalOrbit.musicTitleKey(it) == seedTitle }
            .distinctBy(::trackKey)
            .take(RELATED_PER_SEED)
            .forEach { track ->
                val key = trackKey(track)
                if (key == seedKey) return@forEach
                val existing = byKey[key]
                val seeds = existing?.seedKeys.orEmpty() - seedKey + seedKey
                byKey[key] = SmartOrbitCandidate(
                    track = track.copy(streamUrl = "", videoStreamUrl = ""),
                    seedKeys = seeds.takeLast(MAX_SEEDS_PER_CANDIDATE),
                    lastSeenAt = nowMs
                )
            }
        return prune(SmartOrbitPool(byKey.values.toList()), nowMs)
    }

    fun prune(pool: SmartOrbitPool, nowMs: Long): SmartOrbitPool {
        if (pool.isEmpty) return pool
        val fresh = pool.candidates.filter { candidate ->
            candidate.seedKeys.isNotEmpty() && nowMs - candidate.lastSeenAt in 0 until CANDIDATE_TTL_MS
        }
        if (fresh.size <= MAX_CANDIDATES) {
            return if (fresh.size == pool.candidates.size) pool else SmartOrbitPool(fresh)
        }
        return SmartOrbitPool(
            fresh.sortedWith(
                compareByDescending<SmartOrbitCandidate> { it.coOccurrence }
                    .thenByDescending { it.lastSeenAt }
            ).take(MAX_CANDIDATES)
        )
    }

    fun discoveries(
        pool: SmartOrbitPool,
        profile: ListeningSignalProfile?,
        isBlocked: (Track) -> Boolean,
        limit: Int = DISCOVERY_SLOTS
    ): List<Track> {
        if (pool.isEmpty || limit <= 0) return emptyList()
        val eligible = pool.candidates.filter { candidate ->
            candidate.coOccurrence >= MIN_CO_OCCURRENCE &&
                !isKnownToListener(candidate, profile) &&
                !isBlocked(candidate.track)
        }
        if (eligible.isEmpty()) return emptyList()
        val ranked = eligible
            .map { it to candidateScore(it, profile) }
            .sortedWith(
                compareByDescending<Pair<SmartOrbitCandidate, Int>> { it.second }
                    .thenByDescending { it.first.lastSeenAt }
                    .thenBy { it.first.key }
            )
        val usedArtists = HashSet<String>()
        val selected = ArrayList<Track>(limit)
        for ((candidate, _) in ranked) {
            if (selected.size >= limit) break
            val artist = primaryArtistKey(candidate.track.artist)
            if (artist.isNotEmpty() && artist in usedArtists) continue
            if (selected.any { LevyraPersonalOrbit.sameRecording(it, candidate.track) }) continue
            if (artist.isNotEmpty()) usedArtists += artist
            selected += candidate.track
        }
        return selected
    }

    private fun isKnownToListener(candidate: SmartOrbitCandidate, profile: ListeningSignalProfile?): Boolean {
        if (profile == null) return false
        val key = candidate.key
        if (key in profile.tracks || key in profile.favoriteKeys || key in profile.playlistKeys) return true
        val recordingKey = LevyraPersonalOrbit.recordingIdentityKey(candidate.track.title, candidate.track.artist)
        if (recordingKey.isNotBlank() && recordingKey in profile.knownRecordingKeys) return true
        if (profile.feedback.isExplicitlyAvoided(candidate.track)) return true
        return profile.isArtistSuppressed(candidate.track.artist)
    }

    private fun candidateScore(candidate: SmartOrbitCandidate, profile: ListeningSignalProfile?): Int {
        val artistScore = profile?.let {
            it.artistScore(candidate.track.artist) + it.feedback.artistScore(candidate.track.artist)
        } ?: 0
        return coOccurrenceBonus(candidate.coOccurrence) + artistScore
    }

    private fun primaryArtistKey(artist: String): String =
        ListeningSignalEngine.splitArtists(artist).firstOrNull()?.let(ListenIdentity::artistKey).orEmpty()
}
