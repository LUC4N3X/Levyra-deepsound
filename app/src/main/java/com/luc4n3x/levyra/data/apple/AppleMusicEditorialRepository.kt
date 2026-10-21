package com.luc4n3x.levyra.data.apple

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class AppleCatalogFreshness { Fresh, Stale }

data class AppleCatalogResult(
    val page: AppleCatalogPage,
    val freshness: AppleCatalogFreshness
)

data class AppleCatalogPlaylist(
    val id: String,
    val title: String,
    val curator: String,
    val description: String,
    val artworkUrl: String,
    val tracks: List<Track>,
    val fetchedAtMs: Long
)

internal enum class AppleCatalogPath(val segment: String) {
    New("new"),
    TopCharts("new/top-charts")
}

class AppleMusicEditorialRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy {
        LevyraHttpClientFactory.feeds(appContext).newBuilder()
            .addNetworkInterceptor { chain ->
                if (!AppleCatalogIds.isCatalogUrl(chain.request().url.toString())) throw java.io.IOException("Apple catalog redirect rejected")
                chain.proceed(chain.request())
            }
            .build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pages = ConcurrentHashMap<String, AppleCatalogPage>()
    private val playlists = ConcurrentHashMap<String, AppleCatalogPlaylist>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Any?>>()
    private val cacheDirectory by lazy { File(appContext.cacheDir, CACHE_DIRECTORY) }
    private val backoff = AppleCatalogBackoff()

    suspend fun newMusic(storefront: String, language: String): AppleCatalogResult? = page(AppleCatalogPath.New, storefront, language)

    suspend fun topCharts(storefront: String, language: String): AppleCatalogResult? = page(AppleCatalogPath.TopCharts, storefront, language)

    suspend fun cachedNewMusic(storefront: String, language: String): AppleCatalogResult? = cachedPage(AppleCatalogPath.New, storefront, language)

    suspend fun cachedTopCharts(storefront: String, language: String): AppleCatalogResult? = cachedPage(AppleCatalogPath.TopCharts, storefront, language)

    suspend fun playlist(storefront: String, playlistId: String, language: String): AppleCatalogPlaylist? {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val id = AppleCatalogIds.playlist(playlistId) ?: return null
        val key = "playlist:$front:$lang:$id"
        playlists[key]?.takeIf { System.currentTimeMillis() - it.fetchedAtMs < PLAYLIST_TTL_MS }?.let { return it }
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    fetchPlaylist(front, id, lang)?.also { playlists[key] = it }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<AppleCatalogPlaylist?>
        return awaitOrNull(shared) ?: playlists[key]
    }

    private suspend fun cachedPage(path: AppleCatalogPath, storefront: String, language: String): AppleCatalogResult? = withContext(Dispatchers.IO) {
        val front = AppleStorefronts.normalize(storefront)
        val key = pageKey(path, front, AppleStorefronts.language(language))
        val now = System.currentTimeMillis()
        val page = pages[key] ?: readStored(key, front)?.also { pages[key] = it }
        page?.takeIf { now - it.fetchedAtMs < MAX_STALE_MS }?.let { AppleCatalogResult(it, freshnessOf(it, now)) }
    }

