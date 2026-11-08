package com.luc4n3x.levyra.data.hqaudio.tidal

import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.network.LevyraNetworkConfiguration
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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject

internal class TidalApi(
    private val exchange: TidalExchange = TidalNetwork,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Token(val value: String, val expiresAtMs: Long)
    private val mutex = Mutex()
    private val tokens = mutableMapOf<HttpUrl, Token>()
    @Volatile private var proxy: Pair<HttpUrl, Long>? = null

    suspend fun get(path: String, parameters: Map<String, String> = emptyMap()): JSONObject {
        require(!path.startsWith('/') && !path.contains("..") && !path.contains('?'))
        val url = "$CATALOG_BASE/$path".toHttpUrl().newBuilder().addQueryParameter("countryCode", COUNTRY)
        parameters.forEach { (key, value) -> url.addQueryParameter(key, value) }
        return parse(authorized(CATALOG_TOKEN_URL, url.build(), API_LIMIT))
    }

    suspend fun manifest(trackId: String, quality: String): ByteArray {
        val url = "$STREAM_BASE/manifests".toHttpUrl().newBuilder()
            .addQueryParameter("id", trackId).addQueryParameter("quality", quality).build()
        val attributes = parse(authorized(STREAM_TOKEN_URL, url, API_LIMIT)).optJSONObject("data")
            ?.optJSONObject("attributes") ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        if (attributes.optString("trackPresentation") != "FULL") throw TidalFailure(ProviderFailure.NOT_FOUND)
        val manifestUrl = runCatching { attributes.getString("uri").toHttpUrl() }.getOrNull()
            ?.takeIf(TidalNetwork::allowsMedia) ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val response = exchange.execute(Request.Builder().url(manifestUrl).build(), MANIFEST_LIMIT)
        if (!response.isSuccessful) throw TidalFailure(failureFor(response.code))
        return response.body
    }

    suspend fun warmCatalog() {
        runCatchingPreservingCancellation { token(CATALOG_TOKEN_URL, null) }
    }

    suspend fun warmStream() {
        runCatchingPreservingCancellation { token(STREAM_TOKEN_URL, null) }
        runCatchingPreservingCancellation { mediaProxy() }
    }

    suspend fun mediaProxy(): HttpUrl {
        proxy?.takeIf { clock() < it.second }?.let { return it.first }
        val active = parse(exchange.execute(Request.Builder().url(PROXY_HEALTH_URL).build(), TOKEN_LIMIT)).optJSONArray("active")
            ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val selected = (0 until active.length()).asSequence().mapNotNull { active.optJSONObject(it) }
            .filter { it.optInt("status") == 200 }
            .mapNotNull { runCatching { it.getString("url").toHttpUrl() }.getOrNull() }
            .firstOrNull(TidalMediaProxy::accepts) ?: throw TidalFailure(ProviderFailure.CIRCUIT_OPEN)
        proxy = selected to clock() + PROXY_TTL_MS
        return selected
    }

    suspend fun probe(url: HttpUrl, limit: Int): ProviderHttpResponse = exchange.execute(
        Request.Builder().url(url).header("Range", "bytes=0-${limit - 1}").build(), limit
    )

    private suspend fun authorized(tokenUrl: HttpUrl, url: HttpUrl, limit: Int): ProviderHttpResponse = withContext(Dispatchers.IO) {
        var bearer = token(tokenUrl, null)
        var response = exchange.execute(request(url, bearer), limit)
        if (response.code == HTTP_UNAUTHORIZED) {
            bearer = token(tokenUrl, bearer)
            response = exchange.execute(request(url, bearer), limit)
        }
        response
    }

    private fun request(url: HttpUrl, token: String) = Request.Builder().url(url).header("Authorization", "Bearer $token").build()

    private suspend fun token(tokenUrl: HttpUrl, rejected: String?): String = mutex.withLock {
        tokens[tokenUrl]?.takeIf { it.value != rejected && clock() + TOKEN_MARGIN_MS < it.expiresAtMs }?.let { return@withLock it.value }
        val json = parse(exchange.execute(Request.Builder().url(tokenUrl).build(), TOKEN_LIMIT))
        val value = json.optString("access_token").takeIf { it.length in 16..4_096 }
            ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val lifetime = json.optLong("expires_in").takeIf { it in 1L..86_400L } ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        tokens[tokenUrl] = Token(value, clock() + lifetime * 1_000L)
        value
    }

    private fun parse(response: ProviderHttpResponse): JSONObject {
        if (!response.isSuccessful) throw TidalFailure(failureFor(response.code))
        return runCatching { JSONObject(response.bodyText) }.getOrElse { throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE) }
    }

    private fun failureFor(code: Int) = when (code) {
        401, 403 -> ProviderFailure.FORBIDDEN
        404 -> ProviderFailure.NOT_FOUND
        429 -> ProviderFailure.RATE_LIMITED
        else -> ProviderFailure.HTTP_ERROR
    }

    companion object {
        const val COUNTRY = "US"
        private const val CATALOG_BASE = "https://api.tidal.com/v1"
        private const val STREAM_BASE = "https://hifi.odskyler.com"
        private val CATALOG_TOKEN_URL = "https://auth.odskyler.workers.dev/token".toHttpUrl()
        private val STREAM_TOKEN_URL = "$STREAM_BASE/token".toHttpUrl()
        private val PROXY_HEALTH_URL = "https://proxy.odskyler.workers.dev/health".toHttpUrl()
        private const val PROXY_TTL_MS = 10L * 60L * 1_000L
        private const val TOKEN_MARGIN_MS = 30_000L
        private const val TOKEN_LIMIT = 65_536
        private const val MANIFEST_LIMIT = 131_072
        private const val API_LIMIT = 1_000_000
        private const val HTTP_UNAUTHORIZED = 401
    }
}

