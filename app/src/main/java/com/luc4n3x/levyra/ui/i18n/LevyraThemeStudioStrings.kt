package com.luc4n3x.levyra.ui.i18n

internal val themeStudioKeys = setOf(
    "themeStudio",
    "themeStudioSubtitle",
    "themeStudioPreview",
    "themeAccent",
    "themeAccentFromPreset",
    "themeAccentBlue",
    "themeAccentGreen",
    "themeAccentIndigo",
    "themeAccentOrange",
    "themeAccentPink",
    "themeAccentCyan",
    "themeAccentPurple",
    "themeAccentYellow"
)

private fun themeStudio(
    title: String,
    subtitle: String,
    preview: String,
    accent: String,
    accentFromPreset: String
): Map<String, String> = mapOf(
    "themeStudio" to title,
    "themeStudioSubtitle" to subtitle,
    "themeStudioPreview" to preview,
    "themeAccent" to accent,
    "themeAccentFromPreset" to accentFromPreset
)

private val themeStudioBaseBundles: Map<String, Map<String, String>> = mapOf(
    "en" to themeStudio(
        "Theme Studio", "One preset drives colour across Levyra", "Preview", "Accent", "From preset"
    ),
    "it" to themeStudio(
        "Theme Studio", "Un preset guida il colore in tutta Levyra", "Anteprima", "Accento", "Dal preset"
    ),
    "es" to themeStudio(
        "Theme Studio", "Un preset define el color en toda Levyra", "Vista previa", "Acento", "Del preset"
    ),
    "fr" to themeStudio(
        "Theme Studio", "Un préréglage pilote la couleur dans tout Levyra", "Aperçu", "Accent", "Du préréglage"
    ),
    "de" to themeStudio(
        "Theme Studio", "Ein Preset bestimmt die Farbe in ganz Levyra", "Vorschau", "Akzent", "Aus dem Preset"
    ),
    "pt" to themeStudio(
        "Theme Studio", "Um preset define a cor em toda a Levyra", "Pré-visualização", "Acento", "Do preset"
    ),
    "nl" to themeStudio(
        "Theme Studio", "Eén preset bepaalt de kleur in heel Levyra", "Voorbeeld", "Accent", "Uit preset"
    ),
    "pl" to themeStudio(
        "Theme Studio", "Jeden preset ustala kolor w całej Levyrze", "Podgląd", "Akcent", "Z presetu"
    ),
    "ro" to themeStudio(
        "Theme Studio", "Un preset dictează culoarea în toată Levyra", "Previzualizare", "Accent", "Din preset"
    ),
    "el" to themeStudio(
        "Theme Studio", "Ένα προεπιλεγμένο σετ καθορίζει το χρώμα σε όλο το Levyra", "Προεπισκόπηση", "Τόνος", "Από το σετ"
    ),
    "sv" to themeStudio(
        "Theme Studio", "En förinställning styr färgen i hela Levyra", "Förhandsvisning", "Accent", "Från förinställning"
    ),
    "da" to themeStudio(
        "Theme Studio", "Én forudindstilling styrer farven i hele Levyra", "Forhåndsvisning", "Accent", "Fra forudindstilling"
    ),
    "cs" to themeStudio(
        "Theme Studio", "Jedna předvolba řídí barvu v celé Levyře", "Náhled", "Akcent", "Z předvolby"
    ),
    "uk" to themeStudio(
        "Theme Studio", "Один пресет задає колір у всій Levyra", "Попередній перегляд", "Акцент", "З пресету"
    ),
    "ru" to themeStudio(
        "Theme Studio", "Один пресет задаёт цвет во всей Levyra", "Предпросмотр", "Акцент", "Из пресета"
    ),
    "tr" to themeStudio(
        "Theme Studio", "Tek bir hazır ayar Levyra genelinde rengi belirler", "Önizleme", "Vurgu", "Hazır ayardan"
    ),
    "ar" to themeStudio(
        "Theme Studio", "إعداد واحد يحدد اللون في كل Levyra", "معاينة", "لون التمييز", "من الإعداد"
    ),
    "zh" to themeStudio(
        "Theme Studio", "一个预设统一 Levyra 的配色", "预览", "强调色", "来自预设"
    ),
    "ja" to themeStudio(
        "Theme Studio", "1 つのプリセットが Levyra 全体の色を決めます", "プレビュー", "アクセント", "プリセットから"
    ),
    "ko" to themeStudio(
        "Theme Studio", "프리셋 하나가 Levyra 전체 색을 결정합니다", "미리보기", "강조색", "프리셋에서"
    ),
    "hi" to themeStudio(
        "Theme Studio", "एक प्रीसेट पूरे Levyra का रंग तय करता है", "पूर्वावलोकन", "एक्सेंट", "प्रीसेट से"
    ),
    "id" to themeStudio(
        "Theme Studio", "Satu preset menentukan warna di seluruh Levyra", "Pratinjau", "Aksen", "Dari preset"
    ),
    "vi" to themeStudio(
        "Theme Studio", "Một preset quyết định màu cho toàn bộ Levyra", "Xem trước", "Màu nhấn", "Từ preset"
    ),
    "th" to themeStudio(
        "Theme Studio", "พรีเซ็ตเดียวกำหนดสีทั้ง Levyra", "ตัวอย่าง", "สีเน้น", "จากพรีเซ็ต"
    ),
    "fil" to themeStudio(
        "Theme Studio", "Isang preset ang humuhubog sa kulay ng buong Levyra", "Preview", "Accent", "Mula sa preset"
    ),
    "he" to themeStudio(
        "Theme Studio", "ערכה אחת קובעת את הצבע בכל Levyra", "תצוגה מקדימה", "צבע הדגשה", "מהערכה"
    ),
    "fi" to themeStudio(
        "Theme Studio", "Yksi esiasetus ohjaa värejä koko Levyra-sovelluksessa", "Esikatselu", "Aksentti", "Esiasetuksesta"
    ),
    "et" to themeStudio(
        "Theme Studio", "Üks eelseadistus määrab värvi kogu Levyras", "Eelvaade", "Rõhk", "Eelseadistusest"
    )
)

