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
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .callTimeout(9, TimeUnit.SECONDS)
        .build()
    private val tokenMutex = Mutex()
    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        return try {
            val token = developerToken()
            val storefront = Locale.getDefault().country.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"
            val songCandidates = search(identity, storefront, token, "songs")
            if (songCandidates.isNotEmpty()) {
                MotionArtworkProviderResult.Found(songCandidates)
            } else {
                val albumCandidates = search(identity, storefront, token, "albums")
                if (albumCandidates.isNotEmpty()) {
                    MotionArtworkProviderResult.Found(albumCandidates)
                } else {
                    MotionArtworkProviderResult.NoMatch
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Apple motion provider failed")
            MotionArtworkProviderResult.Failed(error)
        }
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
            ?: return emptyList()

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

        val output = ArrayList<MotionArtworkCandidate>(ranked.size)
        for (result in ranked) {
            val directUrl = extractEditorialVideoUrl(result.attributes.optJSONObject("editorialVideo"))
            val albumId = resolveAlbumId(result.item, result.attributes, type)
            val resolved = if (!directUrl.isNullOrBlank()) {
                AppleMotionResult(
                    url = directUrl,
                    albumName = result.album,
                    albumArtist = result.artist,
                    upc = result.attributes.optString("upc"),
                    releaseDate = result.attributes.optString("releaseDate")
                )
            } else if (!albumId.isNullOrBlank() && !albumId.startsWith("pl.")) {
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
                url = resolved.url,
                mimeType = "application/x-mpegURL",
                width = 1280,
                height = 1280,
                expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
            )
        }
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
        val motionUrl = extractEditorialVideoUrl(attributes.optJSONObject("editorialVideo")) ?: return null
        return AppleMotionResult(
            url = motionUrl,
            albumName = albumName,
            albumArtist = attributes.optString("artistName").trim(),
            upc = attributes.optString("upc").trim(),
            releaseDate = attributes.optString("releaseDate").trim()
        )
    }

    private suspend fun developerToken(): String = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { tokenExpiresAt > now + TOKEN_EXPIRY_MARGIN_MS }?.let { return@withLock it }
        val html = requestText(APPLE_BROWSE_URL)
        val scripts = SCRIPT_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .map { path -> if (path.startsWith("http")) path else "https://music.apple.com${if (path.startsWith('/')) path else "/$path"}" }
            .distinct()
            .take(5)
            .toList()
        var lastFailure: Throwable? = null
        for (script in scripts) {
            val source = try {
                requestText(script)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastFailure = error
                continue
            }
            for (match in JWT_REGEX.findAll(source)) {
                val token = match.value
                val expiration = jwtExpiration(token) ?: continue
                if (expiration > now + TOKEN_EXPIRY_MARGIN_MS) {
                    cachedToken = token
                    tokenExpiresAt = expiration
                    return@withLock token
                }
            }
        }
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
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
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

    private fun extractEditorialVideoUrl(editorialVideo: JSONObject?): String? {
        val keys = listOf("motionDetailSquare", "motionDetailRaw", "motionDetailTall", "motionDetailStatic")
        for (key in keys) {
            val asset = editorialVideo?.optJSONObject(key) ?: continue
            val url = listOf("video", "videoUrl", "hlsUrl", "url")
                .firstNotNullOfOrNull { field -> asset.optString(field).takeIf { it.startsWith("https://") } }
            if (url != null) return url
        }
        return null
    }

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

    private fun artistMatches(requested: List<String>, returned: List<String>): Boolean {
        if (requested.isEmpty() || returned.isEmpty()) return false
        val normalizedReturned = artistAliases(returned)
        return combinedArtistSignature(requested) == combinedArtistSignature(returned) ||
            normalizeMotionText(requested.first()) in normalizedReturned
    }

    private fun similarity(first: String, second: String): Double {
        val left = normalizeMotionText(first).split(' ').filter { it.isNotBlank() }.toSet()
        val right = normalizeMotionText(second).split(' ').filter { it.isNotBlank() }.toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        if (left == right) return 1.0
        return (2.0 * left.intersect(right).size.toDouble()) / (left.size + right.size).toDouble()
    }

    private data class AppleSearchResult(
        val item: JSONObject,
        val attributes: JSONObject,
        val artist: String,
        val name: String,
        val album: String,
        val quickScore: Double
    )

    private data class AppleMotionResult(
        val url: String,
        val albumName: String,
        val albumArtist: String,
        val upc: String,
        val releaseDate: String
    )

    private companion object {
        const val AMP_BASE_URL = "https://amp-api.music.apple.com"
        const val APPLE_BROWSE_URL = "https://music.apple.com/us/browse"
        const val TOKEN_EXPIRY_MARGIN_MS = 5L * 60L * 1000L
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
        val SCRIPT_REGEX = Regex("[\\\"']([^\\\"']*/assets/index[^\\\"']*\\.js)[\\\"']")
        val JWT_REGEX = Regex("ey[a-zA-Z0-9_-]+\\.ey[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
        val BLACKLIST = setOf("playlist", "set list", "essentials", "dj mix", "apple music", "todays hits", "session")
    }
}

private class MotionProviderException(message: String, cause: Throwable? = null) : IOException(message, cause)

private fun JSONArray.optJSONObject(index: Int): JSONObject? = if (index in 0 until length()) opt(index) as? JSONObject else null
