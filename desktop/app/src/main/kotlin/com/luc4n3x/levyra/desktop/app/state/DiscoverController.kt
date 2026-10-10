package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.charts.ChartsRepository
import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val country: String = "IT",
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = false,
    val error: String = ""
)

class DiscoverController(
    private val scope: CoroutineScope,
    private val charts: ChartsRepository
) {
    private val state = MutableStateFlow(DiscoverUiState())
    private var job: Job? = null

    val discover: StateFlow<DiscoverUiState> = state.asStateFlow()

    fun load(country: String, force: Boolean = false) {
        val region = country.trim().uppercase().take(2).ifBlank { "IT" }
        val current = state.value
        if (!force && region == current.country && current.tracks.isNotEmpty()) return
        job?.cancel()
        state.value = current.copy(country = region, loading = true, error = "")
        job = scope.launch {
            try {
                val tracks = charts.topSongs(region, ChartsRepository.DEFAULT_LIMIT)
                state.value = state.value.copy(
                    tracks = tracks,
                    loading = false,
                    error = if (tracks.isEmpty()) EMPTY_ERROR else ""
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                state.value = state.value.copy(loading = false, error = error.message ?: EMPTY_ERROR)
            }
        }
    }

    private companion object {
        const val EMPTY_ERROR = "Classifica non disponibile"
    }
}
