package com.luc4n3x.levyra.domain

enum class RecommendationFeedbackKind {
    MORE_LIKE_THIS,
    LESS_LIKE_THIS
}

data class RecommendationFeedbackEntry(
    val trackKey: String,
    val artistKeys: List<String>,
    val kind: RecommendationFeedbackKind,
    val updatedAt: Long
)

data class RecommendationFeedbackWeights(
    val preferredTrack: Int = 120,
    val avoidedTrack: Int = 150,
    val artistAffinityStep: Int = 26,
    val artistAffinityCap: Int = 3
) {
    companion object {
        val Default = RecommendationFeedbackWeights()
    }
}

data class RecommendationFeedback(
    val preferredTrackKeys: Set<String> = emptySet(),
    val avoidedTrackKeys: Set<String> = emptySet(),
    val artistAffinity: Map<String, Int> = emptyMap(),
    val weights: RecommendationFeedbackWeights = RecommendationFeedbackWeights.Default
) {
    val isEmpty: Boolean
        get() = preferredTrackKeys.isEmpty() && avoidedTrackKeys.isEmpty() && artistAffinity.isEmpty()

    fun kindFor(track: Track): RecommendationFeedbackKind? {
        val key = ListenIdentity.trackKey(track.id, track.title, track.artist)
        return when (key) {
            in preferredTrackKeys -> RecommendationFeedbackKind.MORE_LIKE_THIS
            in avoidedTrackKeys -> RecommendationFeedbackKind.LESS_LIKE_THIS
            else -> null
        }
    }

    fun trackScore(track: Track): Int {
        if (isEmpty) return 0
        val key = ListenIdentity.trackKey(track.id, track.title, track.artist)
        var score = 0
        if (key in preferredTrackKeys) score += weights.preferredTrack
        if (key in avoidedTrackKeys) score -= weights.avoidedTrack
        return score + artistScore(track.artist)
    }

    fun artistScore(artist: String): Int {
        if (artistAffinity.isEmpty()) return 0
        val names = ListeningSignalEngine.splitArtists(artist)
        if (names.isEmpty()) return 0
        var total = 0
        names.forEach { name ->
            val affinity = artistAffinity[ListenIdentity.artistKey(name)] ?: 0
            if (affinity != 0) total += affinity * weights.artistAffinityStep
        }
        return total
    }

    companion object {
        val Empty = RecommendationFeedback()

        const val MAX_ENTRIES = 240

        fun from(
            entries: List<RecommendationFeedbackEntry>,
            weights: RecommendationFeedbackWeights = RecommendationFeedbackWeights.Default
        ): RecommendationFeedback {
            if (entries.isEmpty()) return Empty
            val preferred = LinkedHashSet<String>()
            val avoided = LinkedHashSet<String>()
            val affinity = LinkedHashMap<String, Int>()
            entries.asSequence().take(MAX_ENTRIES).forEach { entry ->
                val key = entry.trackKey.trim()
                val step = when (entry.kind) {
                    RecommendationFeedbackKind.MORE_LIKE_THIS -> {
                        if (key.isNotEmpty()) preferred += key
                        1
                    }

                    RecommendationFeedbackKind.LESS_LIKE_THIS -> {
                        if (key.isNotEmpty()) avoided += key
                        -1
                    }
                }
                entry.artistKeys.forEach { artistKey ->
                    val clean = artistKey.trim()
                    if (clean.isEmpty()) return@forEach
                    affinity[clean] = (affinity[clean] ?: 0) + step
                }
            }
            return RecommendationFeedback(
                preferredTrackKeys = preferred,
                avoidedTrackKeys = avoided,
                artistAffinity = affinity
                    .mapValues { (_, value) ->
                        value.coerceIn(-weights.artistAffinityCap, weights.artistAffinityCap)
                    }
                    .filterValues { it != 0 },
                weights = weights
            )
        }

        fun entryFor(
            track: Track,
            kind: RecommendationFeedbackKind,
            nowMs: Long = System.currentTimeMillis()
        ): RecommendationFeedbackEntry? {
            val trackKey = ListenIdentity.trackKey(track.id, track.title, track.artist).trim()
            if (trackKey.isEmpty() || trackKey == "|") return null
            val artistKeys = ListeningSignalEngine.splitArtists(track.artist)
                .map(ListenIdentity::artistKey)
                .filter(String::isNotEmpty)
                .distinct()
            return RecommendationFeedbackEntry(
                trackKey = trackKey,
                artistKeys = artistKeys,
                kind = kind,
                updatedAt = nowMs
            )
        }
    }
}
