package com.luc4n3x.levyra.ui.i18n

internal data class SpotifyYearCopy(val topOfYear: String, val topTracks: String)

private val spotifyYearCopies: Map<String, SpotifyYearCopy> = mapOf(
    "ar" to SpotifyYearCopy("الأفضل في %d", "أبرز الأغاني"),
    "bg" to SpotifyYearCopy("Топ %d", "Топ песни"),
    "bn" to SpotifyYearCopy("%d-এর সেরা", "সেরা গান"),
    "ca" to SpotifyYearCopy("Top %d", "Cançons top"),
    "cs" to SpotifyYearCopy("Top %d", "Top skladby"),
    "da" to SpotifyYearCopy("Top %d", "Topnumre"),
    "de" to SpotifyYearCopy("Top %d", "Top-Songs"),
    "el" to SpotifyYearCopy("Κορυφαία %d", "Κορυφαία τραγούδια"),
    "en" to SpotifyYearCopy("Top %d", "Top Tracks"),
    "es" to SpotifyYearCopy("Lo más %d", "Canciones top"),
    "et" to SpotifyYearCopy("Top %d", "Top lood"),
    "fa" to SpotifyYearCopy("برترین‌های %d", "آهنگ‌های برتر"),
    "fi" to SpotifyYearCopy("Top %d", "Suosituimmat kappaleet"),
    "fil" to SpotifyYearCopy("Top %d", "Mga top na kanta"),
    "fr" to SpotifyYearCopy("Top %d", "Titres phares"),
    "he" to SpotifyYearCopy("המובילים של %d", "השירים המובילים"),
    "hi" to SpotifyYearCopy("%d के टॉप", "टॉप गाने"),
    "hr" to SpotifyYearCopy("Top %d", "Top pjesme"),
    "hu" to SpotifyYearCopy("Top %d", "Top dalok"),
    "id" to SpotifyYearCopy("Top %d", "Lagu teratas"),
    "it" to SpotifyYearCopy("Top %d", "Brani top"),
    "ja" to SpotifyYearCopy("%d年トップ", "トップソング"),
    "kk" to SpotifyYearCopy("%d үздіктері", "Үздік әндер"),
    "ko" to SpotifyYearCopy("%d 톱", "인기 곡"),
    "ms" to SpotifyYearCopy("Top %d", "Lagu teratas"),
    "nb" to SpotifyYearCopy("Topp %d", "Toppspor"),
    "nl" to SpotifyYearCopy("Top %d", "Topnummers"),
    "pl" to SpotifyYearCopy("Top %d", "Najpopularniejsze utwory"),
    "pt" to SpotifyYearCopy("Top %d", "Músicas em destaque"),
    "pt-BR" to SpotifyYearCopy("Top %d", "Músicas em alta"),
    "ro" to SpotifyYearCopy("Top %d", "Piese de top"),
    "ru" to SpotifyYearCopy("Топ %d", "Топ треков"),
    "sk" to SpotifyYearCopy("Top %d", "Top skladby"),
    "sq" to SpotifyYearCopy("Më të mirat e %d", "Këngët kryesore"),
    "sr" to SpotifyYearCopy("Топ %d", "Топ песме"),
    "sv" to SpotifyYearCopy("Topp %d", "Topplåtar"),
    "th" to SpotifyYearCopy("ท็อป %d", "เพลงยอดนิยม"),
    "tr" to SpotifyYearCopy("%d zirvesi", "En iyi şarkılar"),
    "uk" to SpotifyYearCopy("Топ %d", "Топ треків"),
    "vi" to SpotifyYearCopy("Top %d", "Bài hát hàng đầu"),
    "zh" to SpotifyYearCopy("%d 年度热门", "热门歌曲"),
    "zh-Hant" to SpotifyYearCopy("%d 年度熱門", "熱門歌曲")
)

internal fun spotifyYearCopy(code: String): SpotifyYearCopy =
    spotifyYearCopies[code] ?: spotifyYearCopies[code.substringBefore('-')] ?: spotifyYearCopies.getValue("en")

internal fun spotifyYearLocalizationCodes(): Set<String> = spotifyYearCopies.keys
