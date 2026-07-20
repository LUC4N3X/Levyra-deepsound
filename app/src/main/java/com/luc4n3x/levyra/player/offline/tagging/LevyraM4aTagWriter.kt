package com.luc4n3x.levyra.player.offline.tagging

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.math.max

object LevyraM4aTagWriter {
    const val MAX_INPUT_BYTES: Long = 128L * 1024L * 1024L
    val isAvailable: Boolean = true

    fun write(input: File, output: File, metadata: LevyraM4aMetadata): LevyraM4aTagResult {
        if (!input.exists() || !input.isFile) return LevyraM4aTagResult(false, false, "input_missing")
        if (input.length() <= 0L) return LevyraM4aTagResult(false, false, "input_empty")
        if (input.length() > MAX_INPUT_BYTES) return LevyraM4aTagResult(false, false, "input_too_large")
        val source = input.readBytes()
        val boxes = runCatching { parseBoxes(source, 0, source.size) }.getOrElse {
            return LevyraM4aTagResult(false, false, "invalid_mp4")
        }
        val moov = boxes.firstOrNull { it.type == Atom.MOOV } ?: return LevyraM4aTagResult(false, false, "moov_missing")
        val metadataItems = buildMetadataItems(metadata)
        if (metadataItems.isEmpty()) return LevyraM4aTagResult(false, false, "metadata_empty")
        val firstMdat = boxes.firstOrNull { it.type == Atom.MDAT }
        val initialMoov = runCatching { rebuildMoov(source, moov, metadataItems, 0L) }.getOrElse {
            return LevyraM4aTagResult(false, false, "moov_rebuild_failed")
        }
        val offsetDelta = if (firstMdat != null && moov.start < firstMdat.start) {
            initialMoov.size.toLong() - moov.length.toLong()
        } else {
            0L
        }
        val finalMoov = runCatching { rebuildMoov(source, moov, metadataItems, offsetDelta) }.getOrElse {
            return LevyraM4aTagResult(false, false, "offset_patch_failed")
        }
        val out = ByteArrayOutputStream(source.size + max(0, finalMoov.size - moov.length))
        for (box in boxes) {
            if (box.type == Atom.MOOV) {
                out.write(finalMoov)
            } else {
                out.write(source, box.start, box.length)
            }
        }
        output.parentFile?.mkdirs()
        output.writeBytes(out.toByteArray())
        return LevyraM4aTagResult(output.exists() && output.length() > 0L, metadata.artworkData?.let(::artworkType) != null, "ok")
    }

    private fun rebuildMoov(source: ByteArray, moov: Mp4Box, metadataItems: List<ByteArray>, offsetDelta: Long): ByteArray {
        val children = parseBoxes(source, moov.payloadStart, moov.end)
        val payload = ByteArrayOutputStream(moov.length + 4096)
        var hasUdta = false
        for (child in children) {
            if (child.type == Atom.UDTA) {
                payload.write(rebuildUdta(source, child, metadataItems, offsetDelta))
                hasUdta = true
            } else {
                payload.write(rebuildWithOffsetPatch(source, child, offsetDelta))
            }
        }
        if (!hasUdta) payload.write(atom(Atom.UDTA, createMeta(metadataItems)))
        return atom(Atom.MOOV, payload.toByteArray())
    }

    private fun rebuildUdta(source: ByteArray, udta: Mp4Box, metadataItems: List<ByteArray>, offsetDelta: Long): ByteArray {
        val children = parseBoxes(source, udta.payloadStart, udta.end)
        val payload = ByteArrayOutputStream(udta.length + 4096)
        var hasMeta = false
        for (child in children) {
            if (child.type == Atom.META) {
                payload.write(rebuildMeta(source, child, metadataItems, offsetDelta))
                hasMeta = true
            } else {
                payload.write(rebuildWithOffsetPatch(source, child, offsetDelta))
            }
        }
        if (!hasMeta) payload.write(createMeta(metadataItems))
        return atom(Atom.UDTA, payload.toByteArray())
    }

