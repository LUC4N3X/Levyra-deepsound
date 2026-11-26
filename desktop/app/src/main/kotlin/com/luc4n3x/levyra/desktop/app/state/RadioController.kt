package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.radio.RadioBrowser
import com.luc4n3x.levyra.desktop.core.radio.RadioStation
import com.luc4n3x.levyra.desktop.core.radio.SavedStation
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.feature.radio.RadioFavoriteSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

internal data class RadioUiState(
    val category: RadioCategory = RadioCategory.Popular,
    val country: String = "",
    val query: String = "",
    val stations: List<RadioStation> = emptyList(),
    val favorites: List<SavedStation> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val favoriteSort: RadioFavoriteSort = RadioFavoriteSort.Default
)

internal class RadioController(
    private val scope: CoroutineScope,
    paths: AppPaths,
    private val browser: RadioBrowser = RadioBrowser(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val store = JsonFileStore<List<SavedStation>>(
        file = paths.root.resolve("radio.json"),
        serializer = ListSerializer(SavedStation.serializer()),
        defaultValue = { emptyList() }
    )
    private val sortStore = JsonFileStore(
        file = paths.root.resolve("radio-sort.json"),
        serializer = String.serializer(),
        defaultValue = { RadioFavoriteSort.Default.id }
    )
    private val state = MutableStateFlow(RadioUiState(favorites = store.read(), favoriteSort = RadioFavoriteSort.fromId(sortStore.read())))
    val radio: StateFlow<RadioUiState> = state.asStateFlow()
    private var job: Job? = null

    fun open(country: String) {
        val region = country.trim().uppercase().take(2)
        if (state.value.country == region && (state.value.stations.isNotEmpty() || state.value.loading)) return
        state.update { it.copy(country = region) }
        reload()
    }

    fun selectCategory(category: RadioCategory) {
        if (state.value.category == category && state.value.query.isBlank()) return
        state.update { it.copy(category = category, query = "") }
        reload()
    }

    fun search(query: String) {
        state.update { it.copy(query = query) }
        job?.cancel()
        job = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            fetch { if (query.trim().length >= 2) browser.search(query) else stationsFor(state.value) }
        }
    }

    fun toggleFavorite(station: SavedStation) {
        val current = state.value.favorites
        val updated = if (current.any { it.uuid == station.uuid }) {
            current.filterNot { it.uuid == station.uuid }
        } else {
            listOf(station.copy(addedAt = now())) + current
        }
        state.update { it.copy(favorites = updated) }
        scope.launch { runCatching { store.write(updated) } }
    }

    fun setFavoriteSort(sort: RadioFavoriteSort) {
        if (state.value.favoriteSort == sort) return
        state.update { it.copy(favoriteSort = sort) }
        scope.launch { runCatching { sortStore.write(sort.id) } }
    }

    private fun reload() {
        job?.cancel()
        job = scope.launch { fetch { stationsFor(state.value) } }
    }

    private suspend fun stationsFor(value: RadioUiState): List<RadioStation> {
        val country = value.country.takeIf { value.category != RadioCategory.Worldwide && it.length == 2 }
        return browser.stations(country, value.category.apiTag)
    }

    private suspend fun fetch(block: suspend () -> List<RadioStation>) {
        state.update { it.copy(loading = true, failed = false) }
        try {
            val stations = block()
            state.update { it.copy(stations = stations, loading = false) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state.update { it.copy(loading = false, failed = true) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
