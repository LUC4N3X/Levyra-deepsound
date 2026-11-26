package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.feature.radio.RadioFavoriteSort

internal data class RadioFavoriteSortCopy(
    val sortBy: String,
    val newest: String,
    val oldest: String,
    val nameAscending: String,
    val nameDescending: String,
    val country: String,
    val genre: String,
    val otherGenre: String,
    val unknownCountry: String
) {
    fun label(sort: RadioFavoriteSort): String = when (sort) {
        RadioFavoriteSort.Newest -> newest
        RadioFavoriteSort.Oldest -> oldest
        RadioFavoriteSort.NameAscending -> nameAscending
        RadioFavoriteSort.NameDescending -> nameDescending
        RadioFavoriteSort.Country -> country
        RadioFavoriteSort.Genre -> genre
    }
}

private data class RadioFavoriteGroupWords(
    val country: String,
    val genre: String,
    val otherGenre: String,
    val unknownCountry: String
)

private val radioFavoriteGroupWords = mapOf(
    "en" to RadioFavoriteGroupWords("Country", "Genre", "Other", "Unknown country"),
    "it" to RadioFavoriteGroupWords("Paese", "Genere", "Altro", "Paese sconosciuto"),
    "es" to RadioFavoriteGroupWords("País", "Género", "Otros", "País desconocido"),
    "fr" to RadioFavoriteGroupWords("Pays", "Genre", "Autres", "Pays inconnu"),
    "de" to RadioFavoriteGroupWords("Land", "Genre", "Sonstige", "Unbekanntes Land"),
    "pt" to RadioFavoriteGroupWords("País", "Género", "Outros", "País desconhecido"),
    "pt-BR" to RadioFavoriteGroupWords("País", "Gênero", "Outros", "País desconhecido"),
    "nl" to RadioFavoriteGroupWords("Land", "Genre", "Overig", "Onbekend land"),
    "pl" to RadioFavoriteGroupWords("Kraj", "Gatunek", "Inne", "Nieznany kraj"),
    "ro" to RadioFavoriteGroupWords("Țară", "Gen", "Altele", "Țară necunoscută"),
    "el" to RadioFavoriteGroupWords("Χώρα", "Είδος", "Άλλα", "Άγνωστη χώρα"),
    "sv" to RadioFavoriteGroupWords("Land", "Genre", "Övrigt", "Okänt land"),
    "da" to RadioFavoriteGroupWords("Land", "Genre", "Andet", "Ukendt land"),
    "cs" to RadioFavoriteGroupWords("Země", "Žánr", "Ostatní", "Neznámá země"),
    "sk" to RadioFavoriteGroupWords("Krajina", "Žáner", "Ostatné", "Neznáma krajina"),
    "hr" to RadioFavoriteGroupWords("Država", "Žanr", "Ostalo", "Nepoznata država"),
    "sr" to RadioFavoriteGroupWords("Земља", "Жанр", "Остало", "Непозната земља"),
    "sq" to RadioFavoriteGroupWords("Shteti", "Zhanri", "Të tjera", "Shtet i panjohur"),
    "bg" to RadioFavoriteGroupWords("Държава", "Жанр", "Други", "Неизвестна държава"),
    "hu" to RadioFavoriteGroupWords("Ország", "Műfaj", "Egyéb", "Ismeretlen ország"),
    "fi" to RadioFavoriteGroupWords("Maa", "Tyylilaji", "Muut", "Tuntematon maa"),
    "et" to RadioFavoriteGroupWords("Riik", "Žanr", "Muu", "Tundmatu riik"),
    "nb" to RadioFavoriteGroupWords("Land", "Sjanger", "Annet", "Ukjent land"),
    "ca" to RadioFavoriteGroupWords("País", "Gènere", "Altres", "País desconegut"),
    "uk" to RadioFavoriteGroupWords("Країна", "Жанр", "Інше", "Невідома країна"),
    "ru" to RadioFavoriteGroupWords("Страна", "Жанр", "Другое", "Неизвестная страна"),
    "kk" to RadioFavoriteGroupWords("Ел", "Жанр", "Басқа", "Белгісіз ел"),
    "tr" to RadioFavoriteGroupWords("Ülke", "Tür", "Diğer", "Bilinmeyen ülke"),
    "ar" to RadioFavoriteGroupWords("البلد", "النوع", "أخرى", "بلد غير معروف"),
    "fa" to RadioFavoriteGroupWords("کشور", "ژانر", "سایر", "کشور نامشخص"),
    "zh" to RadioFavoriteGroupWords("国家/地区", "类型", "其他", "未知国家/地区"),
    "zh-Hant" to RadioFavoriteGroupWords("國家/地區", "類型", "其他", "未知國家/地區"),
    "ja" to RadioFavoriteGroupWords("国・地域", "ジャンル", "その他", "不明な国・地域"),
    "ko" to RadioFavoriteGroupWords("국가", "장르", "기타", "알 수 없는 국가"),
    "hi" to RadioFavoriteGroupWords("देश", "शैली", "अन्य", "अज्ञात देश"),
    "bn" to RadioFavoriteGroupWords("দেশ", "ধরন", "অন্যান্য", "অজানা দেশ"),
    "id" to RadioFavoriteGroupWords("Negara", "Genre", "Lainnya", "Negara tidak diketahui"),
    "ms" to RadioFavoriteGroupWords("Negara", "Genre", "Lain-lain", "Negara tidak diketahui"),
    "vi" to RadioFavoriteGroupWords("Quốc gia", "Thể loại", "Khác", "Quốc gia không xác định"),
    "th" to RadioFavoriteGroupWords("ประเทศ", "แนวเพลง", "อื่นๆ", "ไม่ทราบประเทศ"),
    "fil" to RadioFavoriteGroupWords("Bansa", "Genre", "Iba pa", "Hindi kilalang bansa"),
    "he" to RadioFavoriteGroupWords("מדינה", "ז'אנר", "אחר", "מדינה לא ידועה")
)

internal fun radioFavoriteSortCopy(code: String): RadioFavoriteSortCopy {
    val sort = librarySortLocalizationEntries(code)
    val words = localizedValueOrEnglish(radioFavoriteGroupWords, code)
    return RadioFavoriteSortCopy(
        sortBy = sort.getValue("librarySortBy"),
        newest = sort.getValue("librarySortNewestFirst"),
        oldest = sort.getValue("librarySortOldestFirst"),
        nameAscending = sort.getValue("librarySortAscending"),
        nameDescending = sort.getValue("librarySortDescending"),
        country = words.country,
        genre = words.genre,
        otherGenre = words.otherGenre,
        unknownCountry = words.unknownCountry
    )
}

internal fun radioFavoriteSortLocalizationCodes(): Set<String> = radioFavoriteGroupWords.keys
