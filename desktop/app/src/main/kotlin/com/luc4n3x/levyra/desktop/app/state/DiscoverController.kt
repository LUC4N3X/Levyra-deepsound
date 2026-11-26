package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.charts.ChartsRepository
import com.luc4n3x.levyra.desktop.core.charts.EditorialShelf
import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class DiscoverUiState(
    val country: String = "IT",
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = false,
    val error: String = "",
    val freshCurrents: List<Track> = emptyList(),
    val genres: List<EditorialShelf> = emptyList(),
    val feedsLoading: Boolean = false
)

class DiscoverController(
    private val scope: CoroutineScope,
    private val charts: ChartsRepository
) {
    private val state = MutableStateFlow(DiscoverUiState())
    private var job: Job? = null
    private var freshCountry = ""

    val discover: StateFlow<DiscoverUiState> = state.asStateFlow()

    fun load(country: String, contentCountry: String, force: Boolean = false) {
        val region = country.trim().uppercase().take(2).ifBlank { "IT" }
        val current = state.value
        if (!force && region == current.country && contentCountry == freshCountry &&
            (current.loading || current.tracks.isNotEmpty())) return
        val previousContentCountry = freshCountry
        freshCountry = contentCountry
        job?.cancel()
        state.value = current.copy(
            country = region,
            tracks = current.tracks.takeIf { current.country == region }.orEmpty(),
            freshCurrents = current.freshCurrents.takeIf { previousContentCountry == contentCountry }.orEmpty(),
            loading = true,
            feedsLoading = true,
            error = ""
        )
        job = scope.launch {
            try {
                val tracks = charts.topSongs(region, ChartsRepository.DEFAULT_LIMIT)
                currentCoroutineContext().ensureActive()
                state.update { current ->
                    if (current.country != region) current else current.copy(
                        tracks = tracks,
                        loading = false,
                        error = if (tracks.isEmpty()) EMPTY_ERROR else ""
                    )
                }
                val fresh = charts.freshCurrents(contentCountry)
                val genres = charts.genres()
                currentCoroutineContext().ensureActive()
                state.update { current ->
                    if (current.country != region) current else current.copy(freshCurrents = fresh, genres = genres, feedsLoading = false)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                state.update { current ->
                    when {
                        current.country != region -> current
                        current.loading -> current.copy(loading = false, feedsLoading = false, error = error.message ?: EMPTY_ERROR)
                        else -> current.copy(feedsLoading = false)
                    }
                }
            }
        }
    }

    private companion object {
        const val EMPTY_ERROR = "Classifica non disponibile"
    }
}
