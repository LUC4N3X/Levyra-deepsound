package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText

internal object AppleArtistMatcher {
    fun score(referenceArtist: String, candidateArtist: String): Int {
        val reference = AlternativeTrackText.normalizeArtist(referenceArtist)
        val candidate = AlternativeTrackText.normalizeArtist(candidateArtist)
        if (reference == candidate) return 20
        return (tokenCoverage(reference, candidate) * 15).toInt()
    }

    fun areCompatible(referenceArtist: String, candidateArtist: String): Boolean {
        val reference = AlternativeTrackText.normalizeArtist(referenceArtist)
        val candidate = AlternativeTrackText.normalizeArtist(candidateArtist)
        if (reference.isBlank() || candidate.isBlank()) return false
        if (reference == candidate) return true
        if (hasStructuredCreditOverlap(referenceArtist, candidateArtist)) return true
        if (hasNameOverlap(referenceArtist, candidateArtist)) return true
        return hasMutualCoverage(reference, candidate)
    }

    private fun hasStructuredCreditOverlap(referenceArtist: String, candidateArtist: String): Boolean {
        val reference = AlternativeTrackText.artistCredit(referenceArtist)
        val candidate = AlternativeTrackText.artistCredit(candidateArtist)
        if (reference.primary.isBlank() || candidate.primary.isBlank()) return false
        if (reference.primary == candidate.primary) return true
        return reference.primary in candidate.names || candidate.primary in reference.names
    }

    private fun hasNameOverlap(referenceArtist: String, candidateArtist: String): Boolean {
        val referenceNames = AlternativeTrackText.artistNames(referenceArtist)
        val candidateNames = AlternativeTrackText.artistNames(candidateArtist)
        return referenceNames.any { it in candidateNames } || candidateNames.any { it in referenceNames }
    }

    private fun hasMutualCoverage(reference: String, candidate: String): Boolean {
        val referenceCoverage = tokenCoverage(reference, candidate)
        val candidateCoverage = tokenCoverage(candidate, reference)
        return minOf(referenceCoverage, candidateCoverage) >= 0.50
    }

    private fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.isNotBlank() }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.isNotBlank() }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }
}
