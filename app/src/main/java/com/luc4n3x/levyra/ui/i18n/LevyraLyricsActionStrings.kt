package com.luc4n3x.levyra.ui.i18n

private fun lyricsActions(
    change: String,
    automatic: String,
    select: String,
    copy: String,
    share: String,
    versions: String
): Map<String, String> = mapOf(
    "changeLyrics" to change,
    "automaticLyrics" to automatic,
    "selectVerses" to select,
    "copyVerses" to copy,
    "shareVerses" to share,
    "lyricsVersions" to versions
)

private val lyricsActionBundles: Map<String, Map<String, String>> = mapOf(
    "en" to lyricsActions("Change lyrics", "Automatic", "Select verses", "Copy", "Share", "Lyrics versions"),
    "it" to lyricsActions("Cambia testo", "Automatico", "Seleziona versi", "Copia", "Condividi", "Versioni del testo"),
    "es" to lyricsActions("Cambiar letra", "Automático", "Seleccionar versos", "Copiar", "Compartir", "Versiones de la letra"),
    "fr" to lyricsActions("Changer les paroles", "Automatique", "Sélectionner des lignes", "Copier", "Partager", "Versions des paroles"),
    "de" to lyricsActions("Liedtext ändern", "Automatisch", "Zeilen auswählen", "Kopieren", "Teilen", "Liedtextversionen"),
    "pt" to lyricsActions("Alterar letra", "Automático", "Selecionar versos", "Copiar", "Compartilhar", "Versões da letra"),
    "nl" to lyricsActions("Songtekst wijzigen", "Automatisch", "Regels selecteren", "Kopiëren", "Delen", "Songtekstversies"),
    "pl" to lyricsActions("Zmień tekst", "Automatycznie", "Wybierz wersy", "Kopiuj", "Udostępnij", "Wersje tekstu"),
    "ro" to lyricsActions("Schimbă versurile", "Automat", "Selectează versuri", "Copiază", "Distribuie", "Versiuni ale versurilor"),
    "el" to lyricsActions("Αλλαγή στίχων", "Αυτόματα", "Επιλογή στίχων", "Αντιγραφή", "Κοινοποίηση", "Εκδόσεις στίχων"),
    "sv" to lyricsActions("Byt låttext", "Automatiskt", "Välj textrader", "Kopiera", "Dela", "Låttextversioner"),
    "da" to lyricsActions("Skift sangtekst", "Automatisk", "Vælg tekstlinjer", "Kopiér", "Del", "Sangtekstversioner"),
    "cs" to lyricsActions("Změnit text", "Automaticky", "Vybrat řádky", "Kopírovat", "Sdílet", "Verze textu"),
    "uk" to lyricsActions("Змінити текст", "Автоматично", "Вибрати рядки", "Копіювати", "Поділитися", "Версії тексту"),
    "ru" to lyricsActions("Изменить текст", "Автоматически", "Выбрать строки", "Копировать", "Поделиться", "Версии текста"),
    "tr" to lyricsActions("Şarkı sözünü değiştir", "Otomatik", "Dizeleri seç", "Kopyala", "Paylaş", "Şarkı sözü sürümleri"),
    "ar" to lyricsActions("تغيير الكلمات", "تلقائي", "تحديد المقاطع", "نسخ", "مشاركة", "إصدارات الكلمات"),
    "zh" to lyricsActions("更换歌词", "自动", "选择歌词行", "复制", "分享", "歌词版本"),
    "ja" to lyricsActions("歌詞を変更", "自動", "歌詞行を選択", "コピー", "共有", "歌詞のバージョン"),
    "ko" to lyricsActions("가사 변경", "자동", "가사 줄 선택", "복사", "공유", "가사 버전"),
    "hi" to lyricsActions("गीत के बोल बदलें", "स्वचालित", "पंक्तियाँ चुनें", "कॉपी करें", "साझा करें", "गीत के बोल के संस्करण"),
    "id" to lyricsActions("Ganti lirik", "Otomatis", "Pilih baris", "Salin", "Bagikan", "Versi lirik"),
    "vi" to lyricsActions("Đổi lời bài hát", "Tự động", "Chọn câu hát", "Sao chép", "Chia sẻ", "Các phiên bản lời bài hát"),
    "th" to lyricsActions("เปลี่ยนเนื้อเพลง", "อัตโนมัติ", "เลือกท่อนเพลง", "คัดลอก", "แชร์", "เวอร์ชันเนื้อเพลง"),
    "fil" to lyricsActions("Palitan ang liriko", "Awtomatiko", "Pumili ng mga linya", "Kopyahin", "Ibahagi", "Mga bersyon ng liriko"),
    "he" to lyricsActions("שינוי מילות השיר", "אוטומטי", "בחירת שורות", "העתקה", "שיתוף", "גרסאות של מילות השיר"),
    "fi" to lyricsActions("Vaihda sanoitukset", "Automaattinen", "Valitse säkeet", "Kopioi", "Jaa", "Sanoitusversiot"),
    "et" to lyricsActions("Muuda sõnu", "Automaatne", "Vali salmid", "Kopeeri", "Jaga", "Sõnade versioonid")
)