    private fun rebuildMeta(source: ByteArray, meta: Mp4Box, metadataItems: List<ByteArray>, offsetDelta: Long): ByteArray {
        val payloadStart = meta.payloadStart
        val prefixEnd = payloadStart + 4
        val fullBoxHeader = if (prefixEnd <= meta.end) source.copyOfRange(payloadStart, prefixEnd) else ByteArray(4)
        val childrenStart = prefixEnd.coerceAtMost(meta.end)
        val children = parseBoxes(source, childrenStart, meta.end)
        val payload = ByteArrayOutputStream(meta.length + 4096)
        payload.write(fullBoxHeader)
        var hasHdlr = false
        var hasIlst = false
        if (children.none { it.type == Atom.HDLR }) payload.write(createHdlr())
        for (child in children) {
            when (child.type) {
                Atom.HDLR -> {
                    payload.write(source, child.start, child.length)
                    hasHdlr = true
                }
                Atom.ILST -> {
                    payload.write(rebuildIlst(source, child, metadataItems))
                    hasIlst = true
                }
                else -> payload.write(rebuildWithOffsetPatch(source, child, offsetDelta))
            }
        }
        if (!hasHdlr && children.isEmpty()) Unit
        if (!hasIlst) payload.write(createIlst(metadataItems))
        return atom(Atom.META, payload.toByteArray())
    }

    private fun rebuildIlst(source: ByteArray, ilst: Mp4Box, metadataItems: List<ByteArray>): ByteArray {
        val payload = ByteArrayOutputStream(ilst.length + 4096)
        val children = runCatching { parseBoxes(source, ilst.payloadStart, ilst.end) }.getOrDefault(emptyList())
        for (child in children) {
            if (!shouldReplaceMetadataItem(source, child)) payload.write(source, child.start, child.length)
        }
        for (item in metadataItems) payload.write(item)
        return atom(Atom.ILST, payload.toByteArray())
    }

    private fun rebuildWithOffsetPatch(source: ByteArray, box: Mp4Box, offsetDelta: Long): ByteArray {
        if (offsetDelta == 0L) return source.copyOfRange(box.start, box.end)
        return when (box.type) {
            Atom.STCO -> patchStco(source, box, offsetDelta)
            Atom.CO64 -> patchCo64(source, box, offsetDelta)
            Atom.META -> rebuildMetaPreservingOnly(source, box, offsetDelta)
            in Atom.CONTAINERS -> rebuildContainer(source, box, offsetDelta)
            else -> source.copyOfRange(box.start, box.end)
        }
    }

    private fun rebuildContainer(source: ByteArray, box: Mp4Box, offsetDelta: Long): ByteArray {
        val children = parseBoxes(source, box.payloadStart, box.end)
        val payload = ByteArrayOutputStream(box.length)
        for (child in children) payload.write(rebuildWithOffsetPatch(source, child, offsetDelta))
        return atom(box.type, payload.toByteArray())
    }

    private fun rebuildMetaPreservingOnly(source: ByteArray, box: Mp4Box, offsetDelta: Long): ByteArray {
        val payloadStart = box.payloadStart
        val prefixEnd = payloadStart + 4
        if (prefixEnd > box.end) return source.copyOfRange(box.start, box.end)
        val children = parseBoxes(source, prefixEnd, box.end)
        val payload = ByteArrayOutputStream(box.length)
        payload.write(source, payloadStart, 4)
        for (child in children) payload.write(rebuildWithOffsetPatch(source, child, offsetDelta))
        return atom(Atom.META, payload.toByteArray())
    }

    private fun patchStco(source: ByteArray, box: Mp4Box, offsetDelta: Long): ByteArray {
        val payloadStart = box.payloadStart
        if (payloadStart + 8 > box.end) return source.copyOfRange(box.start, box.end)
        val count = readUInt32(source, payloadStart + 4)
        val expected = payloadStart + 8L + count * 4L
        if (expected > box.end) return source.copyOfRange(box.start, box.end)
        val payload = ByteArrayOutputStream(box.length)
        payload.write(source, payloadStart, 8)
        var pos = payloadStart + 8
        repeat(count.toInt()) {
            val oldOffset = readUInt32(source, pos)
            val next = oldOffset + offsetDelta
            if (next < 0L || next > 0xFFFF_FFFFL) throw IOException("stco_offset_out_of_range")
            writeUInt32(payload, next)
            pos += 4
        }
        if (pos < box.end) payload.write(source, pos, box.end - pos)
        return atom(Atom.STCO, payload.toByteArray())
    }

