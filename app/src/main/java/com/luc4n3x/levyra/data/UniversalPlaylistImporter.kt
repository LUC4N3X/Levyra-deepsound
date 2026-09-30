package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.playlistimport.toImportIdentity
import com.luc4n3x.levyra.data.playlistimport.toMatchCandidate
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.sharedmedia.LevyraSharedPlaylist
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextParsers
import java.net.InetAddress
import java.util.Locale
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
import timber.log.Timber

internal data class SpotifyPlaylistPage(
    val title: String,
    val trackUrls: List<String>,
    val declaredTrackCount: Int? = null
)

internal data class SpotifyTrackMetadata(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUrl: String
)

private const val SHARED_RESOLUTION_CONCURRENCY = 4
private const val SHARED_CANDIDATE_LIMIT = 8
private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val META_TAG_PATTERN = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
private val META_ATTRIBUTE_PATTERN = Regex(
    """([A-Za-z_:][A-Za-z0-9_.:-]*)\s*=\s*(["'])(.*?)\2""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val DECIMAL_HTML_ENTITY = Regex("""&#(\d+);""")
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
    val declaredTrackCount = spotifyMetaValues(html, "music:song_count")
        .firstOrNull()
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it >= 0 }
    return SpotifyPlaylistPage(title, trackUrls, declaredTrackCount)
}

internal fun parseSpotifyTrackPage(html: String): SpotifyTrackMetadata? {
    val title = spotifyMetaValues(html, "og:title").firstOrNull().orEmpty().trim()
    if (title.isBlank()) return null

    val explicitArtist = spotifyMetaValues(html, "music:musician_description")
        .firstOrNull()
        .orEmpty()
        .trim()
    val twitterDescription = spotifyMetaValues(html, "twitter:description").firstOrNull().orEmpty()
    val genericDescription = spotifyMetaValues(html, "description").firstOrNull().orEmpty()
    val artist = explicitArtist.ifBlank {
        spotifyArtistFromDescription(twitterDescription, genericDescription, title)
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

private fun spotifyArtistFromDescription(twitter: String, generic: String, title: String): String {
    val twitterArtist = twitter.split(" · ", limit = 2).firstOrNull().orEmpty().trim()
    if (twitterArtist.isNotBlank() && !twitterArtist.equals(title, ignoreCase = true)) return twitterArtist

    val genericParts = generic.split(" · ").map(String::trim).filter(String::isNotBlank)
    if (genericParts.isEmpty()) return ""
    if (generic.contains(" on Spotify.", ignoreCase = true) || generic.startsWith("Listen to ", ignoreCase = true)) {
        return genericParts.getOrNull(1).orEmpty().trim()
    }
    return genericParts.firstOrNull().orEmpty().trim().takeUnless { it.equals(title, ignoreCase = true) }.orEmpty()
}

internal fun validateSpotifyImportUrl(value: String): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.port != 443) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    if (!isAllowedSpotifyHost(url.host.lowercase(Locale.ROOT))) return null
    return url
}

internal fun spotifyHtmlContentTypeAccepted(value: String?): Boolean {
    val mime = value
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    return mime == "text/html" || mime == "application/xhtml+xml"
}

