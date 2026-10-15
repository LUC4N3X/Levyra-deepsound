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
    "playerBackgroundAurora",
    "playerBackgroundGradient",
    "enterImmersive",
    "exitImmersive"
) + liveChatKeys + diagnosticsKeys + playerColorKeys

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
    "en" to playerVisual("Visual mode", "Choose how artwork and motion canvas appear", "Gallery", "Studio", "Velvet", "Player background", "Background style behind the player", "Adaptive", "Blur", "Dark", "Pure black", "Enter immersive", "Exit immersive"),
    "it" to playerVisual("Modalità visiva", "Scegli come visualizzare copertina e Canvas dinamico", "Gallery", "Studio", "Velvet", "Sfondo del player", "Stile dello sfondo dietro al player", "Adattivo", "Sfocato", "Scuro", "Nero puro", "Entra in immersivo", "Esci da immersivo"),
    "es" to playerVisual("Modo visual", "Elige cómo se muestran la portada y el Canvas en movimiento", "Gallery", "Studio", "Velvet", "Fondo del reproductor", "Estilo de fondo detrás del reproductor", "Adaptativo", "Desenfoque", "Oscuro", "Negro puro", "Modo inmersivo", "Salir de inmersivo"),
    "fr" to playerVisual("Mode visuel", "Choisissez l'affichage de la pochette et du Canvas animé", "Gallery", "Studio", "Velvet", "Arrière-plan du lecteur", "Style d'arrière-plan derrière le lecteur", "Adaptatif", "Flou", "Sombre", "Noir absolu", "Mode immersif", "Quitter le mode immersif"),
    "de" to playerVisual("Visueller Modus", "Wähle, wie Cover und bewegtes Canvas angezeigt werden", "Gallery", "Studio", "Velvet", "Player-Hintergrund", "Hintergrundstil hinter dem Player", "Adaptiv", "Weichzeichner", "Dunkel", "Reines Schwarz", "Immersiv starten", "Immersiv beenden"),
    "pt" to playerVisual("Modo visual", "Escolhe como a capa e o Canvas em movimento são apresentados", "Gallery", "Studio", "Velvet", "Fundo do leitor", "Estilo de fundo por trás do leitor", "Adaptativo", "Desfocado", "Escuro", "Preto puro", "Modo imersivo", "Sair do modo imersivo"),
    "nl" to playerVisual("Visuele modus", "Kies hoe hoes en bewegend Canvas worden weergegeven", "Gallery", "Studio", "Velvet", "Speler-achtergrond", "Achtergrondstijl achter de speler", "Adaptief", "Vervaagd", "Donker", "Puur zwart", "Meeslepende modus openen", "Meeslepende modus verlaten"),
    "pl" to playerVisual("Tryb wizualny", "Wybierz, jak wyświetlać okładkę i animowane Canvas", "Gallery", "Studio", "Velvet", "Tło odtwarzacza", "Styl tła za odtwarzaczem", "Adaptacyjne", "Rozmycie", "Ciemne", "Czysta czerń", "Włącz tryb imersyjny", "Wyłącz tryb imersyjny"),
    "ro" to playerVisual("Mod vizual", "Alege cum se afișează coperta și Canvasul animat", "Gallery", "Studio", "Velvet", "Fundal player", "Stilul de fundal din spatele playerului", "Adaptiv", "Estompat", "Întunecat", "Negru pur", "Mod imersiv", "Ieșire din mod imersiv"),
    "el" to playerVisual("Οπτική λειτουργία", "Επίλεξε πώς εμφανίζονται το εξώφυλλο και το κινούμενο Canvas", "Gallery", "Studio", "Velvet", "Φόντο αναπαραγωγής", "Στυλ φόντου πίσω από το πρόγραμμα αναπαραγωγής", "Προσαρμοστικό", "Θόλωμα", "Σκούρο", "Καθαρό μαύρο", "Είσοδος σε καθηλωτικό", "Έξοδος από καθηλωτικό"),
    "sv" to playerVisual("Visuellt läge", "Välj hur omslag och rörlig Canvas visas", "Gallery", "Studio", "Velvet", "Spelarbakgrund", "Bakgrundsstil bakom spelaren", "Adaptiv", "Oskärpa", "Mörk", "Rent svart", "Öppna helskärmsläge", "Avsluta helskärmsläge"),
    "da" to playerVisual("Visuel tilstand", "Vælg, hvordan cover og levende Canvas vises", "Gallery", "Studio", "Velvet", "Afspillerbaggrund", "Baggrundsstil bag afspilleren", "Adaptiv", "Sløring", "Mørk", "Rent sort", "Åbn immersiv tilstand", "Luk immersiv tilstand"),
    "cs" to playerVisual("Vizuální režim", "Zvol si, jak se zobrazuje obal a pohyblivý Canvas", "Gallery", "Studio", "Velvet", "Pozadí přehrávače", "Styl pozadí za přehrávačem", "Adaptivní", "Rozostření", "Tmavé", "Čistá černá", "Spustit imerzivní režim", "Ukončit imerzivní režim"),
    "uk" to playerVisual("Візуальний режим", "Оберіть, як відображати обкладинку та динамічний Canvas", "Gallery", "Studio", "Velvet", "Фон плеєра", "Стиль фону позаду плеєра", "Адаптивний", "Розмиття", "Темний", "Чистий чорний", "Увійти в імерсивний режим", "Вийти з імерсивного режиму"),
    "ru" to playerVisual("Визуальный режим", "Выберите, как отображать обложку и динамический Canvas", "Gallery", "Studio", "Velvet", "Фон плеера", "Стиль фона за плеером", "Адаптивный", "Размытие", "Тёмный", "Чистый чёрный", "В иммерсивный режим", "Выйти из иммерсивного"),
    "tr" to playerVisual("Görsel mod", "Kapak ve hareketli Canvas görünümünü seç", "Gallery", "Studio", "Velvet", "Oynatıcı arka planı", "Oynatıcı arkasındaki arka plan stili", "Uyarlanabilir", "Bulanık", "Koyu", "Saf siyah", "Sürükleyici moda geç", "Sürükleyici moddan çık"),
    "ar" to playerVisual("الوضع المرئي", "اختر كيفية عرض الغلاف ورسوم Canvas المتحركة", "Gallery", "Studio", "Velvet", "خلفية المشغل", "نمط الخلفية خلف المشغل", "تكيفي", "ضبابي", "داكن", "أسود نقي", "دخول الوضع الغامر", "خروج من الوضع الغامر"),
    "zh" to playerVisual("视觉模式", "选择封面与动态 Canvas 的呈现方式", "Gallery", "Studio", "Velvet", "播放器背景", "播放器背后的背景样式", "自适应", "模糊", "深色", "纯黑", "进入沉浸模式", "退出沉浸模式"),
    "ja" to playerVisual("ビジュアルモード", "アートワークとモーション Canvas の表示方法を選択します", "Gallery", "Studio", "Velvet", "プレーヤーの背景", "プレーヤー背面の背景スタイル", "アダプティブ", "ブラー", "ダーク", "ピュアブラック", "イマーシブ表示", "イマーシブ終了"),
    "ko" to playerVisual("비주얼 모드", "아트워크와 모션 Canvas 표시 방식을 선택하세요", "Gallery", "Studio", "Velvet", "플레이어 배경", "플레이어 뒤쪽 배경 스타일", "적응형", "블러", "다크", "퓨어 블랙", "몰입형 모드 시작", "몰입형 모드 종료"),
    "hi" to playerVisual("विज़ुअल मोड", "चुनें कि आर्टवर्क और मोशन Canvas कैसे दिखाई दें", "Gallery", "Studio", "Velvet", "प्लेयर पृष्ठभूमि", "प्लेयर के पीछे पृष्ठभूमि शैली", "एडेप्टिव", "ब्लर", "डार्क", "प्योर ब्लैक", "इमर्सिव मोड शुरू करें", "इमर्सिव मोड से बाहर निकलें"),
    "id" to playerVisual("Mode visual", "Pilih cara tampilan artwork dan Canvas bergerak", "Gallery", "Studio", "Velvet", "Latar belakang pemutar", "Gaya latar belakang di belakang pemutar", "Adaptif", "Buram", "Gelap", "Hitam pekat", "Masuk mode imersif", "Keluar dari mode imersif"),
    "vi" to playerVisual("Chế độ hiển thị", "Chọn cách hiển thị ảnh bìa và Canvas động", "Gallery", "Studio", "Velvet", "Hình nền trình phát", "Kiểu hình nền phía sau trình phát", "Thích ứng", "Mờ nhòe", "Tối", "Đen thuần", "Vào chế độ đắm chìm", "Thoát chế độ đắm chìm"),
    "th" to playerVisual("โหมดภาพ", "เลือกวิธีแสดงภาพหน้าปกและ Canvas เคลื่อนไหว", "Gallery", "Studio", "Velvet", "พื้นหลังโปรแกรมเล่น", "สไตล์พื้นหลังด้านหลังโปรแกรมเล่น", "ปรับเปลี่ยนตามเนื้อหา", "เบลอ", "มืด", "ดำสนิท", "เข้าสู่โหมดเต็มตา", "ออกจากโหมดเต็มตา"),
    "fil" to playerVisual("Visual mode", "Piliin kung paano ipapakita ang artwork at motion Canvas", "Gallery", "Studio", "Velvet", "Background ng player", "Estilo ng background sa likod ng player", "Adaptive", "Blur", "Madilim", "Purong itim", "Pumasok sa immersive", "Lumabas sa immersive"),
    "he" to playerVisual("מצב חזותי", "בחר כיצד יוצגו העטיפה וה-Canvas המונפש", "Gallery", "Studio", "Velvet", "רקע הנגן", "סגנון הרקע מאחורי הנגן", "אדפטיבי", "טשטוש", "כהה", "שחור מוחלט", "כניסה למצב סוחף", "יציאה ממצב סוחף"),
    "fi" to playerVisual("Visuaalinen tila", "Valitse, miten kansikuva ja liikkuva Canvas näkyvät", "Gallery", "Studio", "Velvet", "Soittimen tausta", "Taustatyyli soittimen takana", "Mukautuva", "Sumeus", "Tumma", "Puhdas musta", "Siirry immersiiviseen tilaan", "Poistu immersiivisestä tilasta"),
    "et" to playerVisual("Visuaalne režiim", "Vali, kuidas kaanepilt ja liikuv Canvas ilmuvad", "Gallery", "Studio", "Velvet", "Mängija taust", "Taustastiil mängija taga", "Kohanduv", "Hägu", "Tume", "Puhas must", "Ava kaasahaarav režiim", "Välju kaasahaaravast režiimist")
)

