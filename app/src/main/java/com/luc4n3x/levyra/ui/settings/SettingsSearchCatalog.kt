package com.luc4n3x.levyra.ui.settings

import com.luc4n3x.levyra.feature.settings.SettingsSearchEntry
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.automationCopy
import com.luc4n3x.levyra.ui.i18n.parametricEqCopy
import com.luc4n3x.levyra.ui.preloadNextTrack
import com.luc4n3x.levyra.ui.preloadNextTrackSubtitle
import java.util.Locale

internal const val SETTINGS_CATEGORY_AUDIO = "audio"

private class SettingsSearchCategory(val id: String, val label: String)

private class SettingsSearchCategories(strings: LevyraStrings, locale: Locale) {
    val design = SettingsSearchCategory("design", settingsCategoryTitle(strings.design, locale))
    val home = SettingsSearchCategory("home", settingsCategoryTitle(strings.homeInterfaceSection, locale))
    val player = SettingsSearchCategory("player", settingsCategoryTitle(strings.player, locale))
    val audio = SettingsSearchCategory(SETTINGS_CATEGORY_AUDIO, strings.audioEngine)
    val downloads = SettingsSearchCategory("downloads", settingsCategoryTitle(strings.downloads, locale))
    val lyrics = SettingsSearchCategory("lyrics", settingsCategoryTitle(strings.lyricsAnalysisSection, locale))
    val backup = SettingsSearchCategory("backup", strings.vaultTitle)
    val system = SettingsSearchCategory("system", settingsCategoryTitle(strings.preferences, locale))
    val app = SettingsSearchCategory("app", settingsCategoryTitle(strings.app, locale))
    val integrations = SettingsSearchCategory("integrations", settingsCategoryTitle(strings.integrations, locale))
    val network = SettingsSearchCategory("network", settingsCategoryTitle(strings.networkTitle, locale))
    val jam = SettingsSearchCategory("jam", settingsCategoryTitle(strings.jamTitle, locale))
}

internal fun settingsCategoryTitle(value: String, locale: Locale): String =
    value.trim().lowercase(locale).replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(locale) else character.toString()
    }

internal fun settingsSearchEntries(
    strings: LevyraStrings,
    locale: Locale,
    updatesAvailable: Boolean,
    aaudioOutputAvailable: Boolean
): List<SettingsSearchEntry> {
    val categories = SettingsSearchCategories(strings, locale)
    return buildList {
        appearanceEntries(strings, categories)
        playerEntries(strings, categories)
        audioEntries(strings, categories, aaudioOutputAvailable)
        libraryEntries(strings, categories)
        systemEntries(strings, categories, updatesAvailable)
    }
}

private fun MutableList<SettingsSearchEntry>.entry(
    id: String,
    title: String,
    description: String,
    keywords: String,
    category: SettingsSearchCategory
) {
    add(SettingsSearchEntry(id, title, description, keywords, category.id, category.label))
}

