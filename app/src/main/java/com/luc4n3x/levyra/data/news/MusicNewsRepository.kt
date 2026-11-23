package com.luc4n3x.levyra.data.news

import android.content.Context
import com.luc4n3x.levyra.data.apple.AppleCatalogBackoff
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.json.JSONException
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal enum class MusicNewsArticleApi { None, WordPress, PreloadedState, JsonLdBody }

internal data class MusicNewsArticleRequest(
    val url: String,
    val api: MusicNewsArticleApi,
    val bodyMarker: String = "",
    val bodyEnd: String = ""
)

internal data class MusicNewsSource(
    val name: String,
    val url: String,
    val articleApi: MusicNewsArticleApi = MusicNewsArticleApi.None,
    val apiHost: String = "",
    val bodyMarker: String = "",
    val bodyEnd: String = ""
)

internal object MusicNewsSources {
    private val pitchfork = MusicNewsSource("Pitchfork", "https://pitchfork.com/feed/feed-news/rss", MusicNewsArticleApi.PreloadedState)
    private val english = listOf(
        pitchfork,
        MusicNewsSource("Stereogum", "https://stereogum.com/feed/", MusicNewsArticleApi.WordPress, apiHost = "lede-admin.stereogum.com"),
        MusicNewsSource("Consequence", "https://consequence.net/feed/", MusicNewsArticleApi.WordPress),
        MusicNewsSource("NME", "https://www.nme.com/news/music/feed", MusicNewsArticleApi.WordPress)
    )
    private val table = mapOf(
        "en" to english,
        "it" to listOf(
            MusicNewsSource("Rockol", "https://www.rockol.it/rss/news", MusicNewsArticleApi.JsonLdBody, bodyMarker = "data-rockol-news-detail-body"),
            MusicNewsSource("All Music Italia", "https://www.allmusicitalia.it/feed", MusicNewsArticleApi.WordPress),
            MusicNewsSource("Rolling Stone Italia", "https://www.rollingstone.it/musica/feed/", MusicNewsArticleApi.WordPress),
            pitchfork
        ),
        "de" to listOf(MusicNewsSource("Musikexpress", "https://www.musikexpress.de/feed/", MusicNewsArticleApi.WordPress), pitchfork),
        "fr" to listOf(MusicNewsSource("Les Inrocks", "https://www.lesinrocks.com/musique/feed/", MusicNewsArticleApi.WordPress), pitchfork),
        "es" to listOf(MusicNewsSource("Mondo Sonoro", "https://www.mondosonoro.com/feed/", MusicNewsArticleApi.WordPress), pitchfork),
        "ru" to listOf(MusicNewsSource(
            "InterMedia",
            "https://www.intermedia.ru/rss/news.xml",
            MusicNewsArticleApi.JsonLdBody,
            bodyMarker = "class=\"news-content\"",
            bodyEnd = "class=\"clear\""
        ), pitchfork)
    )

    val languages: Set<String> get() = table.keys

    fun key(code: String): String =
        code.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_').takeIf { it in table } ?: "en"

    fun forLanguage(code: String): List<MusicNewsSource> = table.getValue(key(code))

    fun articleRequest(story: MusicNewsStory): MusicNewsArticleRequest? {
        val host = hostOf(story.url) ?: return null
        val source = table.values.asSequence().flatten()
            .firstOrNull { it.articleApi != MusicNewsArticleApi.None && it.name == story.source && hostOf(it.url) == host }
            ?: return null
        return when (source.articleApi) {
            MusicNewsArticleApi.WordPress -> story.articleId.takeIf { it.isNotBlank() && it.all(Char::isDigit) }?.let { id ->
                MusicNewsArticleRequest(
                    "https://${source.apiHost.ifBlank { host }}/wp-json/wp/v2/posts/$id" +
                        "?_fields=id,title,date_gmt,content,_links,_embedded&_embed=author,wp:featuredmedia",
                    MusicNewsArticleApi.WordPress
                )
            }
            MusicNewsArticleApi.PreloadedState -> MusicNewsArticleRequest(story.url, MusicNewsArticleApi.PreloadedState)
            MusicNewsArticleApi.JsonLdBody ->
                MusicNewsArticleRequest(story.url, MusicNewsArticleApi.JsonLdBody, source.bodyMarker, source.bodyEnd)
            MusicNewsArticleApi.None -> null
        }
    }

    private fun hostOf(url: String): String? =
        url.takeIf(MusicNewsFeedParser::isHttps)?.substringAfter("://")?.substringBefore('/')?.substringBefore('?')
            ?.lowercase(Locale.ROOT)?.takeIf(String::isNotBlank)
}

class MusicNewsRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy {
        LevyraHttpClientFactory.feeds(appContext).newBuilder()
            .addNetworkInterceptor { chain ->
                if (!chain.request().url.isHttps) throw IOException("Insecure news redirect rejected")
                chain.proceed(chain.request())
            }
            .build()
    }
    private val articleClient: OkHttpClient by lazy {
        httpClient.newBuilder().callTimeout(ARTICLE_CALL_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS).build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pages = ConcurrentHashMap<String, MusicNewsPage>()
    private val inFlight = ConcurrentHashMap<String, Deferred<MusicNewsPage?>>()
    private val articles: MutableMap<String, MusicNewsArticle> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, MusicNewsArticle>(MAX_CACHED_ARTICLES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MusicNewsArticle>?): Boolean =
                size > MAX_CACHED_ARTICLES
        }
    )
    private val backoffs = ConcurrentHashMap<String, AppleCatalogBackoff>()
    private val cacheDirectory by lazy { File(appContext.cacheDir, CACHE_DIRECTORY) }

    suspend fun cached(languageCode: String): List<MusicNewsStory>? =
        usable(MusicNewsSources.key(languageCode), System.currentTimeMillis())?.stories

    suspend fun load(languageCode: String, force: Boolean = false): List<MusicNewsStory>? {
        val key = MusicNewsSources.key(languageCode)
        val now = System.currentTimeMillis()
        val usable = usable(key, now)
        if (usable != null && !force && now - usable.fetchedAtMs < TTL_MS) return usable.stories
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    fetchAll(MusicNewsSources.forLanguage(key), usable?.stories.orEmpty())?.let { fresh -> keepFailedSources(fresh, usable) }?.also { page ->
                        pages[key] = page
                        writeText(key, MusicNewsCodec.encode(page))
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return awaitOrNull(shared)?.stories ?: usable?.stories
    }

    suspend fun article(story: MusicNewsStory): MusicNewsArticle? = withContext(Dispatchers.IO) {
        articles[story.url]?.let { return@withContext it }
        val request = MusicNewsSources.articleRequest(story) ?: return@withContext null
        try {
            when (request.api) {
                MusicNewsArticleApi.WordPress -> fetchText(request.url, "application/json")
                    ?.let { MusicNewsArticleParser.wordpress(it, story) }
                MusicNewsArticleApi.PreloadedState -> fetchText(request.url, "text/html")
                    ?.let { MusicNewsArticleParser.preloadedState(it, story) }
                MusicNewsArticleApi.JsonLdBody -> fetchText(request.url, "text/html")
                    ?.let { MusicNewsArticleParser.jsonLdBody(it, story, request.bodyMarker, request.bodyEnd) }
                MusicNewsArticleApi.None -> null
            }
                ?.also {
                    articles[story.url] = it
                }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: IOException) {
            Timber.w(error, "Music news article %s failed", story.source)
            null
        } catch (error: JSONException) {
            Timber.w(error, "Music news article %s failed", story.source)
            null
        } catch (error: RuntimeException) {
            Timber.w(error, "Music news article %s failed", story.source)
            null
        }
    }

    private fun keepFailedSources(fresh: MusicNewsPage, previous: MusicNewsPage?): MusicNewsPage {
        val refreshed = fresh.stories.mapTo(HashSet()) { it.source }
        val oldest = fresh.fetchedAtMs - MAX_STALE_MS
        val kept = previous?.stories.orEmpty().filter { it.source !in refreshed && it.publishedAtMs > oldest }
        return if (kept.isEmpty()) fresh else MusicNewsPage(merge(fresh.stories + kept), fresh.fetchedAtMs)
    }

    private suspend fun usable(key: String, now: Long): MusicNewsPage? {
        val page = pages[key] ?: withContext(Dispatchers.IO) { readText(key)?.let(MusicNewsCodec::decode) }?.also { pages[key] = it }
        return page?.takeIf { now - it.fetchedAtMs < MAX_STALE_MS && it.stories.isNotEmpty() }
    }

    private suspend fun fetchAll(sources: List<MusicNewsSource>, previous: List<MusicNewsStory>): MusicNewsPage? = coroutineScope {
        val stories = sources.map { source -> async { fetchSource(source) } }.awaitAll().flatten()
        merge(stories).takeIf { it.isNotEmpty() }
            ?.let { withArticleImages(it, previous) }
            ?.let { MusicNewsPage(it, System.currentTimeMillis()) }
    }

    private suspend fun withArticleImages(stories: List<MusicNewsStory>, previous: List<MusicNewsStory>): List<MusicNewsStory> = coroutineScope {
        val known = previous.filter { it.imageUrl.isNotBlank() }.associate { it.url to it.imageUrl }
        val lookups = stories.filter { it.imageUrl.isBlank() && it.url !in known }
            .take(MAX_ARTICLE_IMAGE_LOOKUPS)
            .map { story -> story.url to async { articleImage(story.url) } }
            .toMap()
        stories.map { story ->
            when {
                story.imageUrl.isNotBlank() -> story
                known[story.url] != null -> story.copy(imageUrl = known.getValue(story.url))
                else -> lookups[story.url]?.await()?.takeIf(String::isNotBlank)?.let { story.copy(imageUrl = it) } ?: story
            }
        }
    }

    private fun articleImage(url: String): String = try {
        if (!MusicNewsFeedParser.isHttps(url)) {
            ""
        } else {
            val request = Request.Builder().url(url).header("Accept", "text/html").header("User-Agent", USER_AGENT).build()
            articleClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful || !response.request.url.isHttps) return@use ""
                val source = response.body.source()
                val buffer = Buffer()
                var exhausted = false
                while (!exhausted && buffer.size < MAX_ARTICLE_HEAD_BYTES) {
                    exhausted = source.read(buffer, READ_CHUNK_BYTES) == -1L
                }
                MusicNewsFeedParser.articleImage(buffer.readString(response.body.contentType()?.charset() ?: Charsets.UTF_8))
            }
        }
    } catch (error: IOException) {
        Timber.w(error, "Music news article image failed")
        ""
    } catch (error: RuntimeException) {
        Timber.w(error, "Music news article image failed")
        ""
    }

    private fun fetchSource(source: MusicNewsSource): List<MusicNewsStory> = try {
        fetchText(source.url, FEED_ACCEPT)?.let { MusicNewsFeedParser.parse(it, source.name) }.orEmpty()
    } catch (error: IOException) {
        Timber.w(error, "Music news feed %s failed", source.name)
        emptyList()
    } catch (error: RuntimeException) {
        Timber.w(error, "Music news feed %s failed", source.name)
        emptyList()
    }

    private fun fetchText(url: String, accept: String): String? {
        if (!MusicNewsFeedParser.isHttps(url)) return null
        val host = url.substringAfter("://").substringBefore('/')
        val backoff = backoffs.computeIfAbsent(host) { AppleCatalogBackoff() }
        if (backoff.blocked(System.currentTimeMillis())) return null
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("User-Agent", USER_AGENT)
            .build()
        return httpClient.newCall(request).execute().use { response ->
            when {
                !response.request.url.isHttps -> null
                response.code == HTTP_TOO_MANY_REQUESTS || response.code == HTTP_SERVICE_UNAVAILABLE -> {
                    backoff.onRateLimited(System.currentTimeMillis(), response.header("Retry-After"))
                    null
                }
                !response.isSuccessful -> {
                    Timber.w("Music news feed HTTP %d", response.code)
                    null
                }
                else -> {
                    backoff.onSuccess()
                    val source = response.body.source()
                    val buffer = Buffer()
                    var exhausted = false
                    while (!exhausted && buffer.size < MAX_FEED_BYTES) {
                        exhausted = source.read(buffer, READ_CHUNK_BYTES) == -1L
                    }
                    if (buffer.size >= MAX_FEED_BYTES) null
                    else buffer.readString(response.body.contentType()?.charset() ?: Charsets.UTF_8)
                }
            }
        }
    }

    private suspend fun <T> awaitOrNull(deferred: Deferred<T?>): T? = try {
        deferred.await()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Timber.w(error, "Music news request failed")
        null
    }

    private fun readText(key: String): String? = runCatching {
        File(cacheDirectory, "$key.json").takeIf { it.isFile }?.readText()
    }.getOrNull()

    private fun writeText(key: String, content: String) {
        runCatching {
            cacheDirectory.mkdirs()
            val target = File(cacheDirectory, "$key.json")
            val temporary = File(cacheDirectory, "$key.json.tmp")
            temporary.writeText(content)
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        }.onFailure { Timber.w(it, "Music news cache write failed") }
    }

    companion object {
        private const val CACHE_DIRECTORY = "music-news"
        private const val USER_AGENT = "Levyra"
        private const val TTL_MS = 30L * 60L * 1000L
        private const val MAX_STALE_MS = 3L * 24L * 60L * 60L * 1000L
        private const val MAX_FEED_BYTES = 2L * 1024L * 1024L
        private const val READ_CHUNK_BYTES = 64L * 1024L
        private const val MAX_ARTICLE_HEAD_BYTES = 192L * 1024L
        private const val MAX_ARTICLE_IMAGE_LOOKUPS = 16
        private const val ARTICLE_CALL_TIMEOUT_SECONDS = 6L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val MAX_CACHED_ARTICLES = 24
        private const val FEED_ACCEPT = "application/rss+xml, application/atom+xml, application/xml, text/xml"
        private const val HTTP_SERVICE_UNAVAILABLE = 503

        internal fun merge(stories: List<MusicNewsStory>): List<MusicNewsStory> {
            val titles = HashSet<String>()
            return stories.asSequence()
                .distinctBy { it.url }
                .filter { titles.add(it.title.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit).ifBlank { it.url }) }
                .sortedByDescending { it.publishedAtMs }
                .take(MusicNewsFeedParser.MAX_STORIES)
                .toList()
        }

        @Volatile
        private var instance: MusicNewsRepository? = null

        fun get(context: Context): MusicNewsRepository =
            instance ?: synchronized(this) {
                instance ?: MusicNewsRepository(context).also { instance = it }
            }
    }
}
