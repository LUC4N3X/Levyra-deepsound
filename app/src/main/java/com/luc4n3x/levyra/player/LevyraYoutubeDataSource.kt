package com.luc4n3x.levyra.player

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import java.util.concurrent.atomic.AtomicLong

@UnstableApi
class LevyraYoutubeDataSource private constructor(
    private val delegate: DataSource
) : DataSource {
    companion object {
        private val requestNumber = AtomicLong(1L)
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return LevyraYoutubeDataSource(upstreamFactory.createDataSource())
        }
    }

    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val originalUrl = dataSpec.uri.toString()
        if (!isYoutubeMediaUrl(dataSpec.uri)) return delegate.open(dataSpec)
        val adaptedUri = appendRequestNumber(dataSpec.uri)
        val headers = requestHeaders(originalUrl)
        val httpDelegate = delegate as? HttpDataSource
        if (httpDelegate != null) {
            httpDelegate.clearAllRequestProperties()
            headers.forEach { (name, value) -> httpDelegate.setRequestProperty(name, value) }
        }
        val adaptedSpec = dataSpec
            .withUri(adaptedUri)
            .withAdditionalHeaders(headers)
        return delegate.open(adaptedSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return delegate.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = delegate.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders

    override fun close() {
        delegate.close()
    }

    private fun appendRequestNumber(uri: Uri): Uri {
        if (!isProgressiveGoogleVideo(uri)) return uri
        if (!uri.getQueryParameter("rn").isNullOrBlank()) return uri
        return uri.buildUpon()
            .appendQueryParameter("rn", requestNumber.getAndIncrement().toString())
            .build()
    }

    private fun requestHeaders(url: String): Map<String, String> {
        val clientParam = url.substringAfter("?c=", "").ifBlank { url.substringAfter("&c=", "") }.substringBefore('&').uppercase()
        val userAgent = when {
            clientParam == "VISIONOS" -> "com.google.visionos.youtube/1.02(RealityDevice14,1; U; CPU visionOS 25_6_0 like Mac OS X; US)"
            clientParam == "ANDROID_VR" -> "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip"
            clientParam == "ANDROID_MUSIC" -> "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) com.google.android.apps.youtube.music/8.10.52"
            clientParam == "ANDROID" || runCatching { YoutubeParsingHelper.isAndroidStreamingUrl(url) }.getOrDefault(false) -> YoutubeParsingHelper.getAndroidUserAgent(null)
            clientParam == "IOS" || clientParam == "IPHONE" || runCatching { YoutubeParsingHelper.isIosStreamingUrl(url) }.getOrDefault(false) -> YoutubeParsingHelper.getIosUserAgent(null)
            clientParam == "WEB_REMIX" || clientParam == "WEB" || clientParam == "WEB_EMBEDDED_PLAYER" || clientParam == "TVHTML5_SIMPLY_EMBEDDED_PLAYER" ->
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
            else -> "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
        }
        val isMusicWeb = clientParam == "WEB_REMIX" || url.contains("music.youtube.com")
        val isWeb = clientParam.startsWith("WEB") || isMusicWeb ||
            runCatching { YoutubeParsingHelper.isWebStreamingUrl(url) }.getOrDefault(false)
        val embedded = clientParam.contains("EMBEDDED") ||
            runCatching { YoutubeParsingHelper.isTvHtml5SimplyEmbeddedPlayerStreamingUrl(url) }.getOrDefault(false)

        return linkedMapOf(
            "User-Agent" to userAgent,
            "Accept" to "*/*",
            "Accept-Encoding" to "identity"
        ).apply {
            if (isWeb || embedded) {
                put("Origin", if (isMusicWeb) "https://music.youtube.com" else "https://www.youtube.com")
                put("Referer", if (embedded) "https://www.youtube.com/embed/" else if (isMusicWeb) "https://music.youtube.com/" else "https://www.youtube.com/")
                put("Sec-Fetch-Dest", "empty")
                put("Sec-Fetch-Mode", "cors")
                put("Sec-Fetch-Site", "cross-site")
            }
        }
    }

    private fun isYoutubeMediaUrl(uri: Uri): Boolean {
        val host = uri.host.orEmpty().lowercase()
        return host.endsWith("googlevideo.com") ||
            host.endsWith("youtube.com") ||
            host.endsWith("youtube-nocookie.com") ||
            host.endsWith("ytimg.com")
    }

    private fun isProgressiveGoogleVideo(uri: Uri): Boolean {
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty().lowercase()
        if (!host.endsWith("googlevideo.com")) return false
        if (!path.contains("videoplayback")) return false
        if (uri.getQueryParameter("sq") != null) return false
        if (path.endsWith(".m3u8") || path.endsWith(".mpd")) return false
        return true
    }
}
