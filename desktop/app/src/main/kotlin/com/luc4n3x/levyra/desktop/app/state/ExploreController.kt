package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.charts.AppleCityChart
import com.luc4n3x.levyra.desktop.core.charts.AppleDailyChart
import com.luc4n3x.levyra.desktop.core.charts.AppleEditorialCharts
import com.luc4n3x.levyra.desktop.core.charts.ChartsRepository
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.news.MusicNews
import com.luc4n3x.levyra.desktop.core.news.NewsStory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpenChart(
    val playlistId: String,
    val title: String,
    val badge: String,
    val global: Boolean,
    val city: Boolean,
    val slot: Int,
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true
)

data class ExploreUiState(
    val country: String = "",
    val localReleases: List<Track> = emptyList(),
    val worldReleases: List<Track> = emptyList(),
    val daily: List<AppleDailyChart> = emptyList(),
    val cities: List<AppleCityChart> = emptyList(),
    val news: List<NewsStory> = emptyList(),
    val open: OpenChart? = null,
    val loading: Boolean = false
)

class ExploreController(
    private val scope: CoroutineScope,
    private val charts: ChartsRepository,
    private val editorial: AppleEditorialCharts = AppleEditorialCharts(),
    private val news: MusicNews = MusicNews()
) {
    private val state = MutableStateFlow(ExploreUiState())
    val explore: StateFlow<ExploreUiState> = state.asStateFlow()
    private var loadedKey = ""
    private var language = ""
    private var chartJob: Job? = null

    fun load(country: String, languageCode: String) {
        val region = country.trim().uppercase().take(2).ifBlank { "US" }
        val key = "$region|$languageCode"
        if (key == loadedKey) return
        loadedKey = key
        language = languageCode
        state.update { it.copy(country = region, loading = true) }
        scope.launch {
            val storefront = region.lowercase()
            val local = async { safely { charts.freshCurrents(region) }.orEmpty() }
            val world = async {
                WORLD_MARKETS.filter { it != region }.map { market -> async { safely { charts.freshCurrents(market) }.orEmpty() } }
                    .awaitAll().flatten().distinctBy { it.id }.take(RELEASE_LIMIT)
            }
            val daily = async { safely { editorial.dailyTop100(storefront, languageCode) }.orEmpty() }
            val cities = async { safely { editorial.cityCharts(storefront, languageCode) }.orEmpty() }
            val stories = async { safely { news.latest(languageCode) }.orEmpty() }
            val result = ExploreUiState(
                country = region,
                localReleases = local.await(),
                worldReleases = world.await(),
                daily = daily.await(),
                cities = cities.await(),
                news = stories.await(),
                open = state.value.open,
                loading = false
            )
            if (loadedKey == key) state.value = result
        }
    }

    fun open(chart: OpenChart, storefront: String) {
        chartJob?.cancel()
        state.update { it.copy(open = chart.copy(tracks = emptyList(), loading = true)) }
        val front = storefront.ifBlank { state.value.country.lowercase() }
        chartJob = scope.launch {
            val tracks = safely { editorial.playlist(front, chart.playlistId, language) }?.tracks.orEmpty()
            state.update { current ->
                if (current.open?.playlistId == chart.playlistId) current.copy(open = current.open.copy(tracks = tracks, loading = false)) else current
            }
        }
    }

    fun close() {
        chartJob?.cancel()
        state.update { it.copy(open = null) }
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
    }
}
