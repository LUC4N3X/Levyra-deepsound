package com.luc4n3x.levyra.desktop.core.catalog

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.stream.YoutubeStreamNetworkPolicy
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

object AppleArtistCanvas {
    private val client = ExtractorHttp.client.newBuilder()
        .followRedirects(false).followSslRedirects(false)
        .dns(YoutubeStreamNetworkPolicy.validatingDns())
        .callTimeout(4, TimeUnit.SECONDS)
        .build()

    private var cachedToken: Pair<Long, String>? = null

    suspend fun download(artist: String, country: String): Path? {
        var downloaded: Path? = null
        try {
            return runInterruptible(Dispatchers.IO) {
                if (artist.isBlank()) return@runInterruptible null
                val token = token() ?: return@runInterruptible null
                val asset = sequenceOf("us", country.lowercase(Locale.ROOT)).filter { it.length == 2 }.distinct()
                    .firstNotNullOfOrNull { storefront -> discover(artist, storefront, token) } ?: return@runInterruptible null
                downloadLoop(asset).also { downloaded = it }
            }
        } catch (cancelled: CancellationException) {
            withContext(Dispatchers.IO + NonCancellable) { downloaded?.let { Files.deleteIfExists(it) } }
            throw cancelled
        }
    }

    @Synchronized
    private fun token(): String? {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { it.first > now }?.let { return it.second }
        val url = "https://am-mint.binimum.org/token".toHttpUrl()
        val response = json(Request.Builder().url(url).build()) ?: return null
        val responseToken = response.optString("dev_token").ifBlank { response.optString("token") }
        if (!TOKEN.matches(responseToken)) return null
        val expiresAt = now + TimeUnit.MINUTES.toMillis(25)
        cachedToken = expiresAt to responseToken
        return responseToken
    }

    private fun discover(artist: String, storefront: String, token: String): HttpUrl? {
        val base = "https://amp-api.music.apple.com/v1/catalog/$storefront".toHttpUrl()
        val search = base.newBuilder().addPathSegment("search")
            .addQueryParameter("term", artist).addQueryParameter("types", "artists")
            .addQueryParameter("limit", "5").addQueryParameter("extend", EDITORIAL_FIELDS).build()
        fun request(url: HttpUrl): JSONObject? = json(
            Request.Builder().url(url).header("Authorization", "Bearer $token").header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
                .header("Origin", "https://music.apple.com").header("Referer", "https://music.apple.com/").build()
        )
        fun details(id: String): HttpUrl? {
            val detail = base.newBuilder().addPathSegment("artists").addPathSegment(id)
                .addQueryParameter("extend", EDITORIAL_FIELDS).build()
            return request(detail)?.optJSONArray("data")?.optJSONObject(0)?.optJSONObject("attributes")
                ?.takeIf { artistCreditMatches(it.optString("name"), artist) }?.let(::selectAsset)
        }
        val data = request(search)?.optJSONObject("results")?.optJSONObject("artists")?.optJSONArray("data")
        if (data != null) {
            for (index in 0 until minOf(data.length(), 5)) {
                val item = data.optJSONObject(index) ?: continue
                val attributes = item.optJSONObject("attributes") ?: continue
                if (!artistCreditMatches(attributes.optString("name"), artist)) continue
                selectAsset(attributes)?.let { return it }
                val id = item.optString("id").takeIf(::validArtistId) ?: continue
                return details(id)
            }
        }
        return lookupArtistId(artist, storefront)?.let(::details)
    }

    private fun lookupArtistId(artist: String, storefront: String): String? {
        val query = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", artist).addQueryParameter("entity", "musicArtist")
            .addQueryParameter("country", storefront).addQueryParameter("limit", "8").build()
        val entries = json(Request.Builder().url(query).build())?.optJSONArray("results") ?: return null
        return (0 until entries.length()).asSequence().mapNotNull(entries::optJSONObject)
            .filter { artistCreditMatches(it.optString("artistName"), artist) }
            .map { it.optString("artistId") }.firstOrNull(::validArtistId)
    }

    private fun validArtistId(id: String): Boolean = id.length in 1..16 && id.all { it in '0'..'9' }

