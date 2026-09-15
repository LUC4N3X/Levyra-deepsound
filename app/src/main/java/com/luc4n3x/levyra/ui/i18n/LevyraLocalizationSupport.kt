package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal fun <T> localizedValueOrEnglish(
    values: Map<String, T>,
    code: String
): T = values[code] ?: values.getValue("en")

internal fun localizedBundleOrEnglish(
    bundles: Map<String, Map<String, String>>,
    code: String
): Map<String, String> = localizedValueOrEnglish(bundles, code)

internal fun supportedLocalizationCodes(): Set<String> =
    LevyraLanguageCatalog.languages.map { it.code }.toSet()
