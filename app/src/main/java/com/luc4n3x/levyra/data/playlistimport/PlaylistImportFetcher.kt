package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.isPublicNetworkAddress
import com.luc4n3x.levyra.data.readUtf8Bounded
import com.luc4n3x.levyra.domain.PlaylistImportFailureKind
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class PlaylistImportException(
    val kind: PlaylistImportFailureKind,
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

data class PlaylistFetchRequest(
    val url: String,
    val allowedHosts: Set<String>,
    val acceptedMimeTypes: Set<String>,
    val maxBytes: Long,
    val accept: String = acceptedMimeTypes.joinToString(",")
)

data class PlaylistFetchResponse(
    val finalUrl: HttpUrl,
    val body: String
)

internal fun validatePublicImportUrl(value: String, allowedHosts: Set<String>): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.port != 443) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    if (!hostAllowed(url.host.lowercase(Locale.ROOT), allowedHosts)) return null
    return url
}

internal fun hostAllowed(host: String, allowedHosts: Set<String>): Boolean =
    allowedHosts.any { allowed ->
        if (allowed.startsWith("*.")) host.endsWith(allowed.substring(1)) && host.length > allowed.length - 1 else host == allowed
    }

internal fun importContentTypeAccepted(value: String?, accepted: Set<String>): Boolean {
    val mime = value?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return mime in accepted
}

class PlaylistImportFetcher(baseClient: OkHttpClient) {
    private val baseDns = baseClient.dns
    private val client = baseClient.newBuilder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = baseDns.lookup(hostname)
                if (addresses.isEmpty() || addresses.any { !isPublicNetworkAddress(it) }) {
                    throw UnknownHostException("Import destination is not public")
                }
                return addresses
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(request: PlaylistFetchRequest): PlaylistFetchResponse {
        var attempt = 0
        while (true) {
            try {
                return fetchOnce(request)
            } catch (error: PlaylistImportException) {
                val transient = error.kind == PlaylistImportFailureKind.RATE_LIMITED || error.kind == PlaylistImportFailureKind.NETWORK
                if (!transient || attempt >= MAX_RETRIES) throw error
            } catch (error: IOException) {
                if (attempt >= MAX_RETRIES) throw PlaylistImportException(PlaylistImportFailureKind.NETWORK, "Import request failed", error)
            }
            delay(BACKOFF_MS shl attempt)
            attempt++
        }
    }

    private suspend fun fetchOnce(request: PlaylistFetchRequest): PlaylistFetchResponse {
        var current = validatePublicImportUrl(request.url, request.allowedHosts)
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Import URL not allowed")
        var redirects = 0
        while (true) {
            val call = Request.Builder()
                .url(current)
                .header("User-Agent", USER_AGENT)
                .header("Accept", request.accept)
                .header("Accept-Language", "en-US,en;q=0.8")
                .get()
                .build()
            val response = execute(call)
            try {
                if (response.code in REDIRECT_CODES) {
                    if (redirects >= MAX_REDIRECTS) {
                        throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Too many redirects")
                    }
                    val location = response.header("Location")
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Missing redirect location")
                    val next = current.resolve(location)
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Invalid redirect")
                    current = validatePublicImportUrl(next.toString(), request.allowedHosts)
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Redirect not allowed")
                    redirects++
                    continue
                }
                when {
                    response.code == 401 -> throw PlaylistImportException(PlaylistImportFailureKind.AUTH_REQUIRED, "Authentication required")
                    response.code == 403 || response.code == 404 || response.code == 410 ->
                        throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Playlist unavailable")
                    response.code == 429 -> throw PlaylistImportException(PlaylistImportFailureKind.RATE_LIMITED, "Rate limited")
                    response.code >= 500 -> throw PlaylistImportException(PlaylistImportFailureKind.NETWORK, "Provider unavailable")
                    !response.isSuccessful -> throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Unexpected ${response.code}")
                }
                val body = response.body
                if (!importContentTypeAccepted(body.contentType()?.toString(), request.acceptedMimeTypes)) {
                    throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Unexpected content type")
                }
                if (body.contentLength() > request.maxBytes) {
                    throw PlaylistImportException(PlaylistImportFailureKind.TOO_LARGE, "Response too large")
                }
                val text = body.byteStream().use { readUtf8Bounded(it, request.maxBytes) }
                    ?: throw PlaylistImportException(PlaylistImportFailureKind.TOO_LARGE, "Response too large")
                return PlaylistFetchResponse(current, text)
            } finally {
                response.close()
            }
        }
    }

    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }
                continuation.resume(response) { _, value, _ -> value.close() }
            }
        })
    }

    companion object {
        const val MAX_RETRIES = 2
        private const val BACKOFF_MS = 800L
        private const val MAX_REDIRECTS = 4
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36"
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        val HTML = setOf("text/html", "application/xhtml+xml")
        val JSON = setOf("application/json", "text/json", "text/javascript", "text/plain", "text/html")
    }
}
