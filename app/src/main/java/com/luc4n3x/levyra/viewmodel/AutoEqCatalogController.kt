package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.AutoEqCatalogSource
import com.luc4n3x.levyra.domain.AutoEqCatalog
import com.luc4n3x.levyra.domain.AutoEqCatalogEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class AutoEqCatalogStatus { LOADING, READY, UNAVAILABLE }

data class AutoEqCatalogSelection(val name: String, val detail: String, val profileText: String)

data class AutoEqCatalogUiState(
    val visible: Boolean = false,
    val status: AutoEqCatalogStatus = AutoEqCatalogStatus.LOADING,
    val query: String = "",
    val resultsQuery: String? = null,
    val results: List<AutoEqCatalogEntry> = emptyList(),
    val loadingKey: String? = null,
    val failedKey: String? = null,
    val selection: AutoEqCatalogSelection? = null
)

internal class AutoEqCatalogController(
    private val scope: CoroutineScope,
    private val source: AutoEqCatalogSource,
    private val searchDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS
) {
    private val mutableState = MutableStateFlow(AutoEqCatalogUiState())
    val state: StateFlow<AutoEqCatalogUiState> = mutableState.asStateFlow()

    private var catalog: AutoEqCatalog? = null
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var profileJob: Job? = null

    fun open() {
        val current = mutableState.value
        if (current.visible && current.status != AutoEqCatalogStatus.UNAVAILABLE) return
        cancelWork()
        mutableState.value = AutoEqCatalogUiState(visible = true, query = current.query)
        loadJob = scope.launch {
            val loaded = guarded("catalog") { source.loadCatalog() }
            if (!mutableState.value.visible) return@launch
            catalog = loaded
            mutableState.update {
                it.copy(status = if (loaded == null) AutoEqCatalogStatus.UNAVAILABLE else AutoEqCatalogStatus.READY)
            }
            if (loaded != null) scheduleSearch(mutableState.value.query, debounce = false)
        }
    }

    fun updateQuery(query: String) {
        val bounded = query.take(AutoEqCatalog.MAX_QUERY_CHARS)
        if (bounded == mutableState.value.query) return
        mutableState.update { it.copy(query = bounded, failedKey = null) }
        scheduleSearch(bounded, debounce = true)
    }

    fun select(entry: AutoEqCatalogEntry) {
        val current = mutableState.value
        if (!current.visible || current.loadingKey == entry.key) return
        profileJob?.cancel()
        mutableState.update { it.copy(loadingKey = entry.key, failedKey = null) }
        profileJob = scope.launch {
            val text = guarded("profile") { source.loadProfile(entry) }
            mutableState.update { latest ->
                when {
                    !latest.visible || latest.loadingKey != entry.key -> latest
                    text == null -> latest.copy(loadingKey = null, failedKey = entry.key)
                    else -> latest.copy(
                        loadingKey = null,
                        selection = AutoEqCatalogSelection(entry.name, "${entry.source} · ${entry.variant}", text)
                    )
                }
            }
        }
    }

    fun dismissSelection() {
        mutableState.update { it.copy(selection = null) }
    }

    fun close() {
        if (!mutableState.value.visible && catalog == null) return
        cancelWork()
        catalog = null
        source.releaseMemory()
        mutableState.value = AutoEqCatalogUiState()
    }

    private fun scheduleSearch(query: String, debounce: Boolean) {
        val index = catalog ?: return
        searchJob?.cancel()
        searchJob = scope.launch {
            if (debounce && searchDebounceMs > 0L) delay(searchDebounceMs)
            val results = if (query.isBlank()) emptyList() else withContext(searchDispatcher) { index.search(query) }
            mutableState.update { latest ->
                if (latest.visible && latest.query == query) latest.copy(results = results, resultsQuery = query) else latest
            }
        }
    }

    private fun cancelWork() {
        loadJob?.cancel()
        searchJob?.cancel()
        profileJob?.cancel()
        loadJob = null
        searchJob = null
        profileJob = null
    }

    private suspend fun <T> guarded(operation: String, block: suspend () -> T?): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.w(error, "AutoEQ %s load failed", operation)
        null
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 120L
    }
}
