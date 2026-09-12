package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.feature.radio.RadioCategory

internal data class LevyraLiveRadioStrings(
    val title: String,
    val subtitle: String,
    val popularIn: String,
    val worldwide: String,
    val categories: String,
    val countries: String,
    val languages: String,
    val searchHint: String,
    val noStations: String,
    val cachedNotice: String,
    val internetRequired: String,
    val loadMore: String,
    val live: String,
    val categoryLabels: List<String>
) {
    fun popularIn(country: String): String = popularIn.replace("%s", country)
    fun category(category: RadioCategory): String = categoryLabels[category.ordinal]
}

internal object LevyraLiveRadioCatalog {
    private val englishCategories = listOf(
        "Popular", "Music", "Pop", "Rock", "Hip-Hop", "Electronic", "Dance", "Chill", "Jazz",
        "Classical", "News", "Talk", "Sport", "Local", "Worldwide"
    )

    private val localizedCategories = mapOf(
        "en" to englishCategories,
        "it" to listOf("Popolari", "Musica", "Pop", "Rock", "Hip-Hop", "Elettronica", "Dance", "Chill", "Jazz", "Classica", "Notizie", "Talk", "Sport", "Locali", "Tutto il mondo"),
        "es" to listOf("Popular", "Música", "Pop", "Rock", "Hip-Hop", "Electrónica", "Dance", "Chill", "Jazz", "Clásica", "Noticias", "Charlas", "Deporte", "Local", "Mundial"),
        "fr" to listOf("Populaires", "Musique", "Pop", "Rock", "Hip-Hop", "Électronique", "Dance", "Chill", "Jazz", "Classique", "Actualités", "Talk", "Sport", "Local", "Monde"),
        "de" to listOf("Beliebt", "Musik", "Pop", "Rock", "Hip-Hop", "Elektronisch", "Dance", "Chill", "Jazz", "Klassik", "Nachrichten", "Talk", "Sport", "Lokal", "Weltweit"),
        "pt" to listOf("Popular", "Música", "Pop", "Rock", "Hip-Hop", "Eletrónica", "Dance", "Chill", "Jazz", "Clássica", "Notícias", "Talk", "Desporto", "Local", "Mundial"),
        "nl" to listOf("Populair", "Muziek", "Pop", "Rock", "Hip-Hop", "Elektronisch", "Dance", "Chill", "Jazz", "Klassiek", "Nieuws", "Praat", "Sport", "Lokaal", "Wereldwijd"),
        "pl" to listOf("Popularne", "Muzyka", "Pop", "Rock", "Hip-Hop", "Elektroniczna", "Dance", "Chill", "Jazz", "Klasyczna", "Wiadomości", "Rozmowy", "Sport", "Lokalne", "Świat"),
        "ro" to listOf("Populare", "Muzică", "Pop", "Rock", "Hip-Hop", "Electronică", "Dance", "Chill", "Jazz", "Clasică", "Știri", "Discuții", "Sport", "Locale", "Global"),
        "el" to listOf("Δημοφιλή", "Μουσική", "Pop", "Rock", "Hip-Hop", "Ηλεκτρονική", "Dance", "Chill", "Jazz", "Κλασική", "Ειδήσεις", "Συζήτηση", "Αθλητικά", "Τοπικά", "Παγκόσμια"),
        "sv" to listOf("Populärt", "Musik", "Pop", "Rock", "Hip-Hop", "Elektroniskt", "Dance", "Chill", "Jazz", "Klassiskt", "Nyheter", "Prat", "Sport", "Lokalt", "Världen"),
        "da" to listOf("Populært", "Musik", "Pop", "Rock", "Hip-Hop", "Elektronisk", "Dance", "Chill", "Jazz", "Klassisk", "Nyheder", "Tale", "Sport", "Lokalt", "Hele verden"),
        "cs" to listOf("Populární", "Hudba", "Pop", "Rock", "Hip-Hop", "Elektronická", "Dance", "Chill", "Jazz", "Klasická", "Zprávy", "Mluvené", "Sport", "Místní", "Celý svět"),
        "uk" to listOf("Популярні", "Музика", "Поп", "Рок", "Хіп-хоп", "Електронна", "Танцювальна", "Чіл", "Джаз", "Класична", "Новини", "Розмовні", "Спорт", "Місцеві", "Увесь світ"),
        "ru" to listOf("Популярное", "Музыка", "Поп", "Рок", "Хип-хоп", "Электронная", "Танцевальная", "Чилл", "Джаз", "Классика", "Новости", "Разговорное", "Спорт", "Местное", "Весь мир"),
        "tr" to listOf("Popüler", "Müzik", "Pop", "Rock", "Hip-Hop", "Elektronik", "Dans", "Chill", "Caz", "Klasik", "Haber", "Sohbet", "Spor", "Yerel", "Dünya"),
        "ar" to listOf("الأكثر شعبية", "موسيقى", "بوب", "روك", "هيب هوب", "إلكترونية", "رقص", "هادئة", "جاز", "كلاسيكية", "أخبار", "حواري", "رياضة", "محلية", "حول العالم"),
        "zh" to listOf("热门", "音乐", "流行", "摇滚", "嘻哈", "电子", "舞曲", "轻松", "爵士", "古典", "新闻", "谈话", "体育", "本地", "全球"),
        "ja" to listOf("人気", "音楽", "ポップ", "ロック", "ヒップホップ", "エレクトロニック", "ダンス", "チル", "ジャズ", "クラシック", "ニュース", "トーク", "スポーツ", "ローカル", "世界"),
        "ko" to listOf("인기", "음악", "팝", "록", "힙합", "일렉트로닉", "댄스", "칠", "재즈", "클래식", "뉴스", "토크", "스포츠", "지역", "전 세계"),
        "hi" to listOf("लोकप्रिय", "संगीत", "पॉप", "रॉक", "हिप-हॉप", "इलेक्ट्रॉनिक", "डांस", "चिल", "जैज़", "शास्त्रीय", "समाचार", "वार्ता", "खेल", "स्थानीय", "दुनिया भर"),
        "id" to listOf("Populer", "Musik", "Pop", "Rock", "Hip-Hop", "Elektronik", "Dance", "Chill", "Jazz", "Klasik", "Berita", "Bincang", "Olahraga", "Lokal", "Seluruh dunia"),
        "vi" to listOf("Phổ biến", "Âm nhạc", "Pop", "Rock", "Hip-Hop", "Điện tử", "Dance", "Chill", "Jazz", "Cổ điển", "Tin tức", "Trò chuyện", "Thể thao", "Địa phương", "Toàn thế giới"),
        "th" to listOf("ยอดนิยม", "เพลง", "ป๊อป", "ร็อก", "ฮิปฮอป", "อิเล็กทรอนิกส์", "แดนซ์", "ชิล", "แจ๊ส", "คลาสสิก", "ข่าว", "พูดคุย", "กีฬา", "ท้องถิ่น", "ทั่วโลก"),
        "fil" to listOf("Sikat", "Musika", "Pop", "Rock", "Hip-Hop", "Elektronika", "Dance", "Chill", "Jazz", "Klasikal", "Balita", "Usapan", "Isports", "Lokal", "Buong mundo"),
        "he" to listOf("פופולרי", "מוזיקה", "פופ", "רוק", "היפ-הופ", "אלקטרוני", "דאנס", "צ'יל", "ג'אז", "קלאסי", "חדשות", "שיחות", "ספורט", "מקומי", "כל העולם")
    )

