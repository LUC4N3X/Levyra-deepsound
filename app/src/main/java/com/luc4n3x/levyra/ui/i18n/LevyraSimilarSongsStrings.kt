package com.luc4n3x.levyra.ui.i18n

internal val similarSongsKeys = setOf("startRadio")

private fun similarSongs(startRadio: String): Map<String, String> = mapOf(
    "startRadio" to startRadio
)

private val similarSongsBundles: Map<String, Map<String, String>> = mapOf(
    "en" to similarSongs("Start radio"),
    "it" to similarSongs("Avvia radio"),
    "es" to similarSongs("Iniciar radio"),
    "fr" to similarSongs("Lancer la radio"),
    "de" to similarSongs("Radio starten"),
    "pt" to similarSongs("Iniciar rádio"),
    "nl" to similarSongs("Radio starten"),
    "pl" to similarSongs("Włącz radio"),
    "ro" to similarSongs("Pornește radio"),
    "el" to similarSongs("Έναρξη ραδιοφώνου"),
    "sv" to similarSongs("Starta radio"),
    "da" to similarSongs("Start radio"),
    "cs" to similarSongs("Spustit rádio"),
    "uk" to similarSongs("Запустити радіо"),
    "ru" to similarSongs("Запустить радио"),
    "tr" to similarSongs("Radyoyu başlat"),
    "ar" to similarSongs("تشغيل الراديو"),
    "zh" to similarSongs("开始电台"),
    "ja" to similarSongs("ラジオを開始"),
    "ko" to similarSongs("라디오 시작"),
    "hi" to similarSongs("रेडियो शुरू करें"),
    "id" to similarSongs("Mulai radio"),
    "vi" to similarSongs("Bật radio"),
    "th" to similarSongs("เริ่มวิทยุ"),
    "fil" to similarSongs("Simulan ang radyo"),
    "he" to similarSongs("הפעלת רדיו")
)

internal fun similarSongsLocalizationEntries(code: String): Map<String, String> =
    similarSongsBundles.getValue(code)

internal fun similarSongsLocalizationCodes(): Set<String> = similarSongsBundles.keys
