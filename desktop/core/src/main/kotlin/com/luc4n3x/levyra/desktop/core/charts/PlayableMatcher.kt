package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.model.Track

object PlayableMatcher {
    private const val TITLE_EXACT = 600
    private const val TITLE_CONTAINED = 380
    private const val ARTIST_EXACT = 320
    private const val TOKEN_WEIGHT = 40
    private const val LONG_TRACK_PENALTY = 260
    private const val LONG_TRACK_MS = 12L * 60L * 1000L
    private const val MINIMUM_SCORE = 200

    fun best(reference: Track, candidates: List<Track>): Track? = candidates
        .asSequence()
        .filter { it.videoUrl.isNotBlank() }
        .map { candidate -> candidate to score(reference, candidate) }
        .filter { (_, score) -> score >= MINIMUM_SCORE }
        .maxByOrNull { (_, score) -> score }
        ?.first

    fun score(reference: Track, candidate: Track): Int {
        val referenceTitle = ChartFeedParser.normalize(reference.title)
        val candidateTitle = ChartFeedParser.normalize(candidate.title)
        if (referenceTitle.isBlank() || candidateTitle.isBlank()) return 0

        val titleScore = when {
            referenceTitle == candidateTitle -> TITLE_EXACT
            candidateTitle.contains(referenceTitle) || referenceTitle.contains(candidateTitle) -> TITLE_CONTAINED
            else -> tokenOverlap(referenceTitle, candidateTitle) * TOKEN_WEIGHT
        }

        val referenceArtist = ChartFeedParser.normalize(reference.artist)
        val candidateArtist = ChartFeedParser.normalize(candidate.artist)
        val artistScore = when {
            referenceArtist.isBlank() || candidateArtist.isBlank() -> 0
            referenceArtist == candidateArtist -> ARTIST_EXACT
            candidateArtist.contains(referenceArtist) || referenceArtist.contains(candidateArtist) -> ARTIST_EXACT / 2
            else -> tokenOverlap(referenceArtist, candidateArtist) * TOKEN_WEIGHT
        }

        val durationPenalty = if (candidate.durationMs > LONG_TRACK_MS) LONG_TRACK_PENALTY else 0
        return (titleScore + artistScore - durationPenalty).coerceAtLeast(0)
    }

    private fun tokenOverlap(left: String, right: String): Int {
        val leftTokens = left.split(' ').filter { it.length > 2 }.toSet()
        if (leftTokens.isEmpty()) return 0
        val rightTokens = right.split(' ').filter { it.length > 2 }.toSet()
        return leftTokens.count { it in rightTokens }
    }
}
