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
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.Track
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
    val downloadSettings: LevyraDownloadSettings
)

class LevyraPreferences(context: Context) {
    private val dataStore = context.applicationContext.levyraDataStore

    fun snapshot(): LevyraPreferencesSnapshot = read(defaultSnapshot()) { snapshotFrom(it) }

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

    fun themePreset(): String = read(com.luc4n3x.levyra.ui.theme.LevyraThemes.COSMIC) {
        com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(it[KEY_THEME_PRESET].orEmpty())
    }

    fun setThemePreset(value: String) {
        write { it[KEY_THEME_PRESET] = com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(value) }
    }

    fun dynamicColor(): Boolean = read(true) { it[KEY_DYNAMIC_COLOR] ?: true }

    fun setDynamicColor(value: Boolean) {
        write { it[KEY_DYNAMIC_COLOR] = value }
    }

    fun sponsorBlock(): Boolean = read(true) { it[KEY_SPONSORBLOCK] ?: true }

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
            it[KEY_UI_PLAYER_GESTURES] = normalized.playerGesturesEnabled
            it[KEY_UI_DOUBLE_TAP_SECONDS] = normalized.doubleTapSeekSeconds
            it[KEY_UI_LONG_PRESS_SPEED] = normalized.longPressSpeed
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
        }
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
            val legacy = if (normalized == "it") preferences[KEY_HOME_SECTIONS].orEmpty() else ""
            parseHomeSections(localized.ifBlank { legacy })
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
            val legacy = if (normalized == "it" && chartRegion == "it") preferences[KEY_CHART_TRACKS].orEmpty() else ""
            parseTrackList(localized.ifBlank { legacy })
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
            dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            sponsorBlock = preferences[KEY_SPONSORBLOCK] ?: true,
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
            downloadSettings = downloadSettingsFrom(preferences)
        )
    }

    private fun defaultSnapshot(): LevyraPreferencesSnapshot = LevyraPreferencesSnapshot(
        onboarded = false,
        tastes = emptySet(),
        userName = "",
        languageCode = LevyraLanguageCatalog.deviceDefault(),
        animationsEnabled = true,
        dynamicColor = true,
        sponsorBlock = true,
        skipSilence = false,
        audioQuality = "Auto",
        dismissedUpdateVersion = "",
        lastTrack = null,
        lastPositionMs = 0L,
        recentSearches = emptyList(),
        personalOrbitTracks = emptyList(),
        audioNormalization = false,
        lyricsTranslationEnabled = false,
        themePreset = com.luc4n3x.levyra.ui.theme.LevyraThemes.COSMIC,
        audioSettings = LevyraAudioSettings(),
        interfaceSettings = LevyraInterfaceSettings(),
        downloadSettings = LevyraDownloadSettings()
    )


    private fun interfaceSettingsFrom(preferences: Preferences): LevyraInterfaceSettings = LevyraInterfaceSettings(
        compactHome = preferences[KEY_UI_COMPACT_HOME] ?: false,
        showPersonalOrbit = preferences[KEY_UI_PERSONAL_ORBIT] ?: true,
        showResonance = preferences[KEY_UI_RESONANCE] ?: true,
        showNewReleases = preferences[KEY_UI_NEW_RELEASES] ?: true,
        showAlbumsForYou = preferences[KEY_UI_ALBUMS] ?: true,
        showTrendingArtists = preferences[KEY_UI_ARTISTS] ?: true,
        showCharts = preferences[KEY_UI_CHARTS] ?: true,
        playerGesturesEnabled = preferences[KEY_UI_PLAYER_GESTURES] ?: true,
        doubleTapSeekSeconds = preferences[KEY_UI_DOUBLE_TAP_SECONDS] ?: 10,
        longPressSpeed = preferences[KEY_UI_LONG_PRESS_SPEED] ?: 2f
    ).normalized()

    private fun downloadSettingsFrom(preferences: Preferences): LevyraDownloadSettings = LevyraDownloadSettings(
        wifiOnly = preferences[KEY_DOWNLOAD_WIFI_ONLY] ?: false,
        chargingOnly = preferences[KEY_DOWNLOAD_CHARGING_ONLY] ?: false,
        resumable = preferences[KEY_DOWNLOAD_RESUMABLE] ?: true,
        maxConcurrentDownloads = preferences[KEY_DOWNLOAD_CONCURRENCY] ?: 2
    ).normalized()

    private fun homeSectionsKey(languageCode: String): Preferences.Key<String> = stringPreferencesKey("home_sections_${LevyraLanguageCatalog.normalize(languageCode)}")

    private fun homeAlbumsKey(languageCode: String): Preferences.Key<String> = stringPreferencesKey("home_albums_${LevyraLanguageCatalog.normalize(languageCode)}")

    private fun chartTracksKey(languageCode: String, regionId: String): Preferences.Key<String> = stringPreferencesKey("chart_tracks_${LevyraLanguageCatalog.normalize(languageCode)}_${regionId.lowercase()}")

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
                        artistBrowseId = item.optString("artistBrowseId").trim()
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
        val presetId = LevyraAudioPresets.normalizePreset(preferences[KEY_AUDIO_EQ_PRESET].orEmpty())
        val fallbackLevels = LevyraAudioPresets.levelsFor(presetId)
        val levels = parseBandLevels(preferences[KEY_AUDIO_EQ_BANDS].orEmpty()).takeIf { it.size == LevyraAudioPresets.bandCount } ?: fallbackLevels
        return LevyraAudioSettings(
            equalizerEnabled = preferences[KEY_AUDIO_EQ_ENABLED] ?: false,
            presetId = presetId,
            bandLevels = levels,
            bassBoost = preferences[KEY_AUDIO_BASS_BOOST] ?: LevyraAudioPresets.preset(presetId).bassBoost,
            virtualizer = preferences[KEY_AUDIO_VIRTUALIZER] ?: LevyraAudioPresets.preset(presetId).virtualizer,
            crossfadeSeconds = preferences[KEY_AUDIO_CROSSFADE] ?: 0,
            djSoftMode = preferences[KEY_AUDIO_DJ_SOFT] ?: false,
            replayGainEnabled = preferences[KEY_AUDIO_REPLAY_GAIN] ?: (preferences[KEY_AUDIO_NORMALIZATION] ?: false),
            playbackSpeed = preferences[KEY_AUDIO_SPEED] ?: 1f,
            pitch = preferences[KEY_AUDIO_PITCH] ?: 1f,
            gaplessEnabled = preferences[KEY_AUDIO_GAPLESS] ?: true
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
        val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        val KEY_TASTES = stringSetPreferencesKey("tastes")
        val KEY_LAST_TRACK = stringPreferencesKey("last_track")
        val KEY_LAST_POSITION = longPreferencesKey("last_position")
        val KEY_ANIMATIONS = booleanPreferencesKey("animations_enabled")
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
        val KEY_AUDIO_CROSSFADE = intPreferencesKey("audio_crossfade_seconds")
        val KEY_AUDIO_DJ_SOFT = booleanPreferencesKey("audio_dj_soft")
        val KEY_AUDIO_REPLAY_GAIN = booleanPreferencesKey("audio_replay_gain")
        val KEY_AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val KEY_AUDIO_PITCH = floatPreferencesKey("audio_pitch")
        val KEY_AUDIO_GAPLESS = booleanPreferencesKey("audio_gapless")
        val KEY_LISTENING_PULSE_LAST_PRUNE = longPreferencesKey("listening_pulse_last_prune")
        val KEY_UI_COMPACT_HOME = booleanPreferencesKey("ui_compact_home")
        val KEY_UI_PERSONAL_ORBIT = booleanPreferencesKey("ui_show_personal_orbit")
        val KEY_UI_RESONANCE = booleanPreferencesKey("ui_show_resonance")
        val KEY_UI_NEW_RELEASES = booleanPreferencesKey("ui_show_new_releases")
        val KEY_UI_ALBUMS = booleanPreferencesKey("ui_show_albums")
        val KEY_UI_ARTISTS = booleanPreferencesKey("ui_show_artists")
        val KEY_UI_CHARTS = booleanPreferencesKey("ui_show_charts")
        val KEY_UI_PLAYER_GESTURES = booleanPreferencesKey("ui_player_gestures")
        val KEY_UI_DOUBLE_TAP_SECONDS = intPreferencesKey("ui_double_tap_seconds")
        val KEY_UI_LONG_PRESS_SPEED = floatPreferencesKey("ui_long_press_speed")
        val KEY_DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val KEY_DOWNLOAD_CHARGING_ONLY = booleanPreferencesKey("download_charging_only")
        val KEY_DOWNLOAD_RESUMABLE = booleanPreferencesKey("download_resumable")
        val KEY_DOWNLOAD_CONCURRENCY = intPreferencesKey("download_concurrency")
    }
}
