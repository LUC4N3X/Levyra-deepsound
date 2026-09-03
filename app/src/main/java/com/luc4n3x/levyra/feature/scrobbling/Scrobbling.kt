package com.luc4n3x.levyra.feature.scrobbling

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore
import com.luc4n3x.levyra.domain.Track
import java.math.BigInteger
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

data class ScrobbleListen(val track: Track, val startedAtMs: Long, val listenedMs: Long)

interface ScrobbleProvider {
    val id: String
    fun isConfigured(): Boolean
    suspend fun nowPlaying(listen: ScrobbleListen)
    suspend fun scrobble(listen: ScrobbleListen)
    fun clear()
}

class LastFmScrobbleProvider(private val credentials: AndroidKeystoreCredentialStore) : ScrobbleProvider {
    override val id = "lastfm"

    override fun isConfigured(): Boolean = credentials.read(LAST_FM_API_SLOT) != null &&
        credentials.read(LAST_FM_SECRET_SLOT) != null && credentials.read(KEY_SESSION) != null

    fun saveApiCredentials(apiKey: String, sharedSecret: String) {
        credentials.write(LAST_FM_API_SLOT, apiKey.trim())
        credentials.write(LAST_FM_SECRET_SLOT, sharedSecret.trim())
        credentials.clear(KEY_SESSION)
    }

    fun authorizationUrl(token: String): String? = credentials.read(LAST_FM_API_SLOT)?.let { apiCredential ->
        "https://www.last.fm/api/auth/".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiCredential)
            .addQueryParameter("token", token)
            .build()
            .toString()
    }

    suspend fun requestToken(): String? = call("auth.getToken", emptyMap())?.optString("token")?.takeIf(String::isNotBlank)

    suspend fun completeAuthorization(token: String): Boolean {
        val session = call("auth.getSession", mapOf("token" to token))
            ?.optJSONObject("session")?.optString("key").orEmpty()
        if (session.isBlank()) return false
        credentials.write(KEY_SESSION, session)
        return true
    }

    override suspend fun nowPlaying(listen: ScrobbleListen) {
        submit("track.updateNowPlaying", listen, emptyMap())
    }

    override suspend fun scrobble(listen: ScrobbleListen) {
        submit("track.scrobble", listen, mapOf("timestamp" to (listen.startedAtMs / 1000L).toString()))
    }

    override fun clear() = credentials.clear(LAST_FM_API_SLOT, LAST_FM_SECRET_SLOT, KEY_SESSION)

    private suspend fun submit(method: String, listen: ScrobbleListen, extra: Map<String, String>) {
        if (!isConfigured()) return
        call(method, trackParameters(listen) + extra)
    }

    private suspend fun call(method: String, parameters: Map<String, String>): JSONObject? =
        withContext(Dispatchers.IO) { callOnIo(method, parameters) }

    private suspend fun callOnIo(method: String, parameters: Map<String, String>): JSONObject? {
        val apiCredential = credentials.read(LAST_FM_API_SLOT) ?: return null
        val signingCredential = credentials.read(LAST_FM_SECRET_SLOT) ?: return null
        val session = credentials.read(KEY_SESSION)
        val sessionParameters = if (method == "auth.getToken" || method == "auth.getSession" || session == null) {
            emptyMap()
        } else {
            mapOf("sk" to session.orEmpty())
        }
        val signed = (parameters + mapOf("method" to method, "api_key" to apiCredential) + sessionParameters).toSortedMap()
        val signature = md5(signed.entries.joinToString("") { it.key + it.value } + signingCredential)
        val body = FormBody.Builder().apply {
            signed.forEach { (key, value) -> add(key, value) }
            add("api_sig", signature)
            add("format", "json")
        }.build()
        val request = Request.Builder().url(LAST_FM_URL).post(body).build()
        try {
            repeat(2) { attempt ->
                LevyraHttpClientFactory.externalIntegrations().newCall(request).execute().use { response ->
                    val root = JSONObject(response.body.jsonPayload() ?: return null)
                    val errorCode = root.optInt("error", 0)
                    if (errorCode == 9) {
                        credentials.clear(KEY_SESSION)
                        return null
                    }
                    if (response.isSuccessful && errorCode == 0) return root
                    if (attempt == 1 || errorCode !in TRANSIENT_CODES) return null
                }
                delay(400L)
            }
            return null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            return null
        }
    }

    private fun trackParameters(listen: ScrobbleListen): Map<String, String> = buildMap {
        put("track", listen.track.title)
        put("artist", listen.track.artist)
        listen.track.album.takeIf(String::isNotBlank)?.let { put("album", it) }
        listen.track.durationMs.takeIf { it > 0 }?.let { put("duration", (it / 1000L).toString()) }
    }

    private fun md5(value: String): String = BigInteger(1, MessageDigest.getInstance("MD5").digest(value.toByteArray())).toString(16).padStart(32, '0')

    private companion object {
        const val LAST_FM_URL = "https://ws.audioscrobbler.com/2.0/"
        const val LAST_FM_API_SLOT = "lastfm_api_key"
        const val LAST_FM_SECRET_SLOT = "lastfm_shared_secret"
        const val KEY_SESSION = "lastfm_session"
        val TRANSIENT_CODES = setOf(11, 16, 29)
    }
}

