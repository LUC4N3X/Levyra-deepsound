package com.luc4n3x.levyra.data

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioPreset
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraBackupFrequency
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.LevyraDownloadFolderMode
import com.luc4n3x.levyra.domain.LevyraDownloadPreset
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

private const val PREFERENCES_NAME = "levyra_prefs"
internal const val DEFAULT_SPONSORBLOCK_ENABLED = true
internal const val JAM_DISPLAY_NAME_MAX_LENGTH = 32

internal fun normalizeJamDisplayName(value: String): String =
    value.trim().take(JAM_DISPLAY_NAME_MAX_LENGTH)

private val Context.levyraDataStore by preferencesDataStore(
    name = PREFERENCES_NAME,
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, PREFERENCES_NAME)) }
)

data class LevyraPreferencesSnapshot(
    val onboarded: Boolean,
    val tastes: Set<String>,
    val userName: String,
    val languageCode: String,
    val animationsEnabled: Boolean,
    val motionArtworkEnabled: Boolean,
    val dynamicColor: Boolean,
    val sponsorBlock: Boolean,
    val skipSilence: Boolean,
    val audioQuality: String,
    val dismissedUpdateVersion: String,
    val lastTrack: Track?,
    val lastPositionMs: Long,
    val recentSearches: List<Track>,
    val personalOrbitTracks: List<Track>,
    val audioNormalization: Boolean,
    val lyricsTranslationEnabled: Boolean,
    val themePreset: String,
    val audioSettings: LevyraAudioSettings,
    val interfaceSettings: LevyraInterfaceSettings,
    val downloadSettings: LevyraDownloadSettings,
    val backupSettings: LevyraBackupSettings,
    val jamDisplayName: String = ""
)

class LevyraPreferences(context: Context) {
    private val dataStore = context.applicationContext.levyraDataStore

    fun snapshot(): LevyraPreferencesSnapshot = read(defaultSnapshot()) { snapshotFrom(it) }

