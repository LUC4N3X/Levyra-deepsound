package com.luc4n3x.levyra.player.enhanced

/**
 * Real-time spectral and behavioral metrics computed by [EnhancedAudioAnalyzer].
 */
data class EnhancedAudioMetrics(
    val spectralCutoffHz: Float = 0f,
    val hfEnergyRatio: Float = 0f,
    val spectralHoleCount: Int = 0,
    val tonality: Float = 0f,
    val transientDensity: Float = 0f,
    val stereoCoherence: Float = 1f,
    val peakAmplitude: Float = 0f,
    val deficitConfidence: Float = 0f,
    val adaptiveResidualGain: Float = 0f,
    val processingTimeUs: Long = 0L,
    val bypassed: Boolean = true,
    val bypassReason: EnhancedAudioBypassReason? = EnhancedAudioBypassReason.ENGINE_NOT_READY
) {
    val isActive: Boolean
        get() = !bypassed && deficitConfidence > 0f
}
