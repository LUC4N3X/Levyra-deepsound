package com.luc4n3x.levyra.ui.i18n

enum class SettingsHubText { Studios, StudiosHint, Appearance, Listening, Library, Accounts, System }

class SettingsHubCopy internal constructor(private val texts: Map<SettingsHubText, String>) {
    internal val brazilian: SettingsHubCopy by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SettingsHubCopy(texts.mapValues { (_, text) -> LevyraBrazilianPortuguese.adapt(text) })
    }

    operator fun get(key: SettingsHubText): String = texts[key] ?: englishSettingsHub.getValue(key)

    internal val complete: Boolean
        get() = texts.size == SettingsHubText.entries.size
}

private fun settingsHub(vararg value: String): Map<SettingsHubText, String> =
    if (value.size == SettingsHubText.entries.size) SettingsHubText.entries.zip(value).toMap() else emptyMap()

private val settingsHubTexts: Map<String, Map<SettingsHubText, String>> = mapOf(
    "en" to settingsHub("Studios", "Shape how Levyra looks", "Look and feel", "Listening", "Library and downloads", "Accounts and services", "System"),
    "it" to settingsHub("Studio", "Dai forma all'aspetto di Levyra", "Aspetto", "Ascolto", "Libreria e download", "Account e servizi", "Sistema"),
    "es" to settingsHub("Estudios", "Da forma al aspecto de Levyra", "Apariencia", "Escucha", "Biblioteca y descargas", "Cuentas y servicios", "Sistema"),
    "fr" to settingsHub("Studios", "Façonnez l'apparence de Levyra", "Apparence", "Écoute", "Bibliothèque et téléchargements", "Comptes et services", "Système"),
    "de" to settingsHub("Studios", "Gestalte das Aussehen von Levyra", "Aussehen", "Hören", "Mediathek und Downloads", "Konten und Dienste", "System"),
    "pt" to settingsHub("Estúdios", "Molde a aparência do Levyra", "Aparência", "Audição", "Biblioteca e downloads", "Contas e serviços", "Sistema"),
    "nl" to settingsHub("Studio's", "Bepaal hoe Levyra eruitziet", "Uiterlijk", "Luisteren", "Bibliotheek en downloads", "Accounts en diensten", "Systeem"),
    "pl" to settingsHub("Studia", "Nadaj Levyra własny wygląd", "Wygląd", "Słuchanie", "Biblioteka i pobrane", "Konta i usługi", "System"),
    "ro" to settingsHub("Studiouri", "Modelează aspectul Levyra", "Aspect", "Ascultare", "Bibliotecă și descărcări", "Conturi și servicii", "Sistem"),
    "el" to settingsHub("Studio", "Διαμορφώστε την εμφάνιση του Levyra", "Εμφάνιση", "Ακρόαση", "Βιβλιοθήκη και λήψεις", "Λογαριασμοί και υπηρεσίες", "Σύστημα"),
    "sv" to settingsHub("Studior", "Forma hur Levyra ser ut", "Utseende", "Lyssning", "Bibliotek och nedladdningar", "Konton och tjänster", "System"),
    "da" to settingsHub("Studier", "Form Levyras udseende", "Udseende", "Lytning", "Bibliotek og downloads", "Konti og tjenester", "System"),
    "cs" to settingsHub("Studia", "Upravte vzhled Levyra", "Vzhled", "Poslech", "Knihovna a stahování", "Účty a služby", "Systém"),
    "sk" to settingsHub("Štúdiá", "Upravte vzhľad Levyra", "Vzhľad", "Počúvanie", "Knižnica a sťahovanie", "Účty a služby", "Systém"),
    "bn" to settingsHub("স্টুডিও", "Levyra-র চেহারা সাজান", "চেহারা", "শোনা", "লাইব্রেরি ও ডাউনলোড", "অ্যাকাউন্ট ও পরিষেবা", "সিস্টেম"),
    "kk" to settingsHub("Студиялар", "Levyra көрінісін өзгертіңіз", "Көрініс", "Тыңдау", "Кітапхана және жүктеулер", "Аккаунттар мен қызметтер", "Жүйе"),
    "sr" to settingsHub("Студији", "Обликујте изглед апликације Levyra", "Изглед", "Слушање", "Библиотека и преузимања", "Налози и услуге", "Систем"),
    "sq" to settingsHub("Studiot", "Jepi formë pamjes së Levyra", "Pamja", "Dëgjimi", "Biblioteka dhe shkarkimet", "Llogaritë dhe shërbimet", "Sistemi"),
    "hr" to settingsHub("Studiji", "Oblikujte izgled aplikacije Levyra", "Izgled", "Slušanje", "Knjižnica i preuzimanja", "Računi i usluge", "Sustav"),
    "bg" to settingsHub("Студия", "Оформете вида на Levyra", "Външен вид", "Слушане", "Библиотека и изтегляния", "Акаунти и услуги", "Система"),
    "hu" to settingsHub("Stúdiók", "Formáld a Levyra megjelenését", "Megjelenés", "Hallgatás", "Könyvtár és letöltések", "Fiókok és szolgáltatások", "Rendszer"),
    "fi" to settingsHub("Studiot", "Muokkaa Levyran ulkoasua", "Ulkoasu", "Kuuntelu", "Kirjasto ja lataukset", "Tilit ja palvelut", "Järjestelmä"),
    "et" to settingsHub("Stuudiod", "Kujunda Levyra välimus", "Välimus", "Kuulamine", "Kogu ja allalaadimised", "Kontod ja teenused", "Süsteem"),
    "nb" to settingsHub("Studioer", "Form hvordan Levyra ser ut", "Utseende", "Lytting", "Bibliotek og nedlastinger", "Kontoer og tjenester", "System"),
    "ca" to settingsHub("Estudis", "Dona forma a l'aspecte de Levyra", "Aparença", "Escolta", "Biblioteca i baixades", "Comptes i serveis", "Sistema"),
    "uk" to settingsHub("Студії", "Налаштуйте вигляд Levyra", "Вигляд", "Прослуховування", "Бібліотека й завантаження", "Облікові записи й сервіси", "Система"),
    "ru" to settingsHub("Студии", "Настройте внешний вид Levyra", "Внешний вид", "Прослушивание", "Библиотека и загрузки", "Аккаунты и сервисы", "Система"),
    "tr" to settingsHub("Stüdyolar", "Levyra'nın görünümünü şekillendirin", "Görünüm", "Dinleme", "Kitaplık ve indirmeler", "Hesaplar ve hizmetler", "Sistem"),
    "ar" to settingsHub("الاستوديوهات", "شكّل مظهر Levyra", "المظهر", "الاستماع", "المكتبة والتنزيلات", "الحسابات والخدمات", "النظام"),
    "fa" to settingsHub("استودیوها", "ظاهر Levyra را شکل دهید", "ظاهر", "شنیدن", "کتابخانه و دانلودها", "حساب‌ها و سرویس‌ها", "سیستم"),
    "zh" to settingsHub("工作室", "塑造 Levyra 的外观", "外观", "收听", "音乐库与下载", "账号与服务", "系统"),
    "zh-Hant" to settingsHub("工作室", "打造 Levyra 的外觀", "外觀", "聆聽", "音樂庫與下載", "帳戶與服務", "系統"),
    "ja" to settingsHub("スタジオ", "Levyraの見た目を整える", "外観", "リスニング", "ライブラリとダウンロード", "アカウントとサービス", "システム"),
    "ko" to settingsHub("스튜디오", "Levyra의 모습을 꾸며 보세요", "모양", "감상", "보관함 및 다운로드", "계정 및 서비스", "시스템"),
    "hi" to settingsHub("स्टूडियो", "Levyra का लुक बनाएँ", "रूप", "सुनना", "लाइब्रेरी और डाउनलोड", "खाते और सेवाएँ", "सिस्टम"),
    "id" to settingsHub("Studio", "Atur tampilan Levyra", "Tampilan", "Mendengarkan", "Koleksi dan unduhan", "Akun dan layanan", "Sistem"),
    "ms" to settingsHub("Studio", "Bentuk rupa Levyra", "Rupa", "Mendengar", "Pustaka dan muat turun", "Akaun dan perkhidmatan", "Sistem"),
    "vi" to settingsHub("Studio", "Tùy biến giao diện Levyra", "Giao diện", "Nghe nhạc", "Thư viện và tải xuống", "Tài khoản và dịch vụ", "Hệ thống"),
    "th" to settingsHub("สตูดิโอ", "ปรับแต่งหน้าตาของ Levyra", "รูปลักษณ์", "การฟัง", "คลังเพลงและดาวน์โหลด", "บัญชีและบริการ", "ระบบ"),
    "fil" to settingsHub("Mga Studio", "Hubugin ang itsura ng Levyra", "Itsura", "Pakikinig", "Library at mga download", "Mga account at serbisyo", "System"),
    "he" to settingsHub("סטודיו", "עצבו את המראה של Levyra", "מראה", "האזנה", "ספרייה והורדות", "חשבונות ושירותים", "מערכת")
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
