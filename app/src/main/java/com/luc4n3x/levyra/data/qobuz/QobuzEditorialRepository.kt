package com.luc4n3x.levyra.data.qobuz

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.apple.AppleCatalogBackoff
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
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class QobuzShelfResult(val shelf: QobuzShelf, val fresh: Boolean)

class QobuzEditorialRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy {
        LevyraHttpClientFactory.feeds(appContext).newBuilder()
            .addNetworkInterceptor { chain ->
                if (!QobuzStore.isStoreUrl(chain.request().url.toString())) throw java.io.IOException("Qobuz store redirect rejected")
                chain.proceed(chain.request())
            }
            .build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val shelves = ConcurrentHashMap<String, QobuzShelf>()
    private val playlists = ConcurrentHashMap<String, QobuzPlaylistDetail>()
    private val magazines = ConcurrentHashMap<String, QobuzMagazinePage>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Any?>>()
    private val cacheDirectory by lazy { File(appContext.cacheDir, CACHE_DIRECTORY) }
    private val backoff = AppleCatalogBackoff()

    suspend fun cachedShelf(kind: QobuzShelfKind, locale: String): QobuzShelfResult? = withContext(Dispatchers.IO) {
        val key = shelfKey(kind, locale)
        val now = System.currentTimeMillis()
        val shelf = shelves[key] ?: readStored(key)?.also { shelves[key] = it }
        shelf?.takeIf { now - it.fetchedAtMs < MAX_STALE_MS && !it.isEmpty }?.let { QobuzShelfResult(it, now - it.fetchedAtMs < SHELF_TTL_MS) }
    }

    suspend fun shelf(kind: QobuzShelfKind, locale: String, force: Boolean = false): QobuzShelfResult? {
        val key = shelfKey(kind, locale)
        val cached = cachedShelf(kind, locale)
        if (cached?.fresh == true && !force) return cached
        if (backoff.blocked(System.currentTimeMillis())) return cached
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val html = fetchHtml(QobuzStore.pageUrl(locale, kind)) ?: return@async null
                    QobuzCatalogParser.parseShelf(kind, html, System.currentTimeMillis())
                        .takeUnless { it.isEmpty }
                        ?.also { shelf ->
                            shelves[key] = shelf
                            persist(key, shelf)
                        }
                        ?: run {
                            Timber.w("Qobuz %s page parsed empty", kind.cacheName)
                            null
                        }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<QobuzShelf?>
        val fresh = awaitOrNull(shared) ?: return cached
        return QobuzShelfResult(fresh, true)
    }

    suspend fun playlist(locale: String, playlist: QobuzPlaylist): QobuzPlaylistDetail? {
        val key = "playlist-$locale-${playlist.id}"
        playlists[key]?.takeIf { System.currentTimeMillis() - it.fetchedAtMs < PLAYLIST_TTL_MS }?.let { return it }
        if (backoff.blocked(System.currentTimeMillis())) return playlists[key]
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val html = fetchHtml(QobuzStore.playlistUrl(locale, playlist.slug, playlist.id)) ?: return@async null
                    QobuzCatalogParser.playlistDetail(playlist.id, html, System.currentTimeMillis())?.also { detail ->
                        if (playlists.size >= MAX_PLAYLISTS) playlists.keys.firstOrNull()?.let(playlists::remove)
                        playlists[key] = detail
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<QobuzPlaylistDetail?>
        return awaitOrNull(shared) ?: playlists[key]
    }

    suspend fun magazine(locale: String, section: String, force: Boolean = false): QobuzMagazinePage? {
        val slug = section.takeIf { it in QobuzMagazineParser.SECTIONS }.orEmpty()
        val key = "magazine-${slug.ifBlank { "home" }}-$locale"
        val now = System.currentTimeMillis()
        val cached = magazines[key] ?: withContext(Dispatchers.IO) { readText(key)?.let(QobuzMagazineCodec::decode) }?.also { magazines[key] = it }
        val usable = cached?.takeIf { now - it.fetchedAtMs < MAX_STALE_MS && !it.isEmpty }
        if (usable != null && !force && now - usable.fetchedAtMs < SHELF_TTL_MS) return usable
        if (backoff.blocked(now)) return usable
        @Suppress("UNCHECKED_CAST")
        val shared = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val path = if (slug.isBlank()) "magazine" else "magazine/section/$slug"
                    val html = fetchHtml("https://www.qobuz.com/$locale/$path") ?: return@async null
                    QobuzMagazineParser.parse(slug, html, System.currentTimeMillis()).takeUnless { it.isEmpty }?.also { page ->
                        magazines[key] = page
                        writeText(key, QobuzMagazineCodec.encode(page))
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        } as Deferred<QobuzMagazinePage?>
        return awaitOrNull(shared) ?: usable
    }

    private suspend fun <T> awaitOrNull(deferred: Deferred<T?>): T? = try {
        deferred.await()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Timber.w(error, "Qobuz store request failed")
        null
    }

    private fun fetchHtml(url: String): String? {
        if (!QobuzStore.isStoreUrl(url)) return null
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html")
            .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!QobuzStore.isStoreUrl(response.request.url.toString())) return null
            when {
                response.code == HTTP_TOO_MANY_REQUESTS || response.code == HTTP_FORBIDDEN -> {
                    backoff.onRateLimited(System.currentTimeMillis(), response.header("Retry-After"))
                    null
                }
                !response.isSuccessful -> {
                    Timber.w("Qobuz store HTTP %d", response.code)
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

    private fun shelfKey(kind: QobuzShelfKind, locale: String): String = "${kind.cacheName}-$locale"

    private fun readStored(key: String): QobuzShelf? = readText(key)?.let(QobuzShelfCodec::decode)

    private fun readText(key: String): String? = runCatching {
        File(cacheDirectory, "$key.json").takeIf { it.isFile }?.readText()
    }.getOrNull()

    private fun persist(key: String, shelf: QobuzShelf) = writeText(key, QobuzShelfCodec.encode(shelf))

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
            cacheDirectory.listFiles { file -> file.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }?.drop(MAX_STORED_SHELVES)?.forEach { it.delete() }
        }.onFailure { Timber.w(it, "Qobuz shelf cache write failed") }
    }

    companion object {
        private const val CACHE_DIRECTORY = "qobuz_store"
        private const val MAX_STORED_SHELVES = 30
        private const val MAX_PLAYLISTS = 24
        private const val SHELF_TTL_MS = 6L * 60L * 60L * 1000L
        private const val MAX_STALE_MS = 7L * 24L * 60L * 60L * 1000L
        private const val PLAYLIST_TTL_MS = 3L * 60L * 60L * 1000L
        private const val MAX_PAGE_BYTES = 6L * 1024L * 1024L
        private const val READ_CHUNK_BYTES = 64L * 1024L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_FORBIDDEN = 403

        @Volatile
        private var instance: QobuzEditorialRepository? = null

        fun get(context: Context): QobuzEditorialRepository =
            instance ?: synchronized(this) {
                instance ?: QobuzEditorialRepository(context).also { instance = it }
            }
    }
}
