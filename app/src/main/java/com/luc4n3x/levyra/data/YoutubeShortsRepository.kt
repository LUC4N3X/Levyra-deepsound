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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.ChannelTabInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale

internal const val YOUTUBE_SHORTS_SOURCE = "YouTube Shorts"
private const val MAX_SHORT_QUERIES = 4
private const val MAX_SHORT_CHANNELS = 4
private const val SHORTS_SEARCH_CONCURRENCY = 4
private const val SHORTS_CHANNEL_CONCURRENCY = 4
private const val SHORTS_PER_QUERY = 8
private const val SHORTS_PER_CHANNEL = 8
private const val MAX_SHORT_DURATION_SECONDS = 180L
private const val SHORTS_SEARCH_TIMEOUT_MS = 5_500L
private const val SHORTS_CHANNEL_TIMEOUT_MS = 6_500L
private const val YOUTUBE_FRONTEND = "https://www.youtube.com"
private val YOUTUBE_CHANNEL_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")
private val YOUTUBE_CHANNEL_PATH_PREFIXES = setOf("channel", "c", "user")

internal data class YoutubeShortsFeedResult(
    val tracks: List<Track>,
    val completedQueries: Int,
    val failedQueries: Int
) {
    val isConclusive: Boolean
        get() = completedQueries > 0
}

private data class ShortsDiscovery(
    val tracks: List<Track> = emptyList(),
    val channelUrls: List<String> = emptyList()
)

private sealed interface ShortsSourceResult {
    data class Success(val discovery: ShortsDiscovery) : ShortsSourceResult
    data class Failure(val error: Throwable) : ShortsSourceResult
}

internal class YoutubeShortsRepository(private val context: Context) {
    suspend fun feed(
        seeds: List<Track>,
        languageCode: String,
        preferredArtists: List<String> = emptyList(),
        preferredChannelIds: List<String> = emptyList(),
        limit: Int = 24
    ): YoutubeShortsFeedResult = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext YoutubeShortsFeedResult(emptyList(), 0, 0)

        val queries = youtubeShortQueries(seeds, preferredArtists, languageCode)
            .take(MAX_SHORT_QUERIES)
        val directChannelUrls = youtubeShortChannelUrls(seeds, preferredChannelIds)
        val (directChannelResults, searchResults) = coroutineScope {
            val channels = async { runChannelDiscovery(directChannelUrls) }
            val searches = async { runSearchDiscovery(queries) }
            channels.await() to searches.await()
        }
        val successfulSearches = searchResults.filterIsInstance<ShortsSourceResult.Success>()
        val discoveredChannelUrls = successfulSearches
            .asSequence()
            .flatMap { result -> result.discovery.channelUrls.asSequence() }
            .filterNot { channelUrl -> channelUrl in directChannelUrls }
            .distinct()
            .take((MAX_SHORT_CHANNELS - directChannelUrls.size).coerceAtLeast(0))
            .toList()
        val discoveredChannelResults = runChannelDiscovery(discoveredChannelUrls)
        val channelResults = directChannelResults + discoveredChannelResults
        val successfulChannels = channelResults.filterIsInstance<ShortsSourceResult.Success>()
        val tracks = mergeShortTracks(
            channelResults = successfulChannels,
            searchResults = successfulSearches,
            limit = limit
        )

