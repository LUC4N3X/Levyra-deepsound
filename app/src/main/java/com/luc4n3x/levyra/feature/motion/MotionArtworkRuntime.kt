package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.core.config.RuntimeConfigSnapshot
import com.luc4n3x.levyra.core.config.VersionedRuntimeConfig

data class MotionArtworkConfig(
    val providerOrder: List<String> = listOf("apple-motion", "tidal-video-cover"),
    val minimumConfidence: Int = 84,
    val requestTimeoutMs: Long = 6_500L,
    val positiveTtlMs: Long = MOTION_ARTWORK_POSITIVE_TTL_MS,
    val negativeTtlMs: Long = MOTION_ARTWORK_NEGATIVE_TTL_MS
) {
    fun normalized(): MotionArtworkConfig = copy(
        providerOrder = providerOrder.distinct(),
        minimumConfidence = minimumConfidence.coerceIn(70, 100),
        requestTimeoutMs = requestTimeoutMs.coerceIn(2_500L, 12_000L),
        positiveTtlMs = positiveTtlMs.coerceIn(60L * 60L * 1000L, 7L * 24L * 60L * 60L * 1000L),
        negativeTtlMs = negativeTtlMs.coerceIn(10L * 60L * 1000L, 6L * 60L * 60L * 1000L)
    )
}

object MotionArtworkRuntime {
    private val config = VersionedRuntimeConfig(MotionArtworkConfig().normalized())

    fun snapshot(): RuntimeConfigSnapshot<MotionArtworkConfig> = config.snapshot()

    fun update(value: MotionArtworkConfig): RuntimeConfigSnapshot<MotionArtworkConfig> =
        config.update(value.normalized())
}
