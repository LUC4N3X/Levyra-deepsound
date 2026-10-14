package com.luc4n3x.levyra.ui.i18n

internal data class AdvancedPrefetchCopy(
    val title: String,
    val description: String,
    val off: String,
    private val tracksTemplate: String
) {
    fun tracks(count: Int): String = tracksTemplate.replace("{count}", count.toString())
}

private val advancedPrefetchCopies = mapOf(
    "en" to AdvancedPrefetchCopy(
        "Advanced preloading",
        "Temporarily downloads upcoming tracks to the cache so listening continues on an unstable connection. May use more data and storage.",
        "Off",
        "{count} tracks"
    ),
    "it" to AdvancedPrefetchCopy(
        "Precaricamento avanzato",
        "Scarica temporaneamente i brani successivi nella cache per continuare l'ascolto anche con una connessione instabile. Può utilizzare più dati e spazio di archiviazione.",
        "Disattivato",
        "{count} brani"
    ),
    "es" to AdvancedPrefetchCopy(
        "Precarga avanzada",
        "Descarga temporalmente las siguientes canciones en la caché para seguir escuchando incluso con una conexión inestable. Puede usar más datos y almacenamiento.",
        "Desactivada",
        "{count} canciones"
    ),
    "fr" to AdvancedPrefetchCopy(
        "Préchargement avancé",
        "Télécharge temporairement les titres suivants dans le cache pour continuer l’écoute même avec une connexion instable. Peut utiliser plus de données et d’espace de stockage.",
        "Désactivé",
        "{count} titres"
    ),
    "de" to AdvancedPrefetchCopy(
        "Erweitertes Vorladen",
        "Lädt die nächsten Titel vorübergehend in den Cache, damit die Wiedergabe auch bei instabiler Verbindung weiterläuft. Kann mehr Daten und Speicherplatz verbrauchen.",
        "Aus",
        "{count} Titel"
    ),
    "pt" to AdvancedPrefetchCopy(
        "Pré-carregamento avançado",
        "Baixa temporariamente as próximas faixas para o cache para continuar ouvindo mesmo com uma conexão instável. Pode usar mais dados e armazenamento.",
        "Desativado",
        "{count} faixas"
    ),
    "nl" to AdvancedPrefetchCopy(
        "Geavanceerd vooraf laden",
        "Downloadt de volgende nummers tijdelijk naar de cache, zodat je ook bij een instabiele verbinding kunt blijven luisteren. Kan meer data en opslagruimte gebruiken.",
        "Uit",
        "{count} nummers"
    ),
    "pl" to AdvancedPrefetchCopy(
        "Zaawansowane wczytywanie z wyprzedzeniem",
        "Tymczasowo pobiera kolejne utwory do pamięci podręcznej, aby słuchanie trwało nawet przy niestabilnym połączeniu. Może zużywać więcej danych i miejsca.",
        "Wyłączone",
        "Utwory: {count}"
    ),
    "ro" to AdvancedPrefetchCopy(
        "Preîncărcare avansată",
        "Descarcă temporar piesele următoare în cache pentru a continua ascultarea chiar și cu o conexiune instabilă. Poate folosi mai multe date și spațiu de stocare.",
        "Dezactivată",
        "{count} piese"
    ),
    "el" to AdvancedPrefetchCopy(
        "Προηγμένη προφόρτωση",
        "Κατεβάζει προσωρινά τα επόμενα κομμάτια στην προσωρινή μνήμη, ώστε η ακρόαση να συνεχίζεται ακόμη και με ασταθή σύνδεση. Μπορεί να χρησιμοποιεί περισσότερα δεδομένα και αποθηκευτικό χώρο.",
        "Απενεργοποιημένη",
        "{count} κομμάτια"
    ),
    "sv" to AdvancedPrefetchCopy(
        "Avancerad förinläsning",
        "Laddar tillfälligt ned kommande låtar till cachen så att lyssnandet fortsätter även med instabil anslutning. Kan använda mer data och lagringsutrymme.",
        "Av",
        "{count} låtar"
    ),
    "da" to AdvancedPrefetchCopy(
        "Avanceret forudindlæsning",
        "Downloader midlertidigt de næste numre til cachen, så du kan lytte videre selv med en ustabil forbindelse. Kan bruge mere data og lagerplads.",
        "Fra",
        "{count} numre"
    ),
    "cs" to AdvancedPrefetchCopy(
        "Pokročilé přednačítání",
        "Dočasně stahuje následující skladby do mezipaměti, aby poslech pokračoval i při nestabilním připojení. Může využívat více dat a úložiště.",
        "Vypnuto",
        "Skladby: {count}"
    ),
    "sk" to AdvancedPrefetchCopy(
        "Pokročilé prednačítanie",
        "Dočasne sťahuje nasledujúce skladby do vyrovnávacej pamäte, aby počúvanie pokračovalo aj pri nestabilnom pripojení. Môže využívať viac dát a úložiska.",
        "Vypnuté",
        "Skladby: {count}"
    ),
    "bn" to AdvancedPrefetchCopy(
        "উন্নত প্রিফেচ",
        "অস্থির সংযোগেও শোনা চালিয়ে যেতে পরের গানগুলো সাময়িকভাবে ক্যাশে ডাউনলোড করে। এতে বেশি ডেটা ও স্টোরেজ লাগতে পারে।",
        "বন্ধ",
        "গান: {count}"
    ),
    "kk" to AdvancedPrefetchCopy(
        "Кеңейтілген алдын ала жүктеу",
        "Байланыс тұрақсыз болса да тыңдау жалғасуы үшін келесі әндерді уақытша кэшке жүктейді. Көбірек дерек пен жад жұмсалуы мүмкін.",
        "Өшірулі",
        "Әндер: {count}"
    ),
    "sr" to AdvancedPrefetchCopy(
        "Напредно претходно учитавање",
        "Привремено преузима следеће песме у кеш како би се слушање наставило и уз нестабилну везу. Може трошити више података и простора.",
        "Искључено",
        "Песме: {count}"
    ),
    "sq" to AdvancedPrefetchCopy(
        "Parangarkim i avancuar",
        "Shkarkon përkohësisht këngët e radhës në memorien e përkohshme, që dëgjimi të vazhdojë edhe me lidhje të paqëndrueshme. Mund të përdorë më shumë të dhëna dhe hapësirë.",
        "Joaktiv",
        "Këngë: {count}"
    ),
    "hr" to AdvancedPrefetchCopy(
        "Napredno predučitavanje",
        "Privremeno preuzima sljedeće pjesme u predmemoriju kako bi se slušanje nastavilo i uz nestabilnu vezu. Može trošiti više podataka i prostora za pohranu.",
        "Isključeno",
        "Pjesme: {count}"
    ),
    "bg" to AdvancedPrefetchCopy(
        "Разширено предварително зареждане",
        "Временно изтегля следващите песни в кеша, за да продължи слушането и при нестабилна връзка. Може да използва повече данни и място за съхранение.",
        "Изключено",
        "Песни: {count}"
    ),
    "hu" to AdvancedPrefetchCopy(
        "Speciális előtöltés",
        "Ideiglenesen letölti a következő számokat a gyorsítótárba, hogy instabil kapcsolat mellett is folytatódjon a hallgatás. Több adatot és tárhelyet használhat.",
        "Kikapcsolva",
        "{count} szám"
    ),
    "fi" to AdvancedPrefetchCopy(
        "Edistynyt esilataus",
        "Lataa seuraavat kappaleet tilapäisesti välimuistiin, jotta kuuntelu jatkuu epävakaallakin yhteydellä. Voi käyttää enemmän dataa ja tallennustilaa.",
        "Pois",
        "Kappaleita: {count}"
    ),
    "et" to AdvancedPrefetchCopy(
        "Täpsem eellaadimine",
        "Laadib järgmised lood ajutiselt vahemällu, et kuulamine jätkuks ka ebastabiilse ühenduse korral. Võib kasutada rohkem andmeid ja salvestusruumi.",
        "Väljas",
        "Lugusid: {count}"
    ),
    "nb" to AdvancedPrefetchCopy(
        "Avansert forhåndslasting",
        "Laster midlertidig ned de neste sporene til hurtigbufferen slik at lyttingen fortsetter selv med ustabil tilkobling. Kan bruke mer data og lagringsplass.",
        "Av",
        "{count} spor"
    ),
    "ca" to AdvancedPrefetchCopy(
        "Precàrrega avançada",
        "Baixa temporalment les cançons següents a la memòria cau per continuar escoltant fins i tot amb una connexió inestable. Pot utilitzar més dades i emmagatzematge.",
        "Desactivada",
        "{count} cançons"
    ),
    "uk" to AdvancedPrefetchCopy(
        "Розширене попереднє завантаження",
        "Тимчасово завантажує наступні треки в кеш, щоб прослуховування тривало навіть за нестабільного з’єднання. Може використовувати більше даних і пам’яті.",
        "Вимкнено",
        "Треків: {count}"
    ),
    "ru" to AdvancedPrefetchCopy(
        "Расширенная предзагрузка",
        "Временно загружает следующие треки в кеш, чтобы прослушивание продолжалось даже при нестабильном соединении. Может расходовать больше трафика и места.",
        "Выключено",
        "Треков: {count}"
    ),
    "tr" to AdvancedPrefetchCopy(
        "Gelişmiş ön yükleme",
        "Sonraki parçaları geçici olarak önbelleğe indirir, böylece dengesiz bir bağlantıda bile dinlemeye devam edebilirsiniz. Daha fazla veri ve depolama alanı kullanabilir.",
        "Kapalı",
        "{count} parça"
    ),
    "ar" to AdvancedPrefetchCopy(
        "التحميل المسبق المتقدم",
        "ينزّل المقاطع التالية مؤقتًا إلى ذاكرة التخزين المؤقت لمواصلة الاستماع حتى مع اتصال غير مستقر. قد يستهلك المزيد من البيانات ومساحة التخزين.",
        "متوقف",
        "المقاطع: {count}"
    ),
    "fa" to AdvancedPrefetchCopy(
        "پیش‌بارگذاری پیشرفته",
        "آهنگ‌های بعدی را به‌طور موقت در حافظهٔ پنهان بارگیری می‌کند تا شنیدن حتی با اتصال ناپایدار ادامه یابد. ممکن است داده و فضای ذخیره‌سازی بیشتری مصرف کند.",
        "خاموش",
        "{count} آهنگ"
    ),
    "zh" to AdvancedPrefetchCopy(
        "高级预加载",
        "将接下来的曲目临时下载到缓存中，即使网络不稳定也能继续收听。可能会消耗更多流量和存储空间。",
        "关闭",
        "{count} 首"
    ),
    "zh-Hant" to AdvancedPrefetchCopy(
        "進階預先載入",
        "將接下來的曲目暫時下載到快取中，即使連線不穩定也能繼續收聽。可能會使用更多數據和儲存空間。",
        "關閉",
        "{count} 首"
    ),
    "ja" to AdvancedPrefetchCopy(
        "高度な先読み",
        "次の曲を一時的にキャッシュへダウンロードし、接続が不安定でも再生を続けられるようにします。データ通信量とストレージの使用量が増える場合があります。",
        "オフ",
        "{count} 曲"
    ),
    "ko" to AdvancedPrefetchCopy(
        "고급 미리 불러오기",
        "다음 곡을 캐시에 임시로 다운로드하여 연결이 불안정해도 계속 들을 수 있게 합니다. 데이터와 저장 공간을 더 사용할 수 있습니다.",
        "끄기",
        "{count}곡"
    ),
    "hi" to AdvancedPrefetchCopy(
        "उन्नत प्रीलोडिंग",
        "अगले ट्रैक अस्थायी रूप से कैश में डाउनलोड करता है ताकि अस्थिर कनेक्शन पर भी सुनना जारी रहे। इससे अधिक डेटा और स्टोरेज इस्तेमाल हो सकता है।",
        "बंद",
        "{count} ट्रैक"
    ),
    "id" to AdvancedPrefetchCopy(
        "Pramuat lanjutan",
        "Mengunduh sementara lagu berikutnya ke cache agar Anda tetap bisa mendengarkan meski koneksi tidak stabil. Dapat menggunakan lebih banyak data dan penyimpanan.",
        "Nonaktif",
        "{count} lagu"
    ),
    "ms" to AdvancedPrefetchCopy(
        "Pramuat lanjutan",
        "Memuat turun lagu seterusnya ke cache buat sementara supaya anda boleh terus mendengar walaupun sambungan tidak stabil. Mungkin menggunakan lebih banyak data dan storan.",
        "Mati",
        "{count} lagu"
    ),
    "vi" to AdvancedPrefetchCopy(
        "Tải trước nâng cao",
        "Tạm thời tải các bài tiếp theo vào bộ nhớ đệm để vẫn nghe được khi kết nối không ổn định. Có thể dùng thêm dữ liệu và dung lượng lưu trữ.",
        "Tắt",
        "{count} bài"
    ),
    "th" to AdvancedPrefetchCopy(
        "การโหลดล่วงหน้าขั้นสูง",
        "ดาวน์โหลดเพลงถัดไปไว้ในแคชชั่วคราว เพื่อให้ฟังต่อได้แม้การเชื่อมต่อไม่เสถียร อาจใช้ข้อมูลและพื้นที่จัดเก็บมากขึ้น",
        "ปิด",
        "{count} เพลง"
    ),
    "fil" to AdvancedPrefetchCopy(
        "Advanced na pag-preload",
        "Pansamantalang dina-download sa cache ang mga susunod na track para tuloy ang pakikinig kahit hindi stable ang koneksyon. Maaaring gumamit ng mas maraming data at storage.",
        "Naka-off",
        "{count} track"
    ),
    "he" to AdvancedPrefetchCopy(
        "טעינה מוקדמת מתקדמת",
        "מוריד זמנית את הרצועות הבאות למטמון כדי שההאזנה תימשך גם בחיבור לא יציב. עשוי לצרוך יותר נתונים ושטח אחסון.",
        "כבוי",
        "רצועות: {count}"
    )
)

internal fun advancedPrefetchCopy(code: String): AdvancedPrefetchCopy =
    localizedValueOrEnglish(advancedPrefetchCopies, code)

internal fun advancedPrefetchLocalizationCodes(): Set<String> = advancedPrefetchCopies.keys
