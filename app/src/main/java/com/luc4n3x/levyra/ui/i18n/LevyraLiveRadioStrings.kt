package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.feature.radio.RadioCategory

internal data class LevyraLiveRadioStrings(
    val title: String,
    val subtitle: String,
    val exploreSubtitle: String,
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
        "sk" to listOf("Populárne", "Hudba", "Pop", "Rock", "Hip-Hop", "Elektronická", "Tanečná", "Chill", "Jazz", "Klasická", "Správy", "Hovorené slovo", "Šport", "Miestne", "Celý svet"),
        "hr" to listOf("Popularno", "Glazba", "Pop", "Rock", "Hip-Hop", "Elektronička", "Plesna", "Chill", "Jazz", "Klasična", "Vijesti", "Razgovor", "Sport", "Lokalno", "Cijeli svijet"),
        "bg" to listOf("Популярно", "Музика", "Поп", "Рок", "Хип-хоп", "Електронна", "Денс", "Чил", "Джаз", "Класическа", "Новини", "Разговори", "Спорт", "Местни", "Цял свят"),
        "hu" to listOf("Népszerű", "Zene", "Pop", "Rock", "Hip-hop", "Elektronikus", "Dance", "Chill", "Jazz", "Klasszikus", "Hírek", "Beszélgetés", "Sport", "Helyi", "Világszerte"),
        "fi" to listOf("Suositut", "Musiikki", "Pop", "Rock", "Hip-hop", "Elektroninen", "Dance", "Chill", "Jazz", "Klassinen", "Uutiset", "Puhe", "Urheilu", "Paikallinen", "Maailmanlaajuinen"),
        "et" to listOf("Populaarsed", "Muusika", "Pop", "Rock", "Hip-hop", "Elektrooniline", "Tantsumuusika", "Chill", "Jazz", "Klassikaline", "Uudised", "Kõne", "Sport", "Kohalik", "Ülemaailmne"),
        "nb" to listOf("Populært", "Musikk", "Pop", "Rock", "Hip-hop", "Elektronisk", "Dance", "Chill", "Jazz", "Klassisk", "Nyheter", "Prat", "Sport", "Lokalt", "Hele verden"),
        "ca" to listOf("Popular", "Música", "Pop", "Rock", "Hip-hop", "Electrònica", "Dance", "Chill", "Jazz", "Clàssica", "Notícies", "Parla", "Esports", "Local", "Tot el món"),
        "uk" to listOf("Популярні", "Музика", "Поп", "Рок", "Хіп-хоп", "Електронна", "Танцювальна", "Чіл", "Джаз", "Класична", "Новини", "Розмовні", "Спорт", "Місцеві", "Увесь світ"),
        "ru" to listOf("Популярное", "Музыка", "Поп", "Рок", "Хип-хоп", "Электронная", "Танцевальная", "Чилл", "Джаз", "Классика", "Новости", "Разговорное", "Спорт", "Местное", "Весь мир"),
        "tr" to listOf("Popüler", "Müzik", "Pop", "Rock", "Hip-Hop", "Elektronik", "Dans", "Chill", "Caz", "Klasik", "Haber", "Sohbet", "Spor", "Yerel", "Dünya"),
        "ar" to listOf("الأكثر شعبية", "موسيقى", "بوب", "روك", "هيب هوب", "إلكترونية", "رقص", "هادئة", "جاز", "كلاسيكية", "أخبار", "حواري", "رياضة", "محلية", "حول العالم"),
        "fa" to listOf("محبوب", "موسیقی", "پاپ", "راک", "هیپ‌هاپ", "الکترونیک", "رقص", "آرام", "جاز", "کلاسیک", "اخبار", "گفت‌وگو", "ورزش", "محلی", "سراسر جهان"),
        "zh" to listOf("热门", "音乐", "流行", "摇滚", "嘻哈", "电子", "舞曲", "轻松", "爵士", "古典", "新闻", "谈话", "体育", "本地", "全球"),
        "zh-Hant" to listOf("熱門", "音樂", "流行", "搖滾", "嘻哈", "電子", "舞曲", "輕鬆", "爵士", "古典", "新聞", "談話", "體育", "本地", "全球"),
        "ja" to listOf("人気", "音楽", "ポップ", "ロック", "ヒップホップ", "エレクトロニック", "ダンス", "チル", "ジャズ", "クラシック", "ニュース", "トーク", "スポーツ", "ローカル", "世界"),
        "ko" to listOf("인기", "음악", "팝", "록", "힙합", "일렉트로닉", "댄스", "칠", "재즈", "클래식", "뉴스", "토크", "스포츠", "지역", "전 세계"),
        "hi" to listOf("लोकप्रिय", "संगीत", "पॉप", "रॉक", "हिप-हॉप", "इलेक्ट्रॉनिक", "डांस", "चिल", "जैज़", "शास्त्रीय", "समाचार", "वार्ता", "खेल", "स्थानीय", "दुनिया भर"),
        "id" to listOf("Populer", "Musik", "Pop", "Rock", "Hip-Hop", "Elektronik", "Dance", "Chill", "Jazz", "Klasik", "Berita", "Bincang", "Olahraga", "Lokal", "Seluruh dunia"),
        "ms" to listOf("Popular", "Muzik", "Pop", "Rock", "Hip-Hop", "Elektronik", "Tarian", "Santai", "Jazz", "Klasik", "Berita", "Bual bicara", "Sukan", "Tempatan", "Seluruh dunia"),
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
        "cs" to "Živý přenos není dostupný",
        "sk" to "Živé vysielanie nie je dostupné",
        "hr" to "Prijenos uživo nije dostupan",
        "bg" to "Предаването на живо не е достъпно",
        "hu" to "Az élő közvetítés nem érhető el",
        "fi" to "Suoratoisto ei ole saatavilla",
        "et" to "Otseülekanne pole saadaval",
        "nb" to "Direktestrømmen er ikke tilgjengelig",
        "ca" to "L'emissió en directe no està disponible",
        "uk" to "Пряма трансляція недоступна",
        "ru" to "Прямая трансляция недоступна", "tr" to "Canlı yayın kullanılamıyor",
        "ar" to "البث المباشر غير متاح",
        "fa" to "پخش زنده در دسترس نیست",
        "zh" to "直播流不可用",
        "zh-Hant" to "無法使用直播串流",
        "ja" to "ライブ配信を利用できません", "ko" to "라이브 스트림을 사용할 수 없습니다",
        "hi" to "लाइव स्ट्रीम उपलब्ध नहीं है", "id" to "Streaming langsung tidak tersedia",
        "ms" to "Strim langsung tidak tersedia",
        "vi" to "Luồng trực tiếp không khả dụng", "th" to "สตรีมสดไม่พร้อมใช้งาน",
        "fil" to "Hindi available ang live stream", "he" to "השידור החי אינו זמין"
    )

    private val values = mapOf(
        "en" to radio("Live stations from around the world", "Live stations", "Popular in %s", "Worldwide", "Categories", "Countries", "Languages", "Search by name, country, tag or language", "No matching live stations", "Showing saved stations while the catalog reconnects", "An Internet connection is required to play live radio", "Load more", englishCategories),
        "it" to radio("Stazioni in diretta da tutto il mondo", "Stazioni in diretta", "Popolari in %s", "Tutto il mondo", "Categorie", "Paesi", "Lingue", "Cerca per nome, paese, genere o lingua", "Nessuna stazione live corrispondente", "Stazioni salvate visibili mentre il catalogo si riconnette", "Serve una connessione Internet per ascoltare la radio live", "Carica altre", listOf("Popolari", "Musica", "Pop", "Rock", "Hip-Hop", "Elettronica", "Dance", "Chill", "Jazz", "Classica", "Notizie", "Talk", "Sport", "Locali", "Tutto il mondo")),
        "es" to radio("Emisoras en directo de todo el mundo", "Emisoras en directo", "Popular en %s", "Todo el mundo", "Categorías", "Países", "Idiomas", "Buscar por nombre, país, etiqueta o idioma", "No hay emisoras en directo", "Mostrando emisoras guardadas mientras vuelve el catálogo", "Se necesita Internet para escuchar radio en directo", "Cargar más", listOf("Popular", "Música", "Pop", "Rock", "Hip-Hop", "Electrónica", "Dance", "Chill", "Jazz", "Clásica", "Noticias", "Charlas", "Deporte", "Local", "Mundial")),
        "fr" to radio("Stations en direct du monde entier", "Stations en direct", "Populaires en %s", "Monde entier", "Catégories", "Pays", "Langues", "Rechercher par nom, pays, tag ou langue", "Aucune station en direct", "Stations enregistrées affichées pendant la reconnexion", "Une connexion Internet est requise pour la radio en direct", "Charger plus", listOf("Populaires", "Musique", "Pop", "Rock", "Hip-Hop", "Électronique", "Dance", "Chill", "Jazz", "Classique", "Actualités", "Talk", "Sport", "Local", "Monde")),
        "de" to radio("Live-Sender aus aller Welt", "Live-Sender", "Beliebt in %s", "Weltweit", "Kategorien", "Länder", "Sprachen", "Nach Name, Land, Tag oder Sprache suchen", "Keine passenden Live-Sender", "Gespeicherte Sender während der Wiederverbindung", "Für Live-Radio ist Internet erforderlich", "Mehr laden", listOf("Beliebt", "Musik", "Pop", "Rock", "Hip-Hop", "Electronic", "Dance", "Chill", "Jazz", "Klassik", "Nachrichten", "Talk", "Sport", "Lokal", "Weltweit")),
        "pt" to radio("Estações ao vivo de todo o mundo", "Estações ao vivo", "Popular em %s", "Mundo inteiro", "Categorias", "Países", "Idiomas", "Pesquisar por nome, país, etiqueta ou idioma", "Nenhuma estação ao vivo", "A mostrar estações guardadas durante a reconexão", "É necessária Internet para ouvir rádio ao vivo", "Carregar mais", listOf("Popular", "Música", "Pop", "Rock", "Hip-Hop", "Eletrónica", "Dance", "Chill", "Jazz", "Clássica", "Notícias", "Talk", "Desporto", "Local", "Mundial")),
        "nl" to radio("Livezenders van over de hele wereld", "Livezenders", "Populair in %s", "Wereldwijd", "Categorieën", "Landen", "Talen", "Zoek op naam, land, tag of taal", "Geen passende livezenders", "Opgeslagen zenders worden getoond tijdens verbinden", "Internet is nodig voor live radio", "Meer laden", englishCategories),
        "pl" to radio("Stacje na żywo z całego świata", "Stacje na żywo", "Popularne w %s", "Cały świat", "Kategorie", "Kraje", "Języki", "Szukaj po nazwie, kraju, tagu lub języku", "Brak pasujących stacji", "Pokazujemy zapisane stacje podczas ponownego łączenia", "Radio na żywo wymaga Internetu", "Wczytaj więcej", englishCategories),
        "ro" to radio("Posturi live din toată lumea", "Posturi live", "Populare în %s", "Toată lumea", "Categorii", "Țări", "Limbi", "Caută după nume, țară, etichetă sau limbă", "Niciun post live potrivit", "Se afișează posturile salvate în timpul reconectării", "Este necesar Internet pentru radio live", "Încarcă mai multe", englishCategories),
        "el" to radio("Ζωντανοί σταθμοί από όλο τον κόσμο", "Ζωντανοί σταθμοί", "Δημοφιλή στην %s", "Παγκοσμίως", "Κατηγορίες", "Χώρες", "Γλώσσες", "Αναζήτηση με όνομα, χώρα, ετικέτα ή γλώσσα", "Δεν βρέθηκαν σταθμοί", "Προβολή αποθηκευμένων σταθμών", "Απαιτείται Internet για live radio", "Φόρτωση περισσότερων", englishCategories),
        "sv" to radio("Livestationer från hela världen", "Livestationer", "Populärt i %s", "Hela världen", "Kategorier", "Länder", "Språk", "Sök på namn, land, tagg eller språk", "Inga matchande stationer", "Visar sparade stationer under återanslutning", "Internet krävs för live-radio", "Ladda fler", englishCategories),
        "da" to radio("Live-stationer fra hele verden", "Live-stationer", "Populært i %s", "Hele verden", "Kategorier", "Lande", "Sprog", "Søg efter navn, land, tag eller sprog", "Ingen matchende stationer", "Viser gemte stationer under genforbindelse", "Internet kræves til live-radio", "Indlæs flere", englishCategories),
        "cs" to radio("Živé stanice z celého světa", "Živé stanice", "Populární v %s", "Celý svět", "Kategorie", "Země", "Jazyky", "Hledat podle názvu, země, tagu nebo jazyka", "Žádné odpovídající stanice", "Během připojování se zobrazují uložené stanice", "Živé rádio vyžaduje Internet", "Načíst další", englishCategories),
        "sk" to radio("Živé stanice z celého sveta", "Živé stanice", "Populárne v %s", "Celý svet", "Kategórie", "Krajiny", "Jazyky", "Hľadať podľa názvu, krajiny, tagu alebo jazyka", "Nenašli sa žiadne zodpovedajúce živé stanice", "Počas opätovného pripájania katalógu sa zobrazujú uložené stanice", "Na prehrávanie živého rádia je potrebné internetové pripojenie", "Načítať ďalšie", englishCategories),
        "hr" to radio("Postaje uživo iz cijelog svijeta", "Postaje uživo", "Popularno u %s", "Cijeli svijet", "Kategorije", "Zemlje", "Jezici", "Pretraži po nazivu, zemlji, oznaci ili jeziku", "Nema odgovarajućih postaja uživo", "Prikazuju se spremljene postaje dok se katalog ponovno povezuje", "Za reprodukciju radija uživo potrebna je internetska veza", "Učitaj više", englishCategories),
        "bg" to radio("Радиостанции на живо от цял свят", "Радиостанции на живо", "Популярно в %s", "Цял свят", "Категории", "Държави", "Езици", "Търси по име, държава, етикет или език", "Няма съответстващи радиостанции на живо", "Показват се запазените станции, докато каталогът се свързва отново", "За слушане на радио на живо е необходима интернет връзка", "Зареди още", englishCategories),
        "hu" to radio("Élő rádióállomások a világ minden tájáról", "Élő állomások", "Népszerű itt: %s", "Világszerte", "Kategóriák", "Országok", "Nyelvek", "Keresés név, ország, címke vagy nyelv alapján", "Nincs megfelelő élő állomás", "A mentett állomások láthatók, amíg a katalógus újracsatlakozik", "Az élő rádió lejátszásához internetkapcsolat szükséges", "Továbbiak betöltése", englishCategories),
        "fi" to radio("Suoria radioasemia ympäri maailmaa", "Suorat asemat", "Suosittu maassa %s", "Maailmanlaajuinen", "Kategoriat", "Maat", "Kielet", "Hae nimellä, maalla, tunnisteella tai kielellä", "Ei vastaavia suoria asemia", "Tallennetut asemat näytetään, kun luettelo muodostaa yhteyttä uudelleen", "Suoran radion kuuntelu vaatii internetyhteyden", "Lataa lisää", englishCategories),
        "et" to radio("Otseülekandega jaamad üle maailma", "Otsejaamad", "Populaarsed riigis %s", "Ülemaailmne", "Kategooriad", "Riigid", "Keeled", "Otsi nime, riigi, sildi või keele järgi", "Vastavaid otsejaamu ei leitud", "Kuni kataloogi taasühendamiseni kuvatakse salvestatud jaamad", "Otseraadio kuulamiseks on vajalik internetiühendus", "Laadi rohkem", englishCategories),
        "nb" to radio("Direktesendte radiostasjoner fra hele verden", "Direktestasjoner", "Populært i %s", "Hele verden", "Kategorier", "Land", "Språk", "Søk etter navn, land, tagg eller språk", "Ingen matchende direktestasjoner", "Viser lagrede stasjoner mens katalogen kobler til på nytt", "Du trenger internettilkobling for å spille direktesendt radio", "Last inn mer", englishCategories),
        "ca" to radio("Emissores en directe d'arreu del món", "Emissores en directe", "Popular a %s", "Tot el món", "Categories", "Països", "Idiomes", "Cerca per nom, país, etiqueta o idioma", "No hi ha emissores en directe que coincideixin", "Es mostren les emissores desades mentre el catàleg es torna a connectar", "Cal connexió a internet per reproduir ràdio en directe", "Carrega'n més", englishCategories),
        "uk" to radio("Живі станції з усього світу", "Живі станції", "Популярні в %s", "Увесь світ", "Категорії", "Країни", "Мови", "Пошук за назвою, країною, тегом або мовою", "Станцій не знайдено", "Показано збережені станції", "Для живого радіо потрібен інтернет", "Завантажити ще", englishCategories),
        "ru" to radio("Радиостанции со всего мира", "Онлайн-радио", "Популярно в %s", "Весь мир", "Категории", "Страны", "Языки", "Поиск по названию, стране, тегу или языку", "Станции не найдены", "Показаны сохранённые станции", "Для радио нужен интернет", "Загрузить ещё", englishCategories),
        "tr" to radio("Dünyanın dört bir yanından canlı istasyonlar", "Canlı istasyonlar", "%s içinde popüler", "Dünya çapında", "Kategoriler", "Ülkeler", "Diller", "Ad, ülke, etiket veya dile göre ara", "Eşleşen canlı istasyon yok", "Yeniden bağlanırken kayıtlı istasyonlar gösteriliyor", "Canlı radyo için internet gerekir", "Daha fazla yükle", englishCategories),
        "ar" to radio("محطات مباشرة من حول العالم", "محطات مباشرة", "الأكثر شعبية في %s", "حول العالم", "الفئات", "البلدان", "اللغات", "ابحث بالاسم أو البلد أو الوسم أو اللغة", "لا توجد محطات مطابقة", "عرض المحطات المحفوظة", "يلزم الاتصال بالإنترنت للراديو المباشر", "تحميل المزيد", englishCategories),
        "fa" to radio("ایستگاه‌های زنده از سراسر جهان", "ایستگاه‌های زنده", "محبوب در %s", "سراسر جهان", "دسته‌بندی‌ها", "کشورها", "زبان‌ها", "جستجو بر اساس نام، کشور، برچسب یا زبان", "هیچ ایستگاه زنده‌ای پیدا نشد", "تا زمان اتصال دوباره فهرست، ایستگاه‌های ذخیره‌شده نمایش داده می‌شوند", "برای پخش رادیوی زنده به اتصال اینترنت نیاز است", "بیشتر", englishCategories),
        "zh" to radio("来自世界各地的直播电台", "直播电台", "%s 热门", "全球", "分类", "国家和地区", "语言", "按名称、国家、标签或语言搜索", "未找到匹配的直播电台", "重新连接时显示已保存的电台", "收听直播电台需要网络", "加载更多", englishCategories),
        "zh-Hant" to radio("來自世界各地的直播電台", "直播電台", "%s 熱門", "全球", "分類", "國家與地區", "語言", "依名稱、國家、標籤或語言搜尋", "找不到符合的直播電台", "目錄重新連線時顯示已儲存的電台", "收聽直播電台需要網路連線", "載入更多", englishCategories),
        "ja" to radio("世界中のライブラジオ", "ライブラジオ", "%s で人気", "世界中", "カテゴリ", "国", "言語", "名前、国、タグ、言語で検索", "該当するライブ局はありません", "再接続中は保存済みの局を表示します", "ライブラジオにはインターネットが必要です", "さらに読み込む", englishCategories),
        "ko" to radio("전 세계 라이브 라디오", "라이브 라디오", "%s 인기 방송", "전 세계", "카테고리", "국가", "언어", "이름, 국가, 태그 또는 언어로 검색", "일치하는 라이브 방송이 없습니다", "재연결 중 저장된 방송을 표시합니다", "라이브 라디오에는 인터넷이 필요합니다", "더 불러오기", englishCategories),
        "hi" to radio("दुनिया भर के लाइव स्टेशन", "लाइव स्टेशन", "%s में लोकप्रिय", "दुनिया भर", "श्रेणियाँ", "देश", "भाषाएँ", "नाम, देश, टैग या भाषा से खोजें", "कोई लाइव स्टेशन नहीं मिला", "दोबारा जुड़ते समय सहेजे स्टेशन दिखाए जा रहे हैं", "लाइव रेडियो के लिए इंटरनेट चाहिए", "और लोड करें", englishCategories),
        "id" to radio("Stasiun langsung dari seluruh dunia", "Stasiun langsung", "Populer di %s", "Seluruh dunia", "Kategori", "Negara", "Bahasa", "Cari berdasarkan nama, negara, tag, atau bahasa", "Tidak ada stasiun langsung yang cocok", "Menampilkan stasiun tersimpan saat menyambung kembali", "Internet diperlukan untuk radio langsung", "Muat lainnya", englishCategories),
        "ms" to radio("Stesen langsung dari seluruh dunia", "Stesen langsung", "Popular di %s", "Seluruh dunia", "Kategori", "Negara", "Bahasa", "Cari mengikut nama, negara, tag atau bahasa", "Tiada stesen langsung yang sepadan", "Stesen tersimpan dipaparkan sementara katalog menyambung semula", "Sambungan internet diperlukan untuk memainkan radio langsung", "Muat lagi", englishCategories),
        "vi" to radio("Các đài phát trực tiếp trên khắp thế giới", "Đài trực tiếp", "Phổ biến tại %s", "Toàn thế giới", "Danh mục", "Quốc gia", "Ngôn ngữ", "Tìm theo tên, quốc gia, thẻ hoặc ngôn ngữ", "Không có đài trực tiếp phù hợp", "Hiển thị đài đã lưu trong khi kết nối lại", "Cần Internet để nghe radio trực tiếp", "Tải thêm", englishCategories),
        "th" to radio("สถานีสดจากทั่วโลก", "สถานีสด", "ยอดนิยมใน %s", "ทั่วโลก", "หมวดหมู่", "ประเทศ", "ภาษา", "ค้นหาตามชื่อ ประเทศ แท็ก หรือภาษา", "ไม่พบสถานีสดที่ตรงกัน", "แสดงสถานีที่บันทึกไว้ระหว่างเชื่อมต่อใหม่", "ต้องใช้อินเทอร์เน็ตสำหรับวิทยุสด", "โหลดเพิ่ม", englishCategories),
        "fil" to radio("Mga live station mula sa buong mundo", "Mga live station", "Sikat sa %s", "Buong mundo", "Mga kategorya", "Mga bansa", "Mga wika", "Maghanap ayon sa pangalan, bansa, tag, o wika", "Walang katugmang live station", "Ipinapakita ang mga naka-save na station habang kumokonekta", "Kailangan ng Internet para sa live radio", "Mag-load pa", englishCategories),
        "he" to radio("תחנות חיות מכל העולם", "תחנות חיות", "פופולרי ב%s", "בכל העולם", "קטגוריות", "מדינות", "שפות", "חיפוש לפי שם, מדינה, תגית או שפה", "לא נמצאו תחנות חיות", "מציג תחנות שמורות בזמן החיבור", "נדרש אינטרנט לרדיו חי", "טען עוד", englishCategories)
    ).mapValues { (code, strings) ->
        strings.copy(categoryLabels = localizedValueOrEnglish(localizedCategories, code))
    }

    init {
        val languageCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        check(values.keys == languageCodes)
        check(localizedCategories.keys == languageCodes)
        check(unavailableValues.keys == languageCodes)
        check(localizedCategories.values.all { it.size == RadioCategory.entries.size })
        check(values.values.all { it.exploreSubtitle.isNotBlank() })
    }

    fun forCode(code: String): LevyraLiveRadioStrings =
        localizedValueOrEnglish(values, LevyraLanguageCatalog.normalize(code))

    fun streamUnavailable(code: String): String =
        localizedValueOrEnglish(unavailableValues, LevyraLanguageCatalog.normalize(code))

    private fun radio(
        subtitle: String,
        exploreSubtitle: String,
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
        exploreSubtitle,
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
