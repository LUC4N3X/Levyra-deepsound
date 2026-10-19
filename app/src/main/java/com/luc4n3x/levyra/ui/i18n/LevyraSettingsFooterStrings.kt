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
    "bn" to "আইনি তথ্য",
    "kk" to "Құқықтық ақпарат",
    "sr" to "Правне информације",
    "sq" to "Informacione ligjore",
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

private val settingsFooterExitApplicationLabels = mapOf(
        "en" to "Exit application", "it" to "Esci dall’app",
        "es" to "Salir de la aplicación", "fr" to "Quitter l’application",
        "de" to "App beenden", "pt" to "Sair da aplicação",
        "nl" to "App afsluiten", "pl" to "Zamknij aplikację",
        "ro" to "Închide aplicația", "el" to "Έξοδος από την εφαρμογή",
        "sv" to "Avsluta appen", "da" to "Afslut appen",
        "cs" to "Ukončit aplikaci", "sk" to "Ukončiť aplikáciu",
        "bn" to "অ্যাপ থেকে বের হন", "kk" to "Қолданбадан шығу",
        "sr" to "Изађи из апликације", "sq" to "Dil nga aplikacioni",
        "hr" to "Izađi iz aplikacije", "bg" to "Изход от приложението",
        "hu" to "Kilépés az alkalmazásból", "fi" to "Sulje sovellus",
        "et" to "Välju rakendusest", "nb" to "Avslutt appen",
        "ca" to "Surt de l’aplicació", "uk" to "Вийти з програми",
        "ru" to "Выйти из приложения", "tr" to "Uygulamadan çık",
        "ar" to "الخروج من التطبيق", "fa" to "خروج از برنامه",
        "zh" to "退出应用", "zh-Hant" to "結束應用程式",
        "ja" to "アプリを終了", "ko" to "앱 종료",
        "hi" to "ऐप से बाहर निकलें", "id" to "Keluar dari aplikasi",
        "ms" to "Keluar aplikasi", "vi" to "Thoát ứng dụng",
        "th" to "ออกจากแอป", "fil" to "Lumabas sa app",
        "he" to "יציאה מהאפליקציה"
)

internal fun settingsFooterExitApplication(code: String): String =
    localizedValueOrEnglish(settingsFooterExitApplicationLabels, code)
