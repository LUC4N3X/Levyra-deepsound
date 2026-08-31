package com.luc4n3x.levyra.ui.i18n

data class RecommendationDiagnosticsCopy(
    val moreLikeThis: String,
    val lessLikeThis: String,
    val blockArtist: String,
    val allowArtistAgain: String,
    val diagnosticsTitle: String,
    val close: String,
    val copied: String,
    val track: String,
    val id: String,
    val title: String,
    val artist: String,
    val source: String,
    val mode: String,
    val player: String,
    val state: String,
    val playing: String,
    val position: String,
    val buffered: String,
    val duration: String,
    val speed: String,
    val audioSession: String,
    val errorCode: String,
    val formats: String,
    val audio: String,
    val video: String,
    val network: String,
    val cache: String,
    val transport: String,
    val validated: String,
    val metered: String,
    val resolver: String,
    val successFailure: String,
    val failureStreak: String,
    val circuit: String,
    val averageLatency: String,
    val lastFailure: String,
    val security: String,
    val copyReport: String,
    val statusHealthy: String,
    val statusFallback: String,
    val statusError: String,
    val statusIdle: String
)

private data class RecommendationDiagnosticsLocale(
    val moreLikeThis: String,
    val lessLikeThis: String,
    val blockArtist: String,
    val allowArtistAgain: String,
    val diagnosticsTitle: String,
    val copied: String,
    val copyReport: String,
    val statusHealthy: String,
    val statusFallback: String,
    val statusError: String,
    val statusIdle: String,
    val security: String
)

internal val recommendationDiagnosticsDedicatedCodes = setOf(
    "en", "it", "es", "fr", "de", "pt", "nl", "pl", "ro", "el", "sv", "da", "cs",
    "uk", "ru", "tr", "ar", "zh", "ja", "ko", "hi", "id", "vi", "th", "fil", "he"
)

