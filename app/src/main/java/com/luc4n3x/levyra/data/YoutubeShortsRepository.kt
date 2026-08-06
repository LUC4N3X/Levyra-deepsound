package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale

internal const val YOUTUBE_SHORTS_SOURCE = "YouTube Shorts"
private const val MAX_SHORT_QUERIES = 10
private const val SHORTS_SEARCH_CONCURRENCY = 3
private const val SHORTS_PER_QUERY = 4
private const val MAX_SHORT_DURATION_SECONDS = 180L

/**
 * Dedicated short-form feed. Like LibreTube, this trusts NewPipe's
 * StreamInfoItem.isShortFormContent flag instead of guessing from titles.
 */
internal class YoutubeShortsRepository(private val context: Context) {
    suspend fun feed(
        seeds: List<Track>,
        languageCode: String,
        limit: Int = 24
    ): List<Track> = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext emptyList()
        val queries = shortQueries(seeds, languageCode)
        if (queries.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val semaphore = Semaphore(SHORTS_SEARCH_CONCURRENCY)
            queries.map { query ->
                async {
                    semaphore.withPermit {
                        runCatching { searchShorts(query, SHORTS_PER_QUERY) }
                            .getOrDefault(emptyList())
                    }
                }
            }.awaitAll()
        }
            .asSequence()
            .flatten()
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(limit)
            .toList()
    }

    private fun searchShorts(query: String, limit: Int): List<Track> {
        NewPipeRuntime.ensure(context)
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(query)
        val info = SearchInfo.getInfo(service, handler)
        return info.relatedItems
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