    suspend fun restoreSnapshot(snapshot: LevyraPreferencesSnapshot) {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(snapshot.languageCode)
        val normalizedAudio = snapshot.audioSettings.normalized()
        val normalizedInterface = snapshot.interfaceSettings.normalized()
        val normalizedDownloads = snapshot.downloadSettings.normalized()
        val normalizedBackup = snapshot.backupSettings.normalized()
        val recentSearchesJson = JSONArray().apply { snapshot.recentSearches.forEach { put(TrackJson.toJson(it)) } }.toString()
        val personalOrbitJson = JSONArray().apply {
            snapshot.personalOrbitTracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT).forEach { put(TrackJson.toJson(it)) }
        }.toString()
        dataStore.edit { mutable ->
            mutable[KEY_ONBOARDED] = snapshot.onboarded
            mutable[KEY_TASTES] = snapshot.tastes
            mutable[KEY_USER_NAME] = snapshot.userName
            mutable[KEY_LANGUAGE_CODE] = normalizedLanguage
            mutable[KEY_ANIMATIONS] = snapshot.animationsEnabled
            mutable[KEY_MOTION_ARTWORK] = snapshot.motionArtworkEnabled
            mutable[KEY_DYNAMIC_COLOR] = snapshot.dynamicColor
            mutable[KEY_SPONSORBLOCK] = snapshot.sponsorBlock
            mutable[KEY_SKIP_SILENCE] = snapshot.skipSilence
            mutable[KEY_AUDIO_QUALITY] = normalizeAudioQuality(snapshot.audioQuality)
            mutable[KEY_AUDIO_NORMALIZATION] = snapshot.audioNormalization
            mutable[KEY_LYRICS_TRANSLATION] = snapshot.lyricsTranslationEnabled
            mutable[KEY_THEME_PRESET] = com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(snapshot.themePreset)
            mutable[KEY_JAM_DISPLAY_NAME] = normalizeJamDisplayName(snapshot.jamDisplayName)
            mutable[KEY_AUDIO_EQ_ENABLED] = normalizedAudio.equalizerEnabled
            mutable[KEY_AUDIO_EQ_PRESET] = normalizedAudio.presetId
            mutable[KEY_AUDIO_EQ_BANDS] = normalizedAudio.bandLevels.joinToString(",")
            mutable[KEY_AUDIO_BASS_BOOST] = normalizedAudio.bassBoost
            mutable[KEY_AUDIO_VIRTUALIZER] = normalizedAudio.virtualizer
            mutable[KEY_AUDIO_PREAMP_DB] = normalizedAudio.preampDb
            mutable[KEY_AUDIO_CUSTOM_PRESETS] = customPresetsToJson(normalizedAudio.customPresets)
            mutable[KEY_AUDIO_LIMITER] = normalizedAudio.limiterEnabled
            mutable[KEY_AUDIO_CROSSFADE] = normalizedAudio.crossfadeSeconds
            mutable[KEY_AUDIO_DJ_SOFT] = normalizedAudio.djSoftMode
            mutable[KEY_AUDIO_REPLAY_GAIN] = normalizedAudio.replayGainEnabled
            mutable[KEY_AUDIO_SPEED] = normalizedAudio.playbackSpeed
            mutable[KEY_AUDIO_PITCH] = normalizedAudio.pitch
            mutable[KEY_AUDIO_GAPLESS] = normalizedAudio.gaplessEnabled
            mutable[KEY_UI_COMPACT_HOME] = normalizedInterface.compactHome
            mutable[KEY_UI_PERSONAL_ORBIT] = normalizedInterface.showPersonalOrbit
            mutable[KEY_UI_RESONANCE] = normalizedInterface.showResonance
            mutable[KEY_UI_NEW_RELEASES] = normalizedInterface.showNewReleases
            mutable[KEY_UI_ALBUMS] = normalizedInterface.showAlbumsForYou
            mutable[KEY_UI_ARTISTS] = normalizedInterface.showTrendingArtists
            mutable[KEY_UI_CHARTS] = normalizedInterface.showCharts
            mutable[KEY_UI_FONT_PRESET] = normalizedInterface.fontPreset.name
            mutable[KEY_UI_PLAYER_GESTURES] = normalizedInterface.playerGesturesEnabled
            mutable[KEY_UI_DOUBLE_TAP_SECONDS] = normalizedInterface.doubleTapSeekSeconds
            mutable[KEY_UI_LONG_PRESS_SPEED] = normalizedInterface.longPressSpeed
            mutable[KEY_UI_CANVAS_QUALITY] = normalizedInterface.canvasQuality.name
            mutable[KEY_UI_CANVAS_SOURCE] = normalizedInterface.canvasSource.name
            mutable[KEY_UI_ENHANCE_VIDEO_METADATA] = normalizedInterface.enhanceVideoMetadata
            mutable[KEY_UI_PURE_BLACK] = normalizedInterface.pureBlack
            mutable[KEY_UI_HAPTIC_FEEDBACK] = normalizedInterface.hapticFeedback
            mutable[KEY_DOWNLOAD_WIFI_ONLY] = normalizedDownloads.wifiOnly
            mutable[KEY_DOWNLOAD_CHARGING_ONLY] = normalizedDownloads.chargingOnly
            mutable[KEY_DOWNLOAD_RESUMABLE] = normalizedDownloads.resumable
            mutable[KEY_DOWNLOAD_CONCURRENCY] = normalizedDownloads.maxConcurrentDownloads
            mutable[KEY_DOWNLOAD_PRESET] = normalizedDownloads.preset.name
            mutable[KEY_DOWNLOAD_FOLDER_MODE] = normalizedDownloads.folderMode.name
            mutable[KEY_DOWNLOAD_MAX_RATE] = normalizedDownloads.maxRateKbps
            mutable[KEY_DOWNLOAD_EMBED_METADATA] = normalizedDownloads.embedMetadata
            mutable[KEY_DOWNLOAD_EMBED_ARTWORK] = normalizedDownloads.embedArtwork
            mutable[KEY_DOWNLOAD_VERIFY_FILE] = normalizedDownloads.verifyFile
            mutable[KEY_DOWNLOAD_SKIP_EXISTING] = normalizedDownloads.skipExisting
            mutable[KEY_BACKUP_ENABLED] = normalizedBackup.enabled
            mutable[KEY_BACKUP_FREQUENCY] = normalizedBackup.frequency.name
            mutable[KEY_BACKUP_RETENTION] = normalizedBackup.retentionCount
            mutable[KEY_BACKUP_CHARGING_ONLY] = normalizedBackup.chargingOnly
            mutable[KEY_BACKUP_PRE_UPDATE] = normalizedBackup.preUpdate
            mutable[KEY_RECENT_SEARCHES] = recentSearchesJson
            mutable[personalOrbitTracksKey(normalizedLanguage)] = personalOrbitJson
            if (snapshot.lastTrack == null) {
                mutable.remove(KEY_LAST_TRACK)
                mutable.remove(KEY_LAST_POSITION)
            } else {
                mutable[KEY_LAST_TRACK] = TrackJson.toJson(snapshot.lastTrack).toString()
                mutable[KEY_LAST_POSITION] = snapshot.lastPositionMs.coerceAtLeast(0L)
            }
        }
    }

    fun isOnboarded(): Boolean = read(false) { it[KEY_ONBOARDED] ?: false }

    fun setOnboarded(tastes: Set<String>) {
        setOnboardingState(true, tastes)
    }

    fun setOnboardingState(onboarded: Boolean, tastes: Set<String>) {
        write {
            it[KEY_ONBOARDED] = onboarded
            it[KEY_TASTES] = tastes
        }
    }

    fun tastes(): Set<String> = read(emptySet<String>()) { it[KEY_TASTES].orEmpty() }

    fun userName(): String = read("") { it[KEY_USER_NAME].orEmpty() }

    fun setUserName(name: String) {
        write { it[KEY_USER_NAME] = name }
    }

    fun languageCode(): String = read(LevyraLanguageCatalog.deviceDefault()) { LevyraLanguageCatalog.normalize(it[KEY_LANGUAGE_CODE].orEmpty().ifBlank { LevyraLanguageCatalog.deviceDefault() }) }

    fun setLanguageCode(code: String) {
        write { it[KEY_LANGUAGE_CODE] = LevyraLanguageCatalog.normalize(code) }
    }

    fun animationsEnabled(): Boolean = read(true) { it[KEY_ANIMATIONS] ?: true }

    fun setAnimationsEnabled(value: Boolean) {
        write { it[KEY_ANIMATIONS] = value }
    }

    suspend fun setMotionArtworkEnabled(value: Boolean) {
        try {
            dataStore.edit { it[KEY_MOTION_ARTWORK] = value }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "DataStore write failed")
        }
    }

    fun themePreset(): String = read(com.luc4n3x.levyra.ui.theme.LevyraThemes.APPLE_MUSIC) {
        com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(it[KEY_THEME_PRESET].orEmpty())
    }

    fun setThemePreset(value: String) {
        write { it[KEY_THEME_PRESET] = com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(value) }
    }

    fun dynamicColor(): Boolean = read(true) { it[KEY_DYNAMIC_COLOR] ?: true }

    fun setDynamicColor(value: Boolean) {
        write { it[KEY_DYNAMIC_COLOR] = value }
    }

    fun sponsorBlock(): Boolean = read(DEFAULT_SPONSORBLOCK_ENABLED) {
        it[KEY_SPONSORBLOCK] ?: DEFAULT_SPONSORBLOCK_ENABLED
    }

    fun setSponsorBlock(value: Boolean) {
        write { it[KEY_SPONSORBLOCK] = value }
    }

    fun skipSilence(): Boolean = read(false) { it[KEY_SKIP_SILENCE] ?: false }

    fun setSkipSilence(value: Boolean) {
        write { it[KEY_SKIP_SILENCE] = value }
    }

    fun audioNormalization(): Boolean = read(false) { it[KEY_AUDIO_NORMALIZATION] ?: false }

    fun setAudioNormalization(value: Boolean) {
        write { it[KEY_AUDIO_NORMALIZATION] = value }
    }

    fun lyricsTranslationEnabled(): Boolean = read(false) { it[KEY_LYRICS_TRANSLATION] ?: false }

    fun setLyricsTranslationEnabled(value: Boolean) {
        write { it[KEY_LYRICS_TRANSLATION] = value }
    }

    fun interfaceSettings(): LevyraInterfaceSettings = read(LevyraInterfaceSettings()) { interfaceSettingsFrom(it) }

    fun setInterfaceSettings(value: LevyraInterfaceSettings) {
        val normalized = value.normalized()
        write {
            it[KEY_UI_COMPACT_HOME] = normalized.compactHome
            it[KEY_UI_PERSONAL_ORBIT] = normalized.showPersonalOrbit
            it[KEY_UI_RESONANCE] = normalized.showResonance
            it[KEY_UI_NEW_RELEASES] = normalized.showNewReleases
            it[KEY_UI_ALBUMS] = normalized.showAlbumsForYou
            it[KEY_UI_ARTISTS] = normalized.showTrendingArtists
            it[KEY_UI_CHARTS] = normalized.showCharts
            it[KEY_UI_FONT_PRESET] = normalized.fontPreset.name
            it[KEY_UI_PLAYER_GESTURES] = normalized.playerGesturesEnabled
            it[KEY_UI_DOUBLE_TAP_SECONDS] = normalized.doubleTapSeekSeconds
            it[KEY_UI_LONG_PRESS_SPEED] = normalized.longPressSpeed
            it[KEY_UI_CANVAS_QUALITY] = normalized.canvasQuality.name
            it[KEY_UI_CANVAS_SOURCE] = normalized.canvasSource.name
            it[KEY_UI_ENHANCE_VIDEO_METADATA] = normalized.enhanceVideoMetadata
            it[KEY_UI_PURE_BLACK] = normalized.pureBlack
            it[KEY_UI_HAPTIC_FEEDBACK] = normalized.hapticFeedback
        }
    }

    fun ambientSettings(): LevyraAmbientSettings = read(LevyraAmbientSettings()) { ambientSettingsFrom(it) }

    fun setAmbientSettings(value: LevyraAmbientSettings) {
        val normalized = value.normalized()
        write {
            it[KEY_AMBIENT_BRIGHTNESS] = normalized.brightness
            it[KEY_AMBIENT_AUTO_DIM] = normalized.autoDim
            it[KEY_AMBIENT_AUTO_DIM_SECONDS] = normalized.autoDimAfterSeconds
            it[KEY_AMBIENT_PIXEL_SHIFT] = normalized.pixelShift
            it[KEY_AMBIENT_PROXIMITY_BLACKOUT] = normalized.proximityBlackout
            it[KEY_AMBIENT_SHOW_LYRICS] = normalized.showLyrics
            it[KEY_AMBIENT_SHOW_CANVAS] = normalized.showCanvas
        }
    }

    fun downloadSettings(): LevyraDownloadSettings = read(LevyraDownloadSettings()) { downloadSettingsFrom(it) }

    fun setDownloadSettings(value: LevyraDownloadSettings) {
        val normalized = value.normalized()
        write {
            it[KEY_DOWNLOAD_WIFI_ONLY] = normalized.wifiOnly
            it[KEY_DOWNLOAD_CHARGING_ONLY] = normalized.chargingOnly
            it[KEY_DOWNLOAD_RESUMABLE] = normalized.resumable
            it[KEY_DOWNLOAD_CONCURRENCY] = normalized.maxConcurrentDownloads
            it[KEY_DOWNLOAD_PRESET] = normalized.preset.name
            it[KEY_DOWNLOAD_FOLDER_MODE] = normalized.folderMode.name
            it[KEY_DOWNLOAD_MAX_RATE] = normalized.maxRateKbps
            it[KEY_DOWNLOAD_EMBED_METADATA] = normalized.embedMetadata
            it[KEY_DOWNLOAD_EMBED_ARTWORK] = normalized.embedArtwork
            it[KEY_DOWNLOAD_VERIFY_FILE] = normalized.verifyFile
            it[KEY_DOWNLOAD_SKIP_EXISTING] = normalized.skipExisting
        }
    }

    fun backupSettings(): LevyraBackupSettings = read(LevyraBackupSettings()) { backupSettingsFrom(it) }

    fun setBackupSettings(value: LevyraBackupSettings) {
        val normalized = value.normalized()
        write {
            it[KEY_BACKUP_ENABLED] = normalized.enabled
            it[KEY_BACKUP_FREQUENCY] = normalized.frequency.name
            it[KEY_BACKUP_RETENTION] = normalized.retentionCount
            it[KEY_BACKUP_CHARGING_ONLY] = normalized.chargingOnly
            it[KEY_BACKUP_PRE_UPDATE] = normalized.preUpdate
        }
    }

    fun lastBackupAt(): Long = read(0L) { it[KEY_VAULT_LAST_BACKUP] ?: 0L }

    fun setLastBackupAt(value: Long) {
        write { it[KEY_VAULT_LAST_BACKUP] = value.coerceAtLeast(0L) }
    }

    fun backupTreeUri(): String = read("") { it[KEY_VAULT_BACKUP_TREE_URI].orEmpty() }

    fun setBackupTreeUri(value: String) {
        write { it[KEY_VAULT_BACKUP_TREE_URI] = value }
    }

    suspend fun persistBackupTreeUri(value: String) {
        dataStore.edit { it[KEY_VAULT_BACKUP_TREE_URI] = value }
    }

    fun vaultRuntimeState(): Pair<Long, String> = read(0L to "") {
        (it[KEY_VAULT_LAST_BACKUP] ?: 0L) to it[KEY_VAULT_BACKUP_TREE_URI].orEmpty()
    }

    fun jamDisplayName(): String = read("") { it[KEY_JAM_DISPLAY_NAME].orEmpty() }

    fun setJamDisplayName(value: String) {
        write { it[KEY_JAM_DISPLAY_NAME] = normalizeJamDisplayName(value) }
    }

    fun audioSettings(): LevyraAudioSettings = read(LevyraAudioSettings()) { audioSettingsFrom(it) }

    fun setAudioSettings(value: LevyraAudioSettings) {
        val normalized = value.normalized()
        write {
            it[KEY_AUDIO_EQ_ENABLED] = normalized.equalizerEnabled
            it[KEY_AUDIO_EQ_PRESET] = normalized.presetId
            it[KEY_AUDIO_EQ_BANDS] = normalized.bandLevels.joinToString(",")
            it[KEY_AUDIO_BASS_BOOST] = normalized.bassBoost
            it[KEY_AUDIO_VIRTUALIZER] = normalized.virtualizer
            it[KEY_AUDIO_PREAMP_DB] = normalized.preampDb
            it[KEY_AUDIO_CUSTOM_PRESETS] = customPresetsToJson(normalized.customPresets)
            it[KEY_AUDIO_LIMITER] = normalized.limiterEnabled
            it[KEY_AUDIO_CROSSFADE] = normalized.crossfadeSeconds
            it[KEY_AUDIO_DJ_SOFT] = normalized.djSoftMode
            it[KEY_AUDIO_REPLAY_GAIN] = normalized.replayGainEnabled
            it[KEY_AUDIO_SPEED] = normalized.playbackSpeed
            it[KEY_AUDIO_PITCH] = normalized.pitch
            it[KEY_AUDIO_GAPLESS] = normalized.gaplessEnabled
        }
    }

    fun audioQuality(): String = read("Auto") { normalizeAudioQuality(it[KEY_AUDIO_QUALITY].orEmpty()) }

    fun setAudioQuality(value: String) {
        write { it[KEY_AUDIO_QUALITY] = normalizeAudioQuality(value) }
    }

    fun dismissedUpdateVersion(): String = read("") { it[KEY_DISMISSED_UPDATE_VERSION].orEmpty() }

    fun setDismissedUpdateVersion(version: String) {
        write { it[KEY_DISMISSED_UPDATE_VERSION] = version }
    }

    fun saveLastPlayback(track: Track?, positionMs: Long) {
        write {
            if (track == null) {
                it.remove(KEY_LAST_TRACK)
                it.remove(KEY_LAST_POSITION)
            } else {
                it[KEY_LAST_TRACK] = TrackJson.toJson(track).toString()
                it[KEY_LAST_POSITION] = positionMs.coerceAtLeast(0L)
            }
        }
    }

    fun lastTrack(): Track? = snapshot().lastTrack

    fun lastPositionMs(): Long = read(0L) { it[KEY_LAST_POSITION] ?: 0L }

    fun listeningLifetimeBackfillVersion(): Int = read(0) { it[KEY_LISTENING_LIFETIME_BACKFILL] ?: 0 }

    fun setListeningLifetimeBackfillVersion(value: Int) {
        write { it[KEY_LISTENING_LIFETIME_BACKFILL] = value.coerceAtLeast(0) }
    }

    fun listeningPulseLastPruneMs(): Long = read(0L) { it[KEY_LISTENING_PULSE_LAST_PRUNE] ?: 0L }

    fun setListeningPulseLastPruneMs(value: Long) {
        write { it[KEY_LISTENING_PULSE_LAST_PRUNE] = value.coerceAtLeast(0L) }
    }

    fun loadRecentSearches(): List<Track> = snapshot().recentSearches

    fun saveRecentSearches(tracks: List<Track>) {
        val array = JSONArray()
        tracks.forEach { array.put(TrackJson.toJson(it)) }
        write { it[KEY_RECENT_SEARCHES] = array.toString() }
    }

    fun loadHomeSections(languageCode: String = languageCode()): List<HomeSection> {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        return read(emptyList()) { preferences ->
            val localized = preferences[homeSectionsKey(normalized)].orEmpty()
            parseHomeSections(localized)
        }
    }

    fun saveHomeSections(sections: List<HomeSection>, languageCode: String = languageCode()) {
        val array = JSONArray()
        sections.take(10).forEach { section ->
            val tracks = JSONArray()
            section.tracks.take(20).forEach { track -> tracks.put(TrackJson.toJson(track)) }
            if (tracks.length() > 0) {
                array.put(JSONObject().put("title", section.title).put("tracks", tracks))
            }
        }
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        write { it[homeSectionsKey(normalized)] = array.toString() }
    }

    fun loadHomeAlbums(languageCode: String = languageCode()): List<AlbumHit> {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        return read(emptyList()) { preferences ->
            parseAlbumHits(preferences[homeAlbumsKey(normalized)].orEmpty())
        }
    }

    fun saveHomeAlbums(albums: List<AlbumHit>, languageCode: String = languageCode()) {
        val array = JSONArray()
        albums.take(14).forEach { album ->
            array.put(
                JSONObject()
                    .put("title", album.title)
                    .put("artist", album.artist)
                    .put("year", album.year)
                    .put("thumbnailUrl", album.thumbnailUrl)
                    .put("query", album.query)
                    .put("browseId", album.browseId)
                    .put("artistBrowseId", album.artistBrowseId)
                    .put("audioPlaylistId", album.audioPlaylistId)
                    .put("explicit", album.explicit)
                    .put("releaseDate", album.releaseDate)
                    .put("upc", album.upc)
                    .put("canonicalUrl", album.canonicalUrl)
                    .put("metadataProvider", album.metadataProvider)
                    .put("metadataConfidence", album.metadataConfidence)
            )
        }
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        write { it[homeAlbumsKey(normalized)] = array.toString() }
    }

    fun loadChartTracks(languageCode: String = languageCode(), regionId: String = ""): List<Track> {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        val chartRegion = regionId.ifBlank { com.luc4n3x.levyra.domain.ChartsCatalog.defaultRegionForLanguage(normalized).id }
        return read(emptyList()) { preferences ->
            val localized = preferences[chartTracksKey(normalized, chartRegion)].orEmpty()
            parseTrackList(localized)
        }
    }

    /**
     * Reads several chart regions from a single DataStore snapshot. Calling [loadChartTracks] once
     * per region blocks a thread on its own snapshot read each time, which is too expensive when
     * warming every region up front.
     */
    fun loadChartTracksByRegion(
        languageCode: String = languageCode(),
        regionIds: List<String>
    ): Map<String, List<Track>> {
        if (regionIds.isEmpty()) return emptyMap()
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        return read<Map<String, List<Track>>>(emptyMap()) { preferences ->
            val out = LinkedHashMap<String, List<Track>>(regionIds.size)
            regionIds.forEach { regionId ->
                val region = regionId.trim().lowercase()
                if (region.isBlank() || out.containsKey(region)) return@forEach
                val raw = preferences[chartTracksKey(normalized, region)].orEmpty()
                if (raw.isBlank()) return@forEach
                val tracks = parseTrackList(raw)
                if (tracks.isNotEmpty()) out[region] = tracks
            }
            out
        }
    }

    fun saveChartTracks(tracks: List<Track>, languageCode: String = languageCode(), regionId: String = "") {
        val array = JSONArray()
        tracks.take(50).forEach { track -> array.put(TrackJson.toJson(track)) }
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        val chartRegion = regionId.ifBlank { com.luc4n3x.levyra.domain.ChartsCatalog.defaultRegionForLanguage(normalized).id }
        write { it[chartTracksKey(normalized, chartRegion)] = array.toString() }
    }

    fun loadPersonalOrbitTracks(languageCode: String = languageCode()): List<Track> {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        return read(emptyList()) { preferences ->
            val localized = preferences[personalOrbitTracksKey(normalized)].orEmpty()
            parseTrackList(localized)
        }
    }

    fun savePersonalOrbitTracks(tracks: List<Track>, languageCode: String = languageCode()) {
        val array = JSONArray()
        tracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT).forEach { track -> array.put(TrackJson.toJson(track)) }
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        write { it[personalOrbitTracksKey(normalized)] = array.toString() }
    }

    private fun snapshotFrom(preferences: Preferences): LevyraPreferencesSnapshot {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(preferences[KEY_LANGUAGE_CODE].orEmpty().ifBlank { LevyraLanguageCatalog.deviceDefault() })
        val localizedOrbit = preferences[personalOrbitTracksKey(normalizedLanguage)].orEmpty()
        return LevyraPreferencesSnapshot(
            onboarded = preferences[KEY_ONBOARDED] ?: false,
            tastes = preferences[KEY_TASTES].orEmpty(),
            userName = preferences[KEY_USER_NAME].orEmpty(),
            languageCode = normalizedLanguage,
            animationsEnabled = preferences[KEY_ANIMATIONS] ?: true,
            motionArtworkEnabled = preferences[KEY_MOTION_ARTWORK] ?: true,
            dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            sponsorBlock = preferences[KEY_SPONSORBLOCK] ?: DEFAULT_SPONSORBLOCK_ENABLED,
            skipSilence = preferences[KEY_SKIP_SILENCE] ?: false,
            audioQuality = normalizeAudioQuality(preferences[KEY_AUDIO_QUALITY].orEmpty()),
            dismissedUpdateVersion = preferences[KEY_DISMISSED_UPDATE_VERSION].orEmpty(),
            lastTrack = parseTrack(preferences[KEY_LAST_TRACK].orEmpty(), "Last track restore failed"),
            lastPositionMs = preferences[KEY_LAST_POSITION] ?: 0L,
            recentSearches = parseTrackList(preferences[KEY_RECENT_SEARCHES].orEmpty()),
            personalOrbitTracks = parseTrackList(localizedOrbit),
            audioNormalization = preferences[KEY_AUDIO_NORMALIZATION] ?: false,
            lyricsTranslationEnabled = preferences[KEY_LYRICS_TRANSLATION] ?: false,
            themePreset = com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(preferences[KEY_THEME_PRESET].orEmpty()),
            audioSettings = audioSettingsFrom(preferences),
            interfaceSettings = interfaceSettingsFrom(preferences),
            downloadSettings = downloadSettingsFrom(preferences),
            backupSettings = backupSettingsFrom(preferences),
            jamDisplayName = preferences[KEY_JAM_DISPLAY_NAME].orEmpty()
        )
    }

    private fun defaultSnapshot(): LevyraPreferencesSnapshot = LevyraPreferencesSnapshot(
        onboarded = false,
        tastes = emptySet(),
        userName = "",
        languageCode = LevyraLanguageCatalog.deviceDefault(),
        animationsEnabled = true,
        motionArtworkEnabled = true,
        dynamicColor = true,
        sponsorBlock = DEFAULT_SPONSORBLOCK_ENABLED,
        skipSilence = false,
        audioQuality = "Auto",
        dismissedUpdateVersion = "",
        lastTrack = null,
        lastPositionMs = 0L,
        recentSearches = emptyList(),
        personalOrbitTracks = emptyList(),
        audioNormalization = false,
        lyricsTranslationEnabled = false,
        themePreset = com.luc4n3x.levyra.ui.theme.LevyraThemes.APPLE_MUSIC,
        audioSettings = LevyraAudioSettings(),
        interfaceSettings = LevyraInterfaceSettings(),
        downloadSettings = LevyraDownloadSettings(),
        backupSettings = LevyraBackupSettings(),
        jamDisplayName = ""
    )


    private fun interfaceSettingsFrom(preferences: Preferences): LevyraInterfaceSettings = LevyraInterfaceSettings(
        compactHome = preferences[KEY_UI_COMPACT_HOME] ?: false,
        showPersonalOrbit = preferences[KEY_UI_PERSONAL_ORBIT] ?: true,
        showResonance = preferences[KEY_UI_RESONANCE] ?: true,
        showNewReleases = preferences[KEY_UI_NEW_RELEASES] ?: true,
        showAlbumsForYou = preferences[KEY_UI_ALBUMS] ?: true,
        showTrendingArtists = preferences[KEY_UI_ARTISTS] ?: true,
        showCharts = preferences[KEY_UI_CHARTS] ?: true,
        fontPreset = LevyraFontPreset.from(preferences[KEY_UI_FONT_PRESET].orEmpty()),
        playerGesturesEnabled = preferences[KEY_UI_PLAYER_GESTURES] ?: true,
        doubleTapSeekSeconds = preferences[KEY_UI_DOUBLE_TAP_SECONDS] ?: 10,
        longPressSpeed = preferences[KEY_UI_LONG_PRESS_SPEED] ?: 2f,
        canvasQuality = LevyraCanvasQuality.from(preferences[KEY_UI_CANVAS_QUALITY].orEmpty()),
        canvasSource = LevyraCanvasSource.from(preferences[KEY_UI_CANVAS_SOURCE].orEmpty()),
        enhanceVideoMetadata = preferences[KEY_UI_ENHANCE_VIDEO_METADATA] ?: false,
        pureBlack = preferences[KEY_UI_PURE_BLACK] ?: false,
        hapticFeedback = preferences[KEY_UI_HAPTIC_FEEDBACK] ?: true
    ).normalized()

    private fun ambientSettingsFrom(preferences: Preferences): LevyraAmbientSettings = LevyraAmbientSettings(
        brightness = preferences[KEY_AMBIENT_BRIGHTNESS] ?: 0.35f,
        autoDim = preferences[KEY_AMBIENT_AUTO_DIM] ?: true,
        autoDimAfterSeconds = preferences[KEY_AMBIENT_AUTO_DIM_SECONDS] ?: 20,
        pixelShift = preferences[KEY_AMBIENT_PIXEL_SHIFT] ?: true,
        proximityBlackout = preferences[KEY_AMBIENT_PROXIMITY_BLACKOUT] ?: false,
        showLyrics = preferences[KEY_AMBIENT_SHOW_LYRICS] ?: true,
        showCanvas = preferences[KEY_AMBIENT_SHOW_CANVAS] ?: true
    ).normalized()

    private fun downloadSettingsFrom(preferences: Preferences): LevyraDownloadSettings = LevyraDownloadSettings(
        wifiOnly = preferences[KEY_DOWNLOAD_WIFI_ONLY] ?: false,
        chargingOnly = preferences[KEY_DOWNLOAD_CHARGING_ONLY] ?: false,
        resumable = preferences[KEY_DOWNLOAD_RESUMABLE] ?: true,
        maxConcurrentDownloads = preferences[KEY_DOWNLOAD_CONCURRENCY] ?: 2,
        preset = LevyraDownloadPreset.from(preferences[KEY_DOWNLOAD_PRESET].orEmpty()),
        folderMode = LevyraDownloadFolderMode.from(preferences[KEY_DOWNLOAD_FOLDER_MODE].orEmpty()),
        maxRateKbps = preferences[KEY_DOWNLOAD_MAX_RATE] ?: 0,
        embedMetadata = preferences[KEY_DOWNLOAD_EMBED_METADATA] ?: true,
        embedArtwork = preferences[KEY_DOWNLOAD_EMBED_ARTWORK] ?: true,
        verifyFile = preferences[KEY_DOWNLOAD_VERIFY_FILE] ?: true,
        skipExisting = preferences[KEY_DOWNLOAD_SKIP_EXISTING] ?: true
    ).normalized()

    private fun backupSettingsFrom(preferences: Preferences): LevyraBackupSettings = LevyraBackupSettings(
        enabled = preferences[KEY_BACKUP_ENABLED] ?: false,
        frequency = LevyraBackupFrequency.from(preferences[KEY_BACKUP_FREQUENCY].orEmpty()),
        retentionCount = preferences[KEY_BACKUP_RETENTION] ?: 5,
        chargingOnly = preferences[KEY_BACKUP_CHARGING_ONLY] ?: true,
        preUpdate = preferences[KEY_BACKUP_PRE_UPDATE] ?: true
    ).normalized()

    private fun homeSectionsKey(languageCode: String): Preferences.Key<String> = stringPreferencesKey("home_sections_v2_${LevyraLanguageCatalog.normalize(languageCode)}")

    private fun homeAlbumsKey(languageCode: String): Preferences.Key<String> = stringPreferencesKey("home_albums_${LevyraLanguageCatalog.normalize(languageCode)}")

    private fun chartTracksKey(languageCode: String, regionId: String): Preferences.Key<String> = stringPreferencesKey("chart_tracks_v2_${LevyraLanguageCatalog.normalize(languageCode)}_${regionId.lowercase()}")

    private fun personalOrbitTracksKey(languageCode: String): Preferences.Key<String> = stringPreferencesKey("personal_orbit_tracks_${LevyraLanguageCatalog.normalize(languageCode)}")

    private fun parseTrack(raw: String, warning: String): Track? {
        if (raw.isBlank()) return null
        return runCatching { TrackJson.fromJson(JSONObject(raw)) }
            .onFailure { Timber.w(it, warning) }
            .getOrNull()
    }

    private fun parseTrackList(raw: String): List<Track> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(TrackJson::fromJson) }
        }.onFailure { Timber.w(it, "Recent searches restore failed") }.getOrDefault(emptyList())
    }

    private fun parseHomeSections(raw: String): List<HomeSection> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val section = array.optJSONObject(index) ?: return@mapNotNull null
                val title = section.optString("title").ifBlank { "Per te" }
                val tracks = parseTrackList(section.optJSONArray("tracks")?.toString().orEmpty())
                HomeSection(title, tracks).takeIf { it.tracks.isNotEmpty() }
            }
        }.onFailure { Timber.w(it, "Home sections restore failed") }.getOrDefault(emptyList())
    }

    private fun parseAlbumHits(raw: String): List<AlbumHit> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val title = item.optString("title").trim()
                val artist = item.optString("artist").trim()
                val thumbnailUrl = item.optString("thumbnailUrl").trim()
                if (title.isBlank() || artist.isBlank() || thumbnailUrl.isBlank()) {
                    null
                } else {
                    AlbumHit(
                        title = title,
                        artist = artist,
                        year = item.optString("year").trim(),
                        thumbnailUrl = thumbnailUrl,
                        query = item.optString("query").trim().ifBlank { "$title $artist album" },
                        browseId = item.optString("browseId").trim(),
                        artistBrowseId = item.optString("artistBrowseId").trim(),
                        audioPlaylistId = item.optString("audioPlaylistId").trim(),
                        explicit = item.optBoolean("explicit"),
                        releaseDate = item.optString("releaseDate").trim(),
                        upc = item.optString("upc").trim(),
                        canonicalUrl = item.optString("canonicalUrl").trim(),
                        metadataProvider = item.optString("metadataProvider").trim(),
                        metadataConfidence = item.optInt("metadataConfidence").coerceIn(0, 100)
                    )
                }
            }
        }.onFailure { Timber.w(it, "Home albums restore failed") }.getOrDefault(emptyList())
    }

    private fun normalizeAudioQuality(value: String): String = when (value.lowercase()) {
        "high" -> "High"
        "low" -> "Low"
        else -> "Auto"
    }

    private fun audioSettingsFrom(preferences: Preferences): LevyraAudioSettings {
        val customPresets = customPresetsFromJson(preferences[KEY_AUDIO_CUSTOM_PRESETS].orEmpty())
        val storedPresetId = preferences[KEY_AUDIO_EQ_PRESET].orEmpty()
        val customPreset = customPresets.firstOrNull { it.id == storedPresetId }
        val presetId = customPreset?.id ?: LevyraAudioPresets.normalizePreset(storedPresetId)
        val fallbackLevels = customPreset?.levels ?: LevyraAudioPresets.levelsFor(presetId)
        val levels = parseBandLevels(preferences[KEY_AUDIO_EQ_BANDS].orEmpty()).takeIf { it.size == LevyraAudioPresets.bandCount } ?: fallbackLevels
        return LevyraAudioSettings(
            equalizerEnabled = preferences[KEY_AUDIO_EQ_ENABLED] ?: false,
            presetId = presetId,
            bandLevels = levels,
            bassBoost = preferences[KEY_AUDIO_BASS_BOOST] ?: (customPreset?.bassBoost ?: LevyraAudioPresets.preset(presetId).bassBoost),
            virtualizer = preferences[KEY_AUDIO_VIRTUALIZER] ?: (customPreset?.virtualizer ?: LevyraAudioPresets.preset(presetId).virtualizer),
            preampDb = preferences[KEY_AUDIO_PREAMP_DB] ?: 0f,
            limiterEnabled = preferences[KEY_AUDIO_LIMITER] ?: true,
            crossfadeSeconds = preferences[KEY_AUDIO_CROSSFADE] ?: 0,
            djSoftMode = preferences[KEY_AUDIO_DJ_SOFT] ?: false,
            replayGainEnabled = preferences[KEY_AUDIO_REPLAY_GAIN] ?: (preferences[KEY_AUDIO_NORMALIZATION] ?: false),
            playbackSpeed = preferences[KEY_AUDIO_SPEED] ?: 1f,
            pitch = preferences[KEY_AUDIO_PITCH] ?: 1f,
            gaplessEnabled = preferences[KEY_AUDIO_GAPLESS] ?: true,
            customPresets = customPresets
        ).normalized()
    }

    private fun parseBandLevels(raw: String): List<Int> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }
    }

    private fun <T> read(default: T, selector: (Preferences) -> T): T = runBlocking(Dispatchers.IO) {
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    Timber.w(error, "DataStore read failed")
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map(selector)
            .first() ?: default
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runBlocking(Dispatchers.IO) {
            runCatching { dataStore.edit(block) }.onFailure { Timber.w(it, "DataStore write failed") }
        }
    }

    private companion object {
        const val JAM_DISPLAY_NAME_MAX_LENGTH = 32
        val KEY_JAM_DISPLAY_NAME = stringPreferencesKey("jam_display_name")
        val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        val KEY_TASTES = stringSetPreferencesKey("tastes")
        val KEY_LAST_TRACK = stringPreferencesKey("last_track")
        val KEY_LAST_POSITION = longPreferencesKey("last_position")
        val KEY_ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val KEY_MOTION_ARTWORK = booleanPreferencesKey("motion_artwork_enabled")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_SPONSORBLOCK = booleanPreferencesKey("sponsorblock_enabled")
        val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val KEY_HOME_SECTIONS = stringPreferencesKey("home_sections")
        val KEY_CHART_TRACKS = stringPreferencesKey("chart_tracks")
        val KEY_PERSONAL_ORBIT_TRACKS = stringPreferencesKey("personal_orbit_tracks")
        val KEY_DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
        val KEY_AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
        val KEY_LYRICS_TRANSLATION = booleanPreferencesKey("lyrics_translation_enabled")
        val KEY_THEME_PRESET = stringPreferencesKey("theme_preset")
        val KEY_AUDIO_EQ_ENABLED = booleanPreferencesKey("audio_equalizer_enabled")
        val KEY_AUDIO_EQ_PRESET = stringPreferencesKey("audio_equalizer_preset")
        val KEY_AUDIO_EQ_BANDS = stringPreferencesKey("audio_equalizer_bands")
        val KEY_AUDIO_BASS_BOOST = intPreferencesKey("audio_bass_boost")
        val KEY_AUDIO_VIRTUALIZER = intPreferencesKey("audio_virtualizer")
        val KEY_AUDIO_PREAMP_DB = floatPreferencesKey("audio_preamp_db")
        val KEY_AUDIO_CUSTOM_PRESETS = stringPreferencesKey("audio_custom_presets")
        val KEY_AUDIO_LIMITER = booleanPreferencesKey("audio_limiter_enabled")
        val KEY_AUDIO_CROSSFADE = intPreferencesKey("audio_crossfade_seconds")
        val KEY_AUDIO_DJ_SOFT = booleanPreferencesKey("audio_dj_soft")
        val KEY_AUDIO_REPLAY_GAIN = booleanPreferencesKey("audio_replay_gain")
        val KEY_AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val KEY_AUDIO_PITCH = floatPreferencesKey("audio_pitch")
        val KEY_AUDIO_GAPLESS = booleanPreferencesKey("audio_gapless")
        val KEY_LISTENING_PULSE_LAST_PRUNE = longPreferencesKey("listening_pulse_last_prune")
        val KEY_LISTENING_LIFETIME_BACKFILL = intPreferencesKey("listening_lifetime_backfill")
        val KEY_UI_COMPACT_HOME = booleanPreferencesKey("ui_compact_home")
        val KEY_UI_PERSONAL_ORBIT = booleanPreferencesKey("ui_show_personal_orbit")
        val KEY_UI_RESONANCE = booleanPreferencesKey("ui_show_resonance")
        val KEY_UI_NEW_RELEASES = booleanPreferencesKey("ui_show_new_releases")
        val KEY_UI_ALBUMS = booleanPreferencesKey("ui_show_albums")
        val KEY_UI_ARTISTS = booleanPreferencesKey("ui_show_artists")
        val KEY_UI_CHARTS = booleanPreferencesKey("ui_show_charts")
        val KEY_UI_FONT_PRESET = stringPreferencesKey("ui_font_preset")
        val KEY_UI_PLAYER_GESTURES = booleanPreferencesKey("ui_player_gestures")
        val KEY_UI_PURE_BLACK = booleanPreferencesKey("ui_pure_black")
        val KEY_UI_HAPTIC_FEEDBACK = booleanPreferencesKey("ui_haptic_feedback")
        val KEY_UI_DOUBLE_TAP_SECONDS = intPreferencesKey("ui_double_tap_seconds")
        val KEY_UI_LONG_PRESS_SPEED = floatPreferencesKey("ui_long_press_speed")
        val KEY_UI_CANVAS_QUALITY = stringPreferencesKey("ui_canvas_quality")
        val KEY_UI_CANVAS_SOURCE = stringPreferencesKey("ui_canvas_source")
        val KEY_UI_ENHANCE_VIDEO_METADATA = booleanPreferencesKey("ui_enhance_video_metadata")
        val KEY_AMBIENT_BRIGHTNESS = floatPreferencesKey("ambient_brightness")
        val KEY_AMBIENT_AUTO_DIM = booleanPreferencesKey("ambient_auto_dim")
        val KEY_AMBIENT_AUTO_DIM_SECONDS = intPreferencesKey("ambient_auto_dim_seconds")
        val KEY_AMBIENT_PIXEL_SHIFT = booleanPreferencesKey("ambient_pixel_shift")
        val KEY_AMBIENT_PROXIMITY_BLACKOUT = booleanPreferencesKey("ambient_proximity_blackout")
        val KEY_AMBIENT_SHOW_LYRICS = booleanPreferencesKey("ambient_show_lyrics")
        val KEY_AMBIENT_SHOW_CANVAS = booleanPreferencesKey("ambient_show_canvas")
        val KEY_DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val KEY_DOWNLOAD_CHARGING_ONLY = booleanPreferencesKey("download_charging_only")
        val KEY_DOWNLOAD_RESUMABLE = booleanPreferencesKey("download_resumable")
        val KEY_DOWNLOAD_CONCURRENCY = intPreferencesKey("download_concurrency")
        val KEY_DOWNLOAD_PRESET = stringPreferencesKey("download_preset")
        val KEY_DOWNLOAD_FOLDER_MODE = stringPreferencesKey("download_folder_mode")
        val KEY_DOWNLOAD_MAX_RATE = intPreferencesKey("download_max_rate_kbps")
        val KEY_DOWNLOAD_EMBED_METADATA = booleanPreferencesKey("download_embed_metadata")
        val KEY_DOWNLOAD_EMBED_ARTWORK = booleanPreferencesKey("download_embed_artwork")
        val KEY_DOWNLOAD_VERIFY_FILE = booleanPreferencesKey("download_verify_file")
        val KEY_DOWNLOAD_SKIP_EXISTING = booleanPreferencesKey("download_skip_existing")
        val KEY_BACKUP_ENABLED = booleanPreferencesKey("automatic_backup_enabled")
        val KEY_BACKUP_FREQUENCY = stringPreferencesKey("automatic_backup_frequency")
        val KEY_BACKUP_RETENTION = intPreferencesKey("automatic_backup_retention")
        val KEY_BACKUP_CHARGING_ONLY = booleanPreferencesKey("automatic_backup_charging_only")
        val KEY_BACKUP_PRE_UPDATE = booleanPreferencesKey("automatic_backup_pre_update")
        val KEY_VAULT_LAST_BACKUP = longPreferencesKey("vault_last_backup_at")
        val KEY_VAULT_BACKUP_TREE_URI = stringPreferencesKey("vault_backup_tree_uri")
    }
}

