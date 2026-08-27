package com.luc4n3x.levyra.ui.i18n

private fun integrationStrings(
    integrations: String,
    apiKey: String,
    sharedSecret: String,
    token: String,
    lastFmApprovalHint: String,
    subtitlesOff: String,
    subtitles: String
): Map<String, String> = mapOf(
    "integrations" to integrations,
    "apiKeyLabel" to apiKey,
    "sharedSecretLabel" to sharedSecret,
    "credentialTokenLabel" to token,
    "lastFmApprovalHint" to lastFmApprovalHint,
    "subtitlesOff" to subtitlesOff,
    "subtitlesLabel" to subtitles
)

private val integrationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to integrationStrings("Integrations", "API key", "Shared secret", "Token", "After approving in the browser, return here and complete the connection.", "Subtitles off", "Subtitles"),
    "it" to integrationStrings("Integrazioni", "Chiave API", "Segreto condiviso", "Token", "Dopo l'approvazione nel browser, torna qui e completa il collegamento.", "Sottotitoli disattivati", "Sottotitoli"),
    "es" to integrationStrings("Integraciones", "Clave API", "Secreto compartido", "Token", "Después de aprobar en el navegador, vuelve aquí y completa la conexión.", "Subtítulos desactivados", "Subtítulos"),
    "fr" to integrationStrings("Intégrations", "Clé API", "Secret partagé", "Jeton", "Après avoir approuvé dans le navigateur, revenez ici et terminez la connexion.", "Sous-titres désactivés", "Sous-titres"),
    "de" to integrationStrings("Integrationen", "API-Schlüssel", "Gemeinsames Geheimnis", "Token", "Kehre nach der Bestätigung im Browser hierher zurück und schließe die Verbindung ab.", "Untertitel aus", "Untertitel"),
    "pt" to integrationStrings("Integrações", "Chave API", "Segredo partilhado", "Token", "Depois de aprovar no navegador, volta aqui e conclui a ligação.", "Legendas desativadas", "Legendas"),
    "nl" to integrationStrings("Integraties", "API-sleutel", "Gedeeld geheim", "Token", "Kom na goedkeuring in de browser hier terug en voltooi de koppeling.", "Ondertiteling uit", "Ondertiteling"),
    "pl" to integrationStrings("Integracje", "Klucz API", "Wspólny sekret", "Token", "Po zatwierdzeniu w przeglądarce wróć tutaj i dokończ połączenie.", "Napisy wyłączone", "Napisy"),
    "ro" to integrationStrings("Integrări", "Cheie API", "Secret partajat", "Token", "După aprobarea în browser, revino aici și finalizează conectarea.", "Subtitrări dezactivate", "Subtitrări"),
    "el" to integrationStrings("Ενσωματώσεις", "Κλειδί API", "Κοινό μυστικό", "Διακριτικό", "Μετά την έγκριση στο πρόγραμμα περιήγησης, επίστρεψε εδώ και ολοκλήρωσε τη σύνδεση.", "Υπότιτλοι απενεργοποιημένοι", "Υπότιτλοι"),
    "sv" to integrationStrings("Integrationer", "API-nyckel", "Delad hemlighet", "Token", "Återvänd hit efter godkännandet i webbläsaren och slutför anslutningen.", "Undertexter av", "Undertexter"),
    "da" to integrationStrings("Integrationer", "API-nøgle", "Delt hemmelighed", "Token", "Vend tilbage hertil efter godkendelsen i browseren, og fuldfør forbindelsen.", "Undertekster fra", "Undertekster"),
    "cs" to integrationStrings("Integrace", "Klíč API", "Sdílené tajemství", "Token", "Po schválení v prohlížeči se sem vrať a dokonči propojení.", "Titulky vypnuty", "Titulky"),
    "uk" to integrationStrings("Інтеграції", "Ключ API", "Спільний секрет", "Токен", "Після підтвердження у браузері поверніться сюди та завершіть підключення.", "Субтитри вимкнено", "Субтитри"),
    "ru" to integrationStrings("Интеграции", "Ключ API", "Общий секрет", "Токен", "После подтверждения в браузере вернитесь сюда и завершите подключение.", "Субтитры выключены", "Субтитры"),
    "tr" to integrationStrings("Entegrasyonlar", "API anahtarı", "Paylaşılan gizli anahtar", "Jeton", "Tarayıcıda onayladıktan sonra buraya dönüp bağlantıyı tamamla.", "Altyazılar kapalı", "Altyazılar"),
    "ar" to integrationStrings("عمليات التكامل", "مفتاح API", "السر المشترك", "الرمز المميز", "بعد الموافقة في المتصفح، عد إلى هنا وأكمل الربط.", "الترجمة معطلة", "الترجمة"),
    "zh" to integrationStrings("集成", "API 密钥", "共享密钥", "令牌", "在浏览器中授权后，返回此处完成关联。", "字幕已关闭", "字幕"),
    "ja" to integrationStrings("連携", "API キー", "共有シークレット", "トークン", "ブラウザで承認したあと、ここに戻って接続を完了してください。", "字幕オフ", "字幕"),
    "ko" to integrationStrings("연동", "API 키", "공유 시크릿", "토큰", "브라우저에서 승인한 뒤 여기로 돌아와 연결을 완료하세요.", "자막 끔", "자막"),
    "hi" to integrationStrings("इंटीग्रेशन", "API कुंजी", "साझा सीक्रेट", "टोकन", "ब्राउज़र में मंज़ूरी देने के बाद यहाँ लौटें और कनेक्शन पूरा करें।", "उपशीर्षक बंद", "उपशीर्षक"),
    "id" to integrationStrings("Integrasi", "Kunci API", "Rahasia bersama", "Token", "Setelah menyetujui di peramban, kembali ke sini dan selesaikan koneksinya.", "Subtitel nonaktif", "Subtitel"),
    "vi" to integrationStrings("Tích hợp", "Khóa API", "Khóa bí mật dùng chung", "Mã thông báo", "Sau khi phê duyệt trong trình duyệt, hãy quay lại đây và hoàn tất kết nối.", "Tắt phụ đề", "Phụ đề"),
    "th" to integrationStrings("การเชื่อมต่อ", "คีย์ API", "ความลับที่ใช้ร่วมกัน", "โทเคน", "หลังจากอนุมัติในเบราว์เซอร์ ให้กลับมาที่นี่เพื่อเชื่อมต่อให้เสร็จ", "ปิดคำบรรยาย", "คำบรรยาย"),
    "fil" to integrationStrings("Mga integrasyon", "API key", "Shared secret", "Token", "Pagkatapos mag-aprub sa browser, bumalik dito at tapusin ang koneksyon.", "Naka-off ang subtitle", "Mga subtitle"),
    "he" to integrationStrings("שילובים", "מפתח API", "סוד משותף", "אסימון", "לאחר האישור בדפדפן, חזור לכאן והשלם את החיבור.", "כתוביות כבויות", "כתוביות")
)

internal fun integrationLocalizationEntries(code: String): Map<String, String> = integrationBundles.getValue(code)

internal fun integrationLocalizationCodes(): Set<String> = integrationBundles.keys
