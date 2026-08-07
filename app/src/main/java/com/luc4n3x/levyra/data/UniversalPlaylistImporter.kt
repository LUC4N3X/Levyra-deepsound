package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

sealed class PlaylistImportResult {
    data class Success(val playlist: Playlist, val importedCount: Int) : PlaylistImportResult()
    data class Failure(val reason: String) : PlaylistImportResult()
}

internal data class SpotifyPlaylistPage(
    val title: String,
    val trackUrls: List<String>
)

internal data class SpotifyTrackMetadata(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUrl: String
)

internal object PlaylistImportCoordinator {
    private val active = AtomicBoolean(false)

    fun tryBegin(): Boolean = active.compareAndSet(false, true)

    fun finish() {
        active.set(false)
    }
}

internal const val MAX_JSON_IMPORT_TRACKS = 500
private const val MAX_SPOTIFY_IMPORT_TRACKS = 100
private const val MAX_IMPORT_INPUT_CHARS = 2_000_000
private const val IMPORT_RESOLUTION_CONCURRENCY = 4
private const val MAX_SPOTIFY_REDIRECTS = 4
private const val MAX_SPOTIFY_HTML_BYTES = 2L * 1024L * 1024L
private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val META_TAG_PATTERN = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val META_ATTRIBUTE_PATTERN = Regex("([A-Za-z_:][A-Za-z0-9_.:-]*)\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
private val DECIMAL_HTML_ENTITY = Regex("&#(\\d+);")
private val HEX_HTML_ENTITY = Regex("&#x([0-9A-Fa-f]+);")

internal fun parseSpotifyPlaylistPage(html: String): SpotifyPlaylistPage {
    val title = spotifyMetaValues(html, "og:title")
        .firstOrNull()
        .orEmpty()
        .substringBefore(" | Spotify")
        .trim()
        .ifBlank { "Spotify Playlist" }
    val trackUrls = spotifyMetaValues(html, "music:song")
        .mapNotNull(::normalizeSpotifyTrackUrl)
        .distinct()
    return SpotifyPlaylistPage(title = title, trackUrls = trackUrls)
}

internal fun parseSpotifyTrackPage(html: String): SpotifyTrackMetadata? {
    val title = spotifyMetaValues(html, "og:title").firstOrNull().orEmpty().trim()
    if (title.isBlank()) return null

    val description = spotifyMetaValues(html, "twitter:description").firstOrNull()
        .orEmpty()
        .ifBlank { spotifyMetaValues(html, "description").firstOrNull().orEmpty() }
    val artist = spotifyMetaValues(html, "music:musician_description").firstOrNull()
        .orEmpty()
        .trim()
        .ifBlank {
            description.substringBefore(" · ").substringBefore(" - ").trim()
        }
    if (artist.isBlank()) return null

    val durationMs = spotifyMetaValues(html, "music:duration")
        .firstOrNull()
        ?.toLongOrNull()
        ?.coerceAtLeast(0L)
        ?.times(1000L)
        ?: 0L
    val artworkUrl = spotifyMetaValues(html, "og:image").firstOrNull().orEmpty().trim()
    return SpotifyTrackMetadata(title, artist, durationMs, artworkUrl)
}

internal fun importedDurationMs(item: JSONObject): Long {
    val explicitMs = item.optLong("durationMs", 0L)
    if (explicitMs > 0L) return explicitMs
    val duration = item.optLong("duration", 0L)
    if (duration <= 0L) return 0L
    // Compatible exports are seen in both seconds and milliseconds.
    return if (duration > 86_400L) duration else duration * 1000L
}

internal fun jsonImportTrackCountAccepted(count: Int): Boolean = count in 0..MAX_JSON_IMPORT_TRACKS

internal fun validateSpotifyImportUrl(value: String): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps) return null
    val host = url.host.lowercase()
    if (!isAllowedSpotifyHost(host)) return null
    return url
}

private fun spotifyMetaValues(html: String, key: String): List<String> {
    val expected = key.lowercase()
    return META_TAG_PATTERN.findAll(html).mapNotNull { match ->
        val attributes = META_ATTRIBUTE_PATTERN.findAll(match.value)
            .associate { attr ->
                attr.groupValues[1].lowercase() to decodeHtmlEntities(attr.groupValues[2])
            }
        val selector = attributes["property"] ?: attributes["name"] ?: return@mapNotNull null
        if (selector.lowercase() != expected) return@mapNotNull null
        attributes["content"]?.takeIf(String::isNotBlank)
    }.toList()
}

