package com.luc4n3x.levyra.ui.i18n

internal data class OfflineQueueCopy(
    val prefetchCustomTitle: String,
    private val prefetchCustomSubtitleTemplate: String,
    val preloading: String,
    val offlineMixes: String,
    val recentlyDownloaded: String,
    val mostPlayed: String
) {
    fun prefetchCustomSubtitle(max: Int): String = prefetchCustomSubtitleTemplate.replace("{max}", max.toString())
}

private val offlineQueueCopies = mapOf(
    "en" to OfflineQueueCopy("Custom number of tracks", "Choose between 1 and {max} upcoming tracks.", "Preloading", "Offline mixes", "Recently downloaded", "Most played"),
    "it" to OfflineQueueCopy("Numero di brani personalizzato", "Scegli da 1 a {max} brani successivi.", "Precaricamento", "Mix offline", "Scaricati di recente", "Più ascoltati"),
    "es" to OfflineQueueCopy("Número de canciones personalizado", "Elige entre 1 y {max} canciones siguientes.", "Precarga", "Mixes sin conexión", "Descargadas recientemente", "Más escuchadas"),
    "fr" to OfflineQueueCopy("Nombre de titres personnalisé", "Choisissez entre 1 et {max} titres suivants.", "Préchargement", "Mix hors ligne", "Téléchargés récemment", "Les plus écoutés"),
    "de" to OfflineQueueCopy("Eigene Titelanzahl", "Wähle zwischen 1 und {max} folgenden Titeln.", "Vorladen", "Offline-Mixe", "Kürzlich heruntergeladen", "Meistgehört"),
    "pt" to OfflineQueueCopy("Número de faixas personalizado", "Escolha entre 1 e {max} próximas faixas.", "Pré-carregamento", "Mixes offline", "Baixadas recentemente", "Mais ouvidas"),
    "nl" to OfflineQueueCopy("Aangepast aantal nummers", "Kies tussen 1 en {max} volgende nummers.", "Vooraf laden", "Offline mixen", "Recent gedownload", "Meest beluisterd"),
    "pl" to OfflineQueueCopy("Własna liczba utworów", "Wybierz od 1 do {max} kolejnych utworów.", "Wczytywanie z wyprzedzeniem", "Miksy offline", "Ostatnio pobrane", "Najczęściej słuchane"),
    "ro" to OfflineQueueCopy("Număr personalizat de piese", "Alege între 1 și {max} piese următoare.", "Preîncărcare", "Mixuri offline", "Descărcate recent", "Cele mai ascultate"),
    "el" to OfflineQueueCopy("Προσαρμοσμένος αριθμός κομματιών", "Επιλέξτε από 1 έως {max} επόμενα κομμάτια.", "Προφόρτωση", "Μίξεις εκτός σύνδεσης", "Πρόσφατες λήψεις", "Τα πιο ακουσμένα"),
    "sv" to OfflineQueueCopy("Eget antal låtar", "Välj mellan 1 och {max} kommande låtar.", "Förladdning", "Offlinemixar", "Nyligen nedladdade", "Mest spelade"),
    "da" to OfflineQueueCopy("Tilpasset antal numre", "Vælg mellem 1 og {max} kommende numre.", "Forudindlæsning", "Offlinemix", "Senest downloadet", "Mest afspillede"),
    "cs" to OfflineQueueCopy("Vlastní počet skladeb", "Vyberte 1 až {max} následujících skladeb.", "Přednačítání", "Offline mixy", "Nedávno stažené", "Nejposlouchanější"),
    "sk" to OfflineQueueCopy("Vlastný počet skladieb", "Vyberte 1 až {max} nasledujúcich skladieb.", "Prednačítanie", "Offline mixy", "Nedávno stiahnuté", "Najpočúvanejšie"),
    "bn" to OfflineQueueCopy("নিজস্ব গানের সংখ্যা", "পরবর্তী 1 থেকে {max}টি গান বেছে নিন।", "প্রিলোড হচ্ছে", "অফলাইন মিক্স", "সম্প্রতি ডাউনলোড করা", "সবচেয়ে বেশি শোনা"),
    "kk" to OfflineQueueCopy("Тректердің өз саны", "Келесі 1–{max} тректі таңдаңыз.", "Алдын ала жүктеу", "Офлайн микстер", "Жақында жүктелгендер", "Ең көп тыңдалғандар"),
    "sr" to OfflineQueueCopy("Прилагођен број песама", "Изаберите од 1 до {max} следећих песама.", "Претходно учитавање", "Офлајн миксеви", "Недавно преузето", "Најслушаније"),
    "sq" to OfflineQueueCopy("Numër i personalizuar këngësh", "Zgjidh nga 1 deri në {max} këngët e ardhshme.", "Parangarkimi", "Mikse jashtë linje", "Shkarkuar së fundi", "Më të dëgjuarat"),
    "hr" to OfflineQueueCopy("Prilagođen broj pjesama", "Odaberite od 1 do {max} sljedećih pjesama.", "Predučitavanje", "Izvanmrežni miksevi", "Nedavno preuzeto", "Najslušanije"),
    "bg" to OfflineQueueCopy("Собствен брой песни", "Изберете от 1 до {max} следващи песни.", "Предварително зареждане", "Офлайн миксове", "Наскоро изтеглени", "Най-слушани"),
    "hu" to OfflineQueueCopy("Egyéni számmennyiség", "Válassz 1 és {max} következő szám között.", "Előtöltés", "Offline mixek", "Nemrég letöltött", "Legtöbbet hallgatott"),
    "fi" to OfflineQueueCopy("Oma kappalemäärä", "Valitse 1–{max} tulevaa kappaletta.", "Esilataus", "Offline-miksaukset", "Äskettäin ladatut", "Kuunnelluimmat"),
    "et" to OfflineQueueCopy("Kohandatud loo arv", "Vali 1 kuni {max} järgmist lugu.", "Eellaadimine", "Võrguühenduseta miksid", "Hiljuti alla laaditud", "Enim kuulatud"),
    "nb" to OfflineQueueCopy("Eget antall spor", "Velg mellom 1 og {max} kommende spor.", "Forhåndslasting", "Frakoblede mikser", "Nylig lastet ned", "Mest spilt"),
    "ca" to OfflineQueueCopy("Nombre de cançons personalitzat", "Tria entre 1 i {max} cançons següents.", "Precàrrega", "Mescles sense connexió", "Baixades recentment", "Les més escoltades"),
    "uk" to OfflineQueueCopy("Своя кількість треків", "Виберіть від 1 до {max} наступних треків.", "Попереднє завантаження", "Офлайн-мікси", "Нещодавно завантажені", "Найчастіше слухані"),
    "ru" to OfflineQueueCopy("Своё количество треков", "Выберите от 1 до {max} следующих треков.", "Предзагрузка", "Офлайн-миксы", "Недавно загруженные", "Самые прослушиваемые"),
    "tr" to OfflineQueueCopy("Özel parça sayısı", "Sıradaki 1 ile {max} parça arasında seç.", "Ön yükleme", "Çevrimdışı miksler", "Son indirilenler", "En çok dinlenenler"),
    "ar" to OfflineQueueCopy("عدد مخصص من المقاطع", "اختر من 1 إلى {max} من المقاطع التالية.", "التحميل المسبق", "مزيج بلا اتصال", "تم تنزيلها مؤخرًا", "الأكثر استماعًا"),
    "fa" to OfflineQueueCopy("تعداد دلخواه آهنگ‌ها", "بین 1 تا {max} آهنگ بعدی را انتخاب کنید.", "پیش‌بارگذاری", "میکس‌های آفلاین", "دانلودشده‌های اخیر", "پرشنیده‌ترین‌ها"),
    "zh" to OfflineQueueCopy("自定义曲目数量", "选择 1 到 {max} 首接下来的曲目。", "预加载", "离线混音", "最近下载", "最常播放"),
    "zh-Hant" to OfflineQueueCopy("自訂曲目數量", "選擇 1 到 {max} 首接下來的曲目。", "預先載入", "離線混音", "最近下載", "最常播放"),
    "ja" to OfflineQueueCopy("曲数をカスタム", "次の曲を 1〜{max} 曲の範囲で選択します。", "先読み", "オフラインミックス", "最近ダウンロードした曲", "よく聴く曲"),
    "ko" to OfflineQueueCopy("곡 수 직접 설정", "다음 곡을 1~{max}곡 사이에서 선택하세요.", "미리 불러오기", "오프라인 믹스", "최근 다운로드", "가장 많이 들은 곡"),
    "hi" to OfflineQueueCopy("गानों की अपनी संख्या", "अगले 1 से {max} गाने चुनें।", "प्रीलोडिंग", "ऑफ़लाइन मिक्स", "हाल में डाउनलोड किए गए", "सबसे ज़्यादा सुने गए"),
    "id" to OfflineQueueCopy("Jumlah lagu khusus", "Pilih antara 1 dan {max} lagu berikutnya.", "Pramuat", "Mix offline", "Baru diunduh", "Paling sering diputar"),
    "ms" to OfflineQueueCopy("Bilangan lagu tersuai", "Pilih antara 1 dan {max} lagu seterusnya.", "Pramuat", "Campuran luar talian", "Dimuat turun baru-baru ini", "Paling kerap dimainkan"),
    "vi" to OfflineQueueCopy("Số bài tùy chỉnh", "Chọn từ 1 đến {max} bài tiếp theo.", "Tải trước", "Bản phối ngoại tuyến", "Tải xuống gần đây", "Nghe nhiều nhất"),
    "th" to OfflineQueueCopy("กำหนดจำนวนเพลงเอง", "เลือกเพลงถัดไประหว่าง 1 ถึง {max} เพลง", "กำลังโหลดล่วงหน้า", "มิกซ์ออฟไลน์", "ดาวน์โหลดล่าสุด", "ฟังบ่อยที่สุด"),
    "fil" to OfflineQueueCopy("Custom na bilang ng track", "Pumili mula 1 hanggang {max} susunod na track.", "Nagpi-preload", "Mga offline mix", "Kamakailang na-download", "Pinakamadalas pakinggan"),
    "he" to OfflineQueueCopy("מספר רצועות מותאם", "בחרו בין 1 ל-{max} הרצועות הבאות.", "טעינה מראש", "מיקסים לא מקוונים", "הורדו לאחרונה", "הכי מושמעים"),
    "ur" to OfflineQueueCopy("پٹریوں کی اپنی مرضی کی تعداد", "1 اور {max} آنے والے ٹریکس کے درمیان انتخاب کریں۔", "پری لوڈنگ", "آف لائن مکسز", "حال ہی میں ڈاؤن لوڈ کیا گیا۔", "سب سے زیادہ کھیلا گیا۔"),
    "ta" to OfflineQueueCopy("தனிப்பயன் தடங்களின் எண்ணிக்கை", "1 மற்றும் {max} வரவிருக்கும் டிராக்குகளுக்கு இடையே தேர்வு செய்யவும்.", "முன் ஏற்றுகிறது", "ஆஃப்லைன் கலவைகள்", "சமீபத்தில் பதிவிறக்கம் செய்யப்பட்டது", "அதிகம் விளையாடியது"),
    "te" to OfflineQueueCopy("ట్రాక్‌ల అనుకూల సంఖ్య", "1 మరియు {max} రాబోయే ట్రాక్‌ల మధ్య ఎంచుకోండి.", "ప్రీలోడింగ్", "ఆఫ్‌లైన్ మిక్స్‌లు", "ఇటీవల డౌన్‌లోడ్ చేయబడింది", "ఎక్కువగా ఆడారు"),
    "mr" to OfflineQueueCopy("ट्रॅकची सानुकूल संख्या", "1 आणि {max} आगामी ट्रॅक दरम्यान निवडा.", "प्रीलोडिंग", "ऑफलाइन मिक्स", "अलीकडे डाउनलोड केले", "सर्वाधिक खेळले"),
    "gu" to OfflineQueueCopy("ટ્રેકની કસ્ટમ સંખ્યા", "1 અને {max} આગામી ટ્રેક વચ્ચે પસંદ કરો.", "પ્રીલોડિંગ", "ઑફલાઇન મિક્સ", "તાજેતરમાં ડાઉનલોડ કરેલ", "સૌથી વધુ રમ્યા"),
    "pt-BR" to OfflineQueueCopy("Número personalizado de faixas", "Escolha entre as próximas faixas 1 e {max}.", "Pré-carregamento", "Mixagens off-line", "Baixado recentemente", "Mais jogado")
)

internal fun offlineQueueCopy(code: String): OfflineQueueCopy = localizedValueOrEnglish(offlineQueueCopies, code)

internal fun offlineQueueLocalizationCodes(): Set<String> = offlineQueueCopies.keys
