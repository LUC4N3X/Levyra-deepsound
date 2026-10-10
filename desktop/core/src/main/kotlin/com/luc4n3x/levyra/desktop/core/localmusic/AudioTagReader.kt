package com.luc4n3x.levyra.desktop.core.localmusic

import java.nio.file.Path

object AudioTagReader {

    val SUPPORTED_EXTENSIONS: Set<String> = setOf(
        "mp3", "flac", "m4a", "m4b", "mp4", "aac", "ogg", "oga", "opus", "wav", "wma", "aiff", "aif"
    )

    fun extensionOf(path: Path): String =
        path.fileName?.toString().orEmpty().substringAfterLast('.', "").lowercase()

    fun isSupported(path: Path): Boolean = extensionOf(path) in SUPPORTED_EXTENSIONS

    fun read(path: Path): AudioTags {
        val tags = runCatching { TagFile.open(path).use(::readFrom) }.getOrNull() ?: AudioTags()
        return tags.mergedWith(fromFileName(path))
    }

    private fun readFrom(file: TagFile): AudioTags {
        val head = file.readAt(0L, 16)
        if (head.size < 4) return AudioTags()
        val id3Size = Id3TagReader.tagSize(head)
        if (id3Size > 0) {
            val afterTag = file.readAt(id3Size.toLong(), 4)
            if (afterTag.startsWithAscii("fLaC")) {
                return Id3TagReader.read(file).mergedWith(FlacTagReader.read(file, id3Size.toLong()))
            }
            val id3v1 = Id3TagReader.readId3v1(file)
            val trailing = if (id3v1 == AudioTags()) 0 else 128
            return Id3TagReader.read(file)
                .mergedWith(id3v1)
                .mergedWith(MpegAudioReader.read(file, id3Size, trailing))
        }
        return when {
            head.startsWithAscii("fLaC") -> FlacTagReader.read(file, 0L)
            head.startsWithAscii("OggS") -> OggTagReader.read(file)
            head.startsWithAscii("RIFF") -> RiffTagReader.read(file)
            head.size >= 8 && head.startsWithAscii("ftyp", 4) -> Mp4TagReader.read(file)
            head.u8(0) == 0xFF && head.u8(1) and 0xE0 == 0xE0 -> {
                val id3v1 = Id3TagReader.readId3v1(file)
                val trailing = if (id3v1 == AudioTags()) 0 else 128
                id3v1.mergedWith(MpegAudioReader.read(file, 0, trailing))
            }
            else -> AudioTags()
        }
    }

    private fun fromFileName(path: Path): AudioTags {
        val name = path.fileName?.toString().orEmpty().substringBeforeLast('.', "")
        val cleaned = TagText.clean(name.replace('_', ' '))
        if (cleaned.isEmpty()) return AudioTags()
        val separator = cleaned.indexOf(" - ")
        if (separator > 0) {
            return AudioTags(
                title = TagText.clean(cleaned.substring(separator + 3)),
                artist = TagText.clean(cleaned.substring(0, separator))
            )
        }
        return AudioTags(title = cleaned)
    }
}

internal object RiffTagReader {

    private const val MAX_CHUNKS = 64
    private val INFO_KEYS = mapOf(
        "INAM" to "title",
        "IART" to "artist",
        "IPRD" to "album",
        "IGNR" to "genre",
        "ICRD" to "year",
        "ITRK" to "track"
    )

    fun read(file: TagFile): AudioTags {
        val header = file.readAt(0L, 12)
        if (header.size < 12 || !header.startsWithAscii("WAVE", 8)) return AudioTags()
        var cursor = 12L
        var tags = AudioTags(codec = "PCM")
        var byteRate = 0L
        var chunks = 0
        while (chunks < MAX_CHUNKS && cursor + 8 <= file.length) {
            val head = file.readAt(cursor, 8)
            if (head.size < 8) break
            val id = head.ascii(0, 4)
            val size = head.u32le(4)
            val payloadStart = cursor + 8
            if (size < 0L || payloadStart + size > file.length) break
            when (id) {
                "fmt " -> {
                    val payload = file.readAt(payloadStart, minOf(size, 40L).toInt())
                    if (payload.size >= 16) {
                        byteRate = payload.u32le(8)
                        tags = tags.copy(
                            channels = payload.u16le(2),
                            sampleRateHz = payload.u32le(4).toInt(),
                            bitDepth = payload.u16le(14)
                        )
                    }
                }

                "data" -> if (byteRate > 0L) {
                    tags = tags.copy(durationMs = size * 1000L / byteRate)
                }

                "LIST" -> {
                    val payload = file.readAt(payloadStart, minOf(size, 64L * 1024L).toInt())
                    if (payload.startsWithAscii("INFO")) {
                        tags = tags.mergedWith(readInfo(payload))
                    }
                }
            }
            cursor = payloadStart + size + (size and 1L)
            chunks += 1
        }
        if (byteRate > 0L) {
            tags = tags.copy(bitrateKbps = (byteRate * 8L / 1000L).toInt().coerceIn(0, 20_000))
        }
        return tags
    }

    private fun readInfo(payload: ByteArray): AudioTags {
        var cursor = 4
        var tags = AudioTags()
        while (cursor + 8 <= payload.size) {
            val id = payload.ascii(cursor, 4)
            val size = payload.u32le(cursor + 4).toInt()
            cursor += 8
            if (size < 0 || cursor + size > payload.size) break
            val value = TagText.clean(payload.utf8(cursor, size))
            when (INFO_KEYS[id]) {
                "title" -> tags = tags.copy(title = tags.title.ifBlank { value })
                "artist" -> tags = tags.copy(artist = tags.artist.ifBlank { value })
                "album" -> tags = tags.copy(album = tags.album.ifBlank { value })
                "genre" -> tags = tags.copy(genre = tags.genre.ifBlank { value })
                "year" -> tags = tags.copy(year = if (tags.year > 0) tags.year else TagText.year(value))
                "track" -> tags = tags.copy(
                    trackNumber = if (tags.trackNumber > 0) tags.trackNumber else TagText.leadingNumber(value)
                )
            }
            cursor += size + (size and 1)
        }
        return tags
    }
}
