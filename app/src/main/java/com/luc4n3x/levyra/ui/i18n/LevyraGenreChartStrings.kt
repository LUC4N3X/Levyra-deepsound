package com.luc4n3x.levyra.ui.i18n

internal data class GenreChartCopy(
    val title: String,
    val subtitle: String,
    val credit: String,
    val loading: String,
    val showAll: String,
    val showLess: String,
    val shelfTitle: String,
    val shelfSubtitle: String
)

private val genreChartCopies = mapOf(
    "en" to GenreChartCopy("Top 50", "The most played songs in this genre", "Chart data: Last.fm", "Building the chart…", "Show all", "Show less", "Genre hits", "Charts by genre"),
    "it" to GenreChartCopy("Top 50", "I brani più ascoltati di questo genere", "Dati classifica: Last.fm", "Componiamo la classifica…", "Mostra tutti", "Mostra meno", "Generi in vetta", "Classifiche per genere"),
    "es" to GenreChartCopy("Top 50", "Las canciones más escuchadas de este género", "Datos de la lista: Last.fm", "Preparando la lista…", "Ver todas", "Ver menos", "Éxitos por género", "Listas por género"),
    "fr" to GenreChartCopy("Top 50", "Les titres les plus écoutés de ce genre", "Données du classement : Last.fm", "Préparation du classement…", "Tout voir", "Voir moins", "Tubes par genre", "Classements par genre"),
    "de" to GenreChartCopy("Top 50", "Die meistgehörten Songs dieses Genres", "Chartdaten: Last.fm", "Charts werden erstellt …", "Alle anzeigen", "Weniger anzeigen", "Genre-Hits", "Charts nach Genre"),
    "pt" to GenreChartCopy("Top 50", "As músicas mais ouvidas deste género", "Dados da tabela: Last.fm", "A preparar a tabela…", "Ver todas", "Ver menos", "Êxitos por género", "Tops por género"),
    "pt-BR" to GenreChartCopy("Top 50", "As músicas mais ouvidas deste gênero", "Dados da parada: Last.fm", "Montando a parada…", "Ver todas", "Ver menos", "Hits por gênero", "Paradas por gênero"),
    "nl" to GenreChartCopy("Top 50", "De meest beluisterde nummers in dit genre", "Lijstgegevens: Last.fm", "Lijst wordt samengesteld…", "Alles tonen", "Minder tonen", "Genrehits", "Hitlijsten per genre"),
    "pl" to GenreChartCopy("Top 50", "Najczęściej słuchane utwory w tym gatunku", "Dane listy: Last.fm", "Tworzymy listę…", "Pokaż wszystkie", "Pokaż mniej", "Hity gatunków", "Listy według gatunku"),
    "ro" to GenreChartCopy("Top 50", "Cele mai ascultate piese din acest gen", "Date clasament: Last.fm", "Pregătim clasamentul…", "Arată tot", "Arată mai puțin", "Hiturile genurilor", "Topuri pe genuri"),
    "el" to GenreChartCopy("Top 50", "Τα πιο δημοφιλή τραγούδια του είδους", "Δεδομένα chart: Last.fm", "Δημιουργία chart…", "Εμφάνιση όλων", "Λιγότερα", "Επιτυχίες ανά είδος", "Τσαρτ ανά είδος"),
    "sv" to GenreChartCopy("Topp 50", "De mest spelade låtarna i genren", "Listdata: Last.fm", "Listan byggs…", "Visa alla", "Visa färre", "Genrehits", "Topplistor per genre"),
    "da" to GenreChartCopy("Top 50", "De mest spillede numre i genren", "Hitlistedata: Last.fm", "Hitlisten bygges…", "Vis alle", "Vis færre", "Genrehits", "Hitlister efter genre"),
    "cs" to GenreChartCopy("Top 50", "Nejposlouchanější skladby žánru", "Data žebříčku: Last.fm", "Sestavujeme žebříček…", "Zobrazit vše", "Zobrazit méně", "Hity žánrů", "Žebříčky podle žánru"),
    "uk" to GenreChartCopy("Топ-50", "Найпопулярніші пісні цього жанру", "Дані чарту: Last.fm", "Складаємо чарт…", "Показати всі", "Показати менше", "Хіти жанрів", "Чарти за жанрами"),
    "ru" to GenreChartCopy("Топ-50", "Самые популярные песни этого жанра", "Данные чарта: Last.fm", "Собираем чарт…", "Показать все", "Свернуть", "Хиты жанров", "Чарты по жанрам"),
    "nb" to GenreChartCopy("Topp 50", "De mest spilte låtene i sjangeren", "Listedata: Last.fm", "Lager listen…", "Vis alle", "Vis færre", "Sjangerhits", "Lister etter sjanger"),
    "ca" to GenreChartCopy("Top 50", "Les cançons més escoltades d'aquest gènere", "Dades de la llista: Last.fm", "Preparant la llista…", "Mostra-les totes", "Mostra'n menys", "Èxits per gènere", "Llistes per gènere"),
    "kk" to GenreChartCopy("Топ-50", "Осы жанрдың ең көп тыңдалған әндері", "Чарт деректері: Last.fm", "Чарт жасалуда…", "Барлығын көрсету", "Азырақ көрсету", "Жанр хиттері", "Жанр чарттары"),
    "tr" to GenreChartCopy("Top 50", "Bu türde en çok dinlenen şarkılar", "Liste verisi: Last.fm", "Liste hazırlanıyor…", "Tümünü göster", "Daha az göster", "Türlerin hitleri", "Türlere göre listeler"),
    "ar" to GenreChartCopy("أفضل 50", "الأغاني الأكثر استماعًا في هذا النوع", "بيانات القائمة: Last.fm", "جارٍ إعداد القائمة…", "عرض الكل", "عرض أقل", "أبرز أغاني الأنواع", "قوائم حسب النوع"),
    "fa" to GenreChartCopy("۵۰ برتر", "پرشنیده‌ترین آهنگ‌های این سبک", "داده‌های جدول: Last.fm", "در حال ساخت جدول…", "نمایش همه", "نمایش کمتر", "داغ‌ترین‌های هر سبک", "جدول‌ها بر اساس سبک"),
    "zh" to GenreChartCopy("Top 50", "该曲风播放最多的歌曲", "榜单数据：Last.fm", "正在生成榜单…", "显示全部", "收起", "曲风热歌", "按曲风划分的榜单"),
    "zh-Hant" to GenreChartCopy("Top 50", "此曲風播放最多的歌曲", "排行榜資料：Last.fm", "正在產生排行榜…", "顯示全部", "收起", "曲風熱歌", "依曲風劃分的排行榜"),
    "ja" to GenreChartCopy("トップ50", "このジャンルで最も再生された曲", "チャートデータ: Last.fm", "チャートを作成中…", "すべて表示", "表示を減らす", "ジャンル別ヒット", "ジャンル別チャート"),
    "ko" to GenreChartCopy("Top 50", "이 장르에서 가장 많이 재생된 곡", "차트 데이터: Last.fm", "차트를 만드는 중…", "모두 보기", "간단히 보기", "장르별 히트곡", "장르별 차트"),
    "hi" to GenreChartCopy("टॉप 50", "इस शैली के सबसे ज़्यादा सुने गए गाने", "चार्ट डेटा: Last.fm", "चार्ट बन रहा है…", "सभी दिखाएँ", "कम दिखाएँ", "शैली के हिट", "शैली के अनुसार चार्ट"),
    "bn" to GenreChartCopy("সেরা ৫০", "এই ঘরানার সবচেয়ে বেশি শোনা গান", "চার্ট ডেটা: Last.fm", "চার্ট তৈরি হচ্ছে…", "সব দেখুন", "কম দেখুন", "ঘরানার হিট", "ঘরানাভিত্তিক চার্ট"),
    "id" to GenreChartCopy("Top 50", "Lagu paling banyak diputar di genre ini", "Data tangga lagu: Last.fm", "Menyusun tangga lagu…", "Tampilkan semua", "Tampilkan lebih sedikit", "Hit per genre", "Tangga lagu per genre"),
    "ms" to GenreChartCopy("Top 50", "Lagu paling banyak dimainkan dalam genre ini", "Data carta: Last.fm", "Menyusun carta…", "Tunjukkan semua", "Tunjukkan kurang", "Hit mengikut genre", "Carta mengikut genre"),
    "vi" to GenreChartCopy("Top 50", "Những bài hát được nghe nhiều nhất của thể loại", "Dữ liệu bảng xếp hạng: Last.fm", "Đang tạo bảng xếp hạng…", "Hiện tất cả", "Thu gọn", "Hit theo thể loại", "Bảng xếp hạng theo thể loại"),
    "th" to GenreChartCopy("Top 50", "เพลงที่ถูกเล่นมากที่สุดในแนวเพลงนี้", "ข้อมูลชาร์ต: Last.fm", "กำลังสร้างชาร์ต…", "แสดงทั้งหมด", "แสดงน้อยลง", "เพลงฮิตตามแนว", "ชาร์ตตามแนวเพลง"),
    "fil" to GenreChartCopy("Top 50", "Ang pinakapinakikinggang kanta sa genre na ito", "Data ng chart: Last.fm", "Binubuo ang chart…", "Ipakita lahat", "Ipakita nang mas kaunti", "Mga hit bawat genre", "Mga chart ayon sa genre"),
    "he" to GenreChartCopy("טופ 50", "השירים המושמעים ביותר בז׳אנר", "נתוני המצעד: Last.fm", "בונים את המצעד…", "להציג הכול", "להציג פחות", "להיטים לפי ז׳אנר", "מצעדים לפי ז׳אנר"),
    "hu" to GenreChartCopy("Top 50", "A műfaj legtöbbet hallgatott dalai", "Listaadatok: Last.fm", "Összeállítjuk a listát…", "Összes mutatása", "Kevesebb", "Műfajok slágerei", "Listák műfajonként"),
    "fi" to GenreChartCopy("Top 50", "Tyylilajin kuunnelluimmat kappaleet", "Listan tiedot: Last.fm", "Kootaan listaa…", "Näytä kaikki", "Näytä vähemmän", "Tyylilajien hitit", "Listat tyylilajeittain"),
    "bg" to GenreChartCopy("Топ 50", "Най-слушаните песни в жанра", "Данни за класацията: Last.fm", "Подготвяме класацията…", "Покажи всички", "Покажи по-малко", "Хитове по жанрове", "Класации по жанрове"),
    "hr" to GenreChartCopy("Top 50", "Najslušanije pjesme ovog žanra", "Podaci ljestvice: Last.fm", "Slažemo ljestvicu…", "Prikaži sve", "Prikaži manje", "Hitovi po žanrovima", "Ljestvice po žanrovima"),
    "sr" to GenreChartCopy("Топ 50", "Најслушаније песме овог жанра", "Подаци листе: Last.fm", "Слажемо листу…", "Прикажи све", "Прикажи мање", "Хитови по жанровима", "Листе по жанровима"),
    "sk" to GenreChartCopy("Top 50", "Najpočúvanejšie skladby žánru", "Údaje rebríčka: Last.fm", "Zostavujeme rebríček…", "Zobraziť všetko", "Zobraziť menej", "Hity žánrov", "Rebríčky podľa žánru"),
    "et" to GenreChartCopy("Top 50", "Žanri enim kuulatud lood", "Edetabeli andmed: Last.fm", "Koostame edetabelit…", "Näita kõiki", "Näita vähem", "Žanrite hitid", "Edetabelid žanrite kaupa"),
    "sq" to GenreChartCopy("Top 50", "Këngët më të dëgjuara të zhanrit", "Të dhënat e listës: Last.fm", "Po përgatisim listën…", "Shfaq të gjitha", "Shfaq më pak", "Hitet sipas zhanrit", "Listat sipas zhanrit")
)

