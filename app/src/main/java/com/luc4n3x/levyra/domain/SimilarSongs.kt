package com.luc4n3x.levyra.domain

object SimilarSongsSelector {

    const val POOL_SIZE: Int = 24
    const val DISPLAY_LIMIT: Int = 12

    private const val SEED_TITLE_CONTAINS_MIN_LENGTH = 6

    fun select(
        candidates: List<Track>,
        seed: Track?,
        excludedIdentities: Set<String>,
        limit: Int = DISPLAY_LIMIT
    ): List<Track> {
        if (limit <= 0 || candidates.isEmpty()) return emptyList()
        val blocked = HashSet<String>(excludedIdentities.size + 1)
        blocked.addAll(excludedIdentities)
        seed?.let { blocked.add(LevyraPersonalOrbit.identityKey(it)) }
        val seedTitle = seed?.let(LevyraPersonalOrbit::musicTitleKey).orEmpty()
        val seedArtists = seed?.let(LevyraPersonalOrbit::artistKeys).orEmpty()

        val selected = LinkedHashMap<String, Track>(limit * 2)
        for (candidate in candidates) {
            if (candidate.id.isBlank() || candidate.title.isBlank()) continue
            val identity = LevyraPersonalOrbit.identityKey(candidate)
            if (identity in blocked) continue
            if (isSeedDerivative(candidate, seedTitle, seedArtists)) continue
            val existing = selected[identity]
            when {
                existing == null -> {
                    if (selected.size >= limit) continue
                    selected[identity] = candidate
                }

                prefersOver(candidate, existing) -> selected[identity] = candidate
            }
        }
        return selected.values.toList()
    }

    private fun isSeedDerivative(candidate: Track, seedTitle: String, seedArtists: Set<String>): Boolean {
        if (seedTitle.isBlank()) return false
        val candidateTitle = LevyraPersonalOrbit.musicTitleKey(candidate)
        if (candidateTitle.isBlank()) return false
        val repeatsSeedTitle = candidateTitle == seedTitle ||
            (seedTitle.length >= SEED_TITLE_CONTAINS_MIN_LENGTH && candidateTitle.contains(seedTitle))
        if (!repeatsSeedTitle) return false
        if (seedArtists.isEmpty()) return true
        val candidateArtists = LevyraPersonalOrbit.artistKeys(candidate)
        if (candidateArtists.isEmpty()) return true
        return candidateArtists.none { artist -> artist in seedArtists }
    }

    private fun prefersOver(candidate: Track, current: Track): Boolean {
        val candidateIsVideo = YoutubeMusicVideoType.isVideo(candidate.videoType)
        val currentIsVideo = YoutubeMusicVideoType.isVideo(current.videoType)
        if (candidateIsVideo != currentIsVideo) return currentIsVideo
        if (candidate.durationMs > 0L && current.durationMs <= 0L) return true
        return false
    }
}
