package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal object AppleReleaseMatcher {
    private const val COMPILATION_MISMATCH_SCORE = -30

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
        val (baseScore, releaseMatch) = AppleReleaseScoring.scoreIdentity(
            referenceCore = referenceIdentity.core,
            candidateCore = candidateIdentity.core,
            referenceEditions = referenceIdentity.editions,
            candidateEditions = candidateIdentity.editions,
            coresMatch = coresMatch
        )
        val trackPositionScore = AppleReleaseScoring.scoreTrackPosition(
            coresMatch = coresMatch,
            releaseMatch = releaseMatch,
            referenceTrackNumber = reference.trackNumber,
            candidateTrackNumber = candidate.trackNumber
        )
        return baseScore + trackPositionScore to releaseMatch
    }

    private fun hasUsableIdentity(referenceAlbum: String, candidateAlbum: String): Boolean {
        val albumsPresent = referenceAlbum.isNotBlank() && candidateAlbum.isNotBlank()
        return albumsPresent && !isGenericAlbum(referenceAlbum)
    }

    private fun isCompilationMismatch(referenceAlbum: String, candidateAlbum: String): Boolean =
        !isCompilationAlbum(referenceAlbum) && isCompilationAlbum(candidateAlbum)

    private fun isCompilationAlbum(album: String): Boolean {
        val normalized = album.lowercase(Locale.ROOT)
        return COMPILATION_KEYWORDS.any(normalized::contains)
    }

    private fun isGenericAlbum(album: String): Boolean {
        val normalized = album.lowercase(Locale.ROOT).trim()
        val knownGeneric = normalized.isBlank() || normalized in GENERIC_ALBUMS
        return knownGeneric || normalized.startsWith("youtube")
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
