package com.luc4n3x.levyra.ui.insights

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.ListeningInsightsRepository
import com.luc4n3x.levyra.domain.ListeningInsightsArtist
import com.luc4n3x.levyra.domain.ListeningInsightsHistoryItem
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class ListeningInsightsUiState(
    val period: ListeningInsightsPeriod = ListeningInsightsPeriod.Week,
    val snapshot: ListeningInsightsSnapshot = ListeningInsightsSnapshot(),
    val history: List<ListeningInsightsHistoryItem> = emptyList(),
    val query: String = "",
    val searchVisible: Boolean = false,
    val loading: Boolean = true,
    val historyLoading: Boolean = false,
    val hasMoreHistory: Boolean = true,
    val failed: Boolean = false
)

class ListeningInsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ListeningInsightsRepository(application.applicationContext)
    private val _state = MutableStateFlow(ListeningInsightsUiState())
    val state: StateFlow<ListeningInsightsUiState> = _state.asStateFlow()

    private var snapshotJob: Job? = null
    private var portraitJob: Job? = null
    private var historyJob: Job? = null
    private var searchJob: Job? = null

    fun selectPeriod(period: ListeningInsightsPeriod) {
        if (_state.value.period == period) return
        searchJob?.cancel()
        historyJob?.cancel()
        portraitJob?.cancel()
        _state.update {
            it.copy(
                period = period,
                history = emptyList(),
                query = "",
                loading = true,
                historyLoading = false,
                hasMoreHistory = true,
                failed = false
            )
        }
        refresh()
    }

    fun refresh() {
        val period = _state.value.period
        snapshotJob?.cancel()
        portraitJob?.cancel()
        snapshotJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false) }
            try {
                val snapshot = repository.snapshot(period)
                _state.update { current ->
                    if (current.period == period) current.copy(snapshot = snapshot, loading = false) else current
                }
                loadFirstHistoryPage(period, _state.value.query)
                loadArtistPortraits(period, snapshot.topArtists)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening insights load failed")
                _state.update { current ->
                    if (current.period == period) current.copy(loading = false, failed = true) else current
                }
            }
        }
    }

    private fun loadArtistPortraits(period: ListeningInsightsPeriod, artists: List<ListeningInsightsArtist>) {
        if (artists.isEmpty() || artists.all { it.artworkUrl.isNotBlank() }) return
        portraitJob?.cancel()
        portraitJob = viewModelScope.launch {
            try {
                val resolved = repository.resolveArtistPortraits(artists)
                val hasNewArtworks = resolved.zip(artists).any { (newA, oldA) -> newA.artworkUrl != oldA.artworkUrl }
                if (hasNewArtworks) {
                    _state.update { current ->
                        if (current.period == period) {
                            current.copy(
                                snapshot = current.snapshot.copy(topArtists = resolved)
                            )
                        } else {
                            current
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening insights artist portraits resolution failed")
            }
        }
    }

    fun setSearchVisible(visible: Boolean) {
        _state.update { it.copy(searchVisible = visible, query = if (visible) it.query else "") }
        if (!visible) scheduleSearch("")
    }

    fun setQuery(query: String) {
        val clean = query.take(80)
        _state.update { it.copy(query = clean) }
        scheduleSearch(clean)
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(220L)
            loadFirstHistoryPage(_state.value.period, query)
        }
    }

    private fun loadFirstHistoryPage(period: ListeningInsightsPeriod, query: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _state.update { it.copy(historyLoading = true) }
            try {
                val page = repository.historyPage(period = period, query = query)
                _state.update { current ->
                    if (current.period == period && current.query == query) {
                        current.copy(
                            history = page,
                            historyLoading = false,
                            hasMoreHistory = page.size == ListeningInsightsRepository.HISTORY_PAGE_SIZE
                        )
                    } else {
                        current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening history page failed")
                _state.update { it.copy(historyLoading = false) }
            }
        }
    }

    fun loadMoreHistory() {
        val current = _state.value
        if (current.historyLoading || !current.hasMoreHistory) return
        val cursor = current.history.lastOrNull() ?: return
        historyJob = viewModelScope.launch {
            _state.update { it.copy(historyLoading = true) }
            try {
                val page = repository.historyPage(
                    period = current.period,
                    query = current.query,
                    cursorStartedAt = cursor.startedAt,
                    cursorId = cursor.id
                )
                _state.update { latest ->
                    if (latest.period == current.period && latest.query == current.query) {
                        latest.copy(
                            history = latest.history + page,
                            historyLoading = false,
                            hasMoreHistory = page.size == ListeningInsightsRepository.HISTORY_PAGE_SIZE
                        )
                    } else {
                        latest
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening history pagination failed")
                _state.update { it.copy(historyLoading = false) }
            }
        }
    }
}
