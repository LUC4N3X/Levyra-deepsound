package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal data class AppleArtistInfoCopy(
    val about: String,
    val hometown: String,
    val born: String,
    val formed: String,
    val genre: String
)

internal fun appleArtistInfoCopy(code: String): AppleArtistInfoCopy =
    APPLE_ARTIST_INFO_COPY[LevyraLanguageCatalog.normalize(code)] ?: APPLE_ARTIST_INFO_COPY.getValue("en")

internal fun appleArtistInfoLocalizationCodes(): Set<String> = APPLE_ARTIST_INFO_COPY.keys

private val APPLE_ARTIST_INFO_COPY = mapOf(
    "en" to AppleArtistInfoCopy("About", "Hometown", "Born", "Formed", "Genre"),
    "it" to AppleArtistInfoCopy("Informazioni", "Città", "Data di nascita", "Formazione", "Genere"),
    "es" to AppleArtistInfoCopy("Información", "Ciudad natal", "Nacimiento", "Formación", "Género"),
    "fr" to AppleArtistInfoCopy("À propos", "Ville d'origine", "Naissance", "Formation", "Genre"),
    "de" to AppleArtistInfoCopy("Info", "Herkunft", "Geboren", "Gegründet", "Genre"),
    "pt" to AppleArtistInfoCopy("Sobre", "Cidade natal", "Nascimento", "Formação", "Gênero"),
    "nl" to AppleArtistInfoCopy("Info", "Woonplaats", "Geboren", "Opgericht", "Genre"),
    "pl" to AppleArtistInfoCopy("Informacje", "Miejsce pochodzenia", "Data urodzenia", "Założenie", "Gatunek"),
    "ru" to AppleArtistInfoCopy("Об исполнителе", "Родной город", "Дата рождения", "Год основания", "Жанр"),
    "uk" to AppleArtistInfoCopy("Про виконавця", "Рідне місто", "Дата народження", "Рік заснування", "Жанр"),
    "ja" to AppleArtistInfoCopy("詳細", "出身地", "生年月日", "結成", "ジャンル"),
    "ko" to AppleArtistInfoCopy("정보", "출신지", "출생", "결성", "장르"),
    "zh" to AppleArtistInfoCopy("简介", "家乡", "出生", "成立", "流派"),
    "zh-Hant" to AppleArtistInfoCopy("簡介", "家鄉", "出生", "成立", "類型"),
    "ar" to AppleArtistInfoCopy("نبذة", "المدينة الأم", "تاريخ الميلاد", "التأسيس", "النوع"),
    "ro" to AppleArtistInfoCopy("Despre", "Orașul natal", "Data nașterii", "Înființare", "Gen"),
    "sq" to AppleArtistInfoCopy("Rreth", "Vendlindja", "Lindur", "Themeluar", "Zhanri"),
    "bg" to AppleArtistInfoCopy("Информация", "Роден град", "Дата на раждане", "Основаване", "Жанр"),
    "el" to AppleArtistInfoCopy("Πληροφορίες", "Τόπος καταγωγής", "Γέννηση", "Ίδρυση", "Είδος"),
    "sv" to AppleArtistInfoCopy("Om", "Hemort", "Född", "Bildad", "Genre"),
    "da" to AppleArtistInfoCopy("Om", "Hjemby", "Født", "Dannet", "Genre"),
    "cs" to AppleArtistInfoCopy("Informace", "Rodné město", "Narození", "Založení", "Žánr"),
    "sk" to AppleArtistInfoCopy("Informácie", "Rodné mesto", "Narodenie", "Založenie", "Žáner"),
    "bn" to AppleArtistInfoCopy("সম্পর্কে", "নিজ শহর", "জন্ম", "গঠিত", "ধারা"),
    "kk" to AppleArtistInfoCopy("Ақпарат", "Туған қаласы", "Туған күні", "Құрылған", "Жанр"),
    "sr" to AppleArtistInfoCopy("О извођачу", "Родни град", "Рођење", "Основано", "Жанр"),
    "hr" to AppleArtistInfoCopy("O izvođaču", "Rodni grad", "Rođenje", "Osnovano", "Žanr"),
    "hu" to AppleArtistInfoCopy("Névjegy", "Szülőváros", "Született", "Alakult", "Műfaj"),
    "fi" to AppleArtistInfoCopy("Tietoja", "Kotikaupunki", "Syntynyt", "Perustettu", "Tyylilaji"),
    "et" to AppleArtistInfoCopy("Teave", "Kodulinn", "Sündinud", "Asutatud", "Žanr"),
    "nb" to AppleArtistInfoCopy("Om", "Hjemby", "Født", "Dannet", "Sjanger"),
    "ca" to AppleArtistInfoCopy("Informació", "Ciutat natal", "Naixement", "Formació", "Gènere"),
    "tr" to AppleArtistInfoCopy("Hakkında", "Memleket", "Doğum", "Kuruluş", "Tür"),
    "fa" to AppleArtistInfoCopy("درباره", "زادگاه", "تولد", "تشکیل", "سبک"),
    "hi" to AppleArtistInfoCopy("परिचय", "गृहनगर", "जन्म", "गठन", "शैली"),
    "id" to AppleArtistInfoCopy("Tentang", "Kota asal", "Lahir", "Dibentuk", "Genre"),
    "ms" to AppleArtistInfoCopy("Perihal", "Kampung halaman", "Lahir", "Ditubuhkan", "Genre"),
    "vi" to AppleArtistInfoCopy("Giới thiệu", "Quê quán", "Ngày sinh", "Thành lập", "Thể loại"),
    "th" to AppleArtistInfoCopy("เกี่ยวกับ", "บ้านเกิด", "เกิด", "ก่อตั้ง", "แนวเพลง"),
    "fil" to AppleArtistInfoCopy("Tungkol", "Bayang pinagmulan", "Kapanganakan", "Nabuo", "Genre"),
    "he" to AppleArtistInfoCopy("אודות", "עיר הולדת", "לידה", "הקמה", "ז'אנר")
)
