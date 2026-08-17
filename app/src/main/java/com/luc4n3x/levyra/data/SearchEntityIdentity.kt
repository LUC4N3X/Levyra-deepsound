package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.primaryArtistSegment
import java.util.Locale

internal fun searchSongIdentityKey(track: Track): String =
    track.id.trim().lowercase(Locale.ROOT)

internal fun searchAlbumCanonicalKey(album: AlbumHit): String {
    val browseId = album.browseId.trim().lowercase(Locale.ROOT)
    if (browseId.isNotBlank()) return "browse:$browseId"
    val playlistId = album.audioPlaylistId.trim().lowercase(Locale.ROOT)
    if (playlistId.isNotBlank()) return "playlist:$playlistId"
    val upc = album.upc.filter(Char::isLetterOrDigit).lowercase(Locale.ROOT)
    if (upc.isNotBlank()) return "upc:$upc"
    return ""
}

internal fun searchAlbumMetadataKey(album: AlbumHit): String = homeAlbumDeduplicationKey(album)

internal fun searchArtistCanonicalKey(artist: ArtistHit): String {
    val browseId = artist.browseId.trim().lowercase(Locale.ROOT)
    return if (browseId.isBlank()) "" else "browse:$browseId"
}

internal fun searchArtistMetadataKey(artist: ArtistHit): String {
    val primary = artistIdentityKey(primaryArtistSegment(artist.name))
    return if (primary.isBlank()) "" else "artist:$primary"
}

internal fun searchPlaylistCanonicalKey(playlist: PlaylistHit): String {
    val playlistId = playlist.playlistId.trim().lowercase(Locale.ROOT)
    if (playlistId.isNotBlank()) return "playlist:$playlistId"
    val browseId = playlist.browseId.trim().lowercase(Locale.ROOT)
    return if (browseId.isBlank()) "" else "browse:$browseId"
}

internal fun searchPlaylistMetadataKey(playlist: PlaylistHit): String {
    val title = albumRecommendationTextKey(playlist.title)
    if (title.isBlank()) return ""
    val author = albumRecommendationTextKey(primaryArtistSegment(playlist.author))
    return "playlist:$title|$author"
}

internal fun isMusicVideoResult(videoType: String): Boolean {
    val normalized = videoType.trim().uppercase(Locale.ROOT)
    if (normalized.isBlank()) return false
    return normalized != MUSIC_VIDEO_TYPE_AUDIO
}

internal fun mergeSearchSongs(existing: List<Track>, incoming: List<Track>): List<Track> =
    mergeSearchEntities(existing, incoming, ::searchSongIdentityKey, { "" }, ::richerSong)

internal fun mergeSearchAlbums(existing: List<AlbumHit>, incoming: List<AlbumHit>): List<AlbumHit> =
    mergeSearchEntities(existing, incoming, ::searchAlbumCanonicalKey, ::searchAlbumMetadataKey, ::richerAlbum)

internal fun mergeSearchArtists(existing: List<ArtistHit>, incoming: List<ArtistHit>): List<ArtistHit> =
    mergeSearchEntities(existing, incoming, ::searchArtistCanonicalKey, ::searchArtistMetadataKey, ::richerArtist)

internal fun mergeSearchPlaylists(existing: List<PlaylistHit>, incoming: List<PlaylistHit>): List<PlaylistHit> =
    mergeSearchEntities(existing, incoming, ::searchPlaylistCanonicalKey, ::searchPlaylistMetadataKey, ::richerPlaylist)

internal fun deduplicateSearchSongs(songs: List<Track>): List<Track> = mergeSearchSongs(emptyList(), songs)

internal fun deduplicateSearchAlbums(albums: List<AlbumHit>): List<AlbumHit> = mergeSearchAlbums(emptyList(), albums)

internal fun deduplicateSearchArtists(artists: List<ArtistHit>): List<ArtistHit> =
    mergeSearchArtists(emptyList(), artists)

internal fun deduplicateSearchPlaylists(playlists: List<PlaylistHit>): List<PlaylistHit> =
    mergeSearchPlaylists(emptyList(), playlists)

