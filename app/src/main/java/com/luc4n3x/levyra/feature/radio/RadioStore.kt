package com.luc4n3x.levyra.feature.radio

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class RadioStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    suspend fun favorites(): List<RadioStation> = withContext(Dispatchers.IO) {
        decode(preferences.getString(KEY_FAVORITES, null)).take(MAX_FAVORITES)
    }

    suspend fun recent(): List<RadioStation> = withContext(Dispatchers.IO) {
        decode(preferences.getString(KEY_RECENT, null)).sortedByDescending(RadioStation::lastPlayedAt).take(MAX_RECENT)
    }

    suspend fun setFavorite(station: RadioStation, favorite: Boolean): List<RadioStation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = decode(preferences.getString(KEY_FAVORITES, null)).toMutableList()
            current.removeAll { it.uuid.equals(station.uuid, true) }
            if (favorite) current.add(0, station.copy(lastPlayedAt = 0L))
            val result = current.take(MAX_FAVORITES)
            preferences.edit().putString(KEY_FAVORITES, encode(result)).commit()
            result
        }
    }

    suspend fun recordRecent(station: RadioStation, playedAt: Long): List<RadioStation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = decode(preferences.getString(KEY_RECENT, null)).toMutableList()
            current.removeAll { it.uuid.equals(station.uuid, true) }
            current.add(0, station.copy(lastPlayedAt = playedAt))
            val result = current.take(MAX_RECENT)
            preferences.edit().putString(KEY_RECENT, encode(result)).commit()
            result
        }
    }

    suspend fun cachedCatalog(languageCode: String): CachedRadioCatalog? = withContext(Dispatchers.IO) {
        val key = languageCatalogKey(languageCode)
        val stations = decode(preferences.getString("$KEY_CATALOG_PREFIX$key", null))
        if (stations.isEmpty()) return@withContext null
        CachedRadioCatalog(
            stations = stations.take(MAX_CACHED_STATIONS),
            savedAt = preferences.getLong("$KEY_CATALOG_TIME_PREFIX$key", 0L)
        )
    }

    suspend fun saveCatalog(languageCode: String, stations: List<RadioStation>, savedAt: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = languageCatalogKey(languageCode)
            preferences.edit()
                .putString("$KEY_CATALOG_PREFIX$key", encode(stations.take(MAX_CACHED_STATIONS)))
                .putLong("$KEY_CATALOG_TIME_PREFIX$key", savedAt)
                .commit()
        }
    }

    private fun encode(stations: List<RadioStation>): String = JSONArray().apply {
        stations.forEach { put(it.toJson()) }
    }.toString()

    private fun decode(raw: String?): List<RadioStation> = runCatching {
        val array = JSONArray(raw.orEmpty())
        buildList(array.length().coerceAtMost(MAX_DECODED_STATIONS)) {
            for (index in 0 until minOf(array.length(), MAX_DECODED_STATIONS)) {
                array.optJSONObject(index)?.toStation()?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun RadioStation.toJson() = JSONObject()
        .put("uuid", uuid)
        .put("name", name)
        .put("url", streamUrl)
        .put("resolved", resolvedStreamUrl)
        .put("favicon", faviconUrl)
        .put("homepage", homepageUrl)
        .put("country", country)
        .put("countryCode", countryCode)
        .put("language", language)
        .put("tags", JSONArray(tags.take(24)))
        .put("codec", codec)
        .put("bitrate", bitrateKbps)
        .put("votes", votes)
        .put("clicks", clickCount)
        .put("ok", lastCheckOk)
        .put("playedAt", lastPlayedAt)

    private fun JSONObject.toStation(): RadioStation? {
        val uuid = optString("uuid").trim()
        val name = optString("name").trim()
        if (uuid.isBlank() || name.isBlank()) return null
        val tagArray = optJSONArray("tags") ?: JSONArray()
        return RadioStation(
            uuid = uuid,
            name = name,
            streamUrl = optString("url").trim(),
            resolvedStreamUrl = optString("resolved").trim(),
            faviconUrl = optString("favicon").trim(),
            homepageUrl = optString("homepage").trim(),
            country = optString("country").trim(),
            countryCode = optString("countryCode").trim().uppercase(Locale.ROOT),
            language = optString("language").trim(),
            tags = buildList(tagArray.length().coerceAtMost(24)) {
                for (index in 0 until minOf(tagArray.length(), 24)) tagArray.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            },
            codec = optString("codec").trim(),
            bitrateKbps = optInt("bitrate", 0).coerceAtLeast(0),
            votes = optInt("votes", 0).coerceAtLeast(0),
            clickCount = optInt("clicks", 0).coerceAtLeast(0),
            lastCheckOk = optBoolean("ok", true),
            lastPlayedAt = optLong("playedAt", 0L).coerceAtLeast(0L)
        ).takeIf { it.preferredStreamUrl.isNotBlank() }
    }

    private fun languageCatalogKey(value: String): String = value.trim().lowercase(Locale.ROOT).take(12)

    private companion object {
        const val PREFERENCES = "levyra_live_radio"
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENT = "recent"
        const val KEY_CATALOG_PREFIX = "catalog_"
        const val KEY_CATALOG_TIME_PREFIX = "catalog_time_"
        const val MAX_FAVORITES = 200
        const val MAX_RECENT = 30
        const val MAX_CACHED_STATIONS = 64
        const val MAX_DECODED_STATIONS = 256
    }
}

internal data class CachedRadioCatalog(val stations: List<RadioStation>, val savedAt: Long)
