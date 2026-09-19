package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.RecordingIdentityMatch
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import com.luc4n3x.levyra.data.hqaudio.TitleIdentity
import com.luc4n3x.levyra.data.hqaudio.TrackVersionMarker
import com.luc4n3x.levyra.data.recordingIdentityMatch
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.abs

object AppleMetadataMatcher {
    private const val MIN_ACCEPTED_CONFIDENCE = 72
    private const val EXACT_ISRC_BASE_CONFIDENCE = 60

    private val CRITICAL_VERSION_MARKERS = setOf(
        TrackVersionMarker.LIVE,
        TrackVersionMarker.REMIX,
        TrackVersionMarker.ACOUSTIC,
        TrackVersionMarker.INSTRUMENTAL,
        TrackVersionMarker.KARAOKE,
        TrackVersionMarker.COVER,
        TrackVersionMarker.SPED_UP,
        TrackVersionMarker.SLOWED,
        TrackVersionMarker.REVERB,
        TrackVersionMarker.DEMO
    )

    data class Evaluation(
        val accepted: Boolean,
        val confidence: Int,
        val isRecordingMatch: Boolean = false,
        val isReleaseMatch: Boolean = false,
        val releaseConfidence: Int = 0,
        val rejectionReason: String? = null
    )

    fun evaluate(reference: Track, candidate: AppleTrackMetadata): Evaluation {
        val refTitle = reference.title.trim()
        val candTitle = candidate.name.trim()
        val refArtist = reference.artist.trim()
        val candArtist = candidate.artistName.trim()

        val expectedTitleIdentity = AlternativeTrackText.title(refTitle)
        val candidateTitleIdentity = AlternativeTrackText.title(candTitle)
        validateIdentity(
            refTitle = refTitle,
            candTitle = candTitle,
            refArtist = refArtist,
            candArtist = candArtist,
            refExplicit = reference.explicit,
            candExplicit = candidate.explicit,
            expected = expectedTitleIdentity,
            candidate = candidateTitleIdentity
        )?.let { reason ->
            return Evaluation(false, 0, rejectionReason = reason)
        }

        val (titleScore, titleReason) = scoreTitle(expectedTitleIdentity.core, candidateTitleIdentity.core)
        if (titleReason != null) {
            return Evaluation(false, 0, rejectionReason = titleReason)
        }

        val isrcMatch = recordingIdentityMatch(reference.isrc, candidate.isrc)
        if (isrcMatch == RecordingIdentityMatch.Conflict) {
            return Evaluation(false, 0, rejectionReason = "isrc_conflict")
        }
        val isExactIsrc = isrcMatch == RecordingIdentityMatch.Exact

        val (durationDeltaScore, durationReason) = evaluateDuration(reference.durationMs, candidate.durationMs, isExactIsrc)
        if (durationReason != null) {
            return Evaluation(false, 0, rejectionReason = durationReason)
        }

        val (releaseScore, isReleaseMatch) = evaluateRelease(reference, candidate)

        if (isExactIsrc) {
            val totalScore = (EXACT_ISRC_BASE_CONFIDENCE + releaseScore + durationDeltaScore).coerceIn(MIN_ACCEPTED_CONFIDENCE, 100)
            return Evaluation(
                accepted = true,
                confidence = totalScore,
                isRecordingMatch = true,
                isReleaseMatch = isReleaseMatch,
                releaseConfidence = releaseScore
            )
        }

        val artistScore = scoreArtist(refArtist, candArtist)
        val totalScore = (35 + titleScore + artistScore + releaseScore + durationDeltaScore).coerceIn(0, 100)
        val accepted = totalScore >= MIN_ACCEPTED_CONFIDENCE
        return Evaluation(
            accepted = accepted,
            confidence = totalScore,
            isRecordingMatch = accepted,
            isReleaseMatch = isReleaseMatch,
            releaseConfidence = releaseScore,
            rejectionReason = if (accepted) null else "insufficient_confidence"
        )
    }

    private fun validateIdentity(
        refTitle: String,
        candTitle: String,
        refArtist: String,
        candArtist: String,
        refExplicit: Boolean,
        candExplicit: Boolean,
        expected: TitleIdentity,
        candidate: TitleIdentity
    ): String? {
        if (hasBlankIdentity(refTitle, candTitle, refArtist, candArtist)) return "blank_identity"
        checkVersionMismatch(expected, candidate)?.let { return it }
        if (isExplicitMismatched(refExplicit, expected, candExplicit, candidate)) return "explicit_mismatch"
        if (!areArtistsCompatible(refArtist, candArtist)) return "artist_mismatch"
        return null
    }