private val lyricsOffsetBundles: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "lyricsOffsetEarlier" to "Lyrics 0.5 seconds earlier",
        "lyricsOffsetLater" to "Lyrics 0.5 seconds later",
        "lyricsOffsetReset" to "Reset lyrics timing"
    ),
    "it" to mapOf(
        "lyricsOffsetEarlier" to "Testo 0,5 secondi prima",
        "lyricsOffsetLater" to "Testo 0,5 secondi dopo",
        "lyricsOffsetReset" to "Ripristina il sync del testo"
    ),
    "es" to mapOf(
        "lyricsOffsetEarlier" to "Letra 0,5 segundos antes",
        "lyricsOffsetLater" to "Letra 0,5 segundos después",
        "lyricsOffsetReset" to "Restablecer el ajuste de la letra"
    ),
    "fr" to mapOf(
        "lyricsOffsetEarlier" to "Paroles 0,5 seconde plus tôt",
        "lyricsOffsetLater" to "Paroles 0,5 seconde plus tard",
        "lyricsOffsetReset" to "Réinitialiser le calage des paroles"
    ),
    "de" to mapOf(
        "lyricsOffsetEarlier" to "Liedtext 0,5 Sekunden früher",
        "lyricsOffsetLater" to "Liedtext 0,5 Sekunden später",
        "lyricsOffsetReset" to "Liedtext-Timing zurücksetzen"
    ),
    "pt" to mapOf(
        "lyricsOffsetEarlier" to "Letra 0,5 segundos antes",
        "lyricsOffsetLater" to "Letra 0,5 segundos depois",
        "lyricsOffsetReset" to "Repor a sincronização da letra"
    ),
    "nl" to mapOf(
        "lyricsOffsetEarlier" to "Songtekst 0,5 seconde eerder",
        "lyricsOffsetLater" to "Songtekst 0,5 seconde later",
        "lyricsOffsetReset" to "Songtekst-timing resetten"
    ),
    "pl" to mapOf(
        "lyricsOffsetEarlier" to "Tekst 0,5 s wcześniej",
        "lyricsOffsetLater" to "Tekst 0,5 s później",
        "lyricsOffsetReset" to "Zresetuj synchronizację tekstu"
    ),
    "ro" to mapOf(
        "lyricsOffsetEarlier" to "Versurile cu 0,5 secunde mai devreme",
        "lyricsOffsetLater" to "Versurile cu 0,5 secunde mai târziu",
        "lyricsOffsetReset" to "Resetează sincronizarea versurilor"
    ),
    "el" to mapOf(
        "lyricsOffsetEarlier" to "Στίχοι 0,5 δευτερόλεπτα νωρίτερα",
        "lyricsOffsetLater" to "Στίχοι 0,5 δευτερόλεπτα αργότερα",
        "lyricsOffsetReset" to "Επαναφορά χρονισμού στίχων"
    ),
    "sv" to mapOf(
        "lyricsOffsetEarlier" to "Texten 0,5 sekunder tidigare",
        "lyricsOffsetLater" to "Texten 0,5 sekunder senare",
        "lyricsOffsetReset" to "Återställ textens timing"
    ),
    "da" to mapOf(
        "lyricsOffsetEarlier" to "Teksten 0,5 sekunder tidligere",
        "lyricsOffsetLater" to "Teksten 0,5 sekunder senere",
        "lyricsOffsetReset" to "Nulstil tekstens timing"
    ),
    "cs" to mapOf(
        "lyricsOffsetEarlier" to "Text o 0,5 sekundy dříve",
        "lyricsOffsetLater" to "Text o 0,5 sekundy později",
        "lyricsOffsetReset" to "Obnovit načasování textu"
    ),
    "uk" to mapOf(
        "lyricsOffsetEarlier" to "Текст на 0,5 с раніше",
        "lyricsOffsetLater" to "Текст на 0,5 с пізніше",
        "lyricsOffsetReset" to "Скинути синхронізацію тексту"
    ),
    "ru" to mapOf(
        "lyricsOffsetEarlier" to "Текст на 0,5 с раньше",
        "lyricsOffsetLater" to "Текст на 0,5 с позже",
        "lyricsOffsetReset" to "Сбросить синхронизацию текста"
    ),
    "tr" to mapOf(
        "lyricsOffsetEarlier" to "Sözler 0,5 saniye önce",
        "lyricsOffsetLater" to "Sözler 0,5 saniye sonra",
        "lyricsOffsetReset" to "Şarkı sözü zamanlamasını sıfırla"
    ),
    "ar" to mapOf(
        "lyricsOffsetEarlier" to "الكلمات قبل 0.5 ثانية",
        "lyricsOffsetLater" to "الكلمات بعد 0.5 ثانية",
        "lyricsOffsetReset" to "إعادة ضبط توقيت الكلمات"
    ),
    "zh" to mapOf(
        "lyricsOffsetEarlier" to "歌词提前 0.5 秒",
        "lyricsOffsetLater" to "歌词延后 0.5 秒",
        "lyricsOffsetReset" to "重置歌词时间"
    ),
    "ja" to mapOf(
        "lyricsOffsetEarlier" to "歌詞を 0.5 秒早める",
        "lyricsOffsetLater" to "歌詞を 0.5 秒遅らせる",
        "lyricsOffsetReset" to "歌詞のタイミングをリセット"
    ),
    "ko" to mapOf(
        "lyricsOffsetEarlier" to "가사 0.5초 앞당기기",
        "lyricsOffsetLater" to "가사 0.5초 늦추기",
        "lyricsOffsetReset" to "가사 타이밍 초기화"
    ),
    "hi" to mapOf(
        "lyricsOffsetEarlier" to "बोल 0.5 सेकंड पहले",
        "lyricsOffsetLater" to "बोल 0.5 सेकंड बाद में",
        "lyricsOffsetReset" to "बोल का टाइमिंग रीसेट करें"
    ),
    "id" to mapOf(
        "lyricsOffsetEarlier" to "Lirik 0,5 detik lebih awal",
        "lyricsOffsetLater" to "Lirik 0,5 detik lebih lambat",
        "lyricsOffsetReset" to "Atur ulang waktu lirik"
    ),
    "vi" to mapOf(
        "lyricsOffsetEarlier" to "Lời sớm hơn 0,5 giây",
        "lyricsOffsetLater" to "Lời trễ hơn 0,5 giây",
        "lyricsOffsetReset" to "Đặt lại thời gian lời bài hát"
    ),
    "th" to mapOf(
        "lyricsOffsetEarlier" to "เนื้อเพลงเร็วขึ้น 0.5 วินาที",
        "lyricsOffsetLater" to "เนื้อเพลงช้าลง 0.5 วินาที",
        "lyricsOffsetReset" to "รีเซ็ตการจับจังหวะเนื้อเพลง"
    ),
    "fil" to mapOf(
        "lyricsOffsetEarlier" to "Liriko 0.5 segundo mas maaga",
        "lyricsOffsetLater" to "Liriko 0.5 segundo mas huli",
        "lyricsOffsetReset" to "I-reset ang timing ng liriko"
    ),
    "he" to mapOf(
        "lyricsOffsetEarlier" to "מילות השיר 0.5 שניות מוקדם יותר",
        "lyricsOffsetLater" to "מילות השיר 0.5 שניות מאוחר יותר",
        "lyricsOffsetReset" to "איפוס תזמון מילות השיר"
    ),
    "sk" to mapOf(
        "lyricsOffsetEarlier" to "Text o 0,5 sekundy skôr",
        "lyricsOffsetLater" to "Text o 0,5 sekundy neskôr",
        "lyricsOffsetReset" to "Obnoviť načasovanie textu"
    ),
    "bn" to mapOf(
        "lyricsOffsetEarlier" to "গানের কথা 0.5 সেকেন্ড আগে",
        "lyricsOffsetLater" to "গানের কথা 0.5 সেকেন্ড পরে",
        "lyricsOffsetReset" to "গানের কথার সময় সরণ রিসেট করুন"
    ),
    "kk" to mapOf(
        "lyricsOffsetEarlier" to "Мәтін 0,5 секунд ертерек",
        "lyricsOffsetLater" to "Мәтін 0,5 секунд кешірек",
        "lyricsOffsetReset" to "Мәтін уақыт ығысуын қалпына келтіру"
    ),
    "sr" to mapOf(
        "lyricsOffsetEarlier" to "Текст 0,5 секунди раније",
        "lyricsOffsetLater" to "Текст 0,5 секунди касније",
        "lyricsOffsetReset" to "Поништи временски помак текста"
    ),
    "sq" to mapOf(
        "lyricsOffsetEarlier" to "Teksti 0,5 sekonda më herët",
        "lyricsOffsetLater" to "Teksti 0,5 sekonda më vonë",
        "lyricsOffsetReset" to "Rivendos zhvendosjen kohore të tekstit"
    ),
    "hr" to mapOf(
        "lyricsOffsetEarlier" to "Stihovi 0,5 sekunde ranije",
        "lyricsOffsetLater" to "Stihovi 0,5 sekunde kasnije",
        "lyricsOffsetReset" to "Poništi vremenski pomak stihova"
    ),
    "bg" to mapOf(
        "lyricsOffsetEarlier" to "Текст 0,5 с по-рано",
        "lyricsOffsetLater" to "Текст 0,5 с по-късно",
        "lyricsOffsetReset" to "Нулиране на синхронизацията на текста"
    ),
    "hu" to mapOf(
        "lyricsOffsetEarlier" to "Dalszöveg 0,5 másodperccel korábban",
        "lyricsOffsetLater" to "Dalszöveg 0,5 másodperccel később",
        "lyricsOffsetReset" to "Dalszöveg időzítésének visszaállítása"
    ),
    "fi" to mapOf(
        "lyricsOffsetEarlier" to "Sanoitukset 0,5 sekuntia aikaisemmin",
        "lyricsOffsetLater" to "Sanoitukset 0,5 sekuntia myöhemmin",
        "lyricsOffsetReset" to "Nollaa sanoitusten ajoitus"
    ),
    "et" to mapOf(
        "lyricsOffsetEarlier" to "Sõnad 0,5 sekundit varem",
        "lyricsOffsetLater" to "Sõnad 0,5 sekundit hiljem",
        "lyricsOffsetReset" to "Lähtesta sõnade ajastus"
    ),
    "nb" to mapOf(
        "lyricsOffsetEarlier" to "Teksten 0,5 sekunder tidligere",
        "lyricsOffsetLater" to "Teksten 0,5 sekunder senere",
        "lyricsOffsetReset" to "Tilbakestill tekstens tidsjustering"
    ),
    "ca" to mapOf(
        "lyricsOffsetEarlier" to "Lletra 0,5 segons abans",
        "lyricsOffsetLater" to "Lletra 0,5 segons després",
        "lyricsOffsetReset" to "Restableix la sincronització de la lletra"
    ),
    "ms" to mapOf(
        "lyricsOffsetEarlier" to "Lirik 0.5 saat lebih awal",
        "lyricsOffsetLater" to "Lirik 0.5 saat lebih lambat",
        "lyricsOffsetReset" to "Tetapkan semula masa lirik"
    ),
    "fa" to mapOf(
        "lyricsOffsetEarlier" to "متن ترانه 0.5 ثانیه زودتر",
        "lyricsOffsetLater" to "متن ترانه 0.5 ثانیه دیرتر",
        "lyricsOffsetReset" to "بازنشانی زمان‌بندی متن ترانه"
    ),
    "zh-Hant" to mapOf(
        "lyricsOffsetEarlier" to "歌詞提前 0.5 秒",
        "lyricsOffsetLater" to "歌詞延後 0.5 秒",
        "lyricsOffsetReset" to "重設歌詞時間"
    )
)

