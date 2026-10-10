package com.luc4n3x.levyra.ui.i18n

internal val playerDeckKeys = setOf(
    "playerDeck",
    "playerDeckSubtitle",
    "playerDeckEditorial",
    "playerDeckPulse",
    "playerDeckImmersiveHint",
    "playerDeckCardHint",
    "playerDeckArtworkHint",
    "playerDeckEditorialHint",
    "playerDeckPulseHint",
    "playerDeckLandscapeNote"
)

private fun playerDeck(
    title: String,
    subtitle: String,
    editorial: String,
    pulse: String,
    immersiveHint: String,
    cardHint: String,
    artworkHint: String,
    editorialHint: String,
    pulseHint: String,
    landscapeNote: String
): Map<String, String> = mapOf(
    "playerDeck" to title,
    "playerDeckSubtitle" to subtitle,
    "playerDeckEditorial" to editorial,
    "playerDeckPulse" to pulse,
    "playerDeckImmersiveHint" to immersiveHint,
    "playerDeckCardHint" to cardHint,
    "playerDeckArtworkHint" to artworkHint,
    "playerDeckEditorialHint" to editorialHint,
    "playerDeckPulseHint" to pulseHint,
    "playerDeckLandscapeNote" to landscapeNote
)

internal data class PlayerDeckShortLabels(
    val immersive: String,
    val canvasCard: String,
    val artwork: String,
    val editorial: String,
    val pulse: String
)

private val playerDeckShortLabelsByCode: Map<String, PlayerDeckShortLabels> = mapOf(
    "en" to PlayerDeckShortLabels("Immersive", "Canvas Card", "Artwork", "Editorial", "Pulse"),
    "it" to PlayerDeckShortLabels("Immersivo", "Scheda Canvas", "Copertina", "Editoriale", "Pulse"),
    "es" to PlayerDeckShortLabels("Inmersivo", "Tarjeta Canvas", "Portada", "Editorial", "Pulse"),
    "fr" to PlayerDeckShortLabels("Immersif", "Carte Canvas", "Pochette", "Éditorial", "Pulse"),
    "de" to PlayerDeckShortLabels("Immersiv", "Canvas-Karte", "Cover", "Editorial", "Pulse"),
    "pt" to PlayerDeckShortLabels("Imersivo", "Cartão Canvas", "Capa", "Editorial", "Pulse"),
    "nl" to PlayerDeckShortLabels("Immersief", "Canvas-kaart", "Cover", "Editorial", "Pulse"),
    "pl" to PlayerDeckShortLabels("Immersyjny", "Karta Canvas", "Okładka", "Magazyn", "Pulse"),
    "ro" to PlayerDeckShortLabels("Imersiv", "Card Canvas", "Copertă", "Editorial", "Pulse"),
    "el" to PlayerDeckShortLabels("Καθηλωτικό", "Κάρτα Canvas", "Εξώφυλλο", "Editorial", "Pulse"),
    "sv" to PlayerDeckShortLabels("Immersiv", "Canvas-kort", "Omslag", "Editorial", "Pulse"),
    "da" to PlayerDeckShortLabels("Fordybende", "Canvas-kort", "Cover", "Editorial", "Pulse"),
    "cs" to PlayerDeckShortLabels("Pohlcující", "Karta Canvas", "Obal", "Magazín", "Pulse"),
    "uk" to PlayerDeckShortLabels("Імерсивний", "Картка Canvas", "Обкладинка", "Журнал", "Pulse"),
    "ru" to PlayerDeckShortLabels("Иммерсивный", "Карточка Canvas", "Обложка", "Журнал", "Pulse"),
    "tr" to PlayerDeckShortLabels("Sürükleyici", "Canvas Kartı", "Kapak", "Editoryal", "Pulse"),
    "ar" to PlayerDeckShortLabels("غامر", "بطاقة Canvas", "الغلاف", "تحريري", "Pulse"),
    "zh" to PlayerDeckShortLabels("沉浸", "Canvas 卡片", "封面", "杂志", "Pulse"),
    "ja" to PlayerDeckShortLabels("没入型", "Canvasカード", "アートワーク", "エディトリアル", "Pulse"),
    "ko" to PlayerDeckShortLabels("몰입형", "Canvas 카드", "아트워크", "에디토리얼", "Pulse"),
    "hi" to PlayerDeckShortLabels("इमर्सिव", "Canvas कार्ड", "कवर", "संपादकीय", "Pulse"),
    "id" to PlayerDeckShortLabels("Imersif", "Kartu Canvas", "Sampul", "Editorial", "Pulse"),
    "vi" to PlayerDeckShortLabels("Đắm chìm", "Thẻ Canvas", "Ảnh bìa", "Tạp chí", "Pulse"),
    "th" to PlayerDeckShortLabels("เต็มจอ", "การ์ด Canvas", "ปก", "นิตยสาร", "Pulse"),
    "fil" to PlayerDeckShortLabels("Immersive", "Canvas Card", "Cover", "Editorial", "Pulse"),
    "he" to PlayerDeckShortLabels("סוחף", "כרטיס Canvas", "עטיפה", "מגזין", "Pulse"),
    "fi" to PlayerDeckShortLabels("Immersiivinen", "Canvas-kortti", "Kansi", "Toimitus", "Pulse"),
    "et" to PlayerDeckShortLabels("Kaasahaarav", "Canvas-kaart", "Kaas", "Toimetus", "Pulse")
)

