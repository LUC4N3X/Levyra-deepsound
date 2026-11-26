package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.charts.MusicVideoChart
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.moods.CuratedCollections
import com.luc4n3x.levyra.desktop.core.moods.MoodPlaylist
import com.luc4n3x.levyra.desktop.core.moods.MoodShelf
import com.luc4n3x.levyra.desktop.core.moods.MoodsRepository
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.luc4n3x.levyra.desktop.core.model.SearchFilter

data class DailyPick(val track: Track, val seed: Track)

data class HomeFeedState(
    val videos: List<Track> = emptyList(),
    val daily: List<DailyPick> = emptyList(),
    val curatedShelves: List<MoodShelf> = emptyList()
)

data class CuratedPageState(
    val collections: CuratedCollections? = null,
    val loading: Boolean = false,
    val category: String = "",
    val categoryLoading: Boolean = false,
    val playlists: List<MoodPlaylist> = emptyList()
)

class HomeFeedController(
    private val scope: CoroutineScope,
    private val catalog: CatalogRepository,
    private val libraryStore: LibraryStore,
    private val moods: MoodsRepository,
    private val videoChart: MusicVideoChart = MusicVideoChart(catalog)
) {
    private val state = MutableStateFlow(HomeFeedState())
    val feed: StateFlow<HomeFeedState> = state.asStateFlow()
    private var videosCountry = ""
    private var dailyKey = ""
    private var curatedRegion: Pair<String, String>? = null
    private var curatedJob: Job? = null
    private var collectionsJob: Job? = null
    private var categoryJob: Job? = null
    private val categories = mutableMapOf<String, List<MoodPlaylist>>()
    private val curatedState = MutableStateFlow(CuratedPageState())
    val curatedPage: StateFlow<CuratedPageState> = curatedState.asStateFlow()

    @Synchronized
    fun load(country: String, languageTag: String) {
        val locale = languageTag to country
        if (locale != curatedRegion) {
            curatedRegion = locale
            curatedJob?.cancel()
            collectionsJob?.cancel()
            categoryJob?.cancel()
            categories.clear()
            curatedState.value = CuratedPageState()
            state.update { it.copy(curatedShelves = emptyList()) }
            curatedJob = scope.launch {
                val shelves = safely { moods.curatedShelves(languageTag, country) }.orEmpty()
                synchronized(this@HomeFeedController) {
                    if (curatedRegion == locale) state.update { it.copy(curatedShelves = shelves) }
                }
            }
        }
        val region = country.trim().uppercase().take(2).ifBlank { "US" }
        if (region != videosCountry) {
            videosCountry = region
            scope.launch {
                val videos = safely { videoChart.trending(region) }.orEmpty()
                if (videosCountry == region) state.update { it.copy(videos = videos) }
            }
        }
        loadDaily()
    }

    @Synchronized
    fun loadCuratedCollections(language: String, country: String) {
        if (curatedRegion != (language to country)) load(country, language)
        if (curatedState.value.collections != null || collectionsJob?.isActive == true) return
        curatedState.update { it.copy(loading = true) }
        collectionsJob = scope.launch {
            val collections = safely { moods.curatedCollections(language, country) }
            synchronized(this@HomeFeedController) {
                if (curatedRegion != (language to country)) return@synchronized
                curatedState.update { it.copy(collections = collections?.takeIf { it.categories.isNotEmpty() || it.decades.isNotEmpty() || it.everyday.isNotEmpty() }, loading = false) }
                collections?.categories?.firstOrNull()?.let { selectCuratedCategory(it.params) }
            }
        }
    }

    @Synchronized
    fun selectCuratedCategory(params: String) {
        val (language, country) = curatedRegion ?: return
        val current = curatedState.value
        if (current.category == params && (current.categoryLoading || current.playlists.isNotEmpty())) return
        categoryJob?.cancel()
        val cached = categories[params]
        curatedState.update { it.copy(category = params, categoryLoading = cached == null, playlists = cached.orEmpty()) }
        if (cached != null) return
        categoryJob = scope.launch {
            val playlists = safely { moods.curatedCategory(params, language, country) }.orEmpty()
            synchronized(this@HomeFeedController) {
                if (curatedRegion != (language to country) || curatedState.value.category != params) return@synchronized
                if (playlists.isNotEmpty()) categories[params] = playlists
                curatedState.update { it.copy(categoryLoading = false, playlists = playlists) }
            }
        }
    }

    private fun loadDaily() {
        val library = libraryStore.current
        val seeds = (library.history.sortedByDescending { it.playedAt }.map { it.track } + library.favorites)
            .filter { it.videoUrl.isNotBlank() }
            .distinctBy { soundtrackPrimaryArtist(it.artist).lowercase() }
            .take(SEED_COUNT)
        val key = "${LocalDate.now().toEpochDay()}|${seeds.joinToString(",") { it.id }}"
        if (seeds.isEmpty() || key == dailyKey) return
        dailyKey = key
        val known = (library.history.map { it.track.id } + library.favorites.map { it.id }).toHashSet()
        scope.launch {
            val gate = Semaphore(SEARCH_PARALLELISM)
            val perSeed = seeds.map { seed ->
                async {
                    safely { catalog.radio(seed, RADIO_LIMIT) }.orEmpty()
                        .filter(::looksLikeSong)
                        .filterNot { soundtrackPrimaryArtist(it.artist).equals(soundtrackPrimaryArtist(seed.artist), ignoreCase = true) }
                        .take(PICKS_PER_SEED * 2)
                        .map { candidate -> async { gate.withPermit { songMatch(candidate) } } }
                        .awaitAll()
                        .filterNotNull()
                        .filterNot { it.id in known }
                        .take(PICKS_PER_SEED)
                        .map { DailyPick(it, seed) }
                }
            }.awaitAll()
            val interleaved = (0 until PICKS_PER_SEED).flatMap { index -> perSeed.mapNotNull { it.getOrNull(index) } }
                .distinctBy { it.track.id }
            if (dailyKey == key) state.update { it.copy(daily = interleaved) }
        }
    }

    private suspend fun songMatch(candidate: Track): Track? {
        val query = "${candidate.title.replace(DECORATION, "")} ${soundtrackPrimaryArtist(candidate.artist)}".trim()
        val songs = safely { catalog.search(query, SearchFilter.SONGS) }?.tracks.orEmpty()
        val title = MusicVideoChart.normalize(candidate.title.replace(DECORATION, ""))
        return songs.take(5).firstOrNull { song ->
            val songTitle = MusicVideoChart.normalize(song.title.replace(DECORATION, ""))
            songTitle.isNotEmpty() && (songTitle in title || title in songTitle)
        }
    }

    private fun looksLikeSong(track: Track): Boolean {
        if (track.durationMs !in MIN_SONG_MS..MAX_SONG_MS) return false
        val words = MusicVideoChart.normalize(track.title).split(" ").toSet() + MusicVideoChart.normalize(track.artist).split(" ").toSet()
        return NON_SONG_WORDS.none { it in words }
    }

    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val SEED_COUNT = 4
        const val RADIO_LIMIT = 20
        const val PICKS_PER_SEED = 4
        const val SEARCH_PARALLELISM = 4
        const val MIN_SONG_MS = 90_000L
        const val MAX_SONG_MS = 480_000L
        val DECORATION = Regex("""\s*[(\[][^)\]]*[)\]]""")
        val NON_SONG_WORDS = setOf("podcast", "intervista", "interview", "puntata", "episode", "episodio", "compilation", "playlist", "mix", "hits", "top", "reaction", "trailer", "tg", "news")
    }
}
