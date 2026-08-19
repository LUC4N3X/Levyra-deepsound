package com.luc4n3x.levyra.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

@UnstableApi
class LevyraYoutubeDataSource private constructor(
    private val delegate: DataSource
) : DataSource {
    private var openedSpec: DataSpec? = null
    private var bytesReadSinceOpen = 0L
    private var readRetries = 0

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
        openedSpec = dataSpec
        bytesReadSinceOpen = 0L
        readRetries = 0
        return openDelegate(dataSpec)
    }

    private fun openDelegate(dataSpec: DataSpec): Long {
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
        while (true) {
            try {
                val read = delegate.read(buffer, offset, length)
                if (read > 0) bytesReadSinceOpen += read
                return read
            } catch (error: IOException) {
                if (!isRecoverableStreamEnd(error) || !resumeAfterStreamEnd()) throw error
            }
        }
    }

    private fun resumeAfterStreamEnd(): Boolean {
        val original = openedSpec ?: return false
        if (readRetries >= PLAYBACK_STREAM_READ_RETRIES) return false
        if (Thread.currentThread().isInterrupted) return false
        val remaining = if (original.length == C.LENGTH_UNSET.toLong()) {
            C.LENGTH_UNSET.toLong()
        } else {
            (original.length - bytesReadSinceOpen).coerceAtLeast(0L)
        }
        if (remaining == 0L) return false
        readRetries++
        val resumeSpec = original.buildUpon()
            .setPosition(original.position + bytesReadSinceOpen)
            .setLength(remaining)
            .build()
        runCatching { delegate.close() }
        return try {
            openDelegate(resumeSpec)
            true
        } catch (retryFailure: IOException) {
            Timber.d(retryFailure, "stream resume failed")
            false
        }
    }

    override fun getUri(): Uri? = delegate.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders

    override fun close() {
        openedSpec = null
        bytesReadSinceOpen = 0L
        readRetries = 0
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
