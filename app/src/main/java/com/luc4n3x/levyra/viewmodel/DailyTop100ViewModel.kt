package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.apple.AppleCityChart
import com.luc4n3x.levyra.data.apple.AppleDailyChart
import com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository
import com.luc4n3x.levyra.data.apple.AppleStorefronts
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

sealed interface DailyChartRef {
    val playlistId: String
    val global: Boolean get() = false

    data class Country(val chart: AppleDailyChart) : DailyChartRef {
        override val playlistId: String get() = chart.playlistId
        override val global: Boolean get() = chart.global
    }

    data class City(val chart: AppleCityChart) : DailyChartRef {
        override val playlistId: String get() = chart.playlistId
    }
}

@Immutable
data class DailyTop100UiState(
    val key: String = "",
    val storefront: String = "",
    val language: String = "",
    val charts: List<AppleDailyChart> = emptyList(),
    val cities: List<AppleCityChart> = emptyList(),
    val loading: Boolean = false,
    val open: DailyTop100ChartUiState? = null
)

@Immutable
data class DailyTop100ChartUiState(
    val chart: DailyChartRef,
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false
)

class DailyTop100ViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppleMusicEditorialRepository.get(application)
    private val _state = MutableStateFlow(DailyTop100UiState())
    val state: StateFlow<DailyTop100UiState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var chartJob: Job? = null

    fun load(storefront: String, language: String, force: Boolean = false) {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val key = "$front:$lang"
        if (!force && chartsSettled(_state.value, key)) return
        loadJob?.cancel()
        _state.update {
            val sameKey = it.key == key
            it.copy(
                key = key, storefront = front, language = lang, loading = true,
                charts = if (sameKey) it.charts else emptyList(), cities = if (sameKey) it.cities else emptyList()
            )
        }
        loadJob = viewModelScope.launch {
            joinAll(
                launch {
                    publish(key, top100 = chartsOrNull { repository.cachedDailyTop100(front, lang) })
                    publish(key, top100 = chartsOrNull { repository.dailyTop100(front, lang) })
                },
                launch {
                    publish(key, cities = chartsOrNull { repository.cachedCityCharts(front, lang) })
                    publish(key, cities = chartsOrNull { repository.cityCharts(front, lang) })
                }
            )
            _state.update { latest -> if (latest.key == key) latest.copy(loading = false) else latest }
        }
    }

    private fun publish(key: String, top100: List<AppleDailyChart>? = null, cities: List<AppleCityChart>? = null) {
        _state.update { mergeCharts(it, key, top100, cities) }
    }

    fun open(chart: DailyChartRef) {
        val current = _state.value
        if (current.open?.chart == chart && (current.open.loading || current.open.tracks.isNotEmpty())) return
        chartJob?.cancel()
        _state.update { it.copy(open = DailyTop100ChartUiState(chart)) }
        chartJob = viewModelScope.launch {
            val playlist = repository.playlist(current.storefront, chart.playlistId, current.language)
            _state.update { latest -> publishOpen(latest, chart, playlist?.tracks?.take(MAX_TRACKS)) }
        }
    }

    fun retry() {
        _state.value.open?.chart?.let(::open)
    }

    fun close() {
        chartJob?.cancel()
        _state.update { it.copy(open = null) }
    }

    private companion object {
        const val MAX_TRACKS = 100
    }
}

internal suspend fun <T> chartsOrNull(fetch: suspend () -> List<T>): List<T>? =
    try {
        fetch().ifEmpty { null }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

internal fun chartsSettled(state: DailyTop100UiState, key: String): Boolean =
    state.key == key && (state.loading || (state.charts.isNotEmpty() && state.cities.isNotEmpty()))

internal fun mergeCharts(
    state: DailyTop100UiState,
    key: String,
    top100: List<AppleDailyChart>?,
    cities: List<AppleCityChart>?
): DailyTop100UiState =
    if (state.key != key || (top100 == null && cities == null)) state
    else state.copy(charts = top100 ?: state.charts, cities = cities ?: state.cities)

internal fun publishOpen(state: DailyTop100UiState, chart: DailyChartRef, tracks: List<Track>?): DailyTop100UiState {
    val open = state.open?.takeIf { it.chart == chart } ?: return state
    return state.copy(open = open.copy(tracks = tracks.orEmpty(), loading = false, failed = tracks == null))
}
