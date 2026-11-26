package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.podcast.PODCAST_RESUME_MIN_POSITION_MS
import com.luc4n3x.levyra.desktop.core.podcast.PodcastEpisodeProgress
import com.luc4n3x.levyra.desktop.core.podcast.PodcastLibraryStore
import com.luc4n3x.levyra.desktop.core.podcast.PodcastLocale
import com.luc4n3x.levyra.desktop.core.podcast.PodcastRepository
import com.luc4n3x.levyra.desktop.core.podcast.PodcastShelf
import com.luc4n3x.levyra.desktop.core.podcast.PodcastShow
import com.luc4n3x.levyra.desktop.core.podcast.PodcastShowPage
import com.luc4n3x.levyra.desktop.core.podcast.resumePositionMs
import com.luc4n3x.levyra.desktop.player.PlaybackStatus
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PodcastShowState(
    val show: PodcastShow,
    val page: PodcastShowPage? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val failed: Boolean = false
)

data class PodcastUiState(
    val locale: PodcastLocale? = null,
    val query: String = "",
    val searching: Boolean = false,
    val searchFailed: Boolean = false,
    val results: List<PodcastShow> = emptyList(),
    val discover: List<PodcastShelf> = emptyList(),
    val discoverLoading: Boolean = false,
    val discoverFailed: Boolean = false,
    val subscriptions: List<PodcastShow> = emptyList(),
    val history: List<PodcastEpisodeProgress> = emptyList(),
    val openShow: PodcastShowState? = null
) {
    val subscribedIds: Set<String> get() = subscriptions.mapTo(hashSetOf()) { it.browseId }
    val progressById: Map<String, PodcastEpisodeProgress> get() = history.associateBy { it.track.id }
    val continueListening: List<PodcastEpisodeProgress> get() = history.filter { it.inProgress }
}

