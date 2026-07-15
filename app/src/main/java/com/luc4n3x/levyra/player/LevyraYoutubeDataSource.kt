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
            .withAdditionalHeaders(headers.filterKeys { !it.equals("User-Agent", true) })
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
        val userAgent = when {
            runCatching { YoutubeParsingHelper.isIosStreamingUrl(url) }.getOrDefault(false) -> YoutubeParsingHelper.getIosUserAgent(null)
            runCatching { YoutubeParsingHelper.isAndroidStreamingUrl(url) }.getOrDefault(false) -> YoutubeParsingHelper.getAndroidUserAgent(null)
            else -> "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
        }
        val web = runCatching { YoutubeParsingHelper.isWebStreamingUrl(url) }.getOrDefault(false)
        val embedded = runCatching { YoutubeParsingHelper.isTvHtml5SimplyEmbeddedPlayerStreamingUrl(url) }.getOrDefault(false)
        return linkedMapOf(
            "User-Agent" to userAgent,
            "Accept" to "*/*",
            "Accept-Encoding" to "identity"
        ).apply {
            if (web || embedded) {
                put("Origin", "https://www.youtube.com")
                put("Referer", if (embedded) "https://www.youtube.com/embed/" else "https://www.youtube.com/")
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