fun LevyraStrings.recommendationDiagnosticsCopy(): RecommendationDiagnosticsCopy {
    val locale = recommendationDiagnosticsLocale()
    return RecommendationDiagnosticsCopy(
        moreLikeThis = locale.moreLikeThis,
        lessLikeThis = locale.lessLikeThis,
        blockArtist = locale.blockArtist,
        allowArtistAgain = locale.allowArtistAgain,
        diagnosticsTitle = locale.diagnosticsTitle,
        close = close,
        copied = locale.copied,
        track = song,
        id = "ID",
        title = localizedTechnicalLabel(
            it = "Titolo", es = "Título", fr = "Titre", de = "Titel", pt = "Título", nl = "Titel",
            pl = "Tytuł", ro = "Titlu", el = "Τίτλος", sv = "Titel", da = "Titel", cs = "Název",
            uk = "Назва", ru = "Название", tr = "Başlık", ar = "العنوان", zh = "标题", ja = "タイトル",
            ko = "제목", hi = "शीर्षक", id = "Judul", vi = "Tiêu đề", th = "ชื่อ", fil = "Pamagat", he = "כותרת",
            fallback = "Title"
        ),
        artist = artistLabel,
        source = localizedTechnicalLabel(
            it = "Sorgente", es = "Fuente", fr = "Source", de = "Quelle", pt = "Fonte", nl = "Bron",
            pl = "Źródło", ro = "Sursă", el = "Πηγή", sv = "Källa", da = "Kilde", cs = "Zdroj",
            uk = "Джерело", ru = "Источник", tr = "Kaynak", ar = "المصدر", zh = "来源", ja = "ソース",
            ko = "소스", hi = "स्रोत", id = "Sumber", vi = "Nguồn", th = "แหล่งที่มา", fil = "Pinagmulan", he = "מקור",
            fallback = "Source"
        ),
        mode = localizedTechnicalLabel(
            it = "Modalità", es = "Modo", fr = "Mode", de = "Modus", pt = "Modo", nl = "Modus",
            pl = "Tryb", ro = "Mod", el = "Λειτουργία", sv = "Läge", da = "Tilstand", cs = "Režim",
            uk = "Режим", ru = "Режим", tr = "Mod", ar = "الوضع", zh = "模式", ja = "モード",
            ko = "모드", hi = "मोड", id = "Mode", vi = "Chế độ", th = "โหมด", fil = "Mode", he = "מצב",
            fallback = "Mode"
        ),
        player = player,
        state = localizedTechnicalLabel(
            it = "Stato", es = "Estado", fr = "État", de = "Status", pt = "Estado", nl = "Status",
            pl = "Stan", ro = "Stare", el = "Κατάσταση", sv = "Status", da = "Status", cs = "Stav",
            uk = "Стан", ru = "Состояние", tr = "Durum", ar = "الحالة", zh = "状态", ja = "状態",
            ko = "상태", hi = "स्थिति", id = "Status", vi = "Trạng thái", th = "สถานะ", fil = "Katayuan", he = "מצב",
            fallback = "State"
        ),
        playing = playing,
        position = localizedTechnicalLabel(
            it = "Posizione", es = "Posición", fr = "Position", de = "Position", pt = "Posição", nl = "Positie",
            pl = "Pozycja", ro = "Poziție", el = "Θέση", sv = "Position", da = "Position", cs = "Pozice",
            uk = "Позиція", ru = "Позиция", tr = "Konum", ar = "الموضع", zh = "位置", ja = "位置",
            ko = "위치", hi = "स्थिति", id = "Posisi", vi = "Vị trí", th = "ตำแหน่ง", fil = "Posisyon", he = "מיקום",
            fallback = "Position"
        ),
        buffered = localizedTechnicalLabel(
            it = "Buffer", es = "Búfer", fr = "Tampon", de = "Puffer", pt = "Buffer", nl = "Buffer",
            pl = "Bufor", ro = "Buffer", el = "Buffer", sv = "Buffert", da = "Buffer", cs = "Vyrovnávací paměť",
            uk = "Буфер", ru = "Буфер", tr = "Arabellek", ar = "التخزين المؤقت", zh = "缓冲", ja = "バッファ",
            ko = "버퍼", hi = "बफ़र", id = "Buffer", vi = "Bộ đệm", th = "บัฟเฟอร์", fil = "Buffer", he = "מאגר",
            fallback = "Buffered"
        ),
        duration = localizedTechnicalLabel(
            it = "Durata", es = "Duración", fr = "Durée", de = "Dauer", pt = "Duração", nl = "Duur",
            pl = "Czas trwania", ro = "Durată", el = "Διάρκεια", sv = "Längd", da = "Varighed", cs = "Délka",
            uk = "Тривалість", ru = "Длительность", tr = "Süre", ar = "المدة", zh = "时长", ja = "長さ",
            ko = "길이", hi = "अवधि", id = "Durasi", vi = "Thời lượng", th = "ระยะเวลา", fil = "Tagal", he = "משך",
            fallback = "Duration"
        ),
        speed = localizedTechnicalLabel(
            it = "Velocità", es = "Velocidad", fr = "Vitesse", de = "Geschwindigkeit", pt = "Velocidade", nl = "Snelheid",
            pl = "Prędkość", ro = "Viteză", el = "Ταχύτητα", sv = "Hastighet", da = "Hastighed", cs = "Rychlost",
            uk = "Швидкість", ru = "Скорость", tr = "Hız", ar = "السرعة", zh = "速度", ja = "速度",
            ko = "속도", hi = "गति", id = "Kecepatan", vi = "Tốc độ", th = "ความเร็ว", fil = "Bilis", he = "מהירות",
            fallback = "Speed"
        ),
        audioSession = "Audio session",
        errorCode = localizedTechnicalLabel(
            it = "Codice errore", es = "Código de error", fr = "Code d’erreur", de = "Fehlercode", pt = "Código de erro", nl = "Foutcode",
            pl = "Kod błędu", ro = "Cod eroare", el = "Κωδικός σφάλματος", sv = "Felkod", da = "Fejlkode", cs = "Kód chyby",
            uk = "Код помилки", ru = "Код ошибки", tr = "Hata kodu", ar = "رمز الخطأ", zh = "错误代码", ja = "エラーコード",
            ko = "오류 코드", hi = "त्रुटि कोड", id = "Kode kesalahan", vi = "Mã lỗi", th = "รหัสข้อผิดพลาด", fil = "Error code", he = "קוד שגיאה",
            fallback = "Error code"
        ),
        formats = "Formats",
        audio = "Audio",
        video = video,
        network = localizedTechnicalLabel(
            it = "Rete", es = "Red", fr = "Réseau", de = "Netzwerk", pt = "Rede", nl = "Netwerk",
            pl = "Sieć", ro = "Rețea", el = "Δίκτυο", sv = "Nätverk", da = "Netværk", cs = "Síť",
            uk = "Мережа", ru = "Сеть", tr = "Ağ", ar = "الشبكة", zh = "网络", ja = "ネットワーク",
            ko = "네트워크", hi = "नेटवर्क", id = "Jaringan", vi = "Mạng", th = "เครือข่าย", fil = "Network", he = "רשת",
            fallback = "Network"
        ),
        cache = "Cache",
        transport = "Transport",
        validated = "Validated",
        metered = "Metered",
        resolver = "Resolver",
        successFailure = "Success / failure",
        failureStreak = "Failure streak",
        circuit = "Circuit",
        averageLatency = "Average latency",
        lastFailure = "Last failure",
        security = locale.security,
        copyReport = locale.copyReport,
        statusHealthy = locale.statusHealthy,
        statusFallback = locale.statusFallback,
        statusError = locale.statusError,
        statusIdle = locale.statusIdle
    )
}

