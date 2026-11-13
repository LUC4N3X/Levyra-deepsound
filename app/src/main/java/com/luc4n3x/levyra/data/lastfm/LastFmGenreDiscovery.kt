package com.luc4n3x.levyra.data.lastfm

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class LastFmDiscoveryAlbum(val title: String, val artist: String, val imageUrl: String)

data class LastFmGenreDiscovery(
    val about: String,
    val artists: List<String>,
    val albums: List<LastFmDiscoveryAlbum>
) {
    val isEmpty: Boolean get() = about.isBlank() && artists.isEmpty() && albums.isEmpty()
}

internal sealed interface LastFmTagPage<out T> {
    data class Found<T>(val value: T) : LastFmTagPage<T>
    data object RateLimited : LastFmTagPage<Nothing>
    data class Failed(val reason: String) : LastFmTagPage<Nothing>
}

internal object LastFmGenreDiscoveryParser {
    private const val RATE_LIMIT_ERROR = 29
    private const val PLACEHOLDER_IMAGE = "2a96cbd8b46e442fc41c2b86b821562f"
    private val anchor = Regex("<a\\b[^>]*>.*?</a>\\.?", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val tag = Regex("<[^>]+>")
    private val spaces = Regex("[ \\t\\x0B\\f]+")
    private val blankLines = Regex("\\s*\\n\\s*\\n\\s*")
    private val sentenceEnd = Regex("[.!?。！？](?=[\\s\"'»”)\\]]|$)")

    fun parseAbout(body: String, maxChars: Int = 600): LastFmTagPage<String> = parse(body) { root ->
        val summary = root.optJSONObject("tag")?.optJSONObject("wiki")?.optString("summary").orEmpty()
        cleanAbout(summary, maxChars)
    }

    fun parseTopArtists(body: String): LastFmTagPage<List<String>> = parse(body) { root ->
        val items = root.optJSONObject("topartists")?.optJSONArray("artist") ?: JSONArray()
        (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.optString("name")?.trim()?.takeIf(::isRealName)
        }.distinctBy { it.lowercase(Locale.ROOT) }
    }

    fun parseTopAlbums(body: String): LastFmTagPage<List<LastFmDiscoveryAlbum>> = parse(body) { root ->
        val items = root.optJSONObject("albums")?.optJSONArray("album") ?: JSONArray()
        (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("name").trim().takeIf(::isRealName) ?: return@mapNotNull null
            val artist = item.optJSONObject("artist")?.optString("name")?.trim()?.takeIf(::isRealName) ?: return@mapNotNull null
            val image = largestImage(item.optJSONArray("image")) ?: return@mapNotNull null
            LastFmDiscoveryAlbum(title, artist, image)
        }.distinctBy { "${it.artist.lowercase(Locale.ROOT)}|${it.title.lowercase(Locale.ROOT)}" }
    }

    fun cleanAbout(summary: String, maxChars: Int): String {
        val text = tag.replace(anchor.replace(summary, ""), "")
            .replace("&quot;", "\"").replace("&amp;", "&").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace("\r", "")
            .let { blankLines.replace(it, "\n\n") }
            .let { spaces.replace(it, " ") }
            .trim()
        if (text.length <= maxChars) return text
        val window = text.substring(0, maxChars)
        val end = sentenceEnd.findAll(window).lastOrNull()?.range?.last ?: return ""
        return window.substring(0, end + 1).trim()
    }

    private fun <T> parse(body: String, read: (JSONObject) -> T): LastFmTagPage<T> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return LastFmTagPage.Failed("malformed")
        if (root.has("error")) {
            val code = root.optInt("error")
            return if (code == RATE_LIMIT_ERROR) LastFmTagPage.RateLimited else LastFmTagPage.Failed("error $code")
        }
        return LastFmTagPage.Found(read(root))
    }

    private fun largestImage(images: JSONArray?): String? {
        images ?: return null
        val bySize = (0 until images.length()).mapNotNull { index ->
            val image = images.optJSONObject(index) ?: return@mapNotNull null
            val url = image.optString("#text").trim()
            if (!url.startsWith("https://") || url.contains(PLACEHOLDER_IMAGE)) null else image.optString("size") to url
        }.toMap()
        return listOf("mega", "extralarge", "large").firstNotNullOfOrNull { bySize[it] }
    }

