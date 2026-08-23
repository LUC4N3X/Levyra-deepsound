package com.luc4n3x.levyra.domain

data class LevyraAudioPreset(
    val id: String,
    val fallbackLabel: String,
    val levels: List<Int>,
    val bassBoost: Int,
    val virtualizer: Int
)

data class LevyraAudioSettings(
    val equalizerEnabled: Boolean = false,
    val presetId: String = LevyraAudioPresets.FLAT,
    val bandLevels: List<Int> = LevyraAudioPresets.flatLevels,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val preampDb: Float = 0f,
    val limiterEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val djSoftMode: Boolean = false,
    val replayGainEnabled: Boolean = false,
    val playbackSpeed: Float = 1f,
    val pitch: Float = 1f,
    val gaplessEnabled: Boolean = true
) {
    fun normalized(): LevyraAudioSettings {
        val preset = LevyraAudioPresets.normalizePreset(presetId)
        val levels = bandLevels.takeIf { it.size == LevyraAudioPresets.bandCount } ?: LevyraAudioPresets.levelsFor(preset)
        return copy(
            presetId = preset,
            bandLevels = levels.map { it.coerceIn(-100, 100) },
            bassBoost = bassBoost.coerceIn(0, 100),
            virtualizer = virtualizer.coerceIn(0, 100),
            preampDb = preampDb.coerceIn(-12f, 3f),
            crossfadeSeconds = crossfadeSeconds.coerceIn(0, 12),
            playbackSpeed = playbackSpeed.coerceIn(0.5f, 2.0f),
            pitch = pitch.coerceIn(0.5f, 2.0f)
        )
    }
}

object LevyraAudioPresets {
    const val FLAT = "flat"
    const val BASS_BOOST = "bass_boost"
    const val VOCAL = "vocal"
    const val NIGHT = "night"
    const val GYM = "gym"
    const val CAR = "car"
    const val ROCK = "rock"
    const val POP = "pop"
    const val ELECTRONIC = "electronic"
    const val JAZZ = "jazz"
    const val ACOUSTIC = "acoustic"
    const val CLASSICAL = "classical"
    const val AIRPODS_PRO = "autoeq_airpods_pro"
    const val SONY_XM4 = "autoeq_sony_xm4"
    const val SONY_XM5 = "autoeq_sony_xm5"
    const val SENNHEISER_HD600 = "autoeq_hd600"
    const val bandCount = 10
    const val maxBandDb = 12f

    val bandFrequencyLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    fun bandDb(level: Int): Float = level.coerceIn(-100, 100) / 100f * maxBandDb

    fun bandLevelFromVerticalFraction(fraction: Float): Int =
        ((1f - 2f * fraction.coerceIn(0f, 1f)) * 100f).toInt().coerceIn(-100, 100)

    val flatLevels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    val presets = listOf(
        LevyraAudioPreset(FLAT, "Flat", flatLevels, 0, 0),
        LevyraAudioPreset(BASS_BOOST, "Bass Boost", listOf(72, 58, 38, 18, 4, 0, 8, 16, 22, 24), 72, 18),
        LevyraAudioPreset(VOCAL, "Vocal", listOf(-20, -12, 0, 24, 48, 54, 42, 20, 8, 0), 8, 6),
        LevyraAudioPreset(NIGHT, "Night", listOf(-24, -18, -8, 4, 10, 12, 6, -2, -8, -16), 0, 0),
        LevyraAudioPreset(GYM, "Gym", listOf(76, 64, 42, 18, 4, 6, 22, 42, 56, 48), 80, 34),
        LevyraAudioPreset(CAR, "Car", listOf(44, 38, 26, 10, 0, 8, 24, 34, 38, 32), 48, 22),
        LevyraAudioPreset(ROCK, "Rock", listOf(52, 38, 16, -8, -14, 0, 18, 32, 44, 50), 50, 15),
        LevyraAudioPreset(POP, "Pop", listOf(-10, 24, 42, 36, 12, -8, -12, 14, 30, 22), 30, 10),
        LevyraAudioPreset(ELECTRONIC, "Electronic", listOf(68, 54, 28, 0, -16, 12, 24, 42, 58, 62), 65, 25),
        LevyraAudioPreset(JAZZ, "Jazz", listOf(24, 16, 8, 12, -10, -10, 0, 14, 28, 34), 20, 10),
        LevyraAudioPreset(ACOUSTIC, "Acoustic", listOf(28, 18, 10, 12, 18, 14, 22, 30, 26, 18), 15, 5),
        LevyraAudioPreset(CLASSICAL, "Classical", listOf(32, 24, 16, 8, -4, -4, 0, 16, 24, 28), 10, 15),
        LevyraAudioPreset(AIRPODS_PRO, "AirPods Pro · Device tune", listOf(-12, -6, 4, 8, 2, -4, 6, 12, 8, -4), 10, 10),
        LevyraAudioPreset(SONY_XM4, "Sony WH-1000XM4 · Device tune", listOf(-28, -18, -8, 2, 8, 6, 4, 14, 10, -8), 0, 10),
        LevyraAudioPreset(SONY_XM5, "Sony WH-1000XM5 · Device tune", listOf(-22, -14, -4, 4, 6, 4, 6, 12, 6, -6), 0, 10),
        LevyraAudioPreset(SENNHEISER_HD600, "Sennheiser HD600 · Device tune", listOf(42, 32, 14, 2, -2, -4, 2, 8, 4, -12), 35, 5)
    )

    fun normalizePreset(id: String): String = presets.firstOrNull { it.id == id }?.id ?: FLAT

    fun preset(id: String): LevyraAudioPreset = presets.firstOrNull { it.id == normalizePreset(id) } ?: presets.first()

    fun levelsFor(id: String): List<Int> = preset(id).levels

    fun labelFor(id: String): String = preset(id).fallbackLabel
}
