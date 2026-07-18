package com.luc4n3x.levyra.domain

import java.util.Locale

data class LevyraLanguageOption(
    val code: String,
    val flag: String,
    val englishName: String,
    val nativeName: String
)

object LevyraLanguageCatalog {
    val languages = listOf(
        LevyraLanguageOption("en", "🇬🇧", "English", "English"),
        LevyraLanguageOption("it", "🇮🇹", "Italian", "Italiano"),
        LevyraLanguageOption("es", "🇪🇸", "Spanish", "Español"),
        LevyraLanguageOption("fr", "🇫🇷", "French", "Français"),
        LevyraLanguageOption("de", "🇩🇪", "German", "Deutsch"),
        LevyraLanguageOption("pt", "🇵🇹", "Portuguese", "Português"),
        LevyraLanguageOption("nl", "🇳🇱", "Dutch", "Nederlands"),
        LevyraLanguageOption("pl", "🇵🇱", "Polish", "Polski"),
        LevyraLanguageOption("ro", "🇷🇴", "Romanian", "Română"),
        LevyraLanguageOption("el", "🇬🇷", "Greek", "Ελληνικά"),
        LevyraLanguageOption("sv", "🇸🇪", "Swedish", "Svenska"),
        LevyraLanguageOption("da", "🇩🇰", "Danish", "Dansk"),
        LevyraLanguageOption("cs", "🇨🇿", "Czech", "Čeština"),
        LevyraLanguageOption("uk", "🇺🇦", "Ukrainian", "Українська"),
        LevyraLanguageOption("ru", "🇷🇺", "Russian", "Русский"),
        LevyraLanguageOption("tr", "🇹🇷", "Turkish", "Türkçe"),
        LevyraLanguageOption("ar", "🇸🇦", "Arabic", "العربية"),
        LevyraLanguageOption("zh", "🇨🇳", "Chinese (Simplified)", "简体中文"),
        LevyraLanguageOption("ja", "🇯🇵", "Japanese", "日本語"),
        LevyraLanguageOption("ko", "🇰🇷", "Korean", "한국어"),
        LevyraLanguageOption("hi", "🇮🇳", "Hindi", "हिन्दी"),
        LevyraLanguageOption("id", "🇮🇩", "Indonesian", "Bahasa Indonesia"),
        LevyraLanguageOption("vi", "🇻🇳", "Vietnamese", "Tiếng Việt"),
        LevyraLanguageOption("th", "🇹🇭", "Thai", "ไทย"),
        LevyraLanguageOption("fil", "🇵🇭", "Filipino", "Filipino"),
        LevyraLanguageOption("he", "🇮🇱", "Hebrew", "עברית")
    )

    private val supportedCodes = languages.map { it.code }.toSet()
    private val rtlCodes = setOf("ar", "he")

    fun normalize(code: String): String {
        val normalized = code.trim().replace('_', '-').substringBefore('-').lowercase(Locale.ROOT)
        val canonical = when (normalized) {
            "in" -> "id"
            "tl" -> "fil"
            "iw" -> "he"
            else -> normalized
        }
        return if (canonical in supportedCodes) canonical else "en"
    }

    fun deviceDefault(): String = normalize(Locale.getDefault().language)

    fun displayName(code: String): String {
        val language = languages.firstOrNull { it.code == normalize(code) }
        return language?.let { "${it.flag} ${it.nativeName}" } ?: "🇬🇧 English"
    }

    fun isRtl(code: String): Boolean = normalize(code) in rtlCodes
}
