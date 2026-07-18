package com.luc4n3x.levyra.feature.motion

import kotlin.math.abs

object CanonicalTrackMatcher {
    private val unsafeTerms = setOf(
        "karaoke",
        "tribute",
        "cover",
        "nightcore",
        "sped up",
        "slowed",
        "instrumental",
        "dj mix",
        "playlist",
        "essentials",
        "set list",
        "session"
    )

    private val editionTerms = setOf(
        "deluxe",
        "expanded",
        "remaster",
        "remastered",
        "remix",
        "live",
        "acoustic",
        "version",
        "edit",
        "mix"
    )

    fun match(
        reference: MotionTrackIdentity,
        candidate: MotionArtworkCandidate
    ): MotionArtworkMatch {
        val source = candidate.identity
        if (candidate.url.isBlank() || !candidate.url.startsWith("https://")) return MotionArtworkMatch(false, 0)
        if (containsUnexpectedUnsafeTerm(reference, source)) return MotionArtworkMatch(false, 0)
        if (!artistsCompatible(reference.artists, source.artists, candidate.scope)) return MotionArtworkMatch(false, 0)
        if (!editionsCompatible(reference, source, candidate.scope)) return MotionArtworkMatch(false, 0)

        var score = 30
        val exactIsrc = reference.isrc.isNotBlank() && source.isrc.isNotBlank() && reference.isrc == source.isrc
        val conflictingIsrc = reference.isrc.isNotBlank() && source.isrc.isNotBlank() && reference.isrc != source.isrc
        if (conflictingIsrc) return MotionArtworkMatch(false, 0)
        if (exactIsrc) score += 38

        val artistCoverage = artistCoverage(reference.artists, source.artists)
        score += if (candidate.scope == MotionArtworkScope.ALBUM) {
            24
        } else {
            when {
                artistCoverage >= 1.0 -> 24
                artistCoverage >= 0.67 -> 18
                else -> 10
            }
        }

        val titleSimilarity = textSimilarity(reference.title, source.title)
        val albumSimilarity = textSimilarity(reference.album, source.album)

        if (candidate.scope == MotionArtworkScope.TRACK) {
            if (titleSimilarity < 0.72 && !exactIsrc) return MotionArtworkMatch(false, 0)
            score += when {
                titleSimilarity >= 1.0 -> 24
                titleSimilarity >= 0.88 -> 20
                titleSimilarity >= 0.72 -> 14
                else -> 0
            }
            if (albumSimilarity >= 1.0) score += 10 else if (albumSimilarity >= 0.75) score += 6
            if (reference.durationMs > 0L && source.durationMs > 0L) {
                val delta = abs(reference.durationMs - source.durationMs)
                score += when {
                    delta <= 3_000L -> 8
                    delta <= 7_000L -> 5
                    delta <= 15_000L -> 1
                    else -> -12
                }
            }
        } else {
            if (reference.album.isBlank() || albumSimilarity < 0.82) return MotionArtworkMatch(false, 0)
            score += when {
                albumSimilarity >= 1.0 -> 30
                albumSimilarity >= 0.9 -> 25
                else -> 20
            }
            if (reference.upc.isNotBlank() && source.upc.isNotBlank()) {
                if (reference.upc != source.upc) return MotionArtworkMatch(false, 0)
                score += 12
            }
        }

        if (reference.year.isNotBlank() && source.year.isNotBlank() && reference.year == source.year) score += 4
        val bounded = score.coerceIn(0, 100)
        return MotionArtworkMatch(bounded >= MINIMUM_STRUCTURAL_SCORE, bounded)
    }

    private fun artistsCompatible(
        reference: List<String>,
        candidate: List<String>,
        scope: MotionArtworkScope
    ): Boolean {
        if (reference.isEmpty() || candidate.isEmpty()) return false
        val returned = artistAliases(candidate)
        val combinedMatches = combinedArtistSignature(reference) == combinedArtistSignature(candidate)
        val primaryMatches = combinedMatches || normalizeMotionText(reference.first()) in returned
        if (!primaryMatches) return false
        return scope == MotionArtworkScope.ALBUM ||
            combinedMatches ||
            reference.all { normalizeMotionText(it) in returned }
    }

    private fun artistCoverage(reference: List<String>, candidate: List<String>): Double {
        if (reference.isEmpty()) return 0.0
        if (combinedArtistSignature(reference) == combinedArtistSignature(candidate)) return 1.0
        val returned = artistAliases(candidate)
        return reference.count { normalizeMotionText(it) in returned }.toDouble() / reference.size.toDouble()
    }

    private fun textSimilarity(first: String, second: String): Double {
        val left = normalizeMotionText(first)
        val right = normalizeMotionText(second)
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        val leftTokens = left.split(' ').filter { it.isNotBlank() }.toSet()
        val rightTokens = right.split(' ').filter { it.isNotBlank() }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        return (2.0 * intersection) / (leftTokens.size + rightTokens.size).toDouble()
    }

    private fun containsUnexpectedUnsafeTerm(
        reference: MotionTrackIdentity,
        candidate: MotionTrackIdentity
    ): Boolean {
        val target = normalizeMotionText("${reference.title} ${reference.album}")
        val result = normalizeMotionText("${candidate.title} ${candidate.album}")
        return unsafeTerms.any { term -> motionTextContainsTerm(result, term) && !motionTextContainsTerm(target, term) }
    }

    private fun editionsCompatible(
        reference: MotionTrackIdentity,
        candidate: MotionTrackIdentity,
        scope: MotionArtworkScope
    ): Boolean {
        val target = normalizeMotionText(if (scope == MotionArtworkScope.ALBUM) reference.album else reference.title)
        val result = normalizeMotionText(if (scope == MotionArtworkScope.ALBUM) candidate.album else candidate.title)
        return editionTerms.none { term -> motionTextContainsTerm(result, term) != motionTextContainsTerm(target, term) }
    }

    private const val MINIMUM_STRUCTURAL_SCORE = 70
}
