package com.luc4n3x.levyra.desktop.core.podcast

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.serialization.Serializable

private val PODCAST_SHOW_ID = Regex("^MPSP[A-Za-z0-9_-]{8,64}$")
private const val PODCAST_COMPLETION_TAIL_MS = 30_000L
private const val PODCAST_COMPLETION_RATIO = 0.97
const val PODCAST_RESUME_MIN_POSITION_MS = 5_000L
const val PODCAST_HISTORY_LIMIT = 200

data class PodcastLocale(val hl: String, val gl: String) {
    val cacheKey: String get() = "$hl|$gl"
}

data class PodcastShelf(val title: String, val episodes: List<Track>)

@Serializable
data class PodcastShow(
    val browseId: String,
    val title: String,
    val author: String = "",
    val thumbnailUrl: String = ""
)

data class PodcastEpisodePage(val episodes: List<Track>, val continuation: String?)

data class PodcastShowPage(val show: PodcastShow, val description: String, val episodes: PodcastEpisodePage)

@Serializable
data class PodcastSubscription(
    val show: PodcastShow,
    val subscribedAt: Long
)

@Serializable
data class PodcastEpisodeProgress(
    val track: Track,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val completed: Boolean = false,
    val updatedAt: Long = 0L
) {
    val fraction: Float get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val inProgress: Boolean get() = !completed && positionMs >= PODCAST_RESUME_MIN_POSITION_MS
}

@Serializable
data class PodcastLibraryData(
    val subscriptions: List<PodcastSubscription> = emptyList(),
    val history: List<PodcastEpisodeProgress> = emptyList()
)

fun isPodcastShowId(browseId: String): Boolean = PODCAST_SHOW_ID.matches(browseId)

fun podcastEpisodeCompleted(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0L && (durationMs - positionMs <= PODCAST_COMPLETION_TAIL_MS || positionMs >= durationMs * PODCAST_COMPLETION_RATIO)

fun resumePositionMs(progress: PodcastEpisodeProgress?): Long =
    progress?.takeIf { it.inProgress }?.positionMs ?: 0L
