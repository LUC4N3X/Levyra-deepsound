package com.luc4n3x.levyra.data.locallibrary

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.luc4n3x.levyra.data.local.LocalMediaEntity
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

data class LocalTagEdits(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: String,
    val trackNumber: String,
    val discNumber: String,
    val composer: String,
    val lyricist: String,
    val comment: String,
    val copyright: String
)

sealed interface LocalTagWriteResult {
    data class Success(val media: LocalMediaEntity) : LocalTagWriteResult
    data class PermissionRequired(val intentSender: android.content.IntentSender) : LocalTagWriteResult
    data object UnsupportedFormat : LocalTagWriteResult
    data object FileTooLarge : LocalTagWriteResult
    data object FileUnavailable : LocalTagWriteResult
    data object Failed : LocalTagWriteResult
}

internal data class LocalDeepTags(
    val composer: String = "",
    val lyricist: String = "",
    val comment: String = "",
    val copyright: String = "",
    val customTags: String = ""
)

internal object LocalDeepTagReader {
    private const val MAX_ID3_BYTES = 4 * 1024 * 1024
    private const val MAX_COMMENT_BYTES = 4 * 1024 * 1024
    private const val MAX_OGG_PREFIX_BYTES = 2 * 1024 * 1024
    private const val MAX_CUSTOM_VALUE = 4_096
    private const val MAX_CUSTOM_TEXT = 24_000

