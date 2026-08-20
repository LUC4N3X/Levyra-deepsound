package com.luc4n3x.levyra.desktop.core.localmusic

internal object MpegAudioReader {

    private const val SEARCH_WINDOW = 64 * 1024

    private val BITRATES_V1_L3 = intArrayOf(
        0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0
    )
    private val BITRATES_V2_L3 = intArrayOf(
        0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0
    )
    private val SAMPLE_RATES = arrayOf(
        intArrayOf(44100, 48000, 32000),
        intArrayOf(22050, 24000, 16000),
        intArrayOf(11025, 12000, 8000)
    )

    fun read(file: TagFile, tagBytes: Int, trailingBytes: Int): AudioTags {
        val start = tagBytes.toLong().coerceIn(0L, file.length)
        val window = file.readAt(start, SEARCH_WINDOW)
        val offset = findFrame(window) ?: return AudioTags()
        val header = window.u32be(offset)
        val versionBits = (header shr 19).toInt() and 0x03
        val layerBits = (header shr 17).toInt() and 0x03
        val bitrateIndex = (header shr 12).toInt() and 0x0F
        val sampleRateIndex = (header shr 10).toInt() and 0x03
        val channelMode = (header shr 6).toInt() and 0x03

        if (layerBits != 1) return AudioTags()
        val versionRow = when (versionBits) {
            3 -> 0
            2 -> 1
            0 -> 2
            else -> return AudioTags()
        }
        val sampleRate = SAMPLE_RATES[versionRow].getOrElse(sampleRateIndex) { 0 }
        if (sampleRate <= 0) return AudioTags()
        val table = if (versionBits == 3) BITRATES_V1_L3 else BITRATES_V2_L3
        val headerBitrate = table.getOrElse(bitrateIndex) { 0 }
        val channels = if (channelMode == 3) 1 else 2
        val samplesPerFrame = if (versionBits == 3) 1152 else 576

        val audioBytes = (file.length - start - trailingBytes).coerceAtLeast(0L)
        val variable = readVariableBitrateHeader(window, offset, versionBits, channelMode)
        val durationMs: Long
        val bitrateKbps: Int
        if (variable != null && variable.frames > 0) {
            durationMs = variable.frames * samplesPerFrame * 1000L / sampleRate
            val bytes = if (variable.bytes > 0L) variable.bytes else audioBytes
            bitrateKbps = if (durationMs > 0L) (bytes * 8L / durationMs).toInt() else headerBitrate
        } else if (headerBitrate > 0) {
            durationMs = audioBytes * 8L / headerBitrate
            bitrateKbps = headerBitrate
        } else {
            durationMs = 0L
            bitrateKbps = 0
        }

        return AudioTags(
            durationMs = durationMs.coerceAtLeast(0L),
            bitrateKbps = bitrateKbps.coerceIn(0, 3_000),
            sampleRateHz = sampleRate,
            channels = channels,
            codec = "MP3"
        )
    }

    private class VariableHeader(val frames: Long, val bytes: Long)

    private fun readVariableBitrateHeader(
        window: ByteArray,
        frameOffset: Int,
        versionBits: Int,
        channelMode: Int
    ): VariableHeader? {
        val sideInfo = when {
            versionBits == 3 && channelMode == 3 -> 17
            versionBits == 3 -> 32
            channelMode == 3 -> 9
            else -> 17
        }
        val xingOffset = frameOffset + 4 + sideInfo
        if (xingOffset + 16 > window.size) return null
        val marker = window.ascii(xingOffset, 4)
        if (marker == "Xing" || marker == "Info") {
            val flags = window.u32be(xingOffset + 4)
            var cursor = xingOffset + 8
            var frames = 0L
            var bytes = 0L
            if (flags and 0x1L != 0L) {
                if (cursor + 4 > window.size) return null
                frames = window.u32be(cursor)
                cursor += 4
            }
            if (flags and 0x2L != 0L) {
                if (cursor + 4 > window.size) return null
                bytes = window.u32be(cursor)
            }
            return VariableHeader(frames, bytes)
        }
        val vbriOffset = frameOffset + 4 + 32
        if (vbriOffset + 26 <= window.size && window.ascii(vbriOffset, 4) == "VBRI") {
            return VariableHeader(
                frames = window.u32be(vbriOffset + 14),
                bytes = window.u32be(vbriOffset + 10)
            )
        }
        return null
    }

    private fun findFrame(window: ByteArray): Int? {
        var index = 0
        while (index + 4 <= window.size) {
            if (
                window.u8(index) == 0xFF &&
                window.u8(index + 1) and 0xE0 == 0xE0 &&
                window.u8(index + 1) and 0x18 != 0x08 &&
                window.u8(index + 1) and 0x06 != 0x00 &&
                window.u8(index + 2) and 0xF0 != 0xF0 &&
                window.u8(index + 2) and 0x0C != 0x0C
            ) {
                return index
            }
            index += 1
        }
        return null
    }
}
