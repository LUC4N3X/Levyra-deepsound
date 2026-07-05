package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.FollowedArtist
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class FollowedArtistsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<FollowedArtist> {
        val raw = prefs.getString(KEY_ARTISTS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { json ->
                    val name = json.optString("name").trim()
                    if (name.isBlank()) return@let null
                    FollowedArtist(
                        browseId = json.optString("browseId"),
                        name = name,
                        thumbnailUrl = json.optString("thumbnailUrl"),
                        followedAt = json.optLong("followedAt", 0L)
                    )
                }
            }
        }.onFailure { Timber.w(it, "Followed artists load failed") }.getOrDefault(emptyList())
    }

    fun save(artists: List<FollowedArtist>) {
        val array = JSONArray()
        artists.forEach { artist ->
            array.put(
                JSONObject()
                    .put("browseId", artist.browseId)
                    .put("name", artist.name)
                    .put("thumbnailUrl", artist.thumbnailUrl)
                    .put("followedAt", artist.followedAt)
            )
        }
        prefs.edit().putString(KEY_ARTISTS, array.toString()).apply()
    }

    fun hasReleaseBaseline(artistKey: String): Boolean = prefs.contains(KEY_KNOWN_PREFIX + artistKey)

    fun knownReleases(artistKey: String): Set<String> =
        prefs.getStringSet(KEY_KNOWN_PREFIX + artistKey, emptySet()).orEmpty()

    fun saveKnownReleases(artistKey: String, keys: Set<String>) {
        prefs.edit().putStringSet(KEY_KNOWN_PREFIX + artistKey, keys.take(200).toSet()).apply()
    }

    fun clearKnownReleases(artistKey: String) {
        prefs.edit().remove(KEY_KNOWN_PREFIX + artistKey).apply()
    }

    fun radarOffset(): Int = prefs.getInt(KEY_RADAR_OFFSET, 0).coerceAtLeast(0)

    fun saveRadarOffset(offset: Int) {
        prefs.edit().putInt(KEY_RADAR_OFFSET, offset.coerceAtLeast(0)).apply()
    }

    private companion object {
        const val PREFS_NAME = "levyra_followed_artists"
        const val KEY_ARTISTS = "artists"
        const val KEY_KNOWN_PREFIX = "known_releases_"
        const val KEY_RADAR_OFFSET = "radar_offset"
    }
}
