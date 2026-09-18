package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagEdits
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

internal enum class LocalEditableTagFormat { M4a, Mp3, Flac, Unsupported }

internal data class LocalEmbeddedTagWriteResult(
    val success: Boolean,
    val reason: String = ""
)

internal object LocalEmbeddedTagWriter {
    const val MAX_INPUT_BYTES: Long = LevyraM4aTagWriter.MAX_INPUT_BYTES

    fun formatOf(mimeType: String, displayName: String): LocalEditableTagFormat {
        val mime = mimeType.lowercase(Locale.ROOT)
        val name = displayName.lowercase(Locale.ROOT)
        return when {
            mime.contains("mp4") || mime.contains("m4a") || name.endsWith(".m4a") || name.endsWith(".mp4") ->
                LocalEditableTagFormat.M4a
            mime.contains("mpeg") || mime.contains("mp3") || name.endsWith(".mp3") ->
                LocalEditableTagFormat.Mp3
            mime.contains("flac") || name.endsWith(".flac") ->
                LocalEditableTagFormat.Flac
            else -> LocalEditableTagFormat.Unsupported
        }
    }

    fun write(
        input: File,
        output: File,
        format: LocalEditableTagFormat,
        edits: LocalTagEdits
    ): LocalEmbeddedTagWriteResult {
        if (!input.isFile || input.length() <= 0L) return LocalEmbeddedTagWriteResult(false, "input_missing")
        if (input.length() > MAX_INPUT_BYTES) return LocalEmbeddedTagWriteResult(false, "input_too_large")
        return when (format) {
            LocalEditableTagFormat.M4a -> {
                val result = LevyraM4aTagWriter.writeTags(
                    input = input,
                    output = output,
                    edits = LevyraM4aTagEdits(
                        title = edits.title.trim(),
                        artist = edits.artist.trim(),
                        album = edits.album.trim(),
                        albumArtist = edits.albumArtist.trim(),
                        genre = edits.genre.trim(),
                        year = edits.year.trim(),
                        trackNumber = edits.trackNumber.trim().toIntOrNull()?.coerceIn(0, 9_999) ?: 0,
                        discNumber = edits.discNumber.trim().toIntOrNull()?.coerceIn(0, 999) ?: 0,
                        composer = edits.composer.trim(),
                        lyricist = edits.lyricist.trim(),
                        comment = edits.comment.trim(),
                        copyright = edits.copyright.trim()
                    )
                )
                LocalEmbeddedTagWriteResult(result.success, result.reason)
            }
            LocalEditableTagFormat.Mp3 -> writeMp3(input, output, edits)
            LocalEditableTagFormat.Flac -> writeFlac(input, output, edits)
            LocalEditableTagFormat.Unsupported -> LocalEmbeddedTagWriteResult(false, "unsupported_format")
        }
    }

    private fun writeMp3(input: File, output: File, edits: LocalTagEdits): LocalEmbeddedTagWriteResult {
        val source = runCatching { input.readBytes() }.getOrElse {
            return LocalEmbeddedTagWriteResult(false, "read_failed")
        }
        if (source.size < 4) return LocalEmbeddedTagWriteResult(false, "invalid_mp3")

        val hasId3 = source.size >= 10 && source[0] == 'I'.code.toByte() &&
            source[1] == 'D'.code.toByte() && source[2] == '3'.code.toByte()
        val version = if (hasId3) source[3].toInt() and 0xFF else 4
        if (version !in 3..4) return LocalEmbeddedTagWriteResult(false, "unsupported_id3_version")
        val flags = if (hasId3) source[5].toInt() and 0xFF else 0
        if ((flags and 0xD0) != 0) return LocalEmbeddedTagWriteResult(false, "unsupported_id3_flags")

        val oldTagEnd = if (hasId3) {
            val payloadSize = syncSafeInt(source, 6)
            (10L + payloadSize.toLong()).takeIf { it <= source.size }?.toInt()
                ?: return LocalEmbeddedTagWriteResult(false, "invalid_id3")
        } else {
            0
        }

        val preserved = if (hasId3) {
            parsePreservedId3Frames(source, 10, oldTagEnd, version)
                ?: return LocalEmbeddedTagWriteResult(false, "invalid_id3")
        } else {
            emptyList()
        }

        val frames = ByteArrayOutputStream()
        preserved.forEach { frames.write(it) }
        buildId3EditFrames(edits, version).forEach { frames.write(it) }
        val frameBytes = frames.toByteArray()
        val tag = ByteArrayOutputStream(frameBytes.size + 10).apply {
            write("ID3".toByteArray(StandardCharsets.ISO_8859_1))
            write(version)
            write(0)
            write(0)
            write(syncSafe(frameBytes.size))
            write(frameBytes)
        }.toByteArray()

        val audioEnd = if (hasId3v1(source)) source.size - 128 else source.size
        val body = source.copyOfRange(oldTagEnd, audioEnd)
        val id3v1 = if (hasId3v1(source)) rewriteId3v1(source.copyOfRange(audioEnd, source.size), edits) else ByteArray(0)
        output.parentFile?.mkdirs()
        output.writeBytes(tag + body + id3v1)
        return LocalEmbeddedTagWriteResult(output.isFile && output.length() > 0L, "ok")
    }

