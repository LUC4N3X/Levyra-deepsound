package com.luc4n3x.levyra.data.locallibrary

import java.io.InputStream

internal object LevyraTagIdentityReader {
    private const val LEVYRA_MEAN = "com.luc4n3x.levyra"
    private const val TRACK_ID = "TRACK_ID"
    private const val MAX_MOOV_BYTES = 8 * 1024 * 1024
    private const val MAX_TOP_LEVEL_BOXES = 64
    private const val MAX_BOX_DEPTH = 6
    private const val HEADER_BYTES = 8

    fun readTrackId(input: InputStream, streamLength: Long): String? {
        var offset = 0L
        repeat(MAX_TOP_LEVEL_BOXES) {
            if (streamLength in 1..offset) return null
            val header = input.readExactlyOrNull(HEADER_BYTES) ?: return null
            var size = readUInt32(header, 0)
            val type = String(header, 4, 4, Charsets.ISO_8859_1)
            var headerSize = HEADER_BYTES.toLong()
            if (size == 1L) {
                val large = input.readExactlyOrNull(8) ?: return null
                size = readUInt64(large)
                headerSize += 8L
            } else if (size == 0L) {
                if (streamLength <= offset) return null
                size = streamLength - offset
            }
            if (size < headerSize) return null
            val payloadSize = size - headerSize
            if (type == "moov") {
                if (payloadSize > MAX_MOOV_BYTES) return null
                val moov = input.readExactlyOrNull(payloadSize.toInt()) ?: return null
                return findTrackId(moov, 0, moov.size, depth = 0)
            }
            if (!input.skipFully(payloadSize)) return null
            offset += size
        }
        return null
    }

    internal fun findTrackId(source: ByteArray, start: Int, end: Int, depth: Int): String? {
        if (depth > MAX_BOX_DEPTH) return null
        var cursor = start
        while (cursor + HEADER_BYTES <= end) {
            val size = readUInt32(source, cursor)
            if (size < HEADER_BYTES || cursor + size > end) return null
            val boxEnd = cursor + size.toInt()
            val payloadStart = cursor + HEADER_BYTES
            val found = when (String(source, cursor + 4, 4, Charsets.ISO_8859_1)) {
                "udta", "ilst" -> findTrackId(source, payloadStart, boxEnd, depth + 1)
                "meta" -> findTrackId(source, payloadStart + 4, boxEnd, depth + 1)
                "----" -> freeformTrackId(source, payloadStart, boxEnd)
                else -> null
            }
            if (found != null) return found
            cursor = boxEnd
        }
        return null
    }

    private fun freeformTrackId(source: ByteArray, start: Int, end: Int): String? {
        var mean: String? = null
        var name: String? = null
        var value: String? = null
        var cursor = start
        while (cursor + HEADER_BYTES <= end) {
            val size = readUInt32(source, cursor)
            if (size < HEADER_BYTES || cursor + size > end) return null
            val boxEnd = cursor + size.toInt()
            when (String(source, cursor + 4, 4, Charsets.ISO_8859_1)) {
                "mean" -> mean = textBetween(source, cursor + 12, boxEnd)
                "name" -> name = textBetween(source, cursor + 12, boxEnd)
                "data" -> value = textBetween(source, cursor + 16, boxEnd)
            }
            cursor = boxEnd
        }
        if (mean != LEVYRA_MEAN || name != TRACK_ID) return null
        return value?.trim()?.takeIf { it.isNotEmpty() && it.length <= 256 }
    }

    private fun textBetween(source: ByteArray, start: Int, end: Int): String? =
        if (start > end) null else String(source, start, end - start, Charsets.UTF_8)

    private fun readUInt32(source: ByteArray, offset: Int): Long =
        ((source[offset].toLong() and 0xFF) shl 24) or
            ((source[offset + 1].toLong() and 0xFF) shl 16) or
            ((source[offset + 2].toLong() and 0xFF) shl 8) or
            (source[offset + 3].toLong() and 0xFF)

    private fun readUInt64(source: ByteArray): Long {
        var value = 0L
        for (index in 0 until 8) value = (value shl 8) or (source[index].toLong() and 0xFF)
        return value
    }

    private fun InputStream.readExactlyOrNull(length: Int): ByteArray? {
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = read(buffer, read, length - read)
            if (count < 0) return null
            read += count
        }
        return buffer
    }

    private fun InputStream.skipFully(length: Long): Boolean {
        var remaining = length
        while (remaining > 0L) {
            val skipped = skip(remaining)
            when {
                skipped > 0L -> remaining -= skipped
                read() >= 0 -> remaining--
                else -> return false
            }
        }
        return true
    }
}
