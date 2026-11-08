package com.luc4n3x.levyra.player.offline

import java.io.IOException
import java.io.OutputStream

internal object TidalDashFlac {
    private val flacMagic = byteArrayOf(0x66, 0x4C, 0x61, 0x43)
    private val samplePath = listOf("moov", "trak", "mdia", "minf", "stbl", "stsd", "fLaC", "dfLa")
    private val headerSkips = mapOf("stsd" to 8, "fLaC" to 28)
    private const val STREAMINFO_BLOCK_BYTES = 38

    fun header(init: ByteArray): ByteArray {
        val dfla = find(init, samplePath, 0, init.size) ?: throw IOException("FLAC configuration missing")
        if (dfla.end - dfla.body < 4 + STREAMINFO_BLOCK_BYTES) throw IOException("FLAC configuration too small")
        val metadata = init.copyOfRange(dfla.body + 4, dfla.end)
        if (metadata[0].toInt() and 0x7F != 0) throw IOException("FLAC STREAMINFO missing")
        return flacMagic + metadata
    }

    fun writeFrames(segment: ByteArray, output: OutputStream): Long {
        var written = 0L
        boxes(segment, 0, segment.size).filter { it.type == "mdat" }.forEach {
            if (it.end - it.body < 2 || !isFrameSync(segment, it.body)) throw IOException("DASH segment does not start with a FLAC frame")
            output.write(segment, it.body, it.end - it.body)
            written += it.end - it.body
        }
        if (written == 0L) throw IOException("DASH segment has no audio")
        return written
    }

    private fun isFrameSync(data: ByteArray, offset: Int): Boolean =
        data[offset] == 0xFF.toByte() && (data[offset + 1].toInt() and 0xFE) == 0xF8

    private class Box(val type: String, val body: Int, val end: Int)

    private fun boxes(data: ByteArray, start: Int, end: Int): Sequence<Box> = sequence {
        var position = start
        while (position + 8 <= end) {
            var size = readUInt(data, position)
            val type = String(data, position + 4, 4, Charsets.ISO_8859_1)
            var header = 8
            if (size == 1L) {
                if (position + 16 > end) throw IOException("Truncated box")
                size = (readUInt(data, position + 8) shl 32) or readUInt(data, position + 12)
                header = 16
            }
            if (size < header || position + size > end) throw IOException("Malformed box")
            yield(Box(type, position + header, (position + size).toInt()))
            position = (position + size).toInt()
        }
    }

    private fun find(data: ByteArray, path: List<String>, start: Int, end: Int): Box? {
        for (box in boxes(data, start, end)) {
            if (box.type != path.first()) continue
            if (path.size == 1) return box
            val skip = headerSkips[box.type] ?: 0
            if (box.body + skip > box.end) throw IOException("Malformed sample entry")
            find(data, path.drop(1), box.body + skip, box.end)?.let { return it }
        }
        return null
    }

    private fun readUInt(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFF) shl 24) or ((data[offset + 1].toLong() and 0xFF) shl 16) or
            ((data[offset + 2].toLong() and 0xFF) shl 8) or (data[offset + 3].toLong() and 0xFF)
}