    private fun parsePreservedId3Frames(
        source: ByteArray,
        start: Int,
        end: Int,
        version: Int
    ): List<ByteArray>? {
        val preserved = ArrayList<ByteArray>()
        var cursor = start
        while (cursor + 10 <= end) {
            if (source.copyOfRange(cursor, minOf(cursor + 4, end)).all { it == 0.toByte() }) break
            val id = String(source, cursor, 4, StandardCharsets.ISO_8859_1)
            if (!validFrameId(id)) break
            val frameSize = if (version == 4) syncSafeInt(source, cursor + 4) else int32be(source, cursor + 4)
            if (frameSize < 0 || cursor + 10L + frameSize > end) return null
            val frameEnd = cursor + 10 + frameSize
            if (id !in EDITED_ID3_FRAMES) preserved += source.copyOfRange(cursor, frameEnd)
            cursor = frameEnd
        }
        return preserved
    }

    private fun buildId3EditFrames(edits: LocalTagEdits, version: Int): List<ByteArray> {
        val frames = ArrayList<ByteArray>()
        fun text(id: String, value: String) {
            value.cleanTag()?.let { frames += id3TextFrame(id, it, version) }
        }
        text("TIT2", edits.title)
        text("TPE1", edits.artist)
        text("TALB", edits.album)
        text("TPE2", edits.albumArtist)
        text("TCON", edits.genre)
        text(if (version == 4) "TDRC" else "TYER", edits.year)
        text("TRCK", edits.trackNumber)
        text("TPOS", edits.discNumber)
        text("TCOM", edits.composer)
        text("TEXT", edits.lyricist)
        text("TCOP", edits.copyright)
        edits.comment.cleanMultilineTag()?.let { frames += id3CommentFrame(it, version) }
        return frames
    }

    private fun id3TextFrame(id: String, value: String, version: Int): ByteArray {
        val encoding = if (version == 4) 3 else 1
        val charset = if (version == 4) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
        return id3Frame(id, byteArrayOf(encoding.toByte()) + value.toByteArray(charset), version)
    }

    private fun id3CommentFrame(value: String, version: Int): ByteArray {
        val encoding = if (version == 4) 3 else 1
        val charset = if (version == 4) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
        val terminator = if (version == 4) byteArrayOf(0) else byteArrayOf(0, 0)
        val payload = byteArrayOf(encoding.toByte()) +
            "eng".toByteArray(StandardCharsets.ISO_8859_1) +
            terminator +
            value.toByteArray(charset)
        return id3Frame("COMM", payload, version)
    }

    private fun id3Frame(id: String, payload: ByteArray, version: Int): ByteArray = ByteArrayOutputStream().apply {
        write(id.toByteArray(StandardCharsets.ISO_8859_1))
        write(if (version == 4) syncSafe(payload.size) else int32(payload.size))
        write(byteArrayOf(0, 0))
        write(payload)
    }.toByteArray()

    private fun hasId3v1(bytes: ByteArray): Boolean {
        val start = bytes.size - 128
        return start >= 0 &&
            bytes[start] == 'T'.code.toByte() &&
            bytes[start + 1] == 'A'.code.toByte() &&
            bytes[start + 2] == 'G'.code.toByte()
    }

