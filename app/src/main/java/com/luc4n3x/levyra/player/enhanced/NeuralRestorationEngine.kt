package com.luc4n3x.levyra.player.enhanced

import java.io.File

/**
 * Neural restoration backend reserved for a future on-device inference model.
 *
 * Until real inference is implemented this engine stays unavailable so the
 * processor always selects the production DSP restoration path.
 */
class NeuralRestorationEngine(
    private val modelFile: File? = null
) : EnhancedAudioEngine {
    override val name: String = "Levyra Neural Restore"

    var isAvailable: Boolean = false
        private set

    private var sampleRate: Int = 44_100
    private var channels: Int = 2
    private var config: EnhancedAudioConfig = EnhancedAudioConfig()

    init {
        // A model asset alone is not enough: inference is not implemented yet.
        isAvailable = false
    }

    override fun configure(sampleRateHz: Int, channelCount: Int, config: EnhancedAudioConfig) {
        this.sampleRate = sampleRateHz
        this.channels = channelCount
        this.config = config
    }

    override fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        adaptiveResidualGain: Float,
        stereoCoherence: Float
    ): Boolean = false

    override fun reset() = Unit

    override fun release() {
        isAvailable = false
    }
}
