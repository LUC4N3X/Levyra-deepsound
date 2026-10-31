package com.luc4n3x.levyra.ui.i18n

internal data class MusicRegionCopy(val title: String, val automatic: String)

internal val musicRegionCopies = mapOf(
    "en" to MusicRegionCopy("Music region", "Automatic (follows language)"),
    "it" to MusicRegionCopy("Regione musicale", "Automatica (segue la lingua)"),
    "es" to MusicRegionCopy("Región musical", "Automática (según el idioma)"),
    "fr" to MusicRegionCopy("Région musicale", "Automatique (selon la langue)"),
    "de" to MusicRegionCopy("Musikregion", "Automatisch (folgt der Sprache)"),
    "pt" to MusicRegionCopy("Região musical", "Automática (segue o idioma)"),
    "nl" to MusicRegionCopy("Muziekregio", "Automatisch (volgt de taal)"),
    "pl" to MusicRegionCopy("Region muzyczny", "Automatycznie (według języka)"),
    "ro" to MusicRegionCopy("Regiune muzicală", "Automat (urmează limba)"),
    "el" to MusicRegionCopy("Μουσική περιοχή", "Αυτόματα (ακολουθεί τη γλώσσα)"),
    "sv" to MusicRegionCopy("Musikregion", "Automatisk (följer språket)"),
    "da" to MusicRegionCopy("Musikregion", "Automatisk (følger sproget)"),
    "cs" to MusicRegionCopy("Hudební oblast", "Automaticky (podle jazyka)"),
    "sk" to MusicRegionCopy("Hudobný región", "Automaticky (podľa jazyka)"),
    "hr" to MusicRegionCopy("Glazbena regija", "Automatski (prema jeziku)"),
    "sr" to MusicRegionCopy("Музички регион", "Аутоматски (према језику)"),
    "sq" to MusicRegionCopy("Rajoni muzikor", "Automatik (sipas gjuhës)"),
    "bg" to MusicRegionCopy("Музикален регион", "Автоматично (според езика)"),
    "hu" to MusicRegionCopy("Zenei régió", "Automatikus (a nyelvet követi)"),
    "fi" to MusicRegionCopy("Musiikkialue", "Automaattinen (kielen mukaan)"),
    "et" to MusicRegionCopy("Muusikapiirkond", "Automaatne (järgib keelt)"),
    "nb" to MusicRegionCopy("Musikkregion", "Automatisk (følger språket)"),
    "ca" to MusicRegionCopy("Regió musical", "Automàtica (segons l'idioma)"),
    "uk" to MusicRegionCopy("Музичний регіон", "Автоматично (за мовою)"),
    "ru" to MusicRegionCopy("Музыкальный регион", "Автоматически (по языку)"),
    "kk" to MusicRegionCopy("Музыкалық аймақ", "Автоматты (тіл бойынша)"),
    "tr" to MusicRegionCopy("Müzik bölgesi", "Otomatik (dili izler)"),
    "ar" to MusicRegionCopy("منطقة الموسيقى", "تلقائي (حسب اللغة)"),
    "fa" to MusicRegionCopy("منطقه موسیقی", "خودکار (بر اساس زبان)"),
    "zh" to MusicRegionCopy("音乐地区", "自动（跟随语言）"),
    "zh-Hant" to MusicRegionCopy("音樂地區", "自動（依語言）"),
    "ja" to MusicRegionCopy("音楽の地域", "自動（言語に合わせる）"),
    "ko" to MusicRegionCopy("음악 지역", "자동(언어에 따름)"),
    "hi" to MusicRegionCopy("संगीत क्षेत्र", "स्वचालित (भाषा के अनुसार)"),
    "bn" to MusicRegionCopy("সংগীত অঞ্চল", "স্বয়ংক্রিয় (ভাষা অনুযায়ী)"),
    "id" to MusicRegionCopy("Wilayah musik", "Otomatis (mengikuti bahasa)"),
    "ms" to MusicRegionCopy("Rantau muzik", "Automatik (ikut bahasa)"),
    "vi" to MusicRegionCopy("Khu vực âm nhạc", "Tự động (theo ngôn ngữ)"),
    "th" to MusicRegionCopy("ภูมิภาคเพลง", "อัตโนมัติ (ตามภาษา)"),
    "fil" to MusicRegionCopy("Rehiyon ng musika", "Awtomatiko (sumusunod sa wika)"),
    "he" to MusicRegionCopy("אזור מוזיקה", "אוטומטי (לפי השפה)")
)

internal fun musicRegionCopy(code: String): MusicRegionCopy = localizedValueOrEnglish(musicRegionCopies, code)
