package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.abs

private const val CANONICAL_FALLBACK_DURATION_TOLERANCE_MS = 12_000L
private const val CANONICAL_AMBIGUITY_DELTA_MS = 1_500L

private val KNOWN_MISATTRIBUTED_PLAYBACK_ARTISTS = setOf(
    "neptune"
)

internal fun isKnownMisattributedPlaybackArtist(value: String): Boolean =
    value.playbackMatchKey() in KNOWN_MISATTRIBUTED_PLAYBACK_ARTISTS

internal fun bestCanonicalPlaybackMetadataMatch(
    original: Track,
    candidates: List<Track>
): Track? {
    val originalTitleKey = original.title.playbackMatchKey()
    if (originalTitleKey.isBlank()) return null

    val exactTitleCandidates = candidates
        .asSequence()
        .filter { candidate ->
            candidate.artist.isNotBlank() &&
                !isKnownMisattributedPlaybackArtist(candidate.artist) &&
                candidate.title.playbackMatchKey() == originalTitleKey
        }
        .distinctBy { candidate -> "${candidate.id}|${candidate.artist.playbackMatchKey()}" }
        .toList()
    if (exactTitleCandidates.isEmpty()) return null

    if (original.durationMs <= 0L) {
        val artistKeys = exactTitleCandidates
            .map { candidate -> candidate.artist.playbackMatchKey() }
            .filter(String::isNotBlank)
            .distinct()
        if (artistKeys.size != 1) return null
        return exactTitleCandidates.maxWithOrNull(canonicalMetadataComparator(original))
    }

    val durationMatches = exactTitleCandidates
        .filter { candidate ->
            candidate.durationMs > 0L &&
                abs(original.durationMs - candidate.durationMs) <= CANONICAL_FALLBACK_DURATION_TOLERANCE_MS
        }
        .sortedWith(canonicalMetadataComparator(original).reversed())
    val best = durationMatches.firstOrNull() ?: return null
    val runnerUp = durationMatches.drop(1).firstOrNull { candidate ->
        candidate.artist.playbackMatchKey() != best.artist.playbackMatchKey()
    }
    if (runnerUp != null) {
        val bestDelta = abs(original.durationMs - best.durationMs)
        val runnerUpDelta = abs(original.durationMs - runnerUp.durationMs)
        if (runnerUpDelta - bestDelta <= CANONICAL_AMBIGUITY_DELTA_MS) return null
    }
    return best
}

private fun canonicalMetadataComparator(original: Track): Comparator<Track> =
    compareBy<Track> { candidate ->
        if (original.durationMs > 0L && candidate.durationMs > 0L) {
            -abs(original.durationMs - candidate.durationMs)
        } else {
            Long.MIN_VALUE
        }
    }
        .thenBy { candidate -> candidate.isrc.isNotBlank() }
        .thenBy { candidate -> candidate.album.isNotBlank() }
        .thenBy { candidate -> candidate.metadataConfidence }

internal fun playbackAlternativeSearchQueries(track: Track): List<String> {
    val title = track.title.playbackSearchToken()
    val artist = track.artist.playbackSearchToken()
    val base = listOf(artist, title)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { title.ifBlank { track.id.trim() } }
    val misattributedArtist = isKnownMisattributedPlaybackArtist(artist)

    return buildList {
        if (misattributedArtist && title.isNotBlank()) {
            add("$title official audio")
            add("$title official video")
            add("$title topic")
            add(title)
        }
        add("$base official audio")
        if (!misattributedArtist && artist.isNotBlank() && title.isNotBlank()) add("$title official audio")
        add("$base official video")
        if (!misattributedArtist && artist.isNotBlank() && title.isNotBlank()) add("$title official video")
        add("$base topic")
        add(base)
        if (!misattributedArtist && title.isNotBlank() && title != base) add(title)
    }
        .map(String::trim)
        .filter { it.length >= 2 }
        .distinct()
}

internal fun canonicalPlaybackFallbackArtist(
    original: Track,
    candidate: Track,
    resolved: Track
): String {
    val donorArtist = resolved.artist.trim().ifBlank { candidate.artist.trim() }
    if (
        donorArtist.isBlank() ||
        isKnownMisattributedPlaybackArtist(donorArtist) ||
        donorArtist.equals(original.artist.trim(), ignoreCase = true)
    ) {
        return original.artist
    }

    val donorTitle = resolved.title.trim().ifBlank { candidate.title.trim() }
    val donorDurationMs = resolved.durationMs.takeIf { it > 0L } ?: candidate.durationMs
    return if (
        isCanonicalPlaybackRecordingMatch(
            originalTitle = original.title,
            originalDurationMs = original.durationMs,
            candidateTitle = donorTitle,
            candidateDurationMs = donorDurationMs
        )
    ) {
        donorArtist
    } else {
        original.artist
    }
}

internal fun shouldAdoptYoutubeCanonicalArtist(
    track: Track,
    metadataTitle: String,
    metadataArtist: String,
    metadataDurationMs: Long?
): Boolean {
    val donorArtist = metadataArtist.trim()
    if (
        donorArtist.isBlank() ||
        isKnownMisattributedPlaybackArtist(donorArtist) ||
        donorArtist.equals(track.artist.trim(), ignoreCase = true)
    ) return false
    val durationMs = metadataDurationMs?.takeIf { it > 0L } ?: return false
    return isCanonicalPlaybackRecordingMatch(
        originalTitle = track.title,
        originalDurationMs = track.durationMs,
        candidateTitle = metadataTitle,
        candidateDurationMs = durationMs
    )
}

private fun isCanonicalPlaybackRecordingMatch(
    originalTitle: String,
    originalDurationMs: Long,
    candidateTitle: String,
    candidateDurationMs: Long
): Boolean {
    val originalKey = originalTitle.playbackMatchKey()
    val candidateKey = candidateTitle.playbackMatchKey()
    if (originalKey.isBlank() || originalKey != candidateKey) return false
    if (originalDurationMs <= 0L || candidateDurationMs <= 0L) return false
    return abs(originalDurationMs - candidateDurationMs) <= CANONICAL_FALLBACK_DURATION_TOLERANCE_MS
}

private fun String.playbackSearchToken(): String = trim()
    .filterNot { it.code in 0..31 }
    .splitToSequence(' ', '\t', '\n', '\r')
    .filter(String::isNotBlank)
    .joinToString(" ")

private fun String.playbackMatchKey(): String = playbackSearchToken()
    .lowercase(Locale.ROOT)
    .filter { it.isLetterOrDigit() || it.isWhitespace() }
    .splitToSequence(' ')
    .filter(String::isNotBlank)
    .joinToString(" ")