private fun <T> mergeSearchEntities(
    existing: List<T>,
    incoming: List<T>,
    canonicalKey: (T) -> String,
    metadataKey: (T) -> String,
    richer: (T, T) -> T
): List<T> {
    if (existing.isEmpty() && incoming.size < 2) return incoming
    val ordered = mutableListOf<T>()
    val slotByKey = HashMap<String, Int>()
    (existing.asSequence() + incoming.asSequence()).forEach { candidate ->
        val keys = entityKeys(candidate, canonicalKey, metadataKey)
        val slot = keys.firstNotNullOfOrNull(slotByKey::get)
        if (slot == null) {
            val index = ordered.size
            ordered.add(candidate)
            keys.forEach { key -> slotByKey[key] = index }
        } else {
            ordered[slot] = richer(ordered[slot], candidate)
            keys.forEach { key -> slotByKey.putIfAbsent(key, slot) }
        }
    }
    return ordered.toList()
}

private fun <T> entityKeys(
    entity: T,
    canonicalKey: (T) -> String,
    metadataKey: (T) -> String
): List<String> {
    val canonical = canonicalKey(entity)
    val metadata = metadataKey(entity)
    return buildList {
        if (canonical.isNotBlank()) add("id$canonical")
        if (metadata.isNotBlank()) add("meta$metadata")
    }
}

private fun richerSong(current: Track, candidate: Track): Track = current.copy(
    thumbnailUrl = current.thumbnailUrl.ifBlank { candidate.thumbnailUrl },
    largeThumbnailUrl = current.largeThumbnailUrl.ifBlank { candidate.largeThumbnailUrl },
    album = current.album.ifBlank { candidate.album },
    albumBrowseId = current.albumBrowseId.ifBlank { candidate.albumBrowseId },
    artistBrowseIds = current.artistBrowseIds.ifEmpty { candidate.artistBrowseIds },
    durationMs = if (current.durationMs > 0L) current.durationMs else candidate.durationMs,
    videoType = current.videoType.ifBlank { candidate.videoType }
)

private fun richerAlbum(current: AlbumHit, candidate: AlbumHit): AlbumHit = current.copy(
    browseId = current.browseId.ifBlank { candidate.browseId },
    artistBrowseId = current.artistBrowseId.ifBlank { candidate.artistBrowseId },
    audioPlaylistId = current.audioPlaylistId.ifBlank { candidate.audioPlaylistId },
    thumbnailUrl = current.thumbnailUrl.ifBlank { candidate.thumbnailUrl },
    year = current.year.ifBlank { candidate.year },
    releaseDate = current.releaseDate.ifBlank { candidate.releaseDate },
    upc = current.upc.ifBlank { candidate.upc },
    canonicalUrl = current.canonicalUrl.ifBlank { candidate.canonicalUrl },
    explicit = current.explicit || candidate.explicit,
    releaseType = if (current.releaseType == ReleaseType.Unknown) candidate.releaseType else current.releaseType
)

private fun richerArtist(current: ArtistHit, candidate: ArtistHit): ArtistHit = current.copy(
    browseId = current.browseId.ifBlank { candidate.browseId },
    thumbnailUrl = current.thumbnailUrl.ifBlank { candidate.thumbnailUrl },
    subscribers = current.subscribers.ifBlank { candidate.subscribers },
    officialArtwork = current.officialArtwork || candidate.officialArtwork
)

private fun richerPlaylist(current: PlaylistHit, candidate: PlaylistHit): PlaylistHit = current.copy(
    playlistId = current.playlistId.ifBlank { candidate.playlistId },
    browseId = current.browseId.ifBlank { candidate.browseId },
    thumbnailUrl = current.thumbnailUrl.ifBlank { candidate.thumbnailUrl },
    author = current.author.ifBlank { candidate.author },
    trackCountLabel = current.trackCountLabel.ifBlank { candidate.trackCountLabel }
)

private const val MUSIC_VIDEO_TYPE_AUDIO = "MUSIC_VIDEO_TYPE_ATV"
