package com.luc4n3x.levyra.feature.motion

import android.content.Context
import android.util.Base64
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppleMotionArtworkProvider(context: Context) : MotionArtworkProvider {
    override val id: String = "apple-motion"

    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val tokenMutex = Mutex()
    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    suspend fun findArtistMotion(artistName: String): MotionArtworkCandidate? {
        val clean = artistName.trim()
        if (clean.length < 2) return null
        val requested = splitArtists(clean)
        if (requested.isEmpty()) return null

        Timber.d("Apple artist motion lookup start artist=%s", clean)
        val developerTokenValue = developerToken()
        var lastFailure: Throwable? = null
        for (catalogStorefront in appleMotionStorefronts(storefront())) {
            val selected = try {
                Timber.d("Apple artist motion storefront=%s artist=%s", catalogStorefront, clean)
                findArtistMotionInStorefront(clean, requested, catalogStorefront, developerTokenValue)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.d(error, "Apple artist motion storefront failed storefront=%s artist=%s", catalogStorefront, clean)
                lastFailure = error
                continue
            }
            if (selected == null) {
                Timber.d("Apple artist motion storefront miss storefront=%s artist=%s", catalogStorefront, clean)
                continue
            }

            Timber.d(
                "Apple artist motion selected storefront=%s artist=%s appleArtistId=%s",
                catalogStorefront,
                selected.name,
                selected.key
            )
            return MotionArtworkCandidate(
                provider = id,
                scope = MotionArtworkScope.ARTIST,
                identity = MotionTrackIdentity(
                    title = selected.name,
                    artists = splitArtists(selected.name),
                    album = "",
                    durationMs = 0L,
                    isrc = "",
                    upc = "",
                    year = "",
                    trackId = "",
                    albumId = ""
                ),
                url = selected.video.url,
                mimeType = "application/x-mpegURL",
                width = selected.video.width,
                height = selected.video.height,
                expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
            )
        }

        if (lastFailure != null) {
            throw MotionProviderException("Apple artist motion lookup failed", lastFailure)
        }
        Timber.d("Apple artist motion conclusive miss artist=%s", clean)
        return null
    }

    private suspend fun findArtistMotionInStorefront(
        artistName: String,
        requested: List<String>,
        storefront: String,
        token: String
    ): AppleArtistMotionMatch? {
        val url = "$AMP_BASE_URL/v1/catalog/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", artistName)
            .addQueryParameter("types", "artists")
            .addQueryParameter("limit", "5")
            .build()
        val data = requestJson(url.toString(), token)
            .optJSONObject("results")
            ?.optJSONObject("artists")
            ?.optJSONArray("data")
            ?: run {
                Timber.d("Apple artist search returned no data storefront=%s artist=%s", storefront, artistName)
                return null
            }

        val compatible = ArrayList<AppleArtistSearchMatch>(data.length())
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val attributes = item.optJSONObject("attributes") ?: continue
            val name = attributes.optString("name").trim()
            val artistId = item.optString("id").trim()
            if (name.isBlank() || artistId.isBlank() || isUnsafeResult(name, "")) continue
            if (!artistMatches(requested, splitArtists(name))) continue
            compatible += AppleArtistSearchMatch(artistId, name)
        }
        Timber.d(
            "Apple artist search storefront=%s artist=%s raw=%d compatible=%d",
            storefront,
            artistName,
            data.length(),
            compatible.size
        )
        if (compatible.isEmpty()) return null

        val normalizedRequested = normalizeMotionText(artistName)
        val exact = compatible.filter { normalizeMotionText(it.name) == normalizedRequested }
        val candidates = (exact.ifEmpty { compatible }).take(MAX_ARTIST_DETAIL_CANDIDATES)

        for (candidate in candidates) {
            Timber.d(
                "Apple artist detail check storefront=%s appleArtistId=%s name=%s exact=%b",
                storefront,
                candidate.id,
                candidate.name,
                normalizeMotionText(candidate.name) == normalizedRequested
            )
            val video = fetchArtistMotionByAppleId(candidate.id, storefront, token)
            if (video != null) {
                Timber.d(
                    "Apple artist detail motion found storefront=%s appleArtistId=%s name=%s",
                    storefront,
                    candidate.id,
                    candidate.name
                )
                return AppleArtistMotionMatch(candidate.id, candidate.name, video)
            }
            Timber.d(
                "Apple artist detail has no motion storefront=%s appleArtistId=%s name=%s",
                storefront,
                candidate.id,
                candidate.name
            )
        }
        return null
    }

    private suspend fun fetchArtistMotionByAppleId(
        artistId: String,
        storefront: String,
        token: String
    ): AppleEditorialVideo? {
        val url = "$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId".toHttpUrl().newBuilder()
            .addQueryParameter("extend", "editorialVideo,editorialArtwork")
            .build()
        val attributes = requestJson(url.toString(), token)
            .optJSONArray("data")
            ?.optJSONObject(0)
            ?.optJSONObject("attributes")
            ?: return null
        val video = selectAppleArtistEditorialVideo(attributes)
        Timber.d(
            "Apple artist detail parsed storefront=%s appleArtistId=%s editorialVideo=%b editorialArtwork=%b selected=%b",
            storefront,
            artistId,
            attributes.optJSONObject("editorialVideo") != null,
            attributes.optJSONObject("editorialArtwork") != null,
            video != null
        )
        return video
    }

    private fun storefront(): String =
        Locale.getDefault().country.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        return try {
            val developerTokenValue = developerToken()
            var lastFailure: Throwable? = null
            for (catalogStorefront in appleMotionStorefronts(storefront())) {
                Timber.d(
                    "Apple player motion lookup storefront=%s title=%s artists=%s album=%s",
                    catalogStorefront,
                    identity.title,
                    identity.artists.joinToString(),
                    identity.album
                )
                val result = try {
                    findPlayerMotionInStorefront(identity, catalogStorefront, developerTokenValue)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Timber.d(
                        error,
                        "Apple player motion storefront failed storefront=%s title=%s",
                        catalogStorefront,
                        identity.title
                    )
                    lastFailure = error
                    null
                }
                if (result is MotionArtworkProviderResult.Found) return result
                if (result == MotionArtworkProviderResult.NoMatch) {
                    Timber.d("Apple player motion storefront miss storefront=%s title=%s", catalogStorefront, identity.title)
                }
            }
            if (lastFailure != null) {
                MotionArtworkProviderResult.Failed(lastFailure)
            } else {
                Timber.d("Apple player motion conclusive miss title=%s", identity.title)
                MotionArtworkProviderResult.NoMatch
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Apple motion provider failed")
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private suspend fun findPlayerMotionInStorefront(
        identity: MotionTrackIdentity,
        storefront: String,
        token: String
    ): MotionArtworkProviderResult {
        val songCandidates = search(identity, storefront, token, "songs")
        if (songCandidates.isNotEmpty()) {
            Timber.d("Apple player motion songs found=%d storefront=%s title=%s", songCandidates.size, storefront, identity.title)
            return MotionArtworkProviderResult.Found(songCandidates)
        }
        Timber.d("Apple player motion songs miss storefront=%s title=%s; trying albums", storefront, identity.title)
        val albumCandidates = search(identity, storefront, token, "albums")
        if (albumCandidates.isNotEmpty()) {
            Timber.d("Apple player motion albums found=%d storefront=%s title=%s", albumCandidates.size, storefront, identity.title)
            return MotionArtworkProviderResult.Found(albumCandidates)
        }
        return MotionArtworkProviderResult.NoMatch
    }

    private suspend fun search(
        identity: MotionTrackIdentity,
        storefront: String,
        token: String,
        type: String
    ): List<MotionArtworkCandidate> {
        val query = if (type == "albums") {
            listOf(identity.album, identity.artists.firstOrNull().orEmpty())
        } else {
            listOf(identity.title, identity.artists.joinToString(" "), identity.album)
        }.filter { it.isNotBlank() }.joinToString(" ")
        val url = "$AMP_BASE_URL/v1/catalog/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("types", type)
            .addQueryParameter("limit", "8")
            .addQueryParameter("extend", "editorialVideo")
            .addQueryParameter("include", "albums")
            .build()
        val root = requestJson(url.toString(), token)
        val data = root.optJSONObject("results")
            ?.optJSONObject(type)
            ?.optJSONArray("data")
            ?: run {
                Timber.d("Apple player search no data storefront=%s type=%s title=%s", storefront, type, identity.title)
                return emptyList()
            }

        val ranked = buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val attributes = item.optJSONObject("attributes") ?: continue
                val artist = attributes.optString("artistName").trim()
                val name = attributes.optString("name").trim()
                val album = attributes.optString("albumName")
                    .ifBlank { attributes.optString("collectionName") }
                    .ifBlank { if (type == "albums") name else "" }
                    .trim()
                if (isUnsafeResult(name, album)) continue
                if (!artistMatches(identity.artists, splitArtists(artist))) continue
                val quickScore = if (type == "songs") {
                    similarity(identity.title, name) * 70.0 + similarity(identity.album, album) * 30.0
                } else {
                    similarity(identity.album, album) * 100.0
                }
                if (quickScore < 70.0) continue
                add(AppleSearchResult(item, attributes, artist, name, album, quickScore))
            }
        }.sortedByDescending { it.quickScore }.take(3)

        Timber.d(
            "Apple player search storefront=%s type=%s title=%s raw=%d ranked=%d",
            storefront,
            type,
            identity.title,
            data.length(),
            ranked.size
        )

        val output = ArrayList<MotionArtworkCandidate>(ranked.size)
        for (result in ranked) {
            val directVideo = extractEditorialVideoUrl(result.attributes.optJSONObject("editorialVideo"))
            val albumId = resolveAlbumId(result.item, result.attributes, type)
            val resolved = if (directVideo != null) {
                Timber.d(
                    "Apple player direct editorial video storefront=%s type=%s appleId=%s title=%s",
                    storefront,
                    type,
                    result.item.optString("id"),
                    result.name
                )
                AppleMotionResult(
                    video = directVideo,
                    albumName = result.album,
                    albumArtist = result.artist,
                    upc = result.attributes.optString("upc"),
                    releaseDate = result.attributes.optString("releaseDate")
                )
            } else if (!albumId.isNullOrBlank() && !albumId.startsWith("pl.")) {
                Timber.d(
                    "Apple player direct video miss; checking album storefront=%s appleAlbumId=%s title=%s",
                    storefront,
                    albumId,
                    result.name
                )
                fetchAlbumMotion(albumId, storefront, token)
            } else {
                null
            } ?: continue

            output += MotionArtworkCandidate(
                provider = id,
                scope = MotionArtworkScope.ALBUM,
                identity = MotionTrackIdentity(
                    title = identity.title,
                    artists = splitArtists(if (resolved.albumArtist.isNotBlank()) resolved.albumArtist else result.artist),
                    album = resolved.albumName.ifBlank { result.album },
                    durationMs = result.attributes.optLong("durationInMillis", 0L),
                    isrc = result.attributes.optString("isrc").uppercase(Locale.ROOT),
                    upc = resolved.upc,
                    year = resolved.releaseDate.take(4),
                    trackId = result.item.optString("id"),
                    albumId = albumId.orEmpty()
                ),
                url = resolved.video.url,
                mimeType = "application/x-mpegURL",
                width = resolved.video.width,
                height = resolved.video.height,
                expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
            )
        }
        Timber.d("Apple player search produced=%d storefront=%s type=%s title=%s", output.size, storefront, type, identity.title)
        return output
    }

    private suspend fun fetchAlbumMotion(
        albumId: String,
        storefront: String,
        token: String
    ): AppleMotionResult? {
        val url = "$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId".toHttpUrl().newBuilder()
            .addQueryParameter("extend", "editorialVideo")
            .build()
        val root = requestJson(url.toString(), token)
        val attributes = root.optJSONArray("data")?.optJSONObject(0)?.optJSONObject("attributes") ?: return null
        val albumName = attributes.optString("name").trim()
        if (isUnsafeResult(albumName, albumName)) return null
        val motionVideo = extractEditorialVideoUrl(attributes.optJSONObject("editorialVideo"))
        Timber.d(
            "Apple album detail storefront=%s appleAlbumId=%s motion=%b album=%s",
            storefront,
            albumId,
            motionVideo != null,
            albumName
        )
        motionVideo ?: return null
        return AppleMotionResult(
            video = motionVideo,
            albumName = albumName,
            albumArtist = attributes.optString("artistName").trim(),
            upc = attributes.optString("upc").trim(),
            releaseDate = attributes.optString("releaseDate").trim()
        )
    }

    private suspend fun developerToken(): String = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { tokenExpiresAt > now + TOKEN_EXPIRY_MARGIN_MS }?.let {
            Timber.d("Apple motion developer token cache hit expiresInMin=%d", (tokenExpiresAt - now) / 60_000L)
            return@withLock it
        }

        Timber.d("Apple motion developer token discovery start")
        val html = requestText(APPLE_BROWSE_URL)
        val scripts = appleMusicScriptUrls(html).take(MAX_TOKEN_SCRIPT_CANDIDATES)
        Timber.d("Apple motion developer token scripts=%d", scripts.size)
        var lastFailure: Throwable? = null
        for ((index, script) in scripts.withIndex()) {
            val source = try {
                requestText(script)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.d(error, "Apple motion developer token script failed index=%d", index)
                lastFailure = error
                continue
            }
            for (match in JWT_REGEX.findAll(source)) {
                val developerTokenValue = match.value
                val expiration = jwtExpiration(developerTokenValue) ?: continue
                if (expiration > now + TOKEN_EXPIRY_MARGIN_MS) {
                    cachedToken = developerTokenValue
                    tokenExpiresAt = expiration
                    Timber.d(
                        "Apple motion developer token discovered scriptIndex=%d expiresInMin=%d",
                        index,
                        (expiration - now) / 60_000L
                    )
                    return@withLock developerTokenValue
                }
            }
        }
        Timber.d(lastFailure, "Apple motion developer token unavailable scripts=%d", scripts.size)
        throw MotionProviderException("Apple Music developer token unavailable", lastFailure)
    }

    private suspend fun requestJson(url: String, token: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Origin", "https://music.apple.com")
            .header("Referer", "https://music.apple.com/")
            .header("User-Agent", USER_AGENT)
            .build()
        val content = executeText(request)
        return runCatching { JSONObject(content) }
            .getOrElse { throw MotionProviderException("Invalid Apple Music response", it) }
    }

    private suspend fun requestText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        return executeText(request)
    }

    private suspend fun executeText(request: Request): String = withContext(Dispatchers.IO) {
        val safePath = request.url.encodedPath
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.d(
                        "Apple motion HTTP failure host=%s path=%s status=%d",
                        request.url.host,
                        safePath,
                        response.code
                    )
                    throw MotionProviderException("Apple Music HTTP ${response.code}")
                }
                response.body.string().takeIf { it.isNotBlank() }
                    ?: throw MotionProviderException("Empty Apple Music response")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: MotionProviderException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Apple motion request failed host=%s path=%s", request.url.host, safePath)
            throw MotionProviderException("Apple motion request failed", error)
        }
    }

    private fun resolveAlbumId(item: JSONObject, attributes: JSONObject, type: String): String? {
        if (type == "albums") return item.optString("id").takeIf { it.isNotBlank() }
        val relationshipId = item.optJSONObject("relationships")
            ?.optJSONObject("albums")
            ?.optJSONArray("data")
            ?.optJSONObject(0)
            ?.optString("id")
            .orEmpty()
        if (relationshipId.isNotBlank()) return relationshipId
        val url = attributes.optString("url")
        return url.substringAfter("/album/", "")
            .substringBefore('?')
            .substringAfterLast('/', "")
            .takeIf { value -> value.isNotBlank() && value.all { it.isDigit() } }
    }

    private fun extractEditorialVideoUrl(editorialVideo: JSONObject?): AppleEditorialVideo? =
        selectAppleEditorialVideo(editorialVideo)

    private fun jwtExpiration(token: String): Long? = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return@runCatching null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8)).optLong("exp", 0L)
            .takeIf { it > 0L }
            ?.times(1000L)
    }.getOrNull()

    private fun isUnsafeResult(title: String, album: String): Boolean {
        val value = normalizeMotionText("$title $album")
        return BLACKLIST.any { motionTextContainsTerm(value, it) }
    }

    private fun artistMatches(requested: List<String>, returned: List<String>): Boolean =
        primaryMotionArtistMatches(requested, returned)

    private fun similarity(first: String, second: String): Double {
        val left = normalizeMotionText(first).split(' ').filter { it.isNotBlank() }.toSet()
        val right = normalizeMotionText(second).split(' ').filter { it.isNotBlank() }.toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        if (left == right) return 1.0
        return (2.0 * left.intersect(right).size.toDouble()) / (left.size + right.size).toDouble()
    }

    private data class AppleArtistSearchMatch(
        val id: String,
        val name: String
    )

    private data class AppleSearchResult(
        val item: JSONObject,
        val attributes: JSONObject,
        val artist: String,
        val name: String,
        val album: String,
        val quickScore: Double
    )

    private data class AppleMotionResult(
        val video: AppleEditorialVideo,
        val albumName: String,
        val albumArtist: String,
        val upc: String,
        val releaseDate: String
    )

    private companion object {
        const val AMP_BASE_URL = "https://amp-api.music.apple.com"
        const val APPLE_BROWSE_URL = "https://music.apple.com/us/browse"
        const val TOKEN_EXPIRY_MARGIN_MS = 5L * 60L * 1000L
        const val MAX_ARTIST_DETAIL_CANDIDATES = 3
        const val MAX_TOKEN_SCRIPT_CANDIDATES = 12
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
        val JWT_REGEX = Regex("ey[a-zA-Z0-9_-]+\\.ey[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
        val BLACKLIST = setOf("playlist", "set list", "essentials", "dj mix", "apple music", "todays hits", "session")
    }
}