        YoutubeShortsFeedResult(
            tracks = tracks,
            completedQueries = successfulSearches.size + successfulChannels.size,
            failedQueries = searchResults.count { it is ShortsSourceResult.Failure } +
                channelResults.count { it is ShortsSourceResult.Failure }
        )
    }

    private fun mergeShortTracks(
        channelResults: List<ShortsSourceResult.Success>,
        searchResults: List<ShortsSourceResult.Success>,
        limit: Int
    ): List<Track> {
        val channelTracks = channelResults.flatMap { result -> result.discovery.tracks }
        val searchTracks = searchResults.flatMap { result -> result.discovery.tracks }
        return buildList {
            repeat(maxOf(channelTracks.size, searchTracks.size)) { index ->
                channelTracks.getOrNull(index)?.let(::add)
                searchTracks.getOrNull(index)?.let(::add)
            }
        }
            .asSequence()
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(limit)
            .toList()
    }

    private suspend fun runSearchDiscovery(queries: List<String>): List<ShortsSourceResult> {
        if (queries.isEmpty()) return emptyList()
        return coroutineScope {
            val semaphore = Semaphore(SHORTS_SEARCH_CONCURRENCY)
            queries.map { query ->
                async {
                    semaphore.withPermit {
                        safely { searchShorts(query, SHORTS_PER_QUERY) }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun runChannelDiscovery(channelUrls: List<String>): List<ShortsSourceResult> {
        if (channelUrls.isEmpty()) return emptyList()
        return coroutineScope {
            val semaphore = Semaphore(SHORTS_CHANNEL_CONCURRENCY)
            channelUrls.map { channelUrl ->
                async {
                    semaphore.withPermit {
                        safely { channelShorts(channelUrl, SHORTS_PER_CHANNEL) }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun safely(block: suspend () -> ShortsDiscovery): ShortsSourceResult {
        return try {
            ShortsSourceResult.Success(block())
        } catch (error: TimeoutCancellationException) {
            ShortsSourceResult.Failure(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            ShortsSourceResult.Failure(error)
        }
    }

    private suspend fun searchShorts(query: String, limit: Int): ShortsDiscovery {
        return withShortsSearchTimeout {
            runInterruptible(Dispatchers.IO) {
                NewPipeRuntime.ensure(context)
                val service = ServiceList.YouTube
                val handler = service.searchQHFactory.fromQuery(query)
                val info = SearchInfo.getInfo(service, handler)
                val streamItems = info.relatedItems.filterIsInstance<StreamInfoItem>()
                val tracks = streamItems
                    .asSequence()
                    .filter(::isShortCandidate)
                    .mapNotNull(::shortTrack)
                    .distinctBy { track -> track.id }
                    .take(limit)
                    .toList()
                val channelUrls = buildList {
                    info.relatedItems.filterIsInstance<ChannelInfoItem>().forEach { item ->
                        canonicalYoutubeChannelUrl(item.url)?.let(::add)
                    }
                    streamItems.forEach { item ->
                        canonicalYoutubeChannelUrl(item.uploaderUrl)?.let(::add)
                    }
                }.distinct()
                ShortsDiscovery(tracks = tracks, channelUrls = channelUrls)
            }
        }
    }

    private suspend fun channelShorts(channelUrl: String, limit: Int): ShortsDiscovery {
        return withTimeout(SHORTS_CHANNEL_TIMEOUT_MS) {
            runInterruptible(Dispatchers.IO) {
                NewPipeRuntime.ensure(context)
                val service = ServiceList.YouTube
                val shortsUrl = channelUrl.trimEnd('/') + "/shorts"
                val handler = service.channelTabLHFactory.fromUrl(shortsUrl)
                val tabInfo = ChannelTabInfo.getInfo(service, handler)
                val tracks = tabInfo.relatedItems
                    .asSequence()
                    .filterIsInstance<StreamInfoItem>()
                    .filter(::isShortCandidate)
                    .mapNotNull(::shortTrack)
                    .distinctBy { track -> track.id }
                    .take(limit)
                    .toList()
                ShortsDiscovery(tracks = tracks)
            }
        }
    }

    private fun isShortCandidate(item: StreamInfoItem): Boolean {
        return isYoutubeShortCandidate(
            isShortFormContent = item.isShortFormContent,
            url = item.url,
            durationSeconds = item.duration
        )
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
            videoUrl = "$YOUTUBE_FRONTEND/shorts/$id",
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
    val verifiedShort = isShortFormContent || url.contains("/shorts/", ignoreCase = true)
    if (!verifiedShort) return false
    return durationSeconds <= 0L || durationSeconds <= MAX_SHORT_DURATION_SECONDS
}

internal fun youtubeShortQueries(
    seeds: List<Track>,
    preferredArtists: List<String>,
    languageCode: String
): List<String> {
    val followedArtistQueries = preferredArtists
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { artist -> artist.lowercase(Locale.ROOT) }
        .take(6)
        .map { artist -> "$artist #shorts" }
        .toList()
    val seedArtistQueries = seeds
        .asSequence()
        .map { track -> track.artist.trim() }
        .filter(String::isNotBlank)
        .distinctBy { artist -> artist.lowercase(Locale.ROOT) }
        .take(6)
        .map { artist -> "$artist #shorts" }
        .toList()
    val songQueries = seeds
        .asSequence()
        .filter { track -> track.title.isNotBlank() && track.artist.isNotBlank() }
        .distinctBy { track -> "${track.artist.lowercase(Locale.ROOT)}|${track.title.lowercase(Locale.ROOT)}" }
        .take(4)
        .map { track -> "${track.artist} ${track.title} #shorts" }
        .toList()

    val personalized = (followedArtistQueries + seedArtistQueries + songQueries)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(2)
    val localizedLimit = if (personalized.isEmpty()) MAX_SHORT_QUERIES else 2
    val localized = localizedShortQueries(languageCode)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(localizedLimit)
    return buildList {
        repeat(maxOf(personalized.size, localized.size)) { index ->
            personalized.getOrNull(index)?.let(::add)
            localized.getOrNull(index)?.let(::add)
        }
    }.take(MAX_SHORT_QUERIES)
}

internal fun youtubeShortChannelUrls(
    seeds: List<Track>,
    preferredChannelIds: List<String>
): List<String> {
    return (preferredChannelIds.asSequence() + seeds.asSequence().flatMap { track -> track.artistBrowseIds.asSequence() })
        .mapNotNull(::canonicalYoutubeChannelUrl)
        .distinct()
        .take(MAX_SHORT_CHANNELS)
        .toList()
}

internal fun canonicalYoutubeChannelUrl(value: String): String? {
    val candidate = value.trim()
    if (candidate.isBlank()) return null
    if (candidate.startsWith("UC") && candidate.length >= 20 && '/' !in candidate) {
        return "$YOUTUBE_FRONTEND/channel/$candidate"
    }
    val absolute = when {
        candidate.startsWith("https://", ignoreCase = true) -> candidate
        candidate.startsWith("/") -> "$YOUTUBE_FRONTEND$candidate"
        else -> return null
    }
    val parsed = absolute.toHttpUrlOrNull() ?: return null
    if (!parsed.isHttps || parsed.port != 443) return null
    if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return null
    if (parsed.host.lowercase(Locale.ROOT) !in YOUTUBE_CHANNEL_HOSTS) return null
    val segments = parsed.pathSegments.filter(String::isNotBlank)
    val head = segments.firstOrNull() ?: return null
    if (head.startsWith("@")) {
        return if (head.length > 1) "$YOUTUBE_FRONTEND/$head" else null
    }
    val prefix = head.lowercase(Locale.ROOT).takeIf { it in YOUTUBE_CHANNEL_PATH_PREFIXES } ?: return null
    val identifier = segments.getOrNull(1)?.takeIf(String::isNotBlank) ?: return null
    return "$YOUTUBE_FRONTEND/$prefix/$identifier"
}

private fun localizedShortQueries(languageCode: String): List<String> {
    return when (languageCode.lowercase(Locale.ROOT).substringBefore('-')) {
        "en" -> listOf("music shorts USA", "new songs USA #shorts", "viral English music #shorts", "popular music USA #shorts")
        "it" -> listOf("shorts musica italiana", "canzoni del momento #shorts", "nuove hit italiane #shorts", "musica virale #shorts")
        "es" -> listOf("shorts música española", "canciones del momento #shorts", "éxitos latinos #shorts", "música viral #shorts")
        "fr" -> listOf("shorts musique française", "chansons du moment #shorts", "nouveaux tubes #shorts", "musique virale #shorts")
        "de" -> listOf("shorts deutsche musik", "aktuelle deutsche songs #shorts", "neue hits #shorts", "virale musik #shorts")
        "pt" -> listOf("shorts música brasileira", "músicas do momento #shorts", "novos sucessos #shorts", "música viral #shorts")
        "nl" -> listOf("shorts Nederlandse muziek", "Nederlandse hits #shorts", "nieuwe muziek Nederland #shorts", "virale muziek #shorts")
        "pl" -> listOf("shorts polska muzyka", "polskie hity #shorts", "nowa polska muzyka #shorts", "viral muzyka #shorts")
        "ro" -> listOf("shorts muzică românească", "hituri românești #shorts", "muzică nouă România #shorts", "muzică virală #shorts")
        "el" -> listOf("ελληνική μουσική #shorts", "ελληνικές επιτυχίες #shorts", "νέα ελληνικά τραγούδια #shorts", "viral μουσική #shorts")
        "sv" -> listOf("shorts svensk musik", "svenska hits #shorts", "ny svensk musik #shorts", "viral musik #shorts")
        "da" -> listOf("shorts dansk musik", "danske hits #shorts", "ny dansk musik #shorts", "viral musik #shorts")
        "cs" -> listOf("shorts česká hudba", "české hity #shorts", "nová česká hudba #shorts", "virální hudba #shorts")
        "uk" -> listOf("українська музика #shorts", "українські хіти #shorts", "нові українські пісні #shorts", "вірусна музика #shorts")
        "ru" -> listOf("русская музыка #shorts", "русские хиты #shorts", "новые русские песни #shorts", "вирусная музыка #shorts")
        "tr" -> listOf("Türkçe müzik #shorts", "Türkçe hitler #shorts", "yeni Türkçe şarkılar #shorts", "viral müzik #shorts")
        "ar" -> listOf("موسيقى عربية #shorts", "أغاني عربية رائجة #shorts", "أغاني عربية جديدة #shorts", "موسيقى viral #shorts")
        "zh" -> listOf("华语音乐 #shorts", "华语热门歌曲 #shorts", "华语新歌 #shorts", "热门音乐 #shorts")
        "ja" -> listOf("音楽 #shorts", "新曲 #shorts", "人気曲 #shorts", "j-pop #shorts")
        "ko" -> listOf("음악 #shorts", "신곡 #shorts", "인기곡 #shorts", "k-pop #shorts")
        "hi" -> listOf("हिंदी संगीत #shorts", "नए हिंदी गाने #shorts", "बॉलीवुड हिट्स #shorts", "वायरल संगीत #shorts")
        "id" -> listOf("musik Indonesia #shorts", "lagu Indonesia terbaru #shorts", "hit Indonesia #shorts", "musik viral #shorts")
        "vi" -> listOf("nhạc Việt #shorts", "bài hát Việt mới #shorts", "hit Việt Nam #shorts", "nhạc viral #shorts")
        "th" -> listOf("เพลงไทย #shorts", "เพลงไทยใหม่ #shorts", "เพลงฮิตไทย #shorts", "เพลงไวรัล #shorts")
        "fil" -> listOf("OPM #shorts", "bagong kantang Pilipino #shorts", "Pinoy hits #shorts", "viral music Pilipinas #shorts")
        "he" -> listOf("מוזיקה ישראלית #shorts", "להיטים ישראליים #shorts", "שירים ישראליים חדשים #shorts", "מוזיקה ויראלית #shorts")
        else -> listOf("music #shorts", "songs right now #shorts", "new music #shorts", "viral music #shorts")
    }
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
