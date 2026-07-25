package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.model.CatalogPage
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
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

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var collectionJob: Job? = null

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
            val suggestions = catalog.suggestions(value)
            if (searchState.value.query == value) {
                searchState.value = searchState.value.copy(suggestions = suggestions)
            }
        }
    }

    fun submit(query: String = searchState.value.query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        suggestionJob?.cancel()
        searchJob?.cancel()
        libraryStore.recordSearch(trimmed)
        searchState.value = searchState.value.copy(
            query = trimmed,
            submittedQuery = trimmed,
            suggestions = emptyList(),
            loading = true,
            error = "",
            page = CatalogPage()
        )
        searchJob = scope.launch {
            runCatching { catalog.search(trimmed, searchState.value.filter) }
                .onSuccess { page ->
                    searchState.value = searchState.value.copy(page = page, loading = false)
                }
                .onFailure { error ->
                    searchState.value = searchState.value.copy(
                        loading = false,
                        error = error.message ?: "Ricerca non riuscita"
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
        scope.launch {
            runCatching { catalog.searchMore(current.submittedQuery, current.filter, cursor) }
                .onSuccess { page ->
                    searchState.value = searchState.value.copy(
                        page = searchState.value.page.mergedWith(page),
                        loadingMore = false
                    )
                }
                .onFailure { error ->
                    searchState.value = searchState.value.copy(
                        loadingMore = false,
                        error = error.message ?: "Caricamento non riuscito"
                    )
                }
        }
    }

    fun clearSearch() {
        suggestionJob?.cancel()
        searchJob?.cancel()
        searchState.value = SearchUiState(filter = searchState.value.filter)
    }

    fun openCollection(ref: CollectionRef) {
        collectionJob?.cancel()
        collectionState.value = CollectionUiState(ref = ref, loading = true)
        collectionJob = scope.launch {
            runCatching { catalog.collection(ref) }
                .onSuccess { detail ->
                    collectionState.value = CollectionUiState(ref = detail.ref, page = detail.page)
                }
                .onFailure { error ->
                    collectionState.value = CollectionUiState(
                        ref = ref,
                        error = error.message ?: "Contenuto non disponibile"
                    )
                }
        }
    }

    fun openCollectionFromUrl(url: String, onOpened: () -> Unit) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        collectionJob?.cancel()
        collectionState.value = CollectionUiState(loading = true)
        onOpened()
        collectionJob = scope.launch {
            runCatching { catalog.collectionFromUrl(trimmed) }
                .onSuccess { detail ->
                    collectionState.value = CollectionUiState(ref = detail.ref, page = detail.page)
                }
                .onFailure { error ->
                    collectionState.value = CollectionUiState(
                        error = error.message ?: "URL non valido"
                    )
                }
        }
    }

    fun loadMoreCollection() {
        val current = collectionState.value
        val ref = current.ref ?: return
        val cursor = current.page.cursor ?: return
        if (current.loadingMore || current.loading) return
        collectionState.value = current.copy(loadingMore = true)
        scope.launch {
            runCatching { catalog.collectionMore(ref, cursor) }
                .onSuccess { page ->
                    collectionState.value = collectionState.value.copy(
                        page = collectionState.value.page.mergedWith(page),
                        loadingMore = false
                    )
                }
                .onFailure { error ->
                    collectionState.value = collectionState.value.copy(
                        loadingMore = false,
                        error = error.message ?: "Caricamento non riuscito"
                    )
                }
        }
    }

    private companion object {
        const val SUGGESTION_DEBOUNCE_MS = 260L
    }
}