    private fun selectAsset(attributes: JSONObject): HttpUrl? =
        listOf("editorialVideo", "editorialArtwork").firstNotNullOfOrNull { field ->
            val group = attributes.optJSONObject(field) ?: return@firstNotNullOfOrNull null
            group.keys().asSequence().take(12).mapNotNull(group::optJSONObject).mapNotNull { asset ->
                val url = sequenceOf("video", "videoUrl", "hlsUrl", "url").map(asset::optString)
                    .mapNotNull { it.toHttpUrlOrNull() }.firstOrNull { allowedMedia(it) && it.encodedPath.endsWith(".m3u8") }
                    ?: return@mapNotNull null
                val preview = asset.optJSONObject("previewFrame")
                val w = preview?.optInt("width") ?: 0
                val h = preview?.optInt("height") ?: 0
                val rank = when {
                    w <= 0 || h <= 0 -> 1
                    h > w -> 3
                    h == w -> 2
                    else -> 0
                }
                Triple(rank, w.toLong() * h, url)
            }.maxWithOrNull(compareBy<Triple<Int, Long, HttpUrl>> { it.first }.thenBy { it.second })?.third
        }

    private fun canvasMediaPlaylist(master: HttpUrl): Pair<HttpUrl, List<String>>? {
        val (masterUrl, masterLines) = playlist(master) ?: return null
        val rendition = masterLines.indices.mapNotNull { index ->
            val line = masterLines[index]
            if (!line.startsWith("#EXT-X-STREAM-INF:")) return@mapNotNull null
            if (!line.contains("avc1")) return@mapNotNull null
            val height = RESOLUTION.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            val url = masterUrl.resolve(masterLines.getOrNull(index + 1) ?: "")?.takeIf(::allowedMedia) ?: return@mapNotNull null
            height to url
        }.sortedBy { it.first }
        val selected = rendition.lastOrNull { it.first <= 640 } ?: rendition.firstOrNull { it.first <= 1080 }
        if (rendition.isNotEmpty() && selected == null) return null
        return if (selected == null) masterUrl to masterLines else playlist(selected.second)
    }

    private fun downloadLoop(master: HttpUrl): Path? {
        val (url, lines) = canvasMediaPlaylist(master) ?: return null
        if (lines.none { it == "#EXT-X-ENDLIST" } || lines.any { it.startsWith("#EXT-X-KEY") }) return null
        val segments = parseSegments(url, lines) ?: return null
        if (segments.size !in 1..16) return null
        val fragmentedMp4 = lines.any { it.startsWith("#EXT-X-MAP:") }
        val file = Files.createTempFile("levyra-artist-canvas-", if (fragmentedMp4) ".mp4" else ".ts")
        var complete = false
        try {
            Files.newOutputStream(file).use { output ->
                var remaining = 14L * 1024L * 1024L
                for (segment in mergeAdjacentRanges(segments)) {
                    val (_, bytes) = media(
                        segment.url, minOf(remaining, 4L * 1024L * 1024L), manifest = false, range = segment.range
                    ) ?: return null
                    output.write(bytes)
                    remaining -= bytes.size
                    if (Thread.currentThread().isInterrupted) return null
                }
            }
            complete = true
            return file
        } finally {
            if (!complete) Files.deleteIfExists(file)
        }
    }

    private data class Segment(val url: HttpUrl, val range: LongRange? = null)