private fun themeAccentNames(
    blue: String,
    green: String,
    indigo: String,
    orange: String,
    pink: String,
    cyan: String,
    purple: String,
    yellow: String
): Map<String, String> = mapOf(
    "themeAccentBlue" to blue,
    "themeAccentGreen" to green,
    "themeAccentIndigo" to indigo,
    "themeAccentOrange" to orange,
    "themeAccentPink" to pink,
    "themeAccentCyan" to cyan,
    "themeAccentPurple" to purple,
    "themeAccentYellow" to yellow
)

private val themeAccentNameBundles: Map<String, Map<String, String>> = mapOf(
    "en" to themeAccentNames("Blue", "Green", "Indigo", "Orange", "Pink", "Cyan", "Purple", "Yellow"),
    "it" to themeAccentNames("Blu", "Verde", "Indaco", "Arancione", "Rosa", "Ciano", "Viola", "Giallo"),
    "es" to themeAccentNames("Azul", "Verde", "Índigo", "Naranja", "Rosa", "Cian", "Morado", "Amarillo"),
    "fr" to themeAccentNames("Bleu", "Vert", "Indigo", "Orange", "Rose", "Cyan", "Violet", "Jaune"),
    "de" to themeAccentNames("Blau", "Grün", "Indigo", "Orange", "Pink", "Cyan", "Violett", "Gelb"),
    "pt" to themeAccentNames("Azul", "Verde", "Índigo", "Laranja", "Rosa", "Ciano", "Roxo", "Amarelo"),
    "nl" to themeAccentNames("Blauw", "Groen", "Indigo", "Oranje", "Roze", "Cyaan", "Paars", "Geel"),
    "pl" to themeAccentNames("Niebieski", "Zielony", "Indygo", "Pomarańczowy", "Różowy", "Cyjan", "Fioletowy", "Żółty"),
    "ro" to themeAccentNames("Albastru", "Verde", "Indigo", "Portocaliu", "Roz", "Cyan", "Mov", "Galben"),
    "el" to themeAccentNames("Μπλε", "Πράσινο", "Λουλακί", "Πορτοκαλί", "Ροζ", "Κυανό", "Μωβ", "Κίτρινο"),
    "sv" to themeAccentNames("Blå", "Grön", "Indigo", "Orange", "Rosa", "Cyan", "Lila", "Gul"),
    "da" to themeAccentNames("Blå", "Grøn", "Indigo", "Orange", "Pink", "Cyan", "Lilla", "Gul"),
    "cs" to themeAccentNames("Modrá", "Zelená", "Indigo", "Oranžová", "Růžová", "Azurová", "Fialová", "Žlutá"),
    "uk" to themeAccentNames("Синій", "Зелений", "Індиго", "Помаранчевий", "Рожевий", "Блакитний", "Фіолетовий", "Жовтий"),
    "ru" to themeAccentNames("Синий", "Зелёный", "Индиго", "Оранжевый", "Розовый", "Голубой", "Фиолетовый", "Жёлтый"),
    "tr" to themeAccentNames("Mavi", "Yeşil", "Çivit", "Turuncu", "Pembe", "Camgöbeği", "Mor", "Sarı"),
    "ar" to themeAccentNames("أزرق", "أخضر", "نيلي", "برتقالي", "وردي", "سماوي", "بنفسجي", "أصفر"),
    "zh" to themeAccentNames("蓝色", "绿色", "靛蓝色", "橙色", "粉色", "青色", "紫色", "黄色"),
    "ja" to themeAccentNames("ブルー", "グリーン", "インディゴ", "オレンジ", "ピンク", "シアン", "パープル", "イエロー"),
    "ko" to themeAccentNames("파랑", "초록", "남색", "주황", "분홍", "청록", "보라", "노랑"),
    "hi" to themeAccentNames("नीला", "हरा", "इंडिगो", "नारंगी", "गुलाबी", "सियान", "बैंगनी", "पीला"),
    "id" to themeAccentNames("Biru", "Hijau", "Nila", "Oranye", "Merah muda", "Sian", "Ungu", "Kuning"),
    "vi" to themeAccentNames("Xanh dương", "Xanh lá", "Chàm", "Cam", "Hồng", "Lục lam", "Tím", "Vàng"),
    "th" to themeAccentNames("น้ำเงิน", "เขียว", "คราม", "ส้ม", "ชมพู", "ฟ้าอมเขียว", "ม่วง", "เหลือง"),
    "fil" to themeAccentNames("Asul", "Berde", "Indigo", "Kahel", "Rosas", "Cyan", "Lila", "Dilaw"),
    "he" to themeAccentNames("כחול", "ירוק", "אינדיגו", "כתום", "ורוד", "ציאן", "סגול", "צהוב"),
    "fi" to themeAccentNames("Sininen", "Vihreä", "Indigo", "Oranssi", "Vaaleanpunainen", "Syaani", "Purppura", "Keltainen"),
    "et" to themeAccentNames("Sinine", "Roheline", "Indigo", "Oranž", "Roosa", "Tsüaan", "Lilla", "Kollane")
)

private val themeStudioBundles: Map<String, Map<String, String>> =
    themeStudioBaseBundles.mapValues { (code, base) ->
        base + themeAccentNameBundles.getValue(code)
    }

internal fun themeStudioLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(themeStudioBundles, code)

internal fun themeStudioLocalizationCodes(): Set<String> = themeStudioBundles.keys