    private fun rewriteId3v1(original: ByteArray, edits: LocalTagEdits): ByteArray {
        if (original.size != 128) return original
        val out = original.copyOf()
        writeFixedLatin1(out, 3, 30, edits.title)
        writeFixedLatin1(out, 33, 30, edits.artist)
        writeFixedLatin1(out, 63, 30, edits.album)
        writeFixedLatin1(out, 93, 4, edits.year)
        val track = edits.trackNumber.trim().toIntOrNull()?.coerceIn(0, 255) ?: 0
        if (track > 0) {
            writeFixedLatin1(out, 97, 28, edits.comment)
            out[125] = 0
            out[126] = track.toByte()
        } else {
            writeFixedLatin1(out, 97, 30, edits.comment)
        }
        return out
    }

    private fun writeFixedLatin1(target: ByteArray, offset: Int, length: Int, value: String) {
        java.util.Arrays.fill(target, offset, offset + length, 0.toByte())
        val raw = value.trim().toByteArray(StandardCharsets.ISO_8859_1)
        System.arraycopy(raw, 0, target, offset, minOf(raw.size, length))
    }

    private fun writeFlac(input: File, output: File, edits: LocalTagEdits): LocalEmbeddedTagWriteResult {
        val source = runCatching { input.readBytes() }.getOrElse {
            return LocalEmbeddedTagWriteResult(false, "read_failed")
        }
        if (source.size < 8 || String(source, 0, 4, StandardCharsets.ISO_8859_1) != "fLaC") {
            return LocalEmbeddedTagWriteResult(false, "invalid_flac")
        }

        val blocks = ArrayList<FlacBlock>()
        var cursor = 4
        var last = false
        while (!last) {
            if (cursor + 4 > source.size) return LocalEmbeddedTagWriteResult(false, "invalid_flac")
            val header = source[cursor].toInt() and 0xFF
            last = (header and 0x80) != 0
            val type = header and 0x7F
            val size = ((source[cursor + 1].toInt() and 0xFF) shl 16) or
                ((source[cursor + 2].toInt() and 0xFF) shl 8) or
                (source[cursor + 3].toInt() and 0xFF)
            val payloadStart = cursor + 4
            val end = payloadStart + size
            if (end > source.size) return LocalEmbeddedTagWriteResult(false, "invalid_flac")
            blocks += FlacBlock(type, source.copyOfRange(payloadStart, end))
            cursor = end
            if (blocks.size > 256) return LocalEmbeddedTagWriteResult(false, "invalid_flac")
        }
        if (blocks.firstOrNull()?.type != 0) return LocalEmbeddedTagWriteResult(false, "streaminfo_missing")

        var replacedComment = false
        val rewritten = ArrayList<FlacBlock>(blocks.size + 1)
        blocks.forEach { block ->
            if (block.type == 4) {
                if (!replacedComment) {
                    val payload = rewriteVorbisComment(block.payload, edits)
                        ?: return LocalEmbeddedTagWriteResult(false, "invalid_vorbis_comment")
                    rewritten += FlacBlock(4, payload)
                    replacedComment = true
                }
            } else {
                rewritten += block
            }
        }
        if (!replacedComment) rewritten += FlacBlock(4, createVorbisComment(edits))

        val out = ByteArrayOutputStream(source.size + 4096)
        out.write("fLaC".toByteArray(StandardCharsets.ISO_8859_1))
        rewritten.forEachIndexed { index, block ->
            if (block.payload.size > 0xFF_FFFF) return LocalEmbeddedTagWriteResult(false, "flac_metadata_too_large")
            val header = block.type or if (index == rewritten.lastIndex) 0x80 else 0
            out.write(header)
            out.write((block.payload.size ushr 16) and 0xFF)
            out.write((block.payload.size ushr 8) and 0xFF)
            out.write(block.payload.size and 0xFF)
            out.write(block.payload)
        }
        out.write(source, cursor, source.size - cursor)
        output.parentFile?.mkdirs()
        output.writeBytes(out.toByteArray())
        return LocalEmbeddedTagWriteResult(output.isFile && output.length() > 0L, "ok")
    }

