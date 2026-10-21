package com.luc4n3x.levyra.ui.i18n

internal data class SearchDiscographyCopy(
    val discography: String,
    val album: String,
    val single: String,
    val featuredOn: String
)

internal fun searchDiscographyCopy(code: String): SearchDiscographyCopy =
    SEARCH_DISCOGRAPHY_COPY[code] ?: SEARCH_DISCOGRAPHY_COPY.getValue("en")

internal fun searchDiscographyLocalizationCodes(): Set<String> = SEARCH_DISCOGRAPHY_COPY.keys

private fun discographyCopy(discography: String, album: String, single: String, featuredOn: String) =
    SearchDiscographyCopy(discography, album, single, featuredOn)

private val SEARCH_DISCOGRAPHY_COPY = mapOf(
    "en" to discographyCopy("Discography", "Album", "Single", "Featured on"),
    "it" to discographyCopy("Discografia", "Album", "Singolo", "Presente in"),
    "es" to discographyCopy("Discografía", "Álbum", "Sencillo", "Aparece en"),
    "fr" to discographyCopy("Discographie", "Album", "Single", "Présent dans"),
    "de" to discographyCopy("Diskografie", "Album", "Single", "Enthalten in"),
    "pt" to discographyCopy("Discografia", "Álbum", "Single", "Presente em"),
    "nl" to discographyCopy("Discografie", "Album", "Single", "Te horen in"),
    "pl" to discographyCopy("Dyskografia", "Album", "Singiel", "Występuje w"),
    "ro" to discographyCopy("Discografie", "Album", "Single", "Apare în"),
    "el" to discographyCopy("Δισκογραφία", "Άλμπουμ", "Σινγκλ", "Εμφανίζεται σε"),
    "sv" to discographyCopy("Diskografi", "Album", "Singel", "Finns i"),
    "da" to discographyCopy("Diskografi", "Album", "Single", "Med i"),
    "cs" to discographyCopy("Diskografie", "Album", "Singl", "Najdete v"),
    "sk" to discographyCopy("Diskografia", "Album", "Singel", "Nájdete v"),
    "bn" to discographyCopy("ডিস্কোগ্রাফি", "অ্যালবাম", "সিঙ্গেল", "যেখানে আছে"),
    "kk" to discographyCopy("Дискография", "Альбом", "Сингл", "Қамтылған"),
    "sr" to discographyCopy("Дискографија", "Албум", "Сингл", "Налази се у"),
    "sq" to discographyCopy("Diskografia", "Album", "Këngë e vetme", "Gjendet në"),
    "hr" to discographyCopy("Diskografija", "Album", "Singl", "Nalazi se u"),
    "bg" to discographyCopy("Дискография", "Албум", "Сингъл", "Включено в"),
    "hu" to discographyCopy("Diszkográfia", "Album", "Kislemez", "Szerepel itt"),
    "fi" to discographyCopy("Diskografia", "Albumi", "Single", "Mukana listoilla"),
    "et" to discographyCopy("Diskograafia", "Album", "Singel", "Leidub"),
    "nb" to discographyCopy("Diskografi", "Album", "Singel", "Med i"),
    "ca" to discographyCopy("Discografia", "Àlbum", "Senzill", "Apareix a"),
    "uk" to discographyCopy("Дискографія", "Альбом", "Сингл", "Звучить у"),
    "ru" to discographyCopy("Дискография", "Альбом", "Сингл", "Входит в"),
    "tr" to discographyCopy("Diskografi", "Albüm", "Single", "Yer aldığı listeler"),
    "ar" to discographyCopy("الأعمال الموسيقية", "ألبوم", "أغنية منفردة", "موجود في"),
    "fa" to discographyCopy("آثار منتشرشده", "آلبوم", "تک‌آهنگ", "حاضر در"),
    "zh" to discographyCopy("唱片目录", "专辑", "单曲", "收录于"),
    "zh-Hant" to discographyCopy("唱片目錄", "專輯", "單曲", "收錄於"),
    "ja" to discographyCopy("ディスコグラフィ", "アルバム", "シングル", "収録プレイリスト"),
    "ko" to discographyCopy("디스코그래피", "앨범", "싱글", "수록된 플레이리스트"),
    "hi" to discographyCopy("डिस्कोग्राफ़ी", "एल्बम", "सिंगल", "इनमें शामिल"),
    "id" to discographyCopy("Diskografi", "Album", "Single", "Ada di"),
    "ms" to discographyCopy("Diskografi", "Album", "Single", "Terdapat dalam"),
    "vi" to discographyCopy("Danh sách đĩa nhạc", "Album", "Đĩa đơn", "Có mặt trong"),
    "th" to discographyCopy("ผลงานทั้งหมด", "อัลบั้ม", "ซิงเกิล", "อยู่ในเพลย์ลิสต์"),
    "fil" to discographyCopy("Diskograpiya", "Album", "Single", "Kasama sa"),
    "he" to discographyCopy("דיסקוגרפיה", "אלבום", "סינגל", "מופיע ב")
)
