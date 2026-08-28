package com.luc4n3x.levyra.feature.recognition

import com.luc4n3x.levyra.recognition.AcousticFingerprintEngine
import com.luc4n3x.levyra.recognition.FingerprintProfile
import com.luc4n3x.levyra.recognition.RecognitionSignature

typealias ShazamSignature = RecognitionSignature

class ShazamSignatureGenerator(
    maxDurationSeconds: Double = DEFAULT_MAX_DURATION_SECONDS,
    maxPeaks: Int = DEFAULT_MAX_PEAKS
) {
    private val engine = AcousticFingerprintEngine(
        FingerprintProfile(
            durationTargetSeconds = maxDurationSeconds,
            peakTarget = maxPeaks,
            hardMaxDurationSeconds = maxOf(HARD_MAX_DURATION_SECONDS, maxDurationSeconds),
            hardPeakLimit = maxOf(HARD_MAX_PEAKS, maxPeaks)
        )
    )

    fun generate(samples: ShortArray): ShazamSignature? = engine.fingerprint(samples)

    companion object {
        const val SAMPLE_RATE_HZ = AcousticFingerprintEngine.SAMPLE_RATE_HZ
        const val DEFAULT_MAX_DURATION_SECONDS = 3.1
        const val DEFAULT_MAX_PEAKS = 255
        const val SIGNATURE_URI_PREFIX = RecognitionSignature.DATA_URI_PREFIX
        private const val HARD_MAX_DURATION_SECONDS = 12.0
        private const val HARD_MAX_PEAKS = 2_048
    }
}
