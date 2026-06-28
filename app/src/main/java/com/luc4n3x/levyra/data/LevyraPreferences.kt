package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject

/** Onboarding state, taste profile and last playback position persisted on device. */
class LevyraPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("levyra_prefs", Context.MODE_PRIVATE)

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(tastes: Set<String>) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDED, true)
            .putStringSet(KEY_TASTES, tastes)
            .apply()
    }

    fun tastes(): Set<String> = prefs.getStringSet(KEY_TASTES, emptySet()).orEmpty()

    fun userName(): String = prefs.getString(KEY_USER_NAME, "").orEmpty()

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun animationsEnabled(): Boolean = prefs.getBoolean(KEY_ANIMATIONS, true)

    fun setAnimationsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATIONS, value).apply()
    }

    fun dynamicColor(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColor(value: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
    }

    fun sponsorBlock(): Boolean = prefs.getBoolean(KEY_SPONSORBLOCK, true)

    fun setSponsorBlock(value: Boolean) {
        prefs.edit().putBoolean(KEY_SPONSORBLOCK, value).apply()
    }

    fun skipSilence(): Boolean = prefs.getBoolean(KEY_SKIP_SILENCE, false)

    fun setSkipSilence(value: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_SILENCE, value).apply()
    }

    fun saveLastPlayback(track: Track?, positionMs: Long) {
        if (track == null) {
            prefs.edit().remove(KEY_LAST_TRACK).remove(KEY_LAST_POSITION).apply()
            return
        }
        prefs.edit()
            .putString(KEY_LAST_TRACK, TrackJson.toJson(track).toString())
            .putLong(KEY_LAST_POSITION, positionMs.coerceAtLeast(0L))
            .apply()
    }

    fun lastTrack(): Track? {
        val raw = prefs.getString(KEY_LAST_TRACK, null).orEmpty()
        if (raw.isBlank()) return null
        return runCatching { TrackJson.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun lastPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION, 0L)

    fun loadRecentSearches(): List<Track> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(TrackJson::fromJson)
            }
        }.getOrDefault(emptyList())
    }

    fun saveRecentSearches(tracks: List<Track>) {
        val array = JSONArray()
        tracks.forEach { array.put(TrackJson.toJson(it)) }
        prefs.edit().putString(KEY_RECENT_SEARCHES, array.toString()).apply()
    }

    private companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_TASTES = "tastes"
        const val KEY_LAST_TRACK = "last_track"
        const val KEY_LAST_POSITION = "last_position"
        const val KEY_ANIMATIONS = "animations_enabled"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_SPONSORBLOCK = "sponsorblock_enabled"
        private const val KEY_SKIP_SILENCE = "skip_silence"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
    }
}
