package com.luc4n3x.levyra.desktop.core.localmusic

import com.luc4n3x.levyra.desktop.core.model.Track
import java.security.MessageDigest
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class LocalFolder(
    val id: String,
    val path: String,
    val addedAtMs: Long = 0L
)

@Serializable
data class LocalTrack(
    val id: String,
    val path: String,
    val folderId: String = "",
    val fileSize: Long = 0L,
    val modifiedAtMs: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val albumArtist: String = "",
    val album: String = "",
    val genre: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val durationMs: Long = 0L,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val channels: Int = 0,
    val codec: String = "",
    val artworkPath: String = "",
    val available: Boolean = true,
    val addedAtMs: Long = 0L
) {
    val effectiveAlbumArtist: String
        get() = albumArtist.ifBlank { artist }

    val qualityLabel: String
        get() = buildList {
            if (codec.isNotBlank()) add(codec)
            if (bitDepth > 0) add("${bitDepth}bit")
            if (sampleRateHz > 0) add(formatSampleRate(sampleRateHz))
            if (bitrateKbps > 0 && bitDepth == 0) add("${bitrateKbps}kbps")
        }.joinToString(" · ")

    val isHighResolution: Boolean
        get() = bitDepth >= 24 && sampleRateHz >= 44_100

    fun toTrack(): Track = Track(
        id = LocalMusicIdentity.trackId(id),
        title = title.ifBlank { path.takeLastWhile { it !in PATH_SEPARATORS } },
        artist = artist,
        album = album,
        videoUrl = "",
        artworkUrl = artworkPath,
        durationMs = durationMs,
        offlinePath = path,
        offlineMediaLabel = qualityLabel
    )

    private fun formatSampleRate(value: Int): String {
        val khz = value / 1000.0
        return if (khz % 1.0 == 0.0) "${khz.toInt()}kHz" else String.format(Locale.ROOT, "%.1fkHz", khz)
    }
}

private val PATH_SEPARATORS = charArrayOf('/', Char(92))

@Serializable
data class LocalLibraryData(
    val folders: List<LocalFolder> = emptyList(),
    val tracks: List<LocalTrack> = emptyList(),
    val lastScanAtMs: Long = 0L
)

data class LocalAlbum(
    val key: String,
    val title: String,
    val albumArtist: String,
    val year: Int,
    val artworkPath: String,
    val trackCount: Int,
    val durationMs: Long
)

data class LocalArtist(
    val key: String,
    val name: String,
    val artworkPath: String,
    val trackCount: Int,
    val albumCount: Int
)

object LocalMusicIdentity {
    const val TRACK_ID_PREFIX = "local:"

    fun trackId(hash: String): String = "$TRACK_ID_PREFIX$hash"

    fun isLocalTrackId(value: String): Boolean = value.startsWith(TRACK_ID_PREFIX)

    fun hashOf(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(value.lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    fun normalizeKey(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun albumKey(album: String, albumArtist: String): String =
        "${normalizeKey(album)}|${normalizeKey(albumArtist)}"
}
