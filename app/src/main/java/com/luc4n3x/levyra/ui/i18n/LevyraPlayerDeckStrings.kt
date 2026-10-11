package com.luc4n3x.levyra.ui.i18n

internal val playerDeckKeys = setOf(
    "playerDeck",
    "playerDeckSubtitle",
    "playerDeckExpressive",
    "playerDeckPulse",
    "playerDeckImmersiveHint",
    "playerDeckCardHint",
    "playerDeckArtworkHint",
    "playerDeckExpressiveHint",
    "playerDeckPulseHint",
    "playerDeckLandscapeNote"
)

private fun playerDeck(
    title: String,
    subtitle: String,
    expressive: String,
    pulse: String,
    immersiveHint: String,
    cardHint: String,
    artworkHint: String,
    expressiveHint: String,
    pulseHint: String,
    landscapeNote: String
): Map<String, String> = mapOf(
    "playerDeck" to title,
    "playerDeckSubtitle" to subtitle,
    "playerDeckExpressive" to expressive,
    "playerDeckPulse" to pulse,
    "playerDeckImmersiveHint" to immersiveHint,
    "playerDeckCardHint" to cardHint,
    "playerDeckArtworkHint" to artworkHint,
    "playerDeckExpressiveHint" to expressiveHint,
    "playerDeckPulseHint" to pulseHint,
    "playerDeckLandscapeNote" to landscapeNote
)

internal data class PlayerDeckShortLabels(
    val immersive: String,
    val canvasCard: String,
    val artwork: String,
    val expressive: String,
    val pulse: String
)

private val playerDeckShortLabelsByCode: Map<String, PlayerDeckShortLabels> = mapOf(
    "en" to PlayerDeckShortLabels("Immersive", "Canvas Card", "Poster", "Expressive", "Pulse"),
    "it" to PlayerDeckShortLabels("Immersivo", "Scheda Canvas", "Poster", "Espressivo", "Pulse"),
    "es" to PlayerDeckShortLabels("Inmersivo", "Tarjeta Canvas", "Póster", "Expresivo", "Pulse"),
    "fr" to PlayerDeckShortLabels("Immersif", "Carte Canvas", "Affiche", "Expressif", "Pulse"),
    "de" to PlayerDeckShortLabels("Immersiv", "Canvas-Karte", "Poster", "Expressiv", "Pulse"),
    "pt" to PlayerDeckShortLabels("Imersivo", "Cartão Canvas", "Póster", "Expressivo", "Pulse"),
    "nl" to PlayerDeckShortLabels("Immersief", "Canvas-kaart", "Poster", "Expressief", "Pulse"),
    "pl" to PlayerDeckShortLabels("Immersyjny", "Karta Canvas", "Plakat", "Ekspresyjny", "Pulse"),
    "ro" to PlayerDeckShortLabels("Imersiv", "Card Canvas", "Poster", "Expresiv", "Pulse"),
    "el" to PlayerDeckShortLabels("Καθηλωτικό", "Κάρτα Canvas", "Αφίσα", "Εκφραστικό", "Pulse"),
    "sv" to PlayerDeckShortLabels("Immersiv", "Canvas-kort", "Affisch", "Expressiv", "Pulse"),
    "da" to PlayerDeckShortLabels("Fordybende", "Canvas-kort", "Plakat", "Ekspressiv", "Pulse"),
    "cs" to PlayerDeckShortLabels("Pohlcující", "Karta Canvas", "Plakát", "Expresivní", "Pulse"),
    "uk" to PlayerDeckShortLabels("Імерсивний", "Картка Canvas", "Постер", "Експресивний", "Pulse"),
    "ru" to PlayerDeckShortLabels("Иммерсивный", "Карточка Canvas", "Постер", "Экспрессивный", "Pulse"),
    "tr" to PlayerDeckShortLabels("Sürükleyici", "Canvas Kartı", "Poster", "Etkileyici", "Pulse"),
    "ar" to PlayerDeckShortLabels("غامر", "بطاقة Canvas", "ملصق", "تعبيري", "Pulse"),
    "zh" to PlayerDeckShortLabels("沉浸", "Canvas 卡片", "海报", "表现力", "Pulse"),
    "ja" to PlayerDeckShortLabels("没入型", "Canvasカード", "ポスター", "エクスプレッシブ", "Pulse"),
    "ko" to PlayerDeckShortLabels("몰입형", "Canvas 카드", "포스터", "익스프레시브", "Pulse"),
    "hi" to PlayerDeckShortLabels("इमर्सिव", "Canvas कार्ड", "पोस्टर", "एक्सप्रेसिव", "Pulse"),
    "id" to PlayerDeckShortLabels("Imersif", "Kartu Canvas", "Poster", "Ekspresif", "Pulse"),
    "vi" to PlayerDeckShortLabels("Đắm chìm", "Thẻ Canvas", "Áp phích", "Biểu cảm", "Pulse"),
    "th" to PlayerDeckShortLabels("เต็มจอ", "การ์ด Canvas", "โปสเตอร์", "เอ็กซ์เพรสซีฟ", "Pulse"),
    "fil" to PlayerDeckShortLabels("Immersive", "Canvas Card", "Poster", "Expressive", "Pulse"),
    "he" to PlayerDeckShortLabels("סוחף", "כרטיס Canvas", "פוסטר", "אקספרסיבי", "Pulse"),
    "fi" to PlayerDeckShortLabels("Immersiivinen", "Canvas-kortti", "Juliste", "Ilmeikäs", "Pulse"),
    "et" to PlayerDeckShortLabels("Kaasahaarav", "Canvas-kaart", "Plakat", "Ekspressiivne", "Pulse")
)

