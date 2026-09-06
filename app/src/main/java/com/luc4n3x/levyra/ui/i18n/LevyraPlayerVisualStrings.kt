package com.luc4n3x.levyra.ui.i18n

internal val playerVisualKeys = setOf(
    "playerVisualMode",
    "playerVisualModeSubtitle",
    "playerVisualModeArtwork",
    "playerVisualModeCanvasCard",
    "playerVisualModeCanvasImmersive",
    "playerBackground",
    "playerBackgroundSubtitle",
    "playerBackgroundDynamic",
    "playerBackgroundBlur",
    "playerBackgroundDark",
    "playerBackgroundPureBlack",
    "enterImmersive",
    "exitImmersive"
)

private fun playerVisual(
    visualMode: String,
    visualModeSubtitle: String,
    artwork: String,
    canvasCard: String,
    canvasImmersive: String,
    background: String,
    backgroundSubtitle: String,
    dynamic: String,
    blur: String,
    dark: String,
    pureBlack: String,
    enterImmersive: String,
    exitImmersive: String
): Map<String, String> = mapOf(
    "playerVisualMode" to visualMode,
    "playerVisualModeSubtitle" to visualModeSubtitle,
    "playerVisualModeArtwork" to artwork,
    "playerVisualModeCanvasCard" to canvasCard,
    "playerVisualModeCanvasImmersive" to canvasImmersive,
    "playerBackground" to background,
    "playerBackgroundSubtitle" to backgroundSubtitle,
    "playerBackgroundDynamic" to dynamic,
    "playerBackgroundBlur" to blur,
    "playerBackgroundDark" to dark,
    "playerBackgroundPureBlack" to pureBlack,
    "enterImmersive" to enterImmersive,
    "exitImmersive" to exitImmersive
)

