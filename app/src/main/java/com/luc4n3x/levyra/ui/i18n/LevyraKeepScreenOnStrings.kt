package com.luc4n3x.levyra.ui.i18n

internal val keepScreenOnKeys = setOf(
    "keepScreenOn",
    "keepScreenOnSubtitle"
)

private fun keepScreenOn(title: String, subtitle: String): Map<String, String> = mapOf(
    "keepScreenOn" to title,
    "keepScreenOnSubtitle" to subtitle
)

private val keepScreenOnBundles: Map<String, Map<String, String>> = mapOf(
    "en" to keepScreenOn("Keep screen on", "Prevents the screen from turning off automatically during music playback."),
    "it" to keepScreenOn("Mantieni lo schermo acceso", "Impedisce lo spegnimento automatico dello schermo durante la riproduzione musicale."),
    "es" to keepScreenOn("Mantener la pantalla encendida", "Evita que la pantalla se apague automáticamente durante la reproducción de música."),
    "fr" to keepScreenOn("Garder l’écran allumé", "Empêche l’extinction automatique de l’écran pendant la lecture de musique."),
    "de" to keepScreenOn("Bildschirm eingeschaltet lassen", "Verhindert das automatische Ausschalten des Bildschirms während der Musikwiedergabe."),
    "pt" to keepScreenOn("Manter o ecrã ligado", "Impede que o ecrã se desligue automaticamente durante a reprodução de música."),
    "nl" to keepScreenOn("Scherm aan houden", "Voorkomt dat het scherm automatisch uitgaat tijdens het afspelen van muziek."),
    "pl" to keepScreenOn("Nie wygaszaj ekranu", "Zapobiega automatycznemu wyłączaniu ekranu podczas odtwarzania muzyki."),
    "ro" to keepScreenOn("Menține ecranul pornit", "Împiedică oprirea automată a ecranului în timpul redării muzicii."),
    "el" to keepScreenOn("Διατήρηση οθόνης ενεργής", "Αποτρέπει την αυτόματη απενεργοποίηση της οθόνης κατά την αναπαραγωγή μουσικής."),
    "sv" to keepScreenOn("Håll skärmen på", "Förhindrar att skärmen stängs av automatiskt under musikuppspelning."),
    "da" to keepScreenOn("Hold skærmen tændt", "Forhindrer, at skærmen slukker automatisk under musikafspilning."),
    "cs" to keepScreenOn("Ponechat obrazovku zapnutou", "Zabrání automatickému vypnutí obrazovky během přehrávání hudby."),
    "sk" to keepScreenOn("Ponechať obrazovku zapnutú", "Zabráni automatickému vypnutiu obrazovky počas prehrávania hudby."),
    "hr" to keepScreenOn("Zadrži zaslon uključenim", "Sprječava automatsko isključivanje zaslona tijekom reprodukcije glazbe."),
    "sr" to keepScreenOn("Задржи екран укљученим", "Спречава аутоматско искључивање екрана током репродукције музике."),
    "sq" to keepScreenOn("Mbaje ekranin ndezur", "Parandalon fikjen automatike të ekranit gjatë luajtjes së muzikës."),
    "bg" to keepScreenOn("Екранът да остава включен", "Предотвратява автоматичното изключване на екрана по време на възпроизвеждане на музика."),
    "hu" to keepScreenOn("Képernyő bekapcsolva tartása", "Megakadályozza a képernyő automatikus kikapcsolását zenelejátszás közben."),
    "fi" to keepScreenOn("Pidä näyttö päällä", "Estää näyttöä sammumasta automaattisesti musiikin toiston aikana."),
    "et" to keepScreenOn("Hoia ekraan sees", "Takistab ekraani automaatset väljalülitumist muusika esitamise ajal."),
    "nb" to keepScreenOn("Hold skjermen på", "Hindrer at skjermen slår seg av automatisk under musikkavspilling."),
    "ca" to keepScreenOn("Mantén la pantalla encesa", "Evita que la pantalla s’apagui automàticament durant la reproducció de música."),
    "uk" to keepScreenOn("Не вимикати екран", "Запобігає автоматичному вимкненню екрана під час відтворення музики."),
    "ru" to keepScreenOn("Не выключать экран", "Предотвращает автоматическое выключение экрана во время воспроизведения музыки."),
    "kk" to keepScreenOn("Экранды өшірмеу", "Музыка ойнатылып жатқанда экранның автоматты түрде өшуіне жол бермейді."),
    "tr" to keepScreenOn("Ekranı açık tut", "Müzik çalarken ekranın otomatik olarak kapanmasını önler."),
    "ar" to keepScreenOn("إبقاء الشاشة قيد التشغيل", "يمنع انطفاء الشاشة تلقائيًا أثناء تشغيل الموسيقى."),
    "fa" to keepScreenOn("روشن نگه داشتن صفحه", "از خاموش شدن خودکار صفحه هنگام پخش موسیقی جلوگیری می‌کند."),
    "zh" to keepScreenOn("保持屏幕常亮", "在播放音乐时防止屏幕自动关闭。"),
    "zh-Hant" to keepScreenOn("保持螢幕開啟", "在播放音樂時防止螢幕自動關閉。"),
    "ja" to keepScreenOn("画面をオンのままにする", "音楽の再生中に画面が自動的にオフになるのを防ぎます。"),
    "ko" to keepScreenOn("화면 켜짐 유지", "음악을 재생하는 동안 화면이 자동으로 꺼지지 않도록 합니다."),
    "hi" to keepScreenOn("स्क्रीन चालू रखें", "संगीत चलने के दौरान स्क्रीन को अपने आप बंद होने से रोकता है।"),
    "bn" to keepScreenOn("স্ক্রিন চালু রাখুন", "সংগীত বাজানোর সময় স্ক্রিন স্বয়ংক্রিয়ভাবে বন্ধ হওয়া রোধ করে।"),
    "id" to keepScreenOn("Biarkan layar tetap menyala", "Mencegah layar mati otomatis saat musik diputar."),
    "ms" to keepScreenOn("Kekalkan skrin menyala", "Menghalang skrin daripada mati secara automatik semasa muzik dimainkan."),
    "vi" to keepScreenOn("Giữ màn hình luôn bật", "Ngăn màn hình tự động tắt trong khi phát nhạc."),
    "th" to keepScreenOn("เปิดหน้าจอค้างไว้", "ป้องกันไม่ให้หน้าจอดับเองโดยอัตโนมัติระหว่างเล่นเพลง"),
    "fil" to keepScreenOn("Panatilihing bukas ang screen", "Pinipigilan ang awtomatikong pagpatay ng screen habang nagpe-play ng musika."),
    "he" to keepScreenOn("השארת המסך דולק", "מונע כיבוי אוטומטי של המסך בזמן ניגון מוזיקה.")
)

internal fun keepScreenOnLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(keepScreenOnBundles, code)

internal fun keepScreenOnLocalizationCodes(): Set<String> = keepScreenOnBundles.keys
