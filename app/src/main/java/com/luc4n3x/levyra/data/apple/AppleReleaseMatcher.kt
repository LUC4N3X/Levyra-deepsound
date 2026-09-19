package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.hqaudio.AlbumEdition
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal object AppleReleaseMatcher {
    fun evaluate(reference: Track, candidate: AppleTrackMetadata): Pair<Int, Boolean> {
        val referenceAlbum = reference.album.trim()
        val candidateAlbum = candidate.albumName.trim()
        if (!hasUsableIdentity(referenceAlbum, candidateAlbum)) return 0 to false
        if (isCompilationMismatch(referenceAlbum, candidateAlbum)) return -30 to false

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
        return (baseScore + scoreTrackPosition(
            coresMatch = coresMatch,
            releaseMatch = releaseMatch,
            referenceTrackNumber = reference.trackNumber,
            candidateTrackNumber = candidate.trackNumber
        )) to releaseMatch
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
    ): Pair<Int, Boolean> {
        if (coresMatch) return scoreMatchingCores(referenceEditions, candidateEditions)
        if (referenceCore.isBlank() || candidateCore.isBlank()) return -20 to false

        val mutualCoverage = minOf(
            AppleMatchScoring.tokenCoverage(referenceCore, candidateCore),
            AppleMatchScoring.tokenCoverage(candidateCore, referenceCore)
        )
        val sameEditions = referenceEditions == candidateEditions
        return when {
            mutualCoverage >= 0.90 && sameEditions -> 18 to true
            mutualCoverage >= 0.75 -> 12 to false
            mutualCoverage >= 0.50 -> 5 to false
            else -> -20 to false
        }
    }

    private fun scoreMatchingCores(
        referenceEditions: Set<AlbumEdition>,
        candidateEditions: Set<AlbumEdition>
    ): Pair<Int, Boolean> {
        if (referenceEditions == candidateEditions) return 30 to true
        if (referenceEditions.isEmpty() xor candidateEditions.isEmpty()) return 10 to false
        return 5 to false
    }

    private fun scoreTrackPosition(
        coresMatch: Boolean,
        releaseMatch: Boolean,
        referenceTrackNumber: Int,
        candidateTrackNumber: Int
    ): Int {
        if (!coresMatch || referenceTrackNumber <= 0 || candidateTrackNumber <= 0) return 0
        if (referenceTrackNumber == candidateTrackNumber) return 5
        return if (releaseMatch) 0 else -5
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