private fun MutableList<SettingsSearchEntry>.appearanceEntries(strings: LevyraStrings, categories: SettingsSearchCategories) {
    entry("design.theme_studio", strings.themeStudio, strings.themeStudioSubtitle, "${strings.theme} ${strings.themeAccent} theme color palette accent", categories.design)
    entry("design.app_font", strings.appFont, strings.appFontSubtitle, "font typography text", categories.design)
    entry("design.animations", strings.animations, strings.animationsSubtitle, "animation motion", categories.design)
    entry("design.visual_performance", strings.visualPerformance, strings.visualPerformanceFullSubtitle, "${strings.visualPerformanceAuto} ${strings.visualPerformanceSmooth} performance fps", categories.design)
    entry("design.liquid_glass", strings.liquidGlass, strings.liquidGlassSubtitle, "glass blur transparency", categories.design)
    entry("design.motion_artwork", strings.motionArtwork, strings.motionArtworkSubtitle, "canvas animated artwork video cover", categories.design)
    entry("design.canvas_quality", strings.canvasQuality, strings.canvasQualitySubtitle, "canvas video quality resolution", categories.design)
    entry("design.canvas_source", strings.canvasSource, strings.canvasSourceSubtitle, "canvas provider source", categories.design)
    entry("design.canvas_wifi_only", strings.motionArtworkWifiOnly, strings.motionArtworkWifiOnlySubtitle, "canvas wifi data", categories.design)
    entry("design.dynamic_color", strings.dynamicColor, strings.dynamicColorSubtitle, "material you color wallpaper", categories.design)
    entry("home.compact", strings.compactHome, strings.compactHomeSubtitle, "home layout", categories.home)
    entry("home.your_orbit", strings.yourOrbitSetting, strings.showPersonalListening, "personal orbit listening", categories.home)
    entry("home.voices", strings.voicesSetting, strings.voicesSettingSubtitle, "voices", categories.home)
    entry("home.new_releases", strings.newReleasesSetting, strings.showRecentReleases, "releases", categories.home)
    entry("home.release_radar", strings.releaseRadar, strings.followedArtistsSubtitle, "releases radar notifications artists", categories.home)
    entry("home.albums_for_you", strings.albumsForYouSetting, strings.showRecommendedAlbums, "albums recommendations", categories.home)
    entry("home.trending_artists", strings.trendingArtists, strings.showDiscoveredArtists, "artists trending", categories.home)
    entry("home.pure_black", strings.pureBlack, strings.pureBlackSubtitle, "amoled oled black dark", categories.home)
    entry("home.top_charts", strings.top50Charts, strings.showChartsCountry, "charts top 50", categories.home)
}

