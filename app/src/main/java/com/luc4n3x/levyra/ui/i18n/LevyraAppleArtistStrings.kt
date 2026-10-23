package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal data class AppleArtistInfoCopy(
    val about: String,
    val hometown: String,
    val born: String,
    val formed: String,
    val genre: String,
    val topVideos: String
)

internal fun appleArtistInfoCopy(code: String): AppleArtistInfoCopy =
    APPLE_ARTIST_INFO_COPY[LevyraLanguageCatalog.normalize(code)] ?: APPLE_ARTIST_INFO_COPY.getValue("en")

internal fun appleArtistInfoLocalizationCodes(): Set<String> = APPLE_ARTIST_INFO_COPY.keys

private val APPLE_ARTIST_INFO_COPY = mapOf(
    "en" to AppleArtistInfoCopy("About", "Hometown", "Born", "Formed", "Genre", "Top Videos"),
    "it" to AppleArtistInfoCopy("Informazioni", "Città", "Data di nascita", "Formazione", "Genere", "Top video"),
    "es" to AppleArtistInfoCopy("Información", "Ciudad natal", "Nacimiento", "Formación", "Género", "Videos destacados"),
    "fr" to AppleArtistInfoCopy("À propos", "Ville d'origine", "Naissance", "Formation", "Genre", "Meilleures vidéos"),
    "de" to AppleArtistInfoCopy("Info", "Herkunft", "Geboren", "Gegründet", "Genre", "Top-Videos"),
    "pt" to AppleArtistInfoCopy("Sobre", "Cidade natal", "Nascimento", "Formação", "Gênero", "Principais vídeos"),
    "nl" to AppleArtistInfoCopy("Info", "Woonplaats", "Geboren", "Opgericht", "Genre", "Topvideo's"),
    "pl" to AppleArtistInfoCopy("Informacje", "Miejsce pochodzenia", "Data urodzenia", "Założenie", "Gatunek", "Najlepsze teledyski"),
    "ru" to AppleArtistInfoCopy("Об исполнителе", "Родной город", "Дата рождения", "Год основания", "Жанр", "Лучшие клипы"),
    "uk" to AppleArtistInfoCopy("Про виконавця", "Рідне місто", "Дата народження", "Рік заснування", "Жанр", "Найкращі кліпи"),
    "ja" to AppleArtistInfoCopy("詳細", "出身地", "生年月日", "結成", "ジャンル", "トップビデオ"),
    "ko" to AppleArtistInfoCopy("정보", "출신지", "출생", "결성", "장르", "인기 비디오"),
    "zh" to AppleArtistInfoCopy("简介", "家乡", "出生", "成立", "流派", "热门视频"),
    "zh-Hant" to AppleArtistInfoCopy("簡介", "家鄉", "出生", "成立", "類型", "熱門影片"),
    "ar" to AppleArtistInfoCopy("نبذة", "المدينة الأم", "تاريخ الميلاد", "التأسيس", "النوع", "أبرز الفيديوهات"),
    "ro" to AppleArtistInfoCopy("Despre", "Orașul natal", "Data nașterii", "Înființare", "Gen", "Videoclipuri de top"),
    "sq" to AppleArtistInfoCopy("Rreth", "Vendlindja", "Lindur", "Themeluar", "Zhanri", "Videot kryesore"),
    "bg" to AppleArtistInfoCopy("Информация", "Роден град", "Дата на раждане", "Основаване", "Жанр", "Топ видеоклипове"),
    "el" to AppleArtistInfoCopy("Πληροφορίες", "Τόπος καταγωγής", "Γέννηση", "Ίδρυση", "Είδος", "Κορυφαία βίντεο"),
    "sv" to AppleArtistInfoCopy("Om", "Hemort", "Född", "Bildad", "Genre", "Toppvideor"),
    "da" to AppleArtistInfoCopy("Om", "Hjemby", "Født", "Dannet", "Genre", "Topvideoer"),
    "cs" to AppleArtistInfoCopy("Informace", "Rodné město", "Narození", "Založení", "Žánr", "Nejlepší videa"),
    "sk" to AppleArtistInfoCopy("Informácie", "Rodné mesto", "Narodenie", "Založenie", "Žáner", "Najlepšie videá"),
    "bn" to AppleArtistInfoCopy("সম্পর্কে", "নিজ শহর", "জন্ম", "গঠিত", "ধারা", "শীর্ষ ভিডিও"),
    "kk" to AppleArtistInfoCopy("Ақпарат", "Туған қаласы", "Туған күні", "Құрылған", "Жанр", "Үздік бейнелер"),
    "sr" to AppleArtistInfoCopy("О извођачу", "Родни град", "Рођење", "Основано", "Жанр", "Најбољи спотови"),
    "hr" to AppleArtistInfoCopy("O izvođaču", "Rodni grad", "Rođenje", "Osnovano", "Žanr", "Najbolji spotovi"),
    "hu" to AppleArtistInfoCopy("Névjegy", "Szülőváros", "Született", "Alakult", "Műfaj", "Legnépszerűbb videók"),
    "fi" to AppleArtistInfoCopy("Tietoja", "Kotikaupunki", "Syntynyt", "Perustettu", "Tyylilaji", "Suosituimmat videot"),
    "et" to AppleArtistInfoCopy("Teave", "Kodulinn", "Sündinud", "Asutatud", "Žanr", "Populaarseimad videod"),
    "nb" to AppleArtistInfoCopy("Om", "Hjemby", "Født", "Dannet", "Sjanger", "Toppvideoer"),
    "ca" to AppleArtistInfoCopy("Informació", "Ciutat natal", "Naixement", "Formació", "Gènere", "Vídeos destacats"),
    "tr" to AppleArtistInfoCopy("Hakkında", "Memleket", "Doğum", "Kuruluş", "Tür", "Popüler videolar"),
    "fa" to AppleArtistInfoCopy("درباره", "زادگاه", "تولد", "تشکیل", "سبک", "ویدیوهای برتر"),
    "hi" to AppleArtistInfoCopy("परिचय", "गृहनगर", "जन्म", "गठन", "शैली", "टॉप वीडियो"),
    "id" to AppleArtistInfoCopy("Tentang", "Kota asal", "Lahir", "Dibentuk", "Genre", "Video teratas"),
    "ms" to AppleArtistInfoCopy("Perihal", "Kampung halaman", "Lahir", "Ditubuhkan", "Genre", "Video teratas"),
    "vi" to AppleArtistInfoCopy("Giới thiệu", "Quê quán", "Ngày sinh", "Thành lập", "Thể loại", "Video hàng đầu"),
    "th" to AppleArtistInfoCopy("เกี่ยวกับ", "บ้านเกิด", "เกิด", "ก่อตั้ง", "แนวเพลง", "วิดีโอยอดนิยม"),
    "fil" to AppleArtistInfoCopy("Tungkol", "Bayang pinagmulan", "Kapanganakan", "Nabuo", "Genre", "Mga nangungunang video"),
    "he" to AppleArtistInfoCopy("אודות", "עיר הולדת", "לידה", "הקמה", "ז'אנר", "סרטונים מובילים")
)
