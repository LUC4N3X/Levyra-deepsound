package com.luc4n3x.levyra.ui.i18n

internal data class RegionalCopy(
    val appLanguageTitle: String,
    val appLanguageBody: String,
    val contentCountryTitle: String,
    val contentCountryBody: String,
    val audioLanguageTitle: String,
    val audioLanguageBody: String,
    val automatic: String,
    val automaticBody: String,
    val fallbackNote: String
)

internal object LevyraRegionalStrings {
    private val copies = mapOf(
        "en" to RegionalCopy(
            "App language", "Menus and text in the app",
            "Content country", "Charts, new releases and suggestions",
            "Preferred audio language", "Version to play when a song has several",
            "Automatic", "Plays the standard audio of each song",
            "If a song has no version in this language, the standard audio plays."
        ),
        "it" to RegionalCopy(
            "Lingua dell'app", "Menu e testi dell'app",
            "Paese dei contenuti", "Classifiche, novità e suggerimenti",
            "Lingua audio preferita", "Versione da riprodurre se un brano ne ha più di una",
            "Automatico", "Riproduce l'audio standard di ogni brano",
            "Se un brano non ha una versione in questa lingua, viene riprodotto l'audio standard."
        ),
        "es" to RegionalCopy(
            "Idioma de la app", "Menús y textos de la app",
            "País del contenido", "Listas, novedades y sugerencias",
            "Idioma de audio preferido", "Versión que suena si una canción tiene varias",
            "Automático", "Reproduce el audio estándar de cada canción",
            "Si una canción no tiene versión en este idioma, se reproduce el audio estándar."
        ),
        "fr" to RegionalCopy(
            "Langue de l'application", "Menus et textes de l'application",
            "Pays du contenu", "Classements, nouveautés et suggestions",
            "Langue audio préférée", "Version à lire si un titre en propose plusieurs",
            "Automatique", "Lit l'audio standard de chaque titre",
            "Si un titre n'a pas de version dans cette langue, l'audio standard est lu."
        ),
        "de" to RegionalCopy(
            "App-Sprache", "Menüs und Texte der App",
            "Land der Inhalte", "Charts, Neuheiten und Vorschläge",
            "Bevorzugte Audiosprache", "Version, wenn ein Song mehrere hat",
            "Automatisch", "Spielt den Standardton jedes Songs",
            "Hat ein Song keine Version in dieser Sprache, wird der Standardton gespielt."
        ),
        "pt" to RegionalCopy(
            "Idioma da aplicação", "Menus e textos da aplicação",
            "País do conteúdo", "Tops, novidades e sugestões",
            "Idioma de áudio preferido", "Versão a reproduzir se uma música tiver várias",
            "Automático", "Reproduz o áudio padrão de cada música",
            "Se uma música não tiver versão neste idioma, é reproduzido o áudio padrão."
        ),
        "pt-BR" to RegionalCopy(
            "Idioma do app", "Menus e textos do app",
            "País do conteúdo", "Paradas, novidades e sugestões",
            "Idioma de áudio preferido", "Versão que toca se uma música tiver várias",
            "Automático", "Toca o áudio padrão de cada música",
            "Se uma música não tiver versão neste idioma, o áudio padrão será tocado."
        ),
        "nl" to RegionalCopy(
            "App-taal", "Menu's en teksten in de app",
            "Land van de inhoud", "Hitlijsten, nieuwe releases en suggesties",
            "Voorkeurstaal voor audio", "Versie om af te spelen als een nummer er meerdere heeft",
            "Automatisch", "Speelt de standaardaudio van elk nummer",
            "Heeft een nummer geen versie in deze taal, dan wordt de standaardaudio afgespeeld."
        ),
        "pl" to RegionalCopy(
            "Język aplikacji", "Menu i teksty w aplikacji",
            "Kraj treści", "Listy przebojów, nowości i sugestie",
            "Preferowany język dźwięku", "Wersja odtwarzana, gdy utwór ma ich kilka",
            "Automatycznie", "Odtwarza standardowy dźwięk każdego utworu",
            "Jeśli utwór nie ma wersji w tym języku, odtwarzany jest standardowy dźwięk."
        ),
        "ro" to RegionalCopy(
            "Limba aplicației", "Meniurile și textele aplicației",
            "Țara conținutului", "Topuri, noutăți și sugestii",
            "Limba audio preferată", "Versiunea redată dacă o piesă are mai multe",
            "Automat", "Redă audio standard al fiecărei piese",
            "Dacă o piesă nu are versiune în această limbă, se redă audio standard."
        ),
        "el" to RegionalCopy(
            "Γλώσσα εφαρμογής", "Μενού και κείμενα της εφαρμογής",
            "Χώρα περιεχομένου", "Τσαρτ, νέες κυκλοφορίες και προτάσεις",
            "Προτιμώμενη γλώσσα ήχου", "Έκδοση αναπαραγωγής όταν ένα τραγούδι έχει πολλές",
            "Αυτόματα", "Παίζει τον τυπικό ήχο κάθε τραγουδιού",
            "Αν ένα τραγούδι δεν έχει έκδοση σε αυτή τη γλώσσα, παίζει ο τυπικός ήχος."
        ),
        "sv" to RegionalCopy(
            "Appspråk", "Menyer och texter i appen",
            "Innehållsland", "Topplistor, nyheter och förslag",
            "Önskat ljudspråk", "Version som spelas när en låt har flera",
            "Automatiskt", "Spelar standardljudet för varje låt",
            "Om en låt saknar version på det här språket spelas standardljudet."
        ),
        "da" to RegionalCopy(
            "Appsprog", "Menuer og tekster i appen",
            "Indholdsland", "Hitlister, nyheder og forslag",
            "Foretrukket lydsprog", "Version, der afspilles, når en sang har flere",
            "Automatisk", "Afspiller standardlyden for hver sang",
            "Hvis en sang ikke har en version på dette sprog, afspilles standardlyden."
        ),
        "cs" to RegionalCopy(
            "Jazyk aplikace", "Nabídky a texty v aplikaci",
            "Země obsahu", "Žebříčky, novinky a návrhy",
            "Preferovaný jazyk zvuku", "Verze, která hraje, když má skladba více verzí",
            "Automaticky", "Přehrává standardní zvuk každé skladby",
            "Pokud skladba nemá verzi v tomto jazyce, přehraje se standardní zvuk."
        ),
        "sk" to RegionalCopy(
            "Jazyk aplikácie", "Ponuky a texty v aplikácii",
            "Krajina obsahu", "Rebríčky, novinky a návrhy",
            "Preferovaný jazyk zvuku", "Verzia, ktorá hrá, keď má skladba viac verzií",
            "Automaticky", "Prehráva štandardný zvuk každej skladby",
            "Ak skladba nemá verziu v tomto jazyku, prehrá sa štandardný zvuk."
        ),
        "hr" to RegionalCopy(
            "Jezik aplikacije", "Izbornici i tekstovi u aplikaciji",
            "Država sadržaja", "Ljestvice, novosti i prijedlozi",
            "Željeni jezik zvuka", "Verzija koja se pušta kad pjesma ima više verzija",
            "Automatski", "Pušta standardni zvuk svake pjesme",
            "Ako pjesma nema verziju na ovom jeziku, pušta se standardni zvuk."
        ),
        "sr" to RegionalCopy(
            "Језик апликације", "Менији и текстови у апликацији",
            "Земља садржаја", "Топ-листе, новитети и предлози",
            "Жељени језик звука", "Верзија која се пушта кад песма има више верзија",
            "Аутоматски", "Пушта стандардни звук сваке песме",
            "Ако песма нема верзију на овом језику, пушта се стандардни звук."
        ),
        "sq" to RegionalCopy(
            "Gjuha e aplikacionit", "Menytë dhe tekstet e aplikacionit",
            "Vendi i përmbajtjes", "Renditje, risi dhe sugjerime",
            "Gjuha e preferuar e audios", "Versioni që luhet kur një këngë ka disa",
            "Automatik", "Luan audion standarde të çdo kënge",
            "Nëse një këngë nuk ka version në këtë gjuhë, luhet audioja standarde."
        ),
        "bg" to RegionalCopy(
            "Език на приложението", "Менюта и текстове в приложението",
            "Държава на съдържанието", "Класации, новости и предложения",
            "Предпочитан език на аудиото", "Версия за възпроизвеждане, ако песента има няколко",
            "Автоматично", "Възпроизвежда стандартното аудио на всяка песен",
            "Ако песента няма версия на този език, се възпроизвежда стандартното аудио."
        ),
        "hu" to RegionalCopy(
            "Alkalmazás nyelve", "Menük és szövegek az alkalmazásban",
            "Tartalom országa", "Toplisták, újdonságok és javaslatok",
            "Előnyben részesített hangnyelv", "Ez a változat szól, ha egy dalnak több is van",
            "Automatikus", "Minden dal normál hangját játssza le",
            "Ha egy dalnak nincs ilyen nyelvű változata, a normál hang szól."
        ),
        "fi" to RegionalCopy(
            "Sovelluksen kieli", "Sovelluksen valikot ja tekstit",
            "Sisällön maa", "Listat, uutuudet ja ehdotukset",
            "Ensisijainen äänen kieli", "Toistettava versio, jos kappaleella on useita",
            "Automaattinen", "Toistaa jokaisen kappaleen vakioäänen",
            "Jos kappaleesta ei ole versiota tällä kielellä, toistetaan vakioääni."
        ),
        "et" to RegionalCopy(
            "Rakenduse keel", "Rakenduse menüüd ja tekstid",
            "Sisu riik", "Edetabelid, uudised ja soovitused",
            "Eelistatud heli keel", "Versioon, mida esitada, kui lool on neid mitu",
            "Automaatne", "Esitab iga loo tavalist heli",
            "Kui lool pole selles keeles versiooni, esitatakse tavaline heli."
        ),
        "nb" to RegionalCopy(
            "Appspråk", "Menyer og tekster i appen",
            "Innholdsland", "Topplister, nyheter og forslag",
            "Foretrukket lydspråk", "Versjon som spilles når en sang har flere",
            "Automatisk", "Spiller standardlyden for hver sang",
            "Hvis en sang ikke har en versjon på dette språket, spilles standardlyden."
        ),
        "ca" to RegionalCopy(
            "Idioma de l'app", "Menús i textos de l'app",
            "País del contingut", "Llistes, novetats i suggeriments",
            "Idioma d'àudio preferit", "Versió que sona si una cançó en té diverses",
            "Automàtic", "Reprodueix l'àudio estàndard de cada cançó",
            "Si una cançó no té versió en aquest idioma, es reprodueix l'àudio estàndard."
        ),
        "uk" to RegionalCopy(
            "Мова застосунку", "Меню й тексти застосунку",
            "Країна контенту", "Чарти, новинки та пропозиції",
            "Бажана мова аудіо", "Версія для відтворення, якщо пісня має кілька",
            "Автоматично", "Відтворює стандартне аудіо кожної пісні",
            "Якщо пісня не має версії цією мовою, відтворюється стандартне аудіо."
        ),
        "ru" to RegionalCopy(
            "Язык приложения", "Меню и тексты приложения",
            "Страна контента", "Чарты, новинки и рекомендации",
            "Предпочитаемый язык аудио", "Какую версию включать, если у песни их несколько",
            "Автоматически", "Воспроизводит стандартное аудио каждой песни",
            "Если у песни нет версии на этом языке, воспроизводится стандартное аудио."
        ),
        "kk" to RegionalCopy(
            "Қолданба тілі", "Қолданбадағы мәзірлер мен мәтіндер",
            "Контент елі", "Чарттар, жаңалықтар және ұсыныстар",
            "Қалаулы аудио тілі", "Әннің бірнеше нұсқасы болса, ойнатылатын нұсқа",
            "Автоматты", "Әр әннің стандартты аудиосын ойнатады",
            "Әннің бұл тілдегі нұсқасы болмаса, стандартты аудио ойнатылады."
        ),
        "tr" to RegionalCopy(
            "Uygulama dili", "Uygulamadaki menüler ve metinler",
            "İçerik ülkesi", "Listeler, yeni çıkanlar ve öneriler",
            "Tercih edilen ses dili", "Bir şarkının birden çok sürümü varsa çalınacak sürüm",
            "Otomatik", "Her şarkının standart sesini çalar",
            "Şarkının bu dilde sürümü yoksa standart ses çalınır."
        ),
        "ar" to RegionalCopy(
            "لغة التطبيق", "قوائم التطبيق ونصوصه",
            "بلد المحتوى", "قوائم الأغاني والإصدارات الجديدة والاقتراحات",
            "لغة الصوت المفضلة", "النسخة التي تُشغَّل عندما يكون للأغنية عدة نسخ",
            "تلقائي", "يشغّل الصوت القياسي لكل أغنية",
            "إذا لم تتوفر للأغنية نسخة بهذه اللغة، فسيُشغَّل الصوت القياسي."
        ),
        "fa" to RegionalCopy(
            "زبان برنامه", "منوها و متن‌های برنامه",
            "کشور محتوا", "جدول‌ها، تازه‌ها و پیشنهادها",
            "زبان صوتی ترجیحی", "نسخه‌ای که هنگام وجود چند نسخه از آهنگ پخش می‌شود",
            "خودکار", "صدای استاندارد هر آهنگ را پخش می‌کند",
            "اگر آهنگ نسخه‌ای به این زبان نداشته باشد، صدای استاندارد پخش می‌شود."
        ),
        "zh" to RegionalCopy(
            "应用语言", "应用内的菜单和文字",
            "内容所在国家/地区", "排行榜、新歌和推荐",
            "首选音频语言", "歌曲有多个版本时播放的版本",
            "自动", "播放每首歌曲的标准音频",
            "如果歌曲没有此语言版本，将播放标准音频。"
        ),
        "zh-Hant" to RegionalCopy(
            "應用程式語言", "應用程式內的選單和文字",
            "內容所在國家/地區", "排行榜、新歌和推薦",
            "偏好音訊語言", "歌曲有多個版本時播放的版本",
            "自動", "播放每首歌曲的標準音訊",
            "如果歌曲沒有此語言版本，將播放標準音訊。"
        ),
        "ja" to RegionalCopy(
            "アプリの言語", "アプリ内のメニューとテキスト",
            "コンテンツの国", "チャート、新着、おすすめ",
            "優先する音声言語", "曲に複数のバージョンがあるときに再生する版",
            "自動", "各曲の標準音声を再生します",
            "この言語のバージョンがない曲は、標準音声を再生します。"
        ),
        "ko" to RegionalCopy(
            "앱 언어", "앱의 메뉴와 텍스트",
            "콘텐츠 국가", "차트, 최신 음악, 추천",
            "선호 오디오 언어", "곡에 여러 버전이 있을 때 재생할 버전",
            "자동", "각 곡의 기본 오디오를 재생합니다",
            "이 언어 버전이 없는 곡은 기본 오디오를 재생합니다."
        ),
        "hi" to RegionalCopy(
            "ऐप की भाषा", "ऐप के मेनू और टेक्स्ट",
            "कंटेंट का देश", "चार्ट, नई रिलीज़ और सुझाव",
            "पसंदीदा ऑडियो भाषा", "गाने के कई संस्करण होने पर चलने वाला संस्करण",
            "स्वचालित", "हर गाने का मानक ऑडियो चलाता है",
            "अगर गाने का इस भाषा में संस्करण नहीं है, तो मानक ऑडियो चलेगा।"
        ),
        "bn" to RegionalCopy(
            "অ্যাপের ভাষা", "অ্যাপের মেনু ও লেখা",
            "কনটেন্টের দেশ", "চার্ট, নতুন রিলিজ ও পরামর্শ",
            "পছন্দের অডিও ভাষা", "গানের একাধিক সংস্করণ থাকলে যেটি বাজবে",
            "স্বয়ংক্রিয়", "প্রতিটি গানের মানক অডিও বাজায়",
            "গানের এই ভাষায় সংস্করণ না থাকলে মানক অডিও বাজবে।"
        ),
        "id" to RegionalCopy(
            "Bahasa aplikasi", "Menu dan teks di aplikasi",
            "Negara konten", "Tangga lagu, rilisan baru, dan saran",
            "Bahasa audio pilihan", "Versi yang diputar jika lagu memiliki beberapa versi",
            "Otomatis", "Memutar audio standar setiap lagu",
            "Jika lagu tidak memiliki versi dalam bahasa ini, audio standar akan diputar."
        ),
        "ms" to RegionalCopy(
            "Bahasa aplikasi", "Menu dan teks dalam aplikasi",
            "Negara kandungan", "Carta, keluaran baharu dan cadangan",
            "Bahasa audio pilihan", "Versi yang dimainkan jika lagu ada beberapa versi",
            "Automatik", "Memainkan audio standard setiap lagu",
            "Jika lagu tiada versi dalam bahasa ini, audio standard akan dimainkan."
        ),
        "vi" to RegionalCopy(
            "Ngôn ngữ ứng dụng", "Menu và văn bản trong ứng dụng",
            "Quốc gia của nội dung", "Bảng xếp hạng, bản mới và gợi ý",
            "Ngôn ngữ âm thanh ưu tiên", "Phiên bản được phát khi bài hát có nhiều bản",
            "Tự động", "Phát âm thanh tiêu chuẩn của từng bài hát",
            "Nếu bài hát không có bản bằng ngôn ngữ này, âm thanh tiêu chuẩn sẽ được phát."
        ),
        "th" to RegionalCopy(
            "ภาษาของแอป", "เมนูและข้อความในแอป",
            "ประเทศของเนื้อหา", "ชาร์ต เพลงใหม่ และคำแนะนำ",
            "ภาษาเสียงที่ต้องการ", "เวอร์ชันที่จะเล่นเมื่อเพลงมีหลายเวอร์ชัน",
            "อัตโนมัติ", "เล่นเสียงมาตรฐานของแต่ละเพลง",
            "หากเพลงไม่มีเวอร์ชันภาษานี้ ระบบจะเล่นเสียงมาตรฐาน"
        ),
        "fil" to RegionalCopy(
            "Wika ng app", "Mga menu at teksto sa app",
            "Bansa ng content", "Mga chart, bagong labas at mungkahi",
            "Gustong wika ng audio", "Bersyong ipe-play kung marami ang kanta",
            "Awtomatiko", "Pine-play ang karaniwang audio ng bawat kanta",
            "Kung walang bersyon ang kanta sa wikang ito, ang karaniwang audio ang ipe-play."
        ),
        "he" to RegionalCopy(
            "שפת האפליקציה", "התפריטים והטקסטים באפליקציה",
            "מדינת התוכן", "מצעדים, חדשים והצעות",
            "שפת שמע מועדפת", "הגרסה שתושמע כשלשיר יש כמה גרסאות",
            "אוטומטי", "משמיע את השמע הרגיל של כל שיר",
            "אם אין לשיר גרסה בשפה זו, יושמע השמע הרגיל."
        )
    )

    val localizedCodes: Set<String> = copies.keys

    fun forCode(code: String): RegionalCopy = localizedValueOrEnglish(copies, code)
}