private val playerBackgroundExtraLabels: Map<String, Pair<String, String>> = mapOf(
    "en" to ("Aurora" to "Gradient"),
    "it" to ("Aurora" to "Sfumato"),
    "es" to ("Aurora" to "Degradado"),
    "fr" to ("Aurore" to "Dégradé"),
    "de" to ("Polarlicht" to "Verlauf"),
    "pt" to ("Aurora" to "Gradiente"),
    "nl" to ("Poollicht" to "Kleurverloop"),
    "pl" to ("Zorza polarna" to "Gradient"),
    "ro" to ("Auroră" to "Degradeu"),
    "el" to ("Σέλας" to "Διαβάθμιση"),
    "sv" to ("Norrsken" to "Färgtoning"),
    "da" to ("Nordlys" to "Farveforløb"),
    "cs" to ("Polární záře" to "Přechod"),
    "uk" to ("Полярне сяйво" to "Градієнт"),
    "ru" to ("Северное сияние" to "Градиент"),
    "tr" to ("Kutup ışıkları" to "Gradyan"),
    "ar" to ("الشفق القطبي" to "تدرج لوني"),
    "zh" to ("极光" to "渐变"),
    "ja" to ("オーロラ" to "グラデーション"),
    "ko" to ("오로라" to "그라데이션"),
    "hi" to ("ध्रुवीय ज्योति" to "ग्रेडिएंट"),
    "id" to ("Aurora" to "Gradasi"),
    "vi" to ("Cực quang" to "Chuyển sắc"),
    "th" to ("แสงออโรรา" to "ไล่ระดับสี"),
    "fil" to ("Liwanag ng aurora" to "Paglipat ng kulay"),
    "he" to ("זוהר הקוטב" to "מעבר צבעים"),
    "fi" to ("Revontulet" to "Liukuväri"),
    "et" to ("Virmalised" to "Värviüleminek")
)

internal fun playerVisualLocalizationEntries(code: String): Map<String, String> {
    val extra = playerBackgroundExtraLabels[code] ?: playerBackgroundExtraLabels.getValue("en")
    return localizedBundleOrEnglish(playerVisualBundles, code) + liveChatLocalizationEntries(code) +
        diagnosticsLocalizationEntries(code) + playerColorLocalizationEntries(code) + mapOf(
            "playerBackgroundAurora" to extra.first,
            "playerBackgroundGradient" to extra.second
        )
}

internal fun playerVisualLocalizationCodes(): Set<String> = supportedLocalizationCodes()
