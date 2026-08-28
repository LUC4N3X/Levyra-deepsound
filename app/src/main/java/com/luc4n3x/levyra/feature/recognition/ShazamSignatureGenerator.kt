/*
 * Fingerprint format adapted from ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.luc4n3x.levyra.feature.recognition

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

data class ShazamSignature(
    val payload: ByteArray,
    val sampleDurationMs: Long,
    val peakCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShazamSignature) return false
        return sampleDurationMs == other.sampleDurationMs &&
            peakCount == other.peakCount &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + sampleDurationMs.hashCode()
        result = 31 * result + peakCount
        return result
    }
}

class ShazamSignatureGenerator(
    private val maxDurationSeconds: Double = DEFAULT_MAX_DURATION_SECONDS,
    private val maxPeaks: Int = DEFAULT_MAX_PEAKS
) {
    private val window = DoubleArray(FFT_SIZE) { index ->
        0.5 - 0.5 * cos(2.0 * PI * (index + 1) / (WINDOW_PERIOD - 1))
    }
    private val fft = RealFft(FFT_SIZE)
    private val realBuffer = DoubleArray(FFT_SIZE)
    private val imaginaryBuffer = DoubleArray(FFT_SIZE)
    private val ringSamples = IntArray(FFT_SIZE)
    private var ringPosition = 0

    private val fftOutputs = Array(HISTORY_SIZE) { DoubleArray(BIN_COUNT) }
    private var fftPosition = 0

    private val spreadOutputs = Array(HISTORY_SIZE) { DoubleArray(BIN_COUNT) }
    private var spreadPosition = 0
    private var spreadWritten = 0

    private val scratchSpread = DoubleArray(BIN_COUNT)
    private val peaksByBand = linkedMapOf<FrequencyBand, MutableList<FrequencyPeak>>()
    private var totalPeakCount = 0
    private var processedSampleCount = 0

    fun generate(samples: ShortArray): ShazamSignature? {
        reset()
        if (samples.size < CHUNK_SIZE) return null
        var offset = 0
        while (offset + CHUNK_SIZE <= samples.size && shouldKeepProcessing()) {
            processChunk(samples, offset)
            offset += CHUNK_SIZE
            processedSampleCount += CHUNK_SIZE
        }
        if (totalPeakCount == 0) return null
        return ShazamSignature(
            payload = encode(),
            sampleDurationMs = (processedSampleCount.toLong() * 1000L) / SAMPLE_RATE_HZ,
            peakCount = totalPeakCount
        )
    }

    private fun reset() {
        ringSamples.fill(0)
        ringPosition = 0
        fftOutputs.forEach { it.fill(0.0) }
        fftPosition = 0
        spreadOutputs.forEach { it.fill(0.0) }
        spreadPosition = 0
        spreadWritten = 0
        scratchSpread.fill(0.0)
        peaksByBand.clear()
        totalPeakCount = 0
        processedSampleCount = 0
    }

    private fun shouldKeepProcessing(): Boolean {
        val elapsedSeconds = processedSampleCount.toDouble() / SAMPLE_RATE_HZ
        return elapsedSeconds < maxDurationSeconds || totalPeakCount < maxPeaks
    }

    private fun processChunk(samples: ShortArray, offset: Int) {
        transform(samples, offset)
        spreadPeaks()
        if (spreadWritten >= PEAK_RECOGNITION_DELAY) recognizePeaks()
    }

    private fun transform(samples: ShortArray, offset: Int) {
        for (index in 0 until CHUNK_SIZE) {
            ringSamples[ringPosition] = samples[offset + index].toInt()
            ringPosition = (ringPosition + 1) % FFT_SIZE
        }
        var readPosition = ringPosition
        for (index in 0 until FFT_SIZE) {
            realBuffer[index] = ringSamples[readPosition].toDouble() * window[index]
            imaginaryBuffer[index] = 0.0
            readPosition = (readPosition + 1) % FFT_SIZE
        }
        fft.transform(realBuffer, imaginaryBuffer)

        val output = fftOutputs[fftPosition]
        for (bin in 0 until BIN_COUNT) {
            val real = realBuffer[bin]
            val imaginary = imaginaryBuffer[bin]
            val magnitude = (real * real + imaginary * imaginary) / MAGNITUDE_SCALE
            output[bin] = if (magnitude <= MIN_MAGNITUDE) MIN_MAGNITUDE else magnitude
        }
        fftPosition = (fftPosition + 1) % HISTORY_SIZE
    }

    private fun spreadPeaks() {
        val origin = fftOutputs[floorMod(fftPosition - 1, HISTORY_SIZE)]
        for (bin in 0..BIN_COUNT - 4) {
            scratchSpread[bin] = max(origin[bin], max(origin[bin + 1], origin[bin + 2]))
        }
        for (bin in BIN_COUNT - 3 until BIN_COUNT) {
            scratchSpread[bin] = origin[bin]
        }

        val previous1 = spreadOutputs[floorMod(spreadPosition - 1, HISTORY_SIZE)]
        val previous3 = spreadOutputs[floorMod(spreadPosition - 3, HISTORY_SIZE)]
        val previous6 = spreadOutputs[floorMod(spreadPosition - 6, HISTORY_SIZE)]
        for (bin in 0 until BIN_COUNT) {
            val first = max(scratchSpread[bin], previous1[bin])
            previous1[bin] = first
            val second = max(first, previous3[bin])
            previous3[bin] = second
            previous6[bin] = max(second, previous6[bin])
        }

        System.arraycopy(scratchSpread, 0, spreadOutputs[spreadPosition], 0, BIN_COUNT)
        spreadPosition = (spreadPosition + 1) % HISTORY_SIZE
        spreadWritten++
    }

    private fun recognizePeaks() {
        val fftFrame = fftOutputs[floorMod(fftPosition - PEAK_RECOGNITION_DELAY, HISTORY_SIZE)]
        val spreadFrame = spreadOutputs[floorMod(spreadPosition - SPREAD_LOOKBACK, HISTORY_SIZE)]
        for (bin in MIN_PEAK_BIN..MAX_PEAK_BIN) {
            val energy = fftFrame[bin]
            if (energy < MIN_PEAK_ENERGY || energy < spreadFrame[bin - 1]) continue

            var maxNeighbour = 0.0
            for (offset in BIN_NEIGHBOUR_OFFSETS) {
                maxNeighbour = max(maxNeighbour, spreadFrame[bin + offset])
            }
            if (energy <= maxNeighbour) continue

            var maxTimeNeighbour = maxNeighbour
            for (offset in TIME_NEIGHBOUR_OFFSETS) {
                val frame = spreadOutputs[floorMod(spreadPosition + offset, HISTORY_SIZE)]
                maxTimeNeighbour = max(maxTimeNeighbour, frame[bin - 1])
            }
            if (energy <= maxTimeNeighbour) continue

            val magnitude = magnitudeOf(energy)
            val magnitudeBefore = magnitudeOf(fftFrame[bin - 1])
            val magnitudeAfter = magnitudeOf(fftFrame[bin + 1])
            val curvature = magnitude * 2.0 - magnitudeBefore - magnitudeAfter
            if (curvature <= 0.0) continue

            val correction = (magnitudeAfter - magnitudeBefore) * 32.0 / curvature
            val correctedBin = bin * 64.0 + correction
            val frequencyHz = correctedBin * (SAMPLE_RATE_HZ / 2.0 / 1024.0 / 64.0)
            val band = bandFor(frequencyHz) ?: continue

            peaksByBand.getOrPut(band) { mutableListOf() }.add(
                FrequencyPeak(
                    passNumber = spreadWritten - PEAK_RECOGNITION_DELAY,
                    magnitude = magnitude.toInt(),
                    correctedBin = correctedBin.toInt()
                )
            )
            totalPeakCount++
        }
    }

    private fun magnitudeOf(energy: Double): Double = ln(max(MIN_PEAK_ENERGY, energy)) * 1477.3 + 6144.0

    private fun bandFor(frequencyHz: Double): FrequencyBand? = when {
        frequencyHz in 250.0..520.0 -> FrequencyBand.HZ_250_520
        frequencyHz > 520.0 && frequencyHz <= 1450.0 -> FrequencyBand.HZ_520_1450
        frequencyHz > 1450.0 && frequencyHz <= 3500.0 -> FrequencyBand.HZ_1450_3500
        frequencyHz > 3500.0 && frequencyHz <= 5500.0 -> FrequencyBand.HZ_3500_5500
        else -> null
    }

    private fun encode(): ByteArray {
        val contents = ByteArrayOutputStream()
        peaksByBand.entries.sortedBy { it.key.value }.forEach { (band, peaks) ->
            val encodedPeaks = ByteArrayOutputStream()
            var lastPass = 0
            peaks.forEach { peak ->
                var delta = peak.passNumber - lastPass
                if (delta >= PASS_ESCAPE) {
                    encodedPeaks.write(PASS_ESCAPE)
                    encodedPeaks.writeLittleInt(peak.passNumber)
                    delta = 0
                }
                encodedPeaks.write(delta.coerceIn(0, PASS_ESCAPE - 1))
                encodedPeaks.writeLittleShort(peak.magnitude)
                encodedPeaks.writeLittleShort(peak.correctedBin)
                lastPass = peak.passNumber
            }
            val encoded = encodedPeaks.toByteArray()
            contents.writeLittleInt(BAND_MARKER + band.value)
            contents.writeLittleInt(encoded.size)
            contents.write(encoded)
            repeat(floorMod(-encoded.size, 4)) { contents.write(0) }
        }

        val body = contents.toByteArray()
        val sizeMinusHeader = body.size + 8
        val header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(MAGIC_1)
            putInt(0)
            putInt(sizeMinusHeader)
            putInt(MAGIC_2)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(SAMPLE_RATE_ID shl 27)
            putInt(0)
            putInt(0)
            putInt((processedSampleCount + SAMPLE_RATE_HZ * SAMPLE_PADDING_SECONDS).toInt())
            putInt((15 shl 19) + 0x40000)
        }

        val full = ByteArrayOutputStream()
        full.write(header.array())
        full.writeLittleInt(BODY_MARKER)
        full.writeLittleInt(sizeMinusHeader)
        full.write(body)

        val encoded = full.toByteArray()
        val checksum = CRC32().apply { update(encoded, 8, encoded.size - 8) }.value.toInt()
        ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).putInt(4, checksum)
        return encoded
    }

    private enum class FrequencyBand(val value: Int) {
        HZ_250_520(0),
        HZ_520_1450(1),
        HZ_1450_3500(2),
        HZ_3500_5500(3)
    }

    private data class FrequencyPeak(
        val passNumber: Int,
        val magnitude: Int,
        val correctedBin: Int
    )

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val DEFAULT_MAX_DURATION_SECONDS = 3.1
        const val DEFAULT_MAX_PEAKS = 255
        const val SIGNATURE_URI_PREFIX = "data:audio/vnd.shazam.sig;base64,"

        private const val FFT_SIZE = 2048
        private const val WINDOW_PERIOD = 2050
        private const val BIN_COUNT = 1025
        private const val HISTORY_SIZE = 256
        private const val CHUNK_SIZE = 128
        private const val PEAK_RECOGNITION_DELAY = 46
        private const val SPREAD_LOOKBACK = 49
        private const val MIN_PEAK_BIN = 10
        private const val MAX_PEAK_BIN = 1014
        private const val MAGNITUDE_SCALE = 131072.0
        private const val MIN_MAGNITUDE = 1e-10
        private const val MIN_PEAK_ENERGY = 1.0 / 64.0
        private const val PASS_ESCAPE = 255
        private const val BAND_MARKER = 0x60030040
        private const val BODY_MARKER = 0x40000000
        private const val MAGIC_1 = -889313920
        private const val MAGIC_2 = -1810785280
        private const val HEADER_BYTES = 48
        private const val SAMPLE_RATE_ID = 3
        private const val SAMPLE_PADDING_SECONDS = 0.24

        private val BIN_NEIGHBOUR_OFFSETS = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
        private val TIME_NEIGHBOUR_OFFSETS = intArrayOf(
            -53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249
        )

        private fun floorMod(value: Int, modulus: Int): Int {
            val remainder = value % modulus
            return if (remainder < 0) remainder + modulus else remainder
        }
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

private class RealFft(private val size: Int) {
    private val cosTable = DoubleArray(size / 2) { index -> cos(2.0 * PI * index / size) }
    private val sinTable = DoubleArray(size / 2) { index -> sin(2.0 * PI * index / size) }
    private val bitReversal = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (index in 0 until size) table[index] = reverseBits(index, bits)
    }

    fun transform(real: DoubleArray, imaginary: DoubleArray) {
        for (index in 0 until size) {
            val mirrored = bitReversal[index]
            if (mirrored > index) {
                val tempReal = real[index]
                real[index] = real[mirrored]
                real[mirrored] = tempReal
                val tempImaginary = imaginary[index]
                imaginary[index] = imaginary[mirrored]
                imaginary[mirrored] = tempImaginary
            }
        }

        var length = 2
        while (length <= size) {
            val halfLength = length / 2
            val tableStep = size / length
            var block = 0
            while (block < size) {
                var index = 0
                var tableIndex = 0
                while (index < halfLength) {
                    val cosine = cosTable[tableIndex]
                    val sine = sinTable[tableIndex]
                    val low = block + index
                    val high = low + halfLength

                    val highReal = real[high]
                    val highImaginary = imaginary[high]
                    val rotatedReal = highReal * cosine + highImaginary * sine
                    val rotatedImaginary = -highReal * sine + highImaginary * cosine

                    real[high] = real[low] - rotatedReal
                    imaginary[high] = imaginary[low] - rotatedImaginary
                    real[low] += rotatedReal
                    imaginary[low] += rotatedImaginary

                    index++
                    tableIndex += tableStep
                }
                block += length
            }
            length = length shl 1
        }
    }

    private fun reverseBits(value: Int, bitCount: Int): Int {
        var source = value
        var reversed = 0
        repeat(bitCount) {
            reversed = (reversed shl 1) or (source and 1)
            source = source ushr 1
        }
        return reversed
    }
}