    private suspend fun page(path: AppleCatalogPath, storefront: String, language: String): AppleCatalogResult? {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val key = pageKey(path, front, lang)
        val cached = cachedPage(path, front, lang)
        if (cached?.freshness == AppleCatalogFreshness.Fresh) return cached
        if (backoff.blocked(System.currentTimeMillis())) return cached
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    fetchPage(path, front, lang)?.also { page ->
                        pages[key] = page
                        persist(key, page)
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<AppleCatalogPage?>
        val fresh = awaitOrNull(shared) ?: return cached
        return AppleCatalogResult(fresh, AppleCatalogFreshness.Fresh)
    }

    private suspend fun <T> awaitOrNull(deferred: Deferred<T?>): T? = try {
        deferred.await()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Timber.w(error, "Apple catalog request failed")
        null
    }

    private fun fetchPage(path: AppleCatalogPath, storefront: String, language: String): AppleCatalogPage? {
        val html = fetchHtml("https://music.apple.com/$storefront/${path.segment}?l=$language") ?: return null
        return AppleCatalogPageParser.parse(html, storefront, System.currentTimeMillis())
            ?: run {
                Timber.w("Apple catalog markup changed for %s/%s", storefront, path.segment)
                null
            }
    }

    private fun fetchPlaylist(storefront: String, playlistId: String, language: String): AppleCatalogPlaylist? {
        val html = fetchHtml("https://music.apple.com/$storefront/playlist/levyra/$playlistId?l=$language") ?: return null
        val now = System.currentTimeMillis()
        val page = AppleCatalogPageParser.parse(html, storefront, now) ?: run {
            Timber.w("Apple playlist markup changed for %s", playlistId)
            return null
        }
        val header = page.shelf { it.kind == AppleCatalogShelfKind.Header }?.items?.firstOrNull()
        val tracks = page.shelves.filter { it.kind == AppleCatalogShelfKind.Songs }
            .flatMap { it.items }
            .mapNotNull(AppleCatalogTracks::fromItem)
            .distinctBy { it.id }
        if (tracks.isEmpty()) return null
        return AppleCatalogPlaylist(
            id = playlistId,
            title = header?.title.orEmpty(),
            curator = header?.subtitle.orEmpty(),
            description = header?.description.orEmpty(),
            artworkUrl = header?.artworkUrl(PLAYLIST_ARTWORK_SIZE).orEmpty(),
            tracks = tracks,
            fetchedAtMs = now
        )
    }

    private fun fetchHtml(url: String): String? {
        if (!AppleCatalogIds.isCatalogUrl(url)) return null
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html")
            .header("Accept-Language", Locale.getDefault().toLanguageTag())
            .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!AppleCatalogIds.isCatalogUrl(response.request.url.toString())) return null
            when {
                response.code == HTTP_TOO_MANY_REQUESTS -> {
                    backoff.onRateLimited(System.currentTimeMillis(), response.header("Retry-After"))
                    null
                }
                !response.isSuccessful -> {
                    Timber.w("Apple catalog HTTP %d", response.code)
                    null
                }
                else -> {
                    backoff.onSuccess()
                    val source = response.body.source()
                    val buffer = Buffer()
                    var exhausted = false
                    while (!exhausted && buffer.size < MAX_PAGE_BYTES) {
                        exhausted = source.read(buffer, READ_CHUNK_BYTES) == -1L
                    }
                    if (buffer.size >= MAX_PAGE_BYTES) null else buffer.readUtf8()
                }
            }
        }
    }

    private fun freshnessOf(page: AppleCatalogPage, now: Long): AppleCatalogFreshness =
        if (now - page.fetchedAtMs < PAGE_TTL_MS) AppleCatalogFreshness.Fresh else AppleCatalogFreshness.Stale

    private fun pageKey(path: AppleCatalogPath, storefront: String, language: String): String = "${path.name.lowercase(Locale.ROOT)}-$storefront-$language"

    private fun readStored(key: String, storefront: String): AppleCatalogPage? = runCatching {
        val file = File(cacheDirectory, "$key.json")
        if (!file.isFile) return null
        AppleCatalogPageCodec.decode(file.readText(), storefront)
    }.getOrNull()

    private fun persist(key: String, page: AppleCatalogPage) {
        runCatching {
            cacheDirectory.mkdirs()
            val target = File(cacheDirectory, "$key.json")
            val temporary = File(cacheDirectory, "$key.json.tmp")
            temporary.writeText(AppleCatalogPageCodec.encode(page))
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        }.onFailure { Timber.w(it, "Apple catalog cache write failed") }
    }

    companion object {
        private const val CACHE_DIRECTORY = "apple_catalog"
        private const val PAGE_TTL_MS = 6L * 60L * 60L * 1000L
        private const val MAX_STALE_MS = 7L * 24L * 60L * 60L * 1000L
        private const val PLAYLIST_TTL_MS = 3L * 60L * 60L * 1000L
        private const val MAX_PAGE_BYTES = 6L * 1024L * 1024L
        private const val READ_CHUNK_BYTES = 64L * 1024L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val PLAYLIST_ARTWORK_SIZE = 600

        @Volatile
        private var instance: AppleMusicEditorialRepository? = null

        fun get(context: Context): AppleMusicEditorialRepository =
            instance ?: synchronized(this) {
                instance ?: AppleMusicEditorialRepository(context).also { instance = it }
            }
    }
}

internal class AppleCatalogBackoff(
    private val defaultDelayMs: Long = 15L * 60L * 1000L,
    private val maxDelayMs: Long = 2L * 60L * 60L * 1000L
) {
    @Volatile
    private var blockedUntil = 0L

    @Volatile
    private var strikes = 0

    fun blocked(now: Long): Boolean = now < blockedUntil

    fun onRateLimited(now: Long, retryAfter: String?) {
        strikes = (strikes + 1).coerceAtMost(4)
        val requested = retryAfter?.trim()?.toLongOrNull()?.takeIf { it > 0 }?.times(1000L)
        val delay = (requested ?: (defaultDelayMs shl (strikes - 1))).coerceAtMost(maxDelayMs)
        blockedUntil = now + delay
    }

    fun onSuccess() {
        strikes = 0
        blockedUntil = 0L
    }
}

internal object AppleStorefronts {
    fun normalize(value: String): String {
        val code = value.trim().lowercase(Locale.ROOT)
        return if (code.length == 2 && code.all { it in 'a'..'z' }) code else "us"
    }

    fun language(value: String): String {
        val code = value.trim().lowercase(Locale.ROOT).substringBefore('_').substringBefore('-')
        return if (code.length in 2..3 && code.all { it in 'a'..'z' }) code else "en"
    }
}

internal object AppleCatalogIds {
    private val playlistId = Regex("^pl\\.[a-z0-9-]{8,64}$")

    fun playlist(value: String): String? = value.trim().lowercase(Locale.ROOT).takeIf(playlistId::matches)

    fun playlistIdFromUrl(url: String): String? =
        url.substringAfterLast('/').substringBefore('?').let(::playlist)

