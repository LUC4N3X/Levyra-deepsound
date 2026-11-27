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

    val selectableLanguages: List<LevyraLanguageOption> = AppLanguage.entries.map { language ->
        LevyraLanguageOption(
            code = language.tag,
            flag = language.flag,
            englishName = language.englishName,
            nativeName = language.nativeName
        )
    }

    val languages: List<LevyraLanguageOption> = selectableLanguages.filterNot { it.code == BRAZILIAN_PORTUGUESE }

    private val rtlCodes = AppLanguage.entries.filter { it.isRtl }.map { it.tag }.toSet()

    fun normalize(code: String): String {
        return AppLanguage.fromTag(code).tag
    }

    fun translationCode(code: String): String = normalize(code).let { normalized ->
        if (normalized == BRAZILIAN_PORTUGUESE) "pt" else normalized
    }

    fun deviceDefault(): String = normalize(Locale.getDefault().toLanguageTag())

    fun displayName(code: String): String {
        val language = AppLanguage.fromTag(code)
        return "${language.flag} ${language.nativeName}"
    }

    fun isRtl(code: String): Boolean = normalize(code) in rtlCodes
}
