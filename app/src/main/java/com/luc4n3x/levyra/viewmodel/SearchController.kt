package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.EditorialChartsRepository
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import com.luc4n3x.levyra.feature.search.LevyraSearchEngine
import com.luc4n3x.levyra.feature.search.LocalSearchAffinity
import com.luc4n3x.levyra.feature.search.LocalSearchCandidate
import com.luc4n3x.levyra.feature.search.SearchLatencyReport
import com.luc4n3x.levyra.feature.search.SearchSideEffects
import com.luc4n3x.levyra.feature.search.YoutubeMusicSearchBackend
import com.luc4n3x.levyra.player.AdaptivePlaybackPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

private const val SEARCH_HIDDEN_LISTENS_LIMIT = 200
private const val LOCAL_SEARCH_CACHE_CANDIDATE_LIMIT = 600
private const val SEARCH_LATENCY_LOG_TAG = "LevyraSearch"
private const val LOCAL_LIBRARY_SEARCH_CANDIDATE_LIMIT = 4_000

internal class SearchController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val application: Application,
    private val preferences: LevyraPreferences,
    private val repository: YoutubeMusicRepository,
    private val artistRepository: ArtistRepository,
    private val providerRouter: LevyraProviderRouter,
    private val editorialChartsRepository: EditorialChartsRepository,
    private val moodEngine: MoodEngine,
    private val adaptivePlaybackPolicy: AdaptivePlaybackPolicy,
    private val calculateSmartScore: (List<Track>) -> Int,
    private val mergeTracks: (List<Track>, List<Track>) -> List<Track>,
    private val prefetchTop: (List<Track>, Int) -> Unit
) {
    private val spotifySearchClient by lazy {
        com.luc4n3x.levyra.data.spotify.SpotifySearchClient(
            catalogSearch = editorialChartsRepository::searchSpotifyMetadata,
            countryProvider = { ChartsCatalog.defaultRegionForLanguage(uiState.value.languageCode).country }
        )
    }
    val engine = LevyraSearchEngine(
        scope = scope,
        backend = YoutubeMusicSearchBackend(
            repository = repository,
            artistRepository = artistRepository,
            providerRouter = providerRouter,
            spotifyClient = spotifySearchClient,
            localCandidateSource = ::localSearchCandidates,
            playableTrack = { track -> youtubePlayableTrack(track) != null }
        ),
        sideEffects = object : SearchSideEffects {
            override fun onRemoteTracks(tracks: List<Track>) = applyRemoteSearchTracks(tracks)

            override fun onPrefetch(tracks: List<Track>) = prefetchSearchResults(tracks)

            override fun onLatency(report: SearchLatencyReport) {
                if (BuildConfig.DEBUG) Timber.tag(SEARCH_LATENCY_LOG_TAG).d(report.format())
            }
        }
    )

    private val _searchHiddenListens = MutableStateFlow<Set<String>>(emptySet())
    val searchHiddenListens: StateFlow<Set<String>> = _searchHiddenListens.asStateFlow()
    private val searchHiddenListensWrite = kotlinx.coroutines.sync.Mutex()

    fun restoreHiddenListens() {
        scope.launch(Dispatchers.IO) {
            val saved = preferences.searchHiddenListens()
            _searchHiddenListens.update { saved + it }
        }
    }

    fun removeRecentSearch(track: Track) {
        val identity = LevyraPersonalOrbit.identityKey(track)
        val hidden = _searchHiddenListens.updateAndGet { (it + identity).toList().takeLast(SEARCH_HIDDEN_LISTENS_LIMIT).toSet() }
        scope.launch(Dispatchers.IO) {
            searchHiddenListensWrite.withLock {
                val merged = preferences.searchHiddenListens() + _searchHiddenListens.value + hidden
                preferences.saveSearchHiddenListens(merged.toList().takeLast(SEARCH_HIDDEN_LISTENS_LIMIT).toSet())
            }
        }
        val matches: (Track) -> Boolean = { candidate ->
            candidate.id == track.id || LevyraPersonalOrbit.identityKey(candidate) == identity
        }
        var changed = false
        val applied = uiState.updateAndGet { state ->
            val updated = state.recentSearches.filterNot(matches)
            val updatedOrbit = state.personalOrbitTracks.filterNot(matches)
            changed = updated.size != state.recentSearches.size ||
                updatedOrbit.size != state.personalOrbitTracks.size
            if (!changed) {
                state
            } else {
                state.copy(recentSearches = updated, personalOrbitTracks = updatedOrbit)
            }
        }
        if (!changed) return
        val persistedSearches = applied.recentSearches
        val persistedOrbit = applied.personalOrbitTracks
        val languageCode = applied.languageCode
        scope.launch(Dispatchers.IO) {
            preferences.saveRecentSearches(persistedSearches)
            preferences.savePersonalOrbitTracks(persistedOrbit, languageCode)
        }
    }

    private fun localSearchCandidates(): List<LocalSearchCandidate> {
        val snapshot = uiState.value
        val cached = repository.cachedTracks()
        return buildList {
            fun addTracks(tracks: List<Track>, affinity: Int) {
                tracks.forEach { track -> add(LocalSearchCandidate(track, affinity)) }
            }
            addTracks(snapshot.recentSearches, LocalSearchAffinity.RECENT)
            addTracks(snapshot.recentListens, LocalSearchAffinity.LISTENED)
            addTracks(snapshot.favorites, LocalSearchAffinity.FAVORITE)
            addTracks(
                snapshot.localLibrary.catalog.songs.take(LOCAL_LIBRARY_SEARCH_CANDIDATE_LIMIT),
                LocalSearchAffinity.LOCAL_MEDIA
            )
            addTracks(snapshot.personalOrbitTracks, LocalSearchAffinity.ORBIT)
            addTracks(snapshot.queue, LocalSearchAffinity.QUEUE)
            addTracks(snapshot.tracks, LocalSearchAffinity.SESSION)
            snapshot.homeSections.forEach { section -> addTracks(section.tracks, LocalSearchAffinity.HOME) }
            addTracks(snapshot.charts, LocalSearchAffinity.CHARTS)
            addTracks(cached.takeLast(LOCAL_SEARCH_CACHE_CANDIDATE_LIMIT).asReversed(), LocalSearchAffinity.CACHE)
        }
    }

    private fun applyRemoteSearchTracks(tracks: List<Track>) {
        val queue = moodEngine.buildQueue(uiState.value.selectedMood, tracks)
        val smartScore = calculateSmartScore(queue)
        val cacheReport = repository.cacheReport()
        uiState.update {
            it.copy(
                tracks = mergeTracks(it.tracks, tracks),
                cacheReport = cacheReport,
                smartScore = smartScore
            )
        }
    }

    private fun prefetchSearchResults(tracks: List<Track>) {
        val plan = adaptivePlaybackPolicy.current(videoMode = false)
        LevyraArtworkCache.preloadHome(application.applicationContext, tracks, if (plan.lowRam) 6 else 12)
        prefetchTop(tracks, if (plan.lowRam) 2 else 4)
    }
}