    private fun hasBlankIdentity(
        refTitle: String,
        candTitle: String,
        refArtist: String,
        candArtist: String
    ): Boolean = listOf(refTitle, candTitle, refArtist, candArtist).any(String::isBlank)

    private fun checkVersionMismatch(
        expected: TitleIdentity,
        candidate: TitleIdentity
    ): String? {
        val refCritical = expected.markers.filter { it in CRITICAL_VERSION_MARKERS }.toSet()
        val candCritical = candidate.markers.filter { it in CRITICAL_VERSION_MARKERS }.toSet()
        if (refCritical != candCritical) return "version_marker_mismatch"

        if (expected.versionLabels != candidate.versionLabels) {
            val hasDistinctVersionLabels = expected.versionLabels.isNotEmpty() &&
                candidate.versionLabels.isNotEmpty()
            if (hasDistinctVersionLabels) return "version_label_mismatch"
        }
        return null
    }

    private fun isExplicitMismatched(
        refExplicit: Boolean,
        expected: TitleIdentity,
        candExplicit: Boolean,
        candidate: TitleIdentity
    ): Boolean {
        val refIsExplicit = refExplicit || expected.explicitHint == true
        val refIsClean = expected.explicitHint == false
        val candIsExplicit = candExplicit || candidate.explicitHint == true
        val candIsClean = candidate.explicitHint == false

        if (refIsExplicit && candIsClean) return true
        if (refIsClean && candIsExplicit) return true
        if (expected.explicitHint != null && candidate.explicitHint != null) {
            return expected.explicitHint != candidate.explicitHint
        }
        return false
    }

    private fun scoreTitle(refCore: String, candCore: String): Pair<Int, String?> {
        if (refCore.isBlank() || candCore.isBlank()) return 0 to "title_core_blank"
        val coverage = tokenCoverage(refCore, candCore)
        if (refCore != candCore && coverage < 0.60) return 0 to "title_mismatch"
        val score = when {
            refCore == candCore -> 28
            coverage >= 0.85 -> 20
            else -> (coverage * 15).toInt()
        }
        return score to null
    }

    private fun scoreArtist(refArtist: String, candArtist: String): Int {
        val normRef = AlternativeTrackText.normalizeArtist(refArtist)
        val normCand = AlternativeTrackText.normalizeArtist(candArtist)
        if (normRef == normCand) return 20
        val coverage = tokenCoverage(normRef, normCand)
        return (coverage * 15).toInt()
    }

    private fun evaluateRelease(
        reference: Track,
        candidate: AppleTrackMetadata
    ): Pair<Int, Boolean> {
        val refAlbum = reference.album.trim()
        val candAlbum = candidate.albumName.trim()
        if (!hasUsableReleaseIdentity(refAlbum, candAlbum)) return 0 to false
        if (isCompilationMismatch(refAlbum, candAlbum)) return -30 to false

        val refAlbumId = AlternativeTrackText.album(refAlbum)
        val candAlbumId = AlternativeTrackText.album(candAlbum)
        val coresMatch = refAlbumId.core.isNotBlank() && refAlbumId.core == candAlbumId.core
        val (baseScore, releaseMatch) = scoreAlbumReleaseIdentity(
            refCore = refAlbumId.core,
            candCore = candAlbumId.core,
            refEditions = refAlbumId.editions,
            candEditions = candAlbumId.editions,
            coresMatch = coresMatch
        )
        val trackPositionAdjustment = scoreReleaseTrackPosition(
            coresMatch = coresMatch,
            releaseMatch = releaseMatch,
            referenceTrackNumber = reference.trackNumber,
            candidateTrackNumber = candidate.trackNumber
        )
        return (baseScore + trackPositionAdjustment) to releaseMatch
    }

    private fun hasUsableReleaseIdentity(refAlbum: String, candAlbum: String): Boolean =
        refAlbum.isNotBlank() && candAlbum.isNotBlank() && !isGenericAlbum(refAlbum)

    private fun isCompilationMismatch(refAlbum: String, candAlbum: String): Boolean =
        !isCompilationAlbum(refAlbum) && isCompilationAlbum(candAlbum)

