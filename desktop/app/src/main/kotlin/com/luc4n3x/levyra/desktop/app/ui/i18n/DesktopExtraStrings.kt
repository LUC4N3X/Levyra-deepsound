package com.luc4n3x.levyra.desktop.app.ui.i18n

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties

data class DesktopExtraStrings(
    val sleepTimer: String,
    val sleepTimerOff: String,
    val sleepTimerEndOfTrack: String,
    val playbackSpeed: String,
    val preloadNext: String,
    val preloadNextBody: String,
    val mediaKeys: String,
    val mediaKeysBody: String,
    val shortcuts: String,
    val seek: String,
    val miniPlayer: String,
    val audioOutput: String,
    val audioOutputBody: String,
    val audioOutputSystemDefault: String,
    val audioOutputRefresh: String,
    val audioOutputUnavailable: String,
    val audioOutputEmpty: String,
    val equalizerPreset: String,
    val equalizerPresetFlat: String,
    val equalizerPresetBassBoost: String,
    val equalizerPresetVocal: String,
    val equalizerPresetRock: String,
    val equalizerPresetPop: String,
    val equalizerPresetElectronic: String,
    val equalizerPresetHipHop: String,
    val equalizerPresetClassical: String,
    val equalizerPresetAcoustic: String,
    val equalizerPresetNight: String,
    val equalizerPresetCustom: String,
    val localMusic: String,
    val localMusicSubtitle: String,
    val localMusicAddFolder: String,
    val localMusicEmptyTitle: String,
    val localMusicScan: String,
    val localMusicDeepScan: String,
    val localMusicScanning: String,
    val localMusicMissingFiles: String,
    val localMusicForgetMissing: String,
    val localMusicRemoveFolder: String,
    val playlistImport: String,
    val playlistExport: String,
    val playlistSkippedEntries: String
)

internal object DesktopExtras {

    fun forTag(tag: String): DesktopExtraStrings = catalog[tag] ?: catalog.getValue(FALLBACK_TAG)

    fun supportedTags(): Set<String> = catalog.keys

    private const val RESOURCE = "/i18n/desktop-extras.properties"
    private const val FALLBACK_TAG = "en"

    private val catalog: Map<String, DesktopExtraStrings> by lazy { load() }

    private fun load(): Map<String, DesktopExtraStrings> {
        val entries = readEntries()
        val tags = entries.stringPropertyNames()
            .mapNotNull { name -> name.substringBefore('.', "").takeIf { it.isNotBlank() } }
            .toSortedSet()
        require(FALLBACK_TAG in tags) { "Desktop extra strings are missing the $FALLBACK_TAG bundle" }
        return tags.associateWith { tag -> bundle(entries, tag) }
    }

    private fun readEntries(): Properties {
        val stream = requireNotNull(DesktopExtras::class.java.getResourceAsStream(RESOURCE)) {
            "Desktop extra strings resource not found: $RESOURCE"
        }
        return Properties().apply {
            stream.use { source -> load(InputStreamReader(source, StandardCharsets.UTF_8)) }
        }
    }

    private fun bundle(entries: Properties, tag: String): DesktopExtraStrings = DesktopExtraStrings(
        sleepTimer = value(entries, tag, "sleepTimer"),
        sleepTimerOff = value(entries, tag, "sleepTimerOff"),
        sleepTimerEndOfTrack = value(entries, tag, "sleepTimerEndOfTrack"),
        playbackSpeed = value(entries, tag, "playbackSpeed"),
        preloadNext = value(entries, tag, "preloadNext"),
        preloadNextBody = value(entries, tag, "preloadNextBody"),
        mediaKeys = value(entries, tag, "mediaKeys"),
        mediaKeysBody = value(entries, tag, "mediaKeysBody"),
        shortcuts = value(entries, tag, "shortcuts"),
        seek = value(entries, tag, "seek"),
        miniPlayer = value(entries, tag, "miniPlayer"),
        audioOutput = value(entries, tag, "audioOutput"),
        audioOutputBody = value(entries, tag, "audioOutputBody"),
        audioOutputSystemDefault = value(entries, tag, "audioOutputSystemDefault"),
        audioOutputRefresh = value(entries, tag, "audioOutputRefresh"),
        audioOutputUnavailable = value(entries, tag, "audioOutputUnavailable"),
        audioOutputEmpty = value(entries, tag, "audioOutputEmpty"),
        equalizerPreset = value(entries, tag, "equalizerPreset"),
        equalizerPresetFlat = value(entries, tag, "equalizerPresetFlat"),
        equalizerPresetBassBoost = value(entries, tag, "equalizerPresetBassBoost"),
        equalizerPresetVocal = value(entries, tag, "equalizerPresetVocal"),
        equalizerPresetRock = value(entries, tag, "equalizerPresetRock"),
        equalizerPresetPop = value(entries, tag, "equalizerPresetPop"),
        equalizerPresetElectronic = value(entries, tag, "equalizerPresetElectronic"),
        equalizerPresetHipHop = value(entries, tag, "equalizerPresetHipHop"),
        equalizerPresetClassical = value(entries, tag, "equalizerPresetClassical"),
        equalizerPresetAcoustic = value(entries, tag, "equalizerPresetAcoustic"),
        equalizerPresetNight = value(entries, tag, "equalizerPresetNight"),
        equalizerPresetCustom = value(entries, tag, "equalizerPresetCustom"),
        localMusic = value(entries, tag, "localMusic"),
        localMusicSubtitle = value(entries, tag, "localMusicSubtitle"),
        localMusicAddFolder = value(entries, tag, "localMusicAddFolder"),
        localMusicEmptyTitle = value(entries, tag, "localMusicEmptyTitle"),
        localMusicScan = value(entries, tag, "localMusicScan"),
        localMusicDeepScan = value(entries, tag, "localMusicDeepScan"),
        localMusicScanning = value(entries, tag, "localMusicScanning"),
        localMusicMissingFiles = value(entries, tag, "localMusicMissingFiles"),
        localMusicForgetMissing = value(entries, tag, "localMusicForgetMissing"),
        localMusicRemoveFolder = value(entries, tag, "localMusicRemoveFolder"),
        playlistImport = value(entries, tag, "playlistImport"),
        playlistExport = value(entries, tag, "playlistExport"),
        playlistSkippedEntries = value(entries, tag, "playlistSkippedEntries")
    )

    private fun value(entries: Properties, tag: String, key: String): String {
        entries.getProperty("$tag.$key")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val fallback = entries.getProperty("$FALLBACK_TAG.$key")?.trim().orEmpty()
        require(fallback.isNotEmpty()) { "Desktop extra string $key is missing for $tag and $FALLBACK_TAG" }
        return fallback
    }
}