    private val unavailableValues = mapOf(
        "en" to "Live stream unavailable", "it" to "Stream live non disponibile",
        "es" to "Emisión en directo no disponible", "fr" to "Flux en direct indisponible",
        "de" to "Live-Stream nicht verfügbar", "pt" to "Transmissão ao vivo indisponível",
        "nl" to "Livestream niet beschikbaar", "pl" to "Strumień na żywo niedostępny",
        "ro" to "Flux live indisponibil", "el" to "Η ζωντανή ροή δεν είναι διαθέσιμη",
        "sv" to "Liveströmmen är inte tillgänglig", "da" to "Livestream er ikke tilgængelig",
        "cs" to "Živý přenos není dostupný", "uk" to "Пряма трансляція недоступна",
        "ru" to "Прямая трансляция недоступна", "tr" to "Canlı yayın kullanılamıyor",
        "ar" to "البث المباشر غير متاح", "zh" to "直播流不可用",
        "ja" to "ライブ配信を利用できません", "ko" to "라이브 스트림을 사용할 수 없습니다",
        "hi" to "लाइव स्ट्रीम उपलब्ध नहीं है", "id" to "Streaming langsung tidak tersedia",
        "vi" to "Luồng trực tiếp không khả dụng", "th" to "สตรีมสดไม่พร้อมใช้งาน",
        "fil" to "Hindi available ang live stream", "he" to "השידור החי אינו זמין"
    )

