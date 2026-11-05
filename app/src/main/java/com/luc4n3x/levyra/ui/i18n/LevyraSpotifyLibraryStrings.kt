package com.luc4n3x.levyra.ui.i18n

internal data class SpotifyLibraryCopy(
    val sortBy: String,
    val recents: String,
    val recentlyAdded: String,
    val alphabetical: String,
    val creator: String,
    val playlist: String,
    val gridView: String,
    val listView: String
)

private val spotifyLibraryCopies = mapOf(
    "en" to SpotifyLibraryCopy("Sort by", "Recents", "Recently added", "Alphabetical", "Creator", "Playlist", "Grid view", "List view"),
    "it" to SpotifyLibraryCopy("Ordina per", "Recenti", "Aggiunti di recente", "Alfabetico", "Autore", "Playlist", "Vista griglia", "Vista elenco"),
    "es" to SpotifyLibraryCopy("Ordenar por", "Recientes", "Añadidos recientemente", "Alfabético", "Creador", "Playlist", "Vista de cuadrícula", "Vista de lista"),
    "fr" to SpotifyLibraryCopy("Trier par", "Récents", "Ajoutés récemment", "Alphabétique", "Créateur", "Playlist", "Vue en grille", "Vue en liste"),
    "de" to SpotifyLibraryCopy("Sortieren nach", "Zuletzt gehört", "Zuletzt hinzugefügt", "Alphabetisch", "Ersteller", "Playlist", "Rasteransicht", "Listenansicht"),
    "pt" to SpotifyLibraryCopy("Ordenar por", "Recentes", "Adicionados recentemente", "Alfabética", "Criador", "Playlist", "Vista em grelha", "Vista em lista"),
    "pt-BR" to SpotifyLibraryCopy("Ordenar por", "Recentes", "Adicionados recentemente", "Ordem alfabética", "Criador", "Playlist", "Visualização em grade", "Visualização em lista"),
    "nl" to SpotifyLibraryCopy("Sorteren op", "Recent", "Recent toegevoegd", "Alfabetisch", "Maker", "Playlist", "Rasterweergave", "Lijstweergave"),
    "pl" to SpotifyLibraryCopy("Sortuj według", "Ostatnie", "Ostatnio dodane", "Alfabetycznie", "Twórca", "Playlista", "Widok siatki", "Widok listy"),
    "ro" to SpotifyLibraryCopy("Sortează după", "Recente", "Adăugate recent", "Alfabetic", "Creator", "Playlist", "Vizualizare grilă", "Vizualizare listă"),
    "el" to SpotifyLibraryCopy("Ταξινόμηση κατά", "Πρόσφατα", "Προστέθηκαν πρόσφατα", "Αλφαβητικά", "Δημιουργός", "Playlist", "Προβολή πλέγματος", "Προβολή λίστας"),
    "sv" to SpotifyLibraryCopy("Sortera efter", "Senaste", "Nyligen tillagda", "Alfabetisk", "Skapare", "Spellista", "Rutnätsvy", "Listvy"),
    "da" to SpotifyLibraryCopy("Sortér efter", "Seneste", "Senest tilføjet", "Alfabetisk", "Opretter", "Playliste", "Gittervisning", "Listevisning"),
    "cs" to SpotifyLibraryCopy("Seřadit podle", "Nedávné", "Nedávno přidané", "Abecedně", "Autor", "Playlist", "Zobrazení mřížky", "Zobrazení seznamu"),
    "sk" to SpotifyLibraryCopy("Zoradiť podľa", "Nedávne", "Nedávno pridané", "Abecedne", "Autor", "Playlist", "Zobrazenie mriežky", "Zobrazenie zoznamu"),
    "hr" to SpotifyLibraryCopy("Poredaj po", "Nedavno", "Nedavno dodano", "Abecedno", "Autor", "Playlista", "Prikaz rešetke", "Prikaz popisa"),
    "sr" to SpotifyLibraryCopy("Сортирај по", "Недавно", "Недавно додато", "Азбучно", "Аутор", "Плејлиста", "Приказ мреже", "Приказ листе"),
    "sq" to SpotifyLibraryCopy("Rendit sipas", "Të fundit", "Shtuar së fundi", "Alfabetikisht", "Krijuesi", "Playlist", "Pamje rrjetë", "Pamje liste"),
    "bg" to SpotifyLibraryCopy("Подреди по", "Скорошни", "Скоро добавени", "Азбучно", "Създател", "Плейлист", "Изглед в мрежа", "Изглед в списък"),
    "hu" to SpotifyLibraryCopy("Rendezés", "Legutóbbiak", "Nemrég hozzáadva", "Betűrend", "Létrehozó", "Lejátszási lista", "Rácsnézet", "Listanézet"),
    "fi" to SpotifyLibraryCopy("Lajittele", "Viimeisimmät", "Äskettäin lisätyt", "Aakkosjärjestys", "Tekijä", "Soittolista", "Ruudukkonäkymä", "Luettelonäkymä"),
    "et" to SpotifyLibraryCopy("Sorteeri", "Hiljutised", "Hiljuti lisatud", "Tähestikuline", "Looja", "Esitusloend", "Ruudustikvaade", "Loendivaade"),
    "nb" to SpotifyLibraryCopy("Sorter etter", "Nylige", "Nylig lagt til", "Alfabetisk", "Skaper", "Spilleliste", "Rutenettvisning", "Listevisning"),
    "ca" to SpotifyLibraryCopy("Ordena per", "Recents", "Afegits recentment", "Alfabètic", "Creador", "Llista", "Vista de quadrícula", "Vista de llista"),
    "uk" to SpotifyLibraryCopy("Сортувати за", "Нещодавні", "Нещодавно додані", "За алфавітом", "Автор", "Плейлист", "Сітка", "Список"),
    "ru" to SpotifyLibraryCopy("Сортировать", "Недавние", "Недавно добавленные", "По алфавиту", "Автор", "Плейлист", "Сетка", "Список"),
    "kk" to SpotifyLibraryCopy("Сұрыптау", "Соңғылар", "Жақында қосылған", "Әліпби бойынша", "Автор", "Плейлист", "Тор көрінісі", "Тізім көрінісі"),
    "tr" to SpotifyLibraryCopy("Sıralama ölçütü", "Son dinlenenler", "Son eklenenler", "Alfabetik", "Oluşturan", "Çalma listesi", "Izgara görünümü", "Liste görünümü"),
    "ar" to SpotifyLibraryCopy("ترتيب حسب", "الأحدث", "المضافة مؤخرًا", "أبجديًا", "المنشئ", "قائمة تشغيل", "عرض الشبكة", "عرض القائمة"),
    "fa" to SpotifyLibraryCopy("مرتب‌سازی بر اساس", "اخیر", "تازه افزوده‌شده", "الفبایی", "سازنده", "فهرست پخش", "نمای شبکه‌ای", "نمای فهرستی"),
    "zh" to SpotifyLibraryCopy("排序方式", "最近播放", "最近添加", "按字母顺序", "创建者", "歌单", "网格视图", "列表视图"),
    "zh-Hant" to SpotifyLibraryCopy("排序方式", "最近播放", "最近新增", "依字母順序", "建立者", "播放清單", "格狀檢視", "清單檢視"),
    "ja" to SpotifyLibraryCopy("並べ替え", "最近", "最近追加した項目", "アルファベット順", "作成者", "プレイリスト", "グリッド表示", "リスト表示"),
    "ko" to SpotifyLibraryCopy("정렬 기준", "최근 재생", "최근 추가", "가나다순", "만든 사람", "플레이리스트", "그리드 보기", "목록 보기"),
    "hi" to SpotifyLibraryCopy("क्रमबद्ध करें", "हाल के", "हाल में जोड़े गए", "वर्णानुक्रम", "निर्माता", "प्लेलिस्ट", "ग्रिड दृश्य", "सूची दृश्य"),
    "bn" to SpotifyLibraryCopy("সাজান", "সাম্প্রতিক", "সম্প্রতি যোগ করা", "বর্ণানুক্রমিক", "নির্মাতা", "প্লেলিস্ট", "গ্রিড ভিউ", "তালিকা ভিউ"),
    "id" to SpotifyLibraryCopy("Urutkan menurut", "Terbaru", "Baru ditambahkan", "Abjad", "Pembuat", "Playlist", "Tampilan petak", "Tampilan daftar"),
    "ms" to SpotifyLibraryCopy("Isih mengikut", "Terkini", "Baru ditambah", "Abjad", "Pencipta", "Senarai main", "Paparan grid", "Paparan senarai"),
    "vi" to SpotifyLibraryCopy("Sắp xếp theo", "Gần đây", "Mới thêm gần đây", "Theo bảng chữ cái", "Người tạo", "Danh sách phát", "Dạng lưới", "Dạng danh sách"),
    "th" to SpotifyLibraryCopy("จัดเรียงตาม", "ล่าสุด", "เพิ่มล่าสุด", "ตามตัวอักษร", "ผู้สร้าง", "เพลย์ลิสต์", "มุมมองตาราง", "มุมมองรายการ"),
    "fil" to SpotifyLibraryCopy("Ayusin ayon sa", "Kamakailan", "Kamakailang idinagdag", "Alpabetiko", "Gumawa", "Playlist", "Grid view", "List view"),
    "he" to SpotifyLibraryCopy("מיון לפי", "אחרונים", "נוספו לאחרונה", "לפי א״ב", "יוצר", "פלייליסט", "תצוגת רשת", "תצוגת רשימה")
)

internal fun spotifyLibraryCopy(code: String): SpotifyLibraryCopy = localizedValueOrEnglish(spotifyLibraryCopies, code)

internal fun spotifyLibraryLocalizationCodes(): Set<String> = spotifyLibraryCopies.keys
