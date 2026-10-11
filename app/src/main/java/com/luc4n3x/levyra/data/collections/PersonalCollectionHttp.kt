package com.luc4n3x.levyra.data.collections

import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.isLikelyArtworkBytes
import java.io.IOException
import java.io.InputStream
import java.net.Proxy
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

data class CollectionAccessRule(
    val allowCleartext: Boolean,
    val allowLocalNetwork: Boolean,
    val credentials: Map<String, CollectionCredential> = emptyMap()
)

enum class CollectionAccessDenial { Cleartext, LocalNetwork, BlockedAddress, UnknownHost }

class CollectionAccessDeniedException(val reason: CollectionAccessDenial) :
    IOException("Personal collection request blocked: ${reason.name}")

class CollectionHttpException(val code: Int) : IOException("Personal collection server answered HTTP $code")

class CollectionPayloadTooLargeException : IOException("Personal collection payload exceeds the allowed size")

class CollectionNotPlaylistException : IOException("Personal collection server returned a web page instead of a playlist")

object PersonalCollectionNetworkPolicy {
    const val COLLECTION_HEADER = "X-Levyra-Collection"

    @Volatile private var rules: Map<String, CollectionAccessRule> = emptyMap()

    fun replace(next: Map<String, CollectionAccessRule>) {
        rules = next
    }

    fun ruleForCollection(collectionId: String): CollectionAccessRule? = rules[collectionId]

    internal fun hostKey(url: HttpUrl): String = "${url.host.lowercase(Locale.ROOT)}:${url.port}"

    internal fun hostKey(url: String): String? = url.toHttpUrlOrNull()?.let(::hostKey)
}

internal class CollectionAccessInterceptor(
    private val classify: (java.net.InetAddress) -> CollectionAddressClass = PersonalCollectionUrlPolicy::classify
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val rule = request.tag(CollectionAccessRule::class.java)
            ?: request.header(PersonalCollectionNetworkPolicy.COLLECTION_HEADER)?.let(PersonalCollectionNetworkPolicy::ruleForCollection)
            ?: throw CollectionAccessDeniedException(CollectionAccessDenial.UnknownHost)
        if (!url.isHttps && !rule.allowCleartext) throw CollectionAccessDeniedException(CollectionAccessDenial.Cleartext)
        val address = chain.connection()?.route()?.socketAddress?.address
        if (address != null) {
            when (classify(address)) {
                CollectionAddressClass.Blocked -> throw CollectionAccessDeniedException(CollectionAccessDenial.BlockedAddress)
                CollectionAddressClass.Private -> if (!rule.allowLocalNetwork) {
                    throw CollectionAccessDeniedException(CollectionAccessDenial.LocalNetwork)
                }
                CollectionAddressClass.Public -> Unit
            }
        }
        val credential = rule.credentials[PersonalCollectionUrlPolicy.origin(url)]
        val outgoing = request.newBuilder()
            .removeHeader(PersonalCollectionNetworkPolicy.COLLECTION_HEADER)
            .removeHeader("Referer")
            .removeHeader("Origin")
            .removeHeader("Cookie")
            .header("User-Agent", USER_AGENT)
            .apply {
                if (credential != null) {
                    header("Authorization", Credentials.basic(credential.username, credential.password, StandardCharsets.UTF_8))
                } else {
                    removeHeader("Authorization")
                }
            }
            .build()
        return chain.proceed(outgoing)
    }

    companion object {
        val USER_AGENT = "Levyra/${BuildConfig.VERSION_NAME} (Android)"
    }
}

object PersonalCollectionHttp {
    const val MAX_ARTWORK_BYTES = 5L * 1024L * 1024L

    internal fun newClient(
        interceptor: CollectionAccessInterceptor = CollectionAccessInterceptor(),
        readTimeoutMs: Long,
        callTimeoutMs: Long
    ): OkHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .addNetworkInterceptor(interceptor)
        .build()

    val metadataClient: OkHttpClient by lazy { newClient(readTimeoutMs = 20_000L, callTimeoutMs = 45_000L) }

    val mediaClient: OkHttpClient by lazy { newClient(readTimeoutMs = 30_000L, callTimeoutMs = 0L) }

    sealed interface PlaylistFetch {
        data object NotModified : PlaylistFetch
        data class Fetched(val bytes: ByteArray, val etag: String, val lastModified: String, val finalUrl: String) : PlaylistFetch
    }

    suspend fun fetchPlaylist(
        url: String,
        rule: CollectionAccessRule,
        etag: String = "",
        lastModified: String = "",
        client: OkHttpClient = metadataClient
    ): PlaylistFetch {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, application/json, text/plain, */*;q=0.5")
            .tag(CollectionAccessRule::class.java, rule)
            .apply {
                if (etag.isNotBlank()) header("If-None-Match", etag)
                if (lastModified.isNotBlank()) header("If-Modified-Since", lastModified)
            }
            .build()
        return client.newCall(request).await().use { response ->
            if (response.code == 304) return@use PlaylistFetch.NotModified
            if (!response.isSuccessful) throw CollectionHttpException(response.code)
            val type = response.header("Content-Type").orEmpty().lowercase(Locale.ROOT)
            if (type.contains("text/html")) throw CollectionNotPlaylistException()
            val body = response.body
            if (body.contentLength() > PersonalCollectionParser.MAX_PLAYLIST_BYTES) throw CollectionPayloadTooLargeException()
            val bytes = withContext(Dispatchers.IO) {
                readBounded(body.byteStream(), PersonalCollectionParser.MAX_PLAYLIST_BYTES.toLong())
            } ?: throw CollectionPayloadTooLargeException()
            PlaylistFetch.Fetched(
                bytes = bytes,
                etag = response.header("ETag").orEmpty(),
                lastModified = response.header("Last-Modified").orEmpty(),
                finalUrl = response.request.url.toString()
            )
        }
    }

    suspend fun fetchArtwork(url: String, rule: CollectionAccessRule, client: OkHttpClient = metadataClient): ByteArray? {
        val request = Request.Builder()
            .url(url.toHttpUrlOrNull() ?: return null)
            .header("Accept", "image/*")
            .tag(CollectionAccessRule::class.java, rule)
            .build()
        return client.newCall(request).await().use { response ->
            if (!response.isSuccessful) return@use null
            val type = response.header("Content-Type").orEmpty().lowercase(Locale.ROOT)
            if (type.isNotBlank() && !type.startsWith("image/") && !type.startsWith("application/octet-stream")) return@use null
            if (response.body.contentLength() > MAX_ARTWORK_BYTES) return@use null
            val bytes = withContext(Dispatchers.IO) { readBounded(response.body.byteStream(), MAX_ARTWORK_BYTES) }
            bytes?.takeIf(::isLikelyArtworkBytes)
        }
    }

    internal fun readBounded(input: InputStream, maxBytes: Long): ByteArray? = input.use { stream ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    }
}

internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}
