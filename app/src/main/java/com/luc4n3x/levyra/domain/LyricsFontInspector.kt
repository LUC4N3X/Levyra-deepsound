package com.luc4n3x.levyra.domain

import java.io.File
import java.io.RandomAccessFile

enum class LyricsFontFormat(val extension: String) {
    TRUE_TYPE("ttf"),
    OPEN_TYPE("otf");

    companion object {
        fun from(value: String?): LyricsFontFormat? =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}

data class LyricsFontInspection(
    val format: LyricsFontFormat,
    val familyName: String,
    val variableWeight: IntRange?
)

object LyricsFontInspector {
    private const val TRUE_TYPE_VERSION = 0x00010000
    private const val TRUE_TYPE_APPLE = 0x74727565
    private const val OPEN_TYPE_CFF = 0x4F54544F
    private const val HEAD_MAGIC = 0x5F0F3CF5
    private const val MAX_TABLES = 96
    internal const val NAME_LIMIT = 64
    private const val MAX_NAME_RECORD_BYTES = 512
    private val REQUIRED_TABLES = setOf("cmap", "head", "hhea", "hmtx", "maxp", "name")

    fun inspect(file: File): LyricsFontInspection? = runCatching {
        RandomAccessFile(file, "r").use { inspect(it) }
    }.getOrNull()

    private fun inspect(source: RandomAccessFile): LyricsFontInspection? {
        val length = source.length()
        if (length < 12) return null
        val format = when (source.u32(0)) {
            TRUE_TYPE_VERSION, TRUE_TYPE_APPLE -> LyricsFontFormat.TRUE_TYPE
            OPEN_TYPE_CFF -> LyricsFontFormat.OPEN_TYPE
            else -> return null
        }
        val tableCount = source.u16(4)
        if (tableCount !in 1..MAX_TABLES) return null
        val directoryEnd = 12L + tableCount * 16L
        if (directoryEnd > length) return null
        val tables = HashMap<String, LongRange>(tableCount)
        for (index in 0 until tableCount) {
            val record = 12L + index * 16L
            val tag = source.tag(record)
            val offset = source.u32(record + 8).toLong() and 0xFFFFFFFFL
            val size = source.u32(record + 12).toLong() and 0xFFFFFFFFL
            if (offset < directoryEnd || size <= 0L || offset + size > length) return null
            tables[tag] = offset until offset + size
        }
        if (!tables.keys.containsAll(REQUIRED_TABLES)) return null
        val hasOutlines = ("glyf" in tables && "loca" in tables) || "CFF " in tables || "CFF2" in tables
        if (!hasOutlines) return null
        val head = tables.getValue("head")
        if (head.size() < 54 || source.u32(head.first + 12) != HEAD_MAGIC) return null
        return LyricsFontInspection(
            format = format,
            familyName = familyName(source, tables.getValue("name")).orEmpty(),
            variableWeight = tables["fvar"]?.let { weightAxis(source, it) }
        )
    }

    private fun weightAxis(source: RandomAccessFile, table: LongRange): IntRange? {
        if (table.size() < 16) return null
        val axesOffset = source.u16(table.first + 4)
        val axisCount = source.u16(table.first + 8)
        val axisSize = source.u16(table.first + 10)
        if (axisSize < 20 || axesOffset + axisCount.toLong() * axisSize > table.size()) return null
        for (index in 0 until axisCount) {
            val axis = table.first + axesOffset + index.toLong() * axisSize
            if (source.tag(axis) != "wght") continue
            val min = source.u32(axis + 4) shr 16
            val max = source.u32(axis + 12) shr 16
            return if (min in 1 until max && max <= 1000) min..max else null
        }
        return null
    }

    private fun familyName(source: RandomAccessFile, table: LongRange): String? {
        if (table.size() < 6) return null
        val count = source.u16(table.first + 2)
        val storage = source.u16(table.first + 4).toLong()
        if (6L + count * 12L > table.size()) return null
        var bestRank = Int.MAX_VALUE
        var bestName: String? = null
        for (index in 0 until count) {
            val record = table.first + 6 + index * 12L
            val rank = nameRecordRank(source, record) ?: continue
            if (rank >= bestRank) continue
            val name = nameRecordText(source, table, storage, record) ?: continue
            bestRank = rank
            bestName = name
            if (rank == 0) break
        }
        return bestName
    }

    private fun nameRecordRank(source: RandomAccessFile, record: Long): Int? {
        val platform = source.u16(record)
        val encoding = source.u16(record + 2)
        val nameId = source.u16(record + 6)
        val supported = platform == 3 || platform == 0 || (platform == 1 && encoding == 0)
        if ((nameId != 1 && nameId != 16) || !supported) return null
        return nameRank(nameId, platform, source.u16(record + 4))
    }

    private fun nameRecordText(source: RandomAccessFile, table: LongRange, storage: Long, record: Long): String? {
        val size = source.u16(record + 8)
        val offset = source.u16(record + 10)
        if (size == 0 || size > MAX_NAME_RECORD_BYTES || storage + offset + size > table.size()) return null
        val charset = if (source.u16(record) == 1) Charsets.ISO_8859_1 else Charsets.UTF_16BE
        return sanitizeLyricsFontName(String(source.bytes(table.first + storage + offset, size), charset))
            .takeIf(String::isNotEmpty)
    }

    private fun nameRank(nameId: Int, platform: Int, language: Int): Int {
        val familyRank = if (nameId == 16) 0 else 10
        val platformRank = if (platform == 3) 0 else 4
        val englishOrDefault = language == 0x0409 || platform == 1 && language == 0
        return familyRank + platformRank + if (englishOrDefault) 0 else 1
    }

    private fun LongRange.size(): Long = last - first + 1

    private fun RandomAccessFile.u16(position: Long): Int {
        seek(position)
        return readUnsignedShort()
    }

    private fun RandomAccessFile.u32(position: Long): Int {
        seek(position)
        return readInt()
    }

    private fun RandomAccessFile.tag(position: Long): String = String(bytes(position, 4), Charsets.ISO_8859_1)

    private fun RandomAccessFile.bytes(position: Long, size: Int): ByteArray {
        seek(position)
        return ByteArray(size).also(::readFully)
    }
}

private val FONT_NAME_WHITESPACE = Regex("\\s+")

fun sanitizeLyricsFontName(raw: String): String =
    raw.filterNot { it.isISOControl() || it == '�' }
        .replace(FONT_NAME_WHITESPACE, " ")
        .trim()
        .take(LyricsFontInspector.NAME_LIMIT)
        .trim()
