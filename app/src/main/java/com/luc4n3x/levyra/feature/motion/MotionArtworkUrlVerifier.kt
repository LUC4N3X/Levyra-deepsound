package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface MotionArtworkVerificationResult {
    data object Verified : MotionArtworkVerificationResult
    data object Invalid : MotionArtworkVerificationResult
    data class Failed(val cause: Throwable? = null) : MotionArtworkVerificationResult
}

internal object MotionArtworkDestinationPolicy {
    fun isAllowedUrl(provider: String, url: HttpUrl): Boolean {
        if (url.scheme != "https" || url.port != 443) return false
        val host = url.host.lowercase(Locale.ROOT)
        return when (provider) {
            "apple-motion" -> APPLE_MEDIA_HOSTS.any { allowed ->
                host == allowed || host.endsWith(".$allowed")
            }
            "tidal-video-cover" -> host == TIDAL_MEDIA_HOST
            else -> false
        }
    }

    fun isPublicAddress(address: InetAddress): Boolean {
        if (
            address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }
        val bytes = address.address
        if (bytes.size == 4) {
            val first = bytes[0].toInt() and 0xff
            val second = bytes[1].toInt() and 0xff
            if (first == 100 && second in 64..127) return false
        }
        if (address is Inet6Address && bytes.isNotEmpty()) {
            val first = bytes[0].toInt() and 0xff
            if ((first and 0xfe) == 0xfc) return false
        }
        return true
    }

    private val APPLE_MEDIA_HOSTS = setOf(
        "itunes.apple.com",
        "mzstatic.com"
    )
    private const val TIDAL_MEDIA_HOST = "resources.tidal.com"
}

class MotionArtworkUrlVerifier(context: Context) {
    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                if (addresses.isEmpty() || addresses.any { !MotionArtworkDestinationPolicy.isPublicAddress(it) }) {
                    throw UnknownHostException("Blocked non-public motion artwork destination")
                }
                return addresses
            }
        })
        .build()

    suspend fun verify(candidate: MotionArtworkCandidate): MotionArtworkVerificationResult = withContext(Dispatchers.IO) {
        val initialUrl = candidate.url.toHttpUrlOrNull()
            ?: return@withContext MotionArtworkVerificationResult.Invalid
        if (!MotionArtworkDestinationPolicy.isAllowedUrl(candidate.provider, initialUrl)) {
            return@withContext MotionArtworkVerificationResult.Invalid
        }
        try {
            when (val head = executeProbe(candidate, initialUrl, head = true)) {
                ProbeResult.Playable -> MotionArtworkVerificationResult.Verified
                ProbeResult.Invalid -> MotionArtworkVerificationResult.Invalid
                ProbeResult.Failed -> MotionArtworkVerificationResult.Failed()
                ProbeResult.RetryWithGet -> when (executeProbe(candidate, initialUrl, head = false)) {
                    ProbeResult.Playable -> MotionArtworkVerificationResult.Verified
                    ProbeResult.Invalid,
                    ProbeResult.RetryWithGet -> MotionArtworkVerificationResult.Invalid
                    ProbeResult.Failed -> MotionArtworkVerificationResult.Failed()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            Timber.d(error, "Blocked motion artwork destination")
            MotionArtworkVerificationResult.Invalid
        } catch (error: UnknownHostException) {
            Timber.d(error, "Motion artwork destination resolution failed")
            MotionArtworkVerificationResult.Failed(error)
        } catch (error: Exception) {
            Timber.d(error, "Motion artwork verification failed")
            MotionArtworkVerificationResult.Failed(error)
        }
    }

    private fun executeProbe(
        candidate: MotionArtworkCandidate,
        initialUrl: HttpUrl,
        head: Boolean
    ): ProbeResult {
        var currentUrl = initialUrl
        var redirects = 0
        while (true) {
            if (!MotionArtworkDestinationPolicy.isAllowedUrl(candidate.provider, currentUrl)) {
                return ProbeResult.Invalid
            }
            val requestBuilder = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", USER_AGENT)
            if (head) {
                requestBuilder.head()
            } else {
                requestBuilder.get().header("Range", "bytes=0-1023")
            }
            val response = client.newCall(requestBuilder.build()).execute()
            try {
                if (response.code in REDIRECT_CODES) {
                    if (redirects >= MAX_REDIRECTS) return ProbeResult.Invalid
                    val location = response.header("Location") ?: return ProbeResult.Invalid
                    val redirectUrl = currentUrl.resolve(location) ?: return ProbeResult.Invalid
                    if (!MotionArtworkDestinationPolicy.isAllowedUrl(candidate.provider, redirectUrl)) {
                        return ProbeResult.Invalid
                    }
                    currentUrl = redirectUrl
                    redirects++
                    continue
                }
                return classifyResponse(response, currentUrl, head)
            } finally {
                response.close()
            }
        }
    }

    private fun classifyResponse(response: Response, finalUrl: HttpUrl, head: Boolean): ProbeResult {
        if (response.isSuccessful || response.code == 206) {
            return if (contentLooksPlayable(response.header("Content-Type"), finalUrl)) {
                ProbeResult.Playable
            } else {
                ProbeResult.Invalid
            }
        }
        if (response.code == 404 || response.code == 410) return ProbeResult.Invalid
        if (head && response.code in HEAD_FALLBACK_CODES) return ProbeResult.RetryWithGet
        return ProbeResult.Failed
    }

    private fun contentLooksPlayable(contentType: String?, url: HttpUrl): Boolean {
        val normalized = contentType.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
        if (normalized.startsWith("video/")) return true
        if (normalized.contains("mpegurl")) return true
        if (normalized.isNotBlank() && normalized != "application/octet-stream") return false
        return url.encodedPath.endsWith(".mp4", true) || url.encodedPath.endsWith(".m3u8", true)
    }

    private enum class ProbeResult {
        Playable,
        RetryWithGet,
        Invalid,
        Failed
    }

    private companion object {
        const val MAX_REDIRECTS = 4
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
        val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
        val HEAD_FALLBACK_CODES = setOf(400, 403, 405)
    }
}
