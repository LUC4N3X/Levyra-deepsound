package com.luc4n3x.levyra.feature.radio

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class RadioStore(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE))

    fun observeFavorites() = observe(KEY_FAVORITES)

    fun observeCustomStations() = observe(KEY_CUSTOM)

    fun observeRecent() = observe(KEY_RECENT)

    private fun observe(key: String) = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key || changed == null) trySend(Unit)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().map { decode(preferences.getString(key, null)) }.distinctUntilChanged().flowOn(Dispatchers.IO)

    suspend fun customStations(): List<RadioStation> = withContext(Dispatchers.IO) {
        decode(preferences.getString(KEY_CUSTOM, null)).filter(RadioStation::isCustom)
    }

    suspend fun saveCustom(candidate: RadioStation) = withContext(Dispatchers.IO) {
        require(candidate.isCustom)
        val station = customRadioStation(candidate.name, candidate.streamUrl, candidate.faviconUrl, candidate)
        mutex.withLock {
            val current = decode(preferences.getString(KEY_CUSTOM, null))
            if (current.any { it.uuid != station.uuid && canonicalRadioUrl(it.streamUrl) == canonicalRadioUrl(station.streamUrl) }) {
                throw CustomRadioException(CustomRadioError.Duplicate)
            }
            if (current.size >= MAX_CUSTOM && current.none { it.uuid == station.uuid }) throw CustomRadioException(CustomRadioError.Limit)
            val updated = current.map { if (it.uuid == station.uuid) station else it }
                .let { if (current.any { saved -> saved.uuid == station.uuid }) it else it + station }
            val editor = preferences.edit().putString(KEY_CUSTOM, encode(updated))
            listOf(KEY_FAVORITES, KEY_RECENT, KEY_PLAYBACK_STATION).forEach { key ->
                val records = decode(preferences.getString(key, null))
                if (records.any { it.uuid == station.uuid }) {
                    editor.putString(key, encode(records.map { if (it.uuid == station.uuid) station.copy(lastPlayedAt = it.lastPlayedAt) else it }))
                }
            }
            if (!editor.commit()) throw CustomRadioException(CustomRadioError.Storage)
        }
    }

    suspend fun deleteCustom(station: RadioStation) = withContext(Dispatchers.IO) {
        require(station.isCustom)
        mutex.withLock {
            val editor = preferences.edit()
            listOf(KEY_CUSTOM, KEY_FAVORITES, KEY_RECENT, KEY_PLAYBACK_STATION).forEach { key ->
                editor.putString(key, encode(decode(preferences.getString(key, null)).filterNot { it.uuid == station.uuid }))
            }
            if (!editor.commit()) throw CustomRadioException(CustomRadioError.Storage)
        }
    }

    suspend fun favorites(): List<RadioStation> = withContext(Dispatchers.IO) {
        decode(preferences.getString(KEY_FAVORITES, null)).take(MAX_FAVORITES)
    }

    suspend fun recent(): List<RadioStation> = withContext(Dispatchers.IO) {
        decode(preferences.getString(KEY_RECENT, null)).sortedByDescending(RadioStation::lastPlayedAt).take(MAX_RECENT)
    }

    suspend fun playbackStation(): RadioStation? = withContext(Dispatchers.IO) {
        decode(preferences.getString(KEY_PLAYBACK_STATION, null)).firstOrNull()
    }

    suspend fun savePlaybackStation(station: RadioStation?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val saved = station?.let(::currentStation)
            preferences.edit().apply {
                if (saved == null) remove(KEY_PLAYBACK_STATION)
                else putString(KEY_PLAYBACK_STATION, encode(listOf(saved)))
            }.commit()
        }
    }

    suspend fun setFavorite(station: RadioStation, favorite: Boolean): List<RadioStation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = decode(preferences.getString(KEY_FAVORITES, null)).toMutableList()
            val saved = currentStation(station)
            current.removeAll { it.uuid.equals(station.uuid, true) }
            if (favorite && saved != null) current.add(0, saved.copy(lastPlayedAt = 0L))
            val result = current.take(MAX_FAVORITES)
            if (!preferences.edit().putString(KEY_FAVORITES, encode(result)).commit()) throw CustomRadioException(CustomRadioError.Storage)
            result
        }
    }

    suspend fun recordRecent(station: RadioStation, playedAt: Long): List<RadioStation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = decode(preferences.getString(KEY_RECENT, null)).toMutableList()
            val saved = currentStation(station) ?: return@withLock current
            current.removeAll { it.uuid.equals(station.uuid, true) }
            current.add(0, saved.copy(lastPlayedAt = playedAt))
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

    private fun currentStation(station: RadioStation): RadioStation? = if (station.isCustom) {
        decode(preferences.getString(KEY_CUSTOM, null)).firstOrNull { it.uuid == station.uuid }
    } else station

    private companion object {
        val mutex = Mutex()
        const val PREFERENCES = "levyra_live_radio"
        const val KEY_FAVORITES = "favorites"
        const val KEY_CUSTOM = "custom_stations"
        const val MAX_CUSTOM = 200
        const val KEY_RECENT = "recent"
        const val KEY_PLAYBACK_STATION = "playback_station"
        const val KEY_CATALOG_PREFIX = "catalog_"
        const val KEY_CATALOG_TIME_PREFIX = "catalog_time_"
        const val MAX_FAVORITES = 200
        const val MAX_RECENT = 30
        const val MAX_CACHED_STATIONS = 64
        const val MAX_DECODED_STATIONS = 256
    }
}

internal data class CachedRadioCatalog(val stations: List<RadioStation>, val savedAt: Long)