    fun read(context: Context, row: LocalMediaEntity): LocalDeepTags {
        val path = row.filePath.trim()
        if (path.isNotEmpty()) {
            val file = File(path)
            if (file.isFile && file.canRead() && file.length() > 0L) {
                runCatching {
                    RandomAccessTagSource(RandomAccessFile(file, "r")).use(::readSource)
                }.getOrNull()?.let { return it }
            }
        }
        val uri = runCatching { Uri.parse(row.contentUri) }.getOrNull() ?: return LocalDeepTags()
        if (!uri.scheme.equals("content", ignoreCase = true)) return LocalDeepTags()
        return runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@runCatching LocalDeepTags()
            ChannelTagSource(ParcelFileDescriptor.AutoCloseInputStream(descriptor)).use(::readSource)
        }.getOrDefault(LocalDeepTags())
    }

    internal fun read(file: File): LocalDeepTags =
        runCatching {
            RandomAccessTagSource(RandomAccessFile(file, "r")).use(::readSource)
        }.getOrDefault(LocalDeepTags())

    private fun readSource(source: SeekableTagSource): LocalDeepTags {
        if (source.length < 4L) return LocalDeepTags()
        val head = ByteArray(minOf(16L, source.length).toInt())
        source.seek(0L)
        source.readFully(head)
        val tags = when {
            startsWith(head, "ID3") -> readId3(source)
            startsWith(head, "fLaC") -> readFlac(source, 0L)
            startsWith(head, "OggS") -> readOgg(source)
            head.size >= 8 && ascii(head, 4, 4) == "ftyp" -> readMp4(source)
            else -> emptyMap()
        }
        return tags.toDeepTags()
    }

    private fun readId3(source: SeekableTagSource): Map<String, String> {
        source.seek(0L)
        val header = ByteArray(10)
        if (source.read(header) != header.size || !startsWith(header, "ID3")) return emptyMap()
        val version = header[3].toInt() and 0xFF
        if (version !in 2..4) return emptyMap()
        val size = syncSafeInt(header, 6).coerceAtMost(MAX_ID3_BYTES)
        if (size <= 0) return emptyMap()

        var payload = ByteArray(size)
        source.readFully(payload)
        if (header[5].toInt() and 0x80 != 0) payload = deUnsynchronize(payload)
        val cursor = id3FramesStart(payload, version, header[5].toInt() and 0xFF)
        return if (version == 2) {
            readId3v22Frames(payload, cursor)
        } else {
            readId3v23PlusFrames(payload, cursor, version)
        }
    }

    private fun readId3v22Frames(payload: ByteArray, start: Int): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var cursor = start
        while (cursor + 6 <= payload.size) {
            val id = ascii(payload, cursor, 3)
            if (!validFrameId(id)) break
            val frameSize = ((payload[cursor + 3].toInt() and 0xFF) shl 16) or
                ((payload[cursor + 4].toInt() and 0xFF) shl 8) or
                (payload[cursor + 5].toInt() and 0xFF)
            cursor += 6
            if (frameSize <= 0 || cursor + frameSize > payload.size) break
            readId3Frame(id3v22ToV23(id), payload.copyOfRange(cursor, cursor + frameSize), result)
            cursor += frameSize
        }
        return result
    }

    private fun readId3v23PlusFrames(
        payload: ByteArray,
        start: Int,
        version: Int
    ): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var cursor = start
        while (cursor + 10 <= payload.size) {
            val id = ascii(payload, cursor, 4)
            if (!validFrameId(id)) break
            val frameSize = if (version == 4) syncSafeInt(payload, cursor + 4) else int32be(payload, cursor + 4)
            val flags = ((payload[cursor + 8].toInt() and 0xFF) shl 8) or
                (payload[cursor + 9].toInt() and 0xFF)
            cursor += 10
            if (frameSize <= 0 || cursor + frameSize > payload.size) break
            var frame = payload.copyOfRange(cursor, cursor + frameSize)
            if (version == 4 && flags and 0x0002 != 0) frame = deUnsynchronize(frame)
            readId3Frame(id, frame, result)
            cursor += frameSize
        }
        return result
    }

    private fun id3FramesStart(payload: ByteArray, version: Int, flags: Int): Int {
        if (flags and 0x40 == 0 || payload.size < 4 || version == 2) return 0
        val declared = if (version == 4) syncSafeInt(payload, 0) else int32be(payload, 0)
        return when (version) {
            3 -> (declared + 4).coerceIn(0, payload.size)
            4 -> declared.coerceIn(0, payload.size)
            else -> 0
        }
    }

    private fun readId3Frame(id: String, bytes: ByteArray, out: MutableMap<String, String>) {
        if (bytes.isEmpty()) return
        when {
            id == "COMM" -> id3Comment(bytes)?.let { putTag(out, "COMMENT", it) }
            id == "TXXX" -> {
                val pair = id3UserText(bytes) ?: return
                putTag(out, pair.first.uppercase(Locale.ROOT), pair.second)
            }
            id.startsWith("T") -> {
                val value = decodeId3Text(bytes)
                val key = when (id) {
                    "TIT2" -> "TITLE"
                    "TPE1" -> "ARTIST"
                    "TPE2" -> "ALBUMARTIST"
                    "TALB" -> "ALBUM"
                    "TCON" -> "GENRE"
                    "TDRC", "TYER" -> "DATE"
                    "TRCK" -> "TRACKNUMBER"
                    "TPOS" -> "DISCNUMBER"
                    "TCOM" -> "COMPOSER"
                    "TEXT" -> "LYRICIST"
                    "TCOP" -> "COPYRIGHT"
                    else -> id
                }
                putTag(out, key, value)
            }
        }
    }

    private fun id3UserText(bytes: ByteArray): Pair<String, String>? {
        val encoding = bytes[0].toInt() and 0xFF
        val decoded = decodeId3String(bytes, 1, bytes.size - 1, encoding)
        val parts = decoded.split('\u0000', limit = 2)
        if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) return null
        return parts[0].trim() to parts[1].trim()
    }

    private fun id3Comment(bytes: ByteArray): String? {
        if (bytes.size <= 4) return null
        val encoding = bytes[0].toInt() and 0xFF
        val decoded = decodeId3String(bytes, 4, bytes.size - 4, encoding)
        val split = decoded.indexOf('\u0000')
        return (if (split >= 0) decoded.substring(split + 1) else decoded).trim().takeIf { it.isNotEmpty() }
    }

    private fun decodeId3Text(bytes: ByteArray): String {
        if (bytes.size <= 1) return ""
        return decodeId3String(bytes, 1, bytes.size - 1, bytes[0].toInt() and 0xFF)
            .replace('\u0000', ' ')
            .trim()
    }

    private fun decodeId3String(bytes: ByteArray, offset: Int, length: Int, encoding: Int): String {
        if (length <= 0 || offset !in 0..bytes.size || offset + length > bytes.size) return ""
        val charset: Charset = when (encoding) {
            0 -> StandardCharsets.ISO_8859_1
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            else -> StandardCharsets.UTF_8
        }
        return runCatching { String(bytes, offset, length, charset) }.getOrDefault("")
            .trimEnd('\u0000')
    }

    private fun readFlac(source: SeekableTagSource, start: Long): Map<String, String> {
        source.seek(start)
        val magic = ByteArray(4)
        if (source.read(magic) != 4 || !startsWith(magic, "fLaC")) return emptyMap()
        var last = false
        var blocks = 0
        while (!last && blocks++ < 128 && source.position + 4 <= source.length) {
            val header = ByteArray(4)
            source.readFully(header)
            last = header[0].toInt() and 0x80 != 0
            val type = header[0].toInt() and 0x7F
            val size = ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)
            if (size < 0 || source.position + size > source.length) break
            if (type == 4 && size <= MAX_COMMENT_BYTES) {
                val payload = ByteArray(size)
                source.readFully(payload)
                return parseVorbisComments(payload, 0)
            }
            source.seek(source.position + size)
        }
        return emptyMap()
    }

    private fun readOgg(source: SeekableTagSource): Map<String, String> {
        val size = minOf(source.length, MAX_OGG_PREFIX_BYTES.toLong()).toInt()
        val bytes = ByteArray(size)
        source.seek(0L)
        source.readFully(bytes)
        val opus = indexOf(bytes, "OpusTags".toByteArray(StandardCharsets.US_ASCII))
        if (opus >= 0) return parseVorbisComments(bytes, opus + 8)
        val vorbisSignature = byteArrayOf(3) + "vorbis".toByteArray(StandardCharsets.US_ASCII)
        val vorbis = indexOf(bytes, vorbisSignature)
        if (vorbis >= 0) return parseVorbisComments(bytes, vorbis + vorbisSignature.size)
        return emptyMap()
    }

    private fun parseVorbisComments(bytes: ByteArray, start: Int): Map<String, String> {
        var cursor = start
        if (cursor + 4 > bytes.size) return emptyMap()
        val vendorSize = int32le(bytes, cursor)
        cursor += 4
        if (vendorSize < 0 || vendorSize > MAX_COMMENT_BYTES || cursor + vendorSize > bytes.size) return emptyMap()
        cursor += vendorSize
        if (cursor + 4 > bytes.size) return emptyMap()
        val count = int32le(bytes, cursor).coerceIn(0, 50_000)
        cursor += 4
        val result = linkedMapOf<String, String>()
        repeat(count) {
            if (cursor + 4 > bytes.size) return@repeat
            val length = int32le(bytes, cursor)
            cursor += 4
            if (length < 0 || length > MAX_COMMENT_BYTES || cursor + length > bytes.size) return@repeat
            val entry = String(bytes, cursor, length, StandardCharsets.UTF_8)
            cursor += length
            val separator = entry.indexOf('=')
            if (separator > 0) putTag(result, entry.substring(0, separator).uppercase(Locale.ROOT), entry.substring(separator + 1))
        }
        return result
    }

    private fun readMp4(source: SeekableTagSource): Map<String, String> {
        val moov = findChild(source, 0L, source.length, MP4_MOOV) ?: return emptyMap()
        val udta = findChild(source, moov.payloadStart, moov.end, MP4_UDTA)
        val meta = when {
            udta != null -> findChild(source, udta.payloadStart, udta.end, MP4_META)
            else -> findChild(source, moov.payloadStart, moov.end, MP4_META)
        } ?: return emptyMap()
        val ilst = findChild(source, (meta.payloadStart + 4).coerceAtMost(meta.end), meta.end, MP4_ILST)
            ?: return emptyMap()
        val result = linkedMapOf<String, String>()
        var cursor = ilst.payloadStart
        while (cursor + 8 <= ilst.end) {
            val item = readMp4Box(source, cursor, ilst.end) ?: break
            when (item.type) {
                MP4_COMPOSER -> mp4DataText(source, item)?.let { putTag(result, "COMPOSER", it) }
                MP4_COMMENT -> mp4DataText(source, item)?.let { putTag(result, "COMMENT", it) }
                MP4_COPYRIGHT, MP4_COPYRIGHT_ALT -> mp4DataText(source, item)?.let { putTag(result, "COPYRIGHT", it) }
                MP4_FREEFORM -> {
                    val pair = mp4Freeform(source, item)
                    if (pair != null) putTag(result, pair.first.uppercase(Locale.ROOT), pair.second)
                }
            }
            cursor = item.end
        }
        return result
    }

    private fun mp4DataText(source: SeekableTagSource, parent: Mp4Box): String? {
        val data = findChild(source, parent.payloadStart, parent.end, MP4_DATA) ?: return null
        val start = data.payloadStart + 8
        if (start >= data.end || data.end - start > MAX_CUSTOM_VALUE * 4L) return null
        val bytes = ByteArray((data.end - start).toInt())
        source.seek(start)
        source.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8).trim('\u0000', ' ', '\r', '\n').takeIf { it.isNotEmpty() }
    }

    private fun mp4Freeform(source: SeekableTagSource, parent: Mp4Box): Pair<String, String>? {
        var name = ""
        var value = ""
        var cursor = parent.payloadStart
        while (cursor + 8 <= parent.end) {
            val child = readMp4Box(source, cursor, parent.end) ?: break
            when (child.type) {
                MP4_NAME -> name = mp4FullBoxText(source, child)
                MP4_DATA -> {
                    val start = child.payloadStart + 8
                    if (start < child.end && child.end - start <= MAX_CUSTOM_VALUE * 4L) {
                        val bytes = ByteArray((child.end - start).toInt())
                        source.seek(start)
                        source.readFully(bytes)
                        value = String(bytes, StandardCharsets.UTF_8).trim('\u0000', ' ', '\r', '\n')
                    }
                }
            }
            cursor = child.end
        }
        return if (name.isNotBlank() && value.isNotBlank()) name to value else null
    }

    private fun mp4FullBoxText(source: SeekableTagSource, box: Mp4Box): String {
        val start = box.payloadStart + 4
        if (start >= box.end || box.end - start > 1024L) return ""
        val bytes = ByteArray((box.end - start).toInt())
        source.seek(start)
        source.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8).trim('\u0000', ' ', '\r', '\n')
    }

    private fun findChild(source: SeekableTagSource, start: Long, end: Long, type: Int): Mp4Box? {
        var cursor = start
        var guard = 0
        while (cursor + 8 <= end && guard++ < 100_000) {
            val box = readMp4Box(source, cursor, end) ?: return null
            if (box.type == type) return box
            if (box.end <= cursor) return null
            cursor = box.end
        }
        return null
    }

    private fun readMp4Box(source: SeekableTagSource, start: Long, parentEnd: Long): Mp4Box? {
        if (start < 0L || start + 8 > parentEnd || start + 8 > source.length) return null
        source.seek(start)
        val header = ByteArray(16)
        val count = source.read(header)
        if (count < 8) return null
        val shortSize = uint32be(header, 0)
        val type = int32be(header, 4)
        val headerSize: Int
        val size: Long
        when (shortSize) {
            0L -> {
                headerSize = 8
                size = parentEnd - start
            }
            1L -> {
                if (count < 16) return null
                headerSize = 16
                size = int64be(header, 8)
            }
            else -> {
                headerSize = 8
                size = shortSize
            }
        }
        if (size < headerSize || start + size > parentEnd || start + size > source.length) return null
        return Mp4Box(start, start + size, headerSize, type)
    }

    private fun Map<String, String>.toDeepTags(): LocalDeepTags {
        fun first(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
            this[key]?.cleanDeepTag()?.takeIf { it.isNotEmpty() }
        }.orEmpty()

        val composer = first("COMPOSER", "TCOM")
        val lyricist = first("LYRICIST", "TEXT", "WRITER", "SONGWRITER")
        val comment = first("COMMENT", "DESCRIPTION", "COMM")
        val copyright = first("COPYRIGHT", "TCOP", "CPRT")
        val reserved = RESERVED_TAG_KEYS + setOf(
            "COMPOSER", "TCOM", "LYRICIST", "TEXT", "WRITER", "SONGWRITER",
            "COMMENT", "DESCRIPTION", "COMM", "COPYRIGHT", "TCOP", "CPRT"
        )
        val custom = entries.asSequence()
            .filter { (key, value) -> key !in reserved && safeSearchTag(key) && value.isNotBlank() }
            .map { (key, value) -> "${key.take(64)}=${value.cleanDeepTag().take(MAX_CUSTOM_VALUE)}" }
            .filter { it.substringAfter('=').isNotBlank() }
            .joinToString("\n")
            .take(MAX_CUSTOM_TEXT)

        return LocalDeepTags(
            composer = composer,
            lyricist = lyricist,
            comment = comment,
            copyright = copyright,
            customTags = custom
        )
    }

    private fun putTag(out: MutableMap<String, String>, key: String, raw: String) {
        val cleanKey = key.trim().uppercase(Locale.ROOT).take(96)
        val cleanValue = raw.cleanDeepTag()
        if (cleanKey.isBlank() || cleanValue.isBlank()) return
        out[cleanKey] = if (cleanKey in MULTI_VALUE_KEYS && out[cleanKey].orEmpty().isNotBlank()) {
            "${out.getValue(cleanKey)}; $cleanValue".take(MAX_CUSTOM_VALUE)
        } else {
            cleanValue.take(MAX_CUSTOM_VALUE)
        }
    }

    private fun String.cleanDeepTag(): String =
        trim().replace(Regex("[\\u0000-\\u001F&&[^\\n\\r\\t]]+"), " ").replace(Regex("[ \\t]+"), " ").trim()

    private fun safeSearchTag(key: String): Boolean {
        val upper = key.uppercase(Locale.ROOT)
        if (upper in RESERVED_TAG_KEYS) return false
        return !upper.contains("PICTURE") &&
            !upper.contains("COVERART") &&
            upper != "APIC" &&
            upper != "USLT" &&
            upper != "SYLT" &&
            upper != "LYRICS" &&
            upper != "UNSYNCEDLYRICS" &&
            upper != "SYNCEDLYRICS"
    }

    private fun validFrameId(id: String): Boolean =
        id.isNotBlank() && id.all { it in 'A'..'Z' || it in '0'..'9' }

    private fun id3v22ToV23(id: String): String = when (id) {
        "TT2" -> "TIT2"
        "TP1" -> "TPE1"
        "TP2" -> "TPE2"
        "TAL" -> "TALB"
        "TCO" -> "TCON"
        "TYE" -> "TYER"
        "TRK" -> "TRCK"
        "TPA" -> "TPOS"
        "TCM" -> "TCOM"
        "TXT" -> "TEXT"
        "TCR" -> "TCOP"
        "COM" -> "COMM"
        "TXX" -> "TXXX"
        else -> id
    }

    private fun deUnsynchronize(input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        var read = 0
        var write = 0
        while (read < input.size) {
            val current = input[read++]
            out[write++] = current
            if (current == 0xFF.toByte() && read < input.size && input[read] == 0.toByte()) read++
        }
        return out.copyOf(write)
    }

    private fun startsWith(bytes: ByteArray, text: String): Boolean =
        bytes.size >= text.length && text.indices.all { bytes[it] == text[it].code.toByte() }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, StandardCharsets.ISO_8859_1)

    private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun int32be(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private fun uint32be(bytes: ByteArray, offset: Int): Long = int32be(bytes, offset).toLong() and 0xFFFF_FFFFL

    private fun int32le(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun int64be(bytes: ByteArray, offset: Int): Long =
        ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.BIG_ENDIAN).long

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        for (start in 0..haystack.size - needle.size) {
            var match = true
            for (index in needle.indices) {
                if (haystack[start + index] != needle[index]) {
                    match = false
                    break
                }
            }
            if (match) return start
        }
        return -1
    }

    private interface SeekableTagSource : Closeable {
        val length: Long
        val position: Long
        fun seek(position: Long)
        fun read(bytes: ByteArray): Int
        fun readFully(bytes: ByteArray)
    }

    private class RandomAccessTagSource(
        private val file: RandomAccessFile
    ) : SeekableTagSource {
        override val length: Long get() = file.length()
        override val position: Long get() = file.filePointer

        override fun seek(position: Long) {
            file.seek(position.coerceIn(0L, length))
        }

        override fun read(bytes: ByteArray): Int = file.read(bytes)

        override fun readFully(bytes: ByteArray) {
            file.readFully(bytes)
        }

        override fun close() {
            file.close()
        }
    }

    private class ChannelTagSource(
        private val input: FileInputStream
    ) : SeekableTagSource {
        private val channel = input.channel
        override val length: Long get() = channel.size()
        override val position: Long get() = channel.position()

        override fun seek(position: Long) {
            channel.position(position.coerceIn(0L, length))
        }

        override fun read(bytes: ByteArray): Int = input.read(bytes)

        override fun readFully(bytes: ByteArray) {
            var offset = 0
            while (offset < bytes.size) {
                val read = input.read(bytes, offset, bytes.size - offset)
                if (read < 0) throw java.io.EOFException("Unexpected end of audio file")
                if (read == 0) continue
                offset += read
            }
        }

        override fun close() {
            input.close()
        }
    }

    private data class Mp4Box(val start: Long, val end: Long, val headerSize: Int, val type: Int) {
        val payloadStart: Long get() = start + headerSize
    }

    private fun fourCc(a: Int, b: Int, c: Int, d: Int): Int =
        ((a and 0xFF) shl 24) or ((b and 0xFF) shl 16) or ((c and 0xFF) shl 8) or (d and 0xFF)

    private fun ascii4(value: String): Int {
        val raw = value.padEnd(4).take(4)
        return fourCc(raw[0].code, raw[1].code, raw[2].code, raw[3].code)
    }

    private val MP4_MOOV = ascii4("moov")
    private val MP4_UDTA = ascii4("udta")
    private val MP4_META = ascii4("meta")
    private val MP4_ILST = ascii4("ilst")
    private val MP4_DATA = ascii4("data")
    private val MP4_FREEFORM = ascii4("----")
    private val MP4_NAME = ascii4("name")
    private val MP4_COMPOSER = fourCc(0xA9, 'w'.code, 'r'.code, 't'.code)
    private val MP4_COMMENT = fourCc(0xA9, 'c'.code, 'm'.code, 't'.code)
    private val MP4_COPYRIGHT = ascii4("cprt")
    private val MP4_COPYRIGHT_ALT = fourCc(0xA9, 'c'.code, 'p'.code, 'y'.code)

    private val MULTI_VALUE_KEYS = setOf("ARTIST", "COMPOSER", "LYRICIST", "GENRE")
    private val RESERVED_TAG_KEYS = setOf(
        "TITLE", "ARTIST", "ALBUM", "ALBUMARTIST", "ALBUM ARTIST", "GENRE", "DATE", "YEAR",
        "TRACKNUMBER", "TRACK", "DISCNUMBER", "DISC", "BITRATE", "SAMPLERATE", "CHANNELS",
        "DURATION_SEC", "FORMAT", "BITS_PER_SAMPLE", "ENCODER"
    )
}

