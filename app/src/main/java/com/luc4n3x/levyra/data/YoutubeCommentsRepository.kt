package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.YoutubeComment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import timber.log.Timber
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private val COMMENTS_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal data class YoutubeCommentsPage(
    val videoId: String,
    val countText: String,
    val commentsDisabled: Boolean,
    val items: List<YoutubeComment>,
    val nextToken: String
)

internal sealed interface YoutubeCommentsResult {
    data class Available(val page: YoutubeCommentsPage) : YoutubeCommentsResult
    data object Disabled : YoutubeCommentsResult
    data class Failed(val cause: Throwable? = null) : YoutubeCommentsResult
}

/** Read-only YouTube comments integration backed by LevyraExtractor/NewPipeExtractor. */
internal class YoutubeCommentsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<RequestKey, CachedCommentsResult>()
    private val inFlight = ConcurrentHashMap<RequestKey, Deferred<YoutubeCommentsResult>>()

    suspend fun initial(videoId: String, forceRefresh: Boolean = false): YoutubeCommentsResult {
        val normalized = videoId.trim()
        if (!COMMENTS_VIDEO_ID.matches(normalized)) {
            return YoutubeCommentsResult.Failed(IllegalArgumentException("Invalid YouTube video id"))
        }
        return resolve(RequestKey(normalized, "initial"), forceRefresh = forceRefresh) {
            NewPipeRuntime.ensure()
            val url = youtubeUrl(normalized)
            val info = CommentsInfo.getInfo(ServiceList.YouTube, url)
                ?: return@resolve YoutubeCommentsResult.Failed()
            if (info.isCommentsDisabled) {
                YoutubeCommentsResult.Disabled
            } else {
                val initialPage = YoutubeCommentsPage(
                    videoId = normalized,
                    countText = info.commentsCountText.orEmpty().trim().take(MAX_COUNT_TEXT_LENGTH),
                    commentsDisabled = false,
                    items = info.relatedItems.orEmpty().mapNotNull(::toComment),
                    nextToken = info.nextPage?.id.orEmpty()
                )
                YoutubeCommentsResult.Available(
                    advancePastEmptyParsedPages(
                        videoId = normalized,
                        initialPage = initialPage
                    )
                )
            }
        }
    }

    suspend fun more(
        videoId: String,
        continuationToken: String,
        forceRefresh: Boolean = false
    ): YoutubeCommentsResult {
        val normalized = videoId.trim()
        val token = continuationToken.trim()
        if (!COMMENTS_VIDEO_ID.matches(normalized) || token.isBlank()) {
            return YoutubeCommentsResult.Failed(IllegalArgumentException("Invalid comments continuation"))
        }
        return resolve(RequestKey(normalized, "page:$token"), forceRefresh = forceRefresh) {
            NewPipeRuntime.ensure()
            val firstPage = fetchContinuationPage(normalized, token)
            YoutubeCommentsResult.Available(
                advancePastEmptyParsedPages(
                    videoId = normalized,
                    initialPage = firstPage,
                    firstRequestedToken = token
                )
            )
        }
    }

    suspend fun replies(
        videoId: String,
        continuationToken: String,
        forceRefresh: Boolean = false
    ): YoutubeCommentsResult = more(videoId, continuationToken, forceRefresh)

    fun close() {
        scope.cancel()
        inFlight.clear()
        cache.clear()
    }

    private suspend fun resolve(
        key: RequestKey,
        forceRefresh: Boolean = false,
        loader: suspend () -> YoutubeCommentsResult
    ): YoutubeCommentsResult {
        if (forceRefresh) cache.remove(key)
        val now = System.currentTimeMillis()
        cache[key]?.let { cached ->
            if (now < cached.expiresAtMs) return cached.result
            cache.remove(key, cached)
        }

        val created = scope.async(start = CoroutineStart.LAZY) {
            val result = withContext(Dispatchers.IO) {
                try {
                    loader()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    Timber.d(error, "YouTube comments unavailable for %s", key.videoId)
                    YoutubeCommentsResult.Failed(error)
                }
            }
            val ttlMs = when (result) {
                is YoutubeCommentsResult.Available -> POSITIVE_TTL_MS
                YoutubeCommentsResult.Disabled -> DISABLED_TTL_MS
                is YoutubeCommentsResult.Failed -> FAILURE_TTL_MS
            }
            cacheResult(key, result, ttlMs)
            result
        }
        created.invokeOnCompletion { inFlight.remove(key, created) }
        val shared = inFlight.putIfAbsent(key, created) ?: created
        if (shared === created) {
            created.start()
        } else {
            created.cancel()
        }
        return shared.await()
    }

    private fun cacheResult(key: RequestKey, result: YoutubeCommentsResult, ttlMs: Long) {
        val now = System.currentTimeMillis()
        if (cache.size >= MAX_CACHE_ENTRIES) {
            cache.entries.removeIf { now >= it.value.expiresAtMs }
            if (cache.size >= MAX_CACHE_ENTRIES) {
                cache.entries.minByOrNull { it.value.expiresAtMs }?.let { cache.remove(it.key, it.value) }
            }
        }
        cache[key] = CachedCommentsResult(result, now + ttlMs)
    }

    private fun fetchContinuationPage(videoId: String, continuationToken: String): YoutubeCommentsPage {
        val url = youtubeUrl(videoId)
        return CommentsInfo.getMoreItems(
            ServiceList.YouTube,
            url,
            Page(url, continuationToken)
        ).toCommentsPage(videoId)
    }

    /**
     * A legitimate YouTube page can contain only deleted, private, region-blocked or otherwise
     * unsupported comment renderers. Those renderers are intentionally dropped by the mapper, but
     * their valid continuation must not be mistaken for the end of the thread.
     *
     * Follow a small bounded number of empty parsed pages, reject repeated tokens, and preserve the
     * last continuation if the safety budget is exhausted so the UI can continue later.
     */
    private fun advancePastEmptyParsedPages(
        videoId: String,
        initialPage: YoutubeCommentsPage,
        firstRequestedToken: String = ""
    ): YoutubeCommentsPage {
        var page = initialPage
        val seenTokens = linkedSetOf<String>()
        if (firstRequestedToken.isNotBlank()) seenTokens += firstRequestedToken
        var hops = 0

        while (page.items.isEmpty()) {
            val nextToken = nextEmptyCommentsContinuation(
                parsedItemCount = page.items.size,
                nextToken = page.nextToken,
                seenTokens = seenTokens,
                hops = hops,
                maxHops = MAX_EMPTY_PAGE_HOPS
            ) ?: break
            seenTokens += nextToken
            hops++
            val nextPage = fetchContinuationPage(videoId, nextToken)
            page = nextPage.copy(countText = page.countText.ifBlank { nextPage.countText })
        }
        return page
    }

    private fun InfoItemsPage<CommentsInfoItem>.toCommentsPage(videoId: String): YoutubeCommentsPage =
        YoutubeCommentsPage(
            videoId = videoId,
            countText = "",
            commentsDisabled = false,
            items = items.orEmpty().mapNotNull(::toComment),
            nextToken = nextPage?.id.orEmpty()
        )

    private fun toComment(item: CommentsInfoItem): YoutubeComment? {
        val id = item.commentId.orEmpty().trim().take(MAX_COMMENT_ID_LENGTH)
        val text = item.commentText.orEmpty().trim().take(MAX_COMMENT_TEXT_LENGTH)
        val author = item.uploaderName.orEmpty().trim().take(MAX_AUTHOR_LENGTH)
        if (id.isBlank() || text.isBlank() || author.isBlank()) return null

        val textualLikes = item.textualLikeCount.orEmpty().trim().take(MAX_LIKE_TEXT_LENGTH)
        val numericLikes = item.likeCount.takeIf { it >= 0 }?.toString().orEmpty()
        return YoutubeComment(
            id = id,
            text = text,
            author = author,
            authorAvatarUrl = sanitizeYoutubeAvatarUrl(item.uploaderAvatarUrl.orEmpty()),
            authorUrl = sanitizeYoutubeAuthorUrl(item.uploaderUrl.orEmpty()),
            publishedText = item.textualUploadDate.orEmpty().trim().take(MAX_DATE_TEXT_LENGTH),
            likeCountText = textualLikes.ifBlank { numericLikes },
            pinned = item.isPinned,
            heartedByUploader = item.isHeartedByUploader,
            verifiedAuthor = item.isUploaderVerified,
            streamPositionSeconds = item.streamPosition,
            replyCount = item.replyCount.coerceAtLeast(0),
            replyToken = item.replies?.id.orEmpty()
        )
    }

    private fun youtubeUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    private data class RequestKey(val videoId: String, val page: String)
    private data class CachedCommentsResult(
        val result: YoutubeCommentsResult,
        val expiresAtMs: Long
    )

    private companion object {
        const val MAX_CACHE_ENTRIES = 256
        const val MAX_EMPTY_PAGE_HOPS = 6
        const val POSITIVE_TTL_MS = 5L * 60L * 1_000L
        const val DISABLED_TTL_MS = 30L * 60L * 1_000L
        const val FAILURE_TTL_MS = 45L * 1_000L
        const val MAX_COUNT_TEXT_LENGTH = 96
        const val MAX_COMMENT_ID_LENGTH = 256
        const val MAX_COMMENT_TEXT_LENGTH = 12_000
        const val MAX_AUTHOR_LENGTH = 200
        const val MAX_LIKE_TEXT_LENGTH = 40
        const val MAX_DATE_TEXT_LENGTH = 120
    }
}

