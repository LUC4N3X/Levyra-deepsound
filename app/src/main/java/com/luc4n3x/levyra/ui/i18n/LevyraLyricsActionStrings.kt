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

internal fun lyricsActionLocalizationEntries(code: String): Map<String, String> = lyricsActionBundles.getValue(code)

internal fun lyricsActionLocalizationCodes(): Set<String> = lyricsActionBundles.keys