    fun isCatalogUrl(url: String): Boolean {
        val parsed = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        return parsed.scheme == "https" && parsed.host == "music.apple.com" && parsed.port == -1 && parsed.userInfo == null
    }
}

object AppleCatalogTracks {
    fun track(
        title: String,
        artist: String,
        album: String = "",
        artworkUrl: String = "",
        durationMs: Long = 0L,
        explicit: Boolean = false,
        appleSongId: String = ""
    ): Track? {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return null
        val cleanArtist = artist.trim()
        val digest = MessageDigest.getInstance("SHA-256").digest("$cleanTitle|$cleanArtist".lowercase(Locale.ROOT).toByteArray())
        val seed = digest.take(4).fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) } and Int.MAX_VALUE
        val artwork = AppleCatalogArtwork.url(artworkUrl, ARTWORK_SIZE, ARTWORK_SIZE)
        return Track(
            id = "chart-apple-" + digest.take(8).joinToString("") { "%02x".format(it) },
            title = cleanTitle,
            artist = cleanArtist.ifBlank { "Various Artists" },
            album = album.trim(),
            durationMs = durationMs.coerceAtLeast(0L),
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = artwork,
            largeThumbnailUrl = artwork,
            source = SOURCE,
            moodTags = setOf("chart"),
            energy = 70,
            vocal = 55,
            replayScore = 80,
            cacheScore = 70,
            accentStart = 0xFF000000.toInt() or (seed and 0xFFFFFF),
            accentEnd = 0xFF000000.toInt() or ((seed ushr 8) and 0xFFFFFF),
            appleSongId = appleSongId.filter(Char::isDigit),
            explicit = explicit
        )
    }

    fun fromItem(item: AppleCatalogItem): Track? =
        if (item.kind != AppleCatalogItemKind.Song) null
        else track(item.title, item.subtitle, item.album, item.artworkTemplate, item.durationMs, item.explicit, item.id)

    const val SOURCE = "Apple Music Catalog"
    private const val ARTWORK_SIZE = 600
}

internal object AppleCatalogPageCodec {
    fun encode(page: AppleCatalogPage): String = JSONObject()
        .put("fetchedAtMs", page.fetchedAtMs)
        .put("shelves", JSONArray().apply {
            page.shelves.forEach { shelf ->
                put(JSONObject()
                    .put("id", shelf.id)
                    .put("title", shelf.title)
                    .put("kind", shelf.kind.name)
                    .put("rows", shelf.rows)
                    .put("items", JSONArray().apply { shelf.items.forEach { put(encodeItem(it)) } }))
            }
        })
        .toString()

    fun decode(body: String, storefront: String): AppleCatalogPage? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val shelvesJson = root.optJSONArray("shelves") ?: return null
        val shelves = (0 until shelvesJson.length()).mapNotNull { index ->
            val shelf = shelvesJson.optJSONObject(index) ?: return@mapNotNull null
            val kind = runCatching { AppleCatalogShelfKind.valueOf(shelf.optString("kind")) }.getOrNull() ?: return@mapNotNull null
            val itemsJson = shelf.optJSONArray("items") ?: return@mapNotNull null
            val items = (0 until itemsJson.length()).mapNotNull { itemsJson.optJSONObject(it)?.let(::decodeItem) }
            if (items.isEmpty()) null else AppleCatalogShelf(shelf.optString("id"), shelf.optString("title"), kind, items, shelf.optInt("rows", 1).coerceIn(1, 4))
        }
        if (shelves.isEmpty()) return null
        return AppleCatalogPage(storefront, shelves, root.optLong("fetchedAtMs"))
    }

    private fun encodeItem(item: AppleCatalogItem): JSONObject = JSONObject()
        .put("id", item.id)
        .put("kind", item.kind.name)
        .put("title", item.title)
        .put("subtitle", item.subtitle)
        .put("heading", item.heading)
        .put("description", item.description)
        .put("artwork", item.artworkTemplate)
        .put("cover", item.coverArtworkTemplate)
        .put("background", item.backgroundColor ?: JSONObject.NULL)
        .put("url", item.url)
        .put("explicit", item.explicit)
        .put("durationMs", item.durationMs)
        .put("album", item.album)

    private fun decodeItem(json: JSONObject): AppleCatalogItem? {
        val kind = runCatching { AppleCatalogItemKind.valueOf(json.optString("kind")) }.getOrNull() ?: return null
        val title = json.optString("title").takeIf(String::isNotBlank) ?: return null
        return AppleCatalogItem(
            id = json.optString("id"),
            kind = kind,
            title = title,
            subtitle = json.optString("subtitle"),
            heading = json.optString("heading"),
            description = json.optString("description"),
            artworkTemplate = json.optString("artwork"),
            coverArtworkTemplate = json.optString("cover"),
            backgroundColor = if (json.isNull("background")) null else json.optInt("background"),
            url = json.optString("url"),
            explicit = json.optBoolean("explicit"),
            durationMs = json.optLong("durationMs"),
            album = json.optString("album")
        )
    }
}
