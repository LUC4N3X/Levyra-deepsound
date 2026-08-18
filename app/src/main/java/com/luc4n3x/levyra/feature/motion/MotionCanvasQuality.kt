package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.LevyraCanvasQuality

enum class MotionCanvasSurface {
    Card,
    Immersive
}

data class MotionCanvasConditions(
    val unmetered: Boolean,
    val dataSaverActive: Boolean,
    val batterySaverActive: Boolean,
    val lowRamDevice: Boolean
)

data class MotionCanvasProfile(
    val maxDimensionPx: Int,
    val maxBitrateBps: Int,
    val forceHighestSupportedBitrate: Boolean
)

object MotionCanvasQualityPolicy {

    fun profile(
        quality: LevyraCanvasQuality,
        surface: MotionCanvasSurface,
        conditions: MotionCanvasConditions
    ): MotionCanvasProfile {
        val effective = effectiveQuality(quality, conditions)
        return when (effective) {
            LevyraCanvasQuality.DataSaver -> when (surface) {
                MotionCanvasSurface.Card -> MotionCanvasProfile(720, 1_200_000, false)
                MotionCanvasSurface.Immersive -> MotionCanvasProfile(720, 1_800_000, false)
            }
            LevyraCanvasQuality.High -> when (surface) {
                MotionCanvasSurface.Card -> MotionCanvasProfile(1_440, 8_000_000, true)
                MotionCanvasSurface.Immersive -> MotionCanvasProfile(2_160, 20_000_000, true)
            }
            LevyraCanvasQuality.Auto -> when (surface) {
                // Keep the default path identical to main so optional motion artwork
                // cannot become more aggressive than the proven playback baseline.
                MotionCanvasSurface.Card -> MotionCanvasProfile(1_280, 4_000_000, false)
                MotionCanvasSurface.Immersive -> MotionCanvasProfile(1_920, 8_000_000, false)
            }
        }
    }

    internal fun effectiveQuality(
        quality: LevyraCanvasQuality,
        conditions: MotionCanvasConditions
    ): LevyraCanvasQuality {
        if (conditions.batterySaverActive || conditions.lowRamDevice) return LevyraCanvasQuality.DataSaver
        if (conditions.dataSaverActive) return LevyraCanvasQuality.DataSaver
        return when (quality) {
            LevyraCanvasQuality.DataSaver -> LevyraCanvasQuality.DataSaver
            LevyraCanvasQuality.High ->
                if (conditions.unmetered) LevyraCanvasQuality.High else LevyraCanvasQuality.Auto
            LevyraCanvasQuality.Auto ->
                if (conditions.unmetered) LevyraCanvasQuality.Auto else LevyraCanvasQuality.DataSaver
        }
    }
}
