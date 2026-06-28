package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray

/** Lightweight persistence of liked songs backed by SharedPreferences (no extra dependency). */
class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("levyra_favorites", Context.MODE_PRIVATE)

    fun load(): List<Track> {
        val raw = prefs.getString(KEY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(TrackJson::fromJson)
            }
        }.getOrDefault(emptyList())
    }

    fun save(tracks: List<Track>) {
        val array = JSONArray()
        tracks.forEach { array.put(TrackJson.toJson(it)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "liked_tracks"
    }
}
