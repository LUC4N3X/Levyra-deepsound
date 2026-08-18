package com.luc4n3x.levyra.ui.i18n

private fun canvasStrings(
    quality: String,
    qualitySubtitle: String,
    source: String,
    sourceSubtitle: String,
    community: String,
    sharePlaylist: String,
    enhanceVideoMetadata: String,
    enhanceVideoMetadataSubtitle: String,
    recognizeMusic: String
): Map<String, String> = mapOf(
    "canvasQuality" to quality,
    "canvasQualitySubtitle" to qualitySubtitle,
    "canvasSource" to source,
    "canvasSourceSubtitle" to sourceSubtitle,
    "canvasSourceCommunity" to community,
    "sharePlaylist" to sharePlaylist,
    "enhanceVideoMetadata" to enhanceVideoMetadata,
    "enhanceVideoMetadataSubtitle" to enhanceVideoMetadataSubtitle,
    "recognizeMusic" to recognizeMusic
)

private val canvasBundles: Map<String, Map<String, String>> = mapOf(
    "en" to canvasStrings("Canvas quality", "Auto follows network, battery and screen; High keeps the sharpest canvas", "Canvas source", "Choose which provider supplies motion artwork", "Community", "Share Levyra playlist", "Better video titles", "Use community titles and thumbnails on videos only", "Recognize music"),
    "it" to canvasStrings("Qualità Canvas", "Auto si adatta a rete, batteria e schermo; Alta mantiene il Canvas più nitido", "Sorgente Canvas", "Scegli quale provider fornisce il motion artwork", "Community", "Condividi playlist Levyra", "Titoli video migliori", "Usa titoli e miniature della community solo sui video", "Riconosci musica"),
    "es" to canvasStrings("Calidad del Canvas", "Auto se adapta a la red, la batería y la pantalla; Alta mantiene el Canvas más nítido", "Fuente del Canvas", "Elige qué proveedor suministra el motion artwork", "Comunidad", "Compartir playlist de Levyra", "Mejores títulos de vídeo", "Usa títulos y miniaturas de la comunidad solo en vídeos", "Reconocer música"),
    "fr" to canvasStrings("Qualité du Canvas", "Auto s'adapte au réseau, à la batterie et à l'écran ; Élevée garde le Canvas le plus net", "Source du Canvas", "Choisissez le fournisseur du motion artwork", "Communauté", "Partager la playlist Levyra", "Meilleurs titres vidéo", "Utilise les titres et miniatures de la communauté, vidéos uniquement", "Reconnaître la musique"),
    "de" to canvasStrings("Canvas-Qualität", "Auto passt sich Netz, Akku und Display an; Hoch behält das schärfste Canvas", "Canvas-Quelle", "Wähle den Anbieter für Motion Artwork", "Community", "Levyra-Playlist teilen", "Bessere Videotitel", "Community-Titel und -Vorschaubilder nur für Videos verwenden", "Musik erkennen"),
    "pt" to canvasStrings("Qualidade do Canvas", "Auto adapta-se à rede, bateria e ecrã; Alta mantém o Canvas mais nítido", "Fonte do Canvas", "Escolhe o fornecedor do motion artwork", "Comunidade", "Partilhar playlist Levyra", "Melhores títulos de vídeo", "Usa títulos e miniaturas da comunidade apenas em vídeos", "Reconhecer música"),
    "nl" to canvasStrings("Canvas-kwaliteit", "Auto past zich aan netwerk, batterij en scherm aan; Hoog houdt het scherpste canvas", "Canvas-bron", "Kies welke aanbieder motion artwork levert", "Community", "Levyra-afspeellijst delen", "Betere videotitels", "Gebruik community-titels en -thumbnails alleen bij video's", "Muziek herkennen"),
    "pl" to canvasStrings("Jakość Canvas", "Auto dopasowuje się do sieci, baterii i ekranu; Wysoka zachowuje najostrzejszy Canvas", "Źródło Canvas", "Wybierz dostawcę motion artwork", "Społeczność", "Udostępnij playlistę Levyra", "Lepsze tytuły filmów", "Używaj tytułów i miniatur społeczności tylko w filmach", "Rozpoznaj muzykę"),
    "ro" to canvasStrings("Calitate Canvas", "Auto se adaptează la rețea, baterie și ecran; Înaltă păstrează cel mai clar Canvas", "Sursă Canvas", "Alege furnizorul de motion artwork", "Comunitate", "Distribuie playlistul Levyra", "Titluri video mai bune", "Folosește titluri și miniaturi din comunitate doar la videoclipuri", "Recunoaște muzica"),
    "el" to canvasStrings("Ποιότητα Canvas", "Το Auto προσαρμόζεται σε δίκτυο, μπαταρία και οθόνη· η Υψηλή διατηρεί το πιο καθαρό Canvas", "Πηγή Canvas", "Επίλεξε ποιος πάροχος δίνει το motion artwork", "Κοινότητα", "Κοινοποίηση λίστας Levyra", "Καλύτεροι τίτλοι βίντεο", "Τίτλοι και μικρογραφίες της κοινότητας μόνο σε βίντεο", "Αναγνώριση μουσικής"),
    "sv" to canvasStrings("Canvas-kvalitet", "Auto anpassar sig till nätverk, batteri och skärm; Hög behåller den skarpaste canvasen", "Canvas-källa", "Välj vilken leverantör som ger motion artwork", "Community", "Dela Levyra-spellista", "Bättre videotitlar", "Använd community-titlar och miniatyrer endast för videor", "Känn igen musik"),
    "da" to canvasStrings("Canvas-kvalitet", "Auto tilpasser sig netværk, batteri og skærm; Høj bevarer det skarpeste canvas", "Canvas-kilde", "Vælg hvilken udbyder der leverer motion artwork", "Community", "Del Levyra-playliste", "Bedre videotitler", "Brug community-titler og miniaturer kun til videoer", "Genkend musik"),
    "cs" to canvasStrings("Kvalita Canvasu", "Auto se přizpůsobí síti, baterii a displeji; Vysoká zachová nejostřejší Canvas", "Zdroj Canvasu", "Vyber poskytovatele motion artworku", "Komunita", "Sdílet playlist Levyra", "Lepší názvy videí", "Používat komunitní názvy a náhledy jen u videí", "Rozpoznat hudbu"),
    "uk" to canvasStrings("Якість Canvas", "Авто враховує мережу, батарею та екран; Висока зберігає найчіткіший Canvas", "Джерело Canvas", "Обери постачальника motion artwork", "Спільнота", "Поділитися плейлистом Levyra", "Кращі назви відео", "Назви та мініатюри спільноти лише для відео", "Розпізнати музику"),
    "ru" to canvasStrings("Качество Canvas", "Авто учитывает сеть, батарею и экран; Высокое сохраняет самый чёткий Canvas", "Источник Canvas", "Выбери поставщика motion artwork", "Сообщество", "Поделиться плейлистом Levyra", "Лучшие названия видео", "Названия и миниатюры сообщества только для видео", "Распознать музыку"),
    "tr" to canvasStrings("Canvas kalitesi", "Oto; ağa, pile ve ekrana uyum sağlar, Yüksek en net Canvas'ı korur", "Canvas kaynağı", "Motion artwork'ü hangi sağlayıcının vereceğini seç", "Topluluk", "Levyra listesini paylaş", "Daha iyi video başlıkları", "Topluluk başlık ve küçük resimlerini yalnızca videolarda kullan", "Müziği tanı"),
    "ar" to canvasStrings("جودة Canvas", "يتكيف الوضع التلقائي مع الشبكة والبطارية والشاشة، بينما تحافظ الجودة العالية على أوضح Canvas", "مصدر Canvas", "اختر المزوّد الذي يوفّر الرسوم المتحركة", "المجتمع", "مشاركة قائمة Levyra", "عناوين فيديو أفضل", "استخدم عناوين ومصغّرات المجتمع للفيديو فقط", "التعرف على الموسيقى"),
    "zh" to canvasStrings("Canvas 画质", "自动模式会根据网络、电量和屏幕调整；高画质保留最清晰的 Canvas", "Canvas 来源", "选择提供动态封面的来源", "社区", "分享 Levyra 播放列表", "更好的视频标题", "仅对视频使用社区标题和缩略图", "识别音乐"),
    "ja" to canvasStrings("Canvas の画質", "自動は回線・バッテリー・画面に合わせて調整し、高画質は最も鮮明な Canvas を保ちます", "Canvas のソース", "モーションアートワークの提供元を選択します", "コミュニティ", "Levyra のプレイリストを共有", "より良い動画タイトル", "コミュニティのタイトルとサムネイルを動画にのみ使用します", "音楽を認識"),
    "ko" to canvasStrings("Canvas 화질", "자동은 네트워크·배터리·화면에 맞추고, 높음은 가장 선명한 Canvas를 유지합니다", "Canvas 소스", "모션 아트워크를 제공할 소스를 선택하세요", "커뮤니티", "Levyra 재생목록 공유", "더 나은 동영상 제목", "커뮤니티 제목과 미리보기를 동영상에만 사용합니다", "음악 인식"),
    "hi" to canvasStrings("Canvas गुणवत्ता", "ऑटो नेटवर्क, बैटरी और स्क्रीन के अनुसार बदलता है; उच्च सबसे स्पष्ट Canvas रखता है", "Canvas स्रोत", "चुनें कि मोशन आर्टवर्क कौन-सा प्रदाता दे", "समुदाय", "Levyra प्लेलिस्ट साझा करें", "बेहतर वीडियो शीर्षक", "समुदाय के शीर्षक और थंबनेल केवल वीडियो पर उपयोग करें", "संगीत पहचानें"),
    "id" to canvasStrings("Kualitas Canvas", "Auto menyesuaikan jaringan, baterai, dan layar; Tinggi menjaga Canvas paling tajam", "Sumber Canvas", "Pilih penyedia motion artwork", "Komunitas", "Bagikan playlist Levyra", "Judul video lebih baik", "Gunakan judul dan thumbnail komunitas hanya untuk video", "Kenali musik"),
    "vi" to canvasStrings("Chất lượng Canvas", "Tự động thích ứng với mạng, pin và màn hình; Cao giữ Canvas sắc nét nhất", "Nguồn Canvas", "Chọn nhà cung cấp motion artwork", "Cộng đồng", "Chia sẻ playlist Levyra", "Tiêu đề video tốt hơn", "Chỉ dùng tiêu đề và hình thu nhỏ của cộng đồng cho video", "Nhận diện nhạc"),
    "th" to canvasStrings("คุณภาพ Canvas", "อัตโนมัติจะปรับตามเครือข่าย แบตเตอรี่ และหน้าจอ ส่วนสูงจะคง Canvas ที่คมชัดที่สุด", "แหล่ง Canvas", "เลือกผู้ให้บริการภาพเคลื่อนไหว", "ชุมชน", "แชร์เพลย์ลิสต์ Levyra", "ชื่อวิดีโอที่ดีขึ้น", "ใช้ชื่อและภาพขนาดย่อจากชุมชนเฉพาะกับวิดีโอ", "จดจำเพลง"),
    "fil" to canvasStrings("Kalidad ng Canvas", "Ang Auto ay umaangkop sa network, baterya at screen; ang Mataas ay pinapanatili ang pinakamalinaw na Canvas", "Pinagmulan ng Canvas", "Piliin kung aling provider ang magbibigay ng motion artwork", "Komunidad", "Ibahagi ang Levyra playlist", "Mas magandang pamagat ng video", "Gamitin ang mga pamagat at thumbnail ng komunidad sa video lamang", "Kilalanin ang musika"),
    "he" to canvasStrings("איכות Canvas", "מצב אוטומטי מתאים את עצמו לרשת, לסוללה ולמסך; גבוהה שומרת על ה-Canvas החד ביותר", "מקור Canvas", "בחר איזה ספק יספק את המוטיון ארטוורק", "קהילה", "שיתוף פלייליסט של Levyra", "כותרות וידאו טובות יותר", "שימוש בכותרות ובתמונות ממוזערות של הקהילה בסרטונים בלבד", "זיהוי מוזיקה")
)

internal fun canvasLocalizationEntries(code: String): Map<String, String> = canvasBundles.getValue(code)

internal fun canvasLocalizationCodes(): Set<String> = canvasBundles.keys