    private fun rewriteVorbisComment(payload: ByteArray, edits: LocalTagEdits): ByteArray? {
        var cursor = 0
        if (payload.size < 8) return null
        val vendorSize = int32le(payload, cursor)
        cursor += 4
        if (vendorSize < 0 || cursor + vendorSize > payload.size) return null
        val vendor = payload.copyOfRange(cursor, cursor + vendorSize)
        cursor += vendorSize
        if (cursor + 4 > payload.size) return null
        val count = int32le(payload, cursor)
        cursor += 4
        if (count < 0 || count > 50_000) return null

        val kept = ArrayList<String>()
        repeat(count) {
            if (cursor + 4 > payload.size) return null
            val size = int32le(payload, cursor)
            cursor += 4
            if (size < 0 || cursor + size > payload.size) return null
            val entry = String(payload, cursor, size, StandardCharsets.UTF_8)
            cursor += size
            val key = entry.substringBefore('=', "").trim().uppercase(Locale.ROOT)
            if (key !in EDITED_VORBIS_KEYS) kept += entry
        }
        kept += vorbisEditEntries(edits)
        return encodeVorbisComment(vendor, kept)
    }

    private fun createVorbisComment(edits: LocalTagEdits): ByteArray =
        encodeVorbisComment("Levyra".toByteArray(StandardCharsets.UTF_8), vorbisEditEntries(edits))

    private fun vorbisEditEntries(edits: LocalTagEdits): List<String> = buildList {
        fun addValue(key: String, raw: String) {
            raw.cleanTag()?.let { add("$key=$it") }
        }
        addValue("TITLE", edits.title)
        addValue("ARTIST", edits.artist)
        addValue("ALBUM", edits.album)
        addValue("ALBUMARTIST", edits.albumArtist)
        addValue("GENRE", edits.genre)
        addValue("DATE", edits.year)
        addValue("TRACKNUMBER", edits.trackNumber)
        addValue("DISCNUMBER", edits.discNumber)
        addValue("COMPOSER", edits.composer)
        addValue("LYRICIST", edits.lyricist)
        addValue("COMMENT", edits.comment)
        addValue("COPYRIGHT", edits.copyright)
    }

    private fun encodeVorbisComment(vendor: ByteArray, entries: List<String>): ByteArray = ByteArrayOutputStream().apply {
        write(int32le(vendor.size))
        write(vendor)
        write(int32le(entries.size))
        entries.forEach { entry ->
            val raw = entry.toByteArray(StandardCharsets.UTF_8)
            write(int32le(raw.size))
            write(raw)
        }
    }.toByteArray()

    private fun String.cleanTag(maxLength: Int = 4_096): String? {
        val value = trim().replace(Regex("[ \\t]+"), " ").take(maxLength).trim()
        return value.takeIf { it.isNotBlank() }
    }

    private fun String.cleanMultilineTag(): String? {
        val value = lineSequence()
            .map { it.trim().replace(Regex("[ \\t]+"), " ") }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .take(4_096)
            .trim()
        return value.takeIf { it.isNotBlank() }
    }

    private fun validFrameId(id: String): Boolean =
        id.length == 4 && id.all { it in 'A'..'Z' || it in '0'..'9' }

    private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private fun int32be(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun int32le(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun int32(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte()
    )

    private fun int32le(value: Int): ByteArray = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte()
    )

    private data class FlacBlock(val type: Int, val payload: ByteArray)

    private val EDITED_ID3_FRAMES = setOf(
        "TIT2", "TPE1", "TALB", "TPE2", "TCON", "TDRC", "TYER", "TRCK", "TPOS",
        "TCOM", "TEXT", "TCOP", "COMM"
    )

    private val EDITED_VORBIS_KEYS = setOf(
        "TITLE", "ARTIST", "ALBUM", "ALBUMARTIST", "ALBUM ARTIST", "GENRE", "DATE", "YEAR",
        "TRACKNUMBER", "TRACK", "DISCNUMBER", "DISC", "COMPOSER", "LYRICIST", "WRITER",
        "SONGWRITER", "COMMENT", "DESCRIPTION", "COPYRIGHT"
    )
}
