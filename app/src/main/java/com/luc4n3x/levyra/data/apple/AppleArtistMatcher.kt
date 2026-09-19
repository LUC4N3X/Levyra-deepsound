package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText

internal object AppleArtistMatcher {
    fun score(referenceArtist: String, candidateArtist: String): Int {
        val reference = AlternativeTrackText.normalizeArtist(referenceArtist)
        val candidate = AlternativeTrackText.normalizeArtist(candidateArtist)
        if (reference == candidate) return 20
        return (AppleMatchScoring.tokenCoverage(reference, candidate) * 15).toInt()
    }

    fun areCompatible(referenceArtist: String, candidateArtist: String): Boolean {
        val reference = AlternativeTrackText.normalizeArtist(referenceArtist)
        val candidate = AlternativeTrackText.normalizeArtist(candidateArtist)
        if (reference.isBlank() || candidate.isBlank()) return false
        if (reference == candidate) return true
        if (hasStructuredCreditOverlap(referenceArtist, candidateArtist)) return true
        return hasMutualCoverage(
            AlternativeTrackText.artistCredit(referenceArtist).primary,
            AlternativeTrackText.artistCredit(candidateArtist).primary
        )
    }

    private fun hasStructuredCreditOverlap(referenceArtist: String, candidateArtist: String): Boolean {
        val reference = AlternativeTrackText.artistCredit(referenceArtist)
        val candidate = AlternativeTrackText.artistCredit(candidateArtist)
        if (reference.primary.isBlank() || candidate.primary.isBlank()) return false
        if (reference.primary == candidate.primary) return true
        return reference.primary in candidate.names || candidate.primary in reference.names
    }

    private fun hasMutualCoverage(reference: String, candidate: String): Boolean {
        val referenceCoverage = AppleMatchScoring.tokenCoverage(reference, candidate)
        val candidateCoverage = AppleMatchScoring.tokenCoverage(candidate, reference)
        return minOf(referenceCoverage, candidateCoverage) >= 0.50
    }

}