private class MotionProviderException(message: String, cause: Throwable? = null) : IOException(message, cause)

private fun JSONArray.optJSONObject(index: Int): JSONObject? = if (index in 0 until length()) opt(index) as? JSONObject else null

internal data class AppleEditorialVideo(
    val url: String,
    val width: Int?,
    val height: Int?
)

internal data class AppleArtistMotionMatch(
    val key: String,
    val name: String,
    val video: AppleEditorialVideo
)

private val APPLE_SCRIPT_SRC_REGEX = Regex(
    """(?i)<script\b[^>]*\bsrc\s*=\s*[\"']([^\"']+\.js(?:\?[^\"']*)?)[\"']"""
)
private val APPLE_LEGACY_SCRIPT_REGEX = Regex(
    "[\\\"']([^\\\"']*/assets/index[^\\\"']*\\.js)[\\\"']"
)

internal fun appleMusicScriptUrls(html: String): List<String> {
    val paths = LinkedHashSet<String>()
    APPLE_SCRIPT_SRC_REGEX.findAll(html).forEach { match -> paths += match.groupValues[1] }
    APPLE_LEGACY_SCRIPT_REGEX.findAll(html).forEach { match -> paths += match.groupValues[1] }
    return paths.mapNotNull(::trustedAppleMusicScriptUrl)
}

