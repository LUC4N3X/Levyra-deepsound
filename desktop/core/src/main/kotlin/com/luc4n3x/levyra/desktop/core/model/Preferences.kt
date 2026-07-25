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
    val vlcDirectory: String = ""
) {
    fun sanitized(): DesktopSettings = copy(
        contentCountry = contentCountry.trim().uppercase().take(2).ifBlank { "IT" },
        volume = volume.coerceIn(0, 100),
        vlcDirectory = vlcDirectory.trim()
    )
}