internal fun LocalMediaEntity.withDeepTags(tags: LocalDeepTags): LocalMediaEntity = copy(
    composer = cleanMediaStoreText(tags.composer),
    lyricist = cleanMediaStoreText(tags.lyricist),
    comment = tags.comment.trim().take(4_096),
    copyright = cleanMediaStoreText(tags.copyright),
    customTags = tags.customTags.trim().take(24_000),
    fullTagSearchText = buildLocalFullTagSearchText(
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        displayName = displayName,
        folderName = folderName,
        composer = tags.composer,
        lyricist = tags.lyricist,
        comment = tags.comment,
        copyright = tags.copyright,
        customTags = tags.customTags
    )
)

internal fun LocalMediaEntity.withDeepTagsFrom(previous: LocalMediaEntity): LocalMediaEntity = copy(
    composer = previous.composer,
    lyricist = previous.lyricist,
    comment = previous.comment,
    copyright = previous.copyright,
    customTags = previous.customTags,
    fullTagSearchText = if (previous.fullTagSearchText.isNotBlank()) {
        buildLocalFullTagSearchText(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            discNumber = discNumber,
            displayName = displayName,
            folderName = folderName,
            composer = previous.composer,
            lyricist = previous.lyricist,
            comment = previous.comment,
            copyright = previous.copyright,
            customTags = previous.customTags
        )
    } else {
        buildLocalFullTagSearchText(
            title, artist, album, albumArtist, genre, year, trackNumber, discNumber,
            displayName, folderName, previous.composer, previous.lyricist,
            previous.comment, previous.copyright, previous.customTags
        )
    }
)

