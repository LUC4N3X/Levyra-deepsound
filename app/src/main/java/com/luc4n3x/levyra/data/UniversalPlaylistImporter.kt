package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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

private const val MAX_SPOTIFY_IMPORT_TRACKS = 100
private const val IMPORT_RESOLUTION_CONCURRENCY = 4
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
    // ViMusic/InnerTune exports are seen in both seconds and milliseconds.
    return if (duration > 86_400L) duration else duration * 1000L
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
            val uri = runCatching { URI(clean) }.getOrNull() ?: return null
            val host = uri.host.orEmpty().lowercase()
            val path = uri.path.orEmpty()
            if (host == "open.spotify.com" && path.startsWith("/track/")) {
                "https://open.spotify.com/track/${path.substringAfter("/track/").substringBefore('/').substringBefore('?')}"
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
    private val httpClient: OkHttpClient = LevyraHttpClientFactory.media(context.applicationContext)
) {

    suspend fun importFromUrlOrJson(input: String, customName: String? = null): PlaylistImportResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext PlaylistImportResult.Failure("Input vuoto")

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
        val playlistHtml = fetchText(url)
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
                            val metadata = parseSpotifyTrackPage(fetchText(trackUrl)) ?: return@withPermit null
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
            parseJsonArrayToTracks(JSONArray(jsonText), rawTracks)
        } else {
            val obj = JSONObject(jsonText)
            extractedName = obj.optString("name").ifBlank { obj.optString("title").ifBlank { null } }
            val tracksArray = obj.optJSONArray("tracks")
                ?: obj.optJSONArray("songs")
                ?: obj.optJSONArray("queue")
            tracksArray?.let { parseJsonArrayToTracks(it, rawTracks) }
        }

        if (rawTracks.isEmpty()) {
            return PlaylistImportResult.Failure("Nessun brano valido trovato nel JSON")
        }
        val playableTracks = resolveImportedTracks(rawTracks)
        if (playableTracks.isEmpty()) {
            return PlaylistImportResult.Failure("I brani del JSON non sono stati riconosciuti nel catalogo di Levyra")
        }
        val name = customName?.ifBlank { null } ?: extractedName ?: "Imported Playlist"
        return persistPlaylist(name, playableTracks)
    }

    private suspend fun resolveImportedTracks(tracks: List<Track>): List<Track> {
        val limiter = Semaphore(IMPORT_RESOLUTION_CONCURRENCY)
        return coroutineScope {
            tracks.map { track ->
                async {
                    if (YOUTUBE_VIDEO_ID.matches(track.id)) return@async track
                    limiter.withPermit {
                        try {
                            youtubeRepository.searchOne("${track.title} ${track.artist}")?.copy(
                                thumbnailUrl = track.thumbnailUrl.ifBlank { it.thumbnailUrl },
                                largeThumbnailUrl = track.largeThumbnailUrl.ifBlank {
                                    track.thumbnailUrl.ifBlank { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }
                                },
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
        for (index in 0 until array.length()) {
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

        val created = playlistStore.create(name, cleanTracks.first())
        if (cleanTracks.size > 1) playlistStore.addTracks(created.id, cleanTracks.drop(1))
        val updated = playlistStore.load(created.id) ?: created.copy(tracks = cleanTracks)
        return PlaylistImportResult.Success(updated, updated.tracks.size.coerceAtLeast(cleanTracks.size))
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml")
            .get()
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} durante l'importazione")
            response.body.string()
        }
    }

    private fun isYoutubeUrl(value: String): Boolean {
        val host = runCatching { URI(value).host.orEmpty().lowercase() }.getOrDefault("")
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }

    private fun isSpotifyUrl(value: String): Boolean {
        val host = runCatching { URI(value).host.orEmpty().lowercase() }.getOrDefault("")
        return host == "spotify.com" || host.endsWith(".spotify.com") || host == "spotify.link"
    }

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
