package com.luc4n3x.levyra.feature.recognition

data class AudioFingerprint(
    val samples: ShortArray,
    val sampleRateHz: Int,
    val durationMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFingerprint) return false
        return sampleRateHz == other.sampleRateHz &&
            durationMs == other.durationMs &&
            samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

interface RecognitionProvider {
    val id: String

    suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome
}
