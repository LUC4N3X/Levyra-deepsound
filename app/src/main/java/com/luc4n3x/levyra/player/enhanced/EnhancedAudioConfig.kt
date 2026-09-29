package com.luc4n3x.levyra.player.enhanced

data class EnhancedAudioConfig(
    val deficitThreshold: Float = 0.15f,
    val truePeakCeilingLinear: Float = 0.944f,
    val maxAllowedProcessingTimeUs: Long = 5_000L
) {
    fun normalized(): EnhancedAudioConfig = copy(
        deficitThreshold = deficitThreshold.coerceIn(0.01f, 0.99f),
        truePeakCeilingLinear = truePeakCeilingLinear.coerceIn(0.70f, 1.0f)
    )
}