    private fun patchCo64(source: ByteArray, box: Mp4Box, offsetDelta: Long): ByteArray {
        val payloadStart = box.payloadStart
        if (payloadStart + 8 > box.end) return source.copyOfRange(box.start, box.end)
        val count = readUInt32(source, payloadStart + 4)
        val expected = payloadStart + 8L + count * 8L
        if (expected > box.end) return source.copyOfRange(box.start, box.end)
        val payload = ByteArrayOutputStream(box.length)
        payload.write(source, payloadStart, 8)
        var pos = payloadStart + 8
        repeat(count.toInt()) {
            val oldOffset = readUInt64(source, pos)
            val next = oldOffset + offsetDelta
            if (next < 0L) throw IOException("co64_offset_out_of_range")
            writeUInt64(payload, next)
            pos += 8
        }
        if (pos < box.end) payload.write(source, pos, box.end - pos)
        return atom(Atom.CO64, payload.toByteArray())
    }

    private fun createMeta(metadataItems: List<ByteArray>): ByteArray {
        val payload = ByteArrayOutputStream(4096)
        payload.write(ByteArray(4))
        payload.write(createHdlr())
        payload.write(createIlst(metadataItems))
        return atom(Atom.META, payload.toByteArray())
    }

    private fun createHdlr(): ByteArray {
        val payload = ByteArrayOutputStream(33)
        payload.write(ByteArray(4))
        writeUInt32(payload, 0)
        payload.write(Atom.bytes(Atom.MDIR))
        payload.write(ByteArray(12))
        payload.write("appl".toByteArray(StandardCharsets.US_ASCII))
        payload.write(0)
        return atom(Atom.HDLR, payload.toByteArray())
    }

    private fun createIlst(metadataItems: List<ByteArray>): ByteArray {
        val payload = ByteArrayOutputStream(4096)
        for (item in metadataItems) payload.write(item)
        return atom(Atom.ILST, payload.toByteArray())
    }

    private fun buildMetadataItems(metadata: LevyraM4aMetadata): List<ByteArray> {
        val items = ArrayList<ByteArray>()
        metadata.title.cleanTag()?.let { items += textItem(Atom.NAM, it) }
        metadata.artist.cleanTag()?.let { items += textItem(Atom.ART, it) }
        metadata.album.cleanTag()?.let { items += textItem(Atom.ALB, it) }
        metadata.albumArtist.cleanTag()?.let { items += textItem(Atom.AART, it) }
        metadata.releaseDate.ifBlank { metadata.year }.cleanTag()?.let { items += textItem(Atom.DAY, it) }
        metadata.genres.joinToString("; ").cleanTag()?.let { items += textItem(Atom.GENRE, it) }
        metadata.lyrics.cleanMultilineTag()?.let { items += textItem(Atom.LYRICS, it) }
        metadata.encodedBy.cleanTag()?.let { items += textItem(Atom.ENCODER, it) }
        if (metadata.trackNumber > 0) items += pairItem(Atom.TRACK_NUMBER, metadata.trackNumber, metadata.trackTotal, trailingReserved = true)
        if (metadata.discNumber > 0) items += pairItem(Atom.DISC_NUMBER, metadata.discNumber, metadata.discTotal, trailingReserved = false)
        if (metadata.explicit) items += binaryItem(Atom.ADVISORY, byteArrayOf(1), DATA_SIGNED_INTEGER)
        metadata.isrc.cleanTag()?.let { items += freeformItem(FREEFORM_ISRC, it) }
        metadata.upc.cleanTag()?.let { items += freeformItem(FREEFORM_UPC, it) }
        metadata.sourceUrl.cleanTag(4096)?.let { items += freeformItem(FREEFORM_SOURCE_URL, it) }
        metadata.sourceProvider.cleanTag()?.let { items += freeformItem(FREEFORM_SOURCE_PROVIDER, it) }
        metadata.metadataProvider.cleanTag()?.let { items += freeformItem(FREEFORM_METADATA_PROVIDER, it) }
        metadata.metadataConfidence.takeIf { it > 0 }?.let { items += freeformItem(FREEFORM_METADATA_CONFIDENCE, it.toString()) }
        metadata.trackId.cleanTag()?.let { items += freeformItem(FREEFORM_TRACK_ID, it) }
        metadata.albumId.cleanTag()?.let { items += freeformItem(FREEFORM_ALBUM_ID, it) }
        metadata.artistIds.joinToString(",").cleanTag(4096)?.let { items += freeformItem(FREEFORM_ARTIST_IDS, it) }
        metadata.albumUrl.cleanTag(4096)?.let { items += freeformItem(FREEFORM_ALBUM_URL, it) }
        metadata.counterpartId.cleanTag()?.let { items += freeformItem(FREEFORM_COUNTERPART_ID, it) }
        metadata.mediaType.cleanTag()?.let { items += freeformItem(FREEFORM_MEDIA_TYPE, it) }
        val imageType = metadata.artworkData?.let(::artworkType)
        if (metadata.artworkData != null && imageType != null) items += binaryItem(Atom.COVR, metadata.artworkData, imageType)
        return items
    }

