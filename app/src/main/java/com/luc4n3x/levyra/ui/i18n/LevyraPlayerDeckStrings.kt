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
    "playerDeckHalo",
    "playerDeckHaloHint",
    "playerDeckDefaultBadge",
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
    val pulse: String,
    val halo: String = "Liner Notes"
)

private val playerDeckShortLabelsByCode: Map<String, PlayerDeckShortLabels> = mapOf(
    "en" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "it" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "es" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "fr" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "de" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "pt" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "nl" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "pl" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "ro" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "el" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "sv" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "da" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "cs" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "uk" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "ru" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "tr" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "ar" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "zh" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "ja" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "ko" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "hi" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "id" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "vi" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "th" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "fil" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "he" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "fi" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night"),
    "et" to PlayerDeckShortLabels("Velvet", "Studio", "Gallery", "Bloom", "Late Night")
)

internal fun playerDeckShortLabels(code: String): PlayerDeckShortLabels =
    playerDeckShortLabelsByCode[code] ?: playerDeckShortLabelsByCode.getValue("en")

private val playerDeckBundles: Map<String, Map<String, String>> = mapOf(
    "en" to playerDeck("Player Deck", "Choose your player style.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Floating Canvas.", "Artwork only.", "Material 3 Expressive.", "Live audio pulse.", "Video, radio and landscape use Levyra."),
    "it" to playerDeck("Player Deck", "Scegli lo stile del player.", "Bloom", "Late Night", "Copertina luminosa, comandi essenziali.", "Canvas sospeso.", "Solo copertina.", "Material 3 Expressive.", "Segnale audio live.", "Video, radio e orizzontale usano Levyra."),
    "es" to playerDeck("Player Deck", "Elige el estilo del reproductor.", "Bloom", "Late Night", "Portada luminosa, controles sencillos.", "Canvas flotante.", "Solo portada.", "Material 3 Expressive.", "Pulso de audio en vivo.", "Vídeo, radio y horizontal usan Levyra."),
    "fr" to playerDeck("Player Deck", "Choisissez le style du lecteur.", "Bloom", "Late Night", "Pochette lumineuse, commandes épurées.", "Canvas flottant.", "Pochette seule.", "Material 3 Expressive.", "Pulse audio en direct.", "Vidéo, radio et paysage utilisent Levyra."),
    "de" to playerDeck("Player Deck", "Wähle deinen Player-Stil.", "Bloom", "Late Night", "Leuchtendes Cover, schlichte Steuerung.", "Schwebendes Canvas.", "Nur Cover.", "Material 3 Expressive.", "Live-Audiopuls.", "Video, Radio und Querformat nutzen Levyra."),
    "pt" to playerDeck("Player Deck", "Escolhe o estilo do leitor.", "Bloom", "Late Night", "Capa luminosa, controlos simples.", "Canvas flutuante.", "Só capa.", "Material 3 Expressive.", "Pulso de áudio ao vivo.", "Vídeo, rádio e horizontal usam Levyra."),
    "nl" to playerDeck("Player Deck", "Kies je playerstijl.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Zwevend Canvas.", "Alleen cover.", "Material 3 Expressive.", "Live audiopuls.", "Video, radio en liggend gebruiken Levyra."),
    "pl" to playerDeck("Player Deck", "Wybierz styl odtwarzacza.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Pływający Canvas.", "Tylko okładka.", "Material 3 Expressive.", "Puls audio na żywo.", "Wideo, radio i poziom używają Levyra."),
    "ro" to playerDeck("Player Deck", "Alege stilul playerului.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Canvas plutitor.", "Doar coperta.", "Material 3 Expressive.", "Puls audio live.", "Video, radio și peisaj folosesc Levyra."),
    "el" to playerDeck("Player Deck", "Διάλεξε στυλ αναπαραγωγής.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Αιωρούμενο Canvas.", "Μόνο εξώφυλλο.", "Material 3 Expressive.", "Ζωντανός παλμός ήχου.", "Βίντεο, ράδιο και οριζόντια προβολή χρησιμοποιούν Levyra."),
    "sv" to playerDeck("Player Deck", "Välj spelarstil.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Svävande Canvas.", "Endast omslag.", "Material 3 Expressive.", "Live ljudpuls.", "Video, radio och liggande använder Levyra."),
    "da" to playerDeck("Player Deck", "Vælg afspillerstil.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Svævende Canvas.", "Kun cover.", "Material 3 Expressive.", "Live lydpuls.", "Video, radio og liggende bruger Levyra."),
    "cs" to playerDeck("Player Deck", "Vyber styl přehrávače.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Plovoucí Canvas.", "Jen obal.", "Material 3 Expressive.", "Živý zvukový puls.", "Video, rádio a zobrazení na šířku používají Levyra."),
    "uk" to playerDeck("Player Deck", "Виберіть стиль плеєра.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Плаваючий Canvas.", "Лише обкладинка.", "Material 3 Expressive.", "Живий аудіопульс.", "Відео, радіо й альбомний режим використовують Levyra."),
    "ru" to playerDeck("Player Deck", "Выберите стиль плеера.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Плавающий Canvas.", "Только обложка.", "Material 3 Expressive.", "Живой аудиопульс.", "Видео, радио и альбомный режим используют Levyra."),
    "tr" to playerDeck("Player Deck", "Oynatıcı stilini seç.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Yüzen Canvas.", "Yalnızca kapak.", "Material 3 Expressive.", "Canlı ses darbesi.", "Video, radyo ve yatay görünüm Levyra kullanır."),
    "ar" to playerDeck("Player Deck", "اختر نمط المشغل.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Canvas عائم.", "الغلاف فقط.", "Material 3 Expressive.", "نبض صوتي مباشر.", "الفيديو والراديو والوضع الأفقي تستخدم Levyra."),
    "zh" to playerDeck("Player Deck", "选择播放器样式。", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "悬浮 Canvas。", "仅封面。", "Material 3 Expressive.", "实时音频脉冲。", "视频、电台和横屏使用 Levyra。"),
    "ja" to playerDeck("Player Deck", "プレイヤーのスタイルを選択。", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "フローティング Canvas。", "アートワークのみ。", "Material 3 Expressive.", "ライブ音声パルス。", "動画、ラジオ、横画面は Levyra を使用します。"),
    "ko" to playerDeck("Player Deck", "플레이어 스타일을 선택하세요.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "플로팅 Canvas.", "아트워크만.", "Material 3 Expressive.", "라이브 오디오 펄스.", "동영상, 라디오, 가로 화면은 Levyra를 사용합니다."),
    "hi" to playerDeck("Player Deck", "प्लेयर शैली चुनें।", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "फ्लोटिंग Canvas।", "केवल कवर।", "Material 3 Expressive.", "लाइव ऑडियो पल्स।", "वीडियो, रेडियो और लैंडस्केप Levyra का उपयोग करते हैं।"),
    "id" to playerDeck("Player Deck", "Pilih gaya pemutar.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Canvas mengambang.", "Sampul saja.", "Material 3 Expressive.", "Denyut audio langsung.", "Video, radio, dan lanskap memakai Levyra."),
    "vi" to playerDeck("Player Deck", "Chọn kiểu trình phát.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Canvas nổi.", "Chỉ ảnh bìa.", "Material 3 Expressive.", "Nhịp âm thanh trực tiếp.", "Video, radio và màn hình ngang dùng Levyra."),
    "th" to playerDeck("Player Deck", "เลือกสไตล์เครื่องเล่น", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Canvas แบบลอย", "เฉพาะปก", "Material 3 Expressive.", "พัลส์เสียงสด", "วิดีโอ วิทยุ และแนวนอนใช้ Levyra"),
    "fil" to playerDeck("Player Deck", "Piliin ang estilo ng player.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Floating Canvas.", "Cover lang.", "Material 3 Expressive.", "Live audio pulse.", "Video, radio at landscape ay gumagamit ng Levyra."),
    "he" to playerDeck("Player Deck", "בחר סגנון נגן.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Canvas צף.", "עטיפה בלבד.", "Material 3 Expressive.", "פולס שמע חי.", "וידאו, רדיו ותצוגה לרוחב משתמשים ב-Levyra."),
    "fi" to playerDeck("Player Deck", "Valitse soittimen tyyli.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Kelluva Canvas.", "Vain kansi.", "Material 3 Expressive.", "Live-äänipulssi.", "Video, radio ja vaakatila käyttävät Levyraa."),
    "et" to playerDeck("Player Deck", "Vali mängija stiil.", "Bloom", "Late Night", "Glowing artwork, quiet controls.", "Hõljuv Canvas.", "Ainult kaas.", "Material 3 Expressive.", "Reaalaja helipulss.", "Video, raadio ja rõhtvaade kasutavad Levyrat.")
)

private val playerDeckHaloHints: Map<String, String> = mapOf(
    "en" to "Floating artwork and toolbar.",
    "it" to "Copertina e comandi fluttuanti.",
    "es" to "Portada y controles flotantes.",
    "fr" to "Pochette et commandes flottantes.",
    "de" to "Schwebendes Cover und Steuerleiste.",
    "pt" to "Capa e controlos flutuantes."
)

private val playerDeckDefaultBadges: Map<String, String> = mapOf(
    "en" to "Default",
    "it" to "Predefinito",
    "es" to "Predeterminado",
    "fr" to "Par défaut",
    "de" to "Standard",
    "pt" to "Predefinido"
)

internal fun playerDeckLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playerDeckBundles, code) + mapOf(
        "playerDeckHalo" to "Liner Notes",
        "playerDeckHaloHint" to (playerDeckHaloHints[code] ?: playerDeckHaloHints.getValue("en")),
        "playerDeckDefaultBadge" to (playerDeckDefaultBadges[code] ?: playerDeckDefaultBadges.getValue("en"))
    )

internal fun playerDeckLocalizationCodes(): Set<String> = playerDeckBundles.keys
