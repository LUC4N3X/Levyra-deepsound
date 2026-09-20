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
    "he" to lyricsActions("שינוי מילות השיר", "אוטומטי", "בחירת שורות", "העתקה", "שיתוף", "גרסאות של מילות השיר")
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

internal fun lyricsActionLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(lyricsActionBundles, code) + localizedBundleOrEnglish(lyricsOffsetBundles, code)

internal fun lyricsActionLocalizationCodes(): Set<String> = supportedLocalizationCodes()
