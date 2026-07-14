package com.luc4n3x.levyra.domain

data class LevyraInterfaceSettings(
    val compactHome: Boolean = false,
    val showPersonalOrbit: Boolean = true,
    val showResonance: Boolean = true,
    val showNewReleases: Boolean = true,
    val showAlbumsForYou: Boolean = true,
    val showTrendingArtists: Boolean = true,
    val showCharts: Boolean = true,
    val playerGesturesEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val longPressSpeed: Float = 2f
) {
    fun normalized(): LevyraInterfaceSettings = copy(
        doubleTapSeekSeconds = doubleTapSeekSeconds.coerceIn(5, 30),
        longPressSpeed = longPressSpeed.coerceIn(1.25f, 3f)
    )
}

data class LevyraDownloadSettings(
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val resumable: Boolean = true,
    val maxConcurrentDownloads: Int = 2
) {
    fun normalized(): LevyraDownloadSettings = copy(
        maxConcurrentDownloads = maxConcurrentDownloads.coerceIn(1, 4)
    )
}

data class LevyraIntelligenceSummary(
    val overview: String = "",
    val mood: String = "",
    val themes: List<String> = emptyList(),
    val repeatedPhrases: List<String> = emptyList(),
    val lexicalDensity: Int = 0,
    val lineCount: Int = 0,
    val wordCount: Int = 0,
    val localOnly: Boolean = true
) {
    val available: Boolean
        get() = overview.isNotBlank() || themes.isNotEmpty() || repeatedPhrases.isNotEmpty()
}

data class OfflineDownloadTask(
    val taskKey: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val state: String,
    val progress: Int,
    val error: String
)