internal fun LocalMediaEntity.withTagEdits(edits: LocalTagEdits, deepTags: LocalDeepTags): LocalMediaEntity {
    val nextTitle = edits.title.trim().ifBlank { localTitleFallback("", displayName, mediaStoreId) }
    val nextArtist = cleanMediaStoreText(edits.artist)
    val nextAlbum = cleanMediaStoreText(edits.album)
    val nextAlbumArtist = cleanMediaStoreText(edits.albumArtist)
    val nextGenre = cleanMediaStoreText(edits.genre)
    val nextYear = edits.year.trim().toIntOrNull()?.takeIf { it in 1..9_999 } ?: 0
    val nextTrack = edits.trackNumber.trim().toIntOrNull()?.coerceIn(0, 9_999) ?: 0
    val nextDisc = edits.discNumber.trim().toIntOrNull()?.coerceIn(0, 999) ?: 0
    val nextComposer = cleanMediaStoreText(edits.composer)
    val nextLyricist = cleanMediaStoreText(edits.lyricist)
    val nextComment = edits.comment.trim().take(4_096)
    val nextCopyright = cleanMediaStoreText(edits.copyright)
    return copy(
        title = nextTitle,
        artist = nextArtist,
        album = nextAlbum,
        albumArtist = nextAlbumArtist,
        genre = nextGenre,
        year = nextYear,
        trackNumber = nextTrack,
        discNumber = nextDisc,
        albumKey = localAlbumKey(nextAlbum, nextAlbumArtist, folderKey),
        artistKey = localArtistKey(nextArtist),
        composer = nextComposer,
        lyricist = nextLyricist,
        comment = nextComment,
        copyright = nextCopyright,
        customTags = deepTags.customTags,
        fullTagSearchText = buildLocalFullTagSearchText(
            nextTitle, nextArtist, nextAlbum, nextAlbumArtist, nextGenre, nextYear, nextTrack, nextDisc,
            displayName, folderName, nextComposer, nextLyricist, nextComment, nextCopyright, deepTags.customTags
        ),
        contentFingerprint = localContentFingerprint(sizeBytes, durationMs, nextTitle)
    )
}

internal fun buildLocalFullTagSearchText(
    title: String,
    artist: String,
    album: String,
    albumArtist: String,
    genre: String,
    year: Int,
    trackNumber: Int,
    discNumber: Int,
    displayName: String,
    folderName: String,
    composer: String,
    lyricist: String,
    comment: String,
    copyright: String,
    customTags: String
): String = localGroupKey(
    listOf(
        title,
        artist,
        album,
        albumArtist,
        genre,
        year.takeIf { it > 0 }?.toString().orEmpty(),
        trackNumber.takeIf { it > 0 }?.toString().orEmpty(),
        discNumber.takeIf { it > 0 }?.toString().orEmpty(),
        displayName,
        folderName,
        composer,
        lyricist,
        comment,
        copyright,
        customTags
    ).joinToString(" ")
)

internal fun LocalMediaEntity.matchesFullTagQuery(query: String): Boolean {
    val clean = localGroupKey(query)
    return clean.isEmpty() || fullTagSearchText.contains(clean)
}

internal fun LocalMediaEntity.isSafeTagEditorFormat(): Boolean =
    LocalEmbeddedTagWriter.formatOf(mimeType, displayName) != LocalEditableTagFormat.Unsupported
