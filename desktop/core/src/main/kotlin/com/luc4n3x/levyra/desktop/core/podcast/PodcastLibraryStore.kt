package com.luc4n3x.levyra.desktop.core.podcast

import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PodcastLibraryStore(
    private val store: JsonFileStore<PodcastLibraryData>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val state = MutableStateFlow(store.read().sanitized())

    val library: StateFlow<PodcastLibraryData> = state.asStateFlow()

    fun toggleSubscription(show: PodcastShow) = mutate { data ->
        if (data.subscriptions.any { it.show.browseId == show.browseId }) {
            data.copy(subscriptions = data.subscriptions.filterNot { it.show.browseId == show.browseId })
        } else {
            data.copy(subscriptions = listOf(PodcastSubscription(show, nowMillis())) + data.subscriptions)
        }
    }

    fun saveProgress(episode: Track, positionMs: Long, durationMs: Long) = mutate { data ->
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        val entry = PodcastEpisodeProgress(episode.stableEpisode(), position, duration, podcastEpisodeCompleted(position, duration), nowMillis())
        data.copy(history = (listOf(entry) + data.history.filterNot { it.track.id == episode.id }).take(PODCAST_HISTORY_LIMIT))
    }

    fun setPlayed(episode: Track, played: Boolean) = mutate { data ->
        val existing = data.history.firstOrNull { it.track.id == episode.id }
        when {
            existing != null -> data.copy(history = data.history.map {
                if (it.track.id == episode.id) it.copy(completed = played, positionMs = 0L) else it
            })
            played -> data.copy(history = (listOf(PodcastEpisodeProgress(episode.stableEpisode(), completed = true, updatedAt = nowMillis())) + data.history)
                .take(PODCAST_HISTORY_LIMIT))
            else -> data
        }
    }

    fun remove(episodeId: String) = mutate { data -> data.copy(history = data.history.filterNot { it.track.id == episodeId }) }

    fun clearHistory() = mutate { data -> data.copy(history = emptyList()) }

    @Synchronized
    private fun mutate(transform: (PodcastLibraryData) -> PodcastLibraryData) {
        val updated = transform(state.value)
        if (updated == state.value) return
        store.write(updated)
        state.value = updated
    }

    private fun PodcastLibraryData.sanitized(): PodcastLibraryData = copy(
        subscriptions = subscriptions.filter { isPodcastShowId(it.show.browseId) }.distinctBy { it.show.browseId },
        history = history.sortedByDescending { it.updatedAt }.distinctBy { it.track.id }.take(PODCAST_HISTORY_LIMIT)
    )

    private fun Track.stableEpisode(): Track = copy(offlinePath = "", offlineMediaLabel = "")

    companion object {
        fun create(paths: AppPaths): PodcastLibraryStore = PodcastLibraryStore(
            JsonFileStore(
                file = paths.root.resolve("podcasts.json"),
                serializer = PodcastLibraryData.serializer(),
                defaultValue = { PodcastLibraryData() }
            )
        )
    }
}
