package com.luc4n3x.levyra.ui.i18n

internal val ambientModeKeys = setOf(
    "ambientLayout",
    "ambientLayoutSubtitle",
    "ambientModeMinimal",
    "ambientModeArtwork",
    "ambientModeSpotlight",
    "ambientModeLyrics",
    "ambientShowClock",
    "ambientShowClockSubtitle",
    "ambientShowTitle",
    "ambientShowTitleSubtitle",
    "ambientShowProgress",
    "ambientShowProgressSubtitle",
    "ambientAmoledBlack",
    "ambientAmoledBlackSubtitle"
)

private fun ambientMode(
    layout: String,
    layoutSubtitle: String,
    minimal: String,
    artwork: String,
    spotlight: String,
    lyrics: String,
    showClock: String,
    showClockSubtitle: String,
    showTitle: String,
    showTitleSubtitle: String,
    showProgress: String,
    showProgressSubtitle: String,
    amoledBlack: String,
    amoledBlackSubtitle: String
): Map<String, String> = mapOf(
    "ambientLayout" to layout,
    "ambientLayoutSubtitle" to layoutSubtitle,
    "ambientModeMinimal" to minimal,
    "ambientModeArtwork" to artwork,
    "ambientModeSpotlight" to spotlight,
    "ambientModeLyrics" to lyrics,
    "ambientShowClock" to showClock,
    "ambientShowClockSubtitle" to showClockSubtitle,
    "ambientShowTitle" to showTitle,
    "ambientShowTitleSubtitle" to showTitleSubtitle,
    "ambientShowProgress" to showProgress,
    "ambientShowProgressSubtitle" to showProgressSubtitle,
    "ambientAmoledBlack" to amoledBlack,
    "ambientAmoledBlackSubtitle" to amoledBlackSubtitle
)

