package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.apple.AppleCatalogFreshness
import com.luc4n3x.levyra.data.apple.AppleCatalogIds
import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogPage
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
    val collection: AppleMusicCollectionUiState? = null
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

    fun load(storefront: String, language: String, force: Boolean = false) {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val current = _state.value
        if (!force && current.storefront == front && current.language == lang && (current.loading || current.newMusic != null)) return
        pageJob?.cancel()
        _state.update {
            if (it.storefront == front && it.language == lang) it.copy(loading = true, failed = false)
            else AppleMusicEditorialUiState(storefront = front, language = lang, loading = true, collection = it.collection)
        }
        pageJob = viewModelScope.launch {
            val cachedNew = repository.cachedNewMusic(front, lang)
            val cachedCharts = repository.cachedTopCharts(front, lang)
            if (cachedNew != null || cachedCharts != null) {
                _state.update { state ->
                    if (state.storefront != front || state.language != lang) state
                    else state.copy(
                        newMusic = cachedNew?.page ?: state.newMusic,
                        charts = cachedCharts?.page ?: state.charts,
                        stale = cachedNew?.freshness == AppleCatalogFreshness.Stale
                    )
                }
            }
            val fresh = repository.newMusic(front, lang)
            val charts = repository.topCharts(front, lang)
            _state.update { state ->
                if (state.storefront != front || state.language != lang) state
                else state.copy(
                    newMusic = fresh?.page ?: state.newMusic,
                    charts = charts?.page ?: state.charts,
                    stale = fresh?.freshness == AppleCatalogFreshness.Stale || (fresh == null && state.newMusic != null),
                    loading = false,
                    failed = fresh == null && state.newMusic == null
                )
            }
        }
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