private fun LevyraStrings.localizedTechnicalLabel(
    it: String,
    es: String,
    fr: String,
    de: String,
    pt: String,
    nl: String,
    pl: String,
    ro: String,
    el: String,
    sv: String,
    da: String,
    cs: String,
    uk: String,
    ru: String,
    tr: String,
    ar: String,
    zh: String,
    ja: String,
    ko: String,
    hi: String,
    id: String,
    vi: String,
    th: String,
    fil: String,
    he: String,
    fallback: String
): String = when (code) {
    "it" -> it
    "es" -> es
    "fr" -> fr
    "de" -> de
    "pt" -> pt
    "nl" -> nl
    "pl" -> pl
    "ro" -> ro
    "el" -> el
    "sv" -> sv
    "da" -> da
    "cs" -> cs
    "uk" -> uk
    "ru" -> ru
    "tr" -> tr
    "ar" -> ar
    "zh" -> zh
    "ja" -> ja
    "ko" -> ko
    "hi" -> hi
    "id" -> id
    "vi" -> vi
    "th" -> th
    "fil" -> fil
    "he" -> he
    else -> fallback
}

private fun LevyraStrings.recommendationDiagnosticsLocale(): RecommendationDiagnosticsLocale = when (code) {
    "it" -> RecommendationDiagnosticsLocale(
        "Più brani così nella radio", "Meno brani così nella radio", "Non suggerire questo artista nella radio",
        "Suggerisci di nuovo questo artista", "Diagnostica riproduzione", "Report diagnostico copiato", "Copia report",
        "Riproduzione regolare", "Fallback usati di recente", "Errore di riproduzione", "Player inattivo",
        "Il report non include URL firmati, header, cookie, token o chiavi API."
    )
    "es" -> RecommendationDiagnosticsLocale(
        "Más canciones así en la radio", "Menos canciones así en la radio", "No sugerir este artista en la radio",
        "Volver a sugerir este artista", "Diagnóstico de reproducción", "Informe de diagnóstico copiado", "Copiar informe",
        "Reproducción estable", "Fallback usado recientemente", "Error de reproducción", "Reproductor inactivo",
        "El informe no incluye URL firmadas, cabeceras, cookies, tokens ni claves API."
    )
    "fr" -> RecommendationDiagnosticsLocale(
        "Plus de titres comme celui-ci à la radio", "Moins de titres comme celui-ci à la radio", "Ne plus suggérer cet artiste à la radio",
        "Suggérer à nouveau cet artiste", "Diagnostic de lecture", "Rapport de diagnostic copié", "Copier le rapport",
        "Lecture normale", "Fallback utilisé récemment", "Erreur de lecture", "Lecteur inactif",
        "Le rapport n’inclut ni URL signées, ni en-têtes, ni cookies, ni jetons, ni clés API."
    )
    "de" -> RecommendationDiagnosticsLocale(
        "Mehr davon im Radio", "Weniger davon im Radio", "Diesen Künstler im Radio nicht vorschlagen",
        "Diesen Künstler wieder vorschlagen", "Wiedergabediagnose", "Diagnosebericht kopiert", "Bericht kopieren",
        "Wiedergabe stabil", "Kürzlich Fallback verwendet", "Wiedergabefehler", "Player inaktiv",
        "Der Bericht enthält keine signierten URLs, Header, Cookies, Tokens oder API-Schlüssel."
    )
    "pt" -> RecommendationDiagnosticsLocale(
        "Mais músicas assim na rádio", "Menos músicas assim na rádio", "Não sugerir este artista na rádio",
        "Voltar a sugerir este artista", "Diagnóstico de reprodução", "Relatório de diagnóstico copiado", "Copiar relatório",
        "Reprodução estável", "Fallback usado recentemente", "Erro de reprodução", "Player inativo",
        "O relatório não inclui URLs assinados, cabeçalhos, cookies, tokens ou chaves de API."
    )
    "nl" -> RecommendationDiagnosticsLocale(
        "Meer zoals dit op de radio", "Minder zoals dit op de radio", "Deze artiest niet aanbevelen op de radio",
        "Deze artiest weer aanbevelen", "Afspeeldiagnose", "Diagnoserapport gekopieerd", "Rapport kopiëren",
        "Afspelen is stabiel", "Onlangs fallback gebruikt", "Afspeelfout", "Speler inactief",
        "Het rapport bevat geen ondertekende URL’s, headers, cookies, tokens of API-sleutels."
    )
    "pl" -> RecommendationDiagnosticsLocale(
        "Więcej takich utworów w radiu", "Mniej takich utworów w radiu", "Nie polecaj tego artysty w radiu",
        "Ponownie polecaj tego artystę", "Diagnostyka odtwarzania", "Skopiowano raport diagnostyczny", "Kopiuj raport",
        "Odtwarzanie stabilne", "Ostatnio użyto fallbacku", "Błąd odtwarzania", "Odtwarzacz nieaktywny",
        "Raport nie zawiera podpisanych adresów URL, nagłówków, plików cookie, tokenów ani kluczy API."
    )
    "ro" -> RecommendationDiagnosticsLocale(
        "Mai multe piese ca aceasta la radio", "Mai puține piese ca aceasta la radio", "Nu sugera acest artist la radio",
        "Sugerează din nou acest artist", "Diagnostic redare", "Raport de diagnostic copiat", "Copiază raportul",
        "Redare stabilă", "Fallback folosit recent", "Eroare de redare", "Player inactiv",
        "Raportul nu include URL-uri semnate, antete, cookie-uri, tokenuri sau chei API."
    )
    "el" -> RecommendationDiagnosticsLocale(
        "Περισσότερα σαν αυτό στο ραδιόφωνο", "Λιγότερα σαν αυτό στο ραδιόφωνο", "Να μην προτείνεται αυτός ο καλλιτέχνης στο ραδιόφωνο",
        "Να προτείνεται ξανά αυτός ο καλλιτέχνης", "Διαγνωστικά αναπαραγωγής", "Η αναφορά διαγνωστικών αντιγράφηκε", "Αντιγραφή αναφοράς",
        "Ομαλή αναπαραγωγή", "Πρόσφατη χρήση fallback", "Σφάλμα αναπαραγωγής", "Ανενεργό player",
        "Η αναφορά δεν περιλαμβάνει υπογεγραμμένα URL, headers, cookies, tokens ή κλειδιά API."
    )
    "sv" -> RecommendationDiagnosticsLocale(
        "Mer sånt här i radion", "Mindre sånt här i radion", "Föreslå inte den här artisten i radion",
        "Föreslå den här artisten igen", "Uppspelningsdiagnostik", "Diagnostikrapport kopierad", "Kopiera rapport",
        "Uppspelningen är stabil", "Fallback användes nyligen", "Uppspelningsfel", "Spelaren är inaktiv",
        "Rapporten innehåller inga signerade URL:er, headers, cookies, tokens eller API-nycklar."
    )
    "da" -> RecommendationDiagnosticsLocale(
        "Mere som dette i radioen", "Mindre som dette i radioen", "Foreslå ikke denne kunstner i radioen",
        "Foreslå denne kunstner igen", "Afspilningsdiagnostik", "Diagnoserapport kopieret", "Kopiér rapport",
        "Afspilningen er stabil", "Fallback blev brugt for nylig", "Afspilningsfejl", "Afspilleren er inaktiv",
        "Rapporten indeholder ikke signerede URL’er, headers, cookies, tokens eller API-nøgler."
    )
    "cs" -> RecommendationDiagnosticsLocale(
        "Více podobných skladeb v rádiu", "Méně podobných skladeb v rádiu", "Nenabízet tohoto interpreta v rádiu",
        "Znovu nabízet tohoto interpreta", "Diagnostika přehrávání", "Diagnostická zpráva zkopírována", "Kopírovat zprávu",
        "Přehrávání je stabilní", "Nedávno použit fallback", "Chyba přehrávání", "Přehrávač je neaktivní",
        "Zpráva neobsahuje podepsané URL, hlavičky, cookies, tokeny ani API klíče."
    )
    "uk" -> RecommendationDiagnosticsLocale(
        "Більше схожих треків у радіо", "Менше схожих треків у радіо", "Не пропонувати цього виконавця в радіо",
        "Знову пропонувати цього виконавця", "Діагностика відтворення", "Діагностичний звіт скопійовано", "Копіювати звіт",
        "Відтворення стабільне", "Нещодавно використано fallback", "Помилка відтворення", "Програвач неактивний",
        "Звіт не містить підписаних URL, заголовків, cookie, токенів або API-ключів."
    )
    "ru" -> RecommendationDiagnosticsLocale(
        "Больше похожих треков в радио", "Меньше похожих треков в радио", "Не предлагать этого исполнителя в радио",
        "Снова предлагать этого исполнителя", "Диагностика воспроизведения", "Диагностический отчёт скопирован", "Копировать отчёт",
        "Воспроизведение стабильно", "Недавно использован fallback", "Ошибка воспроизведения", "Плеер неактивен",
        "Отчёт не содержит подписанных URL, заголовков, cookie, токенов или API-ключей."
    )
    "tr" -> RecommendationDiagnosticsLocale(
        "Radyoda buna benzer daha fazla", "Radyoda buna benzer daha az", "Bu sanatçıyı radyoda önerme",
        "Bu sanatçıyı tekrar öner", "Oynatma tanılama", "Tanılama raporu kopyalandı", "Raporu kopyala",
        "Oynatma kararlı", "Yakın zamanda fallback kullanıldı", "Oynatma hatası", "Oynatıcı etkin değil",
        "Rapor imzalı URL, başlık, çerez, token veya API anahtarı içermez."
    )
    "ar" -> RecommendationDiagnosticsLocale(
        "المزيد من المقاطع المشابهة في الراديو", "أقل من المقاطع المشابهة في الراديو", "عدم اقتراح هذا الفنان في الراديو",
        "اقتراح هذا الفنان مجددًا", "تشخيص التشغيل", "تم نسخ تقرير التشخيص", "نسخ التقرير",
        "التشغيل مستقر", "تم استخدام مسار بديل مؤخرًا", "خطأ في التشغيل", "المشغل غير نشط",
        "لا يتضمن التقرير روابط موقعة أو ترويسات أو ملفات تعريف ارتباط أو رموزًا أو مفاتيح API."
    )
    "zh" -> RecommendationDiagnosticsLocale(
        "电台中多推荐类似歌曲", "电台中少推荐类似歌曲", "电台中不再推荐此艺人",
        "重新推荐此艺人", "播放诊断", "诊断报告已复制", "复制报告",
        "播放正常", "最近使用了回退策略", "播放错误", "播放器空闲",
        "报告不会包含签名 URL、请求头、Cookie、令牌或 API 密钥。"
    )
    "ja" -> RecommendationDiagnosticsLocale(
        "ラジオで似た曲を増やす", "ラジオで似た曲を減らす", "ラジオでこのアーティストをおすすめしない",
        "このアーティストを再びおすすめする", "再生診断", "診断レポートをコピーしました", "レポートをコピー",
        "再生は正常です", "最近フォールバックを使用しました", "再生エラー", "プレーヤーは待機中です",
        "レポートには署名付き URL、ヘッダー、Cookie、トークン、API キーは含まれません。"
    )
    "ko" -> RecommendationDiagnosticsLocale(
        "라디오에서 비슷한 곡 더 추천", "라디오에서 비슷한 곡 덜 추천", "라디오에서 이 아티스트 추천 안 함",
        "이 아티스트 다시 추천", "재생 진단", "진단 보고서를 복사했습니다", "보고서 복사",
        "재생 정상", "최근 대체 경로 사용", "재생 오류", "플레이어 대기 중",
        "보고서에는 서명된 URL, 헤더, 쿠키, 토큰 또는 API 키가 포함되지 않습니다."
    )
    "hi" -> RecommendationDiagnosticsLocale(
        "रेडियो में ऐसे और गाने", "रेडियो में ऐसे कम गाने", "रेडियो में इस कलाकार को न सुझाएँ",
        "इस कलाकार को फिर सुझाएँ", "प्लेबैक डायग्नोस्टिक्स", "डायग्नोस्टिक रिपोर्ट कॉपी हुई", "रिपोर्ट कॉपी करें",
        "प्लेबैक सामान्य", "हाल ही में फ़ॉलबैक इस्तेमाल हुआ", "प्लेबैक त्रुटि", "प्लेयर निष्क्रिय",
        "रिपोर्ट में साइन किए गए URL, हेडर, कुकी, टोकन या API कुंजियाँ शामिल नहीं हैं।"
    )
    "id" -> RecommendationDiagnosticsLocale(
        "Lebih banyak seperti ini di radio", "Lebih sedikit seperti ini di radio", "Jangan sarankan artis ini di radio",
        "Sarankan artis ini lagi", "Diagnostik pemutaran", "Laporan diagnostik disalin", "Salin laporan",
        "Pemutaran normal", "Fallback baru-baru ini digunakan", "Kesalahan pemutaran", "Pemutar tidak aktif",
        "Laporan tidak menyertakan URL bertanda tangan, header, cookie, token, atau kunci API."
    )
    "vi" -> RecommendationDiagnosticsLocale(
        "Thêm bài tương tự trong radio", "Bớt bài tương tự trong radio", "Không đề xuất nghệ sĩ này trong radio",
        "Đề xuất lại nghệ sĩ này", "Chẩn đoán phát nhạc", "Đã sao chép báo cáo chẩn đoán", "Sao chép báo cáo",
        "Phát nhạc ổn định", "Gần đây đã dùng fallback", "Lỗi phát nhạc", "Trình phát không hoạt động",
        "Báo cáo không chứa URL đã ký, header, cookie, token hoặc khóa API."
    )
    "th" -> RecommendationDiagnosticsLocale(
        "เพิ่มเพลงแบบนี้ในวิทยุ", "ลดเพลงแบบนี้ในวิทยุ", "ไม่แนะนำศิลปินนี้ในวิทยุ",
        "แนะนำศิลปินนี้อีกครั้ง", "การวินิจฉัยการเล่น", "คัดลอกรายงานการวินิจฉัยแล้ว", "คัดลอกรายงาน",
        "การเล่นปกติ", "เพิ่งใช้เส้นทางสำรอง", "ข้อผิดพลาดในการเล่น", "เครื่องเล่นไม่ได้ใช้งาน",
        "รายงานจะไม่รวม URL ที่ลงนามแล้ว ส่วนหัว คุกกี้ โทเค็น หรือคีย์ API"
    )
    "fil" -> RecommendationDiagnosticsLocale(
        "Mas marami pang ganito sa radio", "Mas kaunting ganito sa radio", "Huwag irekomenda ang artist na ito sa radio",
        "Irekomenda ulit ang artist na ito", "Playback diagnostics", "Nakopya ang diagnostic report", "Kopyahin ang report",
        "Maayos ang playback", "Kamakailang gumamit ng fallback", "Playback error", "Hindi aktibo ang player",
        "Hindi kasama sa report ang signed URL, headers, cookies, tokens o API keys."
    )
    "he" -> RecommendationDiagnosticsLocale(
        "עוד שירים כאלה ברדיו", "פחות שירים כאלה ברדיו", "לא להציע את האמן הזה ברדיו",
        "להציע שוב את האמן הזה", "אבחון השמעה", "דוח האבחון הועתק", "העתקת דוח",
        "ההשמעה תקינה", "נעשה שימוש לאחרונה ב-fallback", "שגיאת השמעה", "הנגן לא פעיל",
        "הדוח אינו כולל כתובות URL חתומות, כותרות, קובצי Cookie, אסימונים או מפתחות API."
    )
    else -> RecommendationDiagnosticsLocale(
        "More like this in radio", "Less like this in radio", "Don't suggest this artist in radio",
        "Suggest this artist again", "Playback diagnostics", "Diagnostic report copied", "Copy report",
        "Playback healthy", "Recent fallback activity", "Playback error", "Player idle",
        "The report excludes signed URLs, request headers, cookies, tokens and API keys."
    )
}
