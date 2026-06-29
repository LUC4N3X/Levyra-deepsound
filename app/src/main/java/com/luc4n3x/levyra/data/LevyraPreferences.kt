package com.luc4n3x.levyra.data

import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
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

class LevyraPreferences(context: Context) {
    private val dataStore = context.applicationContext.levyraDataStore

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

    fun lastTrack(): Track? {
        val raw = read("") { it[KEY_LAST_TRACK].orEmpty() }
        if (raw.isBlank()) return null
        return runCatching { TrackJson.fromJson(JSONObject(raw)) }
            .onFailure { Timber.w(it, "Last track restore failed") }
            .getOrNull()
    }

    fun lastPositionMs(): Long = read(0L) { it[KEY_LAST_POSITION] ?: 0L }

    fun loadRecentSearches(): List<Track> {
        val raw = read("") { it[KEY_RECENT_SEARCHES].orEmpty() }
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(TrackJson::fromJson) }
        }.onFailure { Timber.w(it, "Recent searches restore failed") }.getOrDefault(emptyList())
    }

    fun saveRecentSearches(tracks: List<Track>) {
        val array = JSONArray()
        tracks.forEach { array.put(TrackJson.toJson(it)) }
        write { it[KEY_RECENT_SEARCHES] = array.toString() }
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
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }
}
