package com.luc4n3x.levyra.ui.i18n

private val moreByArtistTemplates = mapOf(
    "en" to "More by %s",
    "it" to "Altro di %s",
    "es" to "Más de %s",
    "fr" to "Plus de %s",
    "de" to "Mehr von %s",
    "pt" to "Mais de %s",
    "nl" to "Meer van %s",
    "pl" to "Więcej od %s",
    "ro" to "Mai multe de la %s",
    "el" to "Περισσότερα από %s",
    "sv" to "Mer av %s",
    "da" to "Mere fra %s",
    "cs" to "Další od %s",
    "sk" to "Viac od %s",
    "hr" to "Više od %s",
    "sr" to "Још од %s",
    "sq" to "Më shumë nga %s",
    "bg" to "Още от %s",
    "hu" to "Továbbiak tőle: %s",
    "fi" to "Lisää artistilta %s",
    "et" to "Veel artistilt %s",
    "nb" to "Mer fra %s",
    "ca" to "Més de %s",
    "uk" to "Більше від %s",
    "ru" to "Ещё от %s",
    "kk" to "%s: тағы",
    "tr" to "%s adlı sanatçıdan daha fazlası",
    "ar" to "المزيد من %s",
    "fa" to "بیشتر از %s",
    "zh" to "%s 的更多作品",
    "zh-Hant" to "%s 的更多作品",
    "ja" to "%s のその他の作品",
    "ko" to "%s의 다른 음악",
    "hi" to "%s की और रचनाएँ",
    "bn" to "%s-এর আরও গান",
    "id" to "Lainnya dari %s",
    "ms" to "Lagi daripada %s",
    "vi" to "Thêm từ %s",
    "th" to "เพิ่มเติมจาก %s",
    "fil" to "Higit pa mula kay %s",
    "he" to "עוד מאת %s"
)

fun LevyraStrings.moreByArtist(artist: String): String = moreByArtist(code, artist)

fun moreByArtist(code: String, artist: String): String =
    localizedValueOrEnglish(moreByArtistTemplates, code).replace("%s", artist.trim())
