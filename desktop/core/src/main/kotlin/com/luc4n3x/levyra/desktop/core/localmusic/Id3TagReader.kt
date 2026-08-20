package com.luc4n3x.levyra.desktop.core.localmusic

import java.nio.charset.StandardCharsets

internal object Id3TagReader {

    private const val MAX_TAG_BYTES = 4 * 1024 * 1024
    private const val MAX_PICTURE_BYTES = 6 * 1024 * 1024

    fun tagSize(header: ByteArray): Int {
        if (header.size < 10 || !header.startsWithAscii("ID3")) return 0
        val footer = if (header.u8(5) and 0x10 != 0) 10 else 0
        return 10 + syncSafe(header, 6) + footer
    }

    fun read(file: TagFile): AudioTags {
        val header = file.readAt(0L, 10)
        if (header.size < 10 || !header.startsWithAscii("ID3")) return AudioTags()
        val major = header.u8(3)
        if (major < 2 || major > 4) return AudioTags()
        val flags = header.u8(5)
        val declared = syncSafe(header, 6)
        if (declared <= 0 || declared > MAX_TAG_BYTES) return AudioTags()

        var body = file.readAt(10L, declared)
        if (flags and 0x80 != 0) {
            body = removeUnsynchronisation(body)
        }
        var cursor = 0
        if (flags and 0x40 != 0) {
            val extendedSize = if (major == 4) syncSafe(body, 0) else body.u32be(0).toInt() + 4
            if (extendedSize <= 0 || extendedSize >= body.size) return AudioTags()
            cursor = extendedSize
        }

        val idSize = if (major == 2) 3 else 4
        val headerSize = if (major == 2) 6 else 10
        var tags = AudioTags()
        while (cursor + headerSize <= body.size) {
            val frameId = body.ascii(cursor, idSize)
            if (frameId.isBlank() || frameId[0] == '\u0000') break
            val frameSize = when (major) {
                2 -> body.u24be(cursor + 3)
                4 -> syncSafe(body, cursor + 4)
                else -> body.u32be(cursor + 4).toInt()
            }
            if (frameSize <= 0 || cursor + headerSize + frameSize > body.size) break
            val payload = body.copyOfRange(cursor + headerSize, cursor + headerSize + frameSize)
            tags = applyFrame(tags, frameId, payload)
            cursor += headerSize + frameSize
        }
        return tags
    }

    private fun applyFrame(tags: AudioTags, frameId: String, payload: ByteArray): AudioTags {
        if (frameId == "APIC" || frameId == "PIC") {
            return tags.copy(artwork = tags.artwork ?: picture(frameId, payload))
        }
        if (!frameId.startsWith("T")) return tags
        val text = TagText.clean(decodeText(payload))
        if (text.isEmpty()) return tags
        return when (frameId) {
            "TIT2", "TT2" -> tags.copy(title = tags.title.ifBlank { text })
            "TPE1", "TP1" -> tags.copy(artist = tags.artist.ifBlank { text })
            "TPE2", "TP2" -> tags.copy(albumArtist = tags.albumArtist.ifBlank { text })
            "TALB", "TAL" -> tags.copy(album = tags.album.ifBlank { text })
            "TCON", "TCO" -> tags.copy(genre = tags.genre.ifBlank { decodeGenre(text) })
            "TRCK", "TRK" -> tags.copy(
                trackNumber = if (tags.trackNumber > 0) tags.trackNumber else TagText.leadingNumber(text)
            )
            "TPOS", "TPA" -> tags.copy(
                discNumber = if (tags.discNumber > 0) tags.discNumber else TagText.leadingNumber(text)
            )
            "TDRC", "TYER", "TYE", "TDRL" -> tags.copy(
                year = if (tags.year > 0) tags.year else TagText.year(text)
            )
            else -> tags
        }
    }

