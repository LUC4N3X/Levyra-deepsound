package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeChip
import com.luc4n3x.levyra.domain.YoutubeMusicHomeFeed
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import com.luc4n3x.levyra.data.toHit
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
private const val CURATED_MOOD_SHELVES = 6
private const val CURATED_MOOD_GROUP_QUOTA = 3
private const val CURATED_MOOD_PLAYLISTS = 10
private const val CURATED_MOOD_FETCH_LIMIT = 40

internal fun featuredPlaylistKey(playlistId: String, language: String): String = "$language|$playlistId"

internal object CuratedFeaturedPlaylist {
    const val YOUTUBE_ID = "RDTMAK5uy_lr0LWzGrq6FU9GIxWvFHTRPQD2LHMqlFA"
    const val SPOTIFY_ID = "37i9dQZF1DX4JAvHpjipBk"
}

class YouTubeMusicHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository(application)
    private val account = YoutubeMusicAccountRepository(application)
    private val _state = MutableStateFlow(YouTubeMusicHomeUiState())
    val state: StateFlow<YouTubeMusicHomeUiState> = _state.asStateFlow()
    private val _previews = MutableStateFlow<Map<String, List<Track>>>(emptyMap())
    val previews: StateFlow<Map<String, List<Track>>> = _previews.asStateFlow()
    private val previewRequests = HashSet<String>()
    private val _featuredPlaylists = MutableStateFlow<Map<String, YoutubeMusicHomeItem>>(emptyMap())
    val featuredPlaylists: StateFlow<Map<String, YoutubeMusicHomeItem>> = _featuredPlaylists.asStateFlow()
    private val featuredRequests = HashSet<String>()
    private var job: Job? = null
    private var generation = 0L
    private val _moodShelves = MutableStateFlow<Map<String, List<YoutubeMusicHomeShelf>>>(emptyMap())
    val moodShelves: StateFlow<Map<String, List<YoutubeMusicHomeShelf>>> = _moodShelves.asStateFlow()
    private val moodLoaded = HashSet<String>()
    private var moodJob: Job? = null
    private var moodLanguage = ""

    fun load(language: String, force: Boolean = false, browseId: String = HOME_BROWSE_ID) {
        val current = _state.value
        val same = current.language == language && current.browseId == browseId
        if (!force && same && (current.loading || current.shelves.isNotEmpty())) return
        fetch(language, current.selectedChip.takeIf { same }.orEmpty(), keepChips = same, browseId = browseId)
    }

    fun requestFeaturedPlaylist(playlistId: String, language: String) {
        val cacheKey = featuredPlaylistKey(playlistId, language)
        if (playlistId.isBlank() || _featuredPlaylists.value.containsKey(cacheKey) || !featuredRequests.add(cacheKey)) return
        viewModelScope.launch {
            val detail = try {
                repository.playlist(playlistId, language, 1)
            } catch (error: CancellationException) {
                featuredRequests.remove(cacheKey)
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Featured curated playlist lookup failed")
                null
            }
            featuredRequests.remove(cacheKey)
            if (detail == null || detail.title.isBlank()) return@launch
            val hit = PlaylistHit(title = detail.title, author = detail.author, thumbnailUrl = detail.thumbnailUrl, playlistId = playlistId)
            val item = YoutubeMusicHomeItem(
                key = "featured-$playlistId",
                kind = YoutubeMusicHomeItemKind.Playlist,
                title = detail.title,
                subtitle = detail.author,
                thumbnailUrl = detail.thumbnailUrl,
                playlist = hit
            )
            _featuredPlaylists.update { it + (cacheKey to item) }
        }
    }

    fun requestMoodShelves(language: String) {
        if (language.isBlank() || language in moodLoaded || (moodLanguage == language && moodJob?.isActive == true)) return
        moodJob?.cancel()
        moodLanguage = language
        val cached = language in _moodShelves.value
        moodJob = viewModelScope.launch {
            val categories = try {
                repository.moodCategories(language)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Curated moods and genres lookup failed")
                return@launch
            }
            val selected = curatedMoodCategories(categories)
            val previous = _moodShelves.value[language].orEmpty().associateBy { it.key }
            val shelves = mutableListOf<YoutubeMusicHomeShelf>()
            for (category in selected) {
                previous[curatedMoodShelfKey(category)]?.let { shelves += it; continue }
                val playlists = try {
                    repository.moodPlaylists(category.params, language, CURATED_MOOD_FETCH_LIMIT)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Timber.w(error, "Curated mood shelf lookup failed")
                    continue
                }
                val shelf = curatedMoodShelf(category, playlists) ?: continue
                shelves += shelf
                if (!cached) _moodShelves.update { it + (language to shelves.toList()) }
            }
            if (shelves.isNotEmpty()) _moodShelves.update { it + (language to shelves.toList()) }
            if (selected.isNotEmpty() && shelves.size == selected.size) moodLoaded += language
        }
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

internal fun curatedMoodCategories(categories: List<ExploreCategory>): List<ExploreCategory> {
    val (allMoods, allGenres) = categories.partition { it.sectionIndex == 0 }
    val moodQuota = minOf(allMoods.size, CURATED_MOOD_GROUP_QUOTA)
    val genreQuota = minOf(allGenres.size, CURATED_MOOD_GROUP_QUOTA)
    val moods = evenlySpaced(allMoods, minOf(allMoods.size, moodQuota + CURATED_MOOD_GROUP_QUOTA - genreQuota))
    val genres = evenlySpaced(allGenres, minOf(allGenres.size, genreQuota + CURATED_MOOD_GROUP_QUOTA - moodQuota))
    return (0 until maxOf(moods.size, genres.size))
        .flatMap { index -> listOfNotNull(moods.getOrNull(index), genres.getOrNull(index)) }
        .take(CURATED_MOOD_SHELVES)
}

private fun <T> evenlySpaced(list: List<T>, count: Int): List<T> =
    if (list.size > count) List(count) { list[it * list.size / count] } else list

private fun curatedMoodShelfKey(category: ExploreCategory): String = "mood-${category.params}"

internal fun curatedMoodShelf(category: ExploreCategory, playlists: List<YoutubeMusicMoodPlaylist>): YoutubeMusicHomeShelf? {
    val items = playlists.filter { it.playlistId.isNotBlank() }.map { playlist ->
        YoutubeMusicHomeItem(
            key = "mood-${playlist.playlistId}",
            kind = YoutubeMusicHomeItemKind.Playlist,
            title = playlist.title,
            subtitle = playlist.description,
            thumbnailUrl = playlist.thumbnailUrl,
            playlist = playlist.toHit()
        )
    }
    if (items.isEmpty()) return null
    return YoutubeMusicHomeShelf(
        key = curatedMoodShelfKey(category),
        title = category.title,
        strapline = "",
        thumbnailUrl = "",
        style = YoutubeMusicHomeShelfStyle.Cards,
        items = items
    )
}

internal fun withCuratedMoodShelves(curated: List<YoutubeMusicHomeShelf>, moods: List<YoutubeMusicHomeShelf>): List<YoutubeMusicHomeShelf> {
    if (moods.isEmpty()) return curated
    val seen = curated.flatMapTo(HashSet()) { shelf -> shelf.items.mapNotNull { it.playlist?.playlistId?.takeIf(String::isNotBlank) } }
    val keys = curated.mapTo(HashSet()) { it.key }
    return curated + moods.filter { keys.add(it.key) }.mapNotNull { shelf ->
        val items = shelf.items.asSequence()
            .filter { item -> item.kind == YoutubeMusicHomeItemKind.Playlist && item.playlist?.playlistId?.takeIf(String::isNotBlank)?.let(seen::add) == true }
            .take(CURATED_MOOD_PLAYLISTS)
            .toList()
        if (items.isEmpty()) null else shelf.copy(items = items)
    }
}