private fun lyricsTranslationStatus(
    working: String,
    downloadRequired: String,
    downloading: String,
    unavailable: String,
    failed: String
): Map<String, String> = mapOf(
    "lyricsTranslationWorking" to working,
    "lyricsTranslationDownloadRequired" to downloadRequired,
    "lyricsTranslationDownloading" to downloading,
    "lyricsTranslationUnavailable" to unavailable,
    "lyricsTranslationFailed" to failed
)

private val lyricsTranslationStatusBundles: Map<String, Map<String, String>> = mapOf(
    "en" to lyricsTranslationStatus("Translating lyrics…", "Translation language not downloaded · Tap to get it", "Downloading translation language…", "Translation isn't available for this language on this device", "Translation failed · Tap to retry"),
    "it" to lyricsTranslationStatus("Traduzione del testo in corso…", "Lingua di traduzione non scaricata · Tocca per ottenerla", "Download della lingua di traduzione…", "Traduzione non disponibile per questa lingua su questo dispositivo", "Traduzione non riuscita · Tocca per riprovare"),
    "es" to lyricsTranslationStatus("Traduciendo la letra…", "Idioma de traducción no descargado · Toca para obtenerlo", "Descargando el idioma de traducción…", "La traducción no está disponible para este idioma en este dispositivo", "La traducción falló · Toca para reintentar"),
    "fr" to lyricsTranslationStatus("Traduction des paroles…", "Langue de traduction non téléchargée · Touchez pour l'obtenir", "Téléchargement de la langue de traduction…", "Traduction indisponible pour cette langue sur cet appareil", "Échec de la traduction · Touchez pour réessayer"),
    "de" to lyricsTranslationStatus("Liedtext wird übersetzt…", "Übersetzungssprache nicht heruntergeladen · Zum Laden tippen", "Übersetzungssprache wird heruntergeladen…", "Übersetzung für diese Sprache auf diesem Gerät nicht verfügbar", "Übersetzung fehlgeschlagen · Zum Wiederholen tippen"),
    "pt" to lyricsTranslationStatus("Traduzindo a letra…", "Idioma de tradução não baixado · Toque para obter", "Baixando o idioma de tradução…", "Tradução indisponível para este idioma neste dispositivo", "A tradução falhou · Toque para tentar novamente"),
    "nl" to lyricsTranslationStatus("Songtekst wordt vertaald…", "Vertaaltaal niet gedownload · Tik om te downloaden", "Vertaaltaal wordt gedownload…", "Vertaling niet beschikbaar voor deze taal op dit apparaat", "Vertaling mislukt · Tik om opnieuw te proberen"),
    "pl" to lyricsTranslationStatus("Tłumaczenie tekstu…", "Język tłumaczenia nie jest pobrany · Dotknij, aby pobrać", "Pobieranie języka tłumaczenia…", "Tłumaczenie tego języka jest niedostępne na tym urządzeniu", "Tłumaczenie nie powiodło się · Dotknij, aby ponowić"),
    "ro" to lyricsTranslationStatus("Se traduc versurile…", "Limba de traducere nu este descărcată · Atinge pentru a o obține", "Se descarcă limba de traducere…", "Traducerea nu este disponibilă pentru această limbă pe acest dispozitiv", "Traducerea a eșuat · Atinge pentru a reîncerca"),
    "el" to lyricsTranslationStatus("Μετάφραση στίχων…", "Η γλώσσα μετάφρασης δεν έχει ληφθεί · Πατήστε για λήψη", "Λήψη γλώσσας μετάφρασης…", "Η μετάφραση δεν είναι διαθέσιμη για αυτή τη γλώσσα σε αυτή τη συσκευή", "Η μετάφραση απέτυχε · Πατήστε για επανάληψη"),
    "sv" to lyricsTranslationStatus("Översätter låttexten…", "Översättningsspråket är inte nedladdat · Tryck för att hämta", "Laddar ned översättningsspråk…", "Översättning är inte tillgänglig för det här språket på den här enheten", "Översättningen misslyckades · Tryck för att försöka igen"),
    "da" to lyricsTranslationStatus("Oversætter sangteksten…", "Oversættelsessproget er ikke downloadet · Tryk for at hente det", "Downloader oversættelsessprog…", "Oversættelse er ikke tilgængelig for dette sprog på denne enhed", "Oversættelsen mislykkedes · Tryk for at prøve igen"),
    "cs" to lyricsTranslationStatus("Překládám text…", "Jazyk překladu není stažen · Klepnutím jej získáte", "Stahování jazyka překladu…", "Překlad není pro tento jazyk na tomto zařízení dostupný", "Překlad se nezdařil · Klepnutím to zkuste znovu"),
    "sk" to lyricsTranslationStatus("Prekladám text…", "Jazyk prekladu nie je stiahnutý · Klepnutím ho získate", "Sťahovanie jazyka prekladu…", "Preklad nie je pre tento jazyk na tomto zariadení dostupný", "Preklad zlyhal · Klepnutím to skúste znova"),
    "hr" to lyricsTranslationStatus("Prevođenje stihova…", "Jezik prijevoda nije preuzet · Dodirnite za preuzimanje", "Preuzimanje jezika prijevoda…", "Prijevod nije dostupan za ovaj jezik na ovom uređaju", "Prijevod nije uspio · Dodirnite za ponovni pokušaj"),
    "sr" to lyricsTranslationStatus("Превођење стихова…", "Језик превода није преузет · Додирните да га преузмете", "Преузимање језика превода…", "Превод није доступан за овај језик на овом уређају", "Превод није успео · Додирните да покушате поново"),
    "sq" to lyricsTranslationStatus("Po përkthehen tekstet…", "Gjuha e përkthimit nuk është shkarkuar · Prek për ta marrë", "Po shkarkohet gjuha e përkthimit…", "Përkthimi nuk është i disponueshëm për këtë gjuhë në këtë pajisje", "Përkthimi dështoi · Prek për të provuar përsëri"),
    "bg" to lyricsTranslationStatus("Превеждане на текста…", "Езикът за превод не е изтеглен · Докоснете, за да го изтеглите", "Изтегляне на езика за превод…", "Преводът не е наличен за този език на това устройство", "Преводът не бе успешен · Докоснете, за да опитате отново"),
    "hu" to lyricsTranslationStatus("Dalszöveg fordítása…", "A fordítási nyelv nincs letöltve · Koppints a letöltéshez", "Fordítási nyelv letöltése…", "A fordítás ezen a nyelven nem érhető el ezen az eszközön", "A fordítás sikertelen · Koppints az újrapróbáláshoz"),
    "fi" to lyricsTranslationStatus("Käännetään sanoituksia…", "Käännöskieltä ei ole ladattu · Lataa napauttamalla", "Ladataan käännöskieltä…", "Käännös ei ole saatavilla tälle kielelle tällä laitteella", "Käännös epäonnistui · Yritä uudelleen napauttamalla"),
    "et" to lyricsTranslationStatus("Laulusõnade tõlkimine…", "Tõlkekeel pole alla laaditud · Puuduta hankimiseks", "Tõlkekeele allalaadimine…", "Tõlge pole selle keele jaoks selles seadmes saadaval", "Tõlkimine ebaõnnestus · Puuduta uuesti proovimiseks"),
    "nb" to lyricsTranslationStatus("Oversetter sangteksten…", "Oversettelsesspråket er ikke lastet ned · Trykk for å hente det", "Laster ned oversettelsesspråk…", "Oversettelse er ikke tilgjengelig for dette språket på denne enheten", "Oversettelsen mislyktes · Trykk for å prøve igjen"),
    "ca" to lyricsTranslationStatus("S'està traduint la lletra…", "L'idioma de traducció no s'ha baixat · Toca per obtenir-lo", "S'està baixant l'idioma de traducció…", "La traducció no està disponible per a aquest idioma en aquest dispositiu", "La traducció ha fallat · Toca per tornar-ho a provar"),
    "uk" to lyricsTranslationStatus("Перекладаємо текст…", "Мову перекладу не завантажено · Торкніться, щоб завантажити", "Завантаження мови перекладу…", "Переклад цієї мови недоступний на цьому пристрої", "Не вдалося перекласти · Торкніться, щоб повторити"),
    "ru" to lyricsTranslationStatus("Переводим текст…", "Язык перевода не загружен · Нажмите, чтобы загрузить", "Загрузка языка перевода…", "Перевод для этого языка недоступен на этом устройстве", "Не удалось перевести · Нажмите, чтобы повторить"),
    "kk" to lyricsTranslationStatus("Мәтін аударылуда…", "Аударма тілі жүктелмеген · Жүктеу үшін түртіңіз", "Аударма тілі жүктелуде…", "Бұл құрылғыда осы тілге аударма қолжетімсіз", "Аударма сәтсіз аяқталды · Қайталау үшін түртіңіз"),
    "tr" to lyricsTranslationStatus("Şarkı sözleri çevriliyor…", "Çeviri dili indirilmedi · Almak için dokunun", "Çeviri dili indiriliyor…", "Bu cihazda bu dil için çeviri kullanılamıyor", "Çeviri başarısız oldu · Yeniden denemek için dokunun"),
    "ar" to lyricsTranslationStatus("جارٍ ترجمة الكلمات…", "لم يتم تنزيل لغة الترجمة · انقر للحصول عليها", "جارٍ تنزيل لغة الترجمة…", "الترجمة غير متاحة لهذه اللغة على هذا الجهاز", "فشلت الترجمة · انقر لإعادة المحاولة"),
    "fa" to lyricsTranslationStatus("در حال ترجمه متن ترانه…", "زبان ترجمه دانلود نشده است · برای دریافت ضربه بزنید", "در حال دانلود زبان ترجمه…", "ترجمه برای این زبان در این دستگاه در دسترس نیست", "ترجمه ناموفق بود · برای تلاش دوباره ضربه بزنید"),
    "zh" to lyricsTranslationStatus("正在翻译歌词…", "未下载翻译语言 · 点按获取", "正在下载翻译语言…", "此设备不支持该语言的翻译", "翻译失败 · 点按重试"),
    "zh-Hant" to lyricsTranslationStatus("正在翻譯歌詞…", "尚未下載翻譯語言 · 輕觸以取得", "正在下載翻譯語言…", "此裝置不支援該語言的翻譯", "翻譯失敗 · 輕觸以重試"),
    "ja" to lyricsTranslationStatus("歌詞を翻訳中…", "翻訳言語がダウンロードされていません · タップして取得", "翻訳言語をダウンロード中…", "このデバイスではこの言語の翻訳を利用できません", "翻訳に失敗しました · タップして再試行"),
    "ko" to lyricsTranslationStatus("가사 번역 중…", "번역 언어가 다운로드되지 않았습니다 · 탭하여 받기", "번역 언어 다운로드 중…", "이 기기에서는 이 언어의 번역을 사용할 수 없습니다", "번역 실패 · 탭하여 다시 시도"),
    "hi" to lyricsTranslationStatus("बोल का अनुवाद हो रहा है…", "अनुवाद भाषा डाउनलोड नहीं हुई · पाने के लिए टैप करें", "अनुवाद भाषा डाउनलोड हो रही है…", "इस डिवाइस पर इस भाषा के लिए अनुवाद उपलब्ध नहीं है", "अनुवाद विफल रहा · फिर से कोशिश करने के लिए टैप करें"),
    "bn" to lyricsTranslationStatus("গানের কথা অনুবাদ করা হচ্ছে…", "অনুবাদের ভাষা ডাউনলোড করা হয়নি · পেতে ট্যাপ করুন", "অনুবাদের ভাষা ডাউনলোড হচ্ছে…", "এই ডিভাইসে এই ভাষার অনুবাদ উপলব্ধ নয়", "অনুবাদ ব্যর্থ হয়েছে · আবার চেষ্টা করতে ট্যাপ করুন"),
    "id" to lyricsTranslationStatus("Menerjemahkan lirik…", "Bahasa terjemahan belum diunduh · Ketuk untuk mendapatkannya", "Mengunduh bahasa terjemahan…", "Terjemahan tidak tersedia untuk bahasa ini di perangkat ini", "Terjemahan gagal · Ketuk untuk mencoba lagi"),
    "ms" to lyricsTranslationStatus("Menterjemah lirik…", "Bahasa terjemahan belum dimuat turun · Ketik untuk mendapatkannya", "Memuat turun bahasa terjemahan…", "Terjemahan tidak tersedia untuk bahasa ini pada peranti ini", "Terjemahan gagal · Ketik untuk cuba lagi"),
    "vi" to lyricsTranslationStatus("Đang dịch lời bài hát…", "Chưa tải ngôn ngữ dịch · Nhấn để tải", "Đang tải ngôn ngữ dịch…", "Không thể dịch ngôn ngữ này trên thiết bị này", "Dịch không thành công · Nhấn để thử lại"),
    "th" to lyricsTranslationStatus("กำลังแปลเนื้อเพลง…", "ยังไม่ได้ดาวน์โหลดภาษาสำหรับแปล · แตะเพื่อดาวน์โหลด", "กำลังดาวน์โหลดภาษาสำหรับแปล…", "อุปกรณ์นี้ไม่รองรับการแปลภาษานี้", "แปลไม่สำเร็จ · แตะเพื่อลองอีกครั้ง"),
    "fil" to lyricsTranslationStatus("Isinasalin ang lyrics…", "Hindi pa na-download ang wika ng pagsasalin · I-tap para makuha", "Dina-download ang wika ng pagsasalin…", "Hindi available ang pagsasalin para sa wikang ito sa device na ito", "Nabigo ang pagsasalin · I-tap para subukan ulit"),
    "he" to lyricsTranslationStatus("מתרגם את המילים…", "שפת התרגום לא הורדה · יש להקיש כדי להוריד", "מוריד את שפת התרגום…", "התרגום לא זמין לשפה זו במכשיר הזה", "התרגום נכשל · יש להקיש כדי לנסות שוב")
)

internal fun lyricsActionLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(lyricsActionBundles, code) + localizedBundleOrEnglish(lyricsOffsetBundles, code) +
        localizedBundleOrEnglish(lyricsTranslationStatusBundles, code)

internal fun lyricsActionLocalizationCodes(): Set<String> = supportedLocalizationCodes()