private fun MutableList<SettingsSearchEntry>.playerEntries(strings: LevyraStrings, categories: SettingsSearchCategories) {
    val automation = strings.automationCopy()
    entry("player.video_quality", strings.videoQuality, strings.videoQualitySubtitle, "video quality resolution 1080p 720p", categories.player)
    entry("player.visual_mode", strings.playerVisualMode, strings.playerVisualModeSubtitle, "canvas artwork immersive", categories.player)
    entry("player.background", strings.playerBackground, strings.playerBackgroundSubtitle, "background blur", categories.player)
    entry("player.haptics", strings.hapticFeedback, strings.hapticFeedbackSubtitle, "haptic vibration", categories.player)
    entry("player.gestures", strings.advancedGestures, strings.advancedGesturesSubtitle, "gestures swipe tap", categories.player)
    entry("player.gesture_horizontal", strings.playerGestureHorizontalSwipe, strings.playerGestureHorizontalSwipeSubtitle, "gestures swipe", categories.player)
    entry("player.gesture_double_tap", strings.playerGestureDoubleTapAction, strings.playerGestureDoubleTapActionSubtitle, "gestures double tap", categories.player)
    entry("player.double_tap_seek", strings.doubleTapSeek, strings.doubleTapSeekSubtitle, "gestures double tap seek", categories.player)
    entry("player.gesture_long_press", strings.playerGestureLongPressAction, strings.playerGestureLongPressActionSubtitle, "gestures long press", categories.player)
    entry("player.long_press", strings.longPress, strings.longPressSubtitle, "gestures long press", categories.player)
    entry("player.gesture_vertical", strings.playerGestureVerticalSwipe, strings.playerGestureVerticalSwipeSubtitle, "gestures swipe", categories.player)
    entry("player.ambient", strings.ambientOpen, strings.ambientModeSubtitle, "ambient standby clock", categories.player)
    entry("player.ambient_layout", strings.ambientLayout, strings.ambientLayoutSubtitle, "ambient layout", categories.player)
    entry("player.ambient_clock", strings.ambientShowClock, strings.ambientShowClockSubtitle, "ambient clock", categories.player)
    entry("player.ambient_title", strings.ambientShowTitle, strings.ambientShowTitleSubtitle, "ambient title", categories.player)
    entry("player.ambient_progress", strings.ambientShowProgress, strings.ambientShowProgressSubtitle, "ambient progress", categories.player)
    entry("player.ambient_amoled", strings.ambientAmoledBlack, strings.ambientAmoledBlackSubtitle, "ambient amoled black", categories.player)
    entry("player.ambient_brightness", strings.ambientBrightness, strings.ambientModeSubtitle, "ambient brightness", categories.player)
    entry("player.ambient_auto_dim", strings.ambientAutoDim, strings.ambientModeSubtitle, "ambient dim", categories.player)
    entry("player.ambient_pixel_shift", strings.ambientPixelShift, strings.ambientModeSubtitle, "ambient burn in oled", categories.player)
    entry("player.ambient_proximity", strings.ambientProximityBlackout, strings.ambientModeSubtitle, "ambient proximity sensor", categories.player)
    entry("player.ambient_lyrics", strings.ambientShowLyrics, strings.ambientModeSubtitle, "ambient lyrics", categories.player)
    entry("player.ambient_canvas", strings.ambientShowCanvas, strings.ambientModeSubtitle, "ambient canvas", categories.player)
    entry("player.excluded_artists", strings.excludedArtists, strings.excludedArtistsEmpty, "block hide artists", categories.player)
    entry("player.sponsorblock", strings.sponsorBlock, strings.sponsorBlockSubtitle, "sponsorblock segments skip", categories.player)
    entry("player.enhance_video_metadata", strings.enhanceVideoMetadata, strings.enhanceVideoMetadataSubtitle, "video metadata", categories.player)
    entry("player.skip_silence", strings.skipSilence, strings.skipSilenceSubtitle, "silence skip", categories.player)
    entry("player.bluetooth_resume", automation.bluetoothResume, automation.bluetoothResumeSubtitle, "bluetooth headphones car automation", categories.player)
    entry("player.pause_on_mute", automation.pauseOnMute, automation.pauseOnMuteSubtitle, "mute volume automation", categories.player)
    entry("player.auto_download_favorites", automation.autoDownloadFavorites, automation.autoDownloadFavoritesSubtitle, "download favorites automation", categories.player)
    entry("player.skip_unrecoverable", automation.skipUnrecoverable, automation.skipUnrecoverableSubtitle, "skip error automation", categories.player)
}

