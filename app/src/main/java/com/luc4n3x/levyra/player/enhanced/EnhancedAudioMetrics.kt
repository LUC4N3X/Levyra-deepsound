package com.luc4n3x.levyra.player.enhanced

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

class MutableEnhancedAudioMetrics(
    var spectralCutoffHz: Float = 0f,
    var hfEnergyRatio: Float = 0f,
    var spectralHoleCount: Int = 0,
    var tonality: Float = 0f,
    var transientDensity: Float = 0f,
    var stereoCoherence: Float = 1f,
    var peakAmplitude: Float = 0f,
    var deficitConfidence: Float = 0f,
    var adaptiveResidualGain: Float = 0f,
    var processingTimeUs: Long = 0L,
    var bypassed: Boolean = true,
    var bypassReason: EnhancedAudioBypassReason? = EnhancedAudioBypassReason.ENGINE_NOT_READY
) {
    val isActive: Boolean
        get() = !bypassed && deficitConfidence > 0f

    fun toSnapshot(): EnhancedAudioMetrics = EnhancedAudioMetrics(
        spectralCutoffHz = spectralCutoffHz,
        hfEnergyRatio = hfEnergyRatio,
        spectralHoleCount = spectralHoleCount,
        tonality = tonality,
        transientDensity = transientDensity,
        stereoCoherence = stereoCoherence,
        peakAmplitude = peakAmplitude,
        deficitConfidence = deficitConfidence,
        adaptiveResidualGain = adaptiveResidualGain,
        processingTimeUs = processingTimeUs,
        bypassed = bypassed,
        bypassReason = bypassReason
    )

    fun reset() {
        spectralCutoffHz = 0f
        hfEnergyRatio = 0f
        spectralHoleCount = 0
        tonality = 0f
        transientDensity = 0f
        stereoCoherence = 1f
        peakAmplitude = 0f
        deficitConfidence = 0f
        adaptiveResidualGain = 0f
        processingTimeUs = 0L
        bypassed = true
        bypassReason = EnhancedAudioBypassReason.ENGINE_NOT_READY
    }

    fun setBypass(reason: EnhancedAudioBypassReason?) {
        bypassed = reason != null
        bypassReason = reason
        adaptiveResidualGain = 0f
    }
}