internal fun playerDeckShortLabels(code: String): PlayerDeckShortLabels =
    playerDeckShortLabelsByCode[code] ?: playerDeckShortLabelsByCode.getValue("en")

private val playerDeckBundles: Map<String, Map<String, String>> = mapOf(
    "en" to playerDeck("Player Deck", "Choose your player style.", "Expressive", "Pulse", "Full-screen Canvas.", "Floating Canvas.", "Artwork only.", "Material 3 Expressive.", "Live audio pulse.", "Video, radio and landscape use Levyra."),
    "it" to playerDeck("Player Deck", "Scegli lo stile del player.", "Espressivo", "Pulse", "Canvas a tutto schermo.", "Canvas sospeso.", "Solo copertina.", "Material 3 Expressive.", "Segnale audio live.", "Video, radio e orizzontale usano Levyra."),
    "es" to playerDeck("Player Deck", "Elige el estilo del reproductor.", "Expresivo", "Pulse", "Canvas a pantalla completa.", "Canvas flotante.", "Solo portada.", "Material 3 Expressive.", "Pulso de audio en vivo.", "Vídeo, radio y horizontal usan Levyra."),
    "fr" to playerDeck("Player Deck", "Choisissez le style du lecteur.", "Expressif", "Pulse", "Canvas plein écran.", "Canvas flottant.", "Pochette seule.", "Material 3 Expressive.", "Pulse audio en direct.", "Vidéo, radio et paysage utilisent Levyra."),
    "de" to playerDeck("Player Deck", "Wähle deinen Player-Stil.", "Expressiv", "Pulse", "Canvas im Vollbild.", "Schwebendes Canvas.", "Nur Cover.", "Material 3 Expressive.", "Live-Audiopuls.", "Video, Radio und Querformat nutzen Levyra."),
    "pt" to playerDeck("Player Deck", "Escolhe o estilo do leitor.", "Expressivo", "Pulse", "Canvas em ecrã inteiro.", "Canvas flutuante.", "Só capa.", "Material 3 Expressive.", "Pulso de áudio ao vivo.", "Vídeo, rádio e horizontal usam Levyra."),
    "nl" to playerDeck("Player Deck", "Kies je playerstijl.", "Expressief", "Pulse", "Canvas op volledig scherm.", "Zwevend Canvas.", "Alleen cover.", "Material 3 Expressive.", "Live audiopuls.", "Video, radio en liggend gebruiken Levyra."),
    "pl" to playerDeck("Player Deck", "Wybierz styl odtwarzacza.", "Ekspresyjny", "Pulse", "Canvas na pełnym ekranie.", "Pływający Canvas.", "Tylko okładka.", "Material 3 Expressive.", "Puls audio na żywo.", "Wideo, radio i poziom używają Levyra."),
    "ro" to playerDeck("Player Deck", "Alege stilul playerului.", "Expresiv", "Pulse", "Canvas pe tot ecranul.", "Canvas plutitor.", "Doar coperta.", "Material 3 Expressive.", "Puls audio live.", "Video, radio și peisaj folosesc Levyra."),
    "el" to playerDeck("Player Deck", "Διάλεξε στυλ αναπαραγωγής.", "Εκφραστικό", "Pulse", "Canvas πλήρους οθόνης.", "Αιωρούμενο Canvas.", "Μόνο εξώφυλλο.", "Material 3 Expressive.", "Ζωντανός παλμός ήχου.", "Βίντεο, ράδιο και οριζόντια προβολή χρησιμοποιούν Levyra."),
    "sv" to playerDeck("Player Deck", "Välj spelarstil.", "Expressiv", "Pulse", "Canvas i helskärm.", "Svävande Canvas.", "Endast omslag.", "Material 3 Expressive.", "Live ljudpuls.", "Video, radio och liggande använder Levyra."),
    "da" to playerDeck("Player Deck", "Vælg afspillerstil.", "Ekspressiv", "Pulse", "Canvas i fuld skærm.", "Svævende Canvas.", "Kun cover.", "Material 3 Expressive.", "Live lydpuls.", "Video, radio og liggende bruger Levyra."),
    "cs" to playerDeck("Player Deck", "Vyber styl přehrávače.", "Expresivní", "Pulse", "Canvas přes celou obrazovku.", "Plovoucí Canvas.", "Jen obal.", "Material 3 Expressive.", "Živý zvukový puls.", "Video, rádio a zobrazení na šířku používají Levyra."),
    "uk" to playerDeck("Player Deck", "Виберіть стиль плеєра.", "Експресивний", "Pulse", "Canvas на весь екран.", "Плаваючий Canvas.", "Лише обкладинка.", "Material 3 Expressive.", "Живий аудіопульс.", "Відео, радіо й альбомний режим використовують Levyra."),
    "ru" to playerDeck("Player Deck", "Выберите стиль плеера.", "Экспрессивный", "Pulse", "Canvas на весь экран.", "Плавающий Canvas.", "Только обложка.", "Material 3 Expressive.", "Живой аудиопульс.", "Видео, радио и альбомный режим используют Levyra."),
    "tr" to playerDeck("Player Deck", "Oynatıcı stilini seç.", "Etkileyici", "Pulse", "Tam ekran Canvas.", "Yüzen Canvas.", "Yalnızca kapak.", "Material 3 Expressive.", "Canlı ses darbesi.", "Video, radyo ve yatay görünüm Levyra kullanır."),
    "ar" to playerDeck("Player Deck", "اختر نمط المشغل.", "تعبيري", "Pulse", "Canvas بملء الشاشة.", "Canvas عائم.", "الغلاف فقط.", "Material 3 Expressive.", "نبض صوتي مباشر.", "الفيديو والراديو والوضع الأفقي تستخدم Levyra."),
    "zh" to playerDeck("Player Deck", "选择播放器样式。", "表现力", "Pulse", "全屏 Canvas。", "悬浮 Canvas。", "仅封面。", "Material 3 Expressive.", "实时音频脉冲。", "视频、电台和横屏使用 Levyra。"),
    "ja" to playerDeck("Player Deck", "プレイヤーのスタイルを選択。", "エクスプレッシブ", "Pulse", "フルスクリーン Canvas。", "フローティング Canvas。", "アートワークのみ。", "Material 3 Expressive.", "ライブ音声パルス。", "動画、ラジオ、横画面は Levyra を使用します。"),
    "ko" to playerDeck("Player Deck", "플레이어 스타일을 선택하세요.", "익스프레시브", "Pulse", "전체 화면 Canvas.", "플로팅 Canvas.", "아트워크만.", "Material 3 Expressive.", "라이브 오디오 펄스.", "동영상, 라디오, 가로 화면은 Levyra를 사용합니다."),
    "hi" to playerDeck("Player Deck", "प्लेयर शैली चुनें।", "एक्सप्रेसिव", "Pulse", "फुल-स्क्रीन Canvas।", "फ्लोटिंग Canvas।", "केवल कवर।", "Material 3 Expressive.", "लाइव ऑडियो पल्स।", "वीडियो, रेडियो और लैंडस्केप Levyra का उपयोग करते हैं।"),
    "id" to playerDeck("Player Deck", "Pilih gaya pemutar.", "Ekspresif", "Pulse", "Canvas layar penuh.", "Canvas mengambang.", "Sampul saja.", "Material 3 Expressive.", "Denyut audio langsung.", "Video, radio, dan lanskap memakai Levyra."),
    "vi" to playerDeck("Player Deck", "Chọn kiểu trình phát.", "Biểu cảm", "Pulse", "Canvas toàn màn hình.", "Canvas nổi.", "Chỉ ảnh bìa.", "Material 3 Expressive.", "Nhịp âm thanh trực tiếp.", "Video, radio và màn hình ngang dùng Levyra."),
    "th" to playerDeck("Player Deck", "เลือกสไตล์เครื่องเล่น", "เอ็กซ์เพรสซีฟ", "Pulse", "Canvas เต็มหน้าจอ", "Canvas แบบลอย", "เฉพาะปก", "Material 3 Expressive.", "พัลส์เสียงสด", "วิดีโอ วิทยุ และแนวนอนใช้ Levyra"),
    "fil" to playerDeck("Player Deck", "Piliin ang estilo ng player.", "Expressive", "Pulse", "Full-screen Canvas.", "Floating Canvas.", "Cover lang.", "Material 3 Expressive.", "Live audio pulse.", "Video, radio at landscape ay gumagamit ng Levyra."),
    "he" to playerDeck("Player Deck", "בחר סגנון נגן.", "אקספרסיבי", "Pulse", "Canvas במסך מלא.", "Canvas צף.", "עטיפה בלבד.", "Material 3 Expressive.", "פולס שמע חי.", "וידאו, רדיו ותצוגה לרוחב משתמשים ב-Levyra."),
    "fi" to playerDeck("Player Deck", "Valitse soittimen tyyli.", "Ilmeikäs", "Pulse", "Canvas koko näytöllä.", "Kelluva Canvas.", "Vain kansi.", "Material 3 Expressive.", "Live-äänipulssi.", "Video, radio ja vaakatila käyttävät Levyraa."),
    "et" to playerDeck("Player Deck", "Vali mängija stiil.", "Ekspressiivne", "Pulse", "Canvas täisekraanil.", "Hõljuv Canvas.", "Ainult kaas.", "Material 3 Expressive.", "Reaalaja helipulss.", "Video, raadio ja rõhtvaade kasutavad Levyrat.")
)

internal fun playerDeckLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playerDeckBundles, code)

internal fun playerDeckLocalizationCodes(): Set<String> = playerDeckBundles.keys
