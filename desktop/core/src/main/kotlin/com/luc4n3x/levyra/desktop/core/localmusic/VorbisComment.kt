package com.luc4n3x.levyra.desktop.core.localmusic

import java.util.Base64

internal object VorbisComment {

    private const val MAX_COMMENT_BYTES = 4 * 1024 * 1024

    fun parse(block: ByteArray, offset: Int = 0): AudioTags {
        var cursor = offset
        if (cursor + 4 > block.size) return AudioTags()
        val vendorLength = block.u32le(cursor).toInt()
        cursor += 4
        if (vendorLength < 0 || cursor + vendorLength + 4 > block.size) return AudioTags()
        cursor += vendorLength
        val count = block.u32le(cursor).toInt()
        cursor += 4
        if (count < 0 || count > 4_096) return AudioTags()

        var tags = AudioTags()
        repeat(count) {
            if (cursor + 4 > block.size) return tags
            val length = block.u32le(cursor).toInt()
            cursor += 4
            if (length < 0 || length > MAX_COMMENT_BYTES || cursor + length > block.size) return tags
            val entry = block.utf8(cursor, length)
            cursor += length
            val separator = entry.indexOf('=')
            if (separator > 0) {
                tags = apply(tags, entry.substring(0, separator), entry.substring(separator + 1))
            }
        }
        return tags
    }

    private fun apply(tags: AudioTags, rawKey: String, rawValue: String): AudioTags {
        val key = rawKey.uppercase()
        if (key == "METADATA_BLOCK_PICTURE") {
            return tags.copy(artwork = tags.artwork ?: decodePicture(rawValue.trim()))
        }
        val value = TagText.clean(rawValue)
        if (value.isEmpty()) return tags
        return when (key) {
            "TITLE" -> tags.copy(title = tags.title.ifBlank { value })
            "ARTIST" -> tags.copy(artist = tags.artist.ifBlank { value })
            "ALBUMARTIST", "ALBUM ARTIST" -> tags.copy(albumArtist = tags.albumArtist.ifBlank { value })
            "ALBUM" -> tags.copy(album = tags.album.ifBlank { value })
            "GENRE" -> tags.copy(genre = tags.genre.ifBlank { value })
            "DATE", "YEAR", "ORIGINALDATE" -> tags.copy(
                year = if (tags.year > 0) tags.year else TagText.year(value)
            )
            "TRACKNUMBER" -> tags.copy(
                trackNumber = if (tags.trackNumber > 0) tags.trackNumber else TagText.leadingNumber(value)
            )
            "DISCNUMBER" -> tags.copy(
                discNumber = if (tags.discNumber > 0) tags.discNumber else TagText.leadingNumber(value)
            )
            else -> tags
        }
    }

    private fun decodePicture(encoded: String): EmbeddedArtwork? {
        val decoded = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() ?: return null
        return FlacPicture.parse(decoded)
    }
}

internal object FlacPicture {

    private const val MAX_PICTURE_BYTES = 6 * 1024 * 1024

    fun parse(block: ByteArray): EmbeddedArtwork? {
        if (block.size < 32) return null
        var cursor = 4
        val mimeLength = block.u32be(cursor).toInt()
        cursor += 4
        if (mimeLength < 0 || cursor + mimeLength + 4 > block.size) return null
        val declaredMime = block.ascii(cursor, mimeLength)
        cursor += mimeLength
        val descriptionLength = block.u32be(cursor).toInt()
        cursor += 4
        if (descriptionLength < 0 || cursor + descriptionLength + 20 > block.size) return null
        cursor += descriptionLength + 16
        val dataLength = block.u32be(cursor).toInt()
        cursor += 4
        if (dataLength <= 0 || dataLength > MAX_PICTURE_BYTES || cursor + dataLength > block.size) {
            return null
        }
        val bytes = block.copyOfRange(cursor, cursor + dataLength)
        val mime = TagText.imageMimeType(bytes, declaredMime)
        if (mime.isEmpty()) return null
        return EmbeddedArtwork(bytes, mime)
    }
}
