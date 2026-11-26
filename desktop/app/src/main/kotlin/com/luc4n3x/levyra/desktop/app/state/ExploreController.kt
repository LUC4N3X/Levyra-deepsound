package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.charts.ChartsRepository
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.news.MusicNews
import com.luc4n3x.levyra.desktop.core.news.NewsStory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountryChart(val country: String, val tracks: List<Track>)

data class ExploreUiState(
    val country: String = "",
    val localReleases: List<Track> = emptyList(),
    val worldReleases: List<Track> = emptyList(),
    val charts: List<CountryChart> = emptyList(),
    val news: List<NewsStory> = emptyList(),
    val loading: Boolean = false
)

class ExploreController(
    private val scope: CoroutineScope,
    private val charts: ChartsRepository,
    private val news: MusicNews = MusicNews()
) {
    private val state = MutableStateFlow(ExploreUiState())
    val explore: StateFlow<ExploreUiState> = state.asStateFlow()
    private var loadedKey = ""

    fun load(country: String, languageCode: String) {
        val region = country.trim().uppercase().take(2).ifBlank { "US" }
        val key = "$region|$languageCode"
        if (key == loadedKey) return
        loadedKey = key
        state.update { it.copy(country = region, loading = true) }
        scope.launch {
            val local = async { safely { charts.freshCurrents(region) }.orEmpty() }
            val world = async {
                WORLD_MARKETS.filter { it != region }.map { market -> async { safely { charts.freshCurrents(market) }.orEmpty() } }
                    .awaitAll().flatten().distinctBy { it.id }.take(RELEASE_LIMIT)
            }
            val countryCharts = async {
                (listOf(region) + CHART_MARKETS.filter { it != region }).map { market ->
                    async { CountryChart(market, safely { charts.topSongs(market, 100) }.orEmpty()) }
                }.awaitAll().filter { it.tracks.isNotEmpty() }
            }
            val stories = async { safely { news.latest(languageCode) }.orEmpty() }
            val result = ExploreUiState(region, local.await(), world.await(), countryCharts.await(), stories.await(), loading = false)
            if (loadedKey == key) state.value = result
        }
    }

    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val RELEASE_LIMIT = 24
        val WORLD_MARKETS = listOf("US", "GB")
        val CHART_MARKETS = listOf("US", "GB", "ES", "FR", "DE", "BR", "JP")
    }
}
