package com.luc4n3x.levyra.feature.radio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class LiveRadioUiState(
    val levyraLanguageCode: String = "en",
    val preference: RadioLanguagePreference = RadioLanguagePreferences.forLevyraLanguage("en"),
    val stations: List<RadioStation> = emptyList(),
    val favorites: List<RadioStation> = emptyList(),
    val customStations: List<RadioStation> = emptyList(),
    val savingCustom: Boolean = false,
    val searchLimitReached: Boolean = false,
    val customError: CustomRadioError? = null,
    val recent: List<RadioStation> = emptyList(),
    val countries: List<RadioDirectoryEntry> = emptyList(),
    val languages: List<RadioDirectoryEntry> = emptyList(),
    val category: RadioCategory = RadioCategory.Popular,
    val selectedCountryCode: String? = null,
    val selectedLanguage: String? = null,
    val query: String = "",
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val showingCachedCatalog: Boolean = false,
    val error: String? = null
) {
    val favoriteIds: Set<String> get() = favorites.mapTo(hashSetOf()) { it.uuid.lowercase(Locale.ROOT) }
}

internal class LiveRadioViewModel(
    private val repository: RadioRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LiveRadioUiState())
    val state: StateFlow<LiveRadioUiState> = _state.asStateFlow()
    private var browseJob: Job? = null
    private var searchJob: Job? = null
    private var activationJob: Job? = null
    private var activatedLanguage = ""
    private val favoritesMutex = Mutex()
    private var searchCursor: List<RadioSearchRequest>? = null

    init {
        viewModelScope.launch { repository.observeFavorites().collect { favorites -> _state.update { it.copy(favorites = favorites) } } }
        viewModelScope.launch { repository.observeCustomStations().collect { stations -> _state.update { it.copy(customStations = stations) } } }
        viewModelScope.launch { repository.observeRecent().collect { stations -> _state.update { it.copy(recent = stations) } } }
    }

    fun activate(levyraLanguageCode: String) {
        val preference = RadioLanguagePreferences.forLevyraLanguage(levyraLanguageCode)
        if (activatedLanguage == preference.levyraCode && _state.value.stations.isNotEmpty()) return
        activatedLanguage = preference.levyraCode
        browseJob?.cancel()
        searchJob?.cancel()
        activationJob?.cancel()
        _state.update {
            it.copy(
                levyraLanguageCode = preference.levyraCode,
                preference = preference,
                category = RadioCategory.Popular,
                selectedCountryCode = preference.primaryCountry,
                selectedLanguage = null,
                query = "",
                loading = true,
                error = null
            )
        }
        viewModelScope.launch { refreshDirectories() }
        activationJob = viewModelScope.launch {
            favoritesMutex.withLock {
                val favorites = repository.favorites()
                _state.update { it.copy(favorites = favorites) }
            }
            val recent = repository.recent()
            val cached = repository.cachedCatalog(preference.levyraCode)
            if (activatedLanguage != preference.levyraCode) return@launch
            _state.update {
                it.copy(
                    recent = recent,
                    stations = cached?.stations.orEmpty(),
                    showingCachedCatalog = cached != null
                )
            }
            loadFirstPage(localeBoost = true)
        }
    }

    fun setQuery(value: String) {
        val clean = value.take(MAX_QUERY_LENGTH)
        activationJob?.cancel()
        searchCursor = null
        _state.update { it.copy(query = clean, stations = emptyList(), loading = clean.trim().length >= 2, loadingMore = false, canLoadMore = false, searchLimitReached = false, error = null) }
        searchJob?.cancel()
        browseJob?.cancel()
        searchJob = viewModelScope.launch {
            if (clean.isBlank()) {
                delay(100L)
                loadFirstPage(localeBoost = isDefaultLocaleSelection())
                return@launch
            }
            if (clean.trim().length < 2) {
                _state.update { it.copy(loading = false, loadingMore = false) }
                return@launch
            }
            delay(SEARCH_DEBOUNCE_MS)
            loadSearchPage(clean, append = false)
        }
    }

    private suspend fun loadSearchPage(query: String, append: Boolean) {
        try {
            val page = repository.search(query, if (append) searchCursor else null)
            val previous = if (append) _state.value.stations else emptyList()
            val ranked = withContext(Dispatchers.Default) {
                filterAndRankRadioSearchResults(previous + page.stations, query, requireLocalMatch = false)
            }
            if (_state.value.query != query) return
            searchCursor = page.next
            _state.update {
                val limited = ranked.size >= MAX_SEARCH_STATIONS
                it.copy(stations = ranked.take(MAX_SEARCH_STATIONS), loading = false, loadingMore = false, canLoadMore = page.next.isNotEmpty() && !limited,
                    searchLimitReached = limited,
                    showingCachedCatalog = false, error = if (page.partialFailure) "partial" else null)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            publishFailure(error)
        }
    }

    fun selectCategory(category: RadioCategory) {
        if (_state.value.category == category && _state.value.query.isBlank()) return
        activationJob?.cancel()
        searchJob?.cancel()
        _state.update {
            it.copy(
                category = category,
                query = "",
                selectedCountryCode = if (category == RadioCategory.Worldwide) null else it.selectedCountryCode,
                selectedLanguage = if (category == RadioCategory.Worldwide) null else it.selectedLanguage
            )
        }
        loadFirstPage(localeBoost = category == RadioCategory.Popular && isDefaultLocaleSelection())
    }

    fun selectCountry(countryCode: String?) {
        activationJob?.cancel()
        searchJob?.cancel()
        _state.update {
            it.copy(
                selectedCountryCode = countryCode?.trim()?.uppercase(Locale.ROOT)?.takeIf(String::isNotBlank),
                selectedLanguage = null,
                category = if (countryCode == null) RadioCategory.Worldwide else RadioCategory.Popular,
                query = ""
            )
        }
        loadFirstPage(localeBoost = isDefaultLocaleSelection())
    }

    fun selectLanguage(language: String?) {
        activationJob?.cancel()
        searchJob?.cancel()
        _state.update {
            it.copy(
                selectedLanguage = language?.trim()?.takeIf(String::isNotBlank),
                selectedCountryCode = null,
                category = RadioCategory.Popular,
                query = ""
            )
        }
        loadFirstPage(localeBoost = false)
    }

    fun retry() {
        if (_state.value.query.isNotBlank()) setQuery(_state.value.query) else loadFirstPage(isDefaultLocaleSelection())
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.loadingMore || !snapshot.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        if (snapshot.query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { loadSearchPage(snapshot.query, append = true) }
            return
        }
        browseJob?.cancel()
        val filter = currentFilter(offset = snapshot.stations.size)
        browseJob = viewModelScope.launch {
            runCatching { repository.discover(snapshot.preference, filter, localeBoost = false) }
                .onSuccess { additions ->
                    _state.update { current ->
                        if (current.query.isNotBlank() ||
                            current.category != filter.category ||
                            current.selectedCountryCode != filter.countryCode ||
                            current.selectedLanguage != filter.language
                        ) {
                            return@update current.copy(loadingMore = false)
                        }
                        val merged = filterAndRankRadioStations(current.stations + additions)
                            .take(MAX_VISIBLE_STATIONS)
                        val grew = merged.size > current.stations.size
                        current.copy(
                            stations = merged,
                            loadingMore = false,
                            canLoadMore = grew && merged.size < MAX_VISIBLE_STATIONS && additions.size >= PAGE_SIZE / 2,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(loadingMore = false, error = error.message ?: "Radio Browser unavailable") }
                }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            favoritesMutex.withLock {
                val favorites = repository.favorites()
                _state.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun toggleFavorite(station: RadioStation) {
        clearCustomError()
        viewModelScope.launch {
            favoritesMutex.withLock {
                try {
                    val favorite = repository.favorites().none { it.uuid.equals(station.uuid, true) }
                    val favorites = repository.setFavorite(station, favorite)
                    _state.update { it.copy(favorites = favorites) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (error: Exception) { publishCustomFailure(error) }
            }
        }
    }

    fun clearCustomError() { _state.update { it.copy(customError = null) } }

    private fun publishCustomFailure(error: Exception) {
        _state.update { it.copy(customError = (error as? CustomRadioException)?.reason ?: CustomRadioError.Storage) }
    }

    fun saveCustom(name: String, url: String, image: String, existing: RadioStation?, onSaved: () -> Unit) {
        if (_state.value.savingCustom) return
        _state.update { it.copy(savingCustom = true, customError = null) }
        viewModelScope.launch {
            try {
                repository.saveCustom(customRadioStation(name, url, image, existing))
                val recent = repository.recent()
                _state.update { it.copy(savingCustom = false, recent = recent) }
                onSaved()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(savingCustom = false, customError = (error as? CustomRadioException)?.reason ?: CustomRadioError.Storage) }
            }
        }
    }

    fun deleteCustom(station: RadioStation, onDeleted: () -> Unit) {
        clearCustomError()
        viewModelScope.launch {
            try {
                repository.deleteCustom(station)
                val recent = repository.recent()
                _state.update { it.copy(recent = recent, stations = it.stations.filterNot { item -> item.uuid == station.uuid }) }
                onDeleted()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { publishCustomFailure(error) }
        }
    }

    private fun loadFirstPage(localeBoost: Boolean) {
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, loadingMore = false, canLoadMore = true, error = null) }
            val snapshot = _state.value
            runCatching { repository.discover(snapshot.preference, currentFilter(), localeBoost) }
                .onSuccess { stations ->
                    _state.update {
                        it.copy(
                            stations = stations,
                            loading = false,
                            canLoadMore = stations.size >= PAGE_SIZE / 2,
                            showingCachedCatalog = false,
                            error = null
                        )
                    }
                }
                .onFailure(::publishFailure)
        }
    }

    private fun currentFilter(offset: Int = 0): RadioFilter {
        val snapshot = _state.value
        return RadioFilter(
            category = snapshot.category,
            countryCode = snapshot.selectedCountryCode,
            language = snapshot.selectedLanguage,
            offset = offset,
            limit = PAGE_SIZE
        )
    }

    private fun isDefaultLocaleSelection(): Boolean {
        val snapshot = _state.value
        return snapshot.category == RadioCategory.Popular &&
            snapshot.selectedCountryCode == snapshot.preference.primaryCountry &&
            snapshot.selectedLanguage == null && snapshot.query.isBlank()
    }

    private suspend fun refreshDirectories() {
        try {
            val countries = repository.countries()
            val languages = repository.languages()
            _state.update { it.copy(countries = countries, languages = languages) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }
    }

    private fun publishFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _state.update {
            it.copy(
                loading = false,
                loadingMore = false,
                error = error.message ?: "Radio Browser unavailable"
            )
        }
    }

    companion object {
        private const val PAGE_SIZE = 32
        private const val MAX_VISIBLE_STATIONS = 256
        private const val MAX_SEARCH_STATIONS = 10_000
        private const val MAX_QUERY_LENGTH = 80
        private const val SEARCH_DEBOUNCE_MS = 450L

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LiveRadioViewModel(RadioRepository(context.applicationContext)) as T
            }
        }
    }
}
