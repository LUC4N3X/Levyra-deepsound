package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.apple.AppleDailyChart
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
data class DailyTop100UiState(
    val key: String = "",
    val storefront: String = "",
    val language: String = "",
    val charts: List<AppleDailyChart> = emptyList(),
    val loading: Boolean = false,
    val open: DailyTop100ChartUiState? = null
)

@Immutable
data class DailyTop100ChartUiState(
    val chart: AppleDailyChart,
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
        val current = _state.value
        if (!force && current.key == key && (current.loading || current.charts.isNotEmpty())) return
        loadJob?.cancel()
        _state.update { it.copy(key = key, storefront = front, language = lang, charts = it.charts.takeIf { _ -> it.key == key }.orEmpty(), loading = true) }
        loadJob = viewModelScope.launch {
            val cached = repository.cachedDailyTop100(front, lang)
            if (cached.isNotEmpty()) _state.update { latest -> if (latest.key == key) latest.copy(charts = cached) else latest }
            val fresh = repository.dailyTop100(front, lang)
            _state.update { latest ->
                if (latest.key != key) latest else latest.copy(charts = fresh.ifEmpty { latest.charts }, loading = false)
            }
        }
    }

    fun open(chart: AppleDailyChart) {
        val current = _state.value
        if (current.open?.chart == chart && (current.open.loading || current.open.tracks.isNotEmpty())) return
        chartJob?.cancel()
        _state.update { it.copy(open = DailyTop100ChartUiState(chart)) }
        chartJob = viewModelScope.launch {
            val playlist = repository.playlist(current.storefront, chart.playlistId, current.language)
            _state.update { latest ->
                val open = latest.open?.takeIf { it.chart == chart } ?: return@update latest
                latest.copy(open = open.copy(tracks = playlist?.tracks.orEmpty().take(MAX_TRACKS), loading = false, failed = playlist == null))
            }
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
