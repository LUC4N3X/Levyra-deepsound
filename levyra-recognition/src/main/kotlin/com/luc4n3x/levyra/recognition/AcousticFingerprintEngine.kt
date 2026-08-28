package com.luc4n3x.levyra.recognition

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max

class AcousticFingerprintEngine(
    private val profile: FingerprintProfile = FingerprintProfile()
) {
    fun fingerprint(pcm16Mono: ShortArray): RecognitionSignature? {
        if (pcm16Mono.size < HOP_SIZE) return null
        return Session(profile).run(pcm16Mono)
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        private const val FFT_SIZE = 2_048
        private const val HOP_SIZE = 128
        private const val SPECTRUM_BINS = FFT_SIZE / 2 + 1
        private const val HISTORY_FRAMES = 256
        private const val WINDOW_PERIOD = 2_050
        private const val RECOGNITION_LAG = 46
        private const val SPREAD_LAG = 49
        private const val MIN_SEARCH_BIN = 10
        private const val MAX_SEARCH_BIN = 1_014
        private const val MAGNITUDE_DIVISOR = 131_072.0
        private const val MIN_MAGNITUDE = 1e-10
        private const val MIN_PEAK_ENERGY = 1.0 / 64.0

        private val FREQUENCY_NEIGHBOURS = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
        private val TIME_NEIGHBOURS = intArrayOf(
            -53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249
        )
    }

    private class Session(private val profile: FingerprintProfile) {
        private val fft = Radix2Fft(FFT_SIZE)
        private val window = DoubleArray(FFT_SIZE) { index ->
            0.5 - 0.5 * cos(2.0 * PI * (index + 1) / (WINDOW_PERIOD - 1))
        }
        private val ring = IntArray(FFT_SIZE)
        private var ringCursor = 0
        private val real = DoubleArray(FFT_SIZE)
        private val imaginary = DoubleArray(FFT_SIZE)
        private val spectra = Array(HISTORY_FRAMES) { DoubleArray(SPECTRUM_BINS) }
        private var spectrumCursor = 0
        private val spread = Array(HISTORY_FRAMES) { DoubleArray(SPECTRUM_BINS) }
        private var spreadCursor = 0
        private var spreadFrames = 0
        private val spreadScratch = DoubleArray(SPECTRUM_BINS)
        private val peaks = linkedMapOf<FrequencyBand, MutableList<SpectralPeak>>()
        private var processedSamples = 0
        private var peakCount = 0

        fun run(samples: ShortArray): RecognitionSignature? {
            val targetSamples = (profile.durationTargetSeconds * SAMPLE_RATE_HZ).toInt()
            val hardMaxSamples = (profile.hardMaxDurationSeconds * SAMPLE_RATE_HZ).toInt()
            var offset = 0
            while (
                offset + HOP_SIZE <= samples.size &&
                processedSamples < hardMaxSamples &&
                (processedSamples < targetSamples || peakCount < profile.peakTarget)
            ) {
                appendFrame(samples, offset)
                offset += HOP_SIZE
                processedSamples += HOP_SIZE
            }
            if (peakCount == 0) return null
            val immutablePeaks = peaks.mapValues { (_, value) -> value.toList() }
            val bytes = FingerprintPacketCodec.encode(SAMPLE_RATE_HZ, processedSamples, immutablePeaks)
            return RecognitionSignature(
                payload = bytes,
                sampleDurationMs = processedSamples * 1000L / SAMPLE_RATE_HZ,
                peakCount = peakCount
            )
        }

        private fun appendFrame(samples: ShortArray, offset: Int) {
            for (index in 0 until HOP_SIZE) {
                ring[ringCursor] = samples[offset + index].toInt()
                ringCursor++
                if (ringCursor == FFT_SIZE) ringCursor = 0
            }
            calculateSpectrum()
            spreadCurrentSpectrum()
            if (spreadFrames >= RECOGNITION_LAG && peakCount < profile.hardPeakLimit) {
                collectPeaks()
            }
        }

        private fun calculateSpectrum() {
            var source = ringCursor
            for (index in 0 until FFT_SIZE) {
                real[index] = ring[source].toDouble() * window[index]
                imaginary[index] = 0.0
                source++
                if (source == FFT_SIZE) source = 0
            }
            fft.transform(real, imaginary)

            val output = spectra[spectrumCursor]
            for (bin in 0 until SPECTRUM_BINS) {
                val energy = (real[bin] * real[bin] + imaginary[bin] * imaginary[bin]) / MAGNITUDE_DIVISOR
                output[bin] = if (energy > MIN_MAGNITUDE) energy else MIN_MAGNITUDE
            }
            spectrumCursor = nextIndex(spectrumCursor)
        }

        private fun spreadCurrentSpectrum() {
            val source = spectra[historyIndex(spectrumCursor, -1)]
            for (bin in 0 until SPECTRUM_BINS - 3) {
                spreadScratch[bin] = max(source[bin], max(source[bin + 1], source[bin + 2]))
            }
            for (bin in SPECTRUM_BINS - 3 until SPECTRUM_BINS) {
                spreadScratch[bin] = source[bin]
            }

            val oneBack = spread[historyIndex(spreadCursor, -1)]
            val threeBack = spread[historyIndex(spreadCursor, -3)]
            val sixBack = spread[historyIndex(spreadCursor, -6)]
            for (bin in 0 until SPECTRUM_BINS) {
                val first = max(spreadScratch[bin], oneBack[bin])
                oneBack[bin] = first
                val second = max(first, threeBack[bin])
                threeBack[bin] = second
                sixBack[bin] = max(second, sixBack[bin])
            }

            System.arraycopy(spreadScratch, 0, spread[spreadCursor], 0, SPECTRUM_BINS)
            spreadCursor = nextIndex(spreadCursor)
            spreadFrames++
        }

        private fun collectPeaks() {
            val spectrum = spectra[historyIndex(spectrumCursor, -RECOGNITION_LAG)]
            val comparison = spread[historyIndex(spreadCursor, -SPREAD_LAG)]
            for (bin in MIN_SEARCH_BIN..MAX_SEARCH_BIN) {
                if (peakCount >= profile.hardPeakLimit) return
                val energy = spectrum[bin]
                if (energy < MIN_PEAK_ENERGY || energy < comparison[bin - 1]) continue
                if (!isFrequencyMaximum(energy, comparison, bin)) continue
                if (!isTimeMaximum(energy, bin)) continue

                val magnitude = encodedMagnitude(energy)
                val before = encodedMagnitude(spectrum[bin - 1])
                val after = encodedMagnitude(spectrum[bin + 1])
                val curvature = magnitude * 2.0 - before - after
                if (curvature <= 0.0) continue

                val correction = (after - before) * 32.0 / curvature
                val correctedBin = bin * 64.0 + correction
                val frequencyHz = correctedBin * (SAMPLE_RATE_HZ / 2.0 / 1024.0 / 64.0)
                val band = bandFor(frequencyHz) ?: continue
                peaks.getOrPut(band) { mutableListOf() }.add(
                    SpectralPeak(
                        frameIndex = spreadFrames - RECOGNITION_LAG,
                        magnitude = magnitude.toInt(),
                        correctedBin = correctedBin.toInt()
                    )
                )
                peakCount++
            }
        }

        private fun isFrequencyMaximum(energy: Double, comparison: DoubleArray, bin: Int): Boolean {
            var strongest = 0.0
            for (offset in FREQUENCY_NEIGHBOURS) {
                strongest = max(strongest, comparison[bin + offset])
            }
            return energy > strongest
        }

        private fun isTimeMaximum(energy: Double, bin: Int): Boolean {
            var strongest = 0.0
            for (offset in TIME_NEIGHBOURS) {
                val candidate = spread[historyIndex(spreadCursor, offset)][bin - 1]
                strongest = max(strongest, candidate)
            }
            return energy > strongest
        }

        private fun encodedMagnitude(energy: Double): Double =
            ln(max(MIN_PEAK_ENERGY, energy)) * 1477.3 + 6144.0

        private fun bandFor(frequencyHz: Double): FrequencyBand? = when {
            frequencyHz > 250.0 && frequencyHz <= 520.0 -> FrequencyBand.LOW
            frequencyHz > 520.0 && frequencyHz <= 1450.0 -> FrequencyBand.MID_LOW
            frequencyHz > 1450.0 && frequencyHz <= 3500.0 -> FrequencyBand.MID_HIGH
            frequencyHz > 3500.0 && frequencyHz <= 5500.0 -> FrequencyBand.HIGH
            else -> null
        }

        private fun nextIndex(index: Int): Int = if (index + 1 == HISTORY_FRAMES) 0 else index + 1

        private fun historyIndex(cursor: Int, offset: Int): Int {
            val raw = (cursor + offset) % HISTORY_FRAMES
            return if (raw < 0) raw + HISTORY_FRAMES else raw
        }
    }
}
