package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.network.LevyraNetworkConfiguration
import java.io.IOException
import java.net.InetAddress
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

data class ProviderHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val maxBodyBytes: Int,
    val allowedHosts: Set<String> = emptySet()
)

class ProviderHttpResponse(
    val code: Int,
    headers: Map<String, String>,
    val body: ByteArray
) {
    private val normalizedHeaders = headers.mapKeys { it.key.lowercase(Locale.ROOT) }

    val isSuccessful: Boolean
        get() = code in 200..299

    val bodyText: String
        get() = body.toString(Charsets.UTF_8)

    fun header(name: String): String? = normalizedHeaders[name.lowercase(Locale.ROOT)]
}

fun interface ProviderHttpExchange {
    suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse
}

internal object ProviderDestinationPolicy {
    private val allowedDomains = setOf("jiosaavn.com", "saavncdn.com")

    fun allows(url: HttpUrl): Boolean {
        if (!url.isHttps) return false
        val host = url.host.lowercase(Locale.ROOT).trimEnd('.')
        return allowedDomains.any { domain -> host == domain || host.endsWith(".$domain") }
    }

    fun requireAllowed(url: String): HttpUrl {
        val parsed = url.toHttpUrlOrNull() ?: throw IOException("Invalid provider URL")
        if (!allows(parsed)) throw IOException("Blocked provider destination")
        return parsed
    }
}

internal object ProviderDestinationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!ProviderDestinationPolicy.allows(chain.request().url)) {
            throw IOException("Blocked provider redirect destination")
        }
        return chain.proceed(chain.request())
    }
}

internal class OkHttpProviderExchange(
    private val clientProvider: () -> OkHttpClient
) : ProviderHttpExchange {
    override suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse =
        suspendCancellableCoroutine { continuation ->
            val builder = Request.Builder().url(ProviderDestinationPolicy.requireAllowed(request.url)).get()
            request.headers.forEach { (name, value) -> builder.header(name, value) }
            val call = clientProvider().newCall(builder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use { it.toProviderResponse(request.maxBodyBytes) } }
                    if (!continuation.isActive) return
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) }
                    )
                }
            })
        }

    private fun Response.toProviderResponse(maxBodyBytes: Int): ProviderHttpResponse {
        val source = body.source()
        val buffer = Buffer()
        var remaining = maxBodyBytes.toLong()
        while (remaining > 0L) {
            val read = source.read(buffer, remaining)
            if (read == -1L) break
            remaining -= read
        }
        val headerValues = headers.names().associateWith { name -> headers[name].orEmpty() }
        return ProviderHttpResponse(code, headerValues, buffer.readByteArray())
    }
}

internal class ConfigurableProviderExchange(
    private val clientProvider: () -> OkHttpClient = { HighQualityProviderHttpClient.configurableClient() }
) : ProviderHttpExchange {
    override suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse =
        suspendCancellableCoroutine { continuation ->
            val parsed = request.url.toHttpUrlOrNull()
            if (parsed == null || !ConfigurableProviderDestinationPolicy.allows(parsed, request.allowedHosts)) {
                continuation.resumeWithException(IOException("Blocked configurable provider destination"))
                return@suspendCancellableCoroutine
            }
            val builder = Request.Builder()
                .url(parsed)
                .get()
                .tag(AllowedProviderDestinations::class.java, AllowedProviderDestinations(request.allowedHosts))
            request.headers.forEach { (name, value) -> builder.header(name, value) }
            val call = clientProvider().newCall(builder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use { it.toBoundedProviderResponse(request.maxBodyBytes) } }
                    if (!continuation.isActive) return
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) }
                    )
                }
            })
        }
}

private data class AllowedProviderDestinations(val hosts: Set<String>)

internal object ConfigurableProviderDestinationPolicy {
    fun allows(url: HttpUrl, allowedHosts: Set<String>): Boolean {
        if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty() || url.port != 443) return false
        val host = normalizeHost(url.host)
        return host.isNotBlank() && allowedHosts.any { normalizeHost(it) == host }
    }

    fun normalizeHost(value: String): String = value.trim().trimEnd('.').lowercase(Locale.ROOT)
}

private object ConfigurableProviderDestinationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val allowed = chain.request().tag(AllowedProviderDestinations::class.java)?.hosts.orEmpty()
        if (!ConfigurableProviderDestinationPolicy.allows(chain.request().url, allowed)) {
            throw IOException("Blocked configurable provider redirect destination")
        }
        return chain.proceed(chain.request())
    }
}

private object PublicProviderDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.isEmpty() || addresses.any(::isNonPublicAddress)) {
            throw IOException("Provider host resolved to a non-public address")
        }
        return addresses
    }

    private fun isNonPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) {
            return true
        }
        val bytes = address.address
        if (bytes.size == 4) {
            val first = bytes[0].toInt() and 0xff
            val second = bytes[1].toInt() and 0xff
            val third = bytes[2].toInt() and 0xff
            return first == 0 || first == 127 || first >= 224 ||
                (first == 100 && second in 64..127) ||
                (first == 169 && second == 254) ||
                (first == 192 && second == 0) ||
                (first == 192 && second == 88 && third == 99) ||
                (first == 198 && second in 18..19) ||
                (first == 198 && second == 51 && third == 100) ||
                (first == 203 && second == 0 && third == 113)
        }
        if (bytes.size == 16) {
            val first = bytes[0].toInt() and 0xff
            val second = bytes[1].toInt() and 0xff
            val documentationRange = first == 0x20 && second == 0x01 &&
                (bytes[2].toInt() and 0xff) == 0x0d && (bytes[3].toInt() and 0xff) == 0xb8
            return (first and 0xfe) == 0xfc ||
                (first == 0xfe && (second and 0xc0) == 0x80) ||
                documentationRange
        }
        return true
    }
}

private fun Response.toBoundedProviderResponse(maxBodyBytes: Int): ProviderHttpResponse {
    val source = body.source()
    val buffer = Buffer()
    var remaining = maxBodyBytes.toLong().coerceAtLeast(0L)
    while (remaining > 0L) {
        val read = source.read(buffer, remaining)
        if (read == -1L) break
        remaining -= read
    }
    val headerValues = headers.names().associateWith { name -> headers[name].orEmpty() }
    return ProviderHttpResponse(code, headerValues, buffer.readByteArray())
}

internal object HighQualityProviderHttpClient {
    private const val CONNECT_TIMEOUT_MS = 2_500L
    private const val READ_TIMEOUT_MS = 4_000L
    private const val WRITE_TIMEOUT_MS = 2_000L
    private const val CALL_TIMEOUT_MS = 6_000L

    private val lock = Any()

    @Volatile
    private var client: OkHttpClient? = null

    @Volatile
    private var generation = Long.MIN_VALUE

    @Volatile
    private var configurableClient: OkHttpClient? = null

    @Volatile
    private var configurableGeneration = Long.MIN_VALUE

    fun client(): OkHttpClient {
        val currentGeneration = LevyraNetworkConfiguration.generation
        client?.takeIf { generation == currentGeneration }?.let { return it }
        return synchronized(lock) {
            client?.takeIf { generation == currentGeneration } ?: LevyraHttpClientFactory.externalIntegrations()
                .newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addNetworkInterceptor(ProviderDestinationInterceptor)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(false)
                .build()
                .also {
                    client = it
                    generation = currentGeneration
                }
        }
    }

    fun configurableClient(): OkHttpClient {
        val currentGeneration = LevyraNetworkConfiguration.generation
        configurableClient?.takeIf { configurableGeneration == currentGeneration }?.let { return it }
        return synchronized(lock) {
            configurableClient?.takeIf { configurableGeneration == currentGeneration }
                ?: LevyraHttpClientFactory.externalIntegrations()
                    .newBuilder()
                    .dns(PublicProviderDns)
                    .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .addNetworkInterceptor(ConfigurableProviderDestinationInterceptor)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(false)
                    .build()
                    .also {
                        configurableClient = it
                        configurableGeneration = currentGeneration
                    }
        }
    }
}
