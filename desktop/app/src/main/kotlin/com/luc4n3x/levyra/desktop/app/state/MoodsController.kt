package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.moods.MoodCategory
import com.luc4n3x.levyra.desktop.core.moods.MoodShelf
import com.luc4n3x.levyra.desktop.core.moods.MoodsRepository
import com.luc4n3x.levyra.desktop.core.moods.moodBrowseOrder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class MoodsUiState(
    val language: String = "",
    val country: String = "",
    val categories: List<MoodCategory> = emptyList(),
    val artwork: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val open: MoodCategory? = null,
    val shelves: List<MoodShelf> = emptyList(),
    val shelvesLoading: Boolean = false
)

class MoodsController(
    private val scope: CoroutineScope,
    private val repository: MoodsRepository
) {
    private val state = MutableStateFlow(MoodsUiState())
    private val artworkPermits = Semaphore(ARTWORK_PARALLELISM)
    private val requested = ConcurrentHashMap.newKeySet<String>()
    private var artworkJobs: CompletableJob = SupervisorJob(scope.coroutineContext[Job])
    private var job: Job? = null
    private var shelvesJob: Job? = null

    val moods: StateFlow<MoodsUiState> = state.asStateFlow()

    fun load(language: String, country: String, force: Boolean = false) {
        val current = state.value
        val same = current.language == language && current.country == country
        if (!force && same && (current.loading || current.categories.isNotEmpty())) return
        job?.cancel()
        if (!same) {
            artworkJobs.cancel()
            artworkJobs = SupervisorJob(scope.coroutineContext[Job])
            requested.clear()
            shelvesJob?.cancel()
        }
        state.value = if (same) current.copy(loading = true, failed = false) else MoodsUiState(language, country, loading = true)
        job = scope.launch {
            try {
                val (categories, decades) = coroutineScope {
                    val categories = async { repository.categories(language, country) }
                    val decades = async { repository.decades(language, country) }
                    categories.await() to decades.await()
                }
                val ordered = moodBrowseOrder(categories, decades)
                val covers = decades.filter { it.artworkUrl.isNotBlank() }.associate { it.params to it.artworkUrl }
                state.update {
                    if (it.language != language || it.country != country) it
                    else it.copy(categories = ordered, artwork = covers + it.artwork, loading = false, failed = ordered.isEmpty())
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                state.update { if (it.language == language && it.country == country) it.copy(loading = false, failed = it.categories.isEmpty()) else it }
            }
        }
    }

    fun requestArtwork(params: String) {
        val snapshot = state.value
        if (params.isBlank() || params in snapshot.artwork || !requested.add(params)) return
        val language = snapshot.language
        val country = snapshot.country
        scope.launch(artworkJobs) {
            val url = try {
                artworkPermits.withPermit { repository.artwork(params, language, country) }
            } catch (cancellation: CancellationException) {
                requested.remove(params)
                throw cancellation
            } catch (_: Exception) {
                ""
            }
            if (url.isNotBlank()) {
                state.update {
                    if (it.language != language || it.country != country) it else it.copy(artwork = it.artwork + (params to url))
                }
            }
        }
    }

    fun open(category: MoodCategory) {
        shelvesJob?.cancel()
        val snapshot = state.value
        state.value = snapshot.copy(open = category, shelves = emptyList(), shelvesLoading = true)
        shelvesJob = scope.launch {
            val shelves = try {
                repository.shelves(category.params, snapshot.language, snapshot.country)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                emptyList()
            }
            state.update { if (it.open?.params == category.params) it.copy(shelves = shelves, shelvesLoading = false) else it }
        }
    }

    fun close() {
        shelvesJob?.cancel()
        state.update { it.copy(open = null, shelves = emptyList(), shelvesLoading = false) }
    }

    private companion object {
        const val ARTWORK_PARALLELISM = 4
    }
}
