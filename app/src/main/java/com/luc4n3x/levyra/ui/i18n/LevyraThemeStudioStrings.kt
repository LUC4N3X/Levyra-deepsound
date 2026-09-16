package com.luc4n3x.levyra.ui.i18n

internal val themeStudioKeys = setOf(
    "themeStudio",
    "themeStudioSubtitle",
    "themeStudioPreview",
    "themeAccent",
    "themeAccentFromPreset"
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

private val themeStudioBundles: Map<String, Map<String, String>> = mapOf(
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
    )
)

internal fun themeStudioLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(themeStudioBundles, code)

internal fun themeStudioLocalizationCodes(): Set<String> = themeStudioBundles.keys
