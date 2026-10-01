package com.luc4n3x.levyra.ui.i18n

private val liveUpdateBundles: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf("liveUpdatePlaybackChannel" to "Playback live updates"),
    "it" to mapOf("liveUpdatePlaybackChannel" to "Aggiornamenti live della riproduzione"),
    "es" to mapOf("liveUpdatePlaybackChannel" to "Actualizaciones en directo de reproducción"),
    "fr" to mapOf("liveUpdatePlaybackChannel" to "Mises à jour en direct de la lecture"),
    "de" to mapOf("liveUpdatePlaybackChannel" to "Live-Updates zur Wiedergabe"),
    "pt" to mapOf("liveUpdatePlaybackChannel" to "Atualizações em direto da reprodução"),
    "nl" to mapOf("liveUpdatePlaybackChannel" to "Live-updates voor afspelen"),
    "pl" to mapOf("liveUpdatePlaybackChannel" to "Aktualizacje na żywo odtwarzania"),
    "ro" to mapOf("liveUpdatePlaybackChannel" to "Actualizări live pentru redare"),
    "el" to mapOf("liveUpdatePlaybackChannel" to "Ζωντανές ενημερώσεις αναπαραγωγής"),
    "sv" to mapOf("liveUpdatePlaybackChannel" to "Liveuppdateringar för uppspelning"),
    "da" to mapOf("liveUpdatePlaybackChannel" to "Liveopdateringer for afspilning"),
    "cs" to mapOf("liveUpdatePlaybackChannel" to "Živé aktualizace přehrávání"),
    "uk" to mapOf("liveUpdatePlaybackChannel" to "Живі оновлення відтворення"),
    "ru" to mapOf("liveUpdatePlaybackChannel" to "Живые обновления воспроизведения"),
    "tr" to mapOf("liveUpdatePlaybackChannel" to "Oynatma canlı güncellemeleri"),
    "ar" to mapOf("liveUpdatePlaybackChannel" to "تحديثات التشغيل المباشرة"),
    "zh" to mapOf("liveUpdatePlaybackChannel" to "播放实时更新"),
    "ja" to mapOf("liveUpdatePlaybackChannel" to "再生のライブアップデート"),
    "ko" to mapOf("liveUpdatePlaybackChannel" to "재생 실시간 업데이트"),
    "hi" to mapOf("liveUpdatePlaybackChannel" to "प्लेबैक लाइव अपडेट"),
    "id" to mapOf("liveUpdatePlaybackChannel" to "Pembaruan langsung pemutaran"),
    "vi" to mapOf("liveUpdatePlaybackChannel" to "Cập nhật trực tiếp khi phát"),
    "th" to mapOf("liveUpdatePlaybackChannel" to "การอัปเดตสดของการเล่น"),
    "fil" to mapOf("liveUpdatePlaybackChannel" to "Mga live update ng playback"),
    "he" to mapOf("liveUpdatePlaybackChannel" to "עדכונים חיים של ההשמעה"),
    "fi" to mapOf("liveUpdatePlaybackChannel" to "Toiston live-päivitykset"),
    "et" to mapOf("liveUpdatePlaybackChannel" to "Taasesituse reaalajas värskendused"),
    "sk" to mapOf("liveUpdatePlaybackChannel" to "Živé aktualizácie prehrávania"),
    "hr" to mapOf("liveUpdatePlaybackChannel" to "Ažuriranja reprodukcije uživo"),
    "bg" to mapOf("liveUpdatePlaybackChannel" to "Актуализации на живо за възпроизвеждането"),
    "hu" to mapOf("liveUpdatePlaybackChannel" to "Lejátszás élő frissítései"),
    "nb" to mapOf("liveUpdatePlaybackChannel" to "Direkteoppdateringer for avspilling"),
    "ca" to mapOf("liveUpdatePlaybackChannel" to "Actualitzacions en directe de la reproducció"),
    "ms" to mapOf("liveUpdatePlaybackChannel" to "Kemas kini langsung main balik"),
    "fa" to mapOf("liveUpdatePlaybackChannel" to "به‌روزرسانی‌های زنده پخش"),
    "zh-Hant" to mapOf("liveUpdatePlaybackChannel" to "播放即時更新")
)

internal fun liveUpdateLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(liveUpdateBundles, code)

internal fun liveUpdateLocalizationCodes(): Set<String> = liveUpdateBundles.keys

internal val liveUpdateKeys: Set<String> = setOf("liveUpdatePlaybackChannel")
