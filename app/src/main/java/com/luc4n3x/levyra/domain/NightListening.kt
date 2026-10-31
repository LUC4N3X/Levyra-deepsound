package com.luc4n3x.levyra.domain

enum class NightListeningPreset(
    val storageValue: String,
    val thresholdDb: Float,
    val ratio: Float,
    val kneeDb: Float,
    val attackMs: Float,
    val releaseMs: Float,
    val makeupDb: Float
) {
    LIGHT("light", -22f, 1.6f, 8f, 18f, 220f, 1.5f),
    BALANCED("balanced", -26f, 2.4f, 8f, 12f, 200f, 3f),
    INTENSE("intense", -30f, 3.6f, 10f, 8f, 170f, 5f),
    NIGHT("night", -34f, 5.5f, 12f, 5f, 150f, 7f);

    companion object {
        fun fromStorage(value: String?): NightListeningPreset =
            entries.firstOrNull { it.storageValue == value?.trim()?.lowercase() } ?: BALANCED
    }
}

object NightListeningCurve {
    const val MIN_TRIM_DB = -6f
    const val MAX_TRIM_DB = 3f

    fun gainReductionDb(levelDb: Float, preset: NightListeningPreset): Float =
        gainReductionDb(levelDb, preset.thresholdDb, preset.ratio, preset.kneeDb)

    fun gainReductionDb(levelDb: Float, thresholdDb: Float, ratio: Float, kneeDb: Float): Float {
        val over = levelDb - thresholdDb
        val slope = 1f - 1f / ratio
        val halfKnee = kneeDb / 2f
        return when {
            over <= -halfKnee -> 0f
            over >= halfKnee -> slope * over
            else -> {
                val position = over + halfKnee
                slope * position * position / (2f * kneeDb)
            }
        }
    }

    fun outputDb(levelDb: Float, preset: NightListeningPreset, trimDb: Float = 0f): Float =
        levelDb - gainReductionDb(levelDb, preset) + preset.makeupDb + trimDb
}
