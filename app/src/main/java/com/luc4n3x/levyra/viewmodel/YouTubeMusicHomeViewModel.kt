package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeChip
import com.luc4n3x.levyra.domain.YoutubeMusicHomeFeed
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class YouTubeMusicHomeUiState(
    val language: String = "",
    val browseId: String = HOME_BROWSE_ID,
    val chips: List<YoutubeMusicHomeChip> = emptyList(),
    val selectedChip: String = "",
    val shelves: List<YoutubeMusicHomeShelf> = emptyList(),
    val discovery: List<YoutubeMusicHomeShelf> = emptyList(),
    val continuation: String = "",
    val pages: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val failed: Boolean = false
) {
    val allShelves: List<YoutubeMusicHomeShelf>
        get() {
            val known = shelves.mapTo(HashSet()) { it.title }
            return shelves + discovery.filter { it.title !in known }
        }

    val canLoadMore: Boolean get() = continuation.isNotBlank() && pages < MAX_HOME_PAGES && !loading && !loadingMore
}

private const val MAX_HOME_PAGES = 6
internal const val HOME_BROWSE_ID = "FEmusic_home"
internal const val EXPLORE_BROWSE_ID = "FEmusic_explore"
internal const val CHARTS_BROWSE_ID = "FEmusic_charts"
internal const val NEW_VIDEOS_BROWSE_ID = "FEmusic_new_releases_videos"
private const val PREVIEW_TRACK_LIMIT = 25
private const val MAX_CACHED_PREVIEWS = 24

class YouTubeMusicHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository(application)
    private val account = YoutubeMusicAccountRepository(application)
    private val _state = MutableStateFlow(YouTubeMusicHomeUiState())
    val state: StateFlow<YouTubeMusicHomeUiState> = _state.asStateFlow()
    private val _previews = MutableStateFlow<Map<String, List<Track>>>(emptyMap())
    val previews: StateFlow<Map<String, List<Track>>> = _previews.asStateFlow()
    private val previewRequests = HashSet<String>()
    private var job: Job? = null
    private var generation = 0L

    fun load(language: String, force: Boolean = false, browseId: String = HOME_BROWSE_ID) {
        val current = _state.value
        val same = current.language == language && current.browseId == browseId
        if (!force && same && (current.loading || current.shelves.isNotEmpty())) return
        fetch(language, current.selectedChip.takeIf { same }.orEmpty(), keepChips = same, browseId = browseId)
    }

    fun requestPreview(playlistId: String) {
        if (playlistId.isBlank() || _previews.value.containsKey(playlistId) || !previewRequests.add(playlistId)) return
        val language = _state.value.language
        viewModelScope.launch {
            val tracks = try {
                repository.playlist(playlistId, language, PREVIEW_TRACK_LIMIT)?.tracks.orEmpty()
            } catch (error: CancellationException) {
                previewRequests.remove(playlistId)
                throw error
            } catch (error: Exception) {
                Timber.w(error, "YouTube Music style playlist preview failed")
                previewRequests.remove(playlistId)
                return@launch
            }
            _previews.update { current ->
                val next = current + (playlistId to tracks)
                if (next.size <= MAX_CACHED_PREVIEWS) next else next.entries.drop(next.size - MAX_CACHED_PREVIEWS).associate { it.key to it.value }
            }
            previewRequests.remove(playlistId)
        }
    }

    fun selectChip(params: String) {
        val current = _state.value
        val next = if (current.selectedChip == params) "" else params
        fetch(current.language, next, keepChips = true, browseId = current.browseId)
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        val cursor = current.continuation
        val requestGeneration = generation
        _state.update { it.copy(loadingMore = true) }
        job = viewModelScope.launch {
            val page = request(current.language, current.selectedChip, cursor, current.browseId)
            _state.update { state ->
                if (generation != requestGeneration) return@update state
                val known = state.shelves.mapTo(HashSet()) { it.title }
                state.copy(
                    shelves = state.shelves + page.shelves.filter { it.title !in known },
                    continuation = page.continuation.takeUnless { it == cursor }.orEmpty(),
                    pages = state.pages + 1,
                    loadingMore = false
                )
            }
        }
    }

    private fun fetch(language: String, chip: String, keepChips: Boolean, browseId: String) {
        job?.cancel()
        val requestGeneration = ++generation
        _state.update { state ->
            YouTubeMusicHomeUiState(
                language = language,
                browseId = browseId,
                chips = if (keepChips) state.chips else emptyList(),
                selectedChip = chip,
                shelves = if (keepChips && state.selectedChip == chip) state.shelves else emptyList(),
                discovery = if (keepChips && state.selectedChip == chip) state.discovery else emptyList(),
                loading = true
            )
        }
        job = viewModelScope.launch {
            val feed = request(language, chip, "", browseId)
            _state.update { state ->
                if (generation != requestGeneration) return@update state
                state.copy(
                    chips = feed.chips.ifEmpty { state.chips },
                    shelves = feed.shelves.ifEmpty { state.shelves },
                    continuation = feed.continuation,
                    pages = 1,
                    loading = false,
                    failed = feed.shelves.isEmpty() && state.shelves.isEmpty()
                )
            }
            if (chip.isBlank()) discoverySource(browseId)?.let { source -> appendDiscovery(language, requestGeneration, source) }
        }
    }

    private fun discoverySource(browseId: String): String? = when (browseId) {
        HOME_BROWSE_ID -> EXPLORE_BROWSE_ID
        EXPLORE_BROWSE_ID -> CHARTS_BROWSE_ID
        else -> null
    }

    private suspend fun appendDiscovery(language: String, requestGeneration: Long, source: String) {
        val discovery = try {
            repository.youtubeMusicHomeFeed(language, browseId = source)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "YouTube Music style discovery shelves failed")
            return
        }
        _state.update { state ->
            if (generation != requestGeneration) return@update state
            state.copy(discovery = discovery.shelves.map { it.copy(key = "discovery-${it.key}") })
        }
    }

    private suspend fun request(language: String, params: String, continuation: String, browseId: String): YoutubeMusicHomeFeed {
        val session = withContext(Dispatchers.IO) { runCatching { account.sessions.load().active }.getOrNull() }
        if (session != null) {
            try {
                return account.homeFeed(session, language, params, continuation, browseId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Account home refresh failed; using the public home")
            }
        }
        return try {
            repository.youtubeMusicHomeFeed(language, params, continuation, browseId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "YouTube Music style home refresh failed")
            YoutubeMusicHomeFeed()
        }
    }
}
