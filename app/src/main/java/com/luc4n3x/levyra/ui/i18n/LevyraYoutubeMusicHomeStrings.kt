package com.luc4n3x.levyra.ui.i18n

internal data class YoutubeMusicHomeCopy(
    val title: String,
    val source: String,
    val createdByYou: String,
    val refresh: String,
    val loadFailed: String,
    val retry: String,
    val sessionExpired: String,
    val reconnect: String
)

internal fun youtubeMusicHomeCopy(code: String): YoutubeMusicHomeCopy =
    localizedValueOrEnglish(YOUTUBE_MUSIC_HOME_COPY, code).forBrazil(code) { brazilian() }

internal fun youtubeMusicHomeLocalizationCodes(): Set<String> = YOUTUBE_MUSIC_HOME_COPY.keys

private fun homeCopy(
    title: String,
    source: String,
    createdByYou: String,
    refresh: String,
    loadFailed: String,
    retry: String,
    sessionExpired: String,
    reconnect: String
) = YoutubeMusicHomeCopy(title, source, createdByYou, refresh, loadFailed, retry, sessionExpired, reconnect)

private val YOUTUBE_MUSIC_HOME_COPY = mapOf(
    "en" to homeCopy("Your music", "From your YouTube Music account", "Created by you", "Refresh", "Couldn't load your music.", "Retry", "Your YouTube Music session has expired.", "Sign in again"),
    "it" to homeCopy("La tua musica", "Dal tuo account YouTube Music", "Creata da te", "Aggiorna", "Impossibile caricare la tua musica.", "Riprova", "La sessione di YouTube Music è scaduta.", "Accedi di nuovo"),
    "es" to homeCopy("Tu música", "De tu cuenta de YouTube Music", "Creada por ti", "Actualizar", "No se pudo cargar tu música.", "Reintentar", "Tu sesión de YouTube Music ha caducado.", "Volver a iniciar sesión"),
    "fr" to homeCopy("Votre musique", "Depuis votre compte YouTube Music", "Créée par vous", "Actualiser", "Impossible de charger votre musique.", "Réessayer", "Votre session YouTube Music a expiré.", "Se reconnecter"),
    "de" to homeCopy("Deine Musik", "Aus deinem YouTube Music-Konto", "Von dir erstellt", "Aktualisieren", "Deine Musik konnte nicht geladen werden.", "Erneut versuchen", "Deine YouTube Music-Sitzung ist abgelaufen.", "Erneut anmelden"),
    "pt" to homeCopy("A tua música", "Da tua conta YouTube Music", "Criada por ti", "Atualizar", "Não foi possível carregar a tua música.", "Tentar novamente", "A tua sessão do YouTube Music expirou.", "Iniciar sessão novamente"),
    "nl" to homeCopy("Jouw muziek", "Uit je YouTube Music-account", "Door jou gemaakt", "Vernieuwen", "Je muziek kon niet worden geladen.", "Opnieuw proberen", "Je YouTube Music-sessie is verlopen.", "Opnieuw inloggen"),
    "pl" to homeCopy("Twoja muzyka", "Z Twojego konta YouTube Music", "Utworzona przez Ciebie", "Odśwież", "Nie udało się wczytać Twojej muzyki.", "Spróbuj ponownie", "Sesja YouTube Music wygasła.", "Zaloguj się ponownie"),
    "ro" to homeCopy("Muzica ta", "Din contul tău YouTube Music", "Creată de tine", "Reîmprospătează", "Muzica ta nu a putut fi încărcată.", "Reîncearcă", "Sesiunea YouTube Music a expirat.", "Conectează-te din nou"),
    "el" to homeCopy("Η μουσική σου", "Από τον λογαριασμό σου στο YouTube Music", "Δημιουργήθηκε από εσένα", "Ανανέωση", "Δεν ήταν δυνατή η φόρτωση της μουσικής σου.", "Δοκίμασε ξανά", "Η σύνδεσή σου στο YouTube Music έληξε.", "Σύνδεση ξανά"),
    "sv" to homeCopy("Din musik", "Från ditt YouTube Music-konto", "Skapad av dig", "Uppdatera", "Det gick inte att läsa in din musik.", "Försök igen", "Din YouTube Music-session har gått ut.", "Logga in igen"),
    "da" to homeCopy("Din musik", "Fra din YouTube Music-konto", "Oprettet af dig", "Opdater", "Din musik kunne ikke indlæses.", "Prøv igen", "Din YouTube Music-session er udløbet.", "Log ind igen"),
    "cs" to homeCopy("Tvoje hudba", "Z tvého účtu YouTube Music", "Vytvořeno tebou", "Obnovit", "Tvoji hudbu se nepodařilo načíst.", "Zkusit znovu", "Relace YouTube Music vypršela.", "Přihlásit se znovu"),
    "sk" to homeCopy("Tvoja hudba", "Z tvojho účtu YouTube Music", "Vytvorené tebou", "Obnoviť", "Tvoju hudbu sa nepodarilo načítať.", "Skúsiť znova", "Relácia YouTube Music vypršala.", "Prihlásiť sa znova"),
    "bn" to homeCopy("তোমার মিউজিক", "তোমার YouTube Music অ্যাকাউন্ট থেকে", "তোমার তৈরি", "রিফ্রেশ", "তোমার মিউজিক লোড করা যায়নি।", "আবার চেষ্টা করো", "তোমার YouTube Music সেশনের মেয়াদ শেষ।", "আবার সাইন ইন করো"),
    "kk" to homeCopy("Сіздің музыкаңыз", "YouTube Music аккаунтыңыздан", "Сіз жасаған", "Жаңарту", "Музыкаңызды жүктеу мүмкін болмады.", "Қайталау", "YouTube Music сеансының мерзімі бітті.", "Қайта кіру"),
    "sr" to homeCopy("Твоја музика", "Са твог YouTube Music налога", "Направио/ла си ти", "Освежи", "Није могуће учитати твоју музику.", "Покушај поново", "Твоја YouTube Music сесија је истекла.", "Пријави се поново"),
    "sq" to homeCopy("Muzika jote", "Nga llogaria jote e YouTube Music", "Krijuar nga ti", "Rifresko", "Muzika jote nuk u ngarkua.", "Provo përsëri", "Seanca jote e YouTube Music ka skaduar.", "Hyr përsëri"),
    "hr" to homeCopy("Tvoja glazba", "S tvog YouTube Music računa", "Izradio/la si ti", "Osvježi", "Nije moguće učitati tvoju glazbu.", "Pokušaj ponovno", "Tvoja YouTube Music sesija je istekla.", "Prijavi se ponovno"),
    "bg" to homeCopy("Твоята музика", "От профила ти в YouTube Music", "Създаден от теб", "Опресни", "Музиката ти не можа да се зареди.", "Опитай отново", "Сесията ти в YouTube Music е изтекла.", "Влез отново"),
    "hu" to homeCopy("A zenéd", "A YouTube Music-fiókodból", "Te hoztad létre", "Frissítés", "Nem sikerült betölteni a zenédet.", "Újra", "A YouTube Music-munkameneted lejárt.", "Jelentkezz be újra"),
    "fi" to homeCopy("Sinun musiikkisi", "YouTube Music -tililtäsi", "Sinun luomasi", "Päivitä", "Musiikkiasi ei voitu ladata.", "Yritä uudelleen", "YouTube Music -istuntosi on vanhentunut.", "Kirjaudu uudelleen"),
    "et" to homeCopy("Sinu muusika", "Sinu YouTube Musicu kontolt", "Sinu loodud", "Värskenda", "Sinu muusikat ei õnnestunud laadida.", "Proovi uuesti", "Sinu YouTube Musicu seanss on aegunud.", "Logi uuesti sisse"),
    "nb" to homeCopy("Din musikk", "Fra YouTube Music-kontoen din", "Laget av deg", "Oppdater", "Kunne ikke laste inn musikken din.", "Prøv igjen", "YouTube Music-økten din er utløpt.", "Logg på igjen"),
    "ca" to homeCopy("La teva música", "Del teu compte de YouTube Music", "Creada per tu", "Actualitza", "No s'ha pogut carregar la teva música.", "Torna-ho a provar", "La sessió de YouTube Music ha caducat.", "Torna a iniciar la sessió"),
    "uk" to homeCopy("Твоя музика", "З твого акаунта YouTube Music", "Створено тобою", "Оновити", "Не вдалося завантажити твою музику.", "Повторити", "Сеанс YouTube Music завершився.", "Увійти знову"),
    "ru" to homeCopy("Ваша музыка", "Из вашего аккаунта YouTube Music", "Создано вами", "Обновить", "Не удалось загрузить вашу музыку.", "Повторить", "Сеанс YouTube Music истёк.", "Войти снова"),
    "tr" to homeCopy("Müziğin", "YouTube Music hesabından", "Senin oluşturduğun", "Yenile", "Müziğin yüklenemedi.", "Tekrar dene", "YouTube Music oturumunun süresi doldu.", "Tekrar oturum aç"),
    "ar" to homeCopy("موسيقاك", "من حسابك على YouTube Music", "أنشأتها أنت", "تحديث", "تعذّر تحميل موسيقاك.", "إعادة المحاولة", "انتهت جلستك في YouTube Music.", "سجّل الدخول مجددًا"),
    "fa" to homeCopy("موسیقی تو", "از حساب YouTube Music تو", "ساخته‌شده توسط تو", "به‌روزرسانی", "بارگیری موسیقی تو ممکن نشد.", "تلاش دوباره", "نشست YouTube Music تو منقضی شده است.", "ورود دوباره"),
    "zh" to homeCopy("你的音乐", "来自你的 YouTube Music 账号", "由你创建", "刷新", "无法加载你的音乐。", "重试", "你的 YouTube Music 会话已过期。", "重新登录"),
    "zh-Hant" to homeCopy("你的音樂", "來自你的 YouTube Music 帳戶", "由你建立", "重新整理", "無法載入你的音樂。", "重試", "你的 YouTube Music 工作階段已過期。", "重新登入"),
    "ja" to homeCopy("あなたの音楽", "YouTube Music アカウントから", "あなたが作成", "更新", "音楽を読み込めませんでした。", "再試行", "YouTube Music のセッションの有効期限が切れました。", "もう一度ログイン"),
    "ko" to homeCopy("내 음악", "내 YouTube Music 계정에서", "내가 만든 항목", "새로고침", "음악을 불러오지 못했습니다.", "다시 시도", "YouTube Music 세션이 만료되었습니다.", "다시 로그인"),
    "hi" to homeCopy("आपका संगीत", "आपके YouTube Music खाते से", "आपके द्वारा बनाई गई", "रीफ़्रेश करें", "आपका संगीत लोड नहीं हो सका।", "फिर से कोशिश करें", "आपका YouTube Music सत्र समाप्त हो गया है।", "फिर से साइन इन करें"),
    "id" to homeCopy("Musikmu", "Dari akun YouTube Music-mu", "Dibuat olehmu", "Muat ulang", "Musikmu tidak dapat dimuat.", "Coba lagi", "Sesi YouTube Music-mu telah berakhir.", "Masuk lagi"),
    "ms" to homeCopy("Muzik anda", "Daripada akaun YouTube Music anda", "Dicipta oleh anda", "Muat semula", "Muzik anda tidak dapat dimuatkan.", "Cuba lagi", "Sesi YouTube Music anda telah tamat.", "Log masuk semula"),
    "vi" to homeCopy("Nhạc của bạn", "Từ tài khoản YouTube Music của bạn", "Do bạn tạo", "Làm mới", "Không thể tải nhạc của bạn.", "Thử lại", "Phiên YouTube Music của bạn đã hết hạn.", "Đăng nhập lại"),
    "th" to homeCopy("เพลงของคุณ", "จากบัญชี YouTube Music ของคุณ", "สร้างโดยคุณ", "รีเฟรช", "โหลดเพลงของคุณไม่ได้", "ลองอีกครั้ง", "เซสชัน YouTube Music ของคุณหมดอายุแล้ว", "ลงชื่อเข้าใช้อีกครั้ง"),
    "fil" to homeCopy("Ang musika mo", "Mula sa YouTube Music account mo", "Ginawa mo", "I-refresh", "Hindi ma-load ang musika mo.", "Subukang muli", "Nag-expire na ang session mo sa YouTube Music.", "Mag-sign in muli"),
    "he" to homeCopy("המוזיקה שלך", "מחשבון YouTube Music שלך", "נוצר על ידך", "רענון", "לא ניתן לטעון את המוזיקה שלך.", "ניסיון חוזר", "פג תוקף ההתחברות שלך ל-YouTube Music.", "התחברות מחדש")
)

private fun YoutubeMusicHomeCopy.brazilian(): YoutubeMusicHomeCopy = copy(
    title = LevyraBrazilianPortuguese.adapt(title),
    source = LevyraBrazilianPortuguese.adapt(source),
    createdByYou = LevyraBrazilianPortuguese.adapt(createdByYou),
    refresh = LevyraBrazilianPortuguese.adapt(refresh),
    loadFailed = LevyraBrazilianPortuguese.adapt(loadFailed),
    retry = LevyraBrazilianPortuguese.adapt(retry),
    sessionExpired = LevyraBrazilianPortuguese.adapt(sessionExpired),
    reconnect = LevyraBrazilianPortuguese.adapt(reconnect)
)