internal fun customPresetToJson(preset: LevyraAudioPreset): JSONObject = JSONObject()
    .put("id", preset.id)
    .put("label", preset.fallbackLabel)
    .put("levels", JSONArray(preset.levels))
    .put("bassBoost", preset.bassBoost)
    .put("virtualizer", preset.virtualizer)
    .put("preampDb", preset.preampDb.toDouble())

internal fun customPresetsToJson(presets: List<LevyraAudioPreset>): String =
    JSONArray().apply { presets.forEach { put(customPresetToJson(it)) } }.toString()

internal fun customPresetFromJson(json: JSONObject): LevyraAudioPreset? {
    val id = json.optString("id").trim()
    if (!id.startsWith(LevyraAudioPresets.CUSTOM_PRESET_PREFIX)) return null
    val levelsArray = json.optJSONArray("levels") ?: return null
    val levels = buildList {
        for (index in 0 until levelsArray.length()) add(levelsArray.optInt(index))
    }
    if (levels.size != LevyraAudioPresets.bandCount) return null
    return LevyraAudioPreset(
        id = id,
        fallbackLabel = json.optString("label").ifBlank { "Custom" },
        levels = levels,
        bassBoost = json.optInt("bassBoost"),
        virtualizer = json.optInt("virtualizer"),
        preampDb = json.optDouble("preampDb", 0.0).toFloat()
    )
}

internal fun customPresetsFromJson(value: String): List<LevyraAudioPreset> = runCatching {
    val array = JSONArray(value)
    buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let(::customPresetFromJson)?.let(::add)
        }
    }
}.getOrDefault(emptyList())
