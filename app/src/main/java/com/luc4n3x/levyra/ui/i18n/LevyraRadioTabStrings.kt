package com.luc4n3x.levyra.ui.i18n

internal data class RadioTabCopy(
    val tab: String,
    val forYou: String,
    val forYouSubtitle: String,
    val podcastsSubtitle: String
)

private val radioTabCopies = mapOf(
    "en" to RadioTabCopy("Radio", "Radio for you", "Endless stations from the artists you play", "Your shows, new episodes and discoveries"),
    "it" to RadioTabCopy("Radio", "Radio per te", "Stazioni infinite dagli artisti che ascolti", "I tuoi programmi, nuovi episodi e scoperte"),
    "es" to RadioTabCopy("Radio", "Radio para ti", "Emisoras sin fin de los artistas que escuchas", "Tus programas, nuevos episodios y descubrimientos"),
    "fr" to RadioTabCopy("Radio", "Radios pour toi", "Des stations sans fin à partir des artistes que tu écoutes", "Tes émissions, nouveaux épisodes et découvertes"),
    "de" to RadioTabCopy("Radio", "Radio für dich", "Endlose Sender aus den Künstlern, die du hörst", "Deine Shows, neue Folgen und Entdeckungen"),
    "pt" to RadioTabCopy("Rádio", "Rádio para ti", "Estações sem fim dos artistas que ouves", "Os teus programas, novos episódios e descobertas"),
    "pt-BR" to RadioTabCopy("Rádio", "Rádio para você", "Estações sem fim dos artistas que você ouve", "Seus programas, novos episódios e descobertas"),
    "nl" to RadioTabCopy("Radio", "Radio voor jou", "Eindeloze stations van de artiesten die je luistert", "Je shows, nieuwe afleveringen en ontdekkingen"),
    "pl" to RadioTabCopy("Radio", "Radio dla ciebie", "Niekończące się stacje od artystów, których słuchasz", "Twoje audycje, nowe odcinki i odkrycia"),
    "ro" to RadioTabCopy("Radio", "Radio pentru tine", "Posturi nesfârșite din artiștii pe care îi asculți", "Emisiunile tale, episoade noi și descoperiri"),
    "el" to RadioTabCopy("Ραδιόφωνο", "Ραδιόφωνο για σένα", "Ατελείωτοι σταθμοί από τους καλλιτέχνες που ακούς", "Οι εκπομπές σου, νέα επεισόδια και ανακαλύψεις"),
    "sv" to RadioTabCopy("Radio", "Radio för dig", "Oändliga stationer från artisterna du lyssnar på", "Dina program, nya avsnitt och upptäckter"),
    "da" to RadioTabCopy("Radio", "Radio til dig", "Uendelige stationer fra de kunstnere, du lytter til", "Dine programmer, nye afsnit og opdagelser"),
    "cs" to RadioTabCopy("Rádio", "Rádio pro tebe", "Nekonečné stanice od interpretů, které posloucháš", "Tvé pořady, nové díly a objevy"),
    "sk" to RadioTabCopy("Rádio", "Rádio pre teba", "Nekonečné stanice od interpretov, ktorých počúvaš", "Tvoje relácie, nové diely a objavy"),
    "hr" to RadioTabCopy("Radio", "Radio za tebe", "Beskrajne postaje izvođača koje slušaš", "Tvoje emisije, nove epizode i otkrića"),
    "sr" to RadioTabCopy("Радио", "Радио за тебе", "Бескрајне станице извођача које слушаш", "Твоје емисије, нове епизоде и открића"),
    "sq" to RadioTabCopy("Radio", "Radio për ty", "Stacione pa fund nga artistët që dëgjon", "Emisionet e tua, episode të reja dhe zbulime"),
    "bg" to RadioTabCopy("Радио", "Радио за теб", "Безкрайни станции от изпълнителите, които слушаш", "Твоите предавания, нови епизоди и открития"),
    "hu" to RadioTabCopy("Rádió", "Rádió neked", "Végtelen állomások a kedvenc előadóidból", "A műsoraid, új epizódok és felfedezések"),
    "fi" to RadioTabCopy("Radio", "Radiota sinulle", "Loputtomia asemia kuuntelemiltasi artisteilta", "Ohjelmasi, uudet jaksot ja löydöt"),
    "et" to RadioTabCopy("Raadio", "Raadio sulle", "Lõputud jaamad artistidelt, keda kuulad", "Sinu saated, uued osad ja avastused"),
    "nb" to RadioTabCopy("Radio", "Radio for deg", "Uendelige stasjoner fra artistene du lytter til", "Programmene dine, nye episoder og oppdagelser"),
    "ca" to RadioTabCopy("Ràdio", "Ràdio per a tu", "Emissores infinites dels artistes que escoltes", "Els teus programes, episodis nous i descobertes"),
    "uk" to RadioTabCopy("Радіо", "Радіо для тебе", "Нескінченні станції від виконавців, яких ти слухаєш", "Твої шоу, нові епізоди та відкриття"),
    "ru" to RadioTabCopy("Радио", "Радио для тебя", "Бесконечные станции от исполнителей, которых ты слушаешь", "Твои шоу, новые выпуски и открытия"),
    "kk" to RadioTabCopy("Радио", "Саған арналған радио", "Сен тыңдайтын әртістерден шексіз станциялар", "Сенің шоуларың, жаңа эпизодтар және жаңалықтар"),
    "tr" to RadioTabCopy("Radyo", "Senin için radyo", "Dinlediğin sanatçılardan sonsuz istasyonlar", "Programların, yeni bölümler ve keşifler"),
    "ar" to RadioTabCopy("الراديو", "راديو لك", "محطات لا تنتهي من الفنانين الذين تستمع إليهم", "برامجك والحلقات الجديدة والاكتشافات"),
    "fa" to RadioTabCopy("رادیو", "رادیو برای تو", "ایستگاه‌های بی‌پایان از هنرمندانی که گوش می‌دهی", "برنامه‌هایت، قسمت‌های تازه و کشف‌ها"),
    "zh" to RadioTabCopy("电台", "为你推荐的电台", "来自你常听艺人的无尽电台", "你的节目、新单集和发现"),
    "zh-Hant" to RadioTabCopy("電台", "為你推薦的電台", "來自你常聽藝人的無盡電台", "你的節目、新單集和發現"),
    "ja" to RadioTabCopy("ラジオ", "あなたへのラジオ", "よく聴くアーティストから終わりなく続くステーション", "番組、新しいエピソード、そして新たな発見"),
    "ko" to RadioTabCopy("라디오", "당신을 위한 라디오", "즐겨 듣는 아티스트로 끝없이 이어지는 스테이션", "내 프로그램, 새 에피소드와 발견"),
    "hi" to RadioTabCopy("रेडियो", "आपके लिए रेडियो", "आपके सुने कलाकारों से अंतहीन स्टेशन", "आपके शो, नए एपिसोड और खोजें"),
    "bn" to RadioTabCopy("রেডিও", "আপনার জন্য রেডিও", "আপনার শোনা শিল্পীদের থেকে অফুরন্ত স্টেশন", "আপনার শো, নতুন পর্ব ও আবিষ্কার"),
    "id" to RadioTabCopy("Radio", "Radio untukmu", "Stasiun tanpa henti dari artis yang kamu dengarkan", "Acaramu, episode baru, dan penemuan"),
    "ms" to RadioTabCopy("Radio", "Radio untuk anda", "Stesen tanpa henti daripada artis yang anda dengar", "Rancangan anda, episod baharu dan penemuan"),
    "vi" to RadioTabCopy("Radio", "Radio dành cho bạn", "Các đài phát bất tận từ nghệ sĩ bạn nghe", "Chương trình của bạn, tập mới và khám phá"),
    "th" to RadioTabCopy("วิทยุ", "วิทยุสำหรับคุณ", "สถานีไม่รู้จบจากศิลปินที่คุณฟัง", "รายการของคุณ ตอนใหม่ และการค้นพบ"),
    "fil" to RadioTabCopy("Radyo", "Radyo para sa iyo", "Walang katapusang istasyon mula sa mga artistang pinakikinggan mo", "Ang mga palabas mo, bagong episode at tuklas"),
    "he" to RadioTabCopy("רדיו", "רדיו בשבילך", "תחנות אינסופיות מהאמנים שאתה מאזין להם", "התוכניות שלך, פרקים חדשים וגילויים")
)

internal fun radioTabCopy(code: String): RadioTabCopy = localizedValueOrEnglish(radioTabCopies, code)

internal fun radioTabLocalizationCodes(): Set<String> = radioTabCopies.keys