    private fun shouldReplaceMetadataItem(source: ByteArray, item: Mp4Box): Boolean {
        if (item.type in Atom.REPLACED_TAGS) return true
        if (item.type != Atom.FREEFORM) return false
        return freeformName(source, item) in LEVYRA_FREEFORM_NAMES
    }

    private fun freeformName(source: ByteArray, item: Mp4Box): String? {
        val children = runCatching { parseBoxes(source, item.payloadStart, item.end) }.getOrDefault(emptyList())
        val name = children.firstOrNull { it.type == Atom.NAME } ?: return null
        val start = (name.payloadStart + 4).coerceAtMost(name.end)
        if (start >= name.end) return null
        return String(source, start, name.end - start, StandardCharsets.UTF_8).trim()
    }

    private fun textItem(type: Int, text: String): ByteArray {
        val value = text.toByteArray(StandardCharsets.UTF_8)
        return dataItem(type, value, DATA_UTF8)
    }

    private fun pairItem(type: Int, number: Int, total: Int, trailingReserved: Boolean): ByteArray {
        val value = ByteArrayOutputStream(if (trailingReserved) 8 else 6)
        writeUInt16(value, 0)
        writeUInt16(value, number.coerceIn(0, 0xFFFF))
        writeUInt16(value, total.coerceIn(0, 0xFFFF))
        if (trailingReserved) writeUInt16(value, 0)
        return dataItem(type, value.toByteArray(), DATA_IMPLICIT)
    }

    private fun freeformItem(name: String, value: String): ByteArray {
        val payload = ByteArrayOutputStream(value.length + 64)
        payload.write(fullBoxTextAtom(Atom.MEAN, FREEFORM_MEAN))
        payload.write(fullBoxTextAtom(Atom.NAME, name))
        payload.write(dataItemPayload(value.toByteArray(StandardCharsets.UTF_8), DATA_UTF8))
        return atom(Atom.FREEFORM, payload.toByteArray())
    }

    private fun fullBoxTextAtom(type: Int, value: String): ByteArray {
        val payload = ByteArrayOutputStream(value.length + 4)
        payload.write(ByteArray(4))
        payload.write(value.toByteArray(StandardCharsets.UTF_8))
        return atom(type, payload.toByteArray())
    }

    private fun dataItemPayload(value: ByteArray, dataType: Int): ByteArray {
        val dataPayload = ByteArrayOutputStream(value.size + 8)
        writeUInt32(dataPayload, dataType.toLong())
        writeUInt32(dataPayload, 0)
        dataPayload.write(value)
        return atom(Atom.DATA, dataPayload.toByteArray())
    }

    private fun binaryItem(type: Int, bytes: ByteArray, dataType: Int): ByteArray = dataItem(type, bytes, dataType)

    private fun dataItem(type: Int, value: ByteArray, dataType: Int): ByteArray =
        atom(type, dataItemPayload(value, dataType))

    private fun artworkType(bytes: ByteArray): Int? {
        if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return DATA_JPEG
        if (bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) return DATA_PNG
        return null
    }

    private fun parseBoxes(source: ByteArray, start: Int, end: Int): List<Mp4Box> {
        if (start < 0 || end < start || end > source.size) throw IOException("invalid_range")
        val boxes = ArrayList<Mp4Box>()
        var pos = start
        while (pos + 8 <= end) {
            val smallSize = readUInt32(source, pos)
            val type = readInt32(source, pos + 4)
            var header = 8
            val size = when (smallSize) {
                0L -> (end - pos).toLong()
                1L -> {
                    if (pos + 16 > end) throw IOException("invalid_large_box")
                    header = 16
                    readUInt64(source, pos + 8)
                }
                else -> smallSize
            }
            if (size < header || size > Int.MAX_VALUE || pos + size > end) throw IOException("invalid_box_size")
            boxes += Mp4Box(pos, (pos + size).toInt(), header, type)
            pos += size.toInt()
            if (smallSize == 0L) break
        }
        if (pos != end && !source.copyOfRange(pos, end).all { it == 0.toByte() }) throw IOException("unparsed_bytes")
        return boxes
    }

