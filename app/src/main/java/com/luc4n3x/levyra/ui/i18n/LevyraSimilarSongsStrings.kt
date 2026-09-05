package com.luc4n3x.levyra.ui.i18n

internal val similarSongsKeys = setOf(
    "startRadio",
    "similarSongsPlay",
    "similarSongsAddToQueue"
)

private fun similarSongs(
    startRadio: String,
    play: String,
    addToQueue: String
): Map<String, String> = mapOf(
    "startRadio" to startRadio,
    "similarSongsPlay" to play,
    "similarSongsAddToQueue" to addToQueue
)

private val similarSongsBundles: Map<String, Map<String, String>> = mapOf(
    "en" to similarSongs("Start radio", "Play now", "Add to queue"),
    "it" to similarSongs("Avvia radio", "Riproduci ora", "Aggiungi alla coda"),
    "es" to similarSongs("Iniciar radio", "Reproducir ahora", "Añadir a la cola"),
    "fr" to similarSongs("Lancer la radio", "Lire maintenant", "Ajouter à la file"),
    "de" to similarSongs("Radio starten", "Jetzt abspielen", "Zur Warteschlange"),
    "pt" to similarSongs("Iniciar rádio", "Reproduzir agora", "Adicionar à fila"),
    "nl" to similarSongs("Radio starten", "Nu afspelen", "Aan wachtrij toevoegen"),
    "pl" to similarSongs("Włącz radio", "Odtwórz teraz", "Dodaj do kolejki"),
    "ro" to similarSongs("Pornește radio", "Redă acum", "Adaugă la coadă"),
    "el" to similarSongs("Έναρξη ραδιοφώνου", "Αναπαραγωγή τώρα", "Προσθήκη στη λίστα"),
    "sv" to similarSongs("Starta radio", "Spela nu", "Lägg till i kön"),
    "da" to similarSongs("Start radio", "Afspil nu", "Føj til køen"),
    "cs" to similarSongs("Spustit rádio", "Přehrát nyní", "Přidat do fronty"),
    "uk" to similarSongs("Запустити радіо", "Відтворити зараз", "Додати до черги"),
    "ru" to similarSongs("Запустить радио", "Воспроизвести сейчас", "Добавить в очередь"),
    "tr" to similarSongs("Radyoyu başlat", "Şimdi çal", "Sıraya ekle"),
    "ar" to similarSongs("تشغيل الراديو", "تشغيل الآن", "إضافة إلى قائمة الانتظار"),
    "zh" to similarSongs("开始电台", "立即播放", "加入队列"),
    "ja" to similarSongs("ラジオを開始", "今すぐ再生", "キューに追加"),
    "ko" to similarSongs("라디오 시작", "지금 재생", "대기열에 추가"),
    "hi" to similarSongs("रेडियो शुरू करें", "अभी चलाएँ", "कतार में जोड़ें"),
    "id" to similarSongs("Mulai radio", "Putar sekarang", "Tambahkan ke antrean"),
    "vi" to similarSongs("Bật radio", "Phát ngay", "Thêm vào hàng đợi"),
    "th" to similarSongs("เริ่มวิทยุ", "เล่นตอนนี้", "เพิ่มในคิว"),
    "fil" to similarSongs("Simulan ang radyo", "Patugtugin ngayon", "Idagdag sa pila"),
    "he" to similarSongs("הפעלת רדיו", "נגן עכשיו", "הוספה לתור")
)

internal fun similarSongsLocalizationEntries(code: String): Map<String, String> =
    similarSongsBundles.getValue(code)

internal fun similarSongsLocalizationCodes(): Set<String> = similarSongsBundles.keys
