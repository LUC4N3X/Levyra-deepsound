package com.luc4n3x.levyra.ui.i18n

enum class SettingsHubText {
    Studios, StudiosHint, Appearance, Listening, Library, Accounts, System,
    Content, Advanced, Style, Gestures, Video, QualityAndLocation, WhenToDownload, ManualBackup, ProfileAndTaste
}

class SettingsHubCopy internal constructor(private val texts: Map<SettingsHubText, String>) {
    internal val brazilian: SettingsHubCopy by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SettingsHubCopy(brazilianSettingsHub)
    }

    operator fun get(key: SettingsHubText): String = texts[key] ?: englishSettingsHub.getValue(key)

    internal val complete: Boolean
        get() = texts.size == SettingsHubText.entries.size
}

private fun settingsHub(vararg value: String): Map<SettingsHubText, String> =
    if (value.size == SettingsHubText.entries.size) SettingsHubText.entries.zip(value).toMap() else emptyMap()

private val settingsHubTexts: Map<String, Map<SettingsHubText, String>> = mapOf(
    "en" to settingsHub("Studios", "Shape how Levyra looks", "Look and feel", "Listening", "Library and content", "Accounts and services", "System", "Content", "Advanced", "Style", "Gestures", "Video", "Quality and location", "When to download", "Manual backup", "Profile and taste"),
    "it" to settingsHub("Studio", "Dai forma all'aspetto di Levyra", "Aspetto", "Ascolto", "Libreria e contenuti", "Account e servizi", "Sistema", "Contenuti", "Avanzate", "Stile", "Gesti", "Video", "Qualità e posizione", "Quando scaricare", "Backup manuale", "Profilo e gusti"),
    "es" to settingsHub("Estudios", "Da forma al aspecto de Levyra", "Apariencia", "Escucha", "Biblioteca y contenido", "Cuentas y servicios", "Sistema", "Contenido", "Avanzado", "Estilo", "Gestos", "Vídeo", "Calidad y ubicación", "Cuándo descargar", "Copia manual", "Perfil y gustos"),
    "fr" to settingsHub("Studios", "Façonnez l'apparence de Levyra", "Apparence", "Écoute", "Bibliothèque et contenu", "Comptes et services", "Système", "Contenu", "Avancé", "Style", "Gestes", "Vidéo", "Qualité et emplacement", "Quand télécharger", "Sauvegarde manuelle", "Profil et goûts"),
    "de" to settingsHub("Studios", "Gestalte das Aussehen von Levyra", "Aussehen", "Hören", "Mediathek und Inhalte", "Konten und Dienste", "System", "Inhalte", "Erweitert", "Stil", "Gesten", "Video", "Qualität und Speicherort", "Wann herunterladen", "Manuelles Backup", "Profil und Geschmack"),
    "pt" to settingsHub("Estúdios", "Molde a aparência do Levyra", "Aparência", "Audição", "Biblioteca e conteúdo", "Contas e serviços", "Sistema", "Conteúdo", "Avançado", "Estilo", "Gestos", "Vídeo", "Qualidade e local", "Quando fazer download", "Backup manual", "Perfil e gostos"),
    "nl" to settingsHub("Studio's", "Bepaal hoe Levyra eruitziet", "Uiterlijk", "Luisteren", "Bibliotheek en inhoud", "Accounts en diensten", "Systeem", "Inhoud", "Geavanceerd", "Stijl", "Gebaren", "Video", "Kwaliteit en locatie", "Wanneer downloaden", "Handmatige back-up", "Profiel en smaak"),
    "pl" to settingsHub("Studia", "Nadaj Levyra własny wygląd", "Wygląd", "Słuchanie", "Biblioteka i treści", "Konta i usługi", "System", "Treści", "Zaawansowane", "Styl", "Gesty", "Wideo", "Jakość i lokalizacja", "Kiedy pobierać", "Kopia ręczna", "Profil i gust"),
    "ro" to settingsHub("Studiouri", "Modelează aspectul Levyra", "Aspect", "Ascultare", "Bibliotecă și conținut", "Conturi și servicii", "Sistem", "Conținut", "Avansat", "Stil", "Gesturi", "Video", "Calitate și locație", "Când se descarcă", "Backup manual", "Profil și gusturi"),
    "el" to settingsHub("Studio", "Διαμορφώστε την εμφάνιση του Levyra", "Εμφάνιση", "Ακρόαση", "Βιβλιοθήκη και περιεχόμενο", "Λογαριασμοί και υπηρεσίες", "Σύστημα", "Περιεχόμενο", "Για προχωρημένους", "Στυλ", "Χειρονομίες", "Βίντεο", "Ποιότητα και τοποθεσία", "Πότε γίνεται λήψη", "Χειροκίνητο αντίγραφο", "Προφίλ και προτιμήσεις"),
    "sv" to settingsHub("Studior", "Forma hur Levyra ser ut", "Utseende", "Lyssning", "Bibliotek och innehåll", "Konton och tjänster", "System", "Innehåll", "Avancerat", "Stil", "Gester", "Video", "Kvalitet och plats", "När du laddar ned", "Manuell säkerhetskopia", "Profil och smak"),
    "da" to settingsHub("Studier", "Form Levyras udseende", "Udseende", "Lytning", "Bibliotek og indhold", "Konti og tjenester", "System", "Indhold", "Avanceret", "Stil", "Bevægelser", "Video", "Kvalitet og placering", "Hvornår der downloades", "Manuel backup", "Profil og smag"),
    "cs" to settingsHub("Studia", "Upravte vzhled Levyra", "Vzhled", "Poslech", "Knihovna a obsah", "Účty a služby", "Systém", "Obsah", "Pokročilé", "Styl", "Gesta", "Video", "Kvalita a umístění", "Kdy stahovat", "Ruční záloha", "Profil a vkus"),
    "sk" to settingsHub("Štúdiá", "Upravte vzhľad Levyra", "Vzhľad", "Počúvanie", "Knižnica a obsah", "Účty a služby", "Systém", "Obsah", "Rozšírené", "Štýl", "Gestá", "Video", "Kvalita a umiestnenie", "Kedy sťahovať", "Ručná záloha", "Profil a vkus"),
    "bn" to settingsHub("স্টুডিও", "Levyra-র চেহারা সাজান", "চেহারা", "শোনা", "লাইব্রেরি ও কনটেন্ট", "অ্যাকাউন্ট ও পরিষেবা", "সিস্টেম", "কনটেন্ট", "উন্নত", "স্টাইল", "জেসচার", "ভিডিও", "মান ও অবস্থান", "কখন ডাউনলোড হবে", "ম্যানুয়াল ব্যাকআপ", "প্রোফাইল ও রুচি"),
    "kk" to settingsHub("Студиялар", "Levyra көрінісін өзгертіңіз", "Көрініс", "Тыңдау", "Кітапхана және мазмұн", "Аккаунттар мен қызметтер", "Жүйе", "Мазмұн", "Қосымша", "Стиль", "Қимылдар", "Бейне", "Сапа және орын", "Қашан жүктеу", "Қолмен сақтық көшірме", "Профиль және талғам"),
    "sr" to settingsHub("Студији", "Обликујте изглед апликације Levyra", "Изглед", "Слушање", "Библиотека и садржај", "Налози и услуге", "Систем", "Садржај", "Напредно", "Стил", "Покрети", "Видео", "Квалитет и локација", "Када преузимати", "Ручна резервна копија", "Профил и укус"),
    "sq" to settingsHub("Studiot", "Jepi formë pamjes së Levyra", "Pamja", "Dëgjimi", "Biblioteka dhe përmbajtja", "Llogaritë dhe shërbimet", "Sistemi", "Përmbajtja", "Të avancuara", "Stili", "Gjestet", "Video", "Cilësia dhe vendndodhja", "Kur të shkarkohet", "Kopje rezervë manuale", "Profili dhe shijet"),
    "hr" to settingsHub("Studiji", "Oblikujte izgled aplikacije Levyra", "Izgled", "Slušanje", "Knjižnica i sadržaj", "Računi i usluge", "Sustav", "Sadržaj", "Napredno", "Stil", "Pokreti", "Videozapis", "Kvaliteta i lokacija", "Kada preuzimati", "Ručna sigurnosna kopija", "Profil i ukus"),
    "bg" to settingsHub("Студия", "Оформете вида на Levyra", "Външен вид", "Слушане", "Библиотека и съдържание", "Акаунти и услуги", "Система", "Съдържание", "Разширени", "Стил", "Жестове", "Видео", "Качество и местоположение", "Кога да се изтегля", "Ръчно резервно копие", "Профил и вкус"),
    "hu" to settingsHub("Stúdiók", "Formáld a Levyra megjelenését", "Megjelenés", "Hallgatás", "Könyvtár és tartalom", "Fiókok és szolgáltatások", "Rendszer", "Tartalom", "Speciális", "Stílus", "Kézmozdulatok", "Videó", "Minőség és hely", "Mikor töltsön le", "Kézi biztonsági mentés", "Profil és ízlés"),
    "fi" to settingsHub("Studiot", "Muokkaa Levyran ulkoasua", "Ulkoasu", "Kuuntelu", "Kirjasto ja sisältö", "Tilit ja palvelut", "Järjestelmä", "Sisältö", "Lisäasetukset", "Tyyli", "Eleet", "Video", "Laatu ja sijainti", "Milloin ladataan", "Manuaalinen varmuuskopio", "Profiili ja maku"),
    "et" to settingsHub("Stuudiod", "Kujunda Levyra välimus", "Välimus", "Kuulamine", "Kogu ja sisu", "Kontod ja teenused", "Süsteem", "Sisu", "Täpsem", "Stiil", "Žestid", "Video", "Kvaliteet ja asukoht", "Millal alla laadida", "Käsitsi varundus", "Profiil ja maitse"),
    "nb" to settingsHub("Studioer", "Form hvordan Levyra ser ut", "Utseende", "Lytting", "Bibliotek og innhold", "Kontoer og tjenester", "System", "Innhold", "Avansert", "Stil", "Bevegelser", "Video", "Kvalitet og plassering", "Når det lastes ned", "Manuell sikkerhetskopi", "Profil og smak"),
    "ca" to settingsHub("Estudis", "Dona forma a l'aspecte de Levyra", "Aparença", "Escolta", "Biblioteca i contingut", "Comptes i serveis", "Sistema", "Contingut", "Avançat", "Estil", "Gestos", "Vídeo", "Qualitat i ubicació", "Quan baixar", "Còpia manual", "Perfil i gustos"),
    "uk" to settingsHub("Студії", "Налаштуйте вигляд Levyra", "Вигляд", "Прослуховування", "Бібліотека й вміст", "Облікові записи й сервіси", "Система", "Вміст", "Додатково", "Стиль", "Жести", "Відео", "Якість і розташування", "Коли завантажувати", "Ручна резервна копія", "Профіль і смаки"),
    "ru" to settingsHub("Студии", "Настройте внешний вид Levyra", "Внешний вид", "Прослушивание", "Библиотека и контент", "Аккаунты и сервисы", "Система", "Контент", "Дополнительно", "Стиль", "Жесты", "Видео", "Качество и расположение", "Когда загружать", "Ручная резервная копия", "Профиль и вкусы"),
    "tr" to settingsHub("Stüdyolar", "Levyra'nın görünümünü şekillendirin", "Görünüm", "Dinleme", "Kitaplık ve içerik", "Hesaplar ve hizmetler", "Sistem", "İçerik", "Gelişmiş", "Stil", "Hareketler", "Video", "Kalite ve konum", "Ne zaman indirilsin", "Elle yedekleme", "Profil ve zevkler"),
    "ar" to settingsHub("الاستوديوهات", "شكّل مظهر Levyra", "المظهر", "الاستماع", "المكتبة والمحتوى", "الحسابات والخدمات", "النظام", "المحتوى", "متقدم", "النمط", "الإيماءات", "الفيديو", "الجودة والموقع", "متى يتم التنزيل", "نسخ احتياطي يدوي", "الملف الشخصي والذوق"),
    "fa" to settingsHub("استودیوها", "ظاهر Levyra را شکل دهید", "ظاهر", "شنیدن", "کتابخانه و محتوا", "حساب‌ها و سرویس‌ها", "سیستم", "محتوا", "پیشرفته", "سبک", "حرکات", "ویدیو", "کیفیت و مکان", "زمان دانلود", "پشتیبان‌گیری دستی", "نمایه و سلیقه"),
    "zh" to settingsHub("工作室", "塑造 Levyra 的外观", "外观", "收听", "音乐库与内容", "账号与服务", "系统", "内容", "高级", "风格", "手势", "视频", "质量与位置", "何时下载", "手动备份", "个人资料与喜好"),
    "zh-Hant" to settingsHub("工作室", "打造 Levyra 的外觀", "外觀", "聆聽", "音樂庫與內容", "帳戶與服務", "系統", "內容", "進階", "風格", "手勢", "影片", "品質與位置", "何時下載", "手動備份", "個人檔案與喜好"),
    "ja" to settingsHub("スタジオ", "Levyraの見た目を整える", "外観", "リスニング", "ライブラリとコンテンツ", "アカウントとサービス", "システム", "コンテンツ", "詳細設定", "スタイル", "ジェスチャー", "動画", "品質と保存先", "ダウンロードのタイミング", "手動バックアップ", "プロフィールと好み"),
    "ko" to settingsHub("스튜디오", "Levyra의 모습을 꾸며 보세요", "모양", "감상", "보관함 및 콘텐츠", "계정 및 서비스", "시스템", "콘텐츠", "고급", "스타일", "제스처", "동영상", "품질 및 위치", "다운로드 시점", "수동 백업", "프로필 및 취향"),
    "hi" to settingsHub("स्टूडियो", "Levyra का लुक बनाएँ", "रूप", "सुनना", "लाइब्रेरी और कंटेंट", "खाते और सेवाएँ", "सिस्टम", "कंटेंट", "उन्नत", "स्टाइल", "जेस्चर", "वीडियो", "क्वालिटी और लोकेशन", "कब डाउनलोड करें", "मैन्युअल बैकअप", "प्रोफ़ाइल और पसंद"),
    "id" to settingsHub("Studio", "Atur tampilan Levyra", "Tampilan", "Mendengarkan", "Koleksi dan konten", "Akun dan layanan", "Sistem", "Konten", "Lanjutan", "Gaya", "Gestur", "Video", "Kualitas dan lokasi", "Kapan mengunduh", "Cadangan manual", "Profil dan selera"),
    "ms" to settingsHub("Studio", "Bentuk rupa Levyra", "Rupa", "Mendengar", "Pustaka dan kandungan", "Akaun dan perkhidmatan", "Sistem", "Kandungan", "Lanjutan", "Gaya", "Gerak isyarat", "Video", "Kualiti dan lokasi", "Bila hendak muat turun", "Sandaran manual", "Profil dan citarasa"),
    "vi" to settingsHub("Studio", "Tùy biến giao diện Levyra", "Giao diện", "Nghe nhạc", "Thư viện và nội dung", "Tài khoản và dịch vụ", "Hệ thống", "Nội dung", "Nâng cao", "Phong cách", "Cử chỉ", "Video", "Chất lượng và vị trí", "Khi nào tải xuống", "Sao lưu thủ công", "Hồ sơ và sở thích"),
    "th" to settingsHub("สตูดิโอ", "ปรับแต่งหน้าตาของ Levyra", "รูปลักษณ์", "การฟัง", "คลังเพลงและเนื้อหา", "บัญชีและบริการ", "ระบบ", "เนื้อหา", "ขั้นสูง", "สไตล์", "ท่าทาง", "วิดีโอ", "คุณภาพและตำแหน่ง", "ดาวน์โหลดเมื่อใด", "สำรองข้อมูลด้วยตนเอง", "โปรไฟล์และรสนิยม"),
    "fil" to settingsHub("Mga Studio", "Hubugin ang itsura ng Levyra", "Itsura", "Pakikinig", "Library at content", "Mga account at serbisyo", "System", "Content", "Advanced", "Estilo", "Mga gesture", "Video", "Kalidad at lokasyon", "Kailan magda-download", "Manual na backup", "Profile at panlasa"),
    "he" to settingsHub("סטודיו", "עצבו את המראה של Levyra", "מראה", "האזנה", "ספרייה ותוכן", "חשבונות ושירותים", "מערכת", "תוכן", "מתקדם", "סגנון", "מחוות", "וידאו", "איכות ומיקום", "מתי להוריד", "גיבוי ידני", "פרופיל וטעם")
)

private val brazilianSettingsHub: Map<SettingsHubText, String> = settingsHub(
    "Estúdios", "Personalize o visual do Levyra", "Aparência", "Audição", "Biblioteca e conteúdo", "Contas e serviços", "Sistema", "Conteúdo", "Avançado", "Estilo", "Gestos", "Vídeo", "Qualidade e local", "Quando baixar", "Backup manual", "Perfil e gostos"
)

private val englishSettingsHub: Map<SettingsHubText, String> = settingsHubTexts.getValue("en")

private val settingsHubCopies: Map<String, SettingsHubCopy> by lazy {
    settingsHubTexts.mapValues { (_, texts) -> SettingsHubCopy(texts) }
}

internal fun settingsHubLocalizationCodes(): Set<String> = settingsHubCopies.keys

internal fun settingsHubIncompleteLocalizationCodes(): Set<String> =
    settingsHubCopies.filterValues { !it.complete }.keys

internal val LevyraStrings.settingsHubCopy: SettingsHubCopy
    get() = localizedValueOrEnglish(settingsHubCopies, code).forBrazil(code) { brazilian }
