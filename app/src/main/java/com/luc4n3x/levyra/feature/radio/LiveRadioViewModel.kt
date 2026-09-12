package com.luc4n3x.levyra.feature.radio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LiveRadioUiState(
    val levyraLanguageCode: String = "en",
    val preference: RadioLanguagePreference = RadioLanguagePreferences.forLevyraLanguage("en"),
    val stations: List<RadioStation> = emptyList(),
    val favorites: List<RadioStation> = emptyList(),
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
            val favorites = repository.favorites()
            val recent = repository.recent()
            val cached = repository.cachedCatalog(preference.levyraCode)
            if (activatedLanguage != preference.levyraCode) return@launch
            _state.update {
                it.copy(
                    favorites = favorites,
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
        _state.update { it.copy(query = clean, error = null) }
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
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.search(clean) }
                .onSuccess { stations ->
                    if (_state.value.query == clean) {
                        _state.update {
                            it.copy(
                                stations = stations,
                                loading = false,
                                canLoadMore = false,
                                showingCachedCatalog = false,
                                error = null
                            )
                        }
                    }
                }
                .onFailure { error -> publishFailure(error) }
        }
    }

    fun selectCategory(category: RadioCategory) {
        if (_state.value.category == category && _state.value.query.isBlank()) return
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
        if (snapshot.loading || snapshot.loadingMore || !snapshot.canLoadMore || snapshot.query.isNotBlank()) return
        _state.update { it.copy(loadingMore = true) }
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

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            val favorite = station.uuid.lowercase(Locale.ROOT) !in _state.value.favoriteIds
            val favorites = repository.setFavorite(station, favorite)
            _state.update { it.copy(favorites = favorites) }
        }
    }

    fun recordPlayed(station: RadioStation) {
        viewModelScope.launch {
            val recent = repository.recordRecent(station)
            _state.update { it.copy(recent = recent) }
        }
        viewModelScope.launch { repository.recordClick(station.uuid) }
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
