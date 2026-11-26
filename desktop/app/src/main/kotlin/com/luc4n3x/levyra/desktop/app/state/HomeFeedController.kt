package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.charts.MusicVideoChart
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    val curated: List<com.luc4n3x.levyra.desktop.core.moods.MoodPlaylist> = emptyList(),
    val curatedShelves: List<com.luc4n3x.levyra.desktop.core.moods.MoodShelf> = emptyList()
)

class HomeFeedController(
    private val scope: CoroutineScope,
    private val catalog: CatalogRepository,
    private val libraryStore: LibraryStore,
    private val moods: com.luc4n3x.levyra.desktop.core.moods.MoodsRepository,
    private val videoChart: MusicVideoChart = MusicVideoChart(catalog)
) {
    private val state = MutableStateFlow(HomeFeedState())
    val feed: StateFlow<HomeFeedState> = state.asStateFlow()
    private var videosCountry = ""
    private var dailyKey = ""
    private var curatedKey = ""

    fun load(country: String, languageTag: String) {
        val curatedRegion = "$languageTag|${country.trim().uppercase()}"
        if (curatedRegion != curatedKey) {
            curatedKey = curatedRegion
            scope.launch {
                val shelves = safely { moods.curatedShelves(languageTag, country) }.orEmpty().distinctBy { it.title }
                val playlists = shelves
                    .flatMap { it.playlists }
                    .distinctBy { it.id }
                    .take(CURATED_LIMIT)
                if (curatedKey == curatedRegion) state.update { it.copy(curated = playlists, curatedShelves = shelves) }
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
        const val CURATED_LIMIT = 20
        const val SEARCH_PARALLELISM = 4
        const val MIN_SONG_MS = 90_000L
        const val MAX_SONG_MS = 480_000L
        val DECORATION = Regex("""\s*[(\[][^)\]]*[)\]]""")
        val NON_SONG_WORDS = setOf("podcast", "intervista", "interview", "puntata", "episode", "episodio", "compilation", "playlist", "mix", "hits", "top", "reaction", "trailer", "tg", "news")
    }
}
