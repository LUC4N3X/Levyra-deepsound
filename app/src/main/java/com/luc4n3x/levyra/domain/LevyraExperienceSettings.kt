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

enum class LevyraDownloadPreset {
    Automatic,
    HighQuality,
    DataSaver;

    companion object {
        fun from(value: String): LevyraDownloadPreset = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Automatic
    }
}

enum class LevyraDownloadFolderMode {
    Flat,
    Artist,
    ArtistAlbum;

    companion object {
        fun from(value: String): LevyraDownloadFolderMode = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ArtistAlbum
    }
}

data class LevyraDownloadSettings(
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val resumable: Boolean = true,
    val maxConcurrentDownloads: Int = 2,
    val preset: LevyraDownloadPreset = LevyraDownloadPreset.Automatic,
    val folderMode: LevyraDownloadFolderMode = LevyraDownloadFolderMode.ArtistAlbum,
    val maxRateKbps: Int = 0,
    val embedMetadata: Boolean = true,
    val embedArtwork: Boolean = true,
    val verifyFile: Boolean = true,
    val skipExisting: Boolean = true
) {
    fun normalized(): LevyraDownloadSettings = copy(
        maxConcurrentDownloads = maxConcurrentDownloads.coerceIn(1, 4),
        maxRateKbps = maxRateKbps.takeIf { it in setOf(0, 512, 1024, 2048, 4096, 8192) } ?: 0
    )

    val effectiveRateKbps: Int
        get() = maxRateKbps.coerceAtLeast(0)

    val maxParallelFragments: Int
        get() = when (preset) {
            LevyraDownloadPreset.HighQuality -> 12
            LevyraDownloadPreset.Automatic -> 8
            LevyraDownloadPreset.DataSaver -> 4
        }

    val resolverAudioQuality: String?
        get() = when (preset) {
            LevyraDownloadPreset.HighQuality -> "High"
            LevyraDownloadPreset.DataSaver -> "Low"
            LevyraDownloadPreset.Automatic -> null
        }

    val storedPresetKey: String
        get() = preset.name

    fun storedQualityKey(automaticQuality: String = "Auto"): String {
        return resolverAudioQuality ?: automaticQuality.trim().ifBlank { "Auto" }
    }
}

internal fun LevyraDownloadSettings.shouldSkipExistingDownload(
    trackId: String,
    downloadedTrackIds: Set<String>
): Boolean {
    return skipExisting && trackId.isNotBlank() && trackId in downloadedTrackIds
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
