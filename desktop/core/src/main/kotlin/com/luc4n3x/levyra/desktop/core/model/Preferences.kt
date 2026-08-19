package com.luc4n3x.levyra.desktop.core.model

import java.util.Locale
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

enum class AppLanguage(
    val tag: String,
    val flag: String,
    val nativeName: String,
    val englishName: String,
    val defaultCountry: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "🇬🇧", "English", "English", "GB"),
    ITALIAN("it", "🇮🇹", "Italiano", "Italian", "IT"),
    SPANISH("es", "🇪🇸", "Español", "Spanish", "ES"),
    FRENCH("fr", "🇫🇷", "Français", "French", "FR"),
    GERMAN("de", "🇩🇪", "Deutsch", "German", "DE"),
    PORTUGUESE("pt", "🇵🇹", "Português", "Portuguese", "PT"),
    DUTCH("nl", "🇳🇱", "Nederlands", "Dutch", "NL"),
    POLISH("pl", "🇵🇱", "Polski", "Polish", "PL"),
    ROMANIAN("ro", "🇷🇴", "Română", "Romanian", "RO"),
    GREEK("el", "🇬🇷", "Ελληνικά", "Greek", "GR"),
    SWEDISH("sv", "🇸🇪", "Svenska", "Swedish", "SE"),
    DANISH("da", "🇩🇰", "Dansk", "Danish", "DK"),
    CZECH("cs", "🇨🇿", "Čeština", "Czech", "CZ"),
    UKRAINIAN("uk", "🇺🇦", "Українська", "Ukrainian", "UA"),
    RUSSIAN("ru", "🇷🇺", "Русский", "Russian", "RU"),
    TURKISH("tr", "🇹🇷", "Türkçe", "Turkish", "TR"),
    ARABIC("ar", "🇸🇦", "العربية", "Arabic", "SA", true),
    CHINESE("zh", "🇨🇳", "简体中文", "Chinese (Simplified)", "CN"),
    JAPANESE("ja", "🇯🇵", "日本語", "Japanese", "JP"),
    KOREAN("ko", "🇰🇷", "한국어", "Korean", "KR"),
    HINDI("hi", "🇮🇳", "हिन्दी", "Hindi", "IN"),
    INDONESIAN("id", "🇮🇩", "Bahasa Indonesia", "Indonesian", "ID"),
    VIETNAMESE("vi", "🇻🇳", "Tiếng Việt", "Vietnamese", "VN"),
    THAI("th", "🇹🇭", "ไทย", "Thai", "TH"),
    FILIPINO("fil", "🇵🇭", "Filipino", "Filipino", "PH"),
    HEBREW("he", "🇮🇱", "עברית", "Hebrew", "IL", true);

    val displayLabel: String get() = "$flag $nativeName"

    companion object {
        fun fromTag(tag: String): AppLanguage {
            val normalized = tag
                .trim()
                .replace('_', '-')
                .substringBefore('-')
                .lowercase(Locale.ROOT)
            val canonical = when (normalized) {
                "in" -> "id"
                "tl" -> "fil"
                "iw" -> "he"
                else -> normalized
            }
            return entries.firstOrNull { it.tag == canonical } ?: ENGLISH
        }

        fun deviceDefault(): AppLanguage = fromTag(Locale.getDefault().language)
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
    val language: AppLanguage = AppLanguage.deviceDefault(),
    val contentCountry: String = AppLanguage.deviceDefault().defaultCountry,
    val displayName: String = "",
    val selectedTasteIds: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false,
    val volume: Int = 80,
    val autoplayRadio: Boolean = true,
    val resumeOnStartup: Boolean = true,
    val minimizeToTray: Boolean = true,
    val preloadNextTrack: Boolean = true,
    val globalMediaKeys: Boolean = true,
    val playbackSpeed: Float = DEFAULT_SPEED,
    val vlcDirectory: String = "",
    val audioOutputDeviceId: String = "",
    val equalizer: EqualizerSettings = EqualizerSettings()
) {
    fun sanitized(): DesktopSettings = copy(
        contentCountry = contentCountry.trim().uppercase(Locale.ROOT).take(2).ifBlank { language.defaultCountry },
        displayName = displayName.trim().take(MAX_DISPLAY_NAME_LENGTH),
        selectedTasteIds = selectedTasteIds.map { it.trim().lowercase(Locale.ROOT) }
            .filter { it in VALID_TASTE_IDS }
            .toSet(),
        volume = volume.coerceIn(0, 100),
        playbackSpeed = normalizeSpeed(playbackSpeed),
        vlcDirectory = vlcDirectory.trim(),
        audioOutputDeviceId = audioOutputDeviceId.trim().take(MAX_OUTPUT_DEVICE_ID_LENGTH),
        equalizer = equalizer.sanitized()
    )

    companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 40
        const val MAX_OUTPUT_DEVICE_ID_LENGTH = 512
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
        const val DEFAULT_SPEED = 1f

        val SPEED_STEPS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

        fun normalizeSpeed(value: Float): Float =
            if (value.isNaN()) DEFAULT_SPEED else value.coerceIn(MIN_SPEED, MAX_SPEED)
        val VALID_TASTE_IDS = setOf(
            "hits",
            "rap",
            "local",
            "pop",
            "gym",
            "chill",
            "focus",
            "sad",
            "party",
            "rock",
            "electro",
            "rnb"
        )
    }
}