private fun normalizeSpotifyTrackUrl(value: String): String? {
    val clean = value.trim()
    return when {
        clean.startsWith("spotify:track:", ignoreCase = true) -> {
            val id = clean.substringAfterLast(':').trim()
            id.takeIf(String::isNotBlank)?.let { "https://open.spotify.com/track/$it" }
        }
        else -> {
            val url = validateSpotifyImportUrl(clean) ?: return null
            val path = url.encodedPath
            if (url.host.equals("open.spotify.com", ignoreCase = true) && path.startsWith("/track/")) {
                "https://open.spotify.com/track/${path.substringAfter("/track/").substringBefore('/')}"
            } else {
                null
            }
        }
    }
}

private fun decodeHtmlEntities(value: String): String {
    var decoded = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
    decoded = DECIMAL_HTML_ENTITY.replace(decoded) { match ->
        match.groupValues[1].toIntOrNull()?.let(::codePointToString) ?: match.value
    }
    decoded = HEX_HTML_ENTITY.replace(decoded) { match ->
        match.groupValues[1].toIntOrNull(16)?.let(::codePointToString) ?: match.value
    }
    return decoded
}

private fun codePointToString(codePoint: Int): String =
    runCatching { String(Character.toChars(codePoint)) }.getOrDefault("")

