package com.luc4n3x.levyra.nexus.playlistimport

const val MAX_PLAYLIST_IMPORT_TRACKS = 10_000
const val MAX_PLAYLIST_IMPORT_TEXT_CHARS = 8_000_000

enum class PlaylistImportSource {
    YOUTUBE_MUSIC,
    YOUTUBE,
    SPOTIFY,
    APPLE_MUSIC,
    DEEZER,
    TIDAL,
    SOUNDCLOUD,
    JIOSAAVN,
    AMAZON_MUSIC,
    BANDCAMP,
    M3U,
    PLS,
    XSPF,
    CSV,
    TSV,
    EXPORTIFY,
    TUNEMYMUSIC,
    KREATE,
    JSON,
    TEXT,
    LEVYRA
}

data class ImportedTrackIdentity(
    val position: Int,
    val title: String,
    val artists: List<String>,
    val featuredArtists: List<String> = emptyList(),
    val album: String = "",
    val durationMs: Long = 0L,
    val isrc: String = "",
    val sourceTrackId: String = "",
    val releaseYear: Int = 0,
    val explicit: Boolean? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val artworkUrl: String = "",
    val originalUrl: String = "",
    val directCatalogId: String = "",
    val localReference: String = ""
) {
    val primaryArtist: String
        get() = artists.firstOrNull().orEmpty()

    val artistLine: String
        get() = artists.joinToString(", ")

    val label: String
        get() = if (artists.isEmpty()) title else "$title — $artistLine"
}

data class ImportedPlaylistDescriptor(
    val source: PlaylistImportSource,
    val sourceId: String = "",
    val title: String = "",
    val owner: String = "",
    val description: String = "",
    val artworkUrl: String = "",
    val declaredTrackCount: Int? = null
)

enum class IncompleteReason {
    PROVIDER_PAGE_LIMIT,
    PAGINATION_STOPPED
}

sealed interface PlaylistImportCompleteness {
    data object Complete : PlaylistImportCompleteness

    data object Unknown : PlaylistImportCompleteness

    data class Incomplete(
        val retrieved: Int,
        val declared: Int?,
        val reason: IncompleteReason
    ) : PlaylistImportCompleteness
}

data class ParsedPlaylist(
    val descriptor: ImportedPlaylistDescriptor,
    val tracks: List<ImportedTrackIdentity>,
    val completeness: PlaylistImportCompleteness,
    val skippedRows: Int = 0
)

enum class PlaylistParseFailure {
    MALFORMED,
    TOO_LARGE,
    NO_TRACKS,
    PROVIDER_CHANGED
}

class PlaylistParseException(
    val failure: PlaylistParseFailure,
    message: String
) : Exception(message)

fun completenessOf(retrieved: Int, declared: Int?, reason: IncompleteReason): PlaylistImportCompleteness = when {
    declared == null -> PlaylistImportCompleteness.Unknown
    retrieved >= declared -> PlaylistImportCompleteness.Complete
    else -> PlaylistImportCompleteness.Incomplete(retrieved, declared, reason)
}

internal fun List<ImportedTrackIdentity>.reindexed(): List<ImportedTrackIdentity> =
    mapIndexed { index, track -> if (track.position == index) track else track.copy(position = index) }
