package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.RecordingIdentityMatch
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
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

        val refCriticalMarkers = expectedTitleIdentity.markers.filter { it in CRITICAL_VERSION_MARKERS }.toSet()
        val candCriticalMarkers = candidateTitleIdentity.markers.filter { it in CRITICAL_VERSION_MARKERS }.toSet()
        if (refCriticalMarkers != candCriticalMarkers) {
            return Evaluation(false, 0, "version_marker_mismatch")
        }

        if (expectedTitleIdentity.versionLabels != candidateTitleIdentity.versionLabels) {
            val hasDistinctVersionLabels = expectedTitleIdentity.versionLabels.isNotEmpty() &&
                candidateTitleIdentity.versionLabels.isNotEmpty()
            if (hasDistinctVersionLabels) {
                return Evaluation(false, 0, "version_label_mismatch")
            }
        }

        when (recordingIdentityMatch(reference.isrc, candidate.isrc)) {
            RecordingIdentityMatch.Exact -> {
                val artistComp = areArtistsCompatible(refArtist, candArtist)
                if (artistComp) {
                    return Evaluation(true, 100)
                }
            }
            RecordingIdentityMatch.Conflict -> {
                return Evaluation(false, 0, "isrc_conflict")
            }
            RecordingIdentityMatch.Unknown -> Unit
        }

        val expectedExplicit = reference.explicit || expectedTitleIdentity.explicitHint == true
        val candidateExplicit = candidate.explicit || candidateTitleIdentity.explicitHint == true
        if (expectedTitleIdentity.explicitHint != null && candidateTitleIdentity.explicitHint != null) {
            if (expectedTitleIdentity.explicitHint != candidateTitleIdentity.explicitHint) {
                return Evaluation(false, 0, "explicit_mismatch")
            }
        }

        if (!areArtistsCompatible(refArtist, candArtist)) {
            return Evaluation(false, 0, "artist_mismatch")
        }

        val refCoreTitle = expectedTitleIdentity.core
        val candCoreTitle = candidateTitleIdentity.core
        if (refCoreTitle.isBlank() || candCoreTitle.isBlank()) {
            return Evaluation(false, 0, "title_core_blank")
        }

        val titleCoverage = tokenCoverage(refCoreTitle, candCoreTitle)
        if (refCoreTitle != candCoreTitle && titleCoverage < 0.60) {
            return Evaluation(false, 0, "title_mismatch")
        }

        var score = 40

        if (refCoreTitle == candCoreTitle) {
            score += 28
        } else if (titleCoverage >= 0.85) {
            score += 20
        } else {
            score += (titleCoverage * 15).toInt()
        }

        val artistCoverage = tokenCoverage(normalize(refArtist), normalize(candArtist))
        if (normalize(refArtist) == normalize(candArtist)) {
            score += 20
        } else {
            score += (artistCoverage * 15).toInt()
        }

        val refAlbum = reference.album.trim()
        val candAlbum = candidate.albumName.trim()
        if (refAlbum.isNotBlank() && candAlbum.isNotBlank() && !isGenericAlbum(refAlbum)) {
            val albumCoverage = tokenCoverage(normalize(refAlbum), normalize(candAlbum))
            if (normalize(refAlbum) == normalize(candAlbum)) {
                score += 15
            } else if (albumCoverage >= 0.70) {
                score += 10
            } else if (albumCoverage < 0.20 && reference.durationMs <= 0L) {
                score -= 15
            }
        }

        if (reference.durationMs > 0L && candidate.durationMs > 0L) {
            val delta = abs(reference.durationMs - candidate.durationMs)
            when {
                delta <= 3_000L -> score += 12
                delta <= 7_000L -> score += 8
                delta <= 15_000L -> score += 2
                delta > MAX_PERMISSIBLE_DURATION_DELTA_MS -> return Evaluation(false, 0, "duration_out_of_range")
                else -> score -= 10
            }
        }

        val confidence = score.coerceIn(0, 100)
        val accepted = confidence >= MIN_ACCEPTED_CONFIDENCE
        return Evaluation(
            accepted = accepted,
            confidence = confidence,
            rejectionReason = if (accepted) null else "insufficient_confidence"
        )
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
        .replace(Regex("feat\\.?|featuring|ft\\.?"), " ")
        .replace(Regex("[^a-z0-9àèéìòóùçñäöüß\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun isGenericAlbum(value: String): Boolean {
        val lower = value.lowercase(Locale.ROOT).trim()
        return lower.isBlank() || lower in GENERIC_ALBUMS || lower.startsWith("youtube")
    }

    private val GENERIC_ALBUMS = setOf(
        "album", "single", "unknown album", "music", "youtube music", "youtube", "ep"
    )
}
