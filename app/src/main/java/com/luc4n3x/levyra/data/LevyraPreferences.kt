package com.luc4n3x.levyra.data

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    val animationsEnabled: Boolean,
    val dynamicColor: Boolean,
    val sponsorBlock: Boolean,
    val skipSilence: Boolean,
    val audioQuality: String,
    val dismissedUpdateVersion: String,
    val lastTrack: Track?,
    val lastPositionMs: Long,
    val recentSearches: List<Track>
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

    fun animationsEnabled(): Boolean = read(true) { it[KEY_ANIMATIONS] ?: true }

    fun setAnimationsEnabled(value: Boolean) {
        write { it[KEY_ANIMATIONS] = value }
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

    private fun snapshotFrom(preferences: Preferences): LevyraPreferencesSnapshot {
        return LevyraPreferencesSnapshot(
            onboarded = preferences[KEY_ONBOARDED] ?: false,
            tastes = preferences[KEY_TASTES].orEmpty(),
            userName = preferences[KEY_USER_NAME].orEmpty(),
            animationsEnabled = preferences[KEY_ANIMATIONS] ?: true,
            dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            sponsorBlock = preferences[KEY_SPONSORBLOCK] ?: true,
            skipSilence = preferences[KEY_SKIP_SILENCE] ?: false,
            audioQuality = normalizeAudioQuality(preferences[KEY_AUDIO_QUALITY].orEmpty()),
            dismissedUpdateVersion = preferences[KEY_DISMISSED_UPDATE_VERSION].orEmpty(),
            lastTrack = parseTrack(preferences[KEY_LAST_TRACK].orEmpty(), "Last track restore failed"),
            lastPositionMs = preferences[KEY_LAST_POSITION] ?: 0L,
            recentSearches = parseTrackList(preferences[KEY_RECENT_SEARCHES].orEmpty())
        )
    }

    private fun defaultSnapshot(): LevyraPreferencesSnapshot = LevyraPreferencesSnapshot(
        onboarded = false,
        tastes = emptySet(),
        userName = "",
        animationsEnabled = true,
        dynamicColor = true,
        sponsorBlock = true,
        skipSilence = false,
        audioQuality = "Auto",
        dismissedUpdateVersion = "",
        lastTrack = null,
        lastPositionMs = 0L,
        recentSearches = emptyList()
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

    private fun normalizeAudioQuality(value: String): String = when (value.lowercase()) {
        "high" -> "High"
        "low" -> "Low"
        else -> "Auto"
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
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val KEY_DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
    }
}
