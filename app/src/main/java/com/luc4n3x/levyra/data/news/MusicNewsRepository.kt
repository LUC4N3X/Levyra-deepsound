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
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal data class MusicNewsSource(val name: String, val url: String)

internal object MusicNewsSources {
    private val pitchfork = MusicNewsSource("Pitchfork", "https://pitchfork.com/feed/feed-news/rss")
    private val english = listOf(
        pitchfork,
        MusicNewsSource("Stereogum", "https://stereogum.com/feed/"),
        MusicNewsSource("Consequence", "https://consequence.net/feed/"),
        MusicNewsSource("NME", "https://www.nme.com/news/music/feed")
    )
    private val table = mapOf(
        "en" to english,
        "it" to listOf(
            MusicNewsSource("Rockol", "https://www.rockol.it/rss/news"),
            MusicNewsSource("All Music Italia", "https://www.allmusicitalia.it/feed"),
            MusicNewsSource("Rolling Stone Italia", "https://www.rollingstone.it/musica/feed/"),
            pitchfork
        ),
        "de" to listOf(MusicNewsSource("Musikexpress", "https://www.musikexpress.de/feed/"), pitchfork),
        "fr" to listOf(MusicNewsSource("Les Inrocks", "https://www.lesinrocks.com/musique/feed/"), pitchfork),
        "es" to listOf(MusicNewsSource("Mondo Sonoro", "https://www.mondosonoro.com/feed/"), pitchfork),
        "ru" to listOf(MusicNewsSource("InterMedia", "https://www.intermedia.ru/rss/news.xml"), pitchfork)
    )

    val languages: Set<String> get() = table.keys

    fun key(code: String): String =
        code.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_').takeIf { it in table } ?: "en"

    fun forLanguage(code: String): List<MusicNewsSource> = table.getValue(key(code))
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pages = ConcurrentHashMap<String, MusicNewsPage>()
    private val inFlight = ConcurrentHashMap<String, Deferred<MusicNewsPage?>>()
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
                    fetchAll(MusicNewsSources.forLanguage(key))?.let { fresh -> keepFailedSources(fresh, usable) }?.also { page ->
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

    private suspend fun fetchAll(sources: List<MusicNewsSource>): MusicNewsPage? = coroutineScope {
        val stories = sources.map { source -> async { fetchSource(source) } }.awaitAll().flatten()
        merge(stories).takeIf { it.isNotEmpty() }?.let { MusicNewsPage(it, System.currentTimeMillis()) }
    }

    private fun fetchSource(source: MusicNewsSource): List<MusicNewsStory> = try {
        fetchXml(source.url)?.let { MusicNewsFeedParser.parse(it, source.name) }.orEmpty()
    } catch (error: IOException) {
        Timber.w(error, "Music news feed %s failed", source.name)
        emptyList()
    } catch (error: RuntimeException) {
        Timber.w(error, "Music news feed %s failed", source.name)
        emptyList()
    }

    private fun fetchXml(url: String): String? {
        if (!MusicNewsFeedParser.isHttps(url)) return null
        val host = url.substringAfter("://").substringBefore('/')
        val backoff = backoffs.computeIfAbsent(host) { AppleCatalogBackoff() }
        if (backoff.blocked(System.currentTimeMillis())) return null
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
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
        private const val HTTP_TOO_MANY_REQUESTS = 429
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
