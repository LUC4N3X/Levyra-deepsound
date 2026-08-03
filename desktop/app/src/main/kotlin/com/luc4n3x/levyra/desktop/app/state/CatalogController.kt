package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.model.ArtistDetail
import com.luc4n3x.levyra.desktop.core.model.CatalogPage
import com.luc4n3x.levyra.desktop.core.model.CollectionDetail
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val filter: SearchFilter = SearchFilter.SONGS,
    val page: CatalogPage = CatalogPage(),
    val suggestions: List<String> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String = ""
) {
    val hasResults: Boolean get() = !page.isEmpty
    val canLoadMore: Boolean get() = page.cursor != null && !loadingMore && !loading
}

data class CollectionUiState(
    val ref: CollectionRef? = null,
    val page: CatalogPage = CatalogPage(),
    val artist: ArtistDetail? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String = ""
) {
    val canLoadMore: Boolean get() = page.cursor != null && !loadingMore && !loading
}

class CatalogController(
    private val scope: CoroutineScope,
    private val catalog: CatalogRepository,
    private val libraryStore: LibraryStore
) {
    private val searchState = MutableStateFlow(SearchUiState())
    private val collectionState = MutableStateFlow(CollectionUiState())
    private val collectionHistory = ArrayDeque<CollectionUiState>()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var searchMoreJob: Job? = null
    private var collectionJob: Job? = null
    private var collectionMoreJob: Job? = null

    val search: StateFlow<SearchUiState> = searchState.asStateFlow()
    val collection: StateFlow<CollectionUiState> = collectionState.asStateFlow()

    fun onQueryChange(value: String) {
        searchState.value = searchState.value.copy(query = value)
        suggestionJob?.cancel()
        if (value.isBlank()) {
            searchState.value = searchState.value.copy(suggestions = emptyList())
            return
        }
        suggestionJob = scope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            val suggestions = runCatchingCancellable { catalog.suggestions(value) }.getOrDefault(emptyList())
            if (searchState.value.query == value) {
                searchState.value = searchState.value.copy(suggestions = suggestions)
            }
        }
    }

    fun submit(query: String = searchState.value.query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        suggestionJob?.cancel()
        cancelSearchJobs()
        libraryStore.recordSearch(trimmed)
        val filter = searchState.value.filter
        searchState.value = searchState.value.copy(
            query = trimmed,
            submittedQuery = trimmed,
            suggestions = emptyList(),
            loading = true,
            error = "",
            page = CatalogPage()
        )
        searchJob = scope.launch {
            try {
                val page = catalog.search(trimmed, filter)
                searchState.value = searchState.value.copy(page = page, loading = false, error = "")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                searchState.value = searchState.value.copy(
                    loading = false,
                    error = error.message ?: SEARCH_ERROR
                )
            }
        }
    }

    fun setFilter(filter: SearchFilter) {
        if (filter == searchState.value.filter) return
        searchState.value = searchState.value.copy(filter = filter)
        if (searchState.value.submittedQuery.isNotBlank()) {
            submit(searchState.value.submittedQuery)
        }
    }

    fun loadMoreSearch() {
        val current = searchState.value
        val cursor = current.page.cursor ?: return
        if (current.loadingMore || current.loading) return
        searchState.value = current.copy(loadingMore = true)
        searchMoreJob = scope.launch {
            try {
                val page = catalog.searchMore(current.submittedQuery, current.filter, cursor)
                searchState.value = searchState.value.copy(
                    page = searchState.value.page.mergedWith(page),
                    loadingMore = false
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                searchState.value = searchState.value.copy(
                    loadingMore = false,
                    error = error.message ?: LOAD_ERROR
                )
            }
        }
    }

    fun clearSearch() {
        suggestionJob?.cancel()
        cancelSearchJobs()
        searchState.value = SearchUiState(filter = searchState.value.filter)
    }

    fun pushCollectionHistory() {
        if (collectionState.value.ref != null) {
            collectionHistory.addLast(collectionState.value)
        }
    }

    fun clearCollectionHistory() {
        collectionHistory.clear()
    }

    fun back(): Boolean {
        cancelCollectionJobs()
        val previous = collectionHistory.removeLastOrNull()
        if (previous != null) {
            collectionState.value = previous
            return true
        }
        return false
    }

    fun openCollection(ref: CollectionRef) {
        cancelCollectionJobs()
        collectionState.value = CollectionUiState(ref = ref, loading = true)
        collectionJob = scope.launch { loadCollection { catalog.collection(ref) } }
    }

    fun openCollectionFromUrl(url: String, onOpened: () -> Unit) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        cancelCollectionJobs()
        collectionState.value = CollectionUiState(loading = true)
        onOpened()
        collectionJob = scope.launch {
            loadCollection { catalog.collectionFromUrl(trimmed) }
        }
    }

    fun loadMoreCollection() {
        val current = collectionState.value
        val ref = current.ref ?: return
        val cursor = current.page.cursor ?: return
        if (current.loadingMore || current.loading) return
        collectionState.value = current.copy(loadingMore = true)
        collectionMoreJob = scope.launch {
            try {
                val page = catalog.collectionMore(ref, cursor)
                if (collectionState.value.ref?.url == ref.url) {
                    collectionState.value = collectionState.value.copy(
                        page = collectionState.value.page.mergedWith(page),
                        loadingMore = false
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (collectionState.value.ref?.url == ref.url) {
                    collectionState.value = collectionState.value.copy(
                        loadingMore = false,
                        error = error.message ?: LOAD_ERROR
                    )
                }
            }
        }
    }

    private suspend fun loadCollection(load: suspend () -> CollectionDetail) {
        try {
            val detail = load()
            collectionState.value = CollectionUiState(
                ref = detail.ref,
                page = detail.page,
                artist = detail.artist
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            collectionState.value = collectionState.value.copy(
                loading = false,
                error = error.message ?: COLLECTION_ERROR
            )
        }
    }

    private fun cancelSearchJobs() {
        searchJob?.cancel()
        searchMoreJob?.cancel()
    }

    private fun cancelCollectionJobs() {
        collectionJob?.cancel()
        collectionMoreJob?.cancel()
    }

    private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        const val SUGGESTION_DEBOUNCE_MS = 260L
        const val SEARCH_ERROR = "Ricerca non riuscita"
        const val LOAD_ERROR = "Caricamento non riuscito"
        const val COLLECTION_ERROR = "Contenuto non disponibile"
    }
}