    private fun picture(frameId: String, payload: ByteArray): EmbeddedArtwork? {
        if (payload.isEmpty()) return null
        val encoding = payload.u8(0)
        var cursor = 1
        val declaredMime: String
        if (frameId == "PIC") {
            if (payload.size < 5) return null
            declaredMime = if (payload.ascii(1, 3).uppercase() == "PNG") "image/png" else "image/jpeg"
            cursor = 4
        } else {
            val end = payload.indexOfByte(0, cursor)
            if (end < 0) return null
            declaredMime = payload.ascii(cursor, end - cursor)
            cursor = end + 1
        }
        if (cursor >= payload.size) return null
        cursor += 1
        cursor = skipTerminatedText(payload, cursor, encoding)
        if (cursor < 0 || cursor >= payload.size) return null
        val bytes = payload.copyOfRange(cursor, payload.size)
        if (bytes.isEmpty() || bytes.size > MAX_PICTURE_BYTES) return null
        val mime = TagText.imageMimeType(bytes, declaredMime)
        if (mime.isEmpty()) return null
        return EmbeddedArtwork(bytes, mime)
    }

    private fun skipTerminatedText(payload: ByteArray, start: Int, encoding: Int): Int {
        if (encoding == 1 || encoding == 2) {
            var index = start
            while (index + 1 < payload.size) {
                if (payload[index] == 0.toByte() && payload[index + 1] == 0.toByte()) {
                    return index + 2
                }
                index += 2
            }
            return -1
        }
        val end = payload.indexOfByte(0, start)
        return if (end < 0) -1 else end + 1
    }

    private fun decodeText(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val encoding = payload.u8(0)
        val body = payload.copyOfRange(1, payload.size)
        return when (encoding) {
            1 -> String(body, StandardCharsets.UTF_16)
            2 -> String(body, StandardCharsets.UTF_16BE)
            3 -> String(body, StandardCharsets.UTF_8)
            else -> String(body, StandardCharsets.ISO_8859_1)
        }
    }

    private fun decodeGenre(value: String): String {
        val trimmed = value.trim()
        if (!trimmed.startsWith("(")) return trimmed
        val end = trimmed.indexOf(')')
        if (end <= 1) return trimmed
        val remainder = trimmed.substring(end + 1).trim()
        if (remainder.isNotEmpty()) return remainder
        val index = trimmed.substring(1, end).toIntOrNull() ?: return trimmed
        return ID3V1_GENRES.getOrElse(index) { trimmed }
    }

    fun readId3v1(file: TagFile): AudioTags {
        if (file.length < 128L) return AudioTags()
        val block = file.readAt(file.length - 128L, 128)
        if (block.size < 128 || !block.startsWithAscii("TAG")) return AudioTags()
        val genreIndex = block.u8(127)
        val track = if (block[125] == 0.toByte() && block[126] != 0.toByte()) block.u8(126) else 0
        return AudioTags(
            title = latin(block, 3, 30),
            artist = latin(block, 33, 30),
            album = latin(block, 63, 30),
            year = TagText.year(latin(block, 93, 4)),
            trackNumber = track,
            genre = ID3V1_GENRES.getOrElse(genreIndex) { "" }
        )
    }

    private fun latin(block: ByteArray, offset: Int, size: Int): String =
        TagText.clean(String(block, offset, size, StandardCharsets.ISO_8859_1))

    private fun syncSafe(source: ByteArray, offset: Int): Int {
        if (source.size < offset + 4) return 0
        return ((source.u8(offset) and 0x7F) shl 21) or
            ((source.u8(offset + 1) and 0x7F) shl 14) or
            ((source.u8(offset + 2) and 0x7F) shl 7) or
            (source.u8(offset + 3) and 0x7F)
    }

    private fun removeUnsynchronisation(source: ByteArray): ByteArray {
        val output = ByteArray(source.size)
        var written = 0
        var index = 0
        while (index < source.size) {
            val current = source[index]
            output[written] = current
            written += 1
            index += 1
            if (current == 0xFF.toByte() && index < source.size && source[index] == 0x00.toByte()) {
                index += 1
            }
        }
        return output.copyOf(written)
    }

    private val ID3V1_GENRES = listOf(
        "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop",
        "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock",
        "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack",
        "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance",
        "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise",
        "Alternative Rock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop",
        "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic",
        "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta",
        "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native US", "Cabaret", "New Wave",
        "Psychedelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk",
        "Acid Jazz", "Polka", "Retro", "Musical", "Rock and Roll", "Hard Rock"
    )
}

internal fun ByteArray.indexOfByte(value: Int, from: Int): Int {
    var index = from
    while (index < size) {
        if (this[index].toInt() and 0xFF == value) return index
        index += 1
    }
    return -1
}
