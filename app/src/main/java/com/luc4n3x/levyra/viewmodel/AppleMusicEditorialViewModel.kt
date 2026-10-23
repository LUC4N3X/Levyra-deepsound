package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.apple.AppleArtistPage
import com.luc4n3x.levyra.data.apple.AppleCatalogFreshness
import com.luc4n3x.levyra.data.apple.AppleCatalogIds
import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogPage
import com.luc4n3x.levyra.data.apple.AppleCatalogResult
import com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository
import com.luc4n3x.levyra.data.apple.AppleStorefronts
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AppleMusicEditorialUiState(
    val storefront: String = "",
    val language: String = "",
    val newMusic: AppleCatalogPage? = null,
    val charts: AppleCatalogPage? = null,
    val stale: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val collection: AppleMusicCollectionUiState? = null,
    val artistKey: String = "",
    val artist: AppleArtistPage? = null
)

@Immutable
data class AppleMusicCollectionUiState(
    val id: String,
    val title: String,
    val curator: String,
    val description: String = "",
    val artworkUrl: String = "",
    val backgroundColor: Int? = null,
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val fetchedAtMs: Long = 0L
)

class AppleMusicEditorialViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppleMusicEditorialRepository.get(application)
    private val _state = MutableStateFlow(AppleMusicEditorialUiState())
    val state: StateFlow<AppleMusicEditorialUiState> = _state.asStateFlow()
    private var pageJob: Job? = null
    private var collectionJob: Job? = null
    private var artistJob: Job? = null

    suspend fun prefetchArtistHeader(artistName: String, storefront: String, language: String): AppleArtistPage? =
        repository.artistHeader(storefront, artistName, language)

    fun loadArtist(artistName: String, storefront: String, language: String) {
        val key = appleArtistKey(artistName, storefront, language)
        if (key.isBlank() || _state.value.artistKey == key) return
        artistJob?.cancel()
        _state.update { it.copy(artistKey = key, artist = null) }
        artistJob = viewModelScope.launch {
            val header = repository.artistHeader(storefront, artistName, language)
            if (header == null) {
                _state.update { state -> if (state.artistKey == key) state.copy(artistKey = "") else state }
                return@launch
            }
            _state.update { state -> if (state.artistKey == key && state.artist == null) state.copy(artist = header) else state }
            val page = repository.artist(storefront, artistName, language) ?: return@launch
            _state.update { state -> if (state.artistKey == key) state.copy(artist = page) else state }
        }
    }

    fun load(storefront: String, language: String, force: Boolean = false) {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        if (!force && _state.value.isLoadedFor(front, lang)) return
        pageJob?.cancel()
        _state.update { it.startLoading(front, lang) }
        pageJob = viewModelScope.launch {
            val cachedNew = repository.cachedNewMusic(front, lang)
            val cachedCharts = repository.cachedTopCharts(front, lang)
            updateFor(front, lang) { it.withCached(cachedNew, cachedCharts) }
            val fresh = repository.newMusic(front, lang)
            val charts = repository.topCharts(front, lang)
            updateFor(front, lang) { it.withFresh(fresh, charts) }
        }
    }

    private fun updateFor(front: String, lang: String, transform: (AppleMusicEditorialUiState) -> AppleMusicEditorialUiState) {
        _state.update { state -> if (state.storefront == front && state.language == lang) transform(state) else state }
    }
    fun openCollection(item: AppleCatalogItem) {
        val id = AppleCatalogIds.playlist(item.id) ?: AppleCatalogIds.playlistIdFromUrl(item.url) ?: return
        val front = _state.value.storefront.ifBlank { "us" }
        val lang = _state.value.language.ifBlank { "en" }
        collectionJob?.cancel()
        _state.update {
            it.copy(
                collection = AppleMusicCollectionUiState(
                    id = id,
                    title = item.title,
                    curator = item.subtitle,
                    description = item.description,
                    artworkUrl = item.artworkUrl(COLLECTION_ARTWORK_SIZE),
                    backgroundColor = item.backgroundColor
                )
            )
        }
        collectionJob = viewModelScope.launch {
            val playlist = repository.playlist(front, id, lang)
            _state.update { state ->
                val open = state.collection?.takeIf { it.id == id } ?: return@update state
                state.copy(
                    collection = if (playlist == null) open.copy(loading = false, failed = true)
                    else open.copy(
                        title = playlist.title.ifBlank { open.title },
                        curator = playlist.curator.ifBlank { open.curator },
                        description = playlist.description.ifBlank { open.description },
                        artworkUrl = playlist.artworkUrl.ifBlank { open.artworkUrl },
                        tracks = playlist.tracks,
                        loading = false,
                        failed = false,
                        fetchedAtMs = playlist.fetchedAtMs
                    )
                )
            }
        }
    }

    fun retryCollection() {
        val open = _state.value.collection ?: return
        openCollection(AppleCatalogItem(open.id, com.luc4n3x.levyra.data.apple.AppleCatalogItemKind.Playlist, open.title, open.curator, description = open.description, backgroundColor = open.backgroundColor))
    }

    fun closeCollection() {
        collectionJob?.cancel()
        _state.update { it.copy(collection = null) }
    }

    private companion object {
        const val COLLECTION_ARTWORK_SIZE = 600
    }
}

internal fun appleArtistKey(artistName: String, storefront: String, language: String): String {
    val name = artistName.trim().lowercase(java.util.Locale.ROOT)
    if (name.length < 2) return ""
    return "${AppleStorefronts.normalize(storefront)}:${AppleStorefronts.language(language)}:$name"
}

private fun AppleMusicEditorialUiState.isLoadedFor(front: String, lang: String): Boolean =
    storefront == front && language == lang && (loading || newMusic != null)

private fun AppleMusicEditorialUiState.startLoading(front: String, lang: String): AppleMusicEditorialUiState =
    if (storefront == front && language == lang) copy(loading = true, failed = false)
    else AppleMusicEditorialUiState(storefront = front, language = lang, loading = true, collection = collection, artistKey = artistKey, artist = artist)

private fun AppleMusicEditorialUiState.withCached(news: AppleCatalogResult?, charts: AppleCatalogResult?): AppleMusicEditorialUiState {
    if (news == null && charts == null) return this
    return copy(
        newMusic = news?.page ?: newMusic,
        charts = charts?.page ?: this.charts,
        stale = news?.freshness == AppleCatalogFreshness.Stale
    )
}

private fun AppleMusicEditorialUiState.withFresh(news: AppleCatalogResult?, charts: AppleCatalogResult?): AppleMusicEditorialUiState = copy(
    newMusic = news?.page ?: newMusic,
    charts = charts?.page ?: this.charts,
    stale = news?.freshness == AppleCatalogFreshness.Stale || (news == null && newMusic != null),
    loading = false,
    failed = news == null && newMusic == null
)