    private fun atom(type: Int, payload: ByteArray): ByteArray {
        val size = payload.size + 8L
        if (size > 0xFFFF_FFFFL) throw IOException("atom_too_large")
        val out = ByteArrayOutputStream(size.toInt())
        writeUInt32(out, size)
        out.write(Atom.bytes(type))
        out.write(payload)
        return out.toByteArray()
    }

    private fun String.cleanTag(maxLength: Int = 1024): String? {
        val value = trim().replace(Regex("\\s+"), " ")
        if (value.isBlank()) return null
        return value.take(maxLength)
    }

    private fun String.cleanMultilineTag(): String? {
        val value = lineSequence()
            .map { it.trim().replace(Regex("[ \t]+"), " ") }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
        if (value.isBlank()) return null
        return value.take(MAX_LYRICS_CHARS)
    }

    private fun readInt32(source: ByteArray, offset: Int): Int {
        return ((source[offset].toInt() and 0xFF) shl 24) or
            ((source[offset + 1].toInt() and 0xFF) shl 16) or
            ((source[offset + 2].toInt() and 0xFF) shl 8) or
            (source[offset + 3].toInt() and 0xFF)
    }

    private fun readUInt32(source: ByteArray, offset: Int): Long = readInt32(source, offset).toLong() and 0xFFFF_FFFFL

    private fun readUInt64(source: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (source[offset + i].toLong() and 0xFFL)
        return value
    }

