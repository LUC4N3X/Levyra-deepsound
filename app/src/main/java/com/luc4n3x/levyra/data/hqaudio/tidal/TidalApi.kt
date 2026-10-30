package com.luc4n3x.levyra.data.hqaudio.tidal

import com.luc4n3x.levyra.data.hqaudio.AudioCredentials
import com.luc4n3x.levyra.data.hqaudio.AudioCredentialsRuntime
import com.luc4n3x.levyra.data.hqaudio.AudioCredentialsSource
import com.luc4n3x.levyra.data.hqaudio.TidalCredentialSet
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject

internal class TidalApi(
    private val exchange: TidalExchange = TidalNetwork,
    private val clock: () -> Long = System::currentTimeMillis,
    private val credentialSource: AudioCredentialsSource = AudioCredentialsRuntime
) {
    private class Session(val access: String, val country: String, val expires: Long, val credentials: AudioCredentials)
    private val mutex = Mutex()
    private val sessions = mutableMapOf<Boolean, Session>()
    private var configuration: TidalCredentialSet? = null
    private var refreshToken: String? = null

    suspend fun get(path: String, parameters: Map<String, String> = emptyMap(), hiRes: Boolean = false): JSONObject =
        withContext(Dispatchers.IO) {
            var session = session(hiRes)
            var response = exchange.execute(request(path, parameters, session, hiRes), API_LIMIT)
            if (response.code in setOf(401, 403)) {
                mutex.withLock { if (sessions[hiRes]?.access == session.access) sessions.remove(hiRes) }
                credentialSource.refreshAfterRejection(session.credentials)
                session = session(hiRes)
                response = exchange.execute(request(path, parameters, session, hiRes), API_LIMIT)
            }
            parse(response)
        }

    suspend fun probe(url: HttpUrl, limit: Int): ProviderHttpResponse = exchange.execute(
        Request.Builder().url(url).header("Range", "bytes=0-${limit - 1}").build(), limit
    )

    private class TokenAttempt(val response: ProviderHttpResponse, val refreshToken: String)

    private suspend fun session(hiRes: Boolean): Session = mutex.withLock {
        var credentials = credentialSource.current()
        configure(credentials)
        sessions[hiRes]?.takeIf { clock() + 60_000L < it.expires }?.let { return@withLock it }
        val tried = mutableSetOf<List<String>>()
        var attempt = authenticate(credentials.tidal, hiRes, tried)
        if (attempt == null || isAuthRejection(attempt.response)) {
            credentials = credentialSource.refreshAfterRejection(credentials)
            configure(credentials)
            attempt = authenticate(credentials.tidal, hiRes, tried) ?: attempt
        }
        val accepted = attempt ?: throw TidalFailure(ProviderFailure.FORBIDDEN)
        if (isAuthRejection(accepted.response)) throw TidalFailure(ProviderFailure.FORBIDDEN)
        createSession(accepted, credentials, hiRes).also { sessions[hiRes] = it }
    }

    private fun configure(next: AudioCredentials) {
        if (configuration?.sameAs(next.tidal) == true) return
        sessions.clear()
        configuration = next.tidal
        refreshToken = next.tidal.refreshTokens.firstOrNull()
    }

    private suspend fun authenticate(
        config: TidalCredentialSet,
        hiRes: Boolean,
        tried: MutableSet<List<String>>
    ): TokenAttempt? {
        val tokens = (listOfNotNull(refreshToken) + config.refreshTokens).distinct()
        var last: TokenAttempt? = null
        for (token in tokens) {
            if (!tried.add(listOf(token, config.clientId(hiRes), if (hiRes) "" else config.tvClientSecret))) continue
            val response = refreshSessionToken(config, token, hiRes)
            last = TokenAttempt(response, token)
            if (!isAuthRejection(response)) return last
        }
        return last
    }

    private suspend fun refreshSessionToken(config: TidalCredentialSet, token: String, hiRes: Boolean): ProviderHttpResponse {
        val form = FormBody.Builder().add("client_id", config.clientId(hiRes))
            .add("refresh_token", token).add("grant_type", "refresh_token")
            .apply { if (!hiRes) add("client_secret", config.tvClientSecret) }.build()
        return exchange.execute(Request.Builder().url(TOKEN_URL).post(form).build(), TOKEN_LIMIT)
    }

    private suspend fun createSession(attempt: TokenAttempt, credentials: AudioCredentials, hiRes: Boolean): Session {
        val json = parse(attempt.response)
        val access = json.getString("access_token").takeIf { it.isNotBlank() } ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        if (!json.getString("token_type").equals("bearer", true)) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val lifetime = json.getLong("expires_in")
        if (lifetime !in 1L..86_400L) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val country = resolveCountry(json, access, credentials.tidal, hiRes)
        val nextRefresh = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: attempt.refreshToken
        if (attempt.refreshToken != refreshToken) sessions.clear()
        refreshToken = nextRefresh
        return Session(access, country, clock() + lifetime * 1_000L, credentials)
    }

    private suspend fun resolveCountry(json: JSONObject, access: String, config: TidalCredentialSet, hiRes: Boolean): String {
        var country = json.optJSONObject("user")?.optString("countryCode").orEmpty()
        if (country.length != 2) {
            val response = exchange.execute(Request.Builder().url("$BASE/sessions")
                .header("Authorization", "Bearer $access").header("X-Tidal-Token", config.clientId(hiRes)).build(), TOKEN_LIMIT)
            country = parse(response).getString("countryCode")
        }
        if (country.length != 2 || !country.all { it in 'A'..'Z' }) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        return country
    }

    private fun request(path: String, parameters: Map<String, String>, session: Session, hiRes: Boolean): Request {
        require(!path.startsWith('/') && !path.contains("..") && !path.contains('?'))
        val url = "$BASE/$path".toHttpUrl().newBuilder().addQueryParameter("countryCode", session.country)
        parameters.forEach { (key, value) -> url.addQueryParameter(key, value) }
        return Request.Builder().url(url.build()).header("Authorization", "Bearer ${session.access}")
            .header("X-Tidal-Token", session.credentials.tidal.clientId(hiRes)).build()
    }

    private fun parse(response: ProviderHttpResponse): JSONObject {
        if (!response.isSuccessful) throw TidalFailure(when (response.code) {
            401, 403 -> ProviderFailure.FORBIDDEN
            404 -> ProviderFailure.NOT_FOUND
            429 -> ProviderFailure.RATE_LIMITED
            else -> ProviderFailure.HTTP_ERROR
        })
        return JSONObject(response.bodyText)
    }

    private fun isAuthRejection(response: ProviderHttpResponse): Boolean = response.code in setOf(401, 403) ||
        (response.code == 400 && runCatching { JSONObject(response.bodyText).optString("error") }.getOrNull() == "invalid_grant")

    companion object {
        private const val BASE = "https://api.tidal.com/v1"
        private const val TOKEN_URL = "https://auth.tidal.com/v1/oauth2/token"
        private const val TOKEN_LIMIT = 65_536
        private const val API_LIMIT = 1_000_000
    }
}