private val playerVisualBundles: Map<String, Map<String, String>> = mapOf(
    "en" to playerVisual("Visual mode", "Choose how artwork and motion canvas appear", "Artwork", "Canvas card", "Canvas immersive", "Player background", "Background style behind the player", "Adaptive", "Blur", "Dark", "Pure black", "Enter immersive", "Exit immersive"),
    "it" to playerVisual("Modalità visiva", "Scegli come visualizzare copertina e Canvas dinamico", "Copertina", "Scheda Canvas", "Canvas immersivo", "Sfondo del player", "Stile dello sfondo dietro al player", "Adattivo", "Sfocato", "Scuro", "Nero puro", "Entra in immersivo", "Esci da immersivo"),
    "es" to playerVisual("Modo visual", "Elige cómo se muestran la portada y el Canvas en movimiento", "Portada", "Tarjeta Canvas", "Canvas inmersivo", "Fondo del reproductor", "Estilo de fondo detrás del reproductor", "Adaptativo", "Desenfoque", "Oscuro", "Negro puro", "Modo inmersivo", "Salir de inmersivo"),
    "fr" to playerVisual("Mode visuel", "Choisissez l'affichage de la pochette et du Canvas animé", "Pochette", "Carte Canvas", "Canvas immersif", "Arrière-plan du lecteur", "Style d'arrière-plan derrière le lecteur", "Adaptatif", "Flou", "Sombre", "Noir absolu", "Mode immersif", "Quitter le mode immersif"),
    "de" to playerVisual("Visueller Modus", "Wähle, wie Cover und bewegtes Canvas angezeigt werden", "Cover", "Canvas-Karte", "Immersives Canvas", "Player-Hintergrund", "Hintergrundstil hinter dem Player", "Adaptiv", "Weichzeichner", "Dunkel", "Reines Schwarz", "Immersiv starten", "Immersiv beenden"),
    "pt" to playerVisual("Modo visual", "Escolhe como a capa e o Canvas em movimento são apresentados", "Capa", "Cartão Canvas", "Canvas imersivo", "Fundo do leitor", "Estilo de fundo por trás do leitor", "Adaptativo", "Desfocado", "Escuro", "Preto puro", "Modo imersivo", "Sair do modo imersivo"),
    "nl" to playerVisual("Visuele modus", "Kies hoe hoes en bewegend Canvas worden weergegeven", "Hoes", "Canvas-kaart", "Meeslepend Canvas", "Speler-achtergrond", "Achtergrondstijl achter de speler", "Adaptief", "Vervaagd", "Donker", "Puur zwart", "Meeslepende modus openen", "Meeslepende modus verlaten"),
    "pl" to playerVisual("Tryb wizualny", "Wybierz, jak wyświetlać okładkę i animowane Canvas", "Okładka", "Karta Canvas", "Imersyjne Canvas", "Tło odtwarzacza", "Styl tła za odtwarzaczem", "Adaptacyjne", "Rozmycie", "Ciemne", "Czysta czerń", "Włącz tryb imersyjny", "Wyłącz tryb imersyjny"),
    "ro" to playerVisual("Mod vizual", "Alege cum se afișează coperta și Canvasul animat", "Copertă", "Card Canvas", "Canvas imersiv", "Fundal player", "Stilul de fundal din spatele playerului", "Adaptiv", "Estompat", "Întunecat", "Negru pur", "Mod imersiv", "Ieșire din mod imersiv"),
    "el" to playerVisual("Οπτική λειτουργία", "Επίλεξε πώς εμφανίζονται το εξώφυλλο και το κινούμενο Canvas", "Εξώφυλλο", "Κάρτα Canvas", "Καθηλωτικό Canvas", "Φόντο αναπαραγωγής", "Στυλ φόντου πίσω από το πρόγραμμα αναπαραγωγής", "Προσαρμοστικό", "Θόλωμα", "Σκούρο", "Καθαρό μαύρο", "Είσοδος σε καθηλωτικό", "Έξοδος από καθηλωτικό"),
    "sv" to playerVisual("Visuellt läge", "Välj hur omslag och rörlig Canvas visas", "Omslag", "Canvas-kort", "Fängslande Canvas", "Spelarbakgrund", "Bakgrundsstil bakom spelaren", "Adaptiv", "Oskärpa", "Mörk", "Rent svart", "Öppna helskärmsläge", "Avsluta helskärmsläge"),
    "da" to playerVisual("Visuel tilstand", "Vælg, hvordan cover og levende Canvas vises", "Cover", "Canvas-kort", "Immersiv Canvas", "Afspillerbaggrund", "Baggrundsstil bag afspilleren", "Adaptiv", "Sløring", "Mørk", "Rent sort", "Åbn immersiv tilstand", "Luk immersiv tilstand"),
    "cs" to playerVisual("Vizuální režim", "Zvol si, jak se zobrazuje obal a pohyblivý Canvas", "Obal", "Karta Canvas", "Imerzivní Canvas", "Pozadí přehrávače", "Styl pozadí za přehrávačem", "Adaptivní", "Rozostření", "Tmavé", "Čistá černá", "Spustit imerzivní režim", "Ukončit imerzivní režim"),
    "uk" to playerVisual("Візуальний режим", "Оберіть, як відображати обкладинку та динамічний Canvas", "Обкладинка", "Картка Canvas", "Імерсивний Canvas", "Фон плеєра", "Стиль фону позаду плеєра", "Адаптивний", "Розмиття", "Темний", "Чистий чорний", "Увійти в імерсивний режим", "Вийти з імерсивного режиму"),
    "ru" to playerVisual("Визуальный режим", "Выберите, как отображать обложку и динамический Canvas", "Обложка", "Карточка Canvas", "Иммерсивный Canvas", "Фон плеера", "Стиль фона за плеером", "Адаптивный", "Размытие", "Тёмный", "Чистый чёрный", "В иммерсивный режим", "Выйти из иммерсивного"),
    "tr" to playerVisual("Görsel mod", "Kapak ve hareketli Canvas görünümünü seç", "Kapak", "Canvas kartı", "Sürükleyici Canvas", "Oynatıcı arka planı", "Oynatıcı arkasındaki arka plan stili", "Uyarlanabilir", "Bulanık", "Koyu", "Saf siyah", "Sürükleyici moda geç", "Sürükleyici moddan çık"),
    "ar" to playerVisual("الوضع المرئي", "اختر كيفية عرض الغلاف ورسوم Canvas المتحركة", "الغلاف", "بطاقة Canvas", "Canvas غامر", "خلفية المشغل", "نمط الخلفية خلف المشغل", "تكيفي", "ضبابي", "داكن", "أسود نقي", "دخول الوضع الغامر", "خروج من الوضع الغامر"),
    "zh" to playerVisual("视觉模式", "选择封面与动态 Canvas 的呈现方式", "封面", "Canvas 卡片", "沉浸式 Canvas", "播放器背景", "播放器背后的背景样式", "自适应", "模糊", "深色", "纯黑", "进入沉浸模式", "退出沉浸模式"),
    "ja" to playerVisual("ビジュアルモード", "アートワークとモーション Canvas の表示方法を選択します", "アートワーク", "Canvas カード", "イマーシブ Canvas", "プレーヤーの背景", "プレーヤー背面の背景スタイル", "アダプティブ", "ブラー", "ダーク", "ピュアブラック", "イマーシブ表示", "イマーシブ終了"),
    "ko" to playerVisual("비주얼 모드", "아트워크와 모션 Canvas 표시 방식을 선택하세요", "아트워크", "Canvas 카드", "몰입형 Canvas", "플레이어 배경", "플레이어 뒤쪽 배경 스타일", "적응형", "블러", "다크", "퓨어 블랙", "몰입형 모드 시작", "몰입형 모드 종료"),
    "hi" to playerVisual("विज़ुअल मोड", "चुनें कि आर्टवर्क और मोशन Canvas कैसे दिखाई दें", "आर्टवर्क", "Canvas कार्ड", "इमर्सिव Canvas", "प्लेयर पृष्ठभूमि", "प्लेयर के पीछे पृष्ठभूमि शैली", "एडेप्टिव", "ब्लर", "डार्क", "प्योर ब्लैक", "इमर्सिव मोड शुरू करें", "इमर्सिव मोड से बाहर निकलें"),
    "id" to playerVisual("Mode visual", "Pilih cara tampilan artwork dan Canvas bergerak", "Artwork", "Kartu Canvas", "Canvas imersif", "Latar belakang pemutar", "Gaya latar belakang di belakang pemutar", "Adaptif", "Buram", "Gelap", "Hitam pekat", "Masuk mode imersif", "Keluar dari mode imersif"),
    "vi" to playerVisual("Chế độ hiển thị", "Chọn cách hiển thị ảnh bìa và Canvas động", "Ảnh bìa", "Thẻ Canvas", "Canvas đắm chìm", "Hình nền trình phát", "Kiểu hình nền phía sau trình phát", "Thích ứng", "Mờ nhòe", "Tối", "Đen thuần", "Vào chế độ đắm chìm", "Thoát chế độ đắm chìm"),
    "th" to playerVisual("โหมดภาพ", "เลือกวิธีแสดงภาพหน้าปกและ Canvas เคลื่อนไหว", "หน้าปก", "การ์ด Canvas", "Canvas แบบเต็มตา", "พื้นหลังโปรแกรมเล่น", "สไตล์พื้นหลังด้านหลังโปรแกรมเล่น", "ปรับเปลี่ยนตามเนื้อหา", "เบลอ", "มืด", "ดำสนิท", "เข้าสู่โหมดเต็มตา", "ออกจากโหมดเต็มตา"),
    "fil" to playerVisual("Visual mode", "Piliin kung paano ipapakita ang artwork at motion Canvas", "Artwork", "Canvas card", "Immersive Canvas", "Background ng player", "Estilo ng background sa likod ng player", "Adaptive", "Blur", "Madilim", "Purong itim", "Pumasok sa immersive", "Lumabas sa immersive"),
    "he" to playerVisual("מצב חזותי", "בחר כיצד יוצגו העטיפה וה-Canvas המונפש", "עטיפה", "כרטיס Canvas", "Canvas סוחף", "רקע הנגן", "סגנון הרקע מאחורי הנגן", "אדפטיבי", "טשטוש", "כהה", "שחור מוחלט", "כניסה למצב סוחף", "יציאה ממצב סוחף")
)

internal fun playerVisualLocalizationEntries(code: String): Map<String, String> =
    playerVisualBundles.getValue(code)

internal fun playerVisualLocalizationCodes(): Set<String> = playerVisualBundles.keys
