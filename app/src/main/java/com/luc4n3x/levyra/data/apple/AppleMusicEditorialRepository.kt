package com.luc4n3x.levyra.data.apple

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
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

internal data class AppleCatalogPath(val segment: String, val cacheName: String) {
    companion object {
        val New = AppleCatalogPath("new", "new")
        val TopCharts = AppleCatalogPath("new/top-charts", "topcharts")
        val Genres = AppleCatalogPath("room/1654814539", "genres")
        val Moods = AppleCatalogPath("room/1654813864", "moods")
    }
}

internal enum class AppleCatalogCollection(val segment: String) {
    Playlist("playlist"),
    Album("album")
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
    private val catalogApiClient: OkHttpClient by lazy {
        LevyraHttpClientFactory.feeds(appContext).newBuilder()
            .addNetworkInterceptor { chain ->
                val url = chain.request().url
                if (!url.isHttps || url.host != CATALOG_API_HOST) throw java.io.IOException("Apple catalog API redirect rejected")
                chain.proceed(chain.request())
            }
            .build()
    }
    private val tokenProvider by lazy { AppleDeveloperTokenProvider.get(appContext) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pages = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, AppleCatalogPage>(MAX_MEMORY_PAGES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AppleCatalogPage>?): Boolean = size > MAX_MEMORY_PAGES
    })
    private val playlists = ConcurrentHashMap<String, AppleCatalogPlaylist>()
    private val chartAlbums = ConcurrentHashMap<String, Pair<Long, List<AlbumHit>>>()
    private val artists = object : LinkedHashMap<String, AppleArtistPage>(MAX_ARTIST_PAGES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AppleArtistPage>?): Boolean = size > MAX_ARTIST_PAGES
    }
    private val inFlight = ConcurrentHashMap<String, Deferred<Any?>>()
    private val cacheDirectory by lazy { File(appContext.cacheDir, CACHE_DIRECTORY) }
    private val backoff = AppleCatalogBackoff()

    suspend fun newMusic(storefront: String, language: String): AppleCatalogResult? = page(AppleCatalogPath.New, storefront, language)

    suspend fun topCharts(storefront: String, language: String): AppleCatalogResult? = page(AppleCatalogPath.TopCharts, storefront, language)

    suspend fun newReleaseAlbums(storefront: String, language: String, limit: Int): List<AlbumHit> =
        appleNewReleaseAlbums(newMusic(storefront, language)?.page, limit)

    suspend fun recentChartAlbums(storefront: String, language: String, limit: Int): List<AlbumHit> {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val key = "chart-albums:$front:$lang"
        chartAlbums[key]?.takeIf { System.currentTimeMillis() - it.first < PLAYLIST_TTL_MS }?.let { return it.second.take(limit) }
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val url = "$CATALOG_API_URL/v1/catalog/$front/charts".toHttpUrl().newBuilder()
                        .addQueryParameter("types", "albums")
                        .addQueryParameter("limit", CHART_ALBUMS_LIMIT.toString())
                        .addQueryParameter("l", lang)
                        .build()
                    val now = System.currentTimeMillis()
                    appleChartReleaseAlbums(catalogJson(url, "Apple album charts"), now, CHART_ALBUMS_LIMIT)
                        .also { albums ->
                            chartAlbums.entries.removeIf { now - it.value.first >= PLAYLIST_TTL_MS }
                            if (albums.isNotEmpty()) chartAlbums[key] = now to albums
                        }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<List<AlbumHit>?>
        return (awaitOrNull(shared) ?: chartAlbums[key]?.second).orEmpty().take(limit)
    }

    suspend fun genre(title: String, categoryParams: String, storefront: String, language: String): AppleCatalogPage? {
        val mappedCuratorId = AppleExploreCatalog.curatorId(categoryParams)
        mappedCuratorId?.let { id -> curatorPage(id, storefront, language)?.let { return it } }
        for (directory in listOf(AppleCatalogPath.Genres, AppleCatalogPath.Moods)) {
            val items = page(directory, storefront, language)?.page?.shelves.orEmpty().flatMap { it.items }
            val curator = AppleExploreCatalog.findCurator(title, items)
                ?: AppleExploreCatalog.findCurator(title, page(directory, "us", "en")?.page?.shelves.orEmpty().flatMap { it.items })
                ?: continue
            if (curator.id == mappedCuratorId) continue
            curatorPage(curator.id, storefront, language)?.let { return it }
        }
        return null
    }

    suspend fun browseCategories(storefront: String, language: String): List<AppleCatalogItem> =
        listOf(AppleCatalogPath.Genres, AppleCatalogPath.Moods)
            .flatMap { directory -> page(directory, storefront, language)?.page?.shelves.orEmpty().flatMap { it.items } }
            .filter(AppleExploreCatalog::isCategory)
            .distinctBy { it.id }

    private suspend fun curatorPage(curatorId: String, storefront: String, language: String): AppleCatalogPage? =
        page(AppleCatalogPath("curator/levyra/$curatorId", "curator-$curatorId"), storefront, language)?.page
            ?.takeIf { page -> page.shelves.any { shelf -> shelf.items.isNotEmpty() } }

    suspend fun cachedNewMusic(storefront: String, language: String): AppleCatalogResult? = cachedPage(AppleCatalogPath.New, storefront, language)

    suspend fun cachedTopCharts(storefront: String, language: String): AppleCatalogResult? = cachedPage(AppleCatalogPath.TopCharts, storefront, language)

    suspend fun playlist(storefront: String, playlistId: String, language: String): AppleCatalogPlaylist? {
        val id = AppleCatalogIds.playlist(playlistId) ?: return null
        return collection(AppleCatalogCollection.Playlist, storefront, id, language)
    }

    suspend fun album(storefront: String, albumId: String, language: String): AppleCatalogPlaylist? {
        val id = AppleCatalogIds.album(albumId) ?: return null
        return collection(AppleCatalogCollection.Album, storefront, id, language)
    }

    suspend fun artistHeader(storefront: String, artistName: String, language: String): AppleArtistPage? =
        cachedArtist("artist-header", storefront, artistName, language) { front, name, lang -> searchArtist(front, name, lang) }

    suspend fun artist(storefront: String, artistName: String, language: String): AppleArtistPage? =
        cachedArtist("artist", storefront, artistName, language) { front, name, lang -> fetchArtist(front, name, lang) }

    private suspend fun cachedArtist(
        kind: String,
        storefront: String,
        artistName: String,
        language: String,
        load: suspend (String, String, String) -> AppleArtistPage?
    ): AppleArtistPage? {
        val name = artistName.trim().takeIf { it.length >= 2 } ?: return null
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val key = "$kind:$front:$lang:${name.lowercase(Locale.ROOT)}"
        synchronized(artists) { artists[key] }?.takeIf { System.currentTimeMillis() - it.fetchedAtMs < ARTIST_TTL_MS }?.let { return it }
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    load(front, name, lang)?.also { page -> synchronized(artists) { artists[key] = page } }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<AppleArtistPage?>
        return awaitOrNull(shared) ?: synchronized(artists) { artists[key] }
    }

    private suspend fun fetchArtist(storefront: String, artistName: String, language: String): AppleArtistPage? {
        val header = artistHeader(storefront, artistName, language) ?: return null
        val html = fetchHtml("https://music.apple.com/$storefront/artist/levyra/${header.id}?l=$language") ?: return null
        val page = AppleArtistPageParser.parse(html, System.currentTimeMillis()) ?: run {
            Timber.w("Apple artist markup changed for %s", header.id)
            return null
        }
        val topVideos = runCatching { topMusicVideos(storefront, header.id, language) }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault(emptyList())
        return page.copy(
            artworkTemplate = page.artworkTemplate.ifBlank { header.artworkTemplate },
            squareMotion = header.squareMotion ?: page.squareMotion,
            wideMotion = header.wideMotion ?: page.wideMotion,
            shelves = appleArtistShelvesWithTopVideos(page.shelves, topVideos),
            info = header.info
        )
    }

    private suspend fun topMusicVideos(storefront: String, artistId: String, language: String): List<AppleCatalogItem> {
        val url = "$CATALOG_API_URL/v1/catalog/$storefront/artists/$artistId/view/top-music-videos".toHttpUrl().newBuilder()
            .addQueryParameter("l", language)
            .addQueryParameter("limit", TOP_VIDEOS_LIMIT.toString())
            .addQueryParameter("extend", "editorialNotes")
            .build()
        val data = catalogJson(url, "Apple top videos")?.optJSONArray("data") ?: return emptyList()
        return AppleArtistPageParser.topVideos(data)
    }

    private suspend fun catalogJson(url: HttpUrl, label: String): JSONObject? {
        val developerToken = tokenProvider.getDeveloperToken() ?: return null
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $developerToken")
            .header("Origin", "https://music.apple.com")
            .header("Accept", "application/json")
            .build()
        val body = catalogApiClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.w("%s HTTP %d", label, response.code)
                return null
            }
            response.body.string()
        }
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private suspend fun searchArtist(storefront: String, artistName: String, language: String): AppleArtistPage? {
        val url = "$CATALOG_API_URL/v1/catalog/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", artistName)
            .addQueryParameter("types", "artists")
            .addQueryParameter("limit", ARTIST_SEARCH_LIMIT.toString())
            .addQueryParameter("l", language)
            .addQueryParameter("extend", "origin,bornOrFormed,artistBio,genreNames,isGroup,editorialVideo,artwork")
            .build()
        val results = catalogJson(url, "Apple artist search")
            ?.optJSONObject("results")?.optJSONObject("artists")?.optJSONArray("data") ?: return null
        val candidates = (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val attributes = item.optJSONObject("attributes") ?: return@mapNotNull null
            val id = item.optString("id").trim().takeIf { it.all(Char::isDigit) && it.isNotEmpty() } ?: return@mapNotNull null
            Triple(id, attributes.optString("name").trim(), attributes)
        }
        val chosen = selectAppleArtistCandidate(artistName, candidates.map { it.first to it.second }) ?: return null
        val attributes = candidates.first { it.first == chosen }.third
        return AppleArtistPageParser.header(chosen, attributes, System.currentTimeMillis())
    }

    private suspend fun collection(kind: AppleCatalogCollection, storefront: String, id: String, language: String): AppleCatalogPlaylist? {
        val front = AppleStorefronts.normalize(storefront)
        val lang = AppleStorefronts.language(language)
        val key = "${kind.segment}:$front:$lang:$id"
        playlists[key]?.takeIf { System.currentTimeMillis() - it.fetchedAtMs < PLAYLIST_TTL_MS }?.let { return it }
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    fetchCollection(kind, front, id, lang)?.also { playlists[key] = it }
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

    private fun fetchCollection(kind: AppleCatalogCollection, storefront: String, id: String, language: String): AppleCatalogPlaylist? {
        val html = fetchHtml("https://music.apple.com/$storefront/${kind.segment}/levyra/$id?l=$language") ?: return null
        val now = System.currentTimeMillis()
        val page = AppleCatalogPageParser.parse(html, storefront, now) ?: run {
            Timber.w("Apple %s markup changed for %s", kind.segment, id)
            return null
        }
        val header = page.shelf { it.kind == AppleCatalogShelfKind.Header }?.items?.firstOrNull()
        val tracks = page.shelves.filter { it.kind == AppleCatalogShelfKind.Songs }
            .flatMap { it.items }
            .mapNotNull(AppleCatalogTracks::fromItem)
            .distinctBy { it.id }
        if (tracks.isEmpty()) return null
        return AppleCatalogPlaylist(
            id = id,
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

    private fun pageKey(path: AppleCatalogPath, storefront: String, language: String): String = "${path.cacheName}-$storefront-$language"

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
            cacheDirectory.listFiles { file -> file.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }?.drop(MAX_STORED_PAGES)?.forEach { it.delete() }
        }.onFailure { Timber.w(it, "Apple catalog cache write failed") }
    }

    companion object {
        private const val CACHE_DIRECTORY = "apple_catalog"
        private const val MAX_MEMORY_PAGES = 12
        private const val MAX_STORED_PAGES = 40
        private const val PAGE_TTL_MS = 6L * 60L * 60L * 1000L
        private const val MAX_STALE_MS = 7L * 24L * 60L * 60L * 1000L
        private const val PLAYLIST_TTL_MS = 3L * 60L * 60L * 1000L
        private const val CHART_ALBUMS_LIMIT = 50
        private const val MAX_PAGE_BYTES = 6L * 1024L * 1024L
        private const val READ_CHUNK_BYTES = 64L * 1024L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val PLAYLIST_ARTWORK_SIZE = 600
        private const val ARTIST_TTL_MS = 6L * 60L * 60L * 1000L
        private const val MAX_ARTIST_PAGES = 24
        private const val ARTIST_SEARCH_LIMIT = 5
        private const val TOP_VIDEOS_LIMIT = 12
        private const val CATALOG_API_HOST = "amp-api.music.apple.com"
        private const val CATALOG_API_URL = "https://$CATALOG_API_HOST"

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
        val delay = (requested ?: (defaultDelayMs shl strikes - 1)).coerceAtMost(maxDelayMs)
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
    private val albumId = Regex("^[0-9]{5,15}$")

    fun playlist(value: String): String? = value.trim().lowercase(Locale.ROOT).takeIf(playlistId::matches)

    fun album(value: String): String? = value.trim().takeIf(albumId::matches)

    fun playlistIdFromUrl(url: String): String? =
        url.substringAfterLast('/').substringBefore('?').let(::playlist)

    fun albumFromUrl(url: String): Pair<String, String>? {
        if (!isCatalogUrl(url)) return null
        val segments = java.net.URI(url).path.split('/').filter(String::isNotBlank)
        if (segments.size < 3 || segments[1] != AppleCatalogCollection.Album.segment) return null
        val storefront = segments[0].lowercase(Locale.ROOT).takeIf { it.length == 2 && it.all { char -> char in 'a'..'z' } } ?: return null
        val id = album(segments.last()) ?: return null
        return storefront to id
    }

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
