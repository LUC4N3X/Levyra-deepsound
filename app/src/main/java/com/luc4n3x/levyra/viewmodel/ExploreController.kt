package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.EXPLORE_DISCOVERY_CACHE_TTL_MS
import com.luc4n3x.levyra.data.EditorialChartsRepository
import com.luc4n3x.levyra.data.ExploreDiscoverySnapshot
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LevyraPreferencesSnapshot
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.apple.AppleCatalogPage
import com.luc4n3x.levyra.data.apple.AppleExploreCatalog
import com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository
import com.luc4n3x.levyra.data.apple.AppleStorefronts
import com.luc4n3x.levyra.data.lastfm.LastFmGenreChartRepository
import com.luc4n3x.levyra.data.lastfm.LastFmGenreDiscoveryRepository
import com.luc4n3x.levyra.data.lastfm.LastFmGenreTags
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

private const val EXPLORE_VIDEOS_LIMIT = 40
private const val EXPLORE_DISCOVERY_DEEP_WARMUP_LIMIT = 6
private const val EXPLORE_EDITORIAL_MIN_TRACKS = 10
private const val EXPLORE_GENRE_CHART_FIRST_PAINT = 10

internal class ExploreController(
    private val scope: CoroutineScope,
    private val application: Application,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val repository: YoutubeMusicRepository,
    private val editorialChartsRepository: EditorialChartsRepository,
    private val preferences: LevyraPreferences,
    startupExploreDiscovery: ExploreDiscoverySnapshot?,
    startupSettings: LevyraPreferencesSnapshot,
    private val refreshOfficialMetadataBatch: (List<Track>, Int) -> Unit,
    private val ensureFreshCurrentsLoaded: () -> Unit
) {
    val exploreCache = ConcurrentHashMap<String, List<Track>>()
    private var exploreVideosJob: Job? = null
    private var exploreVideosLanguage = ""
    private var exploreJob: Job? = null
    private val lastFmGenreCharts by lazy { LastFmGenreChartRepository(application) }
    private val _exploreGenreChart = MutableStateFlow(ExploreGenreChartState())
    val exploreGenreChart: StateFlow<ExploreGenreChartState> = _exploreGenreChart.asStateFlow()
    private var exploreGenreChartJob: Job? = null
    private val lastFmGenreDiscovery by lazy { LastFmGenreDiscoveryRepository(application) }
    private val _exploreGenreDiscovery = MutableStateFlow(ExploreGenreDiscoveryState())
    val exploreGenreDiscovery: StateFlow<ExploreGenreDiscoveryState> = _exploreGenreDiscovery.asStateFlow()
    private var exploreGenreDiscoveryJob: Job? = null
    private var exploreCategoriesLoadedLanguage =
        startupExploreDiscovery?.let { LevyraLanguageCatalog.normalize(startupSettings.languageCode) }.orEmpty()
    private var exploreCategoriesRequestLanguage = ""
    private var exploreCategoriesRequestGeneration = 0L
    private var exploreCategoriesCachedAtMs = startupExploreDiscovery?.savedAtMs ?: 0L
    private var exploreCategoriesJob: Job? = null
    private var exploreDiscoveryPersistJob: Job? = null
    private var exploreArtworkWarmupJob: Job? = null
    private var exploreArtworkWarmupLanguage = ""
    private val exploreArtworkWarmupAttempts = mutableSetOf<String>()
    private val exploreCategoryArtworkRequests = mutableSetOf<String>()
    private val exploreCategoryArtworkSemaphore = Semaphore(2)

    fun resetForLanguage(languageCode: String): ExploreDiscoverySnapshot? {
        exploreCache.clear()
        exploreJob?.cancel()
        exploreJob = null
        exploreCategoriesJob?.cancel()
        exploreCategoriesJob = null
        exploreDiscoveryPersistJob?.cancel()
        exploreDiscoveryPersistJob = null
        exploreArtworkWarmupJob?.cancel()
        exploreArtworkWarmupJob = null
        exploreArtworkWarmupLanguage = ""
        exploreArtworkWarmupAttempts.clear()
        val exploreSnapshot = preferences.loadExploreDiscovery(languageCode)
        exploreCategoriesLoadedLanguage = if (exploreSnapshot != null) languageCode else ""
        exploreCategoriesCachedAtMs = exploreSnapshot?.savedAtMs ?: 0L
        exploreCategoryArtworkRequests.clear()
        return exploreSnapshot
    }

    fun ensureExploreVideosLoaded() {
        val language = uiState.value.languageCode
        if (exploreVideosJob?.isActive == true && exploreVideosLanguage == language) return
        if (exploreVideosLanguage == language && uiState.value.exploreVideos.isNotEmpty()) return
        exploreVideosJob?.cancel()
        if (exploreVideosLanguage != language) uiState.update { it.copy(exploreVideos = emptyList()) }
        exploreVideosLanguage = language
        exploreVideosJob = scope.launch {
            val videos = try {
                repository.newMusicVideos(language, EXPLORE_VIDEOS_LIMIT)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Explore videos failed")
                emptyList()
            }
            if (videos.isEmpty()) exploreVideosLanguage = ""
            uiState.update { current -> if (current.languageCode != language || videos.isEmpty()) current else current.copy(exploreVideos = videos) }
        }
    }

    fun selectExploreZone(zone: ExploreZone) {
        exploreJob?.cancel()
        loadExploreGenreChart("zone:${zone.id}", LastFmGenreTags.forZone(zone.id))
        if (zone.id == ExploreCatalog.NEW_RELEASES_ZONE_ID) {
            uiState.update { current ->
                current.copy(
                    exploreZoneId = zone.id,
                    exploreCategoryParams = null,
                    exploreTracks = current.exploreFreshTracks,
                    isExploreLoading = current.isFreshCurrentsLoading
                )
            }
            ensureFreshCurrentsLoaded()
            return
        }
        val languageCode = uiState.value.languageCode
        val cacheKey = "zone:$languageCode:${zone.id}"
        val cached = exploreCache[cacheKey]
        uiState.update { current ->
            current.copy(
                exploreZoneId = zone.id,
                exploreCategoryParams = null,
                exploreTracks = cached.orEmpty(),
                isExploreLoading = cached == null
            )
        }
        cached?.let { cachedTracks ->
            refreshOfficialMetadataBatch(cachedTracks, 8)
            return
        }
        exploreJob = scope.launch {
            val results = try {
                repository.exploreZone(zone.id, zone.query, languageCode, 24)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Editorial Explore zone failed for %s", zone.id)
                emptyList()
            }
            if (results.isNotEmpty()) exploreCache[cacheKey] = results
            if (
                uiState.value.exploreZoneId != zone.id ||
                uiState.value.exploreCategoryParams != null ||
                uiState.value.languageCode != languageCode
            ) return@launch
            uiState.update { it.copy(exploreTracks = results, isExploreLoading = false) }
            refreshOfficialMetadataBatch(results, 8)
        }
    }

    private fun mergeExploreEditorialFallback(
        editorial: List<Track>,
        fallback: List<Track>,
        limit: Int
    ): List<Track> {
        val output = LinkedHashMap<String, Track>()
        (editorial + fallback).forEach { track ->
            val key = LevyraPersonalOrbit.identityKey(track).ifBlank { "id:${track.id}" }
            output.putIfAbsent(key, track)
        }
        return output.values.take(limit.coerceAtLeast(1))
    }

    fun selectExploreCategory(category: ExploreCategory) {
        val params = category.params
        if (params.isBlank() || category.title.isBlank()) return
        exploreJob?.cancel()
        loadExploreGenreChart("category:$params", LastFmGenreTags.forCategory(category))
        val languageCode = uiState.value.languageCode
        val cacheKey = "provider:$languageCode:$params"
        val cached = exploreCache[cacheKey]
        uiState.update { current ->
            current.copy(
                exploreZoneId = null,
                exploreCategoryParams = params,
                exploreTracks = cached.orEmpty(),
                isExploreLoading = cached == null
            )
        }
        cached?.let { cachedTracks ->
            refreshOfficialMetadataBatch(cachedTracks, 8)
            return
        }
        exploreJob = scope.launch {
            val editorial = try {
                editorialChartsRepository.exploreCollection(params, 24)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Spotify editorial Explore category failed")
                null
            }
            val editorialTracks = editorial?.tracks.orEmpty()
            val results = if (editorialTracks.size >= EXPLORE_EDITORIAL_MIN_TRACKS) {
                editorialTracks
            } else {
                val providerTracks = try {
                    repository.exploreCategory(params, languageCode, 24)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Timber.w(error, "Provider Explore category failed")
                    emptyList()
                }
                mergeExploreEditorialFallback(
                    editorial = editorialTracks,
                    fallback = providerTracks,
                    limit = 24
                )
            }
            if (results.isNotEmpty()) exploreCache[cacheKey] = results
            val editorialArtwork = editorial?.artworkUrl.orEmpty()
            if (editorialArtwork.isNotBlank() && uiState.value.languageCode == languageCode) {
                uiState.update { current ->
                    current.copy(
                        exploreCategoryArtwork = current.exploreCategoryArtwork +
                            (params to editorialArtwork)
                    )
                }
                schedulePersistExploreDiscovery(languageCode)
            }
            if (
                uiState.value.exploreCategoryParams != params ||
                uiState.value.exploreZoneId != null ||
                uiState.value.languageCode != languageCode
            ) return@launch
            uiState.update { current -> current.copy(exploreTracks = results, isExploreLoading = false) }
            refreshOfficialMetadataBatch(results, 8)
        }
    }

    fun openExploreCategoryChart(category: ExploreCategory) {
        loadExploreGenreChart("category:${category.params}", LastFmGenreTags.forCategory(category))
    }

    fun openExploreZoneChart(zone: ExploreZone) {
        loadExploreGenreChart("zone:${zone.id}", LastFmGenreTags.forZone(zone.id))
    }

    private suspend fun fetchExploreGenreChart(
        identity: String,
        tags: List<String>,
        languageCode: String,
        onProgress: (List<Track>) -> Unit
    ): List<Track> = runCatchingPreservingCancellation {
        lastFmGenreCharts.genreChart(
            tags = tags,
            languageCode = languageCode,
            match = { title, artist -> repository.searchSongMatch(title, artist, languageCode) },
            onProgress = onProgress
        )
    }.onFailure { Timber.w(it, "Last.fm genre chart failed for %s", identity) }.getOrDefault(emptyList())

    private fun loadExploreGenreChart(identity: String, tags: List<String>) {
        val languageCode = uiState.value.languageCode
        val key = "$languageCode:$identity"
        val current = _exploreGenreChart.value
        if (current.coversRequest(key, System.currentTimeMillis())) return
        exploreGenreChartJob?.cancel()
        if (tags.isEmpty()) {
            _exploreGenreChart.value = ExploreGenreChartState(key = key)
            return
        }
        _exploreGenreChart.value = if (current.key == key) current.copy(isLoading = true) else ExploreGenreChartState(key = key, isLoading = true)
        exploreGenreChartJob = scope.launch {
            fun publish(tracks: List<Track>, loading: Boolean) {
                _exploreGenreChart.update { state ->
                    if (state.key != key) state
                    else if (loading && tracks.size <= state.tracks.size) state
                    else state.copy(tracks = tracks, isLoading = loading)
                }
            }
            publish(lastFmGenreCharts.cached(tags, languageCode), loading = true)
            val tracks = fetchExploreGenreChart(identity, tags, languageCode) { partial ->
                if (partial.size >= EXPLORE_GENRE_CHART_FIRST_PAINT) publish(partial, loading = true)
            }
            _exploreGenreChart.update { state -> if (state.key != key) state else state.finishedWith(tracks, System.currentTimeMillis()) }
        }
    }

    fun loadExploreZoneDiscovery(zone: ExploreZone) {
        val curators = AppleExploreCatalog.zoneCuratorIds(zone.id)
        loadExploreGenreDiscovery("zone:${zone.id}", LastFmGenreTags.forZone(zone.id), curators) { apple, front, lang ->
            apple.curatorGenre(curators, front, lang)
        }
    }

    fun loadExploreCategoryDiscovery(category: ExploreCategory) {
        val curators = listOfNotNull(AppleExploreCatalog.curatorId(category.params))
        val playlistParams = category.params.takeIf { LastFmGenreTags.decadeOf(category) != null }
        loadExploreGenreDiscovery("category:${category.params}", LastFmGenreTags.forCategory(category), curators, playlistParams) { apple, front, lang ->
            apple.genre(category.title, category.params, front, lang)
        }
    }

    private suspend fun loadAppleGenreShelves(
        apple: AppleMusicEditorialRepository,
        identity: String,
        cachedCurators: List<String>,
        storefront: String,
        appleLanguage: String,
        fetchApple: suspend (AppleMusicEditorialRepository, String, String) -> AppleCatalogPage?,
        publish: ((ExploreGenreDiscoveryState) -> ExploreGenreDiscoveryState) -> Unit
    ) {
        if (_exploreGenreDiscovery.value.apple.isEmpty()) {
            apple.cachedCuratorGenre(cachedCurators, storefront, appleLanguage)?.let { page ->
                val shelves = AppleExploreCatalog.genreShelves(page)
                publish { state -> if (state.apple.isEmpty()) state.copy(apple = shelves) else state }
            }
        }
        val page = runCatchingPreservingCancellation { fetchApple(apple, storefront, appleLanguage) }
            .onFailure { Timber.w(it, "Apple genre discovery failed for %s", identity) }
            .getOrNull()
        val shelves = page?.let(AppleExploreCatalog::genreShelves).orEmpty()
        if (shelves.isNotEmpty()) publish { state -> state.copy(apple = shelves) }
    }

    private suspend fun loadLastFmGenreDiscovery(
        identity: String,
        tags: List<String>,
        languageCode: String,
        publish: ((ExploreGenreDiscoveryState) -> ExploreGenreDiscoveryState) -> Unit
    ) {
        if (tags.isEmpty()) return
        if (_exploreGenreDiscovery.value.lastFm == null) {
            lastFmGenreDiscovery.cached(tags, languageCode)?.let { stored ->
                publish { state -> if (state.lastFm == null) state.copy(lastFm = stored) else state }
            }
        }
        val fresh = runCatchingPreservingCancellation { lastFmGenreDiscovery.discovery(tags, languageCode) }
            .onFailure { Timber.w(it, "Last.fm genre discovery failed for %s", identity) }
            .getOrNull()
        if (fresh != null) publish { state -> state.copy(lastFm = fresh) }
    }

    private fun loadExploreGenreDiscovery(
        identity: String,
        tags: List<String>,
        cachedCurators: List<String>,
        playlistParams: String? = null,
        fetchApple: suspend (AppleMusicEditorialRepository, String, String) -> AppleCatalogPage?
    ) {
        val languageCode = uiState.value.languageCode
        val storefront = AppleStorefronts.normalize(ChartsCatalog.region(uiState.value.selectedChartId).country)
        val appleLanguage = AppleStorefronts.language(languageCode)
        val key = "$languageCode:$identity"
        val current = _exploreGenreDiscovery.value
        val sameTarget = current.key == key && current.storefront == storefront
        if (sameTarget && current.isLoading) return
        exploreGenreDiscoveryJob?.cancel()
        _exploreGenreDiscovery.value =
            if (sameTarget) current.copy(isLoading = true)
            else ExploreGenreDiscoveryState(key = key, storefront = storefront, isLoading = true)
        exploreGenreDiscoveryJob = scope.launch {
            fun publish(transform: (ExploreGenreDiscoveryState) -> ExploreGenreDiscoveryState) {
                _exploreGenreDiscovery.update { state -> if (state.key == key && state.storefront == storefront) transform(state) else state }
            }
            val apple = AppleMusicEditorialRepository.get(application)
            coroutineScope {
                launch { loadAppleGenreShelves(apple, identity, cachedCurators, storefront, appleLanguage, fetchApple, ::publish) }
                launch { loadLastFmGenreDiscovery(identity, tags, languageCode, ::publish) }
                playlistParams?.let { params -> launch { loadProviderGenrePlaylists(identity, params, languageCode, ::publish) } }
            }
            publish { state -> state.copy(isLoading = false) }
        }
    }

    private suspend fun loadProviderGenrePlaylists(
        identity: String,
        params: String,
        languageCode: String,
        publish: ((ExploreGenreDiscoveryState) -> ExploreGenreDiscoveryState) -> Unit
    ) {
        val sections = runCatchingPreservingCancellation { repository.moodPlaylistSections(params, languageCode) }
            .onFailure { Timber.w(it, "Provider genre playlists failed for %s", identity) }
            .getOrDefault(emptyList())
            .filter { section -> section.playlists.any { it.thumbnailUrl.isNotBlank() } }
        if (sections.isNotEmpty()) publish { state -> state.copy(playlists = sections) }
    }

    fun ensureExploreCategoryArtwork(params: String, allowTrackFallback: Boolean) {
        if (params.isBlank() || !uiState.value.exploreCategoryArtwork[params].isNullOrBlank()) return
        val languageCode = uiState.value.languageCode
        scope.launch {
            resolveExploreCategoryArtwork(
                params = params,
                languageCode = languageCode,
                allowTrackFallback = allowTrackFallback
            )
        }
    }

    private suspend fun resolveExploreCategoryArtwork(
        params: String,
        languageCode: String,
        allowTrackFallback: Boolean
    ) {
        if (
            params.isBlank() ||
            uiState.value.languageCode != languageCode ||
            !uiState.value.exploreCategoryArtwork[params].isNullOrBlank()
        ) return

        val mode = if (allowTrackFallback) "deep" else "shallow"
        val requestKey = "$languageCode:$params:$mode"
        if (!exploreCategoryArtworkRequests.add(requestKey)) return
        try {
            val editorialArtwork = editorialChartsRepository
                .exploreCollection(params, 1)
                ?.artworkUrl
                .orEmpty()
            val artwork = editorialArtwork.ifBlank {
                exploreCategoryArtworkSemaphore.withPermit {
                    repository.moodCategoryArtwork(
                        params = params,
                        languageCode = languageCode,
                        allowTrackFallback = allowTrackFallback
                    )
                }
            }
            if (artwork.isNotBlank() && uiState.value.languageCode == languageCode) {
                uiState.update { current ->
                    if (!current.exploreCategoryArtwork[params].isNullOrBlank()) current
                    else current.copy(
                        exploreCategoryArtwork = current.exploreCategoryArtwork + (params to artwork)
                    )
                }
                schedulePersistExploreDiscovery(languageCode)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Provider Explore category artwork failed")
        } finally {
            exploreCategoryArtworkRequests.remove(requestKey)
        }
    }

    fun warmExploreDiscoveryArtwork() {
        val snapshot = uiState.value
        val languageCode = snapshot.languageCode
        val missing = snapshot.exploreCategories
            .filter { category ->
                category.params.isNotBlank() &&
                    snapshot.exploreCategoryArtwork[category.params].isNullOrBlank() &&
                    "$languageCode:${category.params}" !in exploreArtworkWarmupAttempts
            }
        if (missing.isEmpty()) return
        if (exploreArtworkWarmupJob?.isActive == true && exploreArtworkWarmupLanguage == languageCode) return

        exploreArtworkWarmupJob?.cancel()
        exploreArtworkWarmupLanguage = languageCode
        exploreArtworkWarmupJob = scope.launch {
            val prioritized = missing.sortedWith(
                compareBy<ExploreCategory> { category -> if (category.sectionIndex == 0) 0 else 1 }
                    .thenBy { category -> category.sectionIndex }
            )
            coroutineScope {
                prioritized.mapIndexed { index, category ->
                    async {
                        exploreArtworkWarmupAttempts += "$languageCode:${category.params}"
                        resolveExploreCategoryArtwork(
                            params = category.params,
                            languageCode = languageCode,
                            allowTrackFallback = category.sectionIndex == 0 &&
                                index < EXPLORE_DISCOVERY_DEEP_WARMUP_LIMIT
                        )
                    }
                }.awaitAll()
            }
            if (uiState.value.languageCode == languageCode) {
                schedulePersistExploreDiscovery(languageCode)
            }
            exploreArtworkWarmupJob = null
            if (uiState.value.languageCode == languageCode) {
                warmExploreDiscoveryArtwork()
            }
        }
    }

    private fun schedulePersistExploreDiscovery(languageCode: String) {
        exploreDiscoveryPersistJob?.cancel()
        exploreDiscoveryPersistJob = scope.launch {
            delay(350L)
            val current = uiState.value
            if (current.languageCode != languageCode || current.exploreCategories.isEmpty()) return@launch
            preferences.saveExploreDiscovery(
                categories = current.exploreCategories,
                artwork = current.exploreCategoryArtwork,
                languageCode = languageCode,
                savedAtMs = exploreCategoriesCachedAtMs
            )
        }
    }

    fun ensureExploreCategoriesLoaded() {
        val languageCode = uiState.value.languageCode
        if (exploreCategoriesJob?.isActive == true) {
            if (exploreCategoriesRequestLanguage == languageCode) return
            exploreCategoriesJob?.cancel()
        }
        val hasCachedCategories =
            exploreCategoriesLoadedLanguage == languageCode && uiState.value.exploreCategories.isNotEmpty()
        val cacheAgeMs = System.currentTimeMillis() - exploreCategoriesCachedAtMs
        if (
            hasCachedCategories &&
            exploreCategoriesCachedAtMs > 0L &&
            cacheAgeMs in 0 until EXPLORE_DISCOVERY_CACHE_TTL_MS
        ) return

        val requestGeneration = ++exploreCategoriesRequestGeneration
        exploreCategoriesRequestLanguage = languageCode
        markExploreCategoriesLoading(languageCode)
        exploreCategoriesJob = scope.launch {
            try {
                val categories = fetchExploreCategories(languageCode)
                publishExploreCategories(languageCode, requestGeneration, categories)
            } finally {
                finishExploreCategoriesRequest(languageCode, requestGeneration)
            }
        }
    }

    private fun markExploreCategoriesLoading(languageCode: String) {
        uiState.update { current ->
            if (current.languageCode != languageCode) {
                current
            } else {
                current.copy(
                    exploreCategories = current.exploreCategories,
                    isExploreCategoriesLoading = current.exploreCategories.isEmpty()
                )
            }
        }
    }

    private suspend fun fetchExploreCategories(languageCode: String): List<ExploreCategory> = try {
        repository.moodCategories(languageCode)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Timber.w(error, "Provider Explore categories failed for %s", languageCode)
        emptyList()
    }.asSequence()
        .filter { category -> category.title.isNotBlank() && category.params.isNotBlank() }
        .distinctBy { category -> category.params }
        .toList()

    private fun publishExploreCategories(
        languageCode: String,
        requestGeneration: Long,
        categories: List<ExploreCategory>
    ) {
        if (
            exploreCategoriesRequestGeneration != requestGeneration ||
            uiState.value.languageCode != languageCode
        ) return
        if (categories.isNotEmpty()) {
            exploreCategoriesLoadedLanguage = languageCode
            exploreCategoriesCachedAtMs = System.currentTimeMillis()
        }
        uiState.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(
                    exploreCategories = categories.ifEmpty { current.exploreCategories },
                    isExploreCategoriesLoading = false
                )
            } else {
                current
            }
        }
        if (categories.isNotEmpty()) {
            schedulePersistExploreDiscovery(languageCode)
            warmExploreDiscoveryArtwork()
        }
    }

    private fun finishExploreCategoriesRequest(languageCode: String, requestGeneration: Long) {
        if (exploreCategoriesRequestGeneration != requestGeneration) return
        uiState.update { current ->
            if (current.languageCode == languageCode && current.isExploreCategoriesLoading) {
                current.copy(isExploreCategoriesLoading = false)
            } else {
                current
            }
        }
        exploreCategoriesJob = null
    }
}
