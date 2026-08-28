package com.luc4n3x.levyra.recognition

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

internal enum class FrequencyBand(val wireId: Int) {
    LOW(0),
    MID_LOW(1),
    MID_HIGH(2),
    HIGH(3)
}

internal data class SpectralPeak(
    val frameIndex: Int,
    val magnitude: Int,
    val correctedBin: Int
)

internal object FingerprintPacketCodec {
    private val MAGIC_A = 0xCAFE2580.toInt()
    private val MAGIC_B = 0x94119C00.toInt()
    private const val HEADER_SIZE = 48
    private const val BODY_TAG = 0x40000000
    private const val BAND_TAG_BASE = 0x60030040
    private const val ABSOLUTE_FRAME_ESCAPE = 0xFF
    private const val SAMPLE_RATE_ID_16_KHZ = 3
    private const val SAMPLE_PADDING_SECONDS = 0.24

    fun encode(
        sampleRateHz: Int,
        sampleCount: Int,
        peaksByBand: Map<FrequencyBand, List<SpectralPeak>>
    ): ByteArray {
        val bandPayload = ByteArrayOutputStream()
        peaksByBand.entries.sortedBy { it.key.wireId }.forEach { (band, peaks) ->
            if (peaks.isEmpty()) return@forEach
            val peakPayload = encodePeaks(peaks)
            bandPayload.writeLittleInt(BAND_TAG_BASE + band.wireId)
            bandPayload.writeLittleInt(peakPayload.size)
            bandPayload.write(peakPayload)
            repeat(floorMod(-peakPayload.size, 4)) { bandPayload.write(0) }
        }

        val encodedBands = bandPayload.toByteArray()
        val sizeMinusHeader = encodedBands.size + 8
        val packet = ByteArrayOutputStream(HEADER_SIZE + sizeMinusHeader)
        val header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(MAGIC_A)
            putInt(0)
            putInt(sizeMinusHeader)
            putInt(MAGIC_B)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(SAMPLE_RATE_ID_16_KHZ shl 27)
            putInt(0)
            putInt(0)
            putInt((sampleCount + sampleRateHz * SAMPLE_PADDING_SECONDS).toInt())
            putInt((15 shl 19) + 0x40000)
        }
        packet.write(header.array())
        packet.writeLittleInt(BODY_TAG)
        packet.writeLittleInt(sizeMinusHeader)
        packet.write(encodedBands)

        val bytes = packet.toByteArray()
        val crc = CRC32().apply { update(bytes, 8, bytes.size - 8) }.value.toInt()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, crc)
        return bytes
    }

    private fun encodePeaks(peaks: List<SpectralPeak>): ByteArray {
        val output = ByteArrayOutputStream(peaks.size * 5)
        var previousFrame = 0
        peaks.sortedBy { it.frameIndex }.forEach { peak ->
            var delta = peak.frameIndex - previousFrame
            if (delta >= ABSOLUTE_FRAME_ESCAPE) {
                output.write(ABSOLUTE_FRAME_ESCAPE)
                output.writeLittleInt(peak.frameIndex)
                previousFrame = peak.frameIndex
                delta = 0
            }
            output.write(delta.coerceIn(0, ABSOLUTE_FRAME_ESCAPE - 1))
            output.writeLittleShort(peak.magnitude)
            output.writeLittleShort(peak.correctedBin)
            previousFrame = peak.frameIndex
        }
        return output.toByteArray()
    }

    private fun floorMod(value: Int, modulus: Int): Int {
        val remainder = value % modulus
        return if (remainder < 0) remainder + modulus else remainder
    }
}

private fun ByteArrayOutputStream.writeLittleInt(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
    write((value ushr 16) and 0xFF)
    write((value ushr 24) and 0xFF)
}

private fun ByteArrayOutputStream.writeLittleShort(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
}
