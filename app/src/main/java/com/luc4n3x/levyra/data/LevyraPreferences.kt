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
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
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
    val themePreset: String,
    val audioSettings: LevyraAudioSettings
)

class LevyraPreferences(context: Context) {
    private val dataStore = context.applicationContext.levyraDataStore

    fun snapshot(): LevyraPreferencesSnapshot = read(defaultSnapshot()) { snapshotFrom(it) }

    fun isOnboarded(): Boolean = read(false) { it[KEY_ONBOARDED] ?: false }

    fun setOnboarded(tastes: Set<String>) {
        write {
            it[KEY_ONBOARDED] = true
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

    fun loadRecentSearches(): List<Track> = snapshot().recentSearches

    fun saveRecentSearches(tracks: List<Track>) {
        val array = JSONArray()
        tracks.forEach { array.put(TrackJson.toJson(it)) }
        write { it[KEY_RECENT_SEARCHES] = array.toString() }
    }

    fun loadHomeSections(): List<HomeSection> = read(emptyList()) { parseHomeSections(it[KEY_HOME_SECTIONS].orEmpty()) }

    fun saveHomeSections(sections: List<HomeSection>) {
        val array = JSONArray()
        sections.take(10).forEach { section ->
            val tracks = JSONArray()
            section.tracks.take(20).forEach { track -> tracks.put(TrackJson.toJson(track)) }
            if (tracks.length() > 0) {
                array.put(JSONObject().put("title", section.title).put("tracks", tracks))
            }
        }
        write { it[KEY_HOME_SECTIONS] = array.toString() }
    }

    fun loadChartTracks(): List<Track> = read(emptyList()) { parseTrackList(it[KEY_CHART_TRACKS].orEmpty()) }

    fun saveChartTracks(tracks: List<Track>) {
        val array = JSONArray()
        tracks.take(50).forEach { track -> array.put(TrackJson.toJson(track)) }
        write { it[KEY_CHART_TRACKS] = array.toString() }
    }

    fun loadPersonalOrbitTracks(): List<Track> = read(emptyList()) { parseTrackList(it[KEY_PERSONAL_ORBIT_TRACKS].orEmpty()) }

    fun savePersonalOrbitTracks(tracks: List<Track>) {
        val array = JSONArray()
        tracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT).forEach { track -> array.put(TrackJson.toJson(track)) }
        write { it[KEY_PERSONAL_ORBIT_TRACKS] = array.toString() }
    }

    private fun snapshotFrom(preferences: Preferences): LevyraPreferencesSnapshot {
        return LevyraPreferencesSnapshot(
            onboarded = preferences[KEY_ONBOARDED] ?: false,
            tastes = preferences[KEY_TASTES].orEmpty(),
            userName = preferences[KEY_USER_NAME].orEmpty(),
            languageCode = LevyraLanguageCatalog.normalize(preferences[KEY_LANGUAGE_CODE].orEmpty().ifBlank { LevyraLanguageCatalog.deviceDefault() }),
            animationsEnabled = preferences[KEY_ANIMATIONS] ?: true,
            dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            sponsorBlock = preferences[KEY_SPONSORBLOCK] ?: true,
            skipSilence = preferences[KEY_SKIP_SILENCE] ?: false,
            audioQuality = normalizeAudioQuality(preferences[KEY_AUDIO_QUALITY].orEmpty()),
            dismissedUpdateVersion = preferences[KEY_DISMISSED_UPDATE_VERSION].orEmpty(),
            lastTrack = parseTrack(preferences[KEY_LAST_TRACK].orEmpty(), "Last track restore failed"),
            lastPositionMs = preferences[KEY_LAST_POSITION] ?: 0L,
            recentSearches = parseTrackList(preferences[KEY_RECENT_SEARCHES].orEmpty()),
            personalOrbitTracks = parseTrackList(preferences[KEY_PERSONAL_ORBIT_TRACKS].orEmpty()),
            audioNormalization = preferences[KEY_AUDIO_NORMALIZATION] ?: false,
            themePreset = com.luc4n3x.levyra.ui.theme.LevyraThemes.normalize(preferences[KEY_THEME_PRESET].orEmpty()),
            audioSettings = audioSettingsFrom(preferences)
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
        themePreset = com.luc4n3x.levyra.ui.theme.LevyraThemes.COSMIC,
        audioSettings = LevyraAudioSettings()
    )

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
    }
}
