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
        LevyraLanguageOption("pt", "🇵🇹", "Portuguese (Portugal)", "Português (Portugal)"),
        LevyraLanguageOption("nl", "🇳🇱", "Dutch", "Nederlands"),
        LevyraLanguageOption("pl", "🇵🇱", "Polish", "Polski"),
        LevyraLanguageOption("ro", "🇷🇴", "Romanian", "Română"),
        LevyraLanguageOption("el", "🇬🇷", "Greek", "Ελληνικά"),
        LevyraLanguageOption("sv", "🇸🇪", "Swedish", "Svenska"),
        LevyraLanguageOption("da", "🇩🇰", "Danish", "Dansk"),
        LevyraLanguageOption("cs", "🇨🇿", "Czech", "Čeština"),
        LevyraLanguageOption("sk", "🇸🇰", "Slovak", "Slovenčina"),
        LevyraLanguageOption("hr", "🇭🇷", "Croatian", "Hrvatski"),
        LevyraLanguageOption("sr", "🇷🇸", "Serbian", "Српски"),
        LevyraLanguageOption("sq", "🇦🇱", "Albanian", "Shqip"),
        LevyraLanguageOption("bg", "🇧🇬", "Bulgarian", "Български"),
        LevyraLanguageOption("hu", "🇭🇺", "Hungarian", "Magyar"),
        LevyraLanguageOption("fi", "🇫🇮", "Finnish", "Suomi"),
        LevyraLanguageOption("et", "🇪🇪", "Estonian", "Eesti"),
        LevyraLanguageOption("nb", "🇳🇴", "Norwegian Bokmål", "Norsk bokmål"),
        LevyraLanguageOption("ca", "🇪🇸", "Catalan", "Català"),
        LevyraLanguageOption("uk", "🇺🇦", "Ukrainian", "Українська"),
        LevyraLanguageOption("ru", "🇷🇺", "Russian", "Русский"),
        LevyraLanguageOption("kk", "🇰🇿", "Kazakh", "Қазақша"),
        LevyraLanguageOption("tr", "🇹🇷", "Turkish", "Türkçe"),
        LevyraLanguageOption("ar", "🇸🇦", "Arabic", "العربية"),
        LevyraLanguageOption("fa", "🇮🇷", "Persian", "فارسی"),
        LevyraLanguageOption("zh", "🇨🇳", "Chinese (Simplified)", "简体中文"),
        LevyraLanguageOption("zh-Hant", "🇹🇼", "Chinese (Traditional)", "繁體中文"),
        LevyraLanguageOption("ja", "🇯🇵", "Japanese", "日本語"),
        LevyraLanguageOption("ko", "🇰🇷", "Korean", "한국어"),
        LevyraLanguageOption("hi", "🇮🇳", "Hindi", "हिन्दी"),
        LevyraLanguageOption("bn", "🇧🇩", "Bengali", "বাংলা"),
        LevyraLanguageOption("id", "🇮🇩", "Indonesian", "Bahasa Indonesia"),
        LevyraLanguageOption("ms", "🇲🇾", "Malay", "Bahasa Melayu"),
        LevyraLanguageOption("vi", "🇻🇳", "Vietnamese", "Tiếng Việt"),
        LevyraLanguageOption("th", "🇹🇭", "Thai", "ไทย"),
        LevyraLanguageOption("fil", "🇵🇭", "Filipino", "Filipino"),
        LevyraLanguageOption("he", "🇮🇱", "Hebrew", "עברית")
    )

    const val BRAZILIAN_PORTUGUESE = "pt-BR"

    val selectableLanguages: List<LevyraLanguageOption> = languages.flatMap { language ->
        if (language.code == "pt") {
            listOf(language, LevyraLanguageOption(BRAZILIAN_PORTUGUESE, "🇧🇷", "Portuguese (Brazil)", "Português (Brasil)"))
        } else {
            listOf(language)
        }
    }

    private val supportedCodes = selectableLanguages.map { it.code }.toSet()
    private val androidRegionQualifier = Regex("^([A-Za-z]{2,3})-r([A-Za-z]{2})$")
    private val rtlCodes = setOf("ar", "fa", "he")
    private val traditionalChineseRegions = setOf("TW", "HK", "MO")

    fun normalize(code: String): String {
        if (code in supportedCodes) return code
        val rawTag = androidRegionQualifier.replace(code.trim().replace('_', '-'), "$1-$2")
        if (rawTag.isBlank()) return "en"
        val locale = Locale.forLanguageTag(rawTag)
        val language = when (locale.language.lowercase(Locale.ROOT)) {
            "in" -> "id"
            "tl" -> "fil"
            "iw" -> "he"
            else -> locale.language.lowercase(Locale.ROOT)
        }
        val canonical = when {
            language == "zh" && (
                locale.script.equals("Hant", ignoreCase = true) ||
                    locale.country.uppercase(Locale.ROOT) in traditionalChineseRegions
            ) -> "zh-Hant"
            language == "zh" -> "zh"
            language == "pt" && locale.country.equals("BR", ignoreCase = true) -> BRAZILIAN_PORTUGUESE
            else -> language
        }
        return if (canonical in supportedCodes) canonical else "en"
    }

    fun translationCode(code: String): String {
        val normalized = normalize(code)
        return if (normalized == BRAZILIAN_PORTUGUESE) "pt" else normalized
    }

    fun deviceDefault(): String = normalize(Locale.getDefault().toLanguageTag())

    fun displayName(code: String): String {
        val language = selectableLanguages.firstOrNull { it.code == normalize(code) }
        return language?.let { "${it.flag} ${it.nativeName}" } ?: "🇬🇧 English"
    }

    fun isRtl(code: String): Boolean = normalize(code) in rtlCodes
}
