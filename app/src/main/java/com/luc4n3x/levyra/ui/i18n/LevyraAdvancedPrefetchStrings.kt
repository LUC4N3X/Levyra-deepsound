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

internal data class AdvancedPrefetchMobileDataCopy(
    val title: String,
    val description: String
)

private val advancedPrefetchMobileDataCopies = mapOf(
    "en" to AdvancedPrefetchMobileDataCopy(
        "Use mobile data",
        "Also preload upcoming tracks on mobile data. This can use a lot of data."
    ),
    "it" to AdvancedPrefetchMobileDataCopy(
        "Usa dati mobili",
        "Precarica i brani successivi anche con i dati mobili. Può consumare molto traffico."
    ),
    "es" to AdvancedPrefetchMobileDataCopy(
        "Usar datos móviles",
        "Precarga las siguientes canciones también con datos móviles. Puede consumir muchos datos."
    ),
    "fr" to AdvancedPrefetchMobileDataCopy(
        "Utiliser les données mobiles",
        "Précharge aussi les titres suivants avec les données mobiles. Peut consommer beaucoup de données."
    ),
    "de" to AdvancedPrefetchMobileDataCopy(
        "Mobile Daten verwenden",
        "Nächste Titel auch über mobile Daten vorladen. Kann viel Datenvolumen verbrauchen."
    ),
    "pt" to AdvancedPrefetchMobileDataCopy(
        "Usar dados móveis",
        "Pré-carrega as próximas faixas também com dados móveis. Pode consumir muitos dados."
    ),
    "nl" to AdvancedPrefetchMobileDataCopy(
        "Mobiele data gebruiken",
        "Laad volgende nummers ook via mobiele data vooraf. Dit kan veel data verbruiken."
    ),
    "pl" to AdvancedPrefetchMobileDataCopy(
        "Używaj danych mobilnych",
        "Wstępnie ładuj kolejne utwory także przez dane mobilne. Może zużywać dużo transferu."
    ),
    "ro" to AdvancedPrefetchMobileDataCopy(
        "Folosește datele mobile",
        "Preîncarcă melodiile următoare și prin date mobile. Poate consuma mult trafic."
    ),
    "el" to AdvancedPrefetchMobileDataCopy(
        "Χρήση δεδομένων κινητής",
        "Προφόρτωση των επόμενων κομματιών και με δεδομένα κινητής. Μπορεί να καταναλώσει πολλά δεδομένα."
    ),
    "sv" to AdvancedPrefetchMobileDataCopy(
        "Använd mobildata",
        "Förladda kommande låtar även via mobildata. Kan använda mycket data."
    ),
    "da" to AdvancedPrefetchMobileDataCopy(
        "Brug mobildata",
        "Forudindlæs også kommende numre via mobildata. Kan bruge meget data."
    ),
    "cs" to AdvancedPrefetchMobileDataCopy(
        "Používat mobilní data",
        "Přednačítat další skladby i přes mobilní data. Může spotřebovat hodně dat."
    ),
    "sk" to AdvancedPrefetchMobileDataCopy(
        "Používať mobilné dáta",
        "Prednačítavať ďalšie skladby aj cez mobilné dáta. Môže spotrebovať veľa dát."
    ),
    "bn" to AdvancedPrefetchMobileDataCopy(
        "মোবাইল ডেটা ব্যবহার করুন",
        "মোবাইল ডেটাতেও পরের গানগুলো আগে থেকে লোড করুন। এতে অনেক ডেটা খরচ হতে পারে।"
    ),
    "kk" to AdvancedPrefetchMobileDataCopy(
        "Мобильді интернетті пайдалану",
        "Келесі тректерді мобильді интернет арқылы да алдын ала жүктеу. Көп трафик жұмсалуы мүмкін."
    ),
    "sr" to AdvancedPrefetchMobileDataCopy(
        "Користи мобилне податке",
        "Унапред учитава следеће песме и преко мобилних података. Може потрошити доста података."
    ),
    "sq" to AdvancedPrefetchMobileDataCopy(
        "Përdor të dhënat celulare",
        "Parangarko këngët e radhës edhe me të dhëna celulare. Mund të konsumojë shumë të dhëna."
    ),
    "hr" to AdvancedPrefetchMobileDataCopy(
        "Koristi mobilne podatke",
        "Unaprijed učitava sljedeće pjesme i putem mobilnih podataka. Može potrošiti mnogo podataka."
    ),
    "bg" to AdvancedPrefetchMobileDataCopy(
        "Използване на мобилни данни",
        "Предварително зарежда следващите песни и през мобилни данни. Може да изразходва много трафик."
    ),
    "hu" to AdvancedPrefetchMobileDataCopy(
        "Mobiladat használata",
        "A következő számok előtöltése mobiladat-kapcsolaton is. Sok adatforgalmat használhat."
    ),
    "fi" to AdvancedPrefetchMobileDataCopy(
        "Käytä mobiilidataa",
        "Esilataa seuraavat kappaleet myös mobiilidatalla. Voi kuluttaa paljon dataa."
    ),
    "et" to AdvancedPrefetchMobileDataCopy(
        "Kasuta mobiilset andmesidet",
        "Eellaadi järgmised lood ka mobiilse andmeside kaudu. Võib kulutada palju andmemahtu."
    ),
    "nb" to AdvancedPrefetchMobileDataCopy(
        "Bruk mobildata",
        "Forhåndslast også neste spor over mobildata. Kan bruke mye data."
    ),
    "ca" to AdvancedPrefetchMobileDataCopy(
        "Utilitza les dades mòbils",
        "Precarrega també les cançons següents amb dades mòbils. Pot consumir moltes dades."
    ),
    "uk" to AdvancedPrefetchMobileDataCopy(
        "Використовувати мобільний інтернет",
        "Попередньо завантажувати наступні треки і через мобільний інтернет. Може витрачатися багато трафіку."
    ),
    "ru" to AdvancedPrefetchMobileDataCopy(
        "Использовать мобильный интернет",
        "Заранее загружать следующие треки и через мобильный интернет. Может расходоваться много трафика."
    ),
    "tr" to AdvancedPrefetchMobileDataCopy(
        "Mobil veri kullan",
        "Sonraki parçaları mobil veriyle de önceden yükle. Çok fazla veri tüketebilir."
    ),
    "ar" to AdvancedPrefetchMobileDataCopy(
        "استخدام بيانات الجوال",
        "تحميل المقاطع التالية مسبقًا عبر بيانات الجوال أيضًا. قد يستهلك ذلك الكثير من البيانات."
    ),
    "fa" to AdvancedPrefetchMobileDataCopy(
        "استفاده از دادهٔ همراه",
        "آهنگ‌های بعدی با دادهٔ همراه هم از قبل بارگیری شوند. ممکن است حجم زیادی مصرف کند."
    ),
    "zh" to AdvancedPrefetchMobileDataCopy(
        "使用移动数据",
        "使用移动数据时也预加载后续曲目。可能会消耗大量流量。"
    ),
    "zh-Hant" to AdvancedPrefetchMobileDataCopy(
        "使用行動數據",
        "使用行動數據時也預先載入接下來的曲目。可能會消耗大量數據流量。"
    ),
    "ja" to AdvancedPrefetchMobileDataCopy(
        "モバイルデータを使用",
        "モバイルデータ通信でも次の曲を先読みします。大量の通信量を消費する場合があります。"
    ),
    "ko" to AdvancedPrefetchMobileDataCopy(
        "모바일 데이터 사용",
        "모바일 데이터에서도 다음 곡을 미리 불러옵니다. 데이터가 많이 사용될 수 있습니다."
    ),
    "hi" to AdvancedPrefetchMobileDataCopy(
        "मोबाइल डेटा का उपयोग करें",
        "मोबाइल डेटा पर भी अगले गाने पहले से लोड करें। इससे काफ़ी डेटा खर्च हो सकता है।"
    ),
    "id" to AdvancedPrefetchMobileDataCopy(
        "Gunakan data seluler",
        "Pramuat lagu berikutnya juga saat memakai data seluler. Dapat menghabiskan banyak kuota."
    ),
    "ms" to AdvancedPrefetchMobileDataCopy(
        "Guna data mudah alih",
        "Pramuat lagu seterusnya juga melalui data mudah alih. Mungkin menggunakan banyak data."
    ),
    "vi" to AdvancedPrefetchMobileDataCopy(
        "Dùng dữ liệu di động",
        "Tải trước các bài tiếp theo cả khi dùng dữ liệu di động. Có thể tốn nhiều dung lượng."
    ),
    "th" to AdvancedPrefetchMobileDataCopy(
        "ใช้อินเทอร์เน็ตมือถือ",
        "โหลดเพลงถัดไปล่วงหน้าแม้ใช้อินเทอร์เน็ตมือถือ อาจใช้ปริมาณข้อมูลมาก"
    ),
    "fil" to AdvancedPrefetchMobileDataCopy(
        "Gamitin ang mobile data",
        "Mag-preload din ng mga susunod na kanta gamit ang mobile data. Maaaring gumamit ng maraming data."
    ),
    "he" to AdvancedPrefetchMobileDataCopy(
        "שימוש בנתונים סלולריים",
        "טעינה מראש של הרצועות הבאות גם ברשת סלולרית. עשוי לצרוך נתונים רבים."
    )
)

internal fun advancedPrefetchMobileDataCopy(code: String): AdvancedPrefetchMobileDataCopy =
    localizedValueOrEnglish(advancedPrefetchMobileDataCopies, code)

internal fun advancedPrefetchMobileDataLocalizationCodes(): Set<String> = advancedPrefetchMobileDataCopies.keys
