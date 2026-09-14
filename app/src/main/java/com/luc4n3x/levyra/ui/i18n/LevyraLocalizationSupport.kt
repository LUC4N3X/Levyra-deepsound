package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal fun localizedBundleOrEnglish(
    bundles: Map<String, Map<String, String>>,
    code: String
): Map<String, String> = bundles[code] ?: bundles.getValue("en")

internal fun supportedLocalizationCodes(): Set<String> =
    LevyraLanguageCatalog.languages.map { it.code }.toSet()
