package com.luc4n3x.levyra.player.enhanced

/**
 * Configuration parameters for Levyra Enhanced Audio.
 *
 * Levyra Enhanced Audio operates on the residual restoration principle:
 * originalPCM = decoded stream (e.g. JioSaavn AAC 320)
 * restoredPCM = engine(originalPCM)
 * residual = restoredPCM - originalPCM
 * output = originalPCM + residual * adaptiveGain
 *
 * Preservation of the original lossy 320 kbps source takes precedence over aggressive synthesis.
 */
data class EnhancedAudioConfig(
    val enabled: Boolean = true,
    val adaptiveGainCap: Float = 0.50f,
    val deficitThreshold: Float = 0.15f,
    val cutoffFrequencyHz: Float = 18_500f,
    val harmonicGain: Float = 0.08f,
    val transientSensitivity: Float = 0.35f,
    val truePeakCeilingLinear: Float = 0.944f, // -0.5 dBFS
    val maxAllowedProcessingTimeUs: Long = 5_000L, // 5ms budget per block
    val protectStereo: Boolean = true
) {
    fun normalized(): EnhancedAudioConfig = copy(
        adaptiveGainCap = adaptiveGainCap.coerceIn(0f, 1f),
        deficitThreshold = deficitThreshold.coerceIn(0.01f, 0.99f),
        cutoffFrequencyHz = cutoffFrequencyHz.coerceIn(10_000f, 22_000f),
        harmonicGain = harmonicGain.coerceIn(0f, 0.30f),
        transientSensitivity = transientSensitivity.coerceIn(0f, 1f),
        truePeakCeilingLinear = truePeakCeilingLinear.coerceIn(0.70f, 1.0f)
    )
}
