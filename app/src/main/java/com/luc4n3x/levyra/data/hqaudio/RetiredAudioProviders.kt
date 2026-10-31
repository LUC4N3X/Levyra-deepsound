package com.luc4n3x.levyra.data.hqaudio

internal object RetiredAudioProviders {
    const val QOBUZ_ID = "qobuz"
    const val TIDAL_ID = "tidal"
    const val QOBUZ_LOOKUP_BUDGET_MS = 4_000L

    fun isTidalManifestUri(value: String): Boolean = value.startsWith("file://", ignoreCase = true) &&
        value.contains("/tidal-manifests/") && value.substringBefore('?').endsWith(".mpd")
}
