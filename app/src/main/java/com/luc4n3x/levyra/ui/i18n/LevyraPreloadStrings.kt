package com.luc4n3x.levyra.ui.i18n

private fun preloadStrings(title: String, subtitle: String): Map<String, String> = mapOf(
    "preloadNextTrack" to title,
    "preloadNextTrackSubtitle" to subtitle
)

private val preloadBundles: Map<String, Map<String, String>> = mapOf(
    "en" to preloadStrings(
        "Preload next track",
        "Prepare the next track in advance for faster transitions. Disable to reduce background network activity."
    ),
    "it" to preloadStrings(
        "Precarica il brano successivo",
        "Prepara in anticipo il brano successivo per passaggi più rapidi. Disattiva per ridurre l'attività di rete in background."
    )
)

internal val preloadKeys = setOf("preloadNextTrack", "preloadNextTrackSubtitle")

internal fun preloadLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(preloadBundles, code)
