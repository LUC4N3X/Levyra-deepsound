package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.RecordingIdentityMatch
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import com.luc4n3x.levyra.data.hqaudio.TitleIdentity
import com.luc4n3x.levyra.data.hqaudio.TrackVersionMarker
import com.luc4n3x.levyra.data.recordingIdentityMatch
import com.luc4n3x.levyra.domain.Track
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
        val expectedTitleIdentity = AlternativeTrackText.title(reference.title.trim())
        val candidateTitleIdentity = AlternativeTrackText.title(candidate.name.trim())
        validateIdentity(
            reference = reference,
            candidateMetadata = candidate,
            expected = expectedTitleIdentity,
            candidate = candidateTitleIdentity
        )?.let { reason ->
            return rejected(reason)
        }
        return evaluateValidatedCandidate(reference, candidate, expectedTitleIdentity, candidateTitleIdentity)
    }

    private fun evaluateValidatedCandidate(
        reference: Track,
        candidate: AppleTrackMetadata,
        expectedTitleIdentity: TitleIdentity,
        candidateTitleIdentity: TitleIdentity
    ): Evaluation {
        val (titleScore, titleReason) = AppleTitleMatcher.score(expectedTitleIdentity.core, candidateTitleIdentity.core)
        if (titleReason != null) return rejected(titleReason)

        val isrcMatch = recordingIdentityMatch(reference.isrc, candidate.isrc)
        if (isrcMatch == RecordingIdentityMatch.Conflict) return rejected("isrc_conflict")
        val isExactIsrc = isrcMatch == RecordingIdentityMatch.Exact

        val (durationScore, durationReason) = evaluateDuration(reference.durationMs, candidate.durationMs, isExactIsrc)
        if (durationReason != null) return rejected(durationReason)

        val (releaseScore, isReleaseMatch) = AppleReleaseMatcher.evaluate(reference, candidate)
        if (isExactIsrc) return exactIsrcEvaluation(releaseScore, durationScore, isReleaseMatch)

        val artistScore = AppleArtistMatcher.score(reference.artist, candidate.artistName)
        val totalScore = (35 + titleScore + artistScore + releaseScore + durationScore).coerceIn(0, 100)
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

    private fun exactIsrcEvaluation(
        releaseScore: Int,
        durationScore: Int,
        isReleaseMatch: Boolean
    ): Evaluation {
        val totalScore = (EXACT_ISRC_BASE_CONFIDENCE + releaseScore + durationScore)
            .coerceIn(MIN_ACCEPTED_CONFIDENCE, 100)
        return Evaluation(
            accepted = true,
            confidence = totalScore,
            isRecordingMatch = true,
            isReleaseMatch = isReleaseMatch,
            releaseConfidence = releaseScore
        )
    }

    private fun rejected(reason: String): Evaluation =
        Evaluation(false, 0, rejectionReason = reason)

    private fun validateIdentity(
        reference: Track,
        candidateMetadata: AppleTrackMetadata,
        expected: TitleIdentity,
        candidate: TitleIdentity
    ): String? {
        val identityParts = listOf(
            reference.title,
            candidateMetadata.name,
            reference.artist,
            candidateMetadata.artistName
        )
        if (identityParts.any(String::isBlank)) return "blank_identity"
        checkVersionMismatch(expected, candidate)?.let { return it }
        if (isExplicitMismatched(reference.explicit, expected, candidateMetadata.explicit, candidate)) {
            return "explicit_mismatch"
        }
        if (!AppleArtistMatcher.areCompatible(reference.artist, candidateMetadata.artistName)) return "artist_mismatch"
        return null
    }

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


}
