package com.luc4n3x.levyra.ui.i18n

internal data class AudioTabsCopy(
    val sound: String,
    val playback: String,
    val smart: String
)

private val audioTabsCopies = mapOf(
    "en" to AudioTabsCopy("Sound", "Playback", "Smart"),
    "it" to AudioTabsCopy("Suono", "Riproduzione", "Smart"),
    "es" to AudioTabsCopy("Sonido", "Reproducción", "Smart"),
    "fr" to AudioTabsCopy("Son", "Lecture", "Smart"),
    "de" to AudioTabsCopy("Klang", "Wiedergabe", "Smart"),
    "pt" to AudioTabsCopy("Som", "Reprodução", "Inteligente"),
    "nl" to AudioTabsCopy("Geluid", "Afspelen", "Slim"),
    "pl" to AudioTabsCopy("Dźwięk", "Odtwarzanie", "Smart"),
    "ro" to AudioTabsCopy("Sunet", "Redare", "Inteligent"),
    "el" to AudioTabsCopy("Ήχος", "Αναπαραγωγή", "Έξυπνο"),
    "sv" to AudioTabsCopy("Ljud", "Uppspelning", "Smart"),
    "da" to AudioTabsCopy("Lyd", "Afspilning", "Smart"),
    "cs" to AudioTabsCopy("Zvuk", "Přehrávání", "Chytré"),
    "sk" to AudioTabsCopy("Zvuk", "Prehrávanie", "Inteligentné"),
    "hr" to AudioTabsCopy("Zvuk", "Reprodukcija", "Pametno"),
    "sr" to AudioTabsCopy("Звук", "Репродукција", "Паметно"),
    "sq" to AudioTabsCopy("Zëri", "Luajtja", "Smart"),
    "bg" to AudioTabsCopy("Звук", "Възпроизвеждане", "Умно"),
    "hu" to AudioTabsCopy("Hang", "Lejátszás", "Okos"),
    "fi" to AudioTabsCopy("Ääni", "Toisto", "Älykäs"),
    "et" to AudioTabsCopy("Heli", "Esitus", "Nutikas"),
    "nb" to AudioTabsCopy("Lyd", "Avspilling", "Smart"),
    "ca" to AudioTabsCopy("So", "Reproducció", "Intel·ligent"),
    "uk" to AudioTabsCopy("Звук", "Відтворення", "Розумне"),
    "ru" to AudioTabsCopy("Звук", "Воспроизведение", "Умное"),
    "kk" to AudioTabsCopy("Дыбыс", "Ойнату", "Ақылды"),
    "tr" to AudioTabsCopy("Ses", "Oynatma", "Akıllı"),
    "ar" to AudioTabsCopy("الصوت", "التشغيل", "ذكي"),
    "fa" to AudioTabsCopy("صدا", "پخش", "هوشمند"),
    "zh" to AudioTabsCopy("音效", "播放", "智能"),
    "zh-Hant" to AudioTabsCopy("音效", "播放", "智慧"),
    "ja" to AudioTabsCopy("サウンド", "再生", "スマート"),
    "ko" to AudioTabsCopy("사운드", "재생", "스마트"),
    "hi" to AudioTabsCopy("ध्वनि", "प्लेबैक", "स्मार्ट"),
    "bn" to AudioTabsCopy("সাউন্ড", "প্লেব্যাক", "স্মার্ট"),
    "id" to AudioTabsCopy("Suara", "Pemutaran", "Pintar"),
    "ms" to AudioTabsCopy("Bunyi", "Main semula", "Pintar"),
    "vi" to AudioTabsCopy("Âm thanh", "Phát nhạc", "Thông minh"),
    "th" to AudioTabsCopy("เสียง", "การเล่น", "อัจฉริยะ"),
    "fil" to AudioTabsCopy("Tunog", "Pag-play", "Smart"),
    "he" to AudioTabsCopy("סאונד", "ניגון", "חכם")
)

internal fun audioTabsCopy(code: String): AudioTabsCopy = localizedValueOrEnglish(audioTabsCopies, code)

internal fun audioTabsLocalizationCodes(): Set<String> = audioTabsCopies.keys
