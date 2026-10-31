package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal fun translationKey(code: String): String = LevyraLanguageCatalog.translationCode(code)

internal fun isBrazilianPortuguese(code: String): Boolean =
    LevyraLanguageCatalog.normalize(code) == LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE

internal inline fun <T> T.forBrazil(code: String, override: T.() -> T): T =
    if (isBrazilianPortuguese(code)) override() else this

internal fun <T> localizedValueOrEnglish(
    values: Map<String, T>,
    code: String
): T {
    values[code]?.let { return it }
    val translated = values[translationKey(code)] ?: return values.getValue("en")
    return if (isBrazilianPortuguese(code)) LevyraBrazilianPortuguese.adaptValue(translated) else translated
}

internal fun localizedBundleOrEnglish(
    bundles: Map<String, Map<String, String>>,
    code: String
): Map<String, String> = localizedValueOrEnglish(bundles, code)

internal fun supportedLocalizationCodes(): Set<String> =
    LevyraLanguageCatalog.languages.map { it.code }.toSet()
