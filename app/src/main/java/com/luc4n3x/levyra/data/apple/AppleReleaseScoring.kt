package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.hqaudio.AlbumEdition

internal object AppleReleaseScoring {
    private const val CORE_MISMATCH_SCORE = -20
    private const val EXACT_RELEASE_SCORE = 30
    private const val EDITION_VARIANT_SCORE = 10
    private const val PARTIAL_RELEASE_SCORE = 5
    private const val STRONG_COVERAGE_SCORE = 18
    private const val GOOD_COVERAGE_SCORE = 12
    private const val MATCHING_TRACK_BONUS = 5
    private const val MISMATCHED_TRACK_PENALTY = -5
    private const val STRONG_COVERAGE_THRESHOLD = 0.90
    private const val GOOD_COVERAGE_THRESHOLD = 0.75
    private const val MIN_COVERAGE_THRESHOLD = 0.50

    fun scoreIdentity(
        referenceCore: String,
        candidateCore: String,
        referenceEditions: Set<AlbumEdition>,
        candidateEditions: Set<AlbumEdition>,
        coresMatch: Boolean
    ): Pair<Int, Boolean> = when {
        coresMatch -> scoreMatchingCores(referenceEditions, candidateEditions)
        referenceCore.isBlank() || candidateCore.isBlank() -> CORE_MISMATCH_SCORE to false
        else -> scoreCoverage(
            mutualCoverage = minOf(
                AppleMatchScoring.tokenCoverage(referenceCore, candidateCore),
                AppleMatchScoring.tokenCoverage(candidateCore, referenceCore)
            ),
            sameEditions = referenceEditions == candidateEditions
        )
    }

    fun scoreTrackPosition(
        coresMatch: Boolean,
        releaseMatch: Boolean,
        referenceTrackNumber: Int,
        candidateTrackNumber: Int
    ): Int {
        val validPosition = referenceTrackNumber > 0 && candidateTrackNumber > 0
        return when {
            !coresMatch || !validPosition -> 0
            referenceTrackNumber == candidateTrackNumber -> MATCHING_TRACK_BONUS
            releaseMatch -> 0
            else -> MISMATCHED_TRACK_PENALTY
        }
    }

    private fun scoreCoverage(
        mutualCoverage: Double,
        sameEditions: Boolean
    ): Pair<Int, Boolean> = when {
        mutualCoverage >= STRONG_COVERAGE_THRESHOLD && sameEditions -> STRONG_COVERAGE_SCORE to true
        mutualCoverage >= GOOD_COVERAGE_THRESHOLD -> GOOD_COVERAGE_SCORE to false
        mutualCoverage >= MIN_COVERAGE_THRESHOLD -> PARTIAL_RELEASE_SCORE to false
        else -> CORE_MISMATCH_SCORE to false
    }

    private fun scoreMatchingCores(
        referenceEditions: Set<AlbumEdition>,
        candidateEditions: Set<AlbumEdition>
    ): Pair<Int, Boolean> = when {
        referenceEditions == candidateEditions -> EXACT_RELEASE_SCORE to true
        referenceEditions.isEmpty() xor candidateEditions.isEmpty() -> EDITION_VARIANT_SCORE to false
        else -> PARTIAL_RELEASE_SCORE to false
    }
}
