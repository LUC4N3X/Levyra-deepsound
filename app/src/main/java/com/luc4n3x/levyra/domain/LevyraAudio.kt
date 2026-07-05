package com.luc4n3x.levyra.domain

data class LevyraAudioPreset(
    val id: String,
    val label: String,
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
    const val bandCount = 10

    val flatLevels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    val presets = listOf(
        LevyraAudioPreset(FLAT, "Flat", flatLevels, 0, 0),
        LevyraAudioPreset(BASS_BOOST, "Bass Boost", listOf(72, 58, 38, 18, 4, 0, 8, 16, 22, 24), 72, 18),
        LevyraAudioPreset(VOCAL, "Vocal", listOf(-20, -12, 0, 24, 48, 54, 42, 20, 8, 0), 8, 6),
        LevyraAudioPreset(NIGHT, "Night", listOf(-24, -18, -8, 4, 10, 12, 6, -2, -8, -16), 0, 0),
        LevyraAudioPreset(GYM, "Gym", listOf(76, 64, 42, 18, 4, 6, 22, 42, 56, 48), 80, 34),
        LevyraAudioPreset(CAR, "Car", listOf(44, 38, 26, 10, 0, 8, 24, 34, 38, 32), 48, 22)
    )

    fun normalizePreset(id: String): String = presets.firstOrNull { it.id == id }?.id ?: FLAT

    fun preset(id: String): LevyraAudioPreset = presets.firstOrNull { it.id == normalizePreset(id) } ?: presets.first()

    fun levelsFor(id: String): List<Int> = preset(id).levels

    fun labelFor(id: String): String = preset(id).label
}
