package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.abs

private const val CANONICAL_FALLBACK_DURATION_TOLERANCE_MS = 12_000L
private const val CANONICAL_AMBIGUITY_DELTA_MS = 1_500L

private data class MisattributedPlaybackSignature(
    val artist: String,
    val title: String
)

private val KNOWN_MISATTRIBUTED_PLAYBACK_SIGNATURES = setOf(
    MisattributedPlaybackSignature(artist = "neptune", title = "sottogonna")
)

private val PLAYBACK_FEATURE_MARKER = Regex(
    """\s*[\[(]?\s*(?:feat\.?|ft\.?|featuring)\b.*$""",
    RegexOption.IGNORE_CASE
)
private val PLAYBACK_ARTIST_SEPARATOR = Regex(
    """\s*(?:,|&|/|;|\bx\b|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b)\s*""",
    RegexOption.IGNORE_CASE
)
private val PLAYBACK_VARIANT_MARKER = Regex(
    """\b(?:karaoke|cover|reaction|nightcore|sped\s+up|slowed|reverb|live|remix)\b""",
    RegexOption.IGNORE_CASE
)
private val PLAYBACK_WHITESPACE = Regex("""\s+""")

internal fun isKnownMisattributedPlaybackMetadata(artist: String, title: String): Boolean =
    MisattributedPlaybackSignature(
        artist = artist.playbackMatchKey(),
        title = title.playbackMatchKey()
    ) in KNOWN_MISATTRIBUTED_PLAYBACK_SIGNATURES

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
                !isKnownMisattributedPlaybackMetadata(candidate.artist, candidate.title) &&
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

internal fun trustedPlaybackFallbackCandidates(
    original: Track,
    candidates: List<Track>
): List<Track> {
    if (candidates.isEmpty()) return emptyList()
    if (isKnownMisattributedPlaybackMetadata(original.artist, original.title)) {
        return listOfNotNull(bestCanonicalPlaybackMetadataMatch(original, candidates))
    }

    val originalIsrc = original.isrc.trim()
    val originalTitle = original.title.playbackRecordingTitleKey()
    val originalArtist = original.artist.playbackPrimaryArtistKey()
    if (originalTitle.isBlank() || originalArtist.isBlank()) return emptyList()

    val eligible = candidates
        .asSequence()
        .filter { candidate -> candidate.id.isNotBlank() && candidate.title.isNotBlank() && candidate.artist.isNotBlank() }
        .distinctBy { candidate -> candidate.id }
        .toList()

    if (originalIsrc.isNotBlank()) {
        return eligible.filter { candidate ->
            candidate.isrc.trim().equals(originalIsrc, ignoreCase = true)
        }
    }

    return eligible.filter { candidate ->
        isTrustedPlaybackRecordingMatch(original, candidate, originalTitle, originalArtist)
    }
}

internal fun isResolvedPlaybackDurationCompatible(original: Track, resolvedDurationMs: Long): Boolean {
    val expectedDurationMs = original.durationMs
    if (expectedDurationMs <= 0L) return true
    if (resolvedDurationMs <= 0L) return false
    return abs(expectedDurationMs - resolvedDurationMs) <= CANONICAL_FALLBACK_DURATION_TOLERANCE_MS
}

internal fun isResolvedPlaybackFallbackDurationCompatible(original: Track, resolved: Track): Boolean =
    isResolvedPlaybackDurationCompatible(original, resolved.durationMs)

private fun isTrustedPlaybackRecordingMatch(
    original: Track,
    candidate: Track,
    originalTitle: String,
    originalArtist: String
): Boolean {
    if (candidate.title.playbackRecordingTitleKey() != originalTitle) return false
    if (candidate.artist.playbackPrimaryArtistKey() != originalArtist) return false
    if (addsDifferentPlaybackVariant(original.title, candidate.title)) return false
    if (original.durationMs > 0L) {
        if (candidate.durationMs <= 0L) return false
        if (abs(original.durationMs - candidate.durationMs) > CANONICAL_FALLBACK_DURATION_TOLERANCE_MS) return false
    }
    return true
}

private fun addsDifferentPlaybackVariant(originalTitle: String, candidateTitle: String): Boolean {
    val originalVariants = PLAYBACK_VARIANT_MARKER.findAll(originalTitle.lowercase(Locale.ROOT))
        .map { it.value.replace(PLAYBACK_WHITESPACE, " ") }
        .toSet()
    val candidateVariants = PLAYBACK_VARIANT_MARKER.findAll(candidateTitle.lowercase(Locale.ROOT))
        .map { it.value.replace(PLAYBACK_WHITESPACE, " ") }
        .toSet()
    return candidateVariants.any { it !in originalVariants }
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
    val misattributedArtist = isKnownMisattributedPlaybackMetadata(track.artist, track.title)

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
    val donorTitle = resolved.title.trim().ifBlank { candidate.title.trim() }
    if (
        donorArtist.isBlank() ||
        isKnownMisattributedPlaybackMetadata(donorArtist, donorTitle) ||
        donorArtist.equals(original.artist.trim(), ignoreCase = true)
    ) {
        return original.artist
    }

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
        isKnownMisattributedPlaybackMetadata(donorArtist, metadataTitle) ||
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

private fun String.playbackRecordingTitleKey(): String = PLAYBACK_FEATURE_MARKER
    .replace(this, " ")
    .playbackMatchKey()

private fun String.playbackPrimaryArtistKey(): String = PLAYBACK_ARTIST_SEPARATOR
    .split(this, limit = 2)
    .firstOrNull()
    .orEmpty()
    .playbackMatchKey()

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
