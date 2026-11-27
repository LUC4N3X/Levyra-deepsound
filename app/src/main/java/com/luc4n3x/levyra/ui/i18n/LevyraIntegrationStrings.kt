package com.luc4n3x.levyra.ui.i18n

private fun integrationStrings(
    integrations: String,
    apiKey: String,
    sharedSecret: String,
    token: String,
    lastFmApprovalHint: String,
    subtitlesOff: String,
    subtitles: String,
    lastFmConnected: String
): Map<String, String> = mapOf(
    "integrations" to integrations,
    "apiKeyLabel" to apiKey,
    "sharedSecretLabel" to sharedSecret,
    "credentialTokenLabel" to token,
    "lastFmApprovalHint" to lastFmApprovalHint,
    "subtitlesOff" to subtitlesOff,
    "subtitlesLabel" to subtitles,
    "lastFmConnected" to lastFmConnected
)

private val integrationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to integrationStrings("Integrations", "API key", "Shared secret", "Token", "After approving in the browser, return here and complete the connection.", "Subtitles off", "Subtitles", "Connected. Your plays are scrobbled to Last.fm."),
    "it" to integrationStrings("Integrazioni", "Chiave API", "Segreto condiviso", "Token", "Dopo l'approvazione nel browser, torna qui e completa il collegamento.", "Sottotitoli disattivati", "Sottotitoli", "Collegato. I tuoi ascolti vengono registrati su Last.fm."),
    "es" to integrationStrings("Integraciones", "Clave API", "Secreto compartido", "Token", "Después de aprobar en el navegador, vuelve aquí y completa la conexión.", "Subtítulos desactivados", "Subtítulos", "Conectado. Tus reproducciones se registran en Last.fm."),
    "fr" to integrationStrings("Intégrations", "Clé API", "Secret partagé", "Jeton", "Après avoir approuvé dans le navigateur, revenez ici et terminez la connexion.", "Sous-titres désactivés", "Sous-titres", "Connecté. Vos écoutes sont scrobblées sur Last.fm."),
    "de" to integrationStrings("Integrationen", "API-Schlüssel", "Gemeinsames Geheimnis", "Token", "Kehre nach der Bestätigung im Browser hierher zurück und schließe die Verbindung ab.", "Untertitel aus", "Untertitel", "Verbunden. Deine Wiedergaben werden an Last.fm gescrobbelt."),
    "pt" to integrationStrings("Integrações", "Chave API", "Segredo partilhado", "Token", "Depois de aprovar no navegador, volta aqui e conclui a ligação.", "Legendas desativadas", "Legendas", "Ligado. As tuas reproduções são registadas no Last.fm."),
    "nl" to integrationStrings("Integraties", "API-sleutel", "Gedeeld geheim", "Token", "Kom na goedkeuring in de browser hier terug en voltooi de koppeling.", "Ondertiteling uit", "Ondertiteling", "Verbonden. Je afspeelgeschiedenis wordt naar Last.fm gescrobbeld."),
    "pl" to integrationStrings("Integracje", "Klucz API", "Wspólny sekret", "Token", "Po zatwierdzeniu w przeglądarce wróć tutaj i dokończ połączenie.", "Napisy wyłączone", "Napisy", "Połączono. Twoje odsłuchania są scrobblowane do Last.fm."),
    "ro" to integrationStrings("Integrări", "Cheie API", "Secret partajat", "Token", "După aprobarea în browser, revino aici și finalizează conectarea.", "Subtitrări dezactivate", "Subtitrări", "Conectat. Redările tale sunt înregistrate pe Last.fm."),
    "el" to integrationStrings("Ενσωματώσεις", "Κλειδί API", "Κοινό μυστικό", "Διακριτικό", "Μετά την έγκριση στο πρόγραμμα περιήγησης, επίστρεψε εδώ και ολοκλήρωσε τη σύνδεση.", "Υπότιτλοι απενεργοποιημένοι", "Υπότιτλοι", "Συνδέθηκε. Οι ακροάσεις σου καταγράφονται στο Last.fm."),
    "sv" to integrationStrings("Integrationer", "API-nyckel", "Delad hemlighet", "Token", "Återvänd hit efter godkännandet i webbläsaren och slutför anslutningen.", "Undertexter av", "Undertexter", "Ansluten. Dina lyssningar scrobblas till Last.fm."),
    "da" to integrationStrings("Integrationer", "API-nøgle", "Delt hemmelighed", "Token", "Vend tilbage hertil efter godkendelsen i browseren, og fuldfør forbindelsen.", "Undertekster fra", "Undertekster", "Forbundet. Dine afspilninger scrobbles til Last.fm."),
    "cs" to integrationStrings("Integrace", "Klíč API", "Sdílené tajemství", "Token", "Po schválení v prohlížeči se sem vrať a dokonči propojení.", "Titulky vypnuty", "Titulky", "Připojeno. Tvá přehrání se scrobblují na Last.fm."),
    "uk" to integrationStrings("Інтеграції", "Ключ API", "Спільний секрет", "Токен", "Після підтвердження у браузері поверніться сюди та завершіть підключення.", "Субтитри вимкнено", "Субтитри", "Підключено. Ваші прослуховування скроблюються в Last.fm."),
    "ru" to integrationStrings("Интеграции", "Ключ API", "Общий секрет", "Токен", "После подтверждения в браузере вернитесь сюда и завершите подключение.", "Субтитры выключены", "Субтитры", "Подключено. Ваши прослушивания скробблятся в Last.fm."),
    "tr" to integrationStrings("Entegrasyonlar", "API anahtarı", "Paylaşılan gizli anahtar", "Jeton", "Tarayıcıda onayladıktan sonra buraya dönüp bağlantıyı tamamla.", "Altyazılar kapalı", "Altyazılar", "Bağlandı. Dinlediklerin Last.fm'e scrobble ediliyor."),
    "ar" to integrationStrings("عمليات التكامل", "مفتاح API", "السر المشترك", "الرمز المميز", "بعد الموافقة في المتصفح، عد إلى هنا وأكمل الربط.", "الترجمة معطلة", "الترجمة", "متصل. يتم تسجيل ما تستمع إليه في Last.fm."),
    "zh" to integrationStrings("集成", "API 密钥", "共享密钥", "令牌", "在浏览器中授权后，返回此处完成关联。", "字幕已关闭", "字幕", "已连接。你的播放记录会同步到 Last.fm。"),
    "ja" to integrationStrings("連携", "API キー", "共有シークレット", "トークン", "ブラウザで承認したあと、ここに戻って接続を完了してください。", "字幕オフ", "字幕", "接続済み。再生履歴は Last.fm に記録されます。"),
    "ko" to integrationStrings("연동", "API 키", "공유 시크릿", "토큰", "브라우저에서 승인한 뒤 여기로 돌아와 연결을 완료하세요.", "자막 끔", "자막", "연결됨. 재생 기록이 Last.fm에 스크로블됩니다."),
    "hi" to integrationStrings("इंटीग्रेशन", "API कुंजी", "साझा सीक्रेट", "टोकन", "ब्राउज़र में मंज़ूरी देने के बाद यहाँ लौटें और कनेक्शन पूरा करें।", "उपशीर्षक बंद", "उपशीर्षक", "कनेक्ट हो गया। आपके प्ले Last.fm पर स्क्रॉबल होते हैं।"),
    "id" to integrationStrings("Integrasi", "Kunci API", "Rahasia bersama", "Token", "Setelah menyetujui di peramban, kembali ke sini dan selesaikan koneksinya.", "Subtitel nonaktif", "Subtitel", "Terhubung. Pemutaranmu di-scrobble ke Last.fm."),
    "vi" to integrationStrings("Tích hợp", "Khóa API", "Khóa bí mật dùng chung", "Mã thông báo", "Sau khi phê duyệt trong trình duyệt, hãy quay lại đây và hoàn tất kết nối.", "Tắt phụ đề", "Phụ đề", "Đã kết nối. Lượt nghe của bạn được ghi lên Last.fm."),
    "th" to integrationStrings("การเชื่อมต่อ", "คีย์ API", "ความลับที่ใช้ร่วมกัน", "โทเคน", "หลังจากอนุมัติในเบราว์เซอร์ ให้กลับมาที่นี่เพื่อเชื่อมต่อให้เสร็จ", "ปิดคำบรรยาย", "คำบรรยาย", "เชื่อมต่อแล้ว การฟังของคุณจะถูกบันทึกไปยัง Last.fm"),
    "fil" to integrationStrings("Mga integrasyon", "API key", "Shared secret", "Token", "Pagkatapos mag-aprub sa browser, bumalik dito at tapusin ang koneksyon.", "Naka-off ang subtitle", "Mga subtitle", "Nakakonekta. Naisi-scrobble sa Last.fm ang mga pinapakinggan mo."),
    "he" to integrationStrings("שילובים", "מפתח API", "סוד משותף", "אסימון", "לאחר האישור בדפדפן, חזור לכאן והשלם את החיבור.", "כתוביות כבויות", "כתוביות", "מחובר. ההאזנות שלך נרשמות ב־Last.fm."),
    "fi" to integrationStrings("Integraatiot", "API-avain", "Jaettu salaisuus", "Tunniste", "Kun olet hyväksynyt selaimessa, palaa tänne ja viimeistele yhteys.", "Tekstitys pois", "Tekstitys", "Yhdistetty. Kuuntelusi scrobblataan Last.fm:ään."),
    "et" to integrationStrings("Integratsioonid", "API-võti", "Jagatud saladus", "Pääsutõend", "Pärast brauseris kinnitamist naase siia ja vii ühendamine lõpule.", "Subtiitrid väljas", "Subtiitrid", "Ühendatud. Sinu kuulamised salvestatakse Last.fm-i."),
    "ur" to integrationStrings("انضمام", "API کلید", "مشترکہ راز", "ٹوکن", "براؤزر میں منظوری دینے کے بعد، یہاں واپس جائیں اور کنکشن مکمل کریں۔", "سب ٹائٹلز بند", "سب ٹائٹلز", "جڑا ہوا آپ کے ڈراموں کو Last.fm پر سکروب کیا جاتا ہے۔"),
    "ta" to integrationStrings("ஒருங்கிணைப்புகள்", "API விசை", "பகிரப்பட்ட ரகசியம்", "டோக்கன்", "உலாவியில் ஒப்புதல் அளித்த பிறகு, இங்கே திரும்பி இணைப்பை முடிக்கவும்.", "வசன வரிகள் முடக்கப்பட்டுள்ளன", "வசன வரிகள்", "இணைக்கப்பட்டது. உங்கள் நாடகங்கள் Last.fm க்கு ஸ்க்ரோப் செய்யப்பட்டன."),
    "te" to integrationStrings("ఇంటిగ్రేషన్లు", "API కీ", "రహస్యంగా పంచుకున్నారు", "టోకెన్", "బ్రౌజర్‌లో ఆమోదించిన తర్వాత, ఇక్కడకు తిరిగి వచ్చి కనెక్షన్‌ని పూర్తి చేయండి.", "ఉపశీర్షికలు ఆఫ్", "ఉపశీర్షికలు", "కనెక్ట్ చేయబడింది. మీ నాటకాలు Last.fmకి స్క్రాబుల్ చేయబడ్డాయి."),
    "mr" to integrationStrings("एकत्रीकरण", "API की", "गुपित शेअर केले", "टोकन", "ब्राउझरमध्ये मंजूरी दिल्यानंतर, येथे परत या आणि कनेक्शन पूर्ण करा.", "उपशीर्षके बंद", "उपशीर्षके", "जोडलेले. तुमची नाटके Last.fm वर स्क्रॉबल केलेली आहेत."),
    "gu" to integrationStrings("એકીકરણ", "API કી", "વહેંચાયેલ રહસ્ય", "ટોકન", "બ્રાઉઝરમાં મંજૂરી આપ્યા પછી, અહીં પાછા આવો અને કનેક્શન પૂર્ણ કરો.", "સબટાઈટલ બંધ", "સબટાઈટલ", "કનેક્ટેડ. તમારા નાટકો Last.fm પર સ્ક્રૉબલ કરવામાં આવે છે."),
    "pt-BR" to integrationStrings("Integrações", "Chave API", "Segredo compartilhado", "Símbolo", "Após aprovar no navegador, volte aqui e finalize a conexão.", "Legendas desativadas", "Legendas", "Conectado. Suas jogadas são codificadas para Last.fm.")
)

internal fun integrationLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(integrationBundles, code)

internal fun integrationLocalizationCodes(): Set<String> = supportedLocalizationCodes()
