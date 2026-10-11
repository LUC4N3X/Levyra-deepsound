package com.luc4n3x.levyra.data.collections

import com.luc4n3x.levyra.domain.Track
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

const val PERSONAL_COLLECTION_SOURCE = "Collection"
const val PERSONAL_COLLECTION_TRACK_PREFIX = "pc:"

fun isPersonalCollectionTrackId(id: String): Boolean = id.startsWith(PERSONAL_COLLECTION_TRACK_PREFIX)

fun Track.isPersonalCollectionTrack(): Boolean = isPersonalCollectionTrackId(id)

internal fun personalCollectionTrackId(collectionId: String, entryKey: String): String =
    "$PERSONAL_COLLECTION_TRACK_PREFIX$collectionId:$entryKey"

internal fun personalCollectionIdOf(trackId: String): String? {
    if (!isPersonalCollectionTrackId(trackId)) return null
    return trackId.removePrefix(PERSONAL_COLLECTION_TRACK_PREFIX).substringBefore(':').takeIf(String::isNotBlank)
}

enum class PersonalCollectionFormat { M3U, JSON }

enum class PersonalCollectionSourceKind { FILE, URL }

data class ParsedCollectionEntry(
    val entryKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val durationMs: Long,
    val audioUrl: String,
    val videoUrl: String,
    val artworkUrl: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int
)

enum class CollectionIssueKind {
    MissingUrl,
    InvalidUrl,
    UnsupportedScheme,
    RelativeWithoutBase,
    Duplicate,
    CredentialsRemoved,
    Truncated
}

data class CollectionParseIssue(
    val kind: CollectionIssueKind,
    val position: Int
)

data class CollectionCredential(
    val origin: String,
    val username: String,
    val password: String
)

data class ParsedCollection(
    val format: PersonalCollectionFormat,
    val name: String,
    val artworkUrl: String,
    val entries: List<ParsedCollectionEntry>,
    val issues: List<CollectionParseIssue>,
    val credentials: List<CollectionCredential>
) {
    fun issueCount(kind: CollectionIssueKind): Int = issues.count { it.kind == kind }

    val skippedCount: Int
        get() = issues.count { it.kind != CollectionIssueKind.CredentialsRemoved }
}

enum class CollectionParseError {
    Empty,
    TooLarge,
    HlsManifest,
    Malformed,
    NoPlayableEntries
}

sealed interface CollectionParseResult {
    data class Success(val collection: ParsedCollection) : CollectionParseResult
    data class Failure(val error: CollectionParseError) : CollectionParseResult
}

internal fun stableHash(value: String, length: Int = 24): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
    val hex = "0123456789abcdef"
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val number = byte.toInt() and 0xFF
            append(hex[number ushr 4])
            append(hex[number and 0x0F])
        }
    }.take(length)
}