internal fun nextEmptyCommentsContinuation(
    parsedItemCount: Int,
    nextToken: String,
    seenTokens: Set<String>,
    hops: Int,
    maxHops: Int
): String? {
    val normalized = nextToken.trim()
    return normalized.takeIf {
        parsedItemCount == 0 &&
            it.isNotBlank() &&
            it !in seenTokens &&
            hops < maxHops
    }
}

internal fun sanitizeYoutubeAvatarUrl(value: String): String = sanitizeYoutubeUrl(
    value = value,
    allowedHosts = setOf("yt3.ggpht.com", "yt3.googleusercontent.com"),
    allowedSuffixes = setOf(".ggpht.com", ".googleusercontent.com")
)

internal fun sanitizeYoutubeAuthorUrl(value: String): String = sanitizeYoutubeUrl(
    value = value,
    allowedHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com"),
    allowedSuffixes = emptySet()
)

private fun sanitizeYoutubeUrl(
    value: String,
    allowedHosts: Set<String>,
    allowedSuffixes: Set<String>
): String {
    val candidate = value.trim()
    if (candidate.isBlank() || candidate.length > 2_048) return ""
    return runCatching {
        val uri = URI(candidate)
        val host = uri.host?.lowercase().orEmpty()
        val allowedHost = host in allowedHosts || allowedSuffixes.any(host::endsWith)
        if (
            !uri.scheme.equals("https", ignoreCase = true) ||
            uri.userInfo != null ||
            uri.fragment != null ||
            uri.port !in setOf(-1, 443) ||
            !allowedHost
        ) {
            ""
        } else {
            uri.toASCIIString()
        }
    }.getOrDefault("")
}