    private val values = mapOf(
        "en" to radio("Live stations from around the world", "Popular in %s", "Worldwide", "Categories", "Countries", "Languages", "Search by name, country, tag or language", "No matching live stations", "Showing saved stations while the catalog reconnects", "An Internet connection is required to play live radio", "Load more", englishCategories),
        "it" to radio("Stazioni in diretta da tutto il mondo", "Popolari in %s", "Tutto il mondo", "Categorie", "Paesi", "Lingue", "Cerca per nome, paese, genere o lingua", "Nessuna stazione live corrispondente", "Stazioni salvate visibili mentre il catalogo si riconnette", "Serve una connessione Internet per ascoltare la radio live", "Carica altre", listOf("Popolari", "Musica", "Pop", "Rock", "Hip-Hop", "Elettronica", "Dance", "Chill", "Jazz", "Classica", "Notizie", "Talk", "Sport", "Locali", "Tutto il mondo")),
        "es" to radio("Emisoras en directo de todo el mundo", "Popular en %s", "Todo el mundo", "Categorías", "Países", "Idiomas", "Buscar por nombre, país, etiqueta o idioma", "No hay emisoras en directo", "Mostrando emisoras guardadas mientras vuelve el catálogo", "Se necesita Internet para escuchar radio en directo", "Cargar más", listOf("Popular", "Música", "Pop", "Rock", "Hip-Hop", "Electrónica", "Dance", "Chill", "Jazz", "Clásica", "Noticias", "Charlas", "Deporte", "Local", "Mundial")),
        "fr" to radio("Stations en direct du monde entier", "Populaires en %s", "Monde entier", "Catégories", "Pays", "Langues", "Rechercher par nom, pays, tag ou langue", "Aucune station en direct", "Stations enregistrées affichées pendant la reconnexion", "Une connexion Internet est requise pour la radio en direct", "Charger plus", listOf("Populaires", "Musique", "Pop", "Rock", "Hip-Hop", "Électronique", "Dance", "Chill", "Jazz", "Classique", "Actualités", "Talk", "Sport", "Local", "Monde")),
        "de" to radio("Live-Sender aus aller Welt", "Beliebt in %s", "Weltweit", "Kategorien", "Länder", "Sprachen", "Nach Name, Land, Tag oder Sprache suchen", "Keine passenden Live-Sender", "Gespeicherte Sender während der Wiederverbindung", "Für Live-Radio ist Internet erforderlich", "Mehr laden", listOf("Beliebt", "Musik", "Pop", "Rock", "Hip-Hop", "Electronic", "Dance", "Chill", "Jazz", "Klassik", "Nachrichten", "Talk", "Sport", "Lokal", "Weltweit")),
        "pt" to radio("Estações ao vivo de todo o mundo", "Popular em %s", "Mundo inteiro", "Categorias", "Países", "Idiomas", "Pesquisar por nome, país, etiqueta ou idioma", "Nenhuma estação ao vivo", "A mostrar estações guardadas durante a reconexão", "É necessária Internet para ouvir rádio ao vivo", "Carregar mais", listOf("Popular", "Música", "Pop", "Rock", "Hip-Hop", "Eletrónica", "Dance", "Chill", "Jazz", "Clássica", "Notícias", "Talk", "Desporto", "Local", "Mundial")),
        "nl" to radio("Livezenders van over de hele wereld", "Populair in %s", "Wereldwijd", "Categorieën", "Landen", "Talen", "Zoek op naam, land, tag of taal", "Geen passende livezenders", "Opgeslagen zenders worden getoond tijdens verbinden", "Internet is nodig voor live radio", "Meer laden", englishCategories),
        "pl" to radio("Stacje na żywo z całego świata", "Popularne w %s", "Cały świat", "Kategorie", "Kraje", "Języki", "Szukaj po nazwie, kraju, tagu lub języku", "Brak pasujących stacji", "Pokazujemy zapisane stacje podczas ponownego łączenia", "Radio na żywo wymaga Internetu", "Wczytaj więcej", englishCategories),
        "ro" to radio("Posturi live din toată lumea", "Populare în %s", "Toată lumea", "Categorii", "Țări", "Limbi", "Caută după nume, țară, etichetă sau limbă", "Niciun post live potrivit", "Se afișează posturile salvate în timpul reconectării", "Este necesar Internet pentru radio live", "Încarcă mai multe", englishCategories),
        "el" to radio("Ζωντανοί σταθμοί από όλο τον κόσμο", "Δημοφιλή στην %s", "Παγκοσμίως", "Κατηγορίες", "Χώρες", "Γλώσσες", "Αναζήτηση με όνομα, χώρα, ετικέτα ή γλώσσα", "Δεν βρέθηκαν σταθμοί", "Προβολή αποθηκευμένων σταθμών", "Απαιτείται Internet για live radio", "Φόρτωση περισσότερων", englishCategories),
        "sv" to radio("Livestationer från hela världen", "Populärt i %s", "Hela världen", "Kategorier", "Länder", "Språk", "Sök på namn, land, tagg eller språk", "Inga matchande stationer", "Visar sparade stationer under återanslutning", "Internet krävs för live-radio", "Ladda fler", englishCategories),
        "da" to radio("Live-stationer fra hele verden", "Populært i %s", "Hele verden", "Kategorier", "Lande", "Sprog", "Søg efter navn, land, tag eller sprog", "Ingen matchende stationer", "Viser gemte stationer under genforbindelse", "Internet kræves til live-radio", "Indlæs flere", englishCategories),
        "cs" to radio("Živé stanice z celého světa", "Populární v %s", "Celý svět", "Kategorie", "Země", "Jazyky", "Hledat podle názvu, země, tagu nebo jazyka", "Žádné odpovídající stanice", "Během připojování se zobrazují uložené stanice", "Živé rádio vyžaduje Internet", "Načíst další", englishCategories),
        "uk" to radio("Живі станції з усього світу", "Популярні в %s", "Увесь світ", "Категорії", "Країни", "Мови", "Пошук за назвою, країною, тегом або мовою", "Станцій не знайдено", "Показано збережені станції", "Для живого радіо потрібен Internet", "Завантажити ще", englishCategories),
        "ru" to radio("Радиостанции со всего мира", "Популярно в %s", "Весь мир", "Категории", "Страны", "Языки", "Поиск по названию, стране, тегу или языку", "Станции не найдены", "Показаны сохранённые станции", "Для радио нужен Internet", "Загрузить ещё", englishCategories),
        "tr" to radio("Dünyanın dört bir yanından canlı istasyonlar", "%s içinde popüler", "Dünya çapında", "Kategoriler", "Ülkeler", "Diller", "Ad, ülke, etiket veya dile göre ara", "Eşleşen canlı istasyon yok", "Yeniden bağlanırken kayıtlı istasyonlar gösteriliyor", "Canlı radyo için Internet gerekir", "Daha fazla yükle", englishCategories),
        "ar" to radio("محطات مباشرة من حول العالم", "الأكثر شعبية في %s", "حول العالم", "الفئات", "البلدان", "اللغات", "ابحث بالاسم أو البلد أو الوسم أو اللغة", "لا توجد محطات مطابقة", "عرض المحطات المحفوظة", "يلزم الاتصال بالإنترنت للراديو المباشر", "تحميل المزيد", englishCategories),
        "zh" to radio("来自世界各地的直播电台", "%s 热门", "全球", "分类", "国家和地区", "语言", "按名称、国家、标签或语言搜索", "未找到匹配的直播电台", "重新连接时显示已保存的电台", "收听直播电台需要网络", "加载更多", englishCategories),
        "ja" to radio("世界中のライブラジオ", "%s で人気", "世界中", "カテゴリ", "国", "言語", "名前、国、タグ、言語で検索", "該当するライブ局はありません", "再接続中は保存済みの局を表示します", "ライブラジオには Internet が必要です", "さらに読み込む", englishCategories),
        "ko" to radio("전 세계 라이브 라디오", "%s 인기 방송", "전 세계", "카테고리", "국가", "언어", "이름, 국가, 태그 또는 언어로 검색", "일치하는 라이브 방송이 없습니다", "재연결 중 저장된 방송을 표시합니다", "라이브 라디오에는 Internet이 필요합니다", "더 불러오기", englishCategories),
        "hi" to radio("दुनिया भर के लाइव स्टेशन", "%s में लोकप्रिय", "दुनिया भर", "श्रेणियाँ", "देश", "भाषाएँ", "नाम, देश, टैग या भाषा से खोजें", "कोई लाइव स्टेशन नहीं मिला", "दोबारा जुड़ते समय सहेजे स्टेशन दिखाए जा रहे हैं", "लाइव रेडियो के लिए Internet चाहिए", "और लोड करें", englishCategories),
        "id" to radio("Stasiun langsung dari seluruh dunia", "Populer di %s", "Seluruh dunia", "Kategori", "Negara", "Bahasa", "Cari berdasarkan nama, negara, tag, atau bahasa", "Tidak ada stasiun langsung yang cocok", "Menampilkan stasiun tersimpan saat menyambung kembali", "Internet diperlukan untuk radio langsung", "Muat lainnya", englishCategories),
        "vi" to radio("Các đài phát trực tiếp trên khắp thế giới", "Phổ biến tại %s", "Toàn thế giới", "Danh mục", "Quốc gia", "Ngôn ngữ", "Tìm theo tên, quốc gia, thẻ hoặc ngôn ngữ", "Không có đài trực tiếp phù hợp", "Hiển thị đài đã lưu trong khi kết nối lại", "Cần Internet để nghe radio trực tiếp", "Tải thêm", englishCategories),
        "th" to radio("สถานีสดจากทั่วโลก", "ยอดนิยมใน %s", "ทั่วโลก", "หมวดหมู่", "ประเทศ", "ภาษา", "ค้นหาตามชื่อ ประเทศ แท็ก หรือภาษา", "ไม่พบสถานีสดที่ตรงกัน", "แสดงสถานีที่บันทึกไว้ระหว่างเชื่อมต่อใหม่", "ต้องใช้ Internet สำหรับวิทยุสด", "โหลดเพิ่ม", englishCategories),
        "fil" to radio("Mga live station mula sa buong mundo", "Sikat sa %s", "Buong mundo", "Mga kategorya", "Mga bansa", "Mga wika", "Maghanap ayon sa pangalan, bansa, tag, o wika", "Walang katugmang live station", "Ipinapakita ang mga naka-save na station habang kumokonekta", "Kailangan ng Internet para sa live radio", "Mag-load pa", englishCategories),
        "he" to radio("תחנות חיות מכל העולם", "פופולרי ב%s", "בכל העולם", "קטגוריות", "מדינות", "שפות", "חיפוש לפי שם, מדינה, תגית או שפה", "לא נמצאו תחנות חיות", "מציג תחנות שמורות בזמן החיבור", "נדרש Internet לרדיו חי", "טען עוד", englishCategories)
    ).mapValues { (code, strings) ->
        strings.copy(categoryLabels = localizedCategories.getValue(code))
    }

    init {
        val languageCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        check(values.keys == languageCodes)
        check(localizedCategories.keys == languageCodes)
        check(unavailableValues.keys == languageCodes)
        check(localizedCategories.values.all { it.size == RadioCategory.entries.size })
    }

    fun forCode(code: String): LevyraLiveRadioStrings = values.getValue(LevyraLanguageCatalog.normalize(code))

    fun streamUnavailable(code: String): String = unavailableValues.getValue(LevyraLanguageCatalog.normalize(code))

    private fun radio(
        subtitle: String,
        popularIn: String,
        worldwide: String,
        categories: String,
        countries: String,
        languages: String,
        searchHint: String,
        noStations: String,
        cachedNotice: String,
        internetRequired: String,
        loadMore: String,
        categoryLabels: List<String>
    ) = LevyraLiveRadioStrings(
        "Live Radio",
        subtitle,
        popularIn,
        worldwide,
        categories,
        countries,
        languages,
        searchHint,
        noStations,
        cachedNotice,
        internetRequired,
        loadMore,
        "LIVE",
        categoryLabels
    ).also { require(it.categoryLabels.size == RadioCategory.entries.size) }
}
