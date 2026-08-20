package com.luc4n3x.levyra.desktop.core.localmusic

class EmbeddedArtwork(val bytes: ByteArray, val mimeType: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddedArtwork) return false
        return mimeType == other.mimeType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * mimeType.hashCode() + bytes.contentHashCode()
}

data class AudioTags(
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
    val channels: Int = 0,
    val bitDepth: Int = 0,
    val codec: String = "",
    val artwork: EmbeddedArtwork? = null
) {
    fun mergedWith(fallback: AudioTags): AudioTags = AudioTags(
        title = title.ifBlank { fallback.title },
        artist = artist.ifBlank { fallback.artist },
        albumArtist = albumArtist.ifBlank { fallback.albumArtist },
        album = album.ifBlank { fallback.album },
        genre = genre.ifBlank { fallback.genre },
        year = if (year > 0) year else fallback.year,
        trackNumber = if (trackNumber > 0) trackNumber else fallback.trackNumber,
        discNumber = if (discNumber > 0) discNumber else fallback.discNumber,
        durationMs = if (durationMs > 0L) durationMs else fallback.durationMs,
        bitrateKbps = if (bitrateKbps > 0) bitrateKbps else fallback.bitrateKbps,
        sampleRateHz = if (sampleRateHz > 0) sampleRateHz else fallback.sampleRateHz,
        channels = if (channels > 0) channels else fallback.channels,
        bitDepth = if (bitDepth > 0) bitDepth else fallback.bitDepth,
        codec = codec.ifBlank { fallback.codec },
        artwork = artwork ?: fallback.artwork
    )
}

internal object TagText {
    private const val MAX_TEXT_LENGTH = 320

    fun clean(value: String): String = value
        .trim()
        .trimEnd('\u0000')
        .trim()
        .replace('\u0000', ' ')
        .take(MAX_TEXT_LENGTH)
        .trim()

    fun leadingNumber(value: String): Int {
        val digits = value.trim().takeWhile { it.isDigit() }
        if (digits.isEmpty()) return 0
        return digits.toIntOrNull()?.coerceIn(0, 9_999) ?: 0
    }

    fun year(value: String): Int {
        val digits = value.trim().takeWhile { it.isDigit() }
        val parsed = digits.toIntOrNull() ?: return 0
        return if (parsed in 1000..9999) parsed else 0
    }

    fun imageMimeType(bytes: ByteArray, declared: String): String {
        val normalized = declared.trim().lowercase()
        if (normalized.startsWith("image/")) return normalized
        return sniffImageMimeType(bytes)
    }

    fun sniffImageMimeType(bytes: ByteArray): String = when {
        bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png"
        bytes.size > 12 && bytes[0] == 'R'.code.toByte() && bytes[8] == 'W'.code.toByte() -> "image/webp"
        else -> ""
    }

    fun extensionFor(mimeType: String): String = when (mimeType.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> ""
    }
}