    private fun scoreAlbumReleaseIdentity(
        refCore: String,
        candCore: String,
        refEditions: Set<com.luc4n3x.levyra.data.hqaudio.AlbumEdition>,
        candEditions: Set<com.luc4n3x.levyra.data.hqaudio.AlbumEdition>,
        coresMatch: Boolean
    ): Pair<Int, Boolean> {
        if (coresMatch) return scoreMatchingAlbumCores(refEditions, candEditions)
        if (refCore.isBlank() || candCore.isBlank()) return -20 to false

        val forwardCoverage = tokenCoverage(refCore, candCore)
        val reverseCoverage = tokenCoverage(candCore, refCore)
        val mutualCoverage = minOf(forwardCoverage, reverseCoverage)
        val sameEditions = refEditions == candEditions

        return when {
            mutualCoverage >= 0.90 && sameEditions -> 18 to true
            mutualCoverage >= 0.75 -> 12 to false
            mutualCoverage >= 0.50 -> 5 to false
            else -> -20 to false
        }
    }

    private fun scoreMatchingAlbumCores(
        refEditions: Set<com.luc4n3x.levyra.data.hqaudio.AlbumEdition>,
        candEditions: Set<com.luc4n3x.levyra.data.hqaudio.AlbumEdition>
    ): Pair<Int, Boolean> {
        if (refEditions == candEditions) return 30 to true
        if (refEditions.isEmpty() xor candEditions.isEmpty()) return 10 to false
        return 5 to false
    }

    private fun scoreReleaseTrackPosition(
        coresMatch: Boolean,
        releaseMatch: Boolean,
        referenceTrackNumber: Int,
        candidateTrackNumber: Int
    ): Int {
        if (!coresMatch || referenceTrackNumber <= 0 || candidateTrackNumber <= 0) return 0
        if (referenceTrackNumber == candidateTrackNumber) return 5
        return if (releaseMatch) 0 else -5
    }

    private fun evaluateDuration(
        refDurationMs: Long,
        candDurationMs: Long,
        isExactIsrc: Boolean
    ): Pair<Int, String?> {
        if (refDurationMs <= 0L || candDurationMs <= 0L) return 0 to null
        val delta = abs(refDurationMs - candDurationMs)
        return when {
            delta <= 3_000L -> 12 to null
            delta <= 7_000L -> 6 to null
            delta <= 12_000L -> -12 to null
            delta <= 15_000L -> {
                if (isExactIsrc) -18 to null else 0 to "duration_out_of_range"
            }
            else -> 0 to "duration_out_of_range"
        }
    }

    private fun areArtistsCompatible(refArtist: String, candArtist: String): Boolean {
        val normRef = AlternativeTrackText.normalizeArtist(refArtist)
        val normCand = AlternativeTrackText.normalizeArtist(candArtist)
        if (normRef.isBlank() || normCand.isBlank()) return false
        if (normRef == normCand) return true
        if (hasStructuredArtistOverlap(refArtist, candArtist)) return true
        if (hasArtistNameOverlap(refArtist, candArtist)) return true
        return hasMutualArtistCoverage(normRef, normCand)
    }

    private fun hasStructuredArtistOverlap(refArtist: String, candArtist: String): Boolean {
        val refCredit = AlternativeTrackText.artistCredit(refArtist)
        val candCredit = AlternativeTrackText.artistCredit(candArtist)
        if (refCredit.primary.isBlank() || candCredit.primary.isBlank()) return false
        if (refCredit.primary == candCredit.primary) return true
        return refCredit.primary in candCredit.names || candCredit.primary in refCredit.names
    }

    private fun hasArtistNameOverlap(refArtist: String, candArtist: String): Boolean {
        val refNames = AlternativeTrackText.artistNames(refArtist)
        val candNames = AlternativeTrackText.artistNames(candArtist)
        return refNames.any { it in candNames } || candNames.any { it in refNames }
    }

    private fun hasMutualArtistCoverage(normRef: String, normCand: String): Boolean {
        val referenceCoverage = tokenCoverage(normRef, normCand)
        val candidateCoverage = tokenCoverage(normCand, normRef)
        return minOf(referenceCoverage, candidateCoverage) >= 0.50
    }

    private fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.isNotBlank() }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.isNotBlank() }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }

    private fun isCompilationAlbum(album: String): Boolean {
        val lower = album.lowercase(Locale.ROOT)
        return COMPILATION_KEYWORDS.any { lower.contains(it) }
    }

    private fun isGenericAlbum(value: String): Boolean {
        val lower = value.lowercase(Locale.ROOT).trim()
        return lower.isBlank() || lower in GENERIC_ALBUMS || lower.startsWith("youtube")
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
