package com.luc4n3x.levyra.desktop.core.model

enum class EqualizerPreset(val id: String, val gains: List<Float>) {
    FLAT("flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("bass_boost", listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
    VOCAL("vocal", listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, 0f)),
    ROCK("rock", listOf(5f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 4f, 4f)),
    POP("pop", listOf(-1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f, -1f)),
    ELECTRONIC("electronic", listOf(5f, 4f, 1f, 0f, -2f, 1f, 1f, 3f, 4f, 5f)),
    HIP_HOP("hip_hop", listOf(6f, 5f, 2f, 3f, -1f, -1f, 1f, 2f, 3f, 3f)),
    CLASSICAL("classical", listOf(4f, 3f, 2f, 0f, 0f, 0f, -1f, -2f, -2f, -3f)),
    ACOUSTIC("acoustic", listOf(4f, 3f, 2f, 1f, 1f, 1f, 2f, 3f, 3f, 2f)),
    NIGHT("night", listOf(-4f, -3f, -1f, 1f, 3f, 3f, 2f, 0f, -2f, -3f));

    val preamp: Float
        get() = 0f - (gains.maxOrNull() ?: 0f).coerceIn(0f, MAX_HEADROOM)

    fun applyTo(settings: EqualizerSettings): EqualizerSettings =
        settings.copy(preamp = preamp, amps = gains).sanitized()

    companion object {
        const val CUSTOM_ID = "custom"
        private const val MAX_HEADROOM = 12f
        private const val MATCH_TOLERANCE = 0.05f

        fun fromId(id: String): EqualizerPreset? = entries.firstOrNull { it.id == id }

        fun matching(settings: EqualizerSettings): EqualizerPreset? {
            val amps = List(EqualizerSettings.BAND_FREQUENCIES.size) { settings.ampAt(it) }
            return entries.firstOrNull { preset ->
                closeEnough(preset.preamp, settings.preamp) &&
                    preset.gains.indices.all { closeEnough(preset.gains[it], amps[it]) }
            }
        }

        fun selectedId(settings: EqualizerSettings): String = matching(settings)?.id ?: CUSTOM_ID

        private fun closeEnough(first: Float, second: Float): Boolean =
            kotlin.math.abs(first - second) <= MATCH_TOLERANCE
    }
}
