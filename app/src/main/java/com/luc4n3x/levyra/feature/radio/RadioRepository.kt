package com.luc4n3x.levyra.feature.radio

import android.content.Context
import java.util.LinkedHashMap
import kotlinx.coroutines.CancellationException
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
    private val cacheLock = Any()
    private val memoryCache = object : LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }
    @Volatile private var countriesCache: DirectoryCacheEntry? = null
    @Volatile private var languagesCache: DirectoryCacheEntry? = null

    suspend fun discover(
        preference: RadioLanguagePreference,
        filter: RadioFilter,
        localeBoost: Boolean
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val key = listOf(filter.category.name, filter.countryCode, filter.language, filter.offset, localeBoost).joinToString("|")
        getCachedStations(key)?.let { return@withContext it }
        val stations = if (localeBoost && filter.offset == 0 && filter.category == RadioCategory.Popular) {
            localeDiscovery(preference, filter.limit)
        } else {
            api.stations(filter)
        }
        val ranked = filterAndRankRadioStations(stations).take(filter.limit)
        putCachedStations(key, ranked)
        if (localeBoost && filter.offset == 0 && ranked.isNotEmpty()) {
            store.saveCatalog(preference.levyraCode, ranked, now())
        }
        ranked
    }

    private fun getCachedStations(key: String): List<RadioStation>? = synchronized(cacheLock) {
        val entry = memoryCache[key] ?: return null
        if (now() - entry.savedAt < STATION_CACHE_MS) {
            entry.stations
        } else {
            memoryCache.remove(key)
            null
        }
    }

    private fun putCachedStations(key: String, stations: List<RadioStation>) = synchronized(cacheLock) {
        val currentTime = now()
        val iterator = memoryCache.entries.iterator()
        while (iterator.hasNext()) {
            if (currentTime - iterator.next().value.savedAt >= STATION_CACHE_MS) {
                iterator.remove()
            }
        }
        memoryCache[key] = CacheEntry(stations, currentTime)
    }

    private suspend fun localeDiscovery(preference: RadioLanguagePreference, limit: Int): List<RadioStation> = coroutineScope {
        val deferreds = buildList {
            add(
                async {
                    try {
                        Result.success(api.stations(RadioFilter(countryCode = preference.primaryCountry, limit = limit)))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                }
            )
            add(
                async {
                    try {
                        Result.success(api.stations(RadioFilter(language = preference.radioLanguages.first(), limit = 18)))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                }
            )
            preference.preferredCountries.drop(1).take(2).forEach { country ->
                add(
                    async {
                        try {
                            Result.success(api.stations(RadioFilter(countryCode = country, limit = 10)))
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }
                    }
                )
            }
        }
        val results = deferreds.awaitAll()
        val (successes, failures) = results.partition { it.isSuccess }
        if (successes.isEmpty() && failures.isNotEmpty()) {
            throw failures.first().exceptionOrNull() ?: IllegalStateException("Radio discovery failed")
        }
        successes.flatMap { it.getOrDefault(emptyList()) }
    }

    suspend fun search(query: String, cursor: List<RadioSearchRequest>? = null): RadioSearchPage = withContext(Dispatchers.IO) {
        val local = if (cursor == null) (store.customStations() + store.favorites()).filter { radioStationMatchesSearch(it, query) } else emptyList()
        val page = try {
            api.search(query, cursor)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (local.isEmpty()) throw error
            RadioSearchPage(emptyList(), radioSearchRequests(query), partialFailure = true)
        }
        page.copy(stations = filterAndRankRadioSearchResults(local + page.stations, query, requireLocalMatch = false))
    }

    suspend fun siblingStreams(station: RadioStation): List<String> = withContext(Dispatchers.IO) {
        if (station.isCustom) return@withContext emptyList()
        val fresh = async { optionalRadioLookup { api.stationByUuid(station.uuid) } }
        val named = async { optionalRadioLookup { api.stationsNamed(station.name) }.orEmpty() }
        liveRadioRecoveryStreamUrls(station, fresh.await(), named.await())
    }

    private suspend fun <T> optionalRadioLookup(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
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

    fun observeFavorites() = store.observeFavorites()

    fun observeCustomStations() = store.observeCustomStations()

    fun observeRecent() = store.observeRecent()

    suspend fun customStations(): List<RadioStation> = store.customStations()

    suspend fun saveCustom(station: RadioStation) = store.saveCustom(station)

    suspend fun deleteCustom(station: RadioStation) = store.deleteCustom(station)

    suspend fun recent(): List<RadioStation> = store.recent()

    suspend fun playbackStation(): RadioStation? = store.playbackStation()

    suspend fun savePlaybackStation(station: RadioStation?) = store.savePlaybackStation(station)

    suspend fun setFavorite(station: RadioStation, favorite: Boolean): List<RadioStation> = store.setFavorite(station, favorite)

    suspend fun recordRecent(station: RadioStation): List<RadioStation> = store.recordRecent(station, now())

    suspend fun cachedCatalog(languageCode: String): CachedRadioCatalog? = store.cachedCatalog(languageCode)

    suspend fun recordClick(stationUuid: String) = api.recordClick(stationUuid)

    private data class CacheEntry(val stations: List<RadioStation>, val savedAt: Long)
    private data class DirectoryCacheEntry(val entries: List<RadioDirectoryEntry>, val savedAt: Long)

    private companion object {
        const val MAX_CACHE_ENTRIES = 48
        const val STATION_CACHE_MS = 30 * 60 * 1_000L
        const val DIRECTORY_CACHE_MS = 24 * 60 * 60 * 1_000L
    }
}
