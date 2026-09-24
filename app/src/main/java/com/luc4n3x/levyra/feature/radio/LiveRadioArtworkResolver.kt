package com.luc4n3x.levyra.feature.radio

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal object LiveRadioArtworkResolver {
    private const val MAX_HTML_BYTES = 256 * 1024
    private const val MAX_IMAGE_CANDIDATES = 4
    private const val MAX_CACHE_ENTRIES = 256
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android) Levyra Live Radio"
    private const val MAX_ARTWORK_BYTES = 1024 * 1024
    private const val MAX_STORED_ARTWORKS = 48
    private const val ARTWORK_DIRECTORY = "live-radio-artwork"

    private val client: OkHttpClient by lazy {
        LiveRadioArtworkLoader.client.newBuilder()
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(4))
    private val lock = Any()
    private val cache = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > MAX_CACHE_ENTRIES
    }
    private val inFlight = HashMap<String, Deferred<String?>>()

    suspend fun resolve(homepageUrl: String): String? {
        val key = homepageKey(homepageUrl) ?: return null
        val pending = synchronized(lock) {
            cache[key]?.let { return it.ifBlank { null } }
            inFlight.getOrPut(key) {
                scope.async {
                    try {
                        val discovered = try {
                            discover(key)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                        synchronized(lock) { discovered?.let { cache[key] = it } }
                        discovered
                    } finally {
                        synchronized(lock) { inFlight.remove(key) }
                    }
                }
            }
        }
        return pending.await()?.ifBlank { null }
    }

    suspend fun playerArtwork(context: Context, station: RadioStation): String? {
        val appContext = context.applicationContext
        val favicon = station.safeFaviconUrl
        if (favicon.isNotBlank()) {
            withContext(Dispatchers.IO) { storeArtwork(appContext, favicon) }?.let { return it }
        }
        val discovered = resolve(station.homepageUrl)?.takeIf { it != favicon } ?: return null
        return withContext(Dispatchers.IO) { storeArtwork(appContext, discovered) }
    }

    private fun storeArtwork(context: Context, url: String): String? {
        if (!RadioUrlPolicy.isAllowed(url)) return null
        return try {
            val directory = File(context.cacheDir, ARTWORK_DIRECTORY).apply { mkdirs() }
            val target = File(directory, sha256(url))
            if (target.length() > 0L) {
                target.setLastModified(System.currentTimeMillis())
                return Uri.fromFile(target).toString()
            }
            val bytes = downloadArtwork(url) ?: return null
            val temporary = File.createTempFile("artwork", ".tmp", directory)
            val stored = try {
                temporary.writeBytes(bytes)
                temporary.renameTo(target)
            } finally {
                if (temporary.exists()) temporary.delete()
            }
            if (!stored) return null
            pruneStoredArtwork(directory)
            Uri.fromFile(target).toString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadArtwork(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body.contentLength() > MAX_ARTWORK_BYTES) return null
            val source = response.body.source()
            source.request(MAX_ARTWORK_BYTES + 1L)
            if (source.buffer.size > MAX_ARTWORK_BYTES) return null
            val bytes = source.buffer.readByteArray()
            return bytes.takeIf { isRasterImageHeader(it.copyOf(minOf(it.size, 12))) }
        }
    }

    private fun pruneStoredArtwork(directory: File) {
        val stored = directory.listFiles { file -> file.isFile && !file.name.endsWith(".tmp") } ?: return
        if (stored.size <= MAX_STORED_ARTWORKS) return
        stored.sortedBy(File::lastModified)
            .take(stored.size - MAX_STORED_ARTWORKS)
            .forEach(File::delete)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun homepageKey(value: String): String? {
        val url = value.trim().toHttpUrlOrNull() ?: return null
        if (!RadioUrlPolicy.isAllowed(url.toString())) return null
        return url.newBuilder().fragment(null).build().toString()
    }

    private fun discover(homepage: String): String? {
        val pages = listOfNotNull(homepage, homepage.toHttpUrlOrNull()?.resolve("/")?.toString()).distinct()
        var reachedSite = false
        for (page in pages) {
            val document = try {
                fetchHtml(page)
            } catch (_: IOException) {
                null
            } ?: continue
            reachedSite = true
            val candidates = (liveRadioArtworkCandidates(document.html, document.url) +
                listOfNotNull(document.url.resolve("/favicon.ico")?.toString()))
                .distinct()
                .filter(RadioUrlPolicy::isAllowed)
                .take(MAX_IMAGE_CANDIDATES)
            candidates.firstOrNull(::isRasterImage)?.let { return it }
        }
        return if (reachedSite) "" else null
    }

    private fun fetchHtml(url: String): HtmlDocument? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val contentType = response.header("Content-Type").orEmpty().lowercase(Locale.ROOT)
            if (contentType.isNotBlank() && "html" !in contentType) return null
            val source = response.body.source()
            source.request(MAX_HTML_BYTES.toLong())
            val bytes = source.buffer.readByteArray(minOf(source.buffer.size, MAX_HTML_BYTES.toLong()))
            return HtmlDocument(response.request.url, String(bytes, Charsets.UTF_8))
        }
    }

    private fun isRasterImage(url: String): Boolean {
        if (!RadioUrlPolicy.isAllowed(url)) return false
        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .header("User-Agent", USER_AGENT)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val source = response.body.source()
                source.request(12)
                isRasterImageHeader(source.buffer.readByteArray(minOf(source.buffer.size, 12L)))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    private class HtmlDocument(val url: HttpUrl, val html: String)
}

