package com.luc4n3x.levyra.feature.podcast

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.PodcastShelf
import com.luc4n3x.levyra.data.PodcastShow
import com.luc4n3x.levyra.data.PodcastShowPage
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.YoutubePodcastsRepository
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PodcastEpisodeProgressEntity
import com.luc4n3x.levyra.data.local.PodcastLibraryDao
import com.luc4n3x.levyra.data.local.PodcastSubscriptionEntity
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MAX_QUERY_LENGTH = 120
private const val RESUME_MIN_POSITION_MS = 5_000L

internal data class PodcastEpisodeProgress(
    val track: Track,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean
) {
    val fraction: Float get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val inProgress: Boolean get() = !completed && positionMs >= RESUME_MIN_POSITION_MS
}

internal data class PodcastShowState(
    val show: PodcastShow,
    val page: PodcastShowPage? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val failed: Boolean = false
)

internal data class PodcastLibraryUiState(
    val languageCode: String = "",
    val contentScope: String = "",
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

internal fun resumePositionMs(progress: PodcastEpisodeProgress?): Long =
    progress?.takeIf { it.inProgress }?.positionMs ?: 0L

internal class PodcastLibraryViewModel(
    private val repository: YoutubePodcastsRepository,
    private val dao: PodcastLibraryDao
) : ViewModel() {
    private val _state = MutableStateFlow(PodcastLibraryUiState())
    val state: StateFlow<PodcastLibraryUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var showJob: Job? = null
    private var discoverJob: Job? = null

    init {
        viewModelScope.launch {
            dao.subscriptions().collect { rows ->
                _state.update { state -> state.copy(subscriptions = rows.map { PodcastShow(it.browseId, it.title, it.author, it.thumbnailUrl) }) }
            }
        }
        viewModelScope.launch {
            dao.history()
                .map { rows -> rows.mapNotNull(::toProgress) }
                .flowOn(Dispatchers.Default)
                .collect { history -> _state.update { it.copy(history = history) } }
        }
    }

    fun activate(languageCode: String) {
        val scope = repository.contentScope(languageCode)
        val current = _state.value
        if (current.languageCode == languageCode && current.contentScope == scope && (current.discover.isNotEmpty() || discoverJob?.isActive == true)) return
        _state.update { it.copy(languageCode = languageCode, contentScope = scope, discover = if (it.contentScope == scope) it.discover else emptyList()) }
        loadDiscover()
    }

    fun retryDiscover() = loadDiscover()

    private fun loadDiscover() {
        val languageCode = _state.value.languageCode
        discoverJob?.cancel()
        _state.update { it.copy(discoverLoading = true, discoverFailed = false) }
        discoverJob = viewModelScope.launch {
            val shelves = guarded("discover") { repository.shelves(languageCode) }
            _state.update { it.copy(discover = shelves ?: it.discover, discoverLoading = false, discoverFailed = shelves == null) }
        }
    }

    fun setQuery(value: String) {
        val clean = value.take(MAX_QUERY_LENGTH)
        searchJob?.cancel()
        val searchable = clean.trim().length >= 2
        _state.update { it.copy(query = clean, searching = searchable, searchFailed = false, results = if (searchable) it.results else emptyList()) }
        if (!searchable) return
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val results = guarded("search") { repository.search(clean, _state.value.languageCode) }
            _state.update { if (it.query != clean) it else it.copy(results = results.orEmpty(), searching = false, searchFailed = results == null) }
        }
    }

    fun openShow(show: PodcastShow) {
        showJob?.cancel()
        _state.update { it.copy(openShow = PodcastShowState(show)) }
        showJob = viewModelScope.launch {
            val page = guarded("show") { repository.show(show.browseId, _state.value.languageCode) }
            _state.update { state ->
                val open = state.openShow?.takeIf { it.show.browseId == show.browseId } ?: return@update state
                state.copy(openShow = open.copy(page = page, loading = false, failed = page == null, show = page?.show ?: open.show))
            }
        }
    }

    fun retryShow() = _state.value.openShow?.show?.let(::openShow)

    fun closeShow() {
        showJob?.cancel()
        _state.update { it.copy(openShow = null) }
    }

    fun loadMoreEpisodes() {
        val open = _state.value.openShow ?: return
        val page = open.page ?: return
        val continuation = page.episodes.continuation ?: return
        if (open.loadingMore) return
        _state.update { it.copy(openShow = open.copy(loadingMore = true)) }
        showJob = viewModelScope.launch {
            val more = guarded("episodes") { repository.moreEpisodes(continuation, _state.value.languageCode) }
            _state.update { state ->
                val current = state.openShow?.takeIf { it.show.browseId == open.show.browseId } ?: return@update state
                val currentPage = current.page ?: return@update state
                val merged = if (more == null) currentPage else currentPage.copy(
                    episodes = currentPage.episodes.copy(
                        episodes = (currentPage.episodes.episodes + more.episodes.map { it.copy(artist = current.show.title.ifBlank { it.artist }) })
                            .distinctBy(Track::id),
                        continuation = more.continuation
                    )
                )
                state.copy(openShow = current.copy(page = merged, loadingMore = false))
            }
        }
    }

    fun toggleSubscription(show: PodcastShow) {
        val subscribed = show.browseId in _state.value.subscribedIds
        launchWrite("subscription") {
            if (subscribed) dao.unsubscribe(show.browseId)
            else dao.subscribe(PodcastSubscriptionEntity(show.browseId, show.title, show.author, show.thumbnailUrl, System.currentTimeMillis()))
        }
    }

    fun setPlayed(episode: Track, played: Boolean) = launchWrite("played") {
        if (_state.value.progressById.containsKey(episode.id)) {
            dao.setCompleted(episode.id, played)
        } else if (played) {
            dao.saveProgress(PodcastEpisodeProgressEntity(episode.id, TrackPayloadCodec.encode(episode.stablePodcastEpisode()), 0L, 0L, true, System.currentTimeMillis()))
        }
    }

    fun removeFromHistory(episodeId: String) = launchWrite("remove") { dao.removeProgress(episodeId) }

    fun clearHistory() = launchWrite("clear") { dao.clearHistory() }

    private fun launchWrite(label: String, block: suspend () -> Unit) {
        viewModelScope.launch { guarded(label) { block() } }
    }

    private suspend fun <T> guarded(label: String, block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.w(error, "Podcast %s failed", label)
        null
    }

    private fun toProgress(row: PodcastEpisodeProgressEntity): PodcastEpisodeProgress? =
        TrackPayloadCodec.decode(row.payload)?.let { PodcastEpisodeProgress(it, row.positionMs, row.durationMs, row.completed) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return PodcastLibraryViewModel(
                    YoutubePodcastsRepository(appContext),
                    LevyraDatabase.get(appContext).podcastLibraryDao()
                ) as T
            }
        }
    }
}

internal fun Track.stablePodcastEpisode(): Track = copy(streamUrl = "", videoStreamUrl = "")