internal class TidalFailure(val failure: ProviderFailure) : IOException(failure.name)

internal fun interface TidalExchange {
    suspend fun execute(request: Request, limit: Int): ProviderHttpResponse
}

internal object TidalNetwork : TidalExchange {
    private val client: OkHttpClient by lazy {
        val base = LevyraHttpClientFactory.externalIntegrations()
        base.newBuilder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .dns(Dns { host -> base.dns.lookup(host).also { addresses ->
                if (addresses.isEmpty() || addresses.any { !SafeImageUrlPolicy.isPublicAddress(it) }) throw IOException("Blocked TIDAL destination")
            } })
            .addNetworkInterceptor { chain ->
                val address = chain.connection()?.socket()?.inetAddress
                if (!allows(chain.request().url) || address == null || !SafeImageUrlPolicy.isPublicAddress(address)) throw IOException("Blocked TIDAL connection")
                chain.proceed(chain.request())
            }.build()
    }

    fun allows(url: HttpUrl): Boolean = url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
        (url.host.endsWith(".tidal.com") || url.host.endsWith(".tidalhifi.com"))

    override suspend fun execute(request: Request, limit: Int): ProviderHttpResponse = suspendCancellableCoroutine { continuation ->
        if (!allows(request.url) || limit !in 1..1_000_000) {
            continuation.resumeWithException(IOException("Blocked TIDAL request"))
            return@suspendCancellableCoroutine
        }
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching { response.use {
                    val buffer = Buffer()
                    val source = it.body.source()
                    var remaining = limit.toLong()
                    while (remaining > 0) {
                        val read = source.read(buffer, remaining)
                        if (read == -1L) break
                        remaining -= read
                    }
                    ProviderHttpResponse(it.code, it.headers.names().associateWith { name -> it.header(name).orEmpty() }, buffer.readByteArray())
                } }
                if (continuation.isActive) result.fold({ continuation.resume(it) }, { continuation.resumeWithException(it) })
            }
        })
    }
}