private fun MutableList<SettingsSearchEntry>.audioEntries(
    strings: LevyraStrings,
    categories: SettingsSearchCategories,
    aaudioOutputAvailable: Boolean
) {
    val parametric = strings.parametricEqCopy()
    entry("audio.engine", strings.audioEngine, strings.audioEngineSubtitle, "audio sound output", categories.audio)
    entry("audio.quality", strings.audioQuality, strings.audioSectionQuality, "${strings.audioQualityAuto} ${strings.audioQualityHigh} ${strings.audioQualityLow} quality bitrate streaming", categories.audio)
    entry("audio.alternative_hq", strings.alternativeAudioTitle, strings.alternativeAudioSubtitle, "${strings.alternativeAudioPrefer320} 320 kbps jiosaavn hq hifi bitrate", categories.audio)
    entry("audio.equalizer", strings.equalizer, strings.equalizerSubtitle, "${parametric.graphicEq} ${parametric.parametricEq} eq equaliser preset bands", categories.audio)
    entry("audio.autoeq", strings.autoEqCatalog, strings.autoEqCatalogHint, "${strings.autoEqImport} autoeq eq headphones", categories.audio)
    entry("audio.preamp", strings.preamp, strings.audioSectionEqualizer, "preamp gain eq", categories.audio)
    entry("audio.bass_boost", strings.bassBoost, strings.audioSectionEqualizer, "bass eq", categories.audio)
    entry("audio.virtualizer", strings.virtualizer, strings.audioSectionSpatial, "spatial surround stereo", categories.audio)
    entry("audio.enhanced", strings.enhancedAudioTitle, strings.enhancedAudioSubtitle, "enhance", categories.audio)
    entry("audio.limiter", strings.truePeakLimiter, strings.audioSectionDynamics, "limiter true peak clipping dbtp", categories.audio)
    entry("audio.replay_gain", strings.replayGain, strings.audioSectionDynamics, "replaygain normalization loudness volume clipping", categories.audio)
    entry("audio.crossfade", strings.crossfade, strings.audioSectionPlayback, "transition fade", categories.audio)
    entry("audio.dj_soft", strings.djSoft, strings.audioSectionPlayback, "dj transition automix", categories.audio)
    entry("audio.gapless", strings.gapless, strings.audioSectionPlayback, "gapless seamless", categories.audio)
    entry("audio.preload", strings.preloadNextTrack, strings.preloadNextTrackSubtitle, "preload prefetch cache buffer", categories.audio)
    entry("audio.efficiency", strings.audioEfficiencyTitle, strings.audioEfficiencySubtitle, "${strings.audioEfficiencyAutomatic} offload battery hardware efficiency power", categories.audio)
    if (aaudioOutputAvailable) {
        entry("audio.aaudio", strings.audioOutputAaudio, strings.audioOutputAaudioSubtitle, "aaudio latency output", categories.audio)
    }
    entry("audio.tempo", strings.tempo, strings.audioSectionPlayback, "tempo speed rate", categories.audio)
    entry("audio.pitch", strings.pitch, strings.audioSectionPlayback, "pitch key", categories.audio)
}

private fun MutableList<SettingsSearchEntry>.libraryEntries(strings: LevyraStrings, categories: SettingsSearchCategories) {
    entry("downloads.quality", strings.downloadQualityPreset, strings.downloadQualityPresetSubtitle, "quality bitrate format", categories.downloads)
    entry("downloads.location", strings.downloadLocation, strings.downloadLocationSubtitle, "folder storage sd card", categories.downloads)
    entry("downloads.folder_organization", strings.downloadFolderOrganization, strings.downloadFolderOrganizationSubtitle, "folder organize", categories.downloads)
    entry("downloads.speed_limit", strings.downloadSpeedLimit, strings.downloadSpeedLimitSubtitle, "speed bandwidth", categories.downloads)
    entry("downloads.wifi_only", strings.wifiOnly, strings.wifiOnlySubtitle, "wifi data", categories.downloads)
    entry("downloads.charging_only", strings.chargingOnly, strings.chargingOnlySubtitle, "battery charging", categories.downloads)
    entry("downloads.auto_resume", strings.automaticResume, strings.partialDownloadResume, "resume", categories.downloads)
    entry("downloads.simultaneous", strings.simultaneousDownloads, strings.simultaneousDownloadsSubtitle, "parallel concurrent", categories.downloads)
    entry("downloads.embed_metadata", strings.downloadEmbedMetadata, strings.downloadEmbedMetadataSubtitle, "metadata tags", categories.downloads)
    entry("downloads.embed_artwork", strings.downloadEmbedArtwork, strings.downloadEmbedArtworkSubtitle, "artwork cover", categories.downloads)
    entry("downloads.verify", strings.downloadVerifyFile, strings.downloadVerifyFileSubtitle, "verify integrity", categories.downloads)
    entry("downloads.skip_duplicates", strings.downloadSkipDuplicates, strings.downloadSkipDuplicatesSubtitle, "duplicates", categories.downloads)
    entry("lyrics.provider_priority", strings.lyricsProviderPriority, strings.lyricsProviderPrioritySubtitle, "${strings.lyrics} lyrics synced provider", categories.lyrics)
    entry("lyrics.analysis", strings.lyricsAnalysisCompact, strings.lyricsAnalysisCompactSubtitle, "${strings.lyrics} lyrics analysis", categories.lyrics)
    entry("backup.vault", strings.vaultTitle, strings.vaultSubtitle, "backup", categories.backup)
    entry("backup.automatic", strings.automaticBackup, strings.automaticBackupSubtitle, "backup schedule", categories.backup)
    entry("backup.frequency", strings.backupFrequency, strings.automaticBackupSubtitle, "backup schedule", categories.backup)
    entry("backup.retention", strings.backupRetention, strings.automaticBackupSubtitle, "backup retention", categories.backup)
    entry("backup.charging_only", strings.backupChargingOnly, strings.backupChargingOnlySubtitle, "backup charging battery", categories.backup)
    entry("backup.location", strings.backupLocation, strings.backupLocationSubtitle, "backup folder", categories.backup)
    entry("backup.before_updates", strings.backupBeforeUpdates, strings.backupBeforeUpdatesSubtitle, "backup update", categories.backup)
    entry("backup.now", strings.backupNow, strings.backupNowSubtitle, "backup", categories.backup)
    entry("backup.create", strings.createDataBackup, strings.createDataBackupSubtitle, "backup export", categories.backup)
    entry("backup.restore", strings.restoreBackup, strings.restoreBackupSubtitle, "backup restore import", categories.backup)
    entry("backup.manage", strings.manageBackups, strings.manageBackupsSubtitle, "backup", categories.backup)
}

