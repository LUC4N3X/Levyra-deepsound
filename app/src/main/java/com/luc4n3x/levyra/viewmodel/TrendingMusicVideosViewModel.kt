package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.AppleMusicVideoChart
import com.luc4n3x.levyra.data.ChartsRepository
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

class TrendingMusicVideosViewModel(application: Application) : AndroidViewModel(application) {
    private val charts = ChartsRepository(application)
    private val youtube = YoutubeMusicRepository(application)
    private val state = MutableStateFlow<List<Track>>(emptyList())
    private var loadedKey: String? = null
    private var job: Job? = null

    val videos: StateFlow<List<Track>> = state.asStateFlow()

    fun load(regionId: String, languageCode: String) {
        val country = regionId.takeIf { it.length == 2 } ?: DEFAULT_STOREFRONT
        val key = "$country|$languageCode"
        if (key == loadedKey) return
        loadedKey = key
        job?.cancel()
        job = viewModelScope.launch {
            state.value = try {
                resolve(country, languageCode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Trending music videos unavailable for %s", country)
                loadedKey = null
                emptyList()
            }
        }
    }

    private suspend fun resolve(country: String, languageCode: String): List<Track> {
        val chart = charts.appleTopMusicVideos(country, CHART_SIZE)
        val gate = Semaphore(SEARCH_PARALLELISM)
        val matches = coroutineScope {
            chart.map { video ->
                async {
                    gate.withPermit {
                        val match = AppleMusicVideoChart.searchQueries(video).firstNotNullOfOrNull { query ->
                            AppleMusicVideoChart.match(video, youtube.searchMusicVideos(query, SEARCH_RESULTS, languageCode))
                        }
                        if (match == null) Timber.d("Trending video without an official match: %s - %s", video.title, video.artist)
                        match?.copy(largeThumbnailUrl = video.artworkUrl.ifBlank { match.largeThumbnailUrl })
                    }
                }
            }.awaitAll()
        }
        return matches.filterNotNull().distinctBy { it.id }
    }

    override fun onCleared() {
        charts.close()
    }

    private companion object {
        const val DEFAULT_STOREFRONT = "us"
        const val CHART_SIZE = 20
        const val SEARCH_RESULTS = 10
        const val SEARCH_PARALLELISM = 4
    }
}