private val ambientModeBundles: Map<String, Map<String, String>> = mapOf(
    "en" to ambientMode(
        "Standby layout", "Choose what the standby screen shows",
        "Minimal", "Artwork", "Spotlight", "Lyrics",
        "Show clock", "Large time above the content",
        "Show title and artist", "Track name and artist under the artwork",
        "Show progress", "Thin playback line",
        "True black", "Pure black background for OLED panels"
    ),
    "it" to ambientMode(
        "Layout standby", "Scegli cosa mostra la schermata standby",
        "Minimale", "Copertina", "Spotlight", "Testo",
        "Mostra orologio", "Ora in grande sopra il contenuto",
        "Mostra titolo e artista", "Titolo e artista sotto la copertina",
        "Mostra avanzamento", "Linea sottile di riproduzione",
        "Nero assoluto", "Sfondo nero puro per pannelli OLED"
    ),
    "es" to ambientMode(
        "Diseño en reposo", "Elige qué muestra la pantalla en reposo",
        "Minimal", "Portada", "Spotlight", "Letra",
        "Mostrar reloj", "Hora grande sobre el contenido",
        "Mostrar título y artista", "Título y artista bajo la portada",
        "Mostrar progreso", "Línea fina de reproducción",
        "Negro puro", "Fondo negro puro para paneles OLED"
    ),
    "fr" to ambientMode(
        "Mise en page veille", "Choisissez ce qu'affiche l'écran de veille",
        "Minimal", "Pochette", "Spotlight", "Paroles",
        "Afficher l'horloge", "Heure en grand au-dessus du contenu",
        "Afficher titre et artiste", "Titre et artiste sous la pochette",
        "Afficher la progression", "Fine ligne de lecture",
        "Noir absolu", "Fond noir pur pour les écrans OLED"
    ),
    "de" to ambientMode(
        "Standby-Layout", "Lege fest, was der Standby-Bildschirm zeigt",
        "Minimal", "Cover", "Spotlight", "Songtext",
        "Uhr anzeigen", "Große Uhrzeit über dem Inhalt",
        "Titel und Interpret anzeigen", "Titel und Interpret unter dem Cover",
        "Fortschritt anzeigen", "Dünne Wiedergabelinie",
        "Echtes Schwarz", "Reiner schwarzer Hintergrund für OLED-Displays"
    ),
    "pt" to ambientMode(
        "Esquema em espera", "Escolhe o que o ecrã em espera mostra",
        "Minimal", "Capa", "Spotlight", "Letra",
        "Mostrar relógio", "Hora grande acima do conteúdo",
        "Mostrar título e artista", "Título e artista sob a capa",
        "Mostrar progresso", "Linha fina de reprodução",
        "Preto absoluto", "Fundo preto puro para painéis OLED"
    ),
    "nl" to ambientMode(
        "Stand-by-indeling", "Kies wat het stand-byscherm toont",
        "Minimaal", "Hoes", "Spotlight", "Songtekst",
        "Klok tonen", "Grote tijd boven de inhoud",
        "Titel en artiest tonen", "Titel en artiest onder de hoes",
        "Voortgang tonen", "Dunne afspeellijn",
        "Echt zwart", "Puur zwarte achtergrond voor OLED-schermen"
    ),
    "pl" to ambientMode(
        "Układ czuwania", "Wybierz, co pokazuje ekran czuwania",
        "Minimalny", "Okładka", "Spotlight", "Tekst",
        "Pokaż zegar", "Duża godzina nad treścią",
        "Pokaż tytuł i wykonawcę", "Tytuł i wykonawca pod okładką",
        "Pokaż postęp", "Cienka linia odtwarzania",
        "Czysta czerń", "Czysto czarne tło dla paneli OLED"
    ),
    "ro" to ambientMode(
        "Aspect standby", "Alege ce afișează ecranul standby",
        "Minimal", "Copertă", "Spotlight", "Versuri",
        "Arată ceasul", "Ora mare deasupra conținutului",
        "Arată titlul și artistul", "Titlu și artist sub copertă",
        "Arată progresul", "Linie subțire de redare",
        "Negru absolut", "Fundal negru pur pentru panouri OLED"
    ),
    "el" to ambientMode(
        "Διάταξη αναμονής", "Επίλεξε τι δείχνει η οθόνη αναμονής",
        "Ελάχιστη", "Εξώφυλλο", "Spotlight", "Στίχοι",
        "Εμφάνιση ρολογιού", "Μεγάλη ώρα πάνω από το περιεχόμενο",
        "Εμφάνιση τίτλου και καλλιτέχνη", "Τίτλος και καλλιτέχνης κάτω από το εξώφυλλο",
        "Εμφάνιση προόδου", "Λεπτή γραμμή αναπαραγωγής",
        "Απόλυτο μαύρο", "Καθαρό μαύρο φόντο για οθόνες OLED"
    ),
    "sv" to ambientMode(
        "Vilolayout", "Välj vad viloskärmen visar",
        "Minimal", "Omslag", "Spotlight", "Låttext",
        "Visa klocka", "Stor tid ovanför innehållet",
        "Visa titel och artist", "Titel och artist under omslaget",
        "Visa förlopp", "Tunn uppspelningslinje",
        "Äkta svart", "Helsvart bakgrund för OLED-skärmar"
    ),
    "da" to ambientMode(
        "Standby-layout", "Vælg hvad standbyskærmen viser",
        "Minimal", "Cover", "Spotlight", "Sangtekst",
        "Vis ur", "Stort klokkeslæt over indholdet",
        "Vis titel og kunstner", "Titel og kunstner under coveret",
        "Vis forløb", "Tynd afspilningslinje",
        "Ægte sort", "Helt sort baggrund til OLED-skærme"
    ),
    "cs" to ambientMode(
        "Rozvržení pohotovosti", "Vyber, co pohotovostní obrazovka zobrazí",
        "Minimální", "Obal", "Spotlight", "Text",
        "Zobrazit hodiny", "Velký čas nad obsahem",
        "Zobrazit název a interpreta", "Název a interpret pod obalem",
        "Zobrazit průběh", "Tenká linka přehrávání",
        "Čistá černá", "Čistě černé pozadí pro OLED panely"
    ),
    "uk" to ambientMode(
        "Макет очікування", "Обери, що показує екран очікування",
        "Мінімальний", "Обкладинка", "Spotlight", "Текст",
        "Показувати годинник", "Великий час над вмістом",
        "Показувати назву й виконавця", "Назва та виконавець під обкладинкою",
        "Показувати прогрес", "Тонка лінія відтворення",
        "Справжній чорний", "Чисто чорний фон для панелей OLED"
    ),
    "ru" to ambientMode(
        "Макет ожидания", "Выберите, что показывает экран ожидания",
        "Минимальный", "Обложка", "Spotlight", "Текст",
        "Показывать часы", "Крупное время над содержимым",
        "Показывать название и исполнителя", "Название и исполнитель под обложкой",
        "Показывать прогресс", "Тонкая линия воспроизведения",
        "Истинно чёрный", "Чисто чёрный фон для OLED-панелей"
    ),
    "tr" to ambientMode(
        "Bekleme düzeni", "Bekleme ekranının ne göstereceğini seç",
        "Minimal", "Kapak", "Spotlight", "Şarkı sözü",
        "Saati göster", "İçeriğin üstünde büyük saat",
        "Başlık ve sanatçıyı göster", "Kapağın altında başlık ve sanatçı",
        "İlerlemeyi göster", "İnce oynatma çizgisi",
        "Gerçek siyah", "OLED paneller için saf siyah arka plan"
    ),
    "ar" to ambientMode(
        "تخطيط وضع الاستعداد", "اختر ما تعرضه شاشة الاستعداد",
        "بسيط", "الغلاف", "Spotlight", "الكلمات",
        "إظهار الساعة", "وقت كبير أعلى المحتوى",
        "إظهار العنوان والفنان", "العنوان والفنان أسفل الغلاف",
        "إظهار التقدم", "خط رفيع للتشغيل",
        "أسود حقيقي", "خلفية سوداء نقية لشاشات OLED"
    ),
    "zh" to ambientMode(
        "待机布局", "选择待机屏幕显示的内容",
        "极简", "封面", "聚光", "歌词",
        "显示时钟", "内容上方的大号时间",
        "显示标题和艺人", "封面下方的歌曲与艺人",
        "显示进度", "纤细的播放进度线",
        "纯黑", "为 OLED 屏幕提供纯黑背景"
    ),
    "ja" to ambientMode(
        "スタンバイ表示", "スタンバイ画面に表示する内容を選びます",
        "ミニマル", "アートワーク", "スポットライト", "歌詞",
        "時計を表示", "コンテンツ上部の大きな時刻",
        "タイトルとアーティストを表示", "アートワークの下に曲名とアーティスト",
        "進捗を表示", "細い再生ライン",
        "真の黒", "OLED パネル向けの純黒背景"
    ),
    "ko" to ambientMode(
        "대기 레이아웃", "대기 화면에 표시할 내용을 선택하세요",
        "미니멀", "아트워크", "스포트라이트", "가사",
        "시계 표시", "콘텐츠 위의 큰 시각",
        "제목과 아티스트 표시", "아트워크 아래의 곡명과 아티스트",
        "진행률 표시", "얇은 재생 선",
        "순수 검정", "OLED 패널을 위한 완전한 검정 배경"
    ),
    "hi" to ambientMode(
        "स्टैंडबाय लेआउट", "चुनें कि स्टैंडबाय स्क्रीन क्या दिखाए",
        "मिनिमल", "आर्टवर्क", "स्पॉटलाइट", "बोल",
        "घड़ी दिखाएं", "सामग्री के ऊपर बड़ा समय",
        "शीर्षक और कलाकार दिखाएं", "आर्टवर्क के नीचे ट्रैक और कलाकार",
        "प्रगति दिखाएं", "पतली प्लेबैक लाइन",
        "शुद्ध काला", "OLED पैनल के लिए पूर्ण काली पृष्ठभूमि"
    ),
    "id" to ambientMode(
        "Tata letak siaga", "Pilih apa yang ditampilkan layar siaga",
        "Minimal", "Sampul", "Spotlight", "Lirik",
        "Tampilkan jam", "Waktu besar di atas konten",
        "Tampilkan judul dan artis", "Judul dan artis di bawah sampul",
        "Tampilkan progres", "Garis pemutaran tipis",
        "Hitam murni", "Latar hitam murni untuk panel OLED"
    ),
    "vi" to ambientMode(
        "Bố cục chờ", "Chọn nội dung màn hình chờ hiển thị",
        "Tối giản", "Ảnh bìa", "Spotlight", "Lời bài hát",
        "Hiện đồng hồ", "Giờ lớn phía trên nội dung",
        "Hiện tên bài và nghệ sĩ", "Tên bài và nghệ sĩ dưới ảnh bìa",
        "Hiện tiến trình", "Đường phát mỏng",
        "Đen tuyệt đối", "Nền đen thuần cho màn hình OLED"
    ),
    "th" to ambientMode(
        "เลย์เอาต์สแตนด์บาย", "เลือกสิ่งที่หน้าจอสแตนด์บายแสดง",
        "เรียบง่าย", "ปกเพลง", "สปอตไลต์", "เนื้อเพลง",
        "แสดงนาฬิกา", "เวลาขนาดใหญ่เหนือเนื้อหา",
        "แสดงชื่อเพลงและศิลปิน", "ชื่อเพลงและศิลปินใต้ปก",
        "แสดงความคืบหน้า", "เส้นเล่นเพลงบาง",
        "ดำสนิท", "พื้นหลังดำสนิทสำหรับจอ OLED"
    ),
    "fil" to ambientMode(
        "Layout ng standby", "Piliin ang ipapakita ng standby screen",
        "Minimal", "Artwork", "Spotlight", "Lyrics",
        "Ipakita ang orasan", "Malaking oras sa itaas ng nilalaman",
        "Ipakita ang pamagat at artist", "Pamagat at artist sa ilalim ng artwork",
        "Ipakita ang progreso", "Manipis na linya ng pag-playback",
        "Tunay na itim", "Purong itim na background para sa OLED"
    ),
    "he" to ambientMode(
        "פריסת המתנה", "בחר מה יוצג במסך ההמתנה",
        "מינימלי", "עטיפה", "Spotlight", "מילים",
        "הצגת שעון", "שעה גדולה מעל התוכן",
        "הצגת שם ואמן", "שם השיר והאמן מתחת לעטיפה",
        "הצגת התקדמות", "פס נגינה דק",
        "שחור מלא", "רקע שחור מלא למסכי OLED"
    )
)

internal fun ambientModeLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(ambientModeBundles, code)

internal fun ambientModeLocalizationCodes(): Set<String> = ambientModeBundles.keys
