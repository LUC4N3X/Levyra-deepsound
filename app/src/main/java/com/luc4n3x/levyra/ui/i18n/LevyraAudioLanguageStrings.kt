package com.luc4n3x.levyra.ui.i18n

private fun audioLanguageStrings(
    title: String,
    subtitle: String,
    originalDefault: String
): Map<String, String> = mapOf(
    "audioLanguageTitle" to title,
    "audioLanguageSubtitle" to subtitle,
    "audioLanguageOriginalDefault" to originalDefault
)

private val audioLanguageBundles: Map<String, Map<String, String>> = mapOf(
    "en" to audioLanguageStrings(
        "Audio track language",
        "Select preferred audio track language or keep original YouTube audio.",
        "Original / Default"
    ),
    "it" to audioLanguageStrings(
        "Lingua traccia audio",
        "Seleziona la lingua audio preferita o mantieni l'audio originale di YouTube.",
        "Originale / Predefinito"
    ),
    "es" to audioLanguageStrings(
        "Idioma de la pista de audio",
        "Selecciona el idioma de audio preferido o mantén el audio original de YouTube.",
        "Original / Predeterminado"
    ),
    "fr" to audioLanguageStrings(
        "Langue de la piste audio",
        "Sélectionnez la langue audio préférée ou conservez l'audio original de YouTube.",
        "Original / Par défaut"
    ),
    "de" to audioLanguageStrings(
        "Audiospur-Sprache",
        "Wähle die bevorzugte Audiosprache oder behalte das Original-Audio von YouTube.",
        "Original / Standard"
    ),
    "pt" to audioLanguageStrings(
        "Idioma da faixa de áudio",
        "Selecione o idioma de áudio preferido ou mantenha o áudio original do YouTube.",
        "Original / Padrão"
    ),
    "nl" to audioLanguageStrings(
        "Taal van audiotrack",
        "Selecteer de gewenste audiotaal of behoud de originele YouTube-audio.",
        "Origineel / Standaard"
    ),
    "pl" to audioLanguageStrings(
        "Język ścieżki dźwiękowej",
        "Wybierz preferowany język dźwięku lub zachowaj oryginalny dźwięk z YouTube.",
        "Oryginalny / Domyślny"
    ),
    "ro" to audioLanguageStrings(
        "Limba piesei audio",
        "Selectați limba audio preferată sau păstrați sunetul original YouTube.",
        "Original / Implicit"
    ),
    "el" to audioLanguageStrings(
        "Γλώσσα κομματιού ήχου",
        "Επιλέξτε την προτιμώμενη γλώσσα ήχου ή διατηρήστε τον αρχικό ήχο του YouTube.",
        "Αρχικό / Προεπιλογή"
    ),
    "sv" to audioLanguageStrings(
        "Språk för ljudspår",
        "Välj föredraget ljudspråk eller behåll YouTubes originalljud.",
        "Original / Standard"
    ),
    "da" to audioLanguageStrings(
        "Lydsporssprog",
        "Vælg foretrukket lydsprog, eller behold original lyd fra YouTube.",
        "Original / Standard"
    ),
    "cs" to audioLanguageStrings(
        "Jazyk zvukové stopy",
        "Vyberte preferovaný jazyk zvuku nebo ponechte původní zvuk YouTube.",
        "Původní / Výchozí"
    ),
    "sk" to audioLanguageStrings(
        "Jazyk zvukovej stopy",
        "Vyberte preferovaný jazyk zvuku alebo ponechajte pôvodný zvuk YouTube.",
        "Pôvodný / Predvolený"
    ),
    "hr" to audioLanguageStrings(
        "Jezik zvučnog zapisa",
        "Odaberite željeni jezik zvuka ili zadržite izvorni YouTube zvuk.",
        "Izvorno / Zadano"
    ),
    "bg" to audioLanguageStrings(
        "Език на аудиозаписа",
        "Изберете предпочитан аудио език или запазете оригиналния звук от YouTube.",
        "Оригинален / По подразбиране"
    ),
    "hu" to audioLanguageStrings(
        "Hangsáv nyelve",
        "Válaszd ki a kívánt hangnyelvet, vagy tartsd meg az eredeti YouTube-hangot.",
        "Eredeti / Alapértelmezett"
    ),
    "fi" to audioLanguageStrings(
        "Ääniraidan kieli",
        "Valitse haluamasi äänen kieli tai säilytä YouTuben alkuperäinen ääni.",
        "Alkuperäinen / Oletus"
    ),
    "et" to audioLanguageStrings(
        "Heliraja keel",
        "Vali eelistatud helikeel või säilita YouTube'i algne heli.",
        "Algne / Vaikimisi"
    ),
    "nb" to audioLanguageStrings(
        "Lydsporspråk",
        "Velg foretrukket lydspråk eller behold original lyd fra YouTube.",
        "Original / Standard"
    ),
    "ca" to audioLanguageStrings(
        "Idioma de la pista d'àudio",
        "Selecciona l'idioma d'àudio preferit o mantén l'àudio original de YouTube.",
        "Original / Per defecte"
    ),
    "uk" to audioLanguageStrings(
        "Мова аудіодоріжки",
        "Виберіть бажану мову аудіо або збережіть оригінальний звук YouTube.",
        "Оригінал / За замовчуванням"
    ),
    "ru" to audioLanguageStrings(
        "Язык аудиодорожки",
        "Выберите предпочитаемый язык аудио или сохраните оригинальный звук YouTube.",
        "Оригинал / По умолчанию"
    ),
    "tr" to audioLanguageStrings(
        "Ses parçası dili",
        "Tercih ettiğiniz ses dilini seçin veya orijinal YouTube sesini koruyun.",
        "Orijinal / Varsayılan"
    ),
    "ar" to audioLanguageStrings(
        "لغة المسار الصوتي",
        "اختر لغة الصوت المفضلة أو احتفظ بالصوت الأصلي من YouTube.",
        "الأصلي / الافتراضي"
    ),
    "fa" to audioLanguageStrings(
        "زبان قطعه صوتی",
        "زبان صوتی مورد نظر را انتخاب کنید یا صدای اصلی YouTube را نگه دارید.",
        "اصلی / پیش‌فرض"
    ),
    "zh" to audioLanguageStrings(
        "音轨语言",
        "选择首选音频语言或保留 YouTube 原始音频。",
        "原始 / 默认"
    ),
    "zh-Hant" to audioLanguageStrings(
        "音軌語言",
        "選擇首選音訊語言或保留 YouTube 原始音訊。",
        "原始 / 預設"
    ),
    "ja" to audioLanguageStrings(
        "音声トラックの言語",
        "好みの音声言語を選択するか、YouTubeのオリジナル音声を維持します。",
        "オリジナル / デフォルト"
    ),
    "ko" to audioLanguageStrings(
        "오디오 트랙 언어",
        "선호하는 오디오 언어를 선택하거나 YouTube 원본 오디오를 유지합니다.",
        "원본 / 기본값"
    ),
    "hi" to audioLanguageStrings(
        "ऑडियो ट्रैक भाषा",
        "पसंदीदा ऑडियो भाषा चुनें या मूल YouTube ऑडियो बनाए रखें।",
        "मूल / डिफ़ॉल्ट"
    ),
    "id" to audioLanguageStrings(
        "Bahasa trek audio",
        "Pilih bahasa audio pilihan atau pertahankan audio asli YouTube.",
        "Asli / Default"
    ),
    "ms" to audioLanguageStrings(
        "Bahasa trek audio",
        "Pilih bahasa audio pilihan atau kekalkan audio asal YouTube.",
        "Asal / Lalai"
    ),
    "vi" to audioLanguageStrings(
        "Ngôn ngữ bản âm thanh",
        "Chọn ngôn ngữ âm thanh ưa thích hoặc giữ âm thanh gốc của YouTube.",
        "Gốc / Mặc định"
    ),
    "th" to audioLanguageStrings(
        "ภาษาแทร็กเสียง",
        "เลือกภาษาเสียงที่ต้องการหรือคงเสียงต้นฉบับของ YouTube ไว้",
        "ต้นฉบับ / ค่าเริ่มต้น"
    ),
    "fil" to audioLanguageStrings(
        "Wika ng audio track",
        "Piliin ang gustong wika ng audio o panatilihin ang orihinal na audio ng YouTube.",
        "Orihinal / Default"
    ),
    "he" to audioLanguageStrings(
        "שפת רצועת השמע",
        "בחר את שפת השמע המועדפת או שמור על השמע המקורי של YouTube.",
        "מקורי / ברירת מחדל"
    )
)

internal fun audioLanguageLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(audioLanguageBundles, code)

internal fun audioLanguageLocalizationCodes(): Set<String> = audioLanguageBundles.keys

internal val audioLanguageKeys: Set<String> = setOf(
    "audioLanguageTitle",
    "audioLanguageSubtitle",
    "audioLanguageOriginalDefault"
)
