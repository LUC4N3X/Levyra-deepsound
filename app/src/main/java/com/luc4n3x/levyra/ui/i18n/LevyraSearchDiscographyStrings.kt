package com.luc4n3x.levyra.ui.i18n

internal data class SearchDiscographyCopy(
    val discography: String,
    val album: String,
    val single: String,
    val featuredOn: String,
    val playlist: String
)

internal fun searchDiscographyCopy(code: String): SearchDiscographyCopy =
    SEARCH_DISCOGRAPHY_COPY[code] ?: SEARCH_DISCOGRAPHY_COPY.getValue("en")

internal fun searchDiscographyLocalizationCodes(): Set<String> = SEARCH_DISCOGRAPHY_COPY.keys

private fun discographyCopy(discography: String, album: String, single: String, featuredOn: String, playlist: String) =
    SearchDiscographyCopy(discography, album, single, featuredOn, playlist)

private val SEARCH_DISCOGRAPHY_COPY = mapOf(
    "en" to discographyCopy("Discography", "Album", "Single", "Featured on", "Playlist"),
    "it" to discographyCopy("Discografia", "Album", "Singolo", "Presente in", "Playlist"),
    "es" to discographyCopy("Discografía", "Álbum", "Sencillo", "Aparece en", "Lista"),
    "fr" to discographyCopy("Discographie", "Album", "Single", "Présent dans", "Playlist"),
    "de" to discographyCopy("Diskografie", "Album", "Single", "Enthalten in", "Playlist"),
    "pt" to discographyCopy("Discografia", "Álbum", "Single", "Presente em", "Playlist"),
    "nl" to discographyCopy("Discografie", "Album", "Single", "Te horen in", "Afspeellijst"),
    "pl" to discographyCopy("Dyskografia", "Album", "Singiel", "Występuje w", "Playlista"),
    "ro" to discographyCopy("Discografie", "Album", "Single", "Apare în", "Playlist"),
    "el" to discographyCopy("Δισκογραφία", "Άλμπουμ", "Σινγκλ", "Εμφανίζεται σε", "Λίστα"),
    "sv" to discographyCopy("Diskografi", "Album", "Singel", "Finns i", "Spellista"),
    "da" to discographyCopy("Diskografi", "Album", "Single", "Med i", "Playliste"),
    "cs" to discographyCopy("Diskografie", "Album", "Singl", "Najdete v", "Playlist"),
    "sk" to discographyCopy("Diskografia", "Album", "Singel", "Nájdete v", "Playlist"),
    "bn" to discographyCopy("ডিস্কোগ্রাফি", "অ্যালবাম", "সিঙ্গেল", "যেখানে আছে", "প্লেলিস্ট"),
    "kk" to discographyCopy("Дискография", "Альбом", "Сингл", "Қамтылған", "Плейлист"),
    "sr" to discographyCopy("Дискографија", "Албум", "Сингл", "Налази се у", "Плејлиста"),
    "sq" to discographyCopy("Diskografia", "Album", "Këngë e vetme", "Gjendet në", "Listë"),
    "hr" to discographyCopy("Diskografija", "Album", "Singl", "Nalazi se u", "Popis"),
    "bg" to discographyCopy("Дискография", "Албум", "Сингъл", "Включено в", "Плейлист"),
    "hu" to discographyCopy("Diszkográfia", "Album", "Kislemez", "Szerepel itt", "Lejátszási lista"),
    "fi" to discographyCopy("Diskografia", "Albumi", "Single", "Mukana listoilla", "Soittolista"),
    "et" to discographyCopy("Diskograafia", "Album", "Singel", "Leidub", "Esitusloend"),
    "nb" to discographyCopy("Diskografi", "Album", "Singel", "Med i", "Spilleliste"),
    "ca" to discographyCopy("Discografia", "Àlbum", "Senzill", "Apareix a", "Llista"),
    "uk" to discographyCopy("Дискографія", "Альбом", "Сингл", "Звучить у", "Плейлист"),
    "ru" to discographyCopy("Дискография", "Альбом", "Сингл", "Входит в", "Плейлист"),
    "tr" to discographyCopy("Diskografi", "Albüm", "Single", "Yer aldığı listeler", "Çalma listesi"),
    "ar" to discographyCopy("الأعمال الموسيقية", "ألبوم", "أغنية منفردة", "موجود في", "قائمة تشغيل"),
    "fa" to discographyCopy("آثار منتشرشده", "آلبوم", "تک‌آهنگ", "حاضر در", "فهرست پخش"),
    "zh" to discographyCopy("唱片目录", "专辑", "单曲", "收录于", "歌单"),
    "zh-Hant" to discographyCopy("唱片目錄", "專輯", "單曲", "收錄於", "播放清單"),
    "ja" to discographyCopy("ディスコグラフィ", "アルバム", "シングル", "収録プレイリスト", "プレイリスト"),
    "ko" to discographyCopy("디스코그래피", "앨범", "싱글", "수록된 플레이리스트", "플레이리스트"),
    "hi" to discographyCopy("डिस्कोग्राफ़ी", "एल्बम", "सिंगल", "इनमें शामिल", "प्लेलिस्ट"),
    "id" to discographyCopy("Diskografi", "Album", "Single", "Ada di", "Playlist"),
    "ms" to discographyCopy("Diskografi", "Album", "Single", "Terdapat dalam", "Senarai main"),
    "vi" to discographyCopy("Danh sách đĩa nhạc", "Album", "Đĩa đơn", "Có mặt trong", "Danh sách phát"),
    "th" to discographyCopy("ผลงานทั้งหมด", "อัลบั้ม", "ซิงเกิล", "อยู่ในเพลย์ลิสต์", "เพลย์ลิสต์"),
    "fil" to discographyCopy("Diskograpiya", "Album", "Single", "Kasama sa", "Playlist"),
    "he" to discographyCopy("דיסקוגרפיה", "אלבום", "סינגל", "מופיע ב", "פלייליסט")
)
