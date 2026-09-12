package com.luc4n3x.levyra.feature.radio

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal class RadioRepository(
    context: Context,
    private val api: RadioBrowserApi = RadioBrowserApi(),
    private val store: RadioStore = RadioStore(context),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    @Volatile private var countriesCache: DirectoryCacheEntry? = null
    @Volatile private var languagesCache: DirectoryCacheEntry? = null

    suspend fun discover(
        preference: RadioLanguagePreference,
        filter: RadioFilter,
        localeBoost: Boolean
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val key = listOf(filter.category.name, filter.countryCode, filter.language, filter.offset, localeBoost).joinToString("|")
        memoryCache[key]?.takeIf { now() - it.savedAt < STATION_CACHE_MS }?.stations?.let { return@withContext it }
        val stations = if (localeBoost && filter.offset == 0 && filter.category == RadioCategory.Popular) {
            localeDiscovery(preference, filter.limit)
        } else {
            api.stations(filter)
        }
        val ranked = filterAndRankRadioStations(stations).take(filter.limit)
        memoryCache[key] = CacheEntry(ranked, now())
        if (localeBoost && filter.offset == 0 && ranked.isNotEmpty()) {
            store.saveCatalog(preference.levyraCode, ranked, now())
        }
        ranked
    }

    private suspend fun localeDiscovery(preference: RadioLanguagePreference, limit: Int): List<RadioStation> = coroutineScope {
        buildList {
            add(async { api.stations(RadioFilter(countryCode = preference.primaryCountry, limit = limit)) })
            add(async { api.stations(RadioFilter(language = preference.radioLanguages.first(), limit = 18)) })
            preference.preferredCountries.drop(1).take(2).forEach { country ->
                add(async { api.stations(RadioFilter(countryCode = country, limit = 10)) })
            }
        }.awaitAll().flatten()
    }

    suspend fun search(query: String): List<RadioStation> = withContext(Dispatchers.IO) {
        filterAndRankRadioStations(api.search(query)).take(64)
    }

    suspend fun countries(): List<RadioDirectoryEntry> = withContext(Dispatchers.IO) {
        countriesCache?.takeIf { now() - it.savedAt < DIRECTORY_CACHE_MS }?.entries ?: api.countries().also {
            countriesCache = DirectoryCacheEntry(it, now())
        }
    }

    suspend fun languages(): List<RadioDirectoryEntry> = withContext(Dispatchers.IO) {
        languagesCache?.takeIf { now() - it.savedAt < DIRECTORY_CACHE_MS }?.entries ?: api.languages().also {
            languagesCache = DirectoryCacheEntry(it, now())
        }
    }

    suspend fun favorites(): List<RadioStation> = store.favorites()

    suspend fun recent(): List<RadioStation> = store.recent()

    suspend fun setFavorite(station: RadioStation, favorite: Boolean): List<RadioStation> = store.setFavorite(station, favorite)

    suspend fun recordRecent(station: RadioStation): List<RadioStation> = store.recordRecent(station, now())

    suspend fun cachedCatalog(languageCode: String): CachedRadioCatalog? = store.cachedCatalog(languageCode)

    suspend fun recordClick(stationUuid: String) = api.recordClick(stationUuid)

    private data class CacheEntry(val stations: List<RadioStation>, val savedAt: Long)
    private data class DirectoryCacheEntry(val entries: List<RadioDirectoryEntry>, val savedAt: Long)

    private companion object {
        const val STATION_CACHE_MS = 30 * 60 * 1_000L
        const val DIRECTORY_CACHE_MS = 24 * 60 * 60 * 1_000L
    }
}
