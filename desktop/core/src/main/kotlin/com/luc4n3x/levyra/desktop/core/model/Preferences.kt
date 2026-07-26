package com.luc4n3x.levyra.desktop.core.model

import kotlinx.serialization.Serializable

enum class AudioQuality {
    LOW,
    BALANCED,
    HIGH
}

enum class PreferredCodec {
    AUTO,
    OPUS,
    AAC
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val tag: String) {
    ITALIAN("it"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH
    }
}

@Serializable
data class EqualizerSettings(
    val enabled: Boolean = false,
    val preamp: Float = 0f,
    val amps: List<Float> = emptyList()
) {
    fun sanitized(): EqualizerSettings = copy(
        preamp = preamp.coerceIn(MIN_GAIN, MAX_GAIN),
        amps = amps.map { it.coerceIn(MIN_GAIN, MAX_GAIN) }
    )

    fun ampAt(index: Int): Float = amps.getOrElse(index) { 0f }

    fun withAmp(index: Int, value: Float): EqualizerSettings {
        val updated = MutableList(BAND_FREQUENCIES.size) { position -> ampAt(position) }
        if (index in updated.indices) {
            updated[index] = value.coerceIn(MIN_GAIN, MAX_GAIN)
        }
        return copy(amps = updated)
    }

    fun flattened(): EqualizerSettings = copy(preamp = 0f, amps = emptyList())

    companion object {
        const val MIN_GAIN = -20f
        const val MAX_GAIN = 20f

        val BAND_FREQUENCIES = listOf(60f, 170f, 310f, 600f, 1_000f, 3_000f, 6_000f, 12_000f, 14_000f, 16_000f)

        fun bandLabel(frequency: Float): String =
            if (frequency >= 1_000f) "${(frequency / 1_000f).toInt()}k" else frequency.toInt().toString()
    }
}

@Serializable
data class DesktopSettings(
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val preferredCodec: PreferredCodec = PreferredCodec.AUTO,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ITALIAN,
    val contentCountry: String = "IT",
    val volume: Int = 80,
    val autoplayRadio: Boolean = true,
    val resumeOnStartup: Boolean = true,
    val minimizeToTray: Boolean = true,
    val vlcDirectory: String = "",
    val equalizer: EqualizerSettings = EqualizerSettings()
) {
    fun sanitized(): DesktopSettings = copy(
        contentCountry = contentCountry.trim().uppercase().take(2).ifBlank { "IT" },
        volume = volume.coerceIn(0, 100),
        vlcDirectory = vlcDirectory.trim(),
        equalizer = equalizer.sanitized()
    )
}