class UniversalPlaylistImporter(
    context: Context,
    private val playlistStore: PlaylistStore = PlaylistStore(context),
    private val youtubeRepository: YoutubeMusicRepository = YoutubeMusicRepository(context),
    httpClient: OkHttpClient = LevyraHttpClientFactory.media(context.applicationContext)
) {
    private val spotifyHttpClient = httpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun importFromUrlOrJson(input: String, customName: String? = null): PlaylistImportResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext PlaylistImportResult.Failure("Input vuoto")
        if (trimmed.length > MAX_IMPORT_INPUT_CHARS) {
            return@withContext PlaylistImportResult.Failure("Il contenuto da importare è troppo grande")
        }
        if (!PlaylistImportCoordinator.tryBegin()) {
            return@withContext PlaylistImportResult.Failure("Un'importazione playlist è già in corso")
        }

        try {
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed, customName)
                isYoutubeUrl(trimmed) -> importFromYoutubeUrl(trimmed, customName)
                isSpotifyUrl(trimmed) -> importFromSpotifyUrl(trimmed, customName)
                else -> PlaylistImportResult.Failure("Formato o URL non supportato")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Playlist import failed")
            PlaylistImportResult.Failure(error.message ?: "Errore durante l'importazione")
        } finally {
            PlaylistImportCoordinator.finish()
        }
    }

    private suspend fun importFromYoutubeUrl(url: String, customName: String?): PlaylistImportResult {
        val listId = extractQueryParam(url, "list")
        if (listId.isBlank()) return PlaylistImportResult.Failure("ID playlist YouTube non trovato nel link")

        val fetchedPlaylist = youtubeRepository.playlist(listId, limit = 300)
            ?: return PlaylistImportResult.Failure("Impossibile recuperare la playlist da YouTube")
        if (fetchedPlaylist.tracks.isEmpty()) {
            return PlaylistImportResult.Failure("La playlist YouTube non contiene brani o non è pubblica")
        }

        val name = customName?.ifBlank { null } ?: fetchedPlaylist.title.ifBlank { "YouTube Playlist" }
        return persistPlaylist(name, fetchedPlaylist.tracks)
    }

    private suspend fun importFromSpotifyUrl(url: String, customName: String?): PlaylistImportResult {
        val playlistHtml = fetchSpotifyText(url)
        val page = parseSpotifyPlaylistPage(playlistHtml)
        if (page.trackUrls.isEmpty()) {
            return PlaylistImportResult.Failure("Non riesco a leggere i brani di questa playlist Spotify. Verifica che sia pubblica.")
        }

        val limiter = Semaphore(IMPORT_RESOLUTION_CONCURRENCY)
        val resolvedTracks = coroutineScope {
            page.trackUrls.take(MAX_SPOTIFY_IMPORT_TRACKS).map { trackUrl ->
                async {
                    limiter.withPermit {
                        try {
                            val metadata = parseSpotifyTrackPage(fetchSpotifyText(trackUrl)) ?: return@withPermit null
                            val resolved = youtubeRepository.searchOne("${metadata.title} ${metadata.artist}")
                                ?: return@withPermit null
                            resolved.copy(
                                thumbnailUrl = resolved.thumbnailUrl.ifBlank { metadata.artworkUrl },
                                largeThumbnailUrl = resolved.largeThumbnailUrl.ifBlank {
                                    resolved.thumbnailUrl.ifBlank { metadata.artworkUrl }
                                },
                                durationMs = resolved.durationMs.takeIf { it > 0L } ?: metadata.durationMs,
                                source = "Spotify import"
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Timber.d(error, "Skipping unresolved Spotify track %s", trackUrl)
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (resolvedTracks.isEmpty()) {
            return PlaylistImportResult.Failure("Nessun brano Spotify è stato risolto nel catalogo di Levyra")
        }
        val name = customName?.ifBlank { null } ?: page.title
        return persistPlaylist(name, resolvedTracks)
    }

    private suspend fun importFromJson(jsonText: String, customName: String?): PlaylistImportResult {
        val rawTracks = mutableListOf<Track>()
        var extractedName: String? = null

        if (jsonText.startsWith("[")) {
            val array = JSONArray(jsonText)
            if (!jsonImportTrackCountAccepted(array.length())) {
                return PlaylistImportResult.Failure("La playlist supera il limite di $MAX_JSON_IMPORT_TRACKS brani")
            }
            parseJsonArrayToTracks(array, rawTracks)
        } else {
            val obj = JSONObject(jsonText)
            extractedName = obj.optString("name").ifBlank { obj.optString("title").ifBlank { null } }
            val tracksArray = obj.optJSONArray("tracks")
                ?: obj.optJSONArray("songs")
                ?: obj.optJSONArray("queue")
                ?: return PlaylistImportResult.Failure("Nessun elenco di brani compatibile trovato nel JSON")
            if (!jsonImportTrackCountAccepted(tracksArray.length())) {
                return PlaylistImportResult.Failure("La playlist supera il limite di $MAX_JSON_IMPORT_TRACKS brani")
            }
            parseJsonArrayToTracks(tracksArray, rawTracks)
        }

        if (rawTracks.isEmpty()) {
            return PlaylistImportResult.Failure("Nessun brano valido trovato nel JSON")
        }
        val playableTracks = resolveImportedTracks(rawTracks.take(MAX_JSON_IMPORT_TRACKS))
        if (playableTracks.isEmpty()) {
            return PlaylistImportResult.Failure("I brani importati non sono stati riconosciuti nel catalogo di Levyra")
        }
        val name = customName?.ifBlank { null } ?: extractedName ?: "Imported Playlist"
        return persistPlaylist(name, playableTracks)
    }

    private suspend fun resolveImportedTracks(tracks: List<Track>): List<Track> {
        val boundedTracks = tracks.take(MAX_JSON_IMPORT_TRACKS)
        val limiter = Semaphore(IMPORT_RESOLUTION_CONCURRENCY)
        return coroutineScope {
            boundedTracks.map { track ->
                async {
                    if (YOUTUBE_VIDEO_ID.matches(track.id)) return@async track
                    limiter.withPermit {
                        try {
                            val resolved = youtubeRepository.searchOne("${track.title} ${track.artist}")
                                ?: return@withPermit null
                            resolved.copy(
                                thumbnailUrl = track.thumbnailUrl.ifBlank { resolved.thumbnailUrl },
                                largeThumbnailUrl = track.largeThumbnailUrl.ifBlank {
                                    track.thumbnailUrl.ifBlank { resolved.largeThumbnailUrl.ifBlank { resolved.thumbnailUrl } }
                                },
                                durationMs = resolved.durationMs.takeIf { it > 0L } ?: track.durationMs,
                                source = "Imported playlist"
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Throwable) {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun parseJsonArrayToTracks(array: JSONArray, outTracks: MutableList<Track>) {
        val boundedLength = minOf(array.length(), MAX_JSON_IMPORT_TRACKS)
        for (index in 0 until boundedLength) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("videoId").ifBlank { item.optString("id") }.trim()
            val title = item.optString("title").ifBlank { item.optString("name") }.trim()
            if (id.isBlank() || title.isBlank()) continue

            val artist = importedArtist(item)
            val album = item.optString("album").ifBlank { item.optString("albumName") }.trim()
            val thumb = item.optString("thumbnailUrl")
                .ifBlank { item.optString("artworkUrl") }
                .ifBlank { item.optString("thumbnail") }
                .trim()
            outTracks += Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = importedDurationMs(item),
                streamUrl = "",
                videoUrl = if (YOUTUBE_VIDEO_ID.matches(id)) "https://www.youtube.com/watch?v=$id" else "",
                thumbnailUrl = thumb,
                largeThumbnailUrl = thumb,
                source = "Imported playlist",
                moodTags = setOf("music", "imported"),
                energy = 50,
                vocal = 50,
                replayScore = 50,
                cacheScore = 50,
                accentStart = 0,
                accentEnd = 0
            )
        }
    }

    private fun importedArtist(item: JSONObject): String {
        val direct = item.optString("artist").ifBlank { item.optString("artistName") }.trim()
        if (direct.isNotBlank()) return direct
        val artists = item.optJSONArray("artists") ?: return "Unknown Artist"
        return buildList {
            for (index in 0 until artists.length()) {
                val value = artists.opt(index)
                val name = when (value) {
                    is JSONObject -> value.optString("name")
                    is String -> value
                    else -> ""
                }.trim()
                if (name.isNotBlank()) add(name)
            }
        }.distinct().joinToString(", ").ifBlank { "Unknown Artist" }
    }

    private suspend fun persistPlaylist(name: String, tracks: List<Track>): PlaylistImportResult {
        val cleanTracks = tracks
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        if (cleanTracks.isEmpty()) return PlaylistImportResult.Failure("Nessun brano importabile trovato")

        val created = playlistStore.createWithTracks(name, cleanTracks)
        return PlaylistImportResult.Success(created, cleanTracks.size)
    }

    private fun fetchSpotifyText(url: String): String {
        var currentUrl = validateSpotifyImportUrl(url)
            ?: throw IOException("URL Spotify non valido o non sicuro")
        var redirectCount = 0

        while (true) {
            ensurePublicSpotifyDestination(currentUrl)
            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .get()
                .build()
            val response = spotifyHttpClient.newCall(request).execute()
            try {
                if (response.code in SPOTIFY_REDIRECT_CODES) {
                    if (redirectCount >= MAX_SPOTIFY_REDIRECTS) throw IOException("Troppi reindirizzamenti Spotify")
                    val location = response.header("Location") ?: throw IOException("Reindirizzamento Spotify non valido")
                    val next = currentUrl.resolve(location)
                        ?: throw IOException("Destinazione Spotify non valida")
                    currentUrl = validateSpotifyImportUrl(next.toString())
                        ?: throw IOException("Reindirizzamento Spotify non consentito")
                    redirectCount += 1
                    continue
                }
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} durante l'importazione")

                val body = response.body
                val declaredLength = body.contentLength()
                if (declaredLength > MAX_SPOTIFY_HTML_BYTES) {
                    throw IOException("Risposta Spotify troppo grande")
                }
                return body.byteStream().use { input ->
                    readUtf8Bounded(input, MAX_SPOTIFY_HTML_BYTES)
                        ?: throw IOException("Risposta Spotify troppo grande")
                }
            } finally {
                response.close()
            }
        }
    }

    private fun ensurePublicSpotifyDestination(url: HttpUrl) {
        if (!url.isHttps || !isAllowedSpotifyHost(url.host.lowercase())) {
            throw IOException("Destinazione Spotify non consentita")
        }
        val addresses = try {
            InetAddress.getAllByName(url.host)
        } catch (error: Throwable) {
            throw IOException("Impossibile risolvere il dominio Spotify", error)
        }
        if (addresses.isEmpty() || addresses.any { !isPublicNetworkAddress(it) }) {
            throw IOException("Destinazione di rete Spotify non consentita")
        }
    }

    private fun isYoutubeUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host.orEmpty().lowercase()
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }

    private fun isSpotifyUrl(value: String): Boolean = validateSpotifyImportUrl(value) != null

    private fun extractQueryParam(url: String, key: String): String {
        val rawQuery = runCatching { URI(url).rawQuery.orEmpty() }.getOrDefault("")
        if (rawQuery.isBlank()) return ""
        return rawQuery.split('&')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator < 0) null else part.substring(0, separator) to part.substring(separator + 1)
            }
            .firstOrNull { (name, _) -> name == key }
            ?.second
            ?.let { encoded -> runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded) }
            .orEmpty()
    }
}

private val SPOTIFY_REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

private fun isAllowedSpotifyHost(host: String): Boolean =
    host == "spotify.com" || host.endsWith(".spotify.com") || host == "spotify.link"

private fun isPublicNetworkAddress(address: InetAddress): Boolean {
    if (
        address.isAnyLocalAddress ||
        address.isLoopbackAddress ||
        address.isLinkLocalAddress ||
        address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) return false

    val bytes = address.address
    if (bytes.size == 4) {
        val first = bytes[0].toInt() and 0xFF
        val second = bytes[1].toInt() and 0xFF
        if (first == 0 || first == 127 || first >= 224) return false
        if (first == 100 && second in 64..127) return false
        if (first == 169 && second == 254) return false
    } else if (bytes.size == 16) {
        val first = bytes[0].toInt() and 0xFF
        if ((first and 0xFE) == 0xFC) return false // fc00::/7 unique-local
    }
    return true
}