internal class TidalFailure(val failure: ProviderFailure) : IOException(failure.name)

internal fun interface TidalExchange {
    suspend fun execute(request: Request, limit: Int): ProviderHttpResponse
}

internal object TidalMediaProxy {
    fun accepts(url: HttpUrl): Boolean = url.isHttps && url.port == 443 && url.username.isEmpty() &&
        url.password.isEmpty() && url.host.endsWith(".odskyler.com")

    fun prefix(proxy: HttpUrl): String = proxy.toString().trimEnd('/') + "/proxy/segment?url="

    fun wrap(proxy: HttpUrl, target: String): String = prefix(proxy) + java.net.URLEncoder.encode(target, Charsets.UTF_8.name())
}

internal object TidalNetwork : TidalExchange {
    const val MAX_RESPONSE_BYTES = 33_554_432
    private val workerHosts = setOf("auth.odskyler.workers.dev", "proxy.odskyler.workers.dev")

    private val lock = Any()
    @Volatile private var cached: Pair<Long, OkHttpClient>? = null

    private fun client(): OkHttpClient {
        val generation = LevyraNetworkConfiguration.generation
        cached?.takeIf { it.first == generation }?.let { return it.second }
        return synchronized(lock) {
            cached?.takeIf { it.first == generation }?.second ?: buildClient().also { cached = generation to it }
        }
    }

    private fun buildClient(): OkHttpClient {
        val base = LevyraHttpClientFactory.externalIntegrations()
        return base.newBuilder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
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

    fun allowsMedia(url: HttpUrl): Boolean = isPlainHttps(url) && (url.host.endsWith(".tidal.com") || url.host.endsWith(".tidalhifi.com"))

    fun allows(url: HttpUrl): Boolean = allowsMedia(url) || TidalMediaProxy.accepts(url) || (isPlainHttps(url) && url.host in workerHosts)

    private fun isPlainHttps(url: HttpUrl) = url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty()

    override suspend fun execute(request: Request, limit: Int): ProviderHttpResponse = suspendCancellableCoroutine { continuation ->
        if (!allows(request.url) || limit !in 1..MAX_RESPONSE_BYTES) {
            continuation.resumeWithException(IOException("Blocked TIDAL request"))
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