private fun spotifyMetaValues(html: String, key: String): List<String> {
    val expected = key.lowercase(Locale.ROOT)
    return META_TAG_PATTERN.findAll(html).mapNotNull { match ->
        val attributes = META_ATTRIBUTE_PATTERN.findAll(match.value)
            .associate { attr ->
                attr.groupValues[1].lowercase(Locale.ROOT) to decodeHtmlEntities(attr.groupValues[3])
            }
        val selector = attributes["property"] ?: attributes["name"] ?: return@mapNotNull null
        if (selector.lowercase(Locale.ROOT) != expected) return@mapNotNull null
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

internal fun bestPlaylistImportCandidate(
    identity: ImportedTrackIdentity,
    candidates: List<Track>
): Track? {
    val tracksById = candidates.associateBy { it.id }
    val outcome = PlaylistMatchEngine.select(identity, candidates.map { it.toMatchCandidate().candidate })
    return outcome.selected?.takeIf { outcome.confidence.autoAccepted }?.candidate?.id?.let(tracksById::get)
}

class UniversalPlaylistImporter(
    context: Context,
    private val playlistStore: PlaylistStore = PlaylistStore(context),
    private val youtubeRepository: YoutubeMusicRepository = YoutubeMusicRepository(context)
) {
    suspend fun resolveSharedPlaylistTracks(
        playlist: LevyraSharedPlaylist,
        languageCode: String = "en"
    ): List<Track> = withContext(Dispatchers.IO) {
        val limiter = Semaphore(SHARED_RESOLUTION_CONCURRENCY)
        coroutineScope {
            playlist.tracks
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
                .mapIndexed { index, shared ->
                    async {
                        if (YOUTUBE_VIDEO_ID.matches(shared.id)) return@async sharedTrack(shared.id, shared.title, shared.artist)
                        limiter.withPermit {
                            try {
                                val identity = ImportedTrackIdentity(index, shared.title, PlaylistTextParsers.splitArtists(shared.artist))
                                val query = listOf(shared.title, identity.primaryArtist).filter(String::isNotBlank).joinToString(" ")
                                if (query.length < 2) return@withPermit null
                                bestPlaylistImportCandidate(identity, youtubeRepository.search(query, SHARED_CANDIDATE_LIMIT, languageCode))
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                Timber.d(error, "Shared playlist entry could not be resolved")
                                null
                            }
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    private fun sharedTrack(id: String, title: String, artist: String) = Track(
        id = id,
        title = title.ifBlank { id },
        artist = artist,
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "Levyra playlist",
        moodTags = setOf("music", "shared"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )
}

internal fun spotifyHeadOnly(html: String): String {
    val end = html.indexOf("</head>", ignoreCase = true)
    return if (end >= 0) html.substring(0, end + "</head>".length) else html
}

private val SPOTIFY_REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
private val SPOTIFY_ALLOWED_HOSTS = setOf("open.spotify.com", "spotify.com", "www.spotify.com", "spotify.link")

private fun isAllowedSpotifyHost(host: String): Boolean = host in SPOTIFY_ALLOWED_HOSTS

internal fun isPublicNetworkAddress(address: InetAddress): Boolean {
    if (
        address.isAnyLocalAddress ||
        address.isLoopbackAddress ||
        address.isLinkLocalAddress ||
        address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) return false

    val bytes = address.address
    val ipv4Bytes = when {
        bytes.size == 4 -> bytes
        bytes.size == 16 &&
            bytes.take(10).all { it == 0.toByte() } &&
            bytes[10] == 0xFF.toByte() &&
            bytes[11] == 0xFF.toByte() -> bytes.copyOfRange(12, 16)
        else -> null
    }
    if (ipv4Bytes != null) return isPublicIpv4Address(ipv4Bytes)
    if (bytes.size != 16) return false

    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    if ((first and 0xFE) == 0xFC) return false
    if (first == 0x01 && second == 0x00 && bytes.drop(2).take(6).all { it == 0.toByte() }) return false
    if (first == 0x20 && second == 0x01) {
        val third = bytes[2].toInt() and 0xFF
        val fourth = bytes[3].toInt() and 0xFF
        if (third <= 0x01) return false
        if (third == 0x0D && fourth == 0xB8) return false
    }
    if (first == 0x20 && second == 0x02) return false
    if (first == 0x3F && (second and 0xF0) == 0xF0) return false
    return true
}

private fun isPublicIpv4Address(bytes: ByteArray): Boolean {
    if (bytes.size != 4) return false
    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    val third = bytes[2].toInt() and 0xFF
    return when {
        first == 0 -> false
        first == 10 -> false
        first == 100 && second in 64..127 -> false
        first == 127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 0 && third == 0 -> false
        first == 192 && second == 0 && third == 2 -> false
        first == 192 && second == 31 && third == 196 -> false
        first == 192 && second == 52 && third == 193 -> false
        first == 192 && second == 88 && third == 99 -> false
        first == 192 && second == 168 -> false
        first == 192 && second == 175 && third == 48 -> false
        first == 198 && second in 18..19 -> false
        first == 198 && second == 51 && third == 100 -> false
        first == 203 && second == 0 && third == 113 -> false
        first >= 224 -> false
        else -> true
    }
}