    private fun isRealName(value: String): Boolean =
        value.isNotBlank() && !value.equals("(null)", ignoreCase = true) && !value.equals("[unknown]", ignoreCase = true)
}

internal fun lastFmLanguage(languageCode: String): String =
    languageCode.trim().substringBefore('-').lowercase(Locale.ROOT).ifBlank { "en" }

internal fun interleaveDistinct(lists: List<List<String>>, limit: Int): List<String> {
    val seen = HashSet<String>()
    val result = ArrayList<String>()
    val deepest = lists.maxOfOrNull { it.size } ?: 0
    for (depth in 0 until deepest) {
        for (list in lists) {
            val value = list.getOrNull(depth) ?: continue
            if (seen.add(value.lowercase(Locale.ROOT))) result += value
            if (result.size >= limit) return result
        }
    }
    return result
}

internal data class LastFmDiscoveryMerge(val discovery: LastFmGenreDiscovery, val complete: Boolean)

internal fun mergeLastFmDiscovery(
    about: String?,
    artists: List<List<String>?>,
    albums: List<List<LastFmDiscoveryAlbum>?>,
    stored: LastFmGenreDiscovery?,
    limit: Int
): LastFmDiscoveryMerge {
    val artistsFailed = artists.all { it == null }
    val albumsFailed = albums.all { it == null }
    val albumLists = albums.map { it.orEmpty() }
    val albumKey = { album: LastFmDiscoveryAlbum -> "${album.artist}\u0000${album.title}" }
    val albumsByKey = albumLists.flatten().associateBy { albumKey(it).lowercase(Locale.ROOT) }
    val freshAlbums = interleaveDistinct(albumLists.map { list -> list.map(albumKey) }, limit)
        .mapNotNull { albumsByKey[it.lowercase(Locale.ROOT)] }
    return LastFmDiscoveryMerge(
        discovery = LastFmGenreDiscovery(
            about = about ?: stored?.about.orEmpty(),
            artists = if (artistsFailed) stored?.artists.orEmpty() else interleaveDistinct(artists.map { it.orEmpty() }, limit),
            albums = if (albumsFailed) stored?.albums.orEmpty() else freshAlbums
        ),
        complete = about != null && !artistsFailed && !albumsFailed
    )
}

