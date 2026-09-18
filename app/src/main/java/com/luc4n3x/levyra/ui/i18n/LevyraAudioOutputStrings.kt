package com.luc4n3x.levyra.ui.i18n

private fun audioOutputStrings(title: String, subtitle: String): Map<String, String> = mapOf(
    "audioOutputAaudio" to title,
    "audioOutputAaudioSubtitle" to subtitle
)

private val audioOutputBundles: Map<String, Map<String, String>> = mapOf(
    "en" to audioOutputStrings("AAudio output (Oboe)", "Native audio output through AAudio and Oboe"),
    "it" to audioOutputStrings("Uscita AAudio (Oboe)", "Uscita audio nativa tramite AAudio e Oboe"),
    "es" to audioOutputStrings("Salida AAudio (Oboe)", "Salida de audio nativa mediante AAudio y Oboe"),
    "fr" to audioOutputStrings("Sortie AAudio (Oboe)", "Sortie audio native via AAudio et Oboe"),
    "de" to audioOutputStrings("AAudio-Ausgabe (Oboe)", "Native Audioausgabe über AAudio und Oboe"),
    "pt" to audioOutputStrings("Saída AAudio (Oboe)", "Saída de áudio nativa através do AAudio e do Oboe"),
    "nl" to audioOutputStrings("AAudio-uitvoer (Oboe)", "Native audio-uitvoer via AAudio en Oboe"),
    "pl" to audioOutputStrings("Wyjście AAudio (Oboe)", "Natywne wyjście audio przez AAudio i Oboe"),
    "ro" to audioOutputStrings("Ieșire AAudio (Oboe)", "Ieșire audio nativă prin AAudio și Oboe"),
    "el" to audioOutputStrings("Έξοδος AAudio (Oboe)", "Εγγενής έξοδος ήχου μέσω AAudio και Oboe"),
    "sv" to audioOutputStrings("AAudio-utgång (Oboe)", "Inbyggd ljudutgång via AAudio och Oboe"),
    "da" to audioOutputStrings("AAudio-udgang (Oboe)", "Indbygget lydudgang via AAudio og Oboe"),
    "cs" to audioOutputStrings("Výstup AAudio (Oboe)", "Nativní zvukový výstup přes AAudio a Oboe"),
    "uk" to audioOutputStrings("Вихід AAudio (Oboe)", "Нативний аудіовихід через AAudio та Oboe"),
    "ru" to audioOutputStrings("Вывод AAudio (Oboe)", "Нативный вывод звука через AAudio и Oboe"),
    "tr" to audioOutputStrings("AAudio çıkışı (Oboe)", "AAudio ve Oboe ile yerel ses çıkışı"),
    "ar" to audioOutputStrings("إخراج AAudio (Oboe)", "إخراج صوت أصلي عبر AAudio وOboe"),
    "zh" to audioOutputStrings("AAudio 输出（Oboe）", "通过 AAudio 和 Oboe 进行原生音频输出"),
    "ja" to audioOutputStrings("AAudio 出力（Oboe）", "AAudio と Oboe によるネイティブ音声出力"),
    "ko" to audioOutputStrings("AAudio 출력(Oboe)", "AAudio와 Oboe를 통한 네이티브 오디오 출력"),
    "hi" to audioOutputStrings("AAudio आउटपुट (Oboe)", "AAudio और Oboe के ज़रिए नेटिव ऑडियो आउटपुट"),
    "id" to audioOutputStrings("Output AAudio (Oboe)", "Output audio native melalui AAudio dan Oboe"),
    "vi" to audioOutputStrings("Đầu ra AAudio (Oboe)", "Đầu ra âm thanh gốc qua AAudio và Oboe"),
    "th" to audioOutputStrings("เอาต์พุต AAudio (Oboe)", "เอาต์พุตเสียงแบบเนทีฟผ่าน AAudio และ Oboe"),
    "fil" to audioOutputStrings("AAudio na output (Oboe)", "Native na audio output gamit ang AAudio at Oboe"),
    "he" to audioOutputStrings("פלט AAudio (Oboe)", "פלט שמע מקורי דרך AAudio ו-Oboe"),
    "sk" to audioOutputStrings("Výstup AAudio (Oboe)", "Natívny zvukový výstup cez AAudio a Oboe"),
    "hr" to audioOutputStrings("AAudio izlaz (Oboe)", "Izvorni audioizlaz preko sučelja AAudio i Oboe"),
    "bg" to audioOutputStrings("Изход AAudio (Oboe)", "Вграден аудиоизход чрез AAudio и Oboe"),
    "hu" to audioOutputStrings("AAudio kimenet (Oboe)", "Natív hangkimenet az AAudio és az Oboe használatával"),
    "fi" to audioOutputStrings("AAudio-ulostulo (Oboe)", "Natiivi äänilähtö AAudion ja Oboen kautta"),
    "nb" to audioOutputStrings("AAudio-utgang (Oboe)", "Innebygd lydutgang via AAudio og Oboe"),
    "ca" to audioOutputStrings("Sortida AAudio (Oboe)", "Sortida d'àudio nativa mitjançant AAudio i Oboe"),
    "fa" to audioOutputStrings("خروجی AAudio (Oboe)", "خروجی صدای بومی از طریق AAudio و Oboe"),
    "zh-Hant" to audioOutputStrings("AAudio 輸出（Oboe）", "透過 AAudio 與 Oboe 進行原生音訊輸出"),
    "ms" to audioOutputStrings("Output AAudio (Oboe)", "Output audio asli melalui AAudio dan Oboe")
)

internal fun audioOutputLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(audioOutputBundles, code)

internal fun audioOutputLocalizationCodes(): Set<String> = audioOutputBundles.keys
