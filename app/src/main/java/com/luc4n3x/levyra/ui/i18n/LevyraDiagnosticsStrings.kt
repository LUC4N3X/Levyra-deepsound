package com.luc4n3x.levyra.ui.i18n

private val diagnosticsKeyList = listOf(
    "moreLikeThis",
    "lessLikeThis",
    "playbackDiagnostics",
    "playbackDiagnosticsSubtitle",
    "diagnosticsCopyReport",
    "diagnosticsCopied",
    "diagnosticsPrivacyNote",
    "diagnosticsStatusHealthy",
    "diagnosticsStatusFallback",
    "diagnosticsStatusError",
    "diagnosticsStatusIdle",
    "diagnosticsSectionPlayback",
    "diagnosticsSectionFormats",
    "diagnosticsSectionNetwork",
    "diagnosticsSectionResolver",
    "diagnosticsNoPlayback"
)

internal val diagnosticsKeys: Set<String> = diagnosticsKeyList.toSet()

private fun diagnostics(vararg values: String): Map<String, String> {
    require(values.size == diagnosticsKeyList.size) {
        "Expected ${diagnosticsKeyList.size} diagnostics strings, received ${values.size}"
    }
    return diagnosticsKeyList.zip(values.asList()).toMap()
}

private val diagnosticsBundles: Map<String, Map<String, String>> = mapOf(
    "en" to diagnostics(
        "More like this", "Less like this", "Playback diagnostics",
        "Live player, format and resolver state", "Copy report", "Report copied",
        "Stream URLs, request headers, cookies, tokens and API keys are never included.",
        "Healthy", "Fallback used recently", "Playback error", "Idle",
        "Playback", "Formats", "Cache and network", "Resolver",
        "Nothing is playing right now."
    ),
    "it" to diagnostics(
        "Più contenuti come questo", "Meno contenuti come questo", "Diagnostica riproduzione",
        "Stato in tempo reale di player, formati e resolver", "Copia report", "Report copiato",
        "URL dei flussi, header, cookie, token e chiavi API non sono mai inclusi.",
        "Tutto regolare", "Fallback usato di recente", "Errore di riproduzione", "Inattivo",
        "Riproduzione", "Formati", "Cache e rete", "Resolver",
        "Non è in riproduzione nulla al momento."
    ),
    "es" to diagnostics(
        "Más contenido como este", "Menos contenido como este", "Diagnóstico de reproducción",
        "Estado en vivo del reproductor, formatos y resolver", "Copiar informe", "Informe copiado",
        "Las URL de streaming, cabeceras, cookies, tokens y claves de API nunca se incluyen.",
        "Correcto", "Se usó un método alternativo hace poco", "Error de reproducción", "Inactivo",
        "Reproducción", "Formatos", "Caché y red", "Resolver",
        "No se está reproduciendo nada ahora mismo."
    ),
    "fr" to diagnostics(
        "Plus de contenu comme ça", "Moins de contenu comme ça", "Diagnostic de lecture",
        "État en direct du lecteur, des formats et du résolveur", "Copier le rapport", "Rapport copié",
        "Les URL de flux, en-têtes, cookies, jetons et clés d'API ne sont jamais inclus.",
        "Correct", "Solution de repli utilisée récemment", "Erreur de lecture", "Inactif",
        "Lecture", "Formats", "Cache et réseau", "Résolveur",
        "Aucune lecture en cours."
    ),
    "de" to diagnostics(
        "Mehr davon", "Weniger davon", "Wiedergabe-Diagnose",
        "Live-Status von Player, Formaten und Resolver", "Bericht kopieren", "Bericht kopiert",
        "Stream-URLs, Anfrage-Header, Cookies, Tokens und API-Schlüssel werden nie aufgenommen.",
        "In Ordnung", "Kürzlich Fallback genutzt", "Wiedergabefehler", "Inaktiv",
        "Wiedergabe", "Formate", "Cache und Netzwerk", "Resolver",
        "Es wird gerade nichts wiedergegeben."
    ),
    "pt" to diagnostics(
        "Mais conteúdo assim", "Menos conteúdo assim", "Diagnóstico de reprodução",
        "Estado ao vivo do player, formatos e resolver", "Copiar relatório", "Relatório copiado",
        "URLs de streaming, cabeçalhos, cookies, tokens e chaves de API nunca são incluídos.",
        "Tudo certo", "Alternativa usada recentemente", "Erro de reprodução", "Inativo",
        "Reprodução", "Formatos", "Cache e rede", "Resolver",
        "Nada está sendo reproduzido agora."
    ),
    "nl" to diagnostics(
        "Meer zoals dit", "Minder zoals dit", "Afspeeldiagnose",
        "Live status van speler, formaten en resolver", "Rapport kopiëren", "Rapport gekopieerd",
        "Stream-URL's, headers, cookies, tokens en API-sleutels worden nooit opgenomen.",
        "In orde", "Onlangs terugval gebruikt", "Afspeelfout", "Inactief",
        "Afspelen", "Formaten", "Cache en netwerk", "Resolver",
        "Er wordt nu niets afgespeeld."
    ),
    "pl" to diagnostics(
        "Więcej takich", "Mniej takich", "Diagnostyka odtwarzania",
        "Bieżący stan odtwarzacza, formatów i resolvera", "Kopiuj raport", "Raport skopiowany",
        "Adresy strumieni, nagłówki, pliki cookie, tokeny i klucze API nigdy nie są dołączane.",
        "Wszystko działa", "Niedawno użyto rozwiązania zapasowego", "Błąd odtwarzania", "Bezczynny",
        "Odtwarzanie", "Formaty", "Pamięć podręczna i sieć", "Resolver",
        "Nic nie jest teraz odtwarzane."
    ),
    "ro" to diagnostics(
        "Mai mult conținut ca acesta", "Mai puțin conținut ca acesta", "Diagnostic redare",
        "Starea în timp real a playerului, formatelor și resolverului", "Copiază raportul", "Raport copiat",
        "URL-urile fluxurilor, anteturile, cookie-urile, tokenurile și cheile API nu sunt incluse niciodată.",
        "În regulă", "Alternativă folosită recent", "Eroare de redare", "Inactiv",
        "Redare", "Formate", "Cache și rețea", "Resolver",
        "Nu se redă nimic acum."
    ),
    "el" to diagnostics(
        "Περισσότερα σαν αυτό", "Λιγότερα σαν αυτό", "Διαγνωστικά αναπαραγωγής",
        "Ζωντανή κατάσταση player, μορφών και resolver", "Αντιγραφή αναφοράς", "Η αναφορά αντιγράφηκε",
        "Οι διευθύνσεις ροής, οι κεφαλίδες, τα cookies, τα tokens και τα κλειδιά API δεν περιλαμβάνονται ποτέ.",
        "Εντάξει", "Χρησιμοποιήθηκε πρόσφατα εφεδρική μέθοδος", "Σφάλμα αναπαραγωγής", "Αδρανές",
        "Αναπαραγωγή", "Μορφές", "Cache και δίκτυο", "Resolver",
        "Δεν αναπαράγεται τίποτα αυτή τη στιγμή."
    ),
    "sv" to diagnostics(
        "Mer som detta", "Mindre som detta", "Uppspelningsdiagnostik",
        "Livestatus för spelare, format och resolver", "Kopiera rapport", "Rapport kopierad",
        "Ström-URL:er, headers, cookies, tokens och API-nycklar inkluderas aldrig.",
        "Allt fungerar", "Reservmetod användes nyligen", "Uppspelningsfel", "Inaktiv",
        "Uppspelning", "Format", "Cache och nätverk", "Resolver",
        "Inget spelas upp just nu."
    ),
    "da" to diagnostics(
        "Mere som dette", "Mindre som dette", "Afspilningsdiagnostik",
        "Livestatus for afspiller, formater og resolver", "Kopiér rapport", "Rapport kopieret",
        "Stream-URL'er, headers, cookies, tokens og API-nøgler medtages aldrig.",
        "Alt fungerer", "Reserveløsning brugt for nylig", "Afspilningsfejl", "Inaktiv",
        "Afspilning", "Formater", "Cache og netværk", "Resolver",
        "Der afspilles ikke noget lige nu."
    ),
    "cs" to diagnostics(
        "Více podobného", "Méně podobného", "Diagnostika přehrávání",
        "Aktuální stav přehrávače, formátů a resolveru", "Kopírovat zprávu", "Zpráva zkopírována",
        "Adresy streamů, hlavičky, cookies, tokeny a klíče API nejsou nikdy součástí.",
        "V pořádku", "Nedávno použita záloha", "Chyba přehrávání", "Nečinné",
        "Přehrávání", "Formáty", "Mezipaměť a síť", "Resolver",
        "Právě se nic nepřehrává."
    ),
    "uk" to diagnostics(
        "Більше такого", "Менше такого", "Діагностика відтворення",
        "Поточний стан плеєра, форматів і резолвера", "Скопіювати звіт", "Звіт скопійовано",
        "URL потоків, заголовки, файли cookie, токени та ключі API ніколи не включаються.",
        "Усе гаразд", "Нещодавно використано резервний шлях", "Помилка відтворення", "Неактивно",
        "Відтворення", "Формати", "Кеш і мережа", "Резолвер",
        "Зараз нічого не відтворюється."
    ),
    "ru" to diagnostics(
        "Больше такого", "Меньше такого", "Диагностика воспроизведения",
        "Текущее состояние плеера, форматов и резолвера", "Скопировать отчёт", "Отчёт скопирован",
        "URL потоков, заголовки, файлы cookie, токены и ключи API никогда не включаются.",
        "Всё в порядке", "Недавно использован резервный путь", "Ошибка воспроизведения", "Простой",
        "Воспроизведение", "Форматы", "Кэш и сеть", "Резолвер",
        "Сейчас ничего не воспроизводится."
    ),
    "tr" to diagnostics(
        "Buna benzer daha fazla", "Buna benzer daha az", "Oynatma tanılaması",
        "Oynatıcı, format ve çözümleyici durumu", "Raporu kopyala", "Rapor kopyalandı",
        "Akış URL'leri, istek başlıkları, çerezler, belirteçler ve API anahtarları asla eklenmez.",
        "Sorun yok", "Yakın zamanda yedek yöntem kullanıldı", "Oynatma hatası", "Boşta",
        "Oynatma", "Formatlar", "Önbellek ve ağ", "Çözümleyici",
        "Şu anda hiçbir şey çalmıyor."
    ),
    "ar" to diagnostics(
        "المزيد مثل هذا", "أقل مثل هذا", "تشخيص التشغيل",
        "الحالة الحية للمشغل والصيغ والمُحلل", "نسخ التقرير", "تم نسخ التقرير",
        "لا يتم أبدًا تضمين روابط البث ورؤوس الطلبات وملفات تعريف الارتباط والرموز ومفاتيح API.",
        "سليم", "تم استخدام بديل مؤخرًا", "خطأ في التشغيل", "خامل",
        "التشغيل", "الصيغ", "ذاكرة التخزين والشبكة", "المُحلل",
        "لا يتم تشغيل أي شيء الآن."
    ),
    "zh" to diagnostics(
        "多推荐类似内容", "少推荐类似内容", "播放诊断",
        "播放器、格式与解析器实时状态", "复制报告", "已复制报告",
        "报告绝不包含流媒体地址、请求头、Cookie、令牌和 API 密钥。",
        "正常", "最近使用了备用方案", "播放错误", "空闲",
        "播放", "格式", "缓存与网络", "解析器",
        "当前没有正在播放的内容。"
    ),
    "ja" to diagnostics(
        "こういう曲をもっと", "こういう曲を減らす", "再生診断",
        "プレーヤー・フォーマット・リゾルバーの現在状態", "レポートをコピー", "レポートをコピーしました",
        "ストリームURL、リクエストヘッダー、Cookie、トークン、APIキーは一切含まれません。",
        "正常", "最近フォールバックを使用", "再生エラー", "アイドル",
        "再生", "フォーマット", "キャッシュとネットワーク", "リゾルバー",
        "現在再生中の項目はありません。"
    ),
    "ko" to diagnostics(
        "이런 곡 더 보기", "이런 곡 그만 보기", "재생 진단",
        "플레이어·형식·리졸버 실시간 상태", "보고서 복사", "보고서를 복사했습니다",
        "스트림 URL, 요청 헤더, 쿠키, 토큰, API 키는 절대 포함되지 않습니다.",
        "정상", "최근 대체 경로 사용", "재생 오류", "대기 중",
        "재생", "형식", "캐시 및 네트워크", "리졸버",
        "현재 재생 중인 항목이 없습니다."
    ),
    "hi" to diagnostics(
        "इस तरह के और", "इस तरह के कम", "प्लेबैक डायग्नोस्टिक्स",
        "प्लेयर, फ़ॉर्मैट और रिज़ॉल्वर की मौजूदा स्थिति", "रिपोर्ट कॉपी करें", "रिपोर्ट कॉपी हो गई",
        "स्ट्रीम URL, रिक्वेस्ट हेडर, कुकीज़, टोकन और API कुंजियाँ कभी शामिल नहीं होतीं।",
        "ठीक है", "हाल ही में फ़ॉलबैक इस्तेमाल हुआ", "प्लेबैक त्रुटि", "निष्क्रिय",
        "प्लेबैक", "फ़ॉर्मैट", "कैश और नेटवर्क", "रिज़ॉल्वर",
        "अभी कुछ भी नहीं चल रहा है।"
    ),
    "id" to diagnostics(
        "Lebih banyak seperti ini", "Lebih sedikit seperti ini", "Diagnostik pemutaran",
        "Status langsung pemutar, format, dan resolver", "Salin laporan", "Laporan disalin",
        "URL streaming, header permintaan, cookie, token, dan kunci API tidak pernah disertakan.",
        "Normal", "Cadangan baru saja digunakan", "Kesalahan pemutaran", "Diam",
        "Pemutaran", "Format", "Cache dan jaringan", "Resolver",
        "Tidak ada yang sedang diputar."
    ),
    "vi" to diagnostics(
        "Thêm nội dung như thế này", "Bớt nội dung như thế này", "Chẩn đoán phát",
        "Trạng thái trực tiếp của trình phát, định dạng và resolver", "Sao chép báo cáo", "Đã sao chép báo cáo",
        "URL luồng, tiêu đề yêu cầu, cookie, token và khóa API không bao giờ được đưa vào.",
        "Bình thường", "Vừa dùng phương án dự phòng", "Lỗi phát", "Không hoạt động",
        "Phát", "Định dạng", "Bộ nhớ đệm và mạng", "Resolver",
        "Hiện không phát nội dung nào."
    ),
    "th" to diagnostics(
        "แนะนำแบบนี้เพิ่ม", "แนะนำแบบนี้น้อยลง", "การวินิจฉัยการเล่น",
        "สถานะสดของโปรแกรมเล่น รูปแบบ และรีโซลเวอร์", "คัดลอกรายงาน", "คัดลอกรายงานแล้ว",
        "รายงานไม่มี URL สตรีม ส่วนหัวคำขอ คุกกี้ โทเค็น และคีย์ API",
        "ปกติ", "เพิ่งใช้วิธีสำรอง", "เล่นผิดพลาด", "ว่าง",
        "การเล่น", "รูปแบบ", "แคชและเครือข่าย", "รีโซลเวอร์",
        "ยังไม่มีการเล่นอะไรตอนนี้"
    ),
    "fil" to diagnostics(
        "Higit pang ganito", "Mas kaunti pang ganito", "Diagnostic ng pag-playback",
        "Live na estado ng player, format at resolver", "Kopyahin ang ulat", "Nakopya ang ulat",
        "Hindi kailanman kasama ang stream URL, request header, cookie, token at API key.",
        "Maayos", "May ginamit na fallback kamakailan", "Error sa pag-playback", "Walang ginagawa",
        "Playback", "Mga format", "Cache at network", "Resolver",
        "Walang kasalukuyang pinapatugtog."
    ),
    "he" to diagnostics(
        "עוד כאלה", "פחות כאלה", "אבחון השמעה",
        "מצב חי של הנגן, הפורמטים והפותר", "העתקת דוח", "הדוח הועתק",
        "כתובות זרימה, כותרות בקשה, עוגיות, אסימונים ומפתחות API לעולם אינם נכללים.",
        "תקין", "נעשה שימוש בחלופה לאחרונה", "שגיאת השמעה", "לא פעיל",
        "השמעה", "פורמטים", "מטמון ורשת", "פותר",
        "כרגע לא מתנגן דבר."
    )
)

internal fun diagnosticsLocalizationEntries(code: String): Map<String, String> =
    diagnosticsBundles.getValue(code)

internal fun diagnosticsLocalizationCodes(): Set<String> = diagnosticsBundles.keys
