package com.luc4n3x.levyra.player.enhanced

interface EnhancedAudioEngine {
    val name: String

    fun configure(sampleRateHz: Int, channelCount: Int, config: EnhancedAudioConfig)

    fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        adaptiveResidualGain: Float,
        stereoCoherence: Float = 1f
    ): Boolean

    fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        metrics: EnhancedAudioMetrics
    ): Boolean = process(input, output, offset, frames, metrics.adaptiveResidualGain, metrics.stereoCoherence)

    fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        metrics: MutableEnhancedAudioMetrics
    ): Boolean = process(input, output, offset, frames, metrics.adaptiveResidualGain, metrics.stereoCoherence)

    fun reset()

    fun release()
}
