package com.luc4n3x.levyra.recognition

import java.util.Base64

class RecognitionSignature(
    val payload: ByteArray,
    val sampleDurationMs: Long,
    val peakCount: Int
) {
    fun toDataUri(): String = DATA_URI_PREFIX + Base64.getEncoder().encodeToString(payload)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecognitionSignature) return false
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

    companion object {
        const val DATA_URI_PREFIX = "data:audio/vnd.shazam.sig;base64,"
    }
}

data class FingerprintProfile(
    val durationTargetSeconds: Double = 3.1,
    val peakTarget: Int = 255,
    val hardMaxDurationSeconds: Double = 12.0,
    val hardPeakLimit: Int = 2_048
) {
    init {
        require(durationTargetSeconds > 0.0)
        require(peakTarget > 0)
        require(hardMaxDurationSeconds >= durationTargetSeconds)
        require(hardPeakLimit >= peakTarget)
    }
}

object FingerprintEscalation {
    val LADDER: List<FingerprintProfile> = listOf(
        FingerprintProfile(),
        FingerprintProfile(
            durationTargetSeconds = 9.0,
            peakTarget = 2_048,
            hardMaxDurationSeconds = 12.0,
            hardPeakLimit = 2_048
        )
    )
}
