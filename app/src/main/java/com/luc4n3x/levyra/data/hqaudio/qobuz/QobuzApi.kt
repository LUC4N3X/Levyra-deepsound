package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.AudioCredentials
import com.luc4n3x.levyra.data.hqaudio.AudioCredentialsSource
import com.luc4n3x.levyra.data.hqaudio.AudioCredentialsRuntime
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.network.LevyraNetworkConfiguration
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject

internal class QobuzApi(
    private val exchange: QobuzExchange = QobuzNetwork,
    private val clock: () -> Long = System::currentTimeMillis,
    private val credentials: List<QobuzCredentials.Account>? = null,
    private val credentialSource: AudioCredentialsSource = AudioCredentialsRuntime
) {
    @Volatile private var preferredToken: String? = null

    suspend fun get(path: String, parameters: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            require(path in setOf("track/search", "track/get", "track/getFileUrl", "user/get"))
            val initial = credentials?.let { AudioCredentials(0, it, AudioCredentials.embedded().tidal) }
                ?: credentialSource.current()
            val tried = mutableSetOf<List<String>>()
            var response: ProviderHttpResponse? = null
            suspend fun attempt(accounts: List<QobuzCredentials.Account>): Boolean {
                val preferred = preferredToken
                for (account in accounts.sortedBy { it.userAuthToken != preferred }) {
                    if (!tried.add(listOf(account.userAuthToken, account.appId, account.appSecret))) continue
                    val result = request(path, parameters, account)
                    response = result
                    if (result.code !in setOf(401, 403)) {
                        if (result.isSuccessful) preferredToken = account.userAuthToken
                        return true
                    }
                }
                return false
            }
            if (!attempt(initial.qobuz) && credentials == null) {
                attempt(credentialSource.refreshAfterRejection(initial).qobuz)
            }
            val result = response ?: throw QobuzFailure(ProviderFailure.FORBIDDEN)
            requireSuccess(result)
            JSONObject(result.bodyText)
        }

    suspend fun probe(url: HttpUrl): ProviderHttpResponse = exchange.execute(
        Request.Builder().url(url).header("Range", "bytes=0-8191").build(), 8_192
    ).also(::requireSuccess)

    private suspend fun request(path: String, parameters: Map<String, String>, account: QobuzCredentials.Account): ProviderHttpResponse {
        val url = "$BASE/$path".toHttpUrl().newBuilder()
        parameters.forEach { (key, value) -> url.addQueryParameter(key, value) }
        if (path == "track/getFileUrl") {
            val timestamp = (clock() / 1_000L).toString()
            val signature = signature(parameters.getValue("track_id"), parameters.getValue("format_id"), timestamp, account.appSecret)
            url.addQueryParameter("request_ts", timestamp).addQueryParameter("request_sig", signature)
        }
        return exchange.execute(Request.Builder().url(url.build())
            .header("X-App-Id", account.appId).header("X-User-Auth-Token", account.userAuthToken).build(), 1_000_000)
    }

    private fun requireSuccess(response: ProviderHttpResponse) {
        if (response.isSuccessful) return
        throw QobuzFailure(when (response.code) {
            401, 403 -> ProviderFailure.FORBIDDEN
            404 -> ProviderFailure.NOT_FOUND
            429 -> ProviderFailure.RATE_LIMITED
            else -> ProviderFailure.HTTP_ERROR
        }, response.header("Retry-After")?.toLongOrNull()?.coerceIn(1, 600)?.times(1_000))
    }

    companion object {
        private const val BASE = "https://www.qobuz.com/api.json/0.2"

        fun signature(trackId: String, format: String, timestamp: String, secret: String): String =
            MessageDigest.getInstance("MD5").digest(
                "trackgetFileUrlformat_id${format}intentstreamtrack_id$trackId$timestamp$secret".toByteArray(Charsets.UTF_8)
            ).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

internal class QobuzFailure(val failure: ProviderFailure, val retryAfterMs: Long? = null) : IOException(failure.name)

internal fun interface QobuzExchange {
    suspend fun execute(request: Request, limit: Int): ProviderHttpResponse
}

internal object QobuzNetwork : QobuzExchange {
    @Volatile private var generation = Long.MIN_VALUE
    @Volatile private var client: OkHttpClient? = null

    private fun client(): OkHttpClient = synchronized(this) {
        val current = LevyraNetworkConfiguration.generation
        client?.takeIf { generation == current }?.let { return@synchronized it }
        val base = LevyraHttpClientFactory.externalIntegrations()
        base.newBuilder().connectTimeout(1_000, TimeUnit.MILLISECONDS).readTimeout(1_500, TimeUnit.MILLISECONDS)
            .callTimeout(2_000, TimeUnit.MILLISECONDS).followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .dns(Dns { host -> base.dns.lookup(host).also { addresses ->
                if (addresses.isEmpty() || addresses.any { !SafeImageUrlPolicy.isPublicAddress(it) }) throw IOException("Blocked Qobuz destination")
            } })
            .addNetworkInterceptor { chain ->
                val address = chain.connection()?.socket()?.inetAddress
                if (!allows(chain.request().url) || address == null || !SafeImageUrlPolicy.isPublicAddress(address)) throw IOException("Blocked Qobuz connection")
                chain.proceed(chain.request())
            }.build().also { client = it; generation = current }
    }

    fun allows(url: HttpUrl): Boolean = url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
        url.host in setOf("www.qobuz.com", "streaming-qobuz-std.akamaized.net", "streaming-qobuz-hires.akamaized.net")

    override suspend fun execute(request: Request, limit: Int): ProviderHttpResponse = suspendCancellableCoroutine { continuation ->
        if (!allows(request.url) || limit !in 1..1_000_000) {
            continuation.resumeWithException(IOException("Blocked Qobuz request"))
            return@suspendCancellableCoroutine
        }
        val call = client().newCall(request)
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
