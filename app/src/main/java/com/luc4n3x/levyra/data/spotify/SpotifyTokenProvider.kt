package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class SpotifyTokenProvider private constructor(
    private val clientFactory: () -> OkHttpClient = { LevyraHttpClientFactory.externalIntegrations() }
) {
    private val tokenMutex = Mutex()

    @Volatile
    private var accessToken: AccessToken? = null

    @Volatile
    private var totpMaterial: TotpMaterial? = null

    suspend fun token(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        accessToken
            ?.takeIf { now + TOKEN_EXPIRY_SKEW_MS < it.expiresAt }
            ?.value
            ?.let { return@withContext it }

        tokenMutex.withLock {
            val currentNow = System.currentTimeMillis()
            accessToken
                ?.takeIf { currentNow + TOKEN_EXPIRY_SKEW_MS < it.expiresAt }
                ?.value
                ?.let { return@withLock it }

            val material = totpMaterial ?: fetchTotpMaterial().also { totpMaterial = it }
            val client = clientFactory()
            try {
                val serverTime = fetchServerTimeSeconds(client)
                val totp = generateTotp(material.secret, serverTime)
                val url = TOKEN_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("reason", "transport")
                    .addQueryParameter("productType", "web-player")
                    .addQueryParameter("totp", totp)
                    .addQueryParameter("totpServer", totp)
                    .addQueryParameter("totpVer", material.version.toString())
                    .build()
                val response = executeJson(
                    client,
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
        val client = clientFactory()
        val root = runCatching {
            executeJson(
                client,
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
                append(value xor (index % 33 + 9))
            }
        }.toByteArray(StandardCharsets.US_ASCII)
        return TotpMaterial(version, decoded)
    }

    private fun fetchServerTimeSeconds(client: OkHttpClient): Long {
        val response = executeJson(
            client,
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

    private fun executeJson(client: OkHttpClient, request: Request): JSONObject {
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Spotify request failed with HTTP ${response.code}")
            val body = response.body.string()
            if (body.length !in 1..MAX_RESPONSE_CHARS) error("Spotify response is invalid")
            JSONObject(body)
        }
    }

    private data class AccessToken(val value: String, val expiresAt: Long)
    private data class TotpMaterial(val version: Int, val secret: ByteArray)

    companion object {
        @Volatile
        private var instance: SpotifyTokenProvider? = null

        fun get(): SpotifyTokenProvider {
            return instance ?: synchronized(this) {
                instance ?: SpotifyTokenProvider().also { instance = it }
            }
        }

        private const val SECRET_DICTIONARY_URL =
            "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/main/secrets/secretDict.json"
        private const val BUNDLED_SECRET_DICTIONARY =
            "{\"59\":[123,105,79,70,110,59,52,125,60,49,80,70,89,75,80,86,63,53,123,37,117,49,52,93,77,62,47,86,48,104,68,72],\"60\":[79,109,69,123,90,65,46,74,94,34,58,48,70,71,92,85,122,63,91,64,87,87],\"61\":[44,55,47,42,70,40,34,114,76,74,50,111,120,97,75,76,94,102,43,69,49,120,118,80,64,78]}"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val TOKEN_URL = "https://open.spotify.com/api/token"
        const val WEB_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36"
        private const val MAX_RESPONSE_CHARS = 1_000_000
        private const val TOKEN_EXPIRY_SKEW_MS = 60_000L
        private const val DEFAULT_TOKEN_TTL_MS = 15L * 60L * 1000L
    }
}