private fun MutableList<SettingsSearchEntry>.systemEntries(
    strings: LevyraStrings,
    categories: SettingsSearchCategories,
    updatesAvailable: Boolean
) {
    entry("system.battery", strings.batteryUnrestricted, strings.batteryUnrestrictedSubtitle, "battery optimization background", categories.system)
    entry("system.playback_diagnostics", strings.playbackDiagnostics, strings.playbackDiagnosticsSubtitle, "diagnostics logs debug", categories.system)
    entry("system.export_diagnostics", strings.exportSafeDiagnostics, strings.safeDiagnosticsSubtitle, "diagnostics logs export", categories.system)
    entry("system.redo_questionnaire", strings.redoQuestionnaire, strings.redoQuestionnaireSubtitle, "onboarding genres taste", categories.system)
    entry("system.profile_photo", strings.profilePhoto, strings.profilePhotoAddSubtitle, "avatar photo profile", categories.system)
    entry("system.language", strings.language, strings.languageSubtitle, "language locale", categories.system)
    if (updatesAvailable) {
        entry("app.updates", strings.updates, strings.checkNewVersions, "version update", categories.app)
    }
    entry("integrations.lastfm", "Last.fm", strings.integrations, "lastfm scrobble scrobbling", categories.integrations)
    entry("integrations.listenbrainz", "ListenBrainz", strings.integrations, "listenbrainz scrobble scrobbling", categories.integrations)
    entry("integrations.audd", "AudD", strings.integrations, "audd recognition identify", categories.integrations)
    entry("network.dns", strings.networkDns, strings.networkSubtitle, "${strings.networkCustomDohUrl} dns doh", categories.network)
    entry("network.proxy", strings.networkProxy, strings.networkSubtitle, "${strings.networkProxyHost} ${strings.networkProxyAuthentication} proxy http socks", categories.network)
    entry("network.byedpi", strings.networkByeDpi, strings.networkByeDpiSubtitle, "byedpi dpi desync", categories.network)
    entry("network.bypass_streams", strings.networkBypassStreams, strings.networkBypassStreamsSubtitle, "proxy streams", categories.network)
    entry("network.restricted_compatibility", strings.networkRestrictedCompatibility, strings.networkRestrictedCompatibilitySubtitle, "youtube restricted", categories.network)
    entry("network.region_profile", strings.networkYoutubeRegionProfile, strings.networkYoutubeRegionProfileSubtitle, "youtube region country", categories.network)
    entry("jam.session", strings.jamTitle, strings.jamSubtitle, "jam session together", categories.jam)
}