internal fun genreChartCopy(code: String): GenreChartCopy = localizedValueOrEnglish(genreChartCopies, code)

internal fun genreChartLocalizationCodes(): Set<String> = genreChartCopies.keys

internal data class GenreChartGenreNames(
    val kpop: String,
    val metal: String,
    val indie: String,
    val jazz: String
)

private val genreChartGenreNames = mapOf(
    "en" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "it" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "es" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "fr" to GenreChartGenreNames("K-pop", "Métal", "Indé", "Jazz"),
    "de" to GenreChartGenreNames("K-Pop", "Metal", "Indie", "Jazz"),
    "pt" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "pt-BR" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "nl" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "pl" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "ro" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "el" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Τζαζ"),
    "sv" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "da" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "cs" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "uk" to GenreChartGenreNames("K-pop", "Метал", "Інді", "Джаз"),
    "ru" to GenreChartGenreNames("K-pop", "Метал", "Инди", "Джаз"),
    "nb" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "ca" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "kk" to GenreChartGenreNames("K-pop", "Метал", "Инди", "Джаз"),
    "tr" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Caz"),
    "ar" to GenreChartGenreNames("كي-بوب", "ميتال", "إندي", "جاز"),
    "fa" to GenreChartGenreNames("کی‌پاپ", "متال", "ایندی", "جز"),
    "zh" to GenreChartGenreNames("K-pop", "金属", "独立音乐", "爵士"),
    "zh-Hant" to GenreChartGenreNames("K-pop", "金屬", "獨立音樂", "爵士"),
    "ja" to GenreChartGenreNames("K-POP", "メタル", "インディー", "ジャズ"),
    "ko" to GenreChartGenreNames("케이팝", "메탈", "인디", "재즈"),
    "hi" to GenreChartGenreNames("के-पॉप", "मेटल", "इंडी", "जैज़"),
    "bn" to GenreChartGenreNames("কে-পপ", "মেটাল", "ইন্ডি", "জ্যাজ"),
    "id" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "ms" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jaz"),
    "vi" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "th" to GenreChartGenreNames("เคป๊อป", "เมทัล", "อินดี้", "แจ๊ส"),
    "fil" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "he" to GenreChartGenreNames("קיי-פופ", "מטאל", "אינדי", "ג׳אז"),
    "hu" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Dzsessz"),
    "fi" to GenreChartGenreNames("K-pop", "Metalli", "Indie", "Jazz"),
    "bg" to GenreChartGenreNames("K-pop", "Метъл", "Инди", "Джаз"),
    "hr" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "sr" to GenreChartGenreNames("K-pop", "Метал", "Инди", "Џез"),
    "sk" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Jazz"),
    "et" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Džäss"),
    "sq" to GenreChartGenreNames("K-pop", "Metal", "Indie", "Xhaz")
)

internal fun genreChartGenreNames(code: String): GenreChartGenreNames = localizedValueOrEnglish(genreChartGenreNames, code)

internal fun genreChartGenreNameCodes(): Set<String> = genreChartGenreNames.keys
