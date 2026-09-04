package com.luc4n3x.levyra.domain

data class LevyraAmbientSettings(
    val brightness: Float = 0.35f,
    val autoDim: Boolean = true,
    val autoDimAfterSeconds: Int = 20,
    val pixelShift: Boolean = true,
    val proximityBlackout: Boolean = false,
    val showLyrics: Boolean = true,
    val showCanvas: Boolean = true
) {
    fun normalized(): LevyraAmbientSettings = copy(
        brightness = brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS),
        autoDimAfterSeconds = autoDimAfterSeconds.coerceIn(MIN_AUTO_DIM_SECONDS, MAX_AUTO_DIM_SECONDS)
    )

    val autoDimAfterMs: Long
        get() = autoDimAfterSeconds.coerceIn(MIN_AUTO_DIM_SECONDS, MAX_AUTO_DIM_SECONDS) * 1_000L

    companion object {
        const val MIN_BRIGHTNESS = 0.05f
        const val MAX_BRIGHTNESS = 1f
        const val MIN_AUTO_DIM_SECONDS = 5
        const val MAX_AUTO_DIM_SECONDS = 120
        const val DIMMED_CONTENT_ALPHA = 0.45f
        const val PIXEL_SHIFT_RANGE_DP = 10f
        const val PIXEL_SHIFT_INTERVAL_MS = 60_000L
        const val CONTROLS_VISIBLE_MS = 6_000L
    }
}

fun ambientPixelShiftOffset(
    stepIndex: Int,
    rangeDp: Float = LevyraAmbientSettings.PIXEL_SHIFT_RANGE_DP
): Pair<Float, Float> {
    if (rangeDp <= 0f) return 0f to 0f
    val step = if (stepIndex < 0) 0 else stepIndex
    val pattern = PIXEL_SHIFT_PATTERN[step % PIXEL_SHIFT_PATTERN.size]
    return pattern.first * rangeDp to pattern.second * rangeDp
}

private val PIXEL_SHIFT_PATTERN = arrayOf(
    0f to 0f,
    1f to -0.6f,
    0.4f to 1f,
    -0.7f to 0.5f,
    -1f to -0.4f,
    0.2f to -1f
)
