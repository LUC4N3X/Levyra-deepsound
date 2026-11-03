package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import java.util.Locale

data class LevyraLanguageOption(
    val code: String,
    val flag: String,
    val englishName: String,
    val nativeName: String
)

object LevyraLanguageCatalog {
    const val BRAZILIAN_PORTUGUESE = "pt-BR"

    val languages: List<LevyraLanguageOption> = AppLanguage.entries.map { language ->
        LevyraLanguageOption(
            code = language.tag,
            flag = language.flag,
            englishName = language.englishName,
            nativeName = language.nativeName
        )
    }

    private val supportedCodes = languages.map { it.code }.toSet()
    private val rtlCodes = AppLanguage.entries.filter { it.isRtl }.map { it.tag }.toSet()

    fun normalize(code: String): String {
        val tag = code.trim().replace('_', '-')
        if (tag.equals(BRAZILIAN_PORTUGUESE, ignoreCase = true) ||
            tag.equals("pt-rBR", ignoreCase = true)
        ) return BRAZILIAN_PORTUGUESE
        val normalized = tag
            .substringBefore('-')
            .lowercase(Locale.ROOT)
        val canonical = when (normalized) {
            "in" -> "id"
            "tl" -> "fil"
            "iw" -> "he"
            else -> normalized
        }
        return if (canonical in supportedCodes) canonical else "en"
    }

    fun translationCode(code: String): String = normalize(code).let { normalized ->
        if (normalized == BRAZILIAN_PORTUGUESE) "pt" else normalized
    }

    fun deviceDefault(): String = normalize(Locale.getDefault().language)

    fun displayName(code: String): String {
        val language = languages.firstOrNull { it.code == normalize(code) }
        return language?.let { "${it.flag} ${it.nativeName}" } ?: "🇬🇧 English"
    }

    fun isRtl(code: String): Boolean = normalize(code) in rtlCodes
}