internal fun playerDeckShortLabels(code: String): PlayerDeckShortLabels =
    playerDeckShortLabelsByCode[code] ?: playerDeckShortLabelsByCode.getValue("en")

private val playerDeckBundles: Map<String, Map<String, String>> = mapOf(
    "en" to playerDeck("Player Deck", "Choose your player style.", "Editorial", "Pulse", "Full-screen Canvas.", "Floating Canvas.", "Artwork only.", "Magazine layout.", "Live audio pulse.", "Video, radio and landscape use Levyra."),
    "it" to playerDeck("Player Deck", "Scegli lo stile del player.", "Editoriale", "Pulse", "Canvas a tutto schermo.", "Canvas sospeso.", "Solo copertina.", "Layout da rivista.", "Segnale audio live.", "Video, radio e orizzontale usano Levyra."),
    "es" to playerDeck("Player Deck", "Elige el estilo del reproductor.", "Editorial", "Pulse", "Canvas a pantalla completa.", "Canvas flotante.", "Solo portada.", "Diseño editorial.", "Pulso de audio en vivo.", "Vídeo, radio y horizontal usan Levyra."),
    "fr" to playerDeck("Player Deck", "Choisissez le style du lecteur.", "Éditorial", "Pulse", "Canvas plein écran.", "Canvas flottant.", "Pochette seule.", "Mise en page éditoriale.", "Pulse audio en direct.", "Vidéo, radio et paysage utilisent Levyra."),
    "de" to playerDeck("Player Deck", "Wähle deinen Player-Stil.", "Editorial", "Pulse", "Canvas im Vollbild.", "Schwebendes Canvas.", "Nur Cover.", "Editoriales Layout.", "Live-Audiopuls.", "Video, Radio und Querformat nutzen Levyra."),
    "pt" to playerDeck("Player Deck", "Escolhe o estilo do leitor.", "Editorial", "Pulse", "Canvas em ecrã inteiro.", "Canvas flutuante.", "Só capa.", "Layout editorial.", "Pulso de áudio ao vivo.", "Vídeo, rádio e horizontal usam Levyra."),
    "nl" to playerDeck("Player Deck", "Kies je playerstijl.", "Editorial", "Pulse", "Canvas op volledig scherm.", "Zwevend Canvas.", "Alleen cover.", "Redactionele lay-out.", "Live audiopuls.", "Video, radio en liggend gebruiken Levyra."),
    "pl" to playerDeck("Player Deck", "Wybierz styl odtwarzacza.", "Magazyn", "Pulse", "Canvas na pełnym ekranie.", "Pływający Canvas.", "Tylko okładka.", "Układ magazynowy.", "Puls audio na żywo.", "Wideo, radio i poziom używają Levyra."),
    "ro" to playerDeck("Player Deck", "Alege stilul playerului.", "Editorial", "Pulse", "Canvas pe tot ecranul.", "Canvas plutitor.", "Doar coperta.", "Aspect editorial.", "Puls audio live.", "Video, radio și peisaj folosesc Levyra."),
    "el" to playerDeck("Player Deck", "Διάλεξε στυλ αναπαραγωγής.", "Editorial", "Pulse", "Canvas πλήρους οθόνης.", "Αιωρούμενο Canvas.", "Μόνο εξώφυλλο.", "Editorial διάταξη.", "Ζωντανός παλμός ήχου.", "Βίντεο, ράδιο και οριζόντια προβολή χρησιμοποιούν Levyra."),
    "sv" to playerDeck("Player Deck", "Välj spelarstil.", "Editorial", "Pulse", "Canvas i helskärm.", "Svävande Canvas.", "Endast omslag.", "Redaktionell layout.", "Live ljudpuls.", "Video, radio och liggande använder Levyra."),
    "da" to playerDeck("Player Deck", "Vælg afspillerstil.", "Editorial", "Pulse", "Canvas i fuld skærm.", "Svævende Canvas.", "Kun cover.", "Redaktionelt layout.", "Live lydpuls.", "Video, radio og liggende bruger Levyra."),
    "cs" to playerDeck("Player Deck", "Vyber styl přehrávače.", "Magazín", "Pulse", "Canvas přes celou obrazovku.", "Plovoucí Canvas.", "Jen obal.", "Magazínové rozvržení.", "Živý zvukový puls.", "Video, rádio a zobrazení na šířku používají Levyra."),
    "uk" to playerDeck("Player Deck", "Виберіть стиль плеєра.", "Журнал", "Pulse", "Canvas на весь екран.", "Плаваючий Canvas.", "Лише обкладинка.", "Журнальний макет.", "Живий аудіопульс.", "Відео, радіо й альбомний режим використовують Levyra."),
    "ru" to playerDeck("Player Deck", "Выберите стиль плеера.", "Журнал", "Pulse", "Canvas на весь экран.", "Плавающий Canvas.", "Только обложка.", "Журнальный макет.", "Живой аудиопульс.", "Видео, радио и альбомный режим используют Levyra."),
    "tr" to playerDeck("Player Deck", "Oynatıcı stilini seç.", "Editoryal", "Pulse", "Tam ekran Canvas.", "Yüzen Canvas.", "Yalnızca kapak.", "Editoryal düzen.", "Canlı ses darbesi.", "Video, radyo ve yatay görünüm Levyra kullanır."),
    "ar" to playerDeck("Player Deck", "اختر نمط المشغل.", "تحريري", "Pulse", "Canvas بملء الشاشة.", "Canvas عائم.", "الغلاف فقط.", "تخطيط تحريري.", "نبض صوتي مباشر.", "الفيديو والراديو والوضع الأفقي تستخدم Levyra."),
    "zh" to playerDeck("Player Deck", "选择播放器样式。", "杂志", "Pulse", "全屏 Canvas。", "悬浮 Canvas。", "仅封面。", "杂志布局。", "实时音频脉冲。", "视频、电台和横屏使用 Levyra。"),
    "ja" to playerDeck("Player Deck", "プレイヤーのスタイルを選択。", "エディトリアル", "Pulse", "フルスクリーン Canvas。", "フローティング Canvas。", "アートワークのみ。", "エディトリアル表示。", "ライブ音声パルス。", "動画、ラジオ、横画面は Levyra を使用します。"),
    "ko" to playerDeck("Player Deck", "플레이어 스타일을 선택하세요.", "에디토리얼", "Pulse", "전체 화면 Canvas.", "플로팅 Canvas.", "아트워크만.", "에디토리얼 레이아웃.", "라이브 오디오 펄스.", "동영상, 라디오, 가로 화면은 Levyra를 사용합니다."),
    "hi" to playerDeck("Player Deck", "प्लेयर शैली चुनें।", "एडिटोरियल", "Pulse", "फुल-स्क्रीन Canvas।", "फ्लोटिंग Canvas।", "केवल कवर।", "संपादकीय लेआउट।", "लाइव ऑडियो पल्स।", "वीडियो, रेडियो और लैंडस्केप Levyra का उपयोग करते हैं।"),
    "id" to playerDeck("Player Deck", "Pilih gaya pemutar.", "Editorial", "Pulse", "Canvas layar penuh.", "Canvas mengambang.", "Sampul saja.", "Tata letak editorial.", "Denyut audio langsung.", "Video, radio, dan lanskap memakai Levyra."),
    "vi" to playerDeck("Player Deck", "Chọn kiểu trình phát.", "Tạp chí", "Pulse", "Canvas toàn màn hình.", "Canvas nổi.", "Chỉ ảnh bìa.", "Bố cục tạp chí.", "Nhịp âm thanh trực tiếp.", "Video, radio và màn hình ngang dùng Levyra."),
    "th" to playerDeck("Player Deck", "เลือกสไตล์เครื่องเล่น", "นิตยสาร", "Pulse", "Canvas เต็มหน้าจอ", "Canvas แบบลอย", "เฉพาะปก", "เลย์เอาต์นิตยสาร", "พัลส์เสียงสด", "วิดีโอ วิทยุ และแนวนอนใช้ Levyra"),
    "fil" to playerDeck("Player Deck", "Piliin ang estilo ng player.", "Editorial", "Pulse", "Full-screen Canvas.", "Floating Canvas.", "Cover lang.", "Editorial na layout.", "Live audio pulse.", "Video, radio at landscape ay gumagamit ng Levyra."),
    "he" to playerDeck("Player Deck", "בחר סגנון נגן.", "מגזין", "Pulse", "Canvas במסך מלא.", "Canvas צף.", "עטיפה בלבד.", "פריסת מגזין.", "פולס שמע חי.", "וידאו, רדיו ותצוגה לרוחב משתמשים ב-Levyra."),
    "fi" to playerDeck("Player Deck", "Valitse soittimen tyyli.", "Toimituksellinen", "Pulse", "Canvas koko näytöllä.", "Kelluva Canvas.", "Vain kansi.", "Toimituksellinen asettelu.", "Live-äänipulssi.", "Video, radio ja vaakatila käyttävät Levyraa."),
    "et" to playerDeck("Player Deck", "Vali mängija stiil.", "Toimetuslik", "Pulse", "Canvas täisekraanil.", "Hõljuv Canvas.", "Ainult kaas.", "Toimetuslik paigutus.", "Reaalaja helipulss.", "Video, raadio ja rõhtvaade kasutavad Levyrat.")
)

internal fun playerDeckLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playerDeckBundles, code)

internal fun playerDeckLocalizationCodes(): Set<String> = playerDeckBundles.keys