private fun trustedAppleMusicScriptUrl(value: String): String? {
    val clean = value.trim()
    if (clean.isBlank()) return null
    return when {
        clean.startsWith("https://music.apple.com/", ignoreCase = true) -> clean
        clean.startsWith("//music.apple.com/", ignoreCase = true) -> "https:$clean"
        clean.startsWith("/") -> "https://music.apple.com$clean"
        "://" !in clean -> "https://music.apple.com/$clean"
        else -> null
    }
}

internal fun appleMotionStorefronts(localStorefront: String): List<String> =
    listOf("us", localStorefront.trim().lowercase(Locale.ROOT))
        .filter { it.length == 2 }
        .distinct()

internal fun appleArtistMotionStorefronts(localStorefront: String): List<String> =
    appleMotionStorefronts(localStorefront)

internal fun selectAppleArtistEditorialVideo(attributes: JSONObject?): AppleEditorialVideo? {
    val root = attributes ?: return null
    return selectAppleEditorialVideo(root.optJSONObject("editorialVideo"))
        ?: selectAppleEditorialAssetVideo(
            root = root.optJSONObject("editorialArtwork"),
            urlFields = APPLE_EDITORIAL_ARTWORK_VIDEO_FIELDS
        )
}

private val APPLE_EDITORIAL_URL_FIELDS = listOf("video", "videoUrl", "hlsUrl", "url")
private val APPLE_EDITORIAL_ARTWORK_VIDEO_FIELDS = listOf("video", "videoUrl", "hlsUrl")