class LastFmGenreDiscoveryRepository(
    context: Context,
    private val apiKey: String = BuildConfig.LASTFM_API_KEY
) {
    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy { LevyraHttpClientFactory.feeds(appContext) }
    private val cacheDir: File by lazy { File(appContext.cacheDir, CACHE_DIRECTORY) }
    private val memory: MutableMap<String, Stored> = lastFmMemoryCache()

    private data class Stored(val savedAtMs: Long, val discovery: LastFmGenreDiscovery)

    suspend fun cached(tags: List<String>, languageCode: String): LastFmGenreDiscovery? = withContext(Dispatchers.IO) {
        readCache(cacheKey(normalizedTags(tags), languageCode))?.discovery
    }

    suspend fun discovery(tags: List<String>, languageCode: String): LastFmGenreDiscovery? = withContext(Dispatchers.IO) {
        val cleanTags = normalizedTags(tags)
        if (cleanTags.isEmpty() || apiKey.isBlank()) return@withContext null
        val key = cacheKey(cleanTags, languageCode)
        val stored = readCache(key)
        if (stored != null && System.currentTimeMillis() - stored.savedAtMs in 0 until CACHE_TTL_MS) {
            return@withContext stored.discovery
        }
        val lang = lastFmLanguage(languageCode)
        val primary = cleanTags.first()
        val (about, artists, albums) = coroutineScope {
            val about = async { request("tag.getinfo", primary, mapOf("lang" to lang), LastFmGenreDiscoveryParser::parseAbout) }
            val artists = cleanTags.map { tag ->
                async { request("tag.gettopartists", tag, mapOf("limit" to PER_TAG_LIMIT.toString()), LastFmGenreDiscoveryParser::parseTopArtists) }
            }
            val albums = cleanTags.map { tag ->
                async { request("tag.gettopalbums", tag, mapOf("limit" to PER_TAG_LIMIT.toString()), LastFmGenreDiscoveryParser::parseTopAlbums) }
            }
            Triple(about.await(), artists.awaitAll(), albums.awaitAll())
        }
        val merged = mergeLastFmDiscovery(about, artists, albums, stored?.discovery, SECTION_LIMIT)
        if (merged.complete && !merged.discovery.isEmpty) writeCache(key, Stored(System.currentTimeMillis(), merged.discovery))
        merged.discovery.takeUnless { it.isEmpty } ?: stored?.discovery
    }

    private suspend fun <T> request(
        method: String,
        tag: String,
        extra: Map<String, String>,
        parse: (String) -> LastFmTagPage<T>
    ): T? {
        val url = LAST_FM_URL.toHttpUrl().newBuilder()
            .addQueryParameter("method", method)
            .addQueryParameter("tag", tag)
            .apply { extra.forEach { (name, value) -> addQueryParameter(name, value) } }
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("format", "json")
            .build()
        val body = try {
            runInterruptible {
                httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                    response.body.string().takeIf { it.length <= MAX_RESPONSE_CHARS }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Last.fm %s request failed for %s", method, tag)
            null
        } ?: return null
        return when (val page = parse(body)) {
            is LastFmTagPage.Found -> page.value
            LastFmTagPage.RateLimited -> {
                Timber.w("Last.fm rate limited %s for %s", method, tag)
                null
            }
            is LastFmTagPage.Failed -> {
                Timber.w("Last.fm %s failed for %s: %s", method, tag, page.reason)
                null
            }
        }
    }

    private fun normalizedTags(tags: List<String>): List<String> =
        tags.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }.distinct()

    private fun cacheKey(tags: List<String>, languageCode: String): String =
        tags.joinToString("+") + "@" + lastFmLanguage(languageCode)

    private fun cacheFile(key: String): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(key.toByteArray(Charsets.UTF_8))
        return File(cacheDir, digest.joinToString("") { "%02x".format(it) } + ".json")
    }

    private fun readCache(key: String): Stored? {
        memory[key]?.let { return it }
        val file = cacheFile(key)
        if (!file.isFile) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val artists = json.optJSONArray("artists") ?: JSONArray()
            val albums = json.optJSONArray("albums") ?: JSONArray()
            Stored(
                json.optLong("savedAtMs"),
                LastFmGenreDiscovery(
                    about = json.optString("about"),
                    artists = (0 until artists.length()).map { artists.optString(it) }.filter(String::isNotBlank),
                    albums = (0 until albums.length()).mapNotNull { index ->
                        val item = albums.optJSONObject(index) ?: return@mapNotNull null
                        LastFmDiscoveryAlbum(item.optString("title"), item.optString("artist"), item.optString("image"))
                            .takeIf { it.title.isNotBlank() && it.artist.isNotBlank() && it.imageUrl.startsWith("https://") }
                    }
                )
            ).takeIf { !it.discovery.isEmpty }
        }.getOrNull()?.also { memory[key] = it }
    }

    private fun writeCache(key: String, stored: Stored) {
        memory[key] = stored
        runCatching {
            cacheDir.mkdirs()
            val payload = JSONObject()
                .put("savedAtMs", stored.savedAtMs)
                .put("about", stored.discovery.about)
                .put("artists", JSONArray(stored.discovery.artists))
                .put("albums", JSONArray().apply {
                    stored.discovery.albums.forEach { album ->
                        put(JSONObject().put("title", album.title).put("artist", album.artist).put("image", album.imageUrl))
                    }
                })
            val target = cacheFile(key)
            val temporary = File(cacheDir, target.name + ".tmp")
            temporary.writeText(payload.toString())
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        }.onFailure { error -> Timber.w(error, "Last.fm discovery cache write failed") }
    }

    private companion object {
        const val LAST_FM_URL = "https://ws.audioscrobbler.com/2.0/"
        const val CACHE_DIRECTORY = "lastfm-genre-discovery"
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
        const val PER_TAG_LIMIT = 30
        const val SECTION_LIMIT = 20
        const val MAX_RESPONSE_CHARS = 2 * 1024 * 1024
    }
}
