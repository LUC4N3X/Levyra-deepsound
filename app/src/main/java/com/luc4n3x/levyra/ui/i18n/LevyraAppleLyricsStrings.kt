package com.luc4n3x.levyra.ui.i18n


internal data class AppleLyricsCopy(
    val lyricsStyle: String,
    val skinDefault: String,
    val originalLyrics: String,
    val autoScroll: String
)

internal const val APPLE_MUSIC_LYRICS_STYLE_LABEL = "Apple Music"

private fun copy(style: String, skin: String, original: String, scroll: String) = AppleLyricsCopy(style, skin, original, scroll)

private val appleLyricsCopies: Map<String, AppleLyricsCopy> = mapOf(
    "en" to copy("Lyrics style", "Skin default", "Original lyrics", "Auto-scroll"),
    "it" to copy("Stile dei testi", "Predefinito della skin", "Testo originale", "Scorrimento automatico"),
    "es" to copy("Estilo de la letra", "Predeterminado del tema", "Letra original", "Desplazamiento automático"),
    "fr" to copy("Style des paroles", "Par défaut du thème", "Paroles originales", "Défilement automatique"),
    "de" to copy("Songtext-Stil", "Standard des Skins", "Originaltext", "Automatisch scrollen"),
    "pt" to copy("Estilo da letra", "Padrão do tema", "Letra original", "Rolagem automática"),
    "nl" to copy("Songtekststijl", "Standaard van skin", "Originele songtekst", "Automatisch scrollen"),
    "pl" to copy("Styl tekstu", "Domyślny dla skórki", "Oryginalny tekst", "Automatyczne przewijanie"),
    "ru" to copy("Стиль текста", "Как в оформлении", "Оригинальный текст", "Автопрокрутка"),
    "uk" to copy("Стиль тексту", "Як в оформленні", "Оригінальний текст", "Автопрокручування"),
    "ja" to copy("歌詞のスタイル", "スキンの既定", "原文の歌詞", "自動スクロール"),
    "ko" to copy("가사 스타일", "스킨 기본값", "원문 가사", "자동 스크롤"),
    "zh" to copy("歌词样式", "皮肤默认", "原文歌词", "自动滚动"),
    "zh-Hant" to copy("歌詞樣式", "主題預設", "原文歌詞", "自動捲動"),
    "ar" to copy("نمط الكلمات", "افتراضي المظهر", "الكلمات الأصلية", "التمرير التلقائي"),
    "ro" to copy("Stilul versurilor", "Implicit pentru temă", "Versuri originale", "Derulare automată"),
    "sq" to copy("Stili i teksteve", "Parazgjedhja e lëkurës", "Teksti origjinal", "Lëvizje automatike"),
    "bg" to copy("Стил на текста", "По подразбиране за темата", "Оригинален текст", "Автоматично превъртане"),
    "el" to copy("Στυλ στίχων", "Προεπιλογή θέματος", "Αρχικοί στίχοι", "Αυτόματη κύλιση"),
    "sv" to copy("Låttextstil", "Skalets standard", "Originaltext", "Automatisk rullning"),
    "da" to copy("Sangtekststil", "Temaets standard", "Original sangtekst", "Automatisk rulning"),
    "cs" to copy("Styl textu", "Výchozí pro vzhled", "Původní text", "Automatické posouvání"),
    "sk" to copy("Štýl textu", "Predvolené pre vzhľad", "Pôvodný text", "Automatické posúvanie"),
    "bn" to copy("লিরিক্সের স্টাইল", "স্কিনের ডিফল্ট", "মূল লিরিক্স", "স্বয়ংক্রিয় স্ক্রল"),
    "kk" to copy("Мәтін стилі", "Тақырып бойынша әдепкі", "Түпнұсқа мәтін", "Автоматты айналдыру"),
    "sr" to copy("Стил текста", "Подразумевано за тему", "Оригинални текст", "Аутоматско померање"),
    "hr" to copy("Stil teksta", "Zadano za temu", "Izvorni tekst", "Automatsko pomicanje"),
    "hu" to copy("Dalszöveg stílusa", "A téma alapértelmezése", "Eredeti dalszöveg", "Automatikus görgetés"),
    "fi" to copy("Sanoitusten tyyli", "Teeman oletus", "Alkuperäiset sanat", "Automaattinen vieritys"),
    "et" to copy("Laulusõnade stiil", "Teema vaikimisi", "Algsed laulusõnad", "Automaatne kerimine"),
    "nb" to copy("Sangtekststil", "Temaets standard", "Original sangtekst", "Automatisk rulling"),
    "ca" to copy("Estil de la lletra", "Predeterminat del tema", "Lletra original", "Desplaçament automàtic"),
    "tr" to copy("Şarkı sözü stili", "Tema varsayılanı", "Orijinal sözler", "Otomatik kaydırma"),
    "fa" to copy("سبک متن ترانه", "پیش‌فرض پوسته", "متن اصلی ترانه", "پیمایش خودکار"),
    "hi" to copy("लिरिक्स शैली", "स्किन डिफ़ॉल्ट", "मूल लिरिक्स", "ऑटो-स्क्रॉल"),
    "id" to copy("Gaya lirik", "Bawaan tema", "Lirik asli", "Gulir otomatis"),
    "ms" to copy("Gaya lirik", "Lalai tema", "Lirik asal", "Tatal automatik"),
    "vi" to copy("Kiểu lời bài hát", "Mặc định của giao diện", "Lời gốc", "Tự động cuộn"),
    "th" to copy("รูปแบบเนื้อเพลง", "ค่าเริ่มต้นของธีม", "เนื้อเพลงต้นฉบับ", "เลื่อนอัตโนมัติ"),
    "fil" to copy("Estilo ng lyrics", "Default ng tema", "Orihinal na lyrics", "Awtomatikong pag-scroll"),
    "he" to copy("סגנון המילים", "ברירת המחדל של העיצוב", "המילים המקוריות", "גלילה אוטומטית")
)

internal fun appleLyricsCopy(code: String): AppleLyricsCopy =
    localizedValueOrEnglish(appleLyricsCopies, code)

internal fun appleLyricsLocalizationCodes(): Set<String> = appleLyricsCopies.keys
