package com.luc4n3x.levyra.player.enhanced

/**
 * Common contract for Levyra Enhanced Audio restoration backends.
 *
 * Implementations process raw decoded PCM samples, generate residual restoration
 * signals, and blend them adaptively based on [EnhancedAudioMetrics].
 */
interface EnhancedAudioEngine {
    /**
     * Human-readable name of the engine displayed in Technical Audio Info
     * (e.g., "Levyra DSP Restoration" or "Levyra Neural Restore").
     */
    val name: String

    /**
     * Initializes or updates format configuration.
     */
    fun configure(sampleRateHz: Int, channelCount: Int, config: EnhancedAudioConfig)

    /**
     * Processes [frames] of interleaved float PCM audio from [input] at [offset],
     * writing the restored result into [output] at [offset].
     *
     * @return true if processing was successful, false if fallback is required.
     */
    fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        metrics: EnhancedAudioMetrics
    ): Boolean

    /**
     * Clears internal state (delay lines, filters, history) upon seek or track change.
     */
    fun reset()

    /**
     * Releases any native or model resources.
     */
    fun release()
}
