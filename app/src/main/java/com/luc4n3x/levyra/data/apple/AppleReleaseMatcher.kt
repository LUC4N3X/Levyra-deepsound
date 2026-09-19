package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.hqaudio.AlbumEdition
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal object AppleReleaseMatcher {
    private const val COMPILATION_MISMATCH_SCORE = -30
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

    fun evaluate(reference: Track, candidate: AppleTrackMetadata): Pair<Int, Boolean> {
        val referenceAlbum = reference.album.trim()
        val candidateAlbum = candidate.albumName.trim()
        return when {
            !hasUsableIdentity(referenceAlbum, candidateAlbum) -> 0 to false
            isCompilationMismatch(referenceAlbum, candidateAlbum) -> COMPILATION_MISMATCH_SCORE to false
            else -> evaluateComparableRelease(reference, candidate, referenceAlbum, candidateAlbum)
        }
    }

    private fun evaluateComparableRelease(
        reference: Track,
        candidate: AppleTrackMetadata,
        referenceAlbum: String,
        candidateAlbum: String
    ): Pair<Int, Boolean> {
        val referenceIdentity = AlternativeTrackText.album(referenceAlbum)
        val candidateIdentity = AlternativeTrackText.album(candidateAlbum)
        val coresMatch = referenceIdentity.core.isNotBlank() &&
            referenceIdentity.core == candidateIdentity.core
        val (baseScore, releaseMatch) = scoreIdentity(
            referenceCore = referenceIdentity.core,
            candidateCore = candidateIdentity.core,
            referenceEditions = referenceIdentity.editions,
            candidateEditions = candidateIdentity.editions,
            coresMatch = coresMatch
        )
        val trackPositionScore = scoreTrackPosition(
            coresMatch = coresMatch,
            releaseMatch = releaseMatch,
            referenceTrackNumber = reference.trackNumber,
            candidateTrackNumber = candidate.trackNumber
        )
        return (baseScore + trackPositionScore) to releaseMatch
    }

    private fun hasUsableIdentity(referenceAlbum: String, candidateAlbum: String): Boolean =
        referenceAlbum.isNotBlank() && candidateAlbum.isNotBlank() && !isGenericAlbum(referenceAlbum)

    private fun isCompilationMismatch(referenceAlbum: String, candidateAlbum: String): Boolean =
        !isCompilationAlbum(referenceAlbum) && isCompilationAlbum(candidateAlbum)

    private fun scoreIdentity(
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

    private fun scoreTrackPosition(
        coresMatch: Boolean,
        releaseMatch: Boolean,
        referenceTrackNumber: Int,
        candidateTrackNumber: Int
    ): Int = when {
        !coresMatch || referenceTrackNumber <= 0 || candidateTrackNumber <= 0 -> 0
        referenceTrackNumber == candidateTrackNumber -> MATCHING_TRACK_BONUS
        releaseMatch -> 0
        else -> MISMATCHED_TRACK_PENALTY
    }

    private fun isCompilationAlbum(album: String): Boolean {
        val normalized = album.lowercase(Locale.ROOT)
        return COMPILATION_KEYWORDS.any(normalized::contains)
    }

    private fun isGenericAlbum(album: String): Boolean {
        val normalized = album.lowercase(Locale.ROOT).trim()
        return normalized.isBlank() || normalized in GENERIC_ALBUMS || normalized.startsWith("youtube")
    }

    private val COMPILATION_KEYWORDS = setOf(
        "greatest hits", "best of", "the best of", "compilation", "anthology",
        "the essential", "essentials", "collection", "ultimate collection",
        "singles collection", "hit collection", "top hits", "soundtrack"
    )

    private val GENERIC_ALBUMS = setOf(
        "album", "single", "unknown album", "music", "youtube music", "youtube", "ep"
    )
}
