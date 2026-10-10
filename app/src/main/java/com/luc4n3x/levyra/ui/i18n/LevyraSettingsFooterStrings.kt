package com.luc4n3x.levyra.ui.i18n

private val settingsFooterLegalInformationLabels = mapOf(
    "en" to "Legal information",
    "it" to "Informazioni legali",
    "es" to "Información legal",
    "fr" to "Informations légales",
    "de" to "Rechtliche Informationen",
    "pt" to "Informações legais",
    "nl" to "Juridische informatie",
    "pl" to "Informacje prawne",
    "ro" to "Informații juridice",
    "el" to "Νομικές πληροφορίες",
    "sv" to "Juridisk information",
    "da" to "Juridiske oplysninger",
    "cs" to "Právní informace",
    "sk" to "Právne informácie",
    "hr" to "Pravne informacije",
    "bg" to "Правна информация",
    "hu" to "Jogi információk",
    "fi" to "Oikeudelliset tiedot",
    "et" to "Õiguslik teave",
    "nb" to "Juridisk informasjon",
    "ca" to "Informació legal",
    "uk" to "Правова інформація",
    "ru" to "Правовая информация",
    "tr" to "Yasal bilgiler",
    "ar" to "المعلومات القانونية",
    "fa" to "اطلاعات حقوقی",
    "zh" to "法律信息",
    "zh-Hant" to "法律資訊",
    "ja" to "法的情報",
    "ko" to "법률 정보",
    "hi" to "कानूनी जानकारी",
    "id" to "Informasi hukum",
    "ms" to "Maklumat undang-undang",
    "vi" to "Thông tin pháp lý",
    "th" to "ข้อมูลทางกฎหมาย",
    "fil" to "Legal na impormasyon",
    "he" to "מידע משפטי"
).also { labels ->
    require(labels.keys == supportedLocalizationCodes()) {
        "Invalid settings footer localization coverage: missing=${supportedLocalizationCodes() - labels.keys}, extra=${labels.keys - supportedLocalizationCodes()}"
    }
}

internal fun settingsFooterLegalInformation(code: String): String =
    localizedValueOrEnglish(settingsFooterLegalInformationLabels, code)

internal fun settingsFooterLocalizationCodes(): Set<String> =
    settingsFooterLegalInformationLabels.keys