class PodcastController(
    private val scope: CoroutineScope,
    private val repository: PodcastRepository,
    private val store: PodcastLibraryStore,
    private val playback: PlaybackController
) {
    private val internalState = MutableStateFlow(PodcastUiState())
    private var searchJob: Job? = null
    private var showJob: Job? = null
    private var discoverJob: Job? = null
    private var resumeJob: Job? = null
    @Volatile private var trackedEpisodes: Map<String, Track> = emptyMap()
    private var lastSavedId = ""
    @Volatile private var resumedId = ""
    private var lastSavedPositionMs = 0L

    val state: StateFlow<PodcastUiState> = internalState.asStateFlow()

    init {
        scope.launch {
            store.library.collect { data ->
                internalState.update { state ->
                    state.copy(subscriptions = data.subscriptions.map { it.show }, history = data.history)
                }
            }
        }
        scope.launch { playback.state.collect(::recordProgress) }
    }

    fun activate(locale: PodcastLocale) {
        val current = internalState.value
        if (current.locale == locale && (current.discover.isNotEmpty() || discoverJob?.isActive == true)) return
        internalState.update { it.copy(locale = locale, discover = if (it.locale == locale) it.discover else emptyList()) }
        loadDiscover()
    }

    fun retryDiscover() = loadDiscover()

    private fun loadDiscover() {
        val locale = internalState.value.locale ?: return
        discoverJob?.cancel()
        internalState.update { it.copy(discoverLoading = true, discoverFailed = false) }
        discoverJob = scope.launch {
            val shelves = guarded { repository.shelves(locale) }
            internalState.update { it.copy(discover = shelves ?: it.discover, discoverLoading = false, discoverFailed = shelves == null) }
        }
    }

    fun setQuery(value: String) {
        val clean = value.take(PodcastRepository.MAX_QUERY_LENGTH)
        searchJob?.cancel()
        val searchable = clean.trim().length >= 2
        internalState.update { it.copy(query = clean, searching = searchable, searchFailed = false, results = if (searchable) it.results else emptyList()) }
        val locale = internalState.value.locale
        if (!searchable || locale == null) return
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val results = guarded { repository.search(clean, locale) }
            internalState.update { if (it.query != clean) it else it.copy(results = results.orEmpty(), searching = false, searchFailed = results == null) }
        }
    }

    fun openShow(show: PodcastShow) {
        val locale = internalState.value.locale ?: return
        showJob?.cancel()
        internalState.update { it.copy(openShow = PodcastShowState(show)) }
        showJob = scope.launch {
            val page = guarded { repository.show(show.browseId, locale) }
            internalState.update { state ->
                val open = state.openShow?.takeIf { it.show.browseId == show.browseId } ?: return@update state
                state.copy(openShow = open.copy(page = page, loading = false, failed = page == null, show = page?.show ?: open.show))
            }
        }
    }

    fun retryShow() = internalState.value.openShow?.show?.let(::openShow)

    fun closeShow() {
        showJob?.cancel()
        internalState.update { it.copy(openShow = null) }
    }

    fun loadMoreEpisodes() {
        val locale = internalState.value.locale ?: return
        val open = internalState.value.openShow ?: return
        val continuation = open.page?.episodes?.continuation ?: return
        if (open.loadingMore) return
        internalState.update { it.copy(openShow = open.copy(loadingMore = true)) }
        showJob = scope.launch {
            val more = guarded { repository.moreEpisodes(continuation, locale) }
            internalState.update { state ->
                val current = state.openShow?.takeIf { it.show.browseId == open.show.browseId } ?: return@update state
                val currentPage = current.page ?: return@update state
                val merged = if (more == null) {
                    currentPage.copy(episodes = currentPage.episodes.copy(continuation = null))
                } else {
                    currentPage.copy(
                        episodes = currentPage.episodes.copy(
                            episodes = (currentPage.episodes.episodes + more.episodes.map { it.copy(artist = current.show.title.ifBlank { it.artist }) })
                                .distinctBy(Track::id)
                                .take(MAX_SHOW_EPISODES),
                            continuation = more.continuation.takeIf { currentPage.episodes.episodes.size < MAX_SHOW_EPISODES }
                        )
                    )
                }
                state.copy(openShow = current.copy(page = merged, loadingMore = false))
            }
        }
    }

    fun toggleSubscription(show: PodcastShow) = write { store.toggleSubscription(show) }

    fun setPlayed(episode: Track, played: Boolean) = write { store.setPlayed(episode, played) }

    fun removeFromHistory(episodeId: String) = write { store.remove(episodeId) }

    fun clearHistory() = write { store.clearHistory() }

    fun play(episodes: List<Track>, episode: Track) {
        val list = episodes.distinctBy(Track::id).ifEmpty { listOf(episode) }
        val index = list.indexOfFirst { it.id == episode.id }.coerceAtLeast(0)
        trackedEpisodes = list.associateBy(Track::id)
        resumeJob?.cancel()
        resumedId = ""
        playback.playTracks(list, index)
    }

    private fun resumeIfSaved(episodeId: String) {
        resumedId = episodeId
        val resume = resumePositionMs(internalState.value.progressById[episodeId])
        resumeJob?.cancel()
        if (resume <= 0L) return
        resumeJob = scope.launch {
            val ready = withTimeoutOrNull(RESUME_TIMEOUT_MS) {
                playback.state.first { it.current?.id == episodeId && it.durationMs > 0L && it.status == PlaybackStatus.PLAYING }
            }
            if (ready != null && resume < ready.durationMs) playback.seekTo(resume)
        }
    }

    private fun recordProgress(snapshot: PlaybackUiState) {
        val current = snapshot.current ?: return
        val episode = trackedEpisodes[current.id] ?: return
        if (current.id != resumedId) {
            resumeIfSaved(current.id)
            return
        }
        if (resumeJob?.isActive == true) return
        val position = snapshot.positionMs
        if (snapshot.durationMs <= 0L || position < PODCAST_RESUME_MIN_POSITION_MS) return
        val moved = abs(position - lastSavedPositionMs)
        val due = current.id != lastSavedId || moved >= SAVE_INTERVAL_MS || (snapshot.status != PlaybackStatus.PLAYING && moved > 0L)
        if (!due) return
        lastSavedId = current.id
        lastSavedPositionMs = position
        write { store.saveProgress(episode, position, snapshot.durationMs) }
    }

    private fun write(block: () -> Unit) {
        scope.launch(Dispatchers.IO) { guarded { block() } }
    }

    private suspend fun <T> guarded(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        null
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        const val SAVE_INTERVAL_MS = 10_000L
        const val RESUME_TIMEOUT_MS = 30_000L
        const val MAX_SHOW_EPISODES = 600
    }
}