class ListenBrainzScrobbleProvider(private val credentials: AndroidKeystoreCredentialStore) : ScrobbleProvider {
    override val id = "listenbrainz"

    override fun isConfigured(): Boolean = credentials.read(LISTEN_BRAINZ_CREDENTIAL_SLOT) != null

    suspend fun saveToken(token: String): Boolean = withContext(Dispatchers.IO) {
        val clean = token.trim()
        if (clean.isBlank()) return@withContext false
        val request = Request.Builder().url("https://api.listenbrainz.org/1/validate-token")
            .header("Authorization", "Token $clean").build()
        try {
            LevyraHttpClientFactory.externalIntegrations().newCall(request).execute().use { response ->
                val payload = if (response.isSuccessful) response.body.jsonPayload() else null
                val valid = payload != null && JSONObject(payload).optBoolean("valid")
                if (valid) credentials.write(LISTEN_BRAINZ_CREDENTIAL_SLOT, clean)
                valid
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun nowPlaying(listen: ScrobbleListen) = submit("playing_now", listen)

    override suspend fun scrobble(listen: ScrobbleListen) = submit("single", listen)

    override fun clear() = credentials.clear(LISTEN_BRAINZ_CREDENTIAL_SLOT)

    private suspend fun submit(type: String, listen: ScrobbleListen): Unit = withContext(Dispatchers.IO) {
        val credentialValue = credentials.read(LISTEN_BRAINZ_CREDENTIAL_SLOT) ?: return@withContext
        val payload = listenBrainzPayload(type, listen)
        val request = Request.Builder().url("https://api.listenbrainz.org/1/submit-listens")
            .header("Authorization", "Token $credentialValue")
            .post(payload.toString().toRequestBody(JSON)).build()
        try {
            LevyraHttpClientFactory.externalIntegrations().newCall(request).execute().close()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val LISTEN_BRAINZ_CREDENTIAL_SLOT = "listenbrainz_token"
    }
}

class ScrobblingCoordinator(private val providers: List<ScrobbleProvider>) {
    private val submitted = LinkedHashSet<String>()

    suspend fun nowPlaying(track: Track, startedAtMs: Long) {
        val listen = ScrobbleListen(track, startedAtMs, 0L)
        configuredProviders().forEach { provider ->
            isolate(provider) { provider.nowPlaying(listen) }
        }
    }

    suspend fun scrobble(track: Track, startedAtMs: Long, listenedMs: Long) {
        val threshold = scrobbleThresholdMs(track.durationMs) ?: return
        if (listenedMs < threshold) return
        configuredProviders().forEach { provider ->
            val key = "${provider.id}:${track.id}:${startedAtMs}"
            if (!markSubmitted(key)) return@forEach
            val delivered = isolate(provider) {
                provider.scrobble(ScrobbleListen(track, startedAtMs, listenedMs))
            }
            if (!delivered) releaseSubmitted(key)
        }
    }

    private fun configuredProviders(): List<ScrobbleProvider> = providers.filter { provider ->
        try {
            provider.isConfigured()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Scrobble provider %s configuration check failed", provider.id)
            false
        }
    }

    private suspend fun isolate(provider: ScrobbleProvider, block: suspend () -> Unit): Boolean {
        return try {
            block()
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Scrobble provider %s failed", provider.id)
            false
        }
    }

    private fun releaseSubmitted(key: String) {
        synchronized(submitted) { submitted.remove(key) }
    }

    private fun markSubmitted(key: String): Boolean = synchronized(submitted) {
        if (!submitted.add(key)) return@synchronized false
        while (submitted.size > MAX_SUBMITTED_LISTENS) {
            val oldest = submitted.iterator()
            if (oldest.hasNext()) {
                oldest.next()
                oldest.remove()
            }
        }
        true
    }

    private companion object {
        const val MAX_SUBMITTED_LISTENS = 512
    }
}

internal const val MAX_SCROBBLE_RESPONSE_BYTES = 64L * 1024L

internal fun ResponseBody.jsonPayload(): String? {
    if (!contentType()?.subtype.orEmpty().endsWith("json", ignoreCase = true)) return null
    if (contentLength() > MAX_SCROBBLE_RESPONSE_BYTES) return null
    val payload = source()
    payload.request(MAX_SCROBBLE_RESPONSE_BYTES + 1L)
    if (payload.buffer.size > MAX_SCROBBLE_RESPONSE_BYTES) return null
    return payload.buffer.readUtf8()
}

internal fun scrobbleThresholdMs(durationMs: Long): Long? = durationMs.takeIf { it >= 30_000L }?.let { minOf(it / 2L, 240_000L) }

internal fun listenBrainzPayload(type: String, listen: ScrobbleListen): JSONObject = JSONObject().put("listen_type", type).put("payload", JSONArray().put(JSONObject().apply {
    if (type != "playing_now") put("listened_at", listen.startedAtMs / 1000L)
    put("track_metadata", JSONObject().put("track_name", listen.track.title).put("artist_name", listen.track.artist).apply {
        listen.track.album.takeIf(String::isNotBlank)?.let { put("release_name", it) }
    })
}))