private val htmlLinkTagPattern = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
private val htmlMetaTagPattern = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val htmlAttributePattern = Regex("""([a-zA-Z:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
private val iconSizePattern = Regex("""(\d{2,4})x\d{2,4}""")

internal fun liveRadioArtworkCandidates(html: String, pageUrl: HttpUrl): List<String> {
    val ranked = mutableListOf<Pair<Int, String>>()
    htmlLinkTagPattern.findAll(html).forEach { match ->
        val attributes = htmlAttributes(match.value)
        val href = attributes["href"]?.trim().orEmpty()
        val rel = attributes["rel"].orEmpty().lowercase(Locale.ROOT).split(' ').filter(String::isNotBlank)
        if (href.isBlank()) return@forEach
        val size = iconSizePattern.find(attributes["sizes"].orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val rank = when {
            rel.any { it.startsWith("apple-touch-icon") } -> 4
            "icon" in rel && size >= 96 -> 3
            "icon" in rel -> 1
            else -> return@forEach
        }
        ranked += rank to href
    }
    htmlMetaTagPattern.findAll(html).forEach { match ->
        val attributes = htmlAttributes(match.value)
        val property = (attributes["property"] ?: attributes["name"]).orEmpty().lowercase(Locale.ROOT)
        val content = attributes["content"]?.trim().orEmpty()
        if (property == "og:image" && content.isNotBlank()) ranked += 2 to content
    }
    return ranked
        .sortedByDescending { it.first }
        .mapNotNull { (_, href) -> pageUrl.resolve(href.replace("&amp;", "&")) }
        .filter { it.scheme == "http" || it.scheme == "https" }
        .filterNot { it.encodedPath.lowercase(Locale.ROOT).endsWith(".svg") }
        .map(HttpUrl::toString)
        .distinct()
}

internal fun isRasterImageHeader(header: ByteArray): Boolean {
    fun matches(offset: Int, vararg bytes: Int) =
        header.size >= offset + bytes.size && bytes.indices.all { header[offset + it] == bytes[it].toByte() }
    return matches(0, 0x89, 0x50, 0x4E, 0x47) ||
        matches(0, 0xFF, 0xD8, 0xFF) ||
        matches(0, 0x47, 0x49, 0x46) ||
        matches(0, 0x00, 0x00, 0x01, 0x00) ||
        (matches(0, 0x52, 0x49, 0x46, 0x46) && matches(8, 0x57, 0x45, 0x42, 0x50))
}

private fun htmlAttributes(tag: String): Map<String, String> =
    htmlAttributePattern.findAll(tag).associate { match ->
        match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues[2].ifEmpty { match.groupValues[3] }
    }
