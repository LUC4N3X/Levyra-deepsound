package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale

internal const val YOUTUBE_SHORTS_SOURCE = "YouTube Shorts"
private const val MAX_SHORT_QUERIES = 10
private const val SHORTS_SEARCH_CONCURRENCY = 3
private const val SHORTS_PER_QUERY = 4
private const val MAX_SHORT_DURATION_SECONDS = 180L
private const val SHORTS_SEARCH_TIMEOUT_MS = 20_000L

internal data class YoutubeShortsFeedResult(
    val tracks: List<Track>,
    val completedQueries: Int,
    val failedQueries: Int
) {
    val isConclusive: Boolean
        get() = completedQueries > 0
}

private sealed interface ShortsQueryResult {
    data class Success(val tracks: List<Track>) : ShortsQueryResult
    data class Failure(val error: Throwable) : ShortsQueryResult
}

/**
 * Dedicated short-form feed. Like LibreTube, this trusts NewPipe's
 * StreamInfoItem.isShortFormContent flag instead of guessing from titles.
 */
internal class YoutubeShortsRepository(private val context: Context) {
    suspend fun feed(
        seeds: List<Track>,
        languageCode: String,
        limit: Int = 24
    ): YoutubeShortsFeedResult = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext YoutubeShortsFeedResult(emptyList(), 0, 0)
        val queries = shortQueries(seeds, languageCode)
        if (queries.isEmpty()) return@withContext YoutubeShortsFeedResult(emptyList(), 0, 0)

        val queryResults = coroutineScope {
            val semaphore = Semaphore(SHORTS_SEARCH_CONCURRENCY)
            queries.map { query ->
                async {
                    semaphore.withPermit {
                        searchShortsSafely(query, SHORTS_PER_QUERY)
                    }
                }
            }.awaitAll()
        }
        val successful = queryResults.filterIsInstance<ShortsQueryResult.Success>()
        val failed = queryResults.count { result -> result is ShortsQueryResult.Failure }
        val tracks = successful
            .asSequence()
            .flatMap { result -> result.tracks.asSequence() }
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(limit)
            .toList()
        YoutubeShortsFeedResult(
            tracks = tracks,
            completedQueries = successful.size,
            failedQueries = failed
        )
    }

    private suspend fun searchShortsSafely(query: String, limit: Int): ShortsQueryResult {
        return try {
            ShortsQueryResult.Success(searchShorts(query, limit))
        } catch (error: TimeoutCancellationException) {
            ShortsQueryResult.Failure(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            ShortsQueryResult.Failure(error)
        }
    }

    private suspend fun searchShorts(query: String, limit: Int): List<Track> {
        return withShortsSearchTimeout {
            runInterruptible(Dispatchers.IO) {
                NewPipeRuntime.ensure(context)
                val service = ServiceList.YouTube
                val handler = service.searchQHFactory.fromQuery(query)
                val info = SearchInfo.getInfo(service, handler)
                info.relatedItems
                    .asSequence()
                    .filterIsInstance<StreamInfoItem>()
                    .filter { item ->
                        isYoutubeShortCandidate(
                            isShortFormContent = item.isShortFormContent,
                            url = item.url,
                            durationSeconds = item.duration
                        )
                    }
                    .mapNotNull(::shortTrack)
                    .distinctBy { track -> track.id }
                    .take(limit)
                    .toList()
            }
        }
    }

    private fun shortTrack(item: StreamInfoItem): Track? {
        val id = youtubeVideoId(item.url)
        if (id.isBlank()) return null
        val thumbnail = item.thumbnailUrl.orEmpty()
        val (accentStart, accentEnd) = shortPalette(id)
        return Track(
            id = id,
            title = item.name.trim().ifBlank { "YouTube Short" },
            artist = item.uploaderName.orEmpty().trim().ifBlank { "YouTube" },
            album = "YouTube Shorts",
            durationMs = item.duration.coerceAtLeast(0L) * 1_000L,
            streamUrl = "",
            videoUrl = "https://www.youtube.com/shorts/$id",
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = thumbnail,
            source = YOUTUBE_SHORTS_SOURCE,
            moodTags = setOf("shorts", "video"),
            energy = 72,
            vocal = 64,
            replayScore = 82,
            cacheScore = 70,
            accentStart = accentStart,
            accentEnd = accentEnd,
            counterpartVideoId = id,
            videoType = "SHORTS"
        )
    }
}

internal suspend fun <T> withShortsSearchTimeout(
    timeoutMs: Long = SHORTS_SEARCH_TIMEOUT_MS,
    block: suspend () -> T
): T = withTimeout(timeoutMs) { block() }

internal fun youtubeShortsRetryDelayMs(failureCount: Int): Long {
    val delays = longArrayOf(30_000L, 60_000L, 120_000L, 300_000L, 600_000L)
    return delays[(failureCount.coerceAtLeast(1) - 1).coerceAtMost(delays.lastIndex)]
}

internal fun isYoutubeShortTrack(track: Track): Boolean {
    return track.source.equals(YOUTUBE_SHORTS_SOURCE, ignoreCase = true) ||
        track.videoType.equals("SHORTS", ignoreCase = true) ||
        track.videoUrl.contains("/shorts/", ignoreCase = true)
}

internal fun isYoutubeShortCandidate(
    isShortFormContent: Boolean,
    url: String,
    durationSeconds: Long
): Boolean {
    if (durationSeconds !in 1L..MAX_SHORT_DURATION_SECONDS) return false
    return isShortFormContent || url.contains("/shorts/", ignoreCase = true)
}

private fun shortQueries(seeds: List<Track>, languageCode: String): List<String> {
    val seedQueries = seeds.asSequence()
        .filter { track -> track.title.isNotBlank() && track.artist.isNotBlank() }
        .take(7)
        .map { track -> "${track.artist} ${track.title} #shorts" }
        .toList()
    val localizedFallbacks = when (languageCode.lowercase(Locale.ROOT).substringBefore('-')) {
        "it" -> listOf("musica #shorts", "nuove canzoni #shorts", "hit italiane #shorts")
        "es" -> listOf("música #shorts", "nuevas canciones #shorts", "éxitos #shorts")
        "fr" -> listOf("musique #shorts", "nouvelles chansons #shorts", "tubes #shorts")
        "de" -> listOf("musik #shorts", "neue songs #shorts", "hits #shorts")
        else -> listOf("music #shorts", "new songs #shorts", "viral music #shorts")
    }
    return (seedQueries + localizedFallbacks)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(MAX_SHORT_QUERIES)
}

private fun youtubeVideoId(url: String): String {
    return YOUTUBE_VIDEO_ID_REGEX.find(url)?.groupValues?.getOrNull(1).orEmpty()
}

private fun shortPalette(id: String): Pair<Int, Int> {
    val palettes = arrayOf(
        0xFFFF2D55.toInt() to 0xFFFF7A00.toInt(),
        0xFF00D4FF.toInt() to 0xFF5B5FFF.toInt(),
        0xFF9C5CFF.toInt() to 0xFFFF4FD8.toInt(),
        0xFF00D68F.toInt() to 0xFF00A8FF.toInt(),
        0xFFFFC400.toInt() to 0xFFFF5C5C.toInt()
    )
    return palettes[(id.hashCode() and Int.MAX_VALUE) % palettes.size]
}

private val YOUTUBE_VIDEO_ID_REGEX =
    Regex("(?:v=|/shorts/|youtu\\.be/|/embed/)([A-Za-z0-9_-]{11})")
