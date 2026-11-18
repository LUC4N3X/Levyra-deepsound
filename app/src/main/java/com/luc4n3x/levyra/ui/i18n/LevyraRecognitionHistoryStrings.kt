package com.luc4n3x.levyra.ui.i18n

private val recognitionHistoryTitles: Map<String, String> = mapOf(
    "en" to "Recognized tracks",
    "it" to "Brani riconosciuti",
    "es" to "Canciones reconocidas",
    "fr" to "Titres reconnus",
    "de" to "Erkannte Titel",
    "pt" to "Faixas reconhecidas",
    "nl" to "Herkende nummers",
    "pl" to "Rozpoznane utwory",
    "ro" to "Piese recunoscute",
    "el" to "Αναγνωρισμένα κομμάτια",
    "sv" to "Igenkända låtar",
    "da" to "Genkendte numre",
    "cs" to "Rozpoznané skladby",
    "sk" to "Rozpoznané skladby",
    "bn" to "শনাক্ত করা গান",
    "kk" to "Анықталған тректер",
    "sr" to "Препознате песме",
    "sq" to "Këngë të njohura",
    "hr" to "Prepoznate pjesme",
    "bg" to "Разпознати песни",
    "hu" to "Felismert dalok",
    "fi" to "Tunnistetut kappaleet",
    "et" to "Tuvastatud lood",
    "nb" to "Gjenkjente spor",
    "ca" to "Cançons reconegudes",
    "uk" to "Розпізнані треки",
    "ru" to "Распознанные треки",
    "tr" to "Tanınan parçalar",
    "ar" to "المقاطع المتعرف عليها",
    "fa" to "آهنگ‌های شناسایی‌شده",
    "zh" to "已识别的歌曲",
    "zh-Hant" to "已辨識的歌曲",
    "ja" to "認識した曲",
    "ko" to "인식한 곡",
    "hi" to "पहचाने गए गाने",
    "id" to "Lagu yang dikenali",
    "ms" to "Lagu yang dikenal pasti",
    "vi" to "Bài hát đã nhận diện",
    "th" to "เพลงที่จดจำได้",
    "fil" to "Mga nakilalang kanta",
    "he" to "שירים שזוהו"
)

internal val recognitionHistoryKeys = setOf("recognitionHistoryTitle")

internal fun recognitionHistoryLocalizationEntries(code: String): Map<String, String> =
    mapOf("recognitionHistoryTitle" to localizedValueOrEnglish(recognitionHistoryTitles, code))
