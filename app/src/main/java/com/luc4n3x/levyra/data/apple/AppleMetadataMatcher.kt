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
    private const val DECISIVE_CONFIDENCE = 90
    private const val MAX_PERMISSIBLE_DURATION_DELTA_MS = 30_000L

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
        val rejectionReason: String? = null
    )

    fun evaluate(reference: Track, candidate: AppleTrackMetadata): Evaluation {
        val refTitle = reference.title.trim()
        val candTitle = candidate.name.trim()
        val refArtist = reference.artist.trim()
        val candArtist = candidate.artistName.trim()

        if (refTitle.isBlank() || candTitle.isBlank() || refArtist.isBlank() || candArtist.isBlank()) {
            return Evaluation(false, 0, "blank_identity")
        }

        val expectedTitleIdentity = AlternativeTrackText.title(refTitle)
        val candidateTitleIdentity = AlternativeTrackText.title(candTitle)

        checkVersionMismatch(expectedTitleIdentity, candidateTitleIdentity)?.let { reason ->
            return Evaluation(false, 0, reason)
        }

        when (recordingIdentityMatch(reference.isrc, candidate.isrc)) {
            RecordingIdentityMatch.Exact -> {
                if (areArtistsCompatible(refArtist, candArtist)) {
                    return Evaluation(true, 100)
                }
            }
            RecordingIdentityMatch.Conflict -> return Evaluation(false, 0, "isrc_conflict")
            RecordingIdentityMatch.Unknown -> Unit
        }

        if (isExplicitMismatched(expectedTitleIdentity, candidateTitleIdentity)) {
            return Evaluation(false, 0, "explicit_mismatch")
        }

        if (!areArtistsCompatible(refArtist, candArtist)) {
            return Evaluation(false, 0, "artist_mismatch")
        }

        val (titleScore, titleReason) = scoreTitle(expectedTitleIdentity.core, candidateTitleIdentity.core)
        if (titleReason != null) {
            return Evaluation(false, 0, titleReason)
        }

        val (durationDeltaScore, durationReason) = evaluateDuration(reference.durationMs, candidate.durationMs)
        if (durationReason != null) {
            return Evaluation(false, 0, durationReason)
        }

        val artistScore = scoreArtist(refArtist, candArtist)
        val albumScore = scoreAlbum(reference.album, candidate.albumName, reference.durationMs)

        val totalScore = (40 + titleScore + artistScore + albumScore + durationDeltaScore).coerceIn(0, 100)
        val accepted = totalScore >= MIN_ACCEPTED_CONFIDENCE
        return Evaluation(
            accepted = accepted,
            confidence = totalScore,
            rejectionReason = if (accepted) null else "insufficient_confidence"
        )
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
        expected: TitleIdentity,
        candidate: TitleIdentity
    ): Boolean {
        val exp = expected.explicitHint
        val cand = candidate.explicitHint
        return exp != null && cand != null && exp != cand
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
        val normRef = normalize(refArtist)
        val normCand = normalize(candArtist)
        if (normRef == normCand) return 20
        val coverage = tokenCoverage(normRef, normCand)
        return (coverage * 15).toInt()
    }

    private fun scoreAlbum(refAlbum: String, candAlbum: String, refDurationMs: Long): Int {
        val cleanRef = refAlbum.trim()
        val cleanCand = candAlbum.trim()
        if (cleanRef.isBlank() || cleanCand.isBlank() || isGenericAlbum(cleanRef)) return 0
        val normRef = normalize(cleanRef)
        val normCand = normalize(cleanCand)
        if (normRef == normCand) return 15
        val coverage = tokenCoverage(normRef, normCand)
        return when {
            coverage >= 0.70 -> 10
            coverage < 0.20 && refDurationMs <= 0L -> -15
            else -> 0
        }
    }

    private fun evaluateDuration(refDurationMs: Long, candDurationMs: Long): Pair<Int, String?> {
        if (refDurationMs <= 0L || candDurationMs <= 0L) return 0 to null
        val delta = abs(refDurationMs - candDurationMs)
        return when {
            delta <= 3_000L -> 12 to null
            delta <= 7_000L -> 8 to null
            delta <= 15_000L -> 2 to null
            delta > MAX_PERMISSIBLE_DURATION_DELTA_MS -> 0 to "duration_out_of_range"
            else -> -10 to null
        }
    }

    private fun areArtistsCompatible(refArtist: String, candArtist: String): Boolean {
        val normRef = normalize(refArtist)
        val normCand = normalize(candArtist)
        if (normRef == normCand) return true
        if (normRef.contains(normCand) || normCand.contains(normRef)) return true

        val refCredit = AlternativeTrackText.artistCredit(refArtist)
        val candCredit = AlternativeTrackText.artistCredit(candArtist)

        if (refCredit.primary.isNotBlank() && candCredit.primary.isNotBlank()) {
            if (refCredit.primary == candCredit.primary) return true
            if (refCredit.primary in candCredit.names || candCredit.primary in refCredit.names) return true
        }

        val coverage = tokenCoverage(normRef, normCand)
        return coverage >= 0.50
    }

    private fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.length >= 2 }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.length >= 2 }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(NORMALIZE_FEAT_REGEX, " ")
        .replace(NORMALIZE_SPECIAL_CHARS_REGEX, " ")
        .replace(NORMALIZE_WHITESPACE_REGEX, " ")
        .trim()

    private fun isGenericAlbum(value: String): Boolean {
        val lower = value.lowercase(Locale.ROOT).trim()
        return lower.isBlank() || lower in GENERIC_ALBUMS || lower.startsWith("youtube")
    }

    private val NORMALIZE_FEAT_REGEX = Regex("feat\\.?|featuring|ft\\.?")
    private val NORMALIZE_SPECIAL_CHARS_REGEX = Regex("[^a-z0-9àèéìòóùçñäöüß\\s]")
    private val NORMALIZE_WHITESPACE_REGEX = Regex("\\s+")

    private val GENERIC_ALBUMS = setOf(
        "album", "single", "unknown album", "music", "youtube music", "youtube", "ep"
    )
}