    private fun parseSegments(base: HttpUrl, lines: List<String>): List<Segment>? {
        val result = mutableListOf<Segment>()
        var pendingRange: String? = null
        for (line in lines) {
            when {
                line.startsWith("#EXT-X-MAP:") -> {
                    if (result.isNotEmpty()) return null
                    val init = INIT_URI.find(line)?.groupValues?.get(1) ?: return null
                    val url = base.resolve(init)?.takeIf(::allowedMedia) ?: return null
                    val range = INIT_RANGE.find(line)?.groupValues?.get(1)?.let { parseRange(it, 0) ?: return null }
                    result += Segment(url, range)
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> pendingRange = line.substringAfter(':')
                line.isNotBlank() && !line.startsWith('#') -> {
                    val url = base.resolve(line)?.takeIf(::allowedMedia) ?: return null
                    val previous = result.lastOrNull()?.takeIf { it.url == url }?.range
                    val range = pendingRange?.let { parseRange(it, previous?.last?.plus(1)) ?: return null }
                    result += Segment(url, range)
                    pendingRange = null
                }
            }
        }
        return result
    }

    private fun mergeAdjacentRanges(segments: List<Segment>): List<Segment> {
        val requests = mutableListOf<Segment>()
        for (segment in segments) {
            val previous = requests.lastOrNull()
            val previousRange = previous?.range
            val range = segment.range
            if (range != null && previousRange != null && previous.url == segment.url &&
                previousRange.last + 1 == range.first && range.last - previousRange.first < MAX_SEGMENT_BYTES
            ) {
                requests[requests.lastIndex] = segment.copy(range = previousRange.first..range.last)
            } else {
                requests += segment
            }
        }
        return requests
    }

    private fun parseRange(value: String, nextOffset: Long?): LongRange? {
        val length = value.substringBefore('@').toLongOrNull()?.takeIf { it in 1..MAX_SEGMENT_BYTES } ?: return null
        val offset = value.substringAfter('@', "").toLongOrNull() ?: nextOffset ?: return null
        if (offset !in 0..MAX_MEDIA_OFFSET) return null
        return offset until offset + length
    }

    private fun playlist(url: HttpUrl): Pair<HttpUrl, List<String>>? {
        val (resolved, bytes) = media(url, 256L * 1024L, manifest = true) ?: return null
        val lines = bytes.toString(Charsets.UTF_8).lineSequence().map(String::trim).toList()
        return (resolved to lines).takeIf { lines.firstOrNull() == "#EXTM3U" }
    }

    private fun media(url: HttpUrl, maxBytes: Long, manifest: Boolean, range: LongRange? = null): Pair<HttpUrl, ByteArray>? {
        var current = url
        repeat(4) {
            if (!allowedMedia(current) || maxBytes <= 0) return null
            val request = Request.Builder().url(current).apply {
                range?.let { header("Range", "bytes=${it.first}-${it.last}") }
            }.build()
            val redirect = client.newCall(request).execute().use { response ->
                if (response.isRedirect) return@use response.header("Location") ?: return null
                val bytes = readMediaBody(response, maxBytes, manifest, range) ?: return null
                return current to bytes
            }
            current = current.resolve(redirect) ?: return null
        }
        return null
    }

    private fun readMediaBody(response: Response, maxBytes: Long, manifest: Boolean, range: LongRange?): ByteArray? {
        if (!response.isSuccessful) return null
        if (range != null && (response.code != 206 || response.header("Content-Range")?.startsWith("bytes ${range.first}-${range.last}/") != true)) return null
        val mime = response.body.contentType()?.toString()?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        val acceptedTypes = if (manifest) MANIFEST_TYPES else SEGMENT_TYPES
        if (mime !in acceptedTypes) return null
        val bytes = response.peekBody(maxBytes + 1).bytes()
        return bytes.takeIf {
            it.isNotEmpty() && it.size <= maxBytes && (range == null || it.size.toLong() == range.last - range.first + 1)
        }
    }

    private fun json(request: Request): JSONObject? = client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val bytes = response.peekBody(JSON_LIMIT + 1).bytes()
        return bytes.takeIf { it.size <= JSON_LIMIT }?.let { JSONObject(it.toString(Charsets.UTF_8)) }
    }

    internal fun allowedMedia(url: HttpUrl): Boolean = url.isHttps && url.port == 443 &&
        url.username.isEmpty() && url.password.isEmpty() &&
        MEDIA_DOMAINS.any { url.host == it || url.host.endsWith(".$it") }

    private const val EDITORIAL_FIELDS = "editorialVideo,editorialArtwork"
    private const val JSON_LIMIT = 512L * 1024L
    private val TOKEN = Regex("""ey[A-Za-z0-9_-]+[.]ey[A-Za-z0-9_-]+[.][A-Za-z0-9_-]+""")
    private val RESOLUTION = Regex("""RESOLUTION=[0-9]+x([0-9]+)""")
    private val INIT_URI = Regex("""URI="([^"]+)"""")
    private val INIT_RANGE = Regex("""BYTERANGE="([0-9@]+)"""")
    private const val MAX_SEGMENT_BYTES = 4L * 1024L * 1024L
    private const val MAX_MEDIA_OFFSET = 200L * 1024L * 1024L
    private val MEDIA_DOMAINS = setOf("itunes.apple.com", "mzstatic.com")
    private val MANIFEST_TYPES = setOf("application/vnd.apple.mpegurl", "application/x-mpegurl", "audio/mpegurl")
    private val SEGMENT_TYPES = setOf("video/mp4", "video/iso.segment", "video/mp2t", "application/octet-stream")
}