private val APPLE_EDITORIAL_KEY_ORDER = listOf(
    "motionDetailTall",
    "motionTallVideo3x4",
    "motionDetailSquare",
    "motionSquareVideo1x1",
    "motionDetailRaw",
    "motionDetailStatic"
)

internal fun selectAppleEditorialVideo(editorialVideo: JSONObject?): AppleEditorialVideo? =
    selectAppleEditorialAssetVideo(editorialVideo, APPLE_EDITORIAL_URL_FIELDS)

private fun selectAppleEditorialAssetVideo(
    root: JSONObject?,
    urlFields: List<String>
): AppleEditorialVideo? {
    root ?: return null
    val keys = buildList {
        APPLE_EDITORIAL_KEY_ORDER.forEach { if (root.has(it)) add(it) }
        root.keys().forEach { key -> if (key !in APPLE_EDITORIAL_KEY_ORDER) add(key) }
    }
    var best: AppleEditorialVideo? = null
    var bestRank = Int.MIN_VALUE
    var bestArea = -1L
    keys.take(APPLE_EDITORIAL_MAX_ASSETS).forEach { key ->
        val asset = root.optJSONObject(key) ?: return@forEach
        val url = urlFields.firstNotNullOfOrNull { field ->
            asset.optString(field).trim().takeIf { it.startsWith("https://") }
        } ?: return@forEach
        val frame = asset.optJSONObject("previewFrame")
        val width = frame?.optInt("width")?.takeIf { it > 0 }
        val height = frame?.optInt("height")?.takeIf { it > 0 }
        val orientationRank = when {
            width == null || height == null -> 1
            height > width -> 3
            height == width -> 2
            else -> 0
        }
        val area = if (width != null && height != null) width.toLong() * height.toLong() else 0L
        val better = orientationRank > bestRank ||
            (orientationRank == bestRank && area > bestArea) ||
            (orientationRank == bestRank && area == bestArea && best == null)
        if (better) {
            best = AppleEditorialVideo(url = url, width = width, height = height)
            bestRank = orientationRank
            bestArea = area
        }
    }
    return best
}

private const val APPLE_EDITORIAL_MAX_ASSETS = 12
