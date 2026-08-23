package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.Track
import java.security.MessageDigest
import java.util.Locale

internal const val MOTION_ARTWORK_POSITIVE_TTL_MS = 24L * 60L * 60L * 1000L
internal const val MOTION_ARTWORK_NEGATIVE_TTL_MS = 10L * 60L * 1000L

enum class MotionArtworkScope {
    TRACK,
    ALBUM
}

data class MotionTrackIdentity(
    val title: String,
    val artists: List<String>,
    val album: String,
    val durationMs: Long,
    val isrc: String,
    val upc: String,
    val year: String,
    val trackId: String,
    val albumId: String
) {
    companion object {
        fun from(track: Track): MotionTrackIdentity = MotionTrackIdentity(
            title = track.title.trim(),
            artists = splitArtists(track.artist),
            album = track.album.trim(),
            durationMs = track.durationMs.coerceAtLeast(0L),
            isrc = track.isrc.trim().uppercase(Locale.ROOT),
            upc = track.upc.trim(),
            year = track.year.trim(),
            trackId = track.id.trim(),
            albumId = track.albumBrowseId.trim()
        )
    }
}

data class MotionArtworkCandidate(
    val provider: String,
    val scope: MotionArtworkScope,
    val identity: MotionTrackIdentity,
    val url: String,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val expiresAtMs: Long
)

data class MotionArtwork(
    val identityKey: String,
    val provider: String,
    val url: String,
    val mimeType: String,
    val width: Int?,
    val height: Int?,
    val confidence: Int,
    val expiresAtMs: Long,
    val lastVerifiedAtMs: Long,
    val configEpoch: Long
)

data class MotionArtworkMatch(
    val accepted: Boolean,
    val score: Int
)

object MotionArtworkIdentityKey {
    fun create(track: Track): String {
        val identity = MotionTrackIdentity.from(track)
        val canonical = when {
            identity.isrc.isNotBlank() -> "isrc:${identity.isrc}"
            identity.trackId.isNotBlank() -> "track:${identity.trackId}"
            identity.albumId.isNotBlank() && track.trackNumber > 0 ->
                "album:${identity.albumId}:${track.discNumber.coerceAtLeast(1)}:${track.trackNumber}"
            else -> listOf(
                normalizeMotionText(identity.title),
                identity.artists.map(::normalizeMotionText).sorted().joinToString(","),
                normalizeMotionText(identity.album),
                (identity.durationMs / 1000L).toString()
            ).joinToString("|")
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

private val MOTION_ARTIST_SEPARATORS = Regex(
    """(?:\s*,\s*|\s*&\s*|\s+×\s+|\s+[xX]\s+|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b|\bcon\b)""",
    RegexOption.IGNORE_CASE
)

internal fun splitArtists(value: String): List<String> {
    return value.split(MOTION_ARTIST_SEPARATORS)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy(::normalizeMotionText)
}

internal fun artistAliases(values: List<String>): Set<String> = buildSet {
    values.forEach { value ->
        normalizeMotionText(value).takeIf { it.isNotBlank() }?.let(::add)
        splitArtists(value).forEach { artist ->
            normalizeMotionText(artist).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

internal fun combinedArtistSignature(values: List<String>): String =
    normalizeMotionText(values.joinToString(" "))

internal fun primaryMotionArtistMatches(requested: List<String>, returned: List<String>): Boolean {
    if (requested.isEmpty() || returned.isEmpty()) return false
    if (combinedArtistSignature(requested) == combinedArtistSignature(returned)) return true
    val requestedPrimary = normalizeMotionText(requested.first())
    return requestedPrimary.isNotBlank() && returned.any { normalizeMotionText(it) == requestedPrimary }
}

// Precompiled: motion-artwork identity normalization runs for every candidate title and artist.
private val MOTION_APOSTROPHES = Regex("""[’'`´]""")
private val MOTION_BRACKETS = Regex("""[()\[\]{}]""")
private val MOTION_NON_ALPHANUMERIC = Regex("""[^\p{L}\p{N}]+""")
private val MOTION_WHITESPACE = Regex("""\s+""")

internal fun normalizeMotionText(value: String): String = value
    .lowercase(Locale.ROOT)
    .replace(MOTION_APOSTROPHES, "")
    .replace(MOTION_BRACKETS, " ")
    .replace(MOTION_NON_ALPHANUMERIC, " ")
    .replace(MOTION_WHITESPACE, " ")
    .trim()

internal fun motionTextContainsTerm(value: String, term: String): Boolean {
    val normalizedValue = normalizeMotionText(value)
    val normalizedTerm = normalizeMotionText(term)
    if (normalizedValue.isBlank() || normalizedTerm.isBlank()) return false
    return " $normalizedValue ".contains(" $normalizedTerm ")
}