    private fun writeUInt16(out: ByteArrayOutputStream, value: Int) {
        out.write((value ushr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeUInt32(out: ByteArrayOutputStream, value: Long) {
        out.write(((value ushr 24) and 0xFF).toInt())
        out.write(((value ushr 16) and 0xFF).toInt())
        out.write(((value ushr 8) and 0xFF).toInt())
        out.write((value and 0xFF).toInt())
    }

    private fun writeUInt64(out: ByteArrayOutputStream, value: Long) {
        for (shift in 56 downTo 0 step 8) out.write(((value ushr shift) and 0xFF).toInt())
    }

    private const val DATA_IMPLICIT = 0
    private const val DATA_UTF8 = 1
    private const val DATA_JPEG = 13
    private const val DATA_SIGNED_INTEGER = 21
    private const val DATA_PNG = 14
    private const val MAX_LYRICS_CHARS = 128 * 1024
    private const val FREEFORM_MEAN = "com.luc4n3x.levyra"
    private const val FREEFORM_ISRC = "ISRC"
    private const val FREEFORM_UPC = "UPC"
    private const val FREEFORM_SOURCE_URL = "SOURCE_URL"
    private const val FREEFORM_SOURCE_PROVIDER = "SOURCE_PROVIDER"
    private const val FREEFORM_METADATA_PROVIDER = "METADATA_PROVIDER"
    private const val FREEFORM_METADATA_CONFIDENCE = "METADATA_CONFIDENCE"
    private const val FREEFORM_TRACK_ID = "TRACK_ID"
    private const val FREEFORM_ALBUM_ID = "ALBUM_ID"
    private const val FREEFORM_ARTIST_IDS = "ARTIST_IDS"
    private const val FREEFORM_ALBUM_URL = "ALBUM_URL"
    private const val FREEFORM_COUNTERPART_ID = "COUNTERPART_ID"
    private const val FREEFORM_MEDIA_TYPE = "MEDIA_TYPE"
    private val LEVYRA_FREEFORM_NAMES = setOf(
        FREEFORM_ISRC,
        FREEFORM_UPC,
        FREEFORM_SOURCE_URL,
        FREEFORM_SOURCE_PROVIDER,
        FREEFORM_METADATA_PROVIDER,
        FREEFORM_METADATA_CONFIDENCE,
        FREEFORM_TRACK_ID,
        FREEFORM_ALBUM_ID,
        FREEFORM_ARTIST_IDS,
        FREEFORM_ALBUM_URL,
        FREEFORM_COUNTERPART_ID,
        FREEFORM_MEDIA_TYPE
    )
}

data class LevyraM4aMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = artist,
    val year: String = "",
    val releaseDate: String = "",
    val genres: List<String> = emptyList(),
    val trackNumber: Int = 0,
    val trackTotal: Int = 0,
    val discNumber: Int = 0,
    val discTotal: Int = 0,
    val lyrics: String = "",
    val explicit: Boolean = false,
    val isrc: String = "",
    val upc: String = "",
    val sourceUrl: String = "",
    val sourceProvider: String = "",
    val metadataProvider: String = "",
    val metadataConfidence: Int = 0,
    val trackId: String = "",
    val albumId: String = "",
    val artistIds: List<String> = emptyList(),
    val albumUrl: String = "",
    val counterpartId: String = "",
    val mediaType: String = "",
    val encodedBy: String = "Levyra",
    val artworkData: ByteArray? = null
)

data class LevyraM4aTagResult(
    val success: Boolean,
    val artworkEmbedded: Boolean,
    val reason: String
)

private data class Mp4Box(
    val start: Int,
    val end: Int,
    val headerSize: Int,
    val type: Int
) {
    val length: Int = end - start
    val payloadStart: Int = start + headerSize
}

private object Atom {
    val FTYP = ascii("ftyp")
    val MOOV = ascii("moov")
    val MDAT = ascii("mdat")
    val UDTA = ascii("udta")
    val META = ascii("meta")
    val HDLR = ascii("hdlr")
    val ILST = ascii("ilst")
    val DATA = ascii("data")
    val MDIR = ascii("mdir")
    val STCO = ascii("stco")
    val CO64 = ascii("co64")
    val TRAK = ascii("trak")
    val MDIA = ascii("mdia")
    val MINF = ascii("minf")
    val STBL = ascii("stbl")
    val EDTS = ascii("edts")
    val DINF = ascii("dinf")
    val MOOF = ascii("moof")
    val TRAF = ascii("traf")
    val MVEX = ascii("mvex")
    val NAM = fourCc(0xA9, 'n'.code, 'a'.code, 'm'.code)
    val ART = fourCc(0xA9, 'A'.code, 'R'.code, 'T'.code)
    val ALB = fourCc(0xA9, 'a'.code, 'l'.code, 'b'.code)
    val DAY = fourCc(0xA9, 'd'.code, 'a'.code, 'y'.code)
    val GENRE = fourCc(0xA9, 'g'.code, 'e'.code, 'n'.code)
    val LYRICS = fourCc(0xA9, 'l'.code, 'y'.code, 'r'.code)
    val ENCODER = fourCc(0xA9, 't'.code, 'o'.code, 'o'.code)
    val AART = ascii("aART")
    val COVR = ascii("covr")
    val TRACK_NUMBER = ascii("trkn")
    val DISC_NUMBER = ascii("disk")
    val ADVISORY = ascii("rtng")
    val FREEFORM = ascii("----")
    val MEAN = ascii("mean")
    val NAME = ascii("name")
    val REPLACED_TAGS = setOf(
        NAM,
        ART,
        ALB,
        DAY,
        GENRE,
        LYRICS,
        ENCODER,
        AART,
        COVR,
        TRACK_NUMBER,
        DISC_NUMBER,
        ADVISORY
    )
    val CONTAINERS = setOf(MOOV, TRAK, MDIA, MINF, STBL, EDTS, DINF, UDTA, MOOF, TRAF, MVEX)

    fun bytes(type: Int): ByteArray {
        return byteArrayOf(
            ((type ushr 24) and 0xFF).toByte(),
            ((type ushr 16) and 0xFF).toByte(),
            ((type ushr 8) and 0xFF).toByte(),
            (type and 0xFF).toByte()
        )
    }

    private fun ascii(value: String): Int {
        val raw = value.padEnd(4).take(4)
        return fourCc(raw[0].code, raw[1].code, raw[2].code, raw[3].code)
    }

    private fun fourCc(a: Int, b: Int, c: Int, d: Int): Int {
        return ((a and 0xFF) shl 24) or ((b and 0xFF) shl 16) or ((c and 0xFF) shl 8) or (d and 0xFF)
    }
}
