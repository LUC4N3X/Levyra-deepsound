package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal data class SpotifySkinCopy(
    val all: String,
    val popular: String,
    val popularReleases: String,
    val latestRelease: String,
    val showDiscography: String,
    val about: String,
    val fansAlsoLike: String
)

internal object LevyraSpotifySkinStrings {
    private val copies = mapOf(
        "en" to SpotifySkinCopy("All", "Popular", "Popular releases", "Latest release", "Show discography", "About", "Fans also like"),
        "it" to SpotifySkinCopy("Tutti", "Popolare", "Uscite popolari", "Ultima uscita", "Vedi discografia", "Informazioni", "I fan apprezzano anche"),
        "es" to SpotifySkinCopy("Todo", "Populares", "Lanzamientos populares", "Último lanzamiento", "Ver discografía", "Información", "A los fans también les gusta"),
        "fr" to SpotifySkinCopy("Tout", "Populaires", "Sorties populaires", "Dernière sortie", "Voir la discographie", "À propos", "Les fans aiment aussi"),
        "de" to SpotifySkinCopy("Alle", "Beliebt", "Beliebte Veröffentlichungen", "Neueste Veröffentlichung", "Diskografie anzeigen", "Info", "Fans mögen auch"),
        "pt" to SpotifySkinCopy("Tudo", "Populares", "Lançamentos populares", "Último lançamento", "Ver discografia", "Sobre", "Os fãs também gostam"),
        "nl" to SpotifySkinCopy("Alles", "Populair", "Populaire releases", "Nieuwste release", "Discografie bekijken", "Over", "Fans vinden ook leuk"),
        "pl" to SpotifySkinCopy("Wszystko", "Popularne", "Popularne wydania", "Najnowsze wydanie", "Pokaż dyskografię", "Informacje", "Fani lubią też"),
        "ru" to SpotifySkinCopy("Все", "Популярное", "Популярные релизы", "Последний релиз", "Показать дискографию", "Об исполнителе", "Поклонникам также нравится"),
        "uk" to SpotifySkinCopy("Усе", "Популярне", "Популярні релізи", "Останній реліз", "Показати дискографію", "Про виконавця", "Шанувальникам також подобається"),
        "ja" to SpotifySkinCopy("すべて", "人気", "人気のリリース", "最新リリース", "ディスコグラフィを見る", "概要", "ファンにはこちらも人気"),
        "ko" to SpotifySkinCopy("전체", "인기", "인기 발매작", "최신 발매", "디스코그래피 보기", "정보", "팬들이 좋아하는 다른 아티스트"),
        "zh" to SpotifySkinCopy("全部", "热门", "热门发行", "最新发行", "查看唱片目录", "简介", "粉丝也喜欢"),
        "zh-Hant" to SpotifySkinCopy("全部", "熱門", "熱門發行", "最新發行", "查看唱片目錄", "簡介", "粉絲也喜歡"),
        "ar" to SpotifySkinCopy("الكل", "الأكثر شعبية", "إصدارات شائعة", "أحدث إصدار", "عرض الأعمال", "نبذة", "يحب المعجبون أيضًا"),
        "ro" to SpotifySkinCopy("Toate", "Populare", "Lansări populare", "Cea mai nouă lansare", "Vezi discografia", "Despre", "Fanilor le mai plac"),
        "sq" to SpotifySkinCopy("Të gjitha", "Popullore", "Publikime popullore", "Publikimi i fundit", "Shiko diskografinë", "Rreth", "Fansave u pëlqejnë edhe"),
        "bg" to SpotifySkinCopy("Всички", "Популярни", "Популярни издания", "Най-ново издание", "Виж дискографията", "Информация", "На феновете харесват също"),
        "el" to SpotifySkinCopy("Όλα", "Δημοφιλή", "Δημοφιλείς κυκλοφορίες", "Τελευταία κυκλοφορία", "Δισκογραφία", "Σχετικά", "Στους θαυμαστές αρέσουν επίσης"),
        "sv" to SpotifySkinCopy("Alla", "Populärt", "Populära släpp", "Senaste släpp", "Visa diskografi", "Om", "Fans gillar också"),
        "da" to SpotifySkinCopy("Alle", "Populære", "Populære udgivelser", "Seneste udgivelse", "Vis diskografi", "Om", "Fans kan også lide"),
        "cs" to SpotifySkinCopy("Vše", "Oblíbené", "Oblíbená vydání", "Nejnovější vydání", "Zobrazit diskografii", "Informace", "Fanoušci mají rádi také"),
        "sk" to SpotifySkinCopy("Všetko", "Obľúbené", "Obľúbené vydania", "Najnovšie vydanie", "Zobraziť diskografiu", "Informácie", "Fanúšikovia majú radi aj"),
        "bn" to SpotifySkinCopy("সব", "জনপ্রিয়", "জনপ্রিয় প্রকাশনা", "সর্বশেষ প্রকাশ", "ডিস্কোগ্রাফি দেখুন", "পরিচিতি", "ভক্তরা আরও পছন্দ করেন"),
        "kk" to SpotifySkinCopy("Барлығы", "Танымал", "Танымал шығарылымдар", "Соңғы шығарылым", "Дискографияны көру", "Ақпарат", "Жанкүйерлерге ұнайтындар"),
        "sr" to SpotifySkinCopy("Све", "Популарно", "Популарна издања", "Најновије издање", "Прикажи дискографију", "Информације", "Фановима се свиђа и"),
        "hr" to SpotifySkinCopy("Sve", "Popularno", "Popularna izdanja", "Najnovije izdanje", "Prikaži diskografiju", "Informacije", "Obožavateljima se sviđa i"),
        "hu" to SpotifySkinCopy("Összes", "Népszerű", "Népszerű megjelenések", "Legújabb megjelenés", "Diszkográfia megtekintése", "Névjegy", "A rajongók ezeket is kedvelik"),
        "fi" to SpotifySkinCopy("Kaikki", "Suositut", "Suositut julkaisut", "Uusin julkaisu", "Näytä diskografia", "Tietoja", "Fanit pitävät myös"),
        "et" to SpotifySkinCopy("Kõik", "Populaarne", "Populaarsed väljaanded", "Uusim väljaanne", "Kuva diskograafia", "Teave", "Fännidele meeldivad ka"),
        "nb" to SpotifySkinCopy("Alle", "Populært", "Populære utgivelser", "Nyeste utgivelse", "Vis diskografi", "Om", "Fans liker også"),
        "ca" to SpotifySkinCopy("Tot", "Populars", "Llançaments populars", "Últim llançament", "Mostra la discografia", "Informació", "Als fans també els agrada"),
        "tr" to SpotifySkinCopy("Tümü", "Popüler", "Popüler yayınlar", "En yeni yayın", "Diskografiyi göster", "Hakkında", "Hayranlar bunları da seviyor"),
        "fa" to SpotifySkinCopy("همه", "محبوب", "انتشارهای محبوب", "آخرین انتشار", "نمایش آثار", "درباره", "طرفداران این‌ها را هم دوست دارند"),
        "hi" to SpotifySkinCopy("सभी", "लोकप्रिय", "लोकप्रिय रिलीज़", "नवीनतम रिलीज़", "डिस्कोग्राफ़ी देखें", "परिचय", "प्रशंसकों को यह भी पसंद है"),
        "id" to SpotifySkinCopy("Semua", "Populer", "Rilisan populer", "Rilisan terbaru", "Lihat diskografi", "Tentang", "Penggemar juga menyukai"),
        "ms" to SpotifySkinCopy("Semua", "Popular", "Keluaran popular", "Keluaran terkini", "Lihat diskografi", "Perihal", "Peminat juga suka"),
        "vi" to SpotifySkinCopy("Tất cả", "Phổ biến", "Bản phát hành nổi bật", "Bản phát hành mới nhất", "Xem danh sách đĩa nhạc", "Giới thiệu", "Người hâm mộ cũng thích"),
        "th" to SpotifySkinCopy("ทั้งหมด", "ยอดนิยม", "ผลงานยอดนิยม", "ผลงานล่าสุด", "ดูผลงานทั้งหมด", "เกี่ยวกับ", "แฟนเพลงยังชอบ"),
        "fil" to SpotifySkinCopy("Lahat", "Sikat", "Mga sikat na release", "Pinakabagong release", "Tingnan ang discography", "Tungkol", "Gusto rin ng mga fan"),
        "he" to SpotifySkinCopy("הכול", "פופולרי", "יציאות פופולריות", "היציאה האחרונה", "הצגת הדיסקוגרפיה", "אודות", "מעריצים אוהבים גם")
    )

    fun forCode(code: String): SpotifySkinCopy = copies[LevyraLanguageCatalog.normalize(code)] ?: copies.getValue("en")
}
