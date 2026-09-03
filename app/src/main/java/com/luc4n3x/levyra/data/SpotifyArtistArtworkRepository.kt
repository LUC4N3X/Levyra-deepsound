package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.BitmapFactory
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistIdentityMatches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal const val FLAT_ARTWORK_CHANNEL_SPREAD = 12

internal fun isFlatArtworkSample(pixels: IntArray): Boolean {
    if (pixels.isEmpty()) return false
    var minRed = 255
    var maxRed = 0
    var minGreen = 255
    var maxGreen = 0
    var minBlue = 255
    var maxBlue = 0
    pixels.forEach { pixel ->
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        if (red < minRed) minRed = red
        if (red > maxRed) maxRed = red
        if (green < minGreen) minGreen = green
        if (green > maxGreen) maxGreen = green
        if (blue < minBlue) minBlue = blue
        if (blue > maxBlue) maxBlue = blue
    }
    return (maxRed - minRed) <= FLAT_ARTWORK_CHANNEL_SPREAD &&
        (maxGreen - minGreen) <= FLAT_ARTWORK_CHANNEL_SPREAD &&
        (maxBlue - minBlue) <= FLAT_ARTWORK_CHANNEL_SPREAD
}

internal fun isAllowedSpotifyArtistArtworkUrl(rawUrl: String): Boolean {
    val url = rawUrl.toHttpUrlOrNull() ?: return false
    if (!url.isHttps) return false
    val host = url.host.lowercase()
    val spotifyHost = host == "i.scdn.co" ||
        host.endsWith(".scdn.co") ||
        host == "image-cdn-ak.spotifycdn.com" ||
        host.endsWith(".spotifycdn.com")
    if (!spotifyHost) return false
    if (host == "i.scdn.co" && !url.encodedPath.contains("ab676161", ignoreCase = true)) return false
    return true
}

internal class SpotifyArtistArtworkRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val client = LevyraHttpClientFactory.externalIntegrations()
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val memoryCache = ConcurrentHashMap<String, CachedArtwork>()
    private val tokenMutex = Mutex()

    @Volatile
    private var accessToken: AccessToken? = null

    @Volatile
    private var totpMaterial: TotpMaterial? = null

    suspend fun resolveArtistPortrait(artistName: String): String = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        val identity = artistIdentityKey(cleanName)
        if (identity.isBlank()) return@withContext ""

        val now = System.currentTimeMillis()
        memoryCache[identity]
            ?.takeIf { now - it.savedAt in 0 until ARTWORK_TTL_MS }
            ?.url
            ?.takeIf(::isAllowedSpotifyArtistArtworkUrl)
            ?.let { return@withContext it }

        readPersisted(identity, now)?.let { cached ->
            memoryCache[identity] = cached
            return@withContext cached.url
        }

        if (isPersistedFlat(identity, now)) return@withContext ""

        val resolved = runCatching {
            val bearer = anonymousAccessToken(now)
            searchArtistPortrait(cleanName, bearer)
        }.getOrNull().orEmpty()

        if (resolved.isBlank()) return@withContext ""

        if (isFlatPortrait(resolved) == true) {
            persistFlat(identity, now)
            return@withContext ""
        }

        val cached = CachedArtwork(resolved, now)
        memoryCache[identity] = cached
        persist(identity, cached)
        resolved
    }

    private fun isFlatPortrait(url: String): Boolean? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEB_USER_AGENT)
            .header("Accept", "image/*")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body
                if (body.contentLength() > MAX_PORTRAIT_PROBE_BYTES) return null
                val source = body.source()
                source.request(MAX_PORTRAIT_PROBE_BYTES + 1L)
                if (source.buffer.size > MAX_PORTRAIT_PROBE_BYTES) return null
                val bytes = source.buffer.readByteArray()
                if (bytes.isEmpty()) return null
                val options = BitmapFactory.Options().apply { inSampleSize = PORTRAIT_PROBE_SAMPLE_SIZE }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    if (pixels.isEmpty()) return null
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    isFlatArtworkSample(pixels)
                } finally {
                    bitmap.recycle()
                }
            }
        }.getOrNull()
    }

    private fun isPersistedFlat(identity: String, now: Long): Boolean {
        if (!preferences.getBoolean("$identity.flat", false)) return false
        val savedAt = preferences.getLong("$identity.flatSavedAt", 0L)
        return now - savedAt in 0 until ARTWORK_TTL_MS
    }

    private fun persistFlat(identity: String, now: Long) {
        memoryCache.remove(identity)
        preferences.edit()
            .putBoolean("$identity.flat", true)
            .putLong("$identity.flatSavedAt", now)
            .remove("$identity.url")
            .remove("$identity.savedAt")
            .apply()
    }

    private fun readPersisted(identity: String, now: Long): CachedArtwork? {
        val url = preferences.getString("$identity.url", null).orEmpty()
        val savedAt = preferences.getLong("$identity.savedAt", 0L)
        if (url.isBlank() || now - savedAt !in 0 until ARTWORK_TTL_MS) return null
        if (!isAllowedSpotifyArtistArtworkUrl(url)) return null
        return CachedArtwork(url, savedAt)
    }

    private fun persist(identity: String, cached: CachedArtwork) {
        preferences.edit()
            .putString("$identity.url", cached.url)
            .putLong("$identity.savedAt", cached.savedAt)
            .apply()
    }

    private suspend fun anonymousAccessToken(now: Long): String {
        accessToken
            ?.takeIf { now + TOKEN_EXPIRY_SKEW_MS < it.expiresAt }
            ?.value
            ?.let { return it }

        return tokenMutex.withLock {
            val currentNow = System.currentTimeMillis()
            accessToken
                ?.takeIf { currentNow + TOKEN_EXPIRY_SKEW_MS < it.expiresAt }
                ?.value
                ?.let { return@withLock it }

            val material = totpMaterial ?: fetchTotpMaterial().also { totpMaterial = it }
            try {
                val serverTime = fetchServerTimeSeconds()
                val totp = generateTotp(material.secret, serverTime)
                val url = TOKEN_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("reason", "transport")
                    .addQueryParameter("productType", "web-player")
                    .addQueryParameter("totp", totp)
                    .addQueryParameter("totpServer", totp)
                    .addQueryParameter("totpVer", material.version.toString())
                    .build()
                val response = executeJson(
                    Request.Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .header("User-Agent", WEB_USER_AGENT)
                        .build()
                )
                val issuedAccessToken = response.optString("accessToken").trim()
                if (issuedAccessToken.isBlank()) error("Spotify returned no anonymous access token")
                val expiresAt = response.optLong("accessTokenExpirationTimestampMs", 0L)
                    .takeIf { it > currentNow }
                    ?: (currentNow + DEFAULT_TOKEN_TTL_MS)
                accessToken = AccessToken(issuedAccessToken, expiresAt)
                issuedAccessToken
            } catch (error: Throwable) {
                totpMaterial = null
                accessToken = null
                throw error
            }
        }
    }

    private fun fetchTotpMaterial(): TotpMaterial {
        val root = runCatching {
            executeJson(
                Request.Builder()
                    .url(SECRET_DICTIONARY_URL)
                    .header("Accept", "application/json")
                    .header("User-Agent", WEB_USER_AGENT)
                    .build()
            )
        }.getOrElse {
            JSONObject(BUNDLED_SECRET_DICTIONARY)
        }
        val version = root.keys().asSequence()
            .mapNotNull(String::toIntOrNull)
            .maxOrNull()
            ?: error("Spotify TOTP dictionary is empty")
        val cipher = root.optJSONArray(version.toString()) ?: error("Spotify TOTP material is missing")
        if (cipher.length() !in 1..128) error("Spotify TOTP material is invalid")
        val decoded = buildString {
            for (index in 0 until cipher.length()) {
                val value = cipher.optInt(index, -1)
                if (value !in 0..255) error("Spotify TOTP material is invalid")
                append(value xor ((index % 33) + 9))
            }
        }.toByteArray(StandardCharsets.US_ASCII)
        return TotpMaterial(version, decoded)
    }

    private fun fetchServerTimeSeconds(): Long {
        val response = executeJson(
            Request.Builder()
                .url(SERVER_TIME_URL)
                .header("Accept", "application/json")
                .header("User-Agent", WEB_USER_AGENT)
                .build()
        )
        return response.optLong("serverTime", 0L).takeIf { it > 0L }
            ?: error("Spotify returned no server time")
    }

    private fun generateTotp(secret: ByteArray, serverTimeSeconds: Long): String {
        val counter = serverTimeSeconds / 30L
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret, "HmacSHA1"))
        val digest = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = digest.last().toInt() and 0x0F
        val binary = ((digest[offset].toInt() and 0x7F) shl 24) or
            ((digest[offset + 1].toInt() and 0xFF) shl 16) or
            ((digest[offset + 2].toInt() and 0xFF) shl 8) or
            (digest[offset + 3].toInt() and 0xFF)
        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private fun searchArtistPortrait(artistName: String, bearer: String): String {
        val variables = JSONObject()
            .put("searchTerm", artistName)
            .put("offset", 0)
            .put("limit", SEARCH_LIMIT)
            .put("numberOfTopResults", 5)
            .put("includeAudiobooks", false)
            .put("includeArtistHasConcertsField", false)
            .put("includePreReleases", false)
            .put("includeLocalConcertsField", false)
            .put("includeAuthors", false)
        val payload = JSONObject()
            .put("variables", variables)
            .put("operationName", SEARCH_OPERATION)
            .put(
                "extensions",
                JSONObject().put(
                    "persistedQuery",
                    JSONObject()
                        .put("version", 1)
                        .put("sha256Hash", SEARCH_QUERY_HASH)
                )
            )
        val response = executeJson(
            Request.Builder()
                .url(GRAPHQL_URL)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $bearer")
                .header("User-Agent", WEB_USER_AGENT)
                .header("app-platform", "WebPlayer")
                .header("Origin", "https://open.spotify.com")
                .header("Referer", "https://open.spotify.com/")
                .header("Accept", "application/json")
                .build()
        )
        val artists = response
            .optJSONObject("data")
            ?.optJSONObject("searchV2")
            ?.optJSONObject("artists")
            ?.optJSONArray("items")
            ?: return ""
        return selectMatchingArtistPortrait(artistName, artists)
    }

    private fun selectMatchingArtistPortrait(artistName: String, items: JSONArray): String {
        val expectedIdentity = artistIdentityKey(artistName)
        for (index in 0 until items.length()) {
            val wrapper = items.optJSONObject(index) ?: continue
            if (!wrapper.optString("__typename").equals("ArtistResponseWrapper", ignoreCase = true)) continue
            val data = wrapper.optJSONObject("data") ?: continue
            if (!data.optString("__typename").equals("Artist", ignoreCase = true)) continue
            val resolvedName = data.optJSONObject("profile")?.optString("name").orEmpty().trim()
            val exactIdentity = artistIdentityKey(resolvedName) == expectedIdentity
            if (!exactIdentity && !artistIdentityMatches(resolvedName, artistName)) continue
            val sources = data
                .optJSONObject("visuals")
                ?.optJSONObject("avatarImage")
                ?.optJSONArray("sources")
                ?: continue
            bestArtistArtwork(sources)?.let { return it }
        }
        return ""
    }

    private fun bestArtistArtwork(sources: JSONArray): String? {
        var bestUrl: String? = null
        var bestArea = -1L
        for (index in 0 until sources.length()) {
            val source = sources.optJSONObject(index) ?: continue
            val url = source.optString("url").trim()
            if (!isAllowedSpotifyArtistArtworkUrl(url)) continue
            val width = source.optLong("width", 0L).coerceAtLeast(0L)
            val height = source.optLong("height", 0L).coerceAtLeast(0L)
            val area = width * height
            if (area >= bestArea) {
                bestArea = area
                bestUrl = url
            }
        }
        return bestUrl
    }

    private fun executeJson(request: Request): JSONObject {
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Spotify request failed with HTTP ${response.code}")
            val body = response.body.string()
            if (body.length !in 1..MAX_RESPONSE_CHARS) error("Spotify response is invalid")
            JSONObject(body)
        }
    }

    private data class AccessToken(val value: String, val expiresAt: Long)
    private data class TotpMaterial(val version: Int, val secret: ByteArray)
    private data class CachedArtwork(val url: String, val savedAt: Long)

    companion object {
        @Volatile
        private var instance: SpotifyArtistArtworkRepository? = null

        fun get(context: Context): SpotifyArtistArtworkRepository {
            return instance ?: synchronized(this) {
                instance ?: SpotifyArtistArtworkRepository(context.applicationContext).also { instance = it }
            }
        }

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val PREFERENCES_NAME = "spotify_artist_artwork"
        private const val SECRET_DICTIONARY_URL =
            "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/main/secrets/secretDict.json"
        private const val BUNDLED_SECRET_DICTIONARY =
            "{\"59\":[123,105,79,70,110,59,52,125,60,49,80,70,89,75,80,86,63,53,123,37,117,49,52,93,77,62,47,86,48,104,68,72],\"60\":[79,109,69,123,90,65,46,74,94,34,58,48,70,71,92,85,122,63,91,64,87,87],\"61\":[44,55,47,42,70,40,34,114,76,74,50,111,120,97,75,76,94,102,43,69,49,120,118,80,64,78]}"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val TOKEN_URL = "https://open.spotify.com/api/token"
        private const val GRAPHQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        private const val SEARCH_OPERATION = "searchDesktop"
        private const val SEARCH_QUERY_HASH =
            "4801118d4a100f756e833d33984436a3899cff359c532f8fd3aaf174b60b3b49"
        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36"
        private const val SEARCH_LIMIT = 10
        private const val MAX_RESPONSE_CHARS = 1_000_000
        private const val MAX_PORTRAIT_PROBE_BYTES = 2_000_000
        private const val PORTRAIT_PROBE_SAMPLE_SIZE = 16
        private const val TOKEN_EXPIRY_SKEW_MS = 60_000L
        private const val DEFAULT_TOKEN_TTL_MS = 15L * 60L * 1000L
        private const val ARTWORK_TTL_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
