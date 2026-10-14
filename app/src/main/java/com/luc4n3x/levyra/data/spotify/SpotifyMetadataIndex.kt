package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import java.util.Locale

internal class SpotifyMetadataIndex(tracks: List<Track>) {
    private val entries = tracks.take(MAX_ENTRIES).distinctBy { it.id }
    private val byRecording = entries.groupBy(::recordingKey)
    private val byIsrc = entries.filter { it.isrc.isNotBlank() }.groupBy { it.isrc.uppercase(Locale.ROOT) }
    private val searchable = entries.map { it to MusicIdentityText.normalize("${it.title} ${it.artist} ${it.album}") }

    fun match(track: Track): Track? {
        if (preservesPersonalMetadata(track)) return null
        val candidates = byIsrc[track.isrc.uppercase(Locale.ROOT)].orEmpty()
            .ifEmpty { byRecording[recordingKey(track)].orEmpty() }
        val sameAlbum = candidates.filter {
            sameSpotifyAlbum(track.album, it.album)
        }
        return SpotifyYouTubeMatcher.findBestMatch(track, if (track.album.isNotBlank()) sameAlbum else candidates, METADATA_CONFIDENCE)?.candidate
    }

    fun search(query: String, limit: Int): List<Track> {
        val tokens = MusicIdentityText.normalize(query).split(' ').filter(String::isNotBlank)
        if (tokens.isEmpty()) return emptyList()
        return searchable.asSequence()
            .filter { (_, text) -> tokens.all(text::contains) }
            .take(limit.coerceIn(1, 25))
            .map { (track, _) -> track.copy(metadataProvider = "spotify") }
            .toList()
    }

    companion object {
        private const val MAX_ENTRIES = 10_000
        private const val METADATA_CONFIDENCE = 0.94

        private fun recordingKey(track: Track): String =
            MusicIdentityText.title(track.title).core + "|" + MusicIdentityText.artistCredit(track.artist).primary
    }
}

internal fun preservesPersonalMetadata(track: Track): Boolean =
    track.id.startsWith("local:", ignoreCase = true) ||
        track.source.equals("Offline", ignoreCase = true) ||
        listOf(track.streamUrl, track.thumbnailUrl, track.largeThumbnailUrl).any {
            it.startsWith("content://", ignoreCase = true) ||
                it.startsWith("file://", ignoreCase = true) || it.startsWith('/')
        }

internal fun withSpotifyMetadata(track: Track, spotify: Track): Track {
    if (preservesPersonalMetadata(track)) return track
    if (SpotifyYouTubeMatcher.scoreMatch(track, spotify) < 0.94) return track
    if (track.album.isNotBlank() && spotify.album.isNotBlank() && !sameSpotifyAlbum(track.album, spotify.album)) return track
    return track.copy(
        title = spotify.title.ifBlank { track.title },
        artist = spotify.artist.ifBlank { track.artist },
        album = spotify.album.ifBlank { track.album },
        thumbnailUrl = spotify.thumbnailUrl.ifBlank { track.thumbnailUrl },
        largeThumbnailUrl = spotify.largeThumbnailUrl.ifBlank { spotify.thumbnailUrl.ifBlank { track.largeThumbnailUrl } },
        isrc = spotify.isrc.ifBlank { track.isrc },
        upc = spotify.upc.ifBlank { track.upc },
        releaseDate = spotify.releaseDate.ifBlank { track.releaseDate },
        year = spotify.year.ifBlank { track.year },
        trackNumber = spotify.trackNumber.takeIf { it > 0 } ?: track.trackNumber,
        discNumber = spotify.discNumber.takeIf { it > 0 } ?: track.discNumber,
        trackTotal = spotify.trackTotal.takeIf { it > 0 } ?: track.trackTotal,
        discTotal = spotify.discTotal.takeIf { it > 0 } ?: track.discTotal,
        albumArtist = spotify.albumArtist.ifBlank { track.albumArtist },
        composer = spotify.composer.ifBlank { track.composer },
        copyright = spotify.copyright.ifBlank { track.copyright },
        canonicalAlbumUrl = spotify.canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
        explicit = spotify.explicit || track.explicit,
        metadataProvider = "spotify",
        metadataConfidence = maxOf(track.metadataConfidence, 94)
    )
}

internal fun sameSpotifyAlbum(first: String, second: String): Boolean {
    val expected = MusicIdentityText.album(first)
    val actual = MusicIdentityText.album(second)
    return expected.core.isNotBlank() && expected.core == actual.core && expected.editions == actual.editions
}
