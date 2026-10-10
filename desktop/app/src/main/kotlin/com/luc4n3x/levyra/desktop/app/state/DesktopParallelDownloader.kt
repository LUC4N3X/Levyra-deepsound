package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.io.BufferedInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request

internal class DesktopParallelDownloader(baseClient: OkHttpClient) {
    private val client = baseClient.newBuilder()
        .dns(PublicAddressDns(baseClient.dns))
        .addNetworkInterceptor(PublicDownloadUrlInterceptor)
        .dispatcher(Dispatcher().apply {
            maxRequests = MAX_REQUESTS
            maxRequestsPerHost = MAX_REQUESTS_PER_HOST
        })
        .connectionPool(ConnectionPool(CONNECTION_POOL_SIZE, 5L, TimeUnit.MINUTES))
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .callTimeout(0L, TimeUnit.MILLISECONDS)
        .readTimeout(60L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .build()

    suspend fun download(
        url: String,
        output: Path,
        totalBytes: Long,
        ranges: List<DesktopDownloadRange>,
        onProgress: (Long) -> Unit
    ) {
        require(totalBytes > 0L) { "La dimensione del download parallelo deve essere nota" }
        require(ranges.isNotEmpty()) { "Serve almeno un segmento per il download parallelo" }
        Files.deleteIfExists(output)
        RandomAccessFile(output.toFile(), "rw").use { it.setLength(totalBytes) }
        val downloadedBytes = AtomicLong(0L)
        val concurrency = minOf(desktopDownloadConcurrency(totalBytes), ranges.size).coerceAtLeast(1)
        val limiter = Semaphore(concurrency)
        try {
            coroutineScope {
                ranges.map { range ->
                    async(Dispatchers.IO) {
                        limiter.withPermit {
                            downloadRangeWithRetry(url, range, output)
                            onProgress(downloadedBytes.addAndGet(range.length).coerceAtMost(totalBytes))
                        }
                    }
                }.awaitAll()
            }
            if (downloadedBytes.get() != totalBytes || Files.size(output) != totalBytes) {
                throw IOException("Download parallelo incompleto: ${downloadedBytes.get()}/$totalBytes byte")
            }
        } catch (error: Throwable) {
            runCatching { Files.deleteIfExists(output) }
            throw error
        }
    }

    private suspend fun downloadRangeWithRetry(
        url: String,
        range: DesktopDownloadRange,
        output: Path
    ) {
        var lastError: IOException? = null
        repeat(RANGE_RETRY_COUNT) { attempt ->
            try {
                downloadRange(url, range, output)
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: IOException) {
                lastError = error
                if (attempt < RANGE_RETRY_COUNT - 1) {
                    delay(RANGE_RETRY_DELAY_MS * (attempt + 1L))
                }
            }
        }
        throw lastError ?: IOException("Segmento download non riuscito")
    }

    private suspend fun downloadRange(
        url: String,
        range: DesktopDownloadRange,
        output: Path
    ) {
        val normalizedUrl = stripDesktopAudioRangeParameter(url)
        val rangeUrl = desktopRangeUrl(normalizedUrl, range)
        val rangeParamApplied = rangeUrl != normalizedUrl
        val safeRangeUrl = requirePublicDownloadUrl(rangeUrl)
        val request = Request.Builder()
            .url(safeRangeUrl)
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("Accept", "audio/*,*/*;q=0.8")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .apply {
                if (!rangeParamApplied) {
                    header("Range", "bytes=${range.start}-${range.endInclusive}")
                }
            }
            .build()

        client.newCall(request).awaitResponse().use { response ->
            val body = response.body
            val bodyLength = body.contentLength()
            val contentRange = response.header("Content-Range").orEmpty()
            if (!isUsableDesktopRangeResponse(
                    code = response.code,
                    bodyLength = bodyLength,
                    contentRange = contentRange,
                    range = range,
                    rangeParamApplied = rangeParamApplied
                )
            ) {
                throw IOException("Download a segmenti non supportato: HTTP ${response.code}")
            }
            var written = 0L
            BufferedInputStream(body.byteStream(), BUFFER_SIZE).use { input ->
                RandomAccessFile(output.toFile(), "rw").use { target ->
                    target.seek(range.start)
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (written < range.length) {
                        currentCoroutineContext().ensureActive()
                        val maxRead = minOf(buffer.size.toLong(), range.length - written).toInt()
                        val read = input.read(buffer, 0, maxRead)
                        if (read < 0) break
                        target.write(buffer, 0, read)
                        written += read
                    }
                }
            }
            if (written != range.length) {
                throw IOException(
                    "Segmento troncato: ${range.start}-${range.endInclusive} ($written/${range.length} byte)"
                )
            }
        }
    }

    companion object {
        private const val MAX_REQUESTS = 64
        private const val MAX_REQUESTS_PER_HOST = 24
        private const val CONNECTION_POOL_SIZE = 24
        private const val BUFFER_SIZE = 64 * 1024
        private const val RANGE_RETRY_COUNT = 2
        private const val RANGE_RETRY_DELAY_MS = 250L
    }
}

internal data class DesktopDownloadRange(
    val start: Long,
    val endInclusive: Long
) {
    val length: Long
        get() = endInclusive - start + 1L
}

internal fun planDesktopDownloadRanges(
    contentLength: Long,
    chunkSize: Long = 2L * 1024L * 1024L,
    minimumLength: Long = 2L * 1024L * 1024L
): List<DesktopDownloadRange> {
    if (contentLength < minimumLength || chunkSize <= 0L) return emptyList()
    val ranges = mutableListOf<DesktopDownloadRange>()
    var start = 0L
    while (start < contentLength) {
        val end = minOf(start + chunkSize - 1L, contentLength - 1L)
        ranges += DesktopDownloadRange(start, end)
        start = end + 1L
    }
    return ranges
}

internal fun desktopDownloadConcurrency(contentLength: Long): Int {
    val oneMb = 1024L * 1024L
    return when {
        contentLength >= 192L * oneMb -> 20
        contentLength >= 96L * oneMb -> 18
        contentLength >= 24L * oneMb -> 16
        else -> 12
    }
}

internal fun desktopDownloadChunkSize(contentLength: Long): Long {
    if (contentLength <= 0L) return 2L * 1024L * 1024L
    val oneMb = 1024L * 1024L
    val concurrency = desktopDownloadConcurrency(contentLength)
    val targetRanges = (concurrency * 2).coerceIn(16, 48)
    val rawSize = (contentLength + targetRanges - 1L) / targetRanges
    val alignment = 256L * 1024L
    val aligned = ((rawSize + alignment - 1L) / alignment) * alignment
    return aligned.coerceIn(oneMb, 8L * oneMb)
}

internal fun desktopAudioContentLength(url: String): Long {
    val query = runCatching { URI(url).rawQuery }.getOrNull().orEmpty()
    return query.split('&')
        .asSequence()
        .mapNotNull { entry ->
            val key = entry.substringBefore('=', "")
            val value = entry.substringAfter('=', "")
            value.toLongOrNull()?.takeIf { key.equals("clen", ignoreCase = true) && it > 0L }
        }
        .firstOrNull()
        ?: -1L
}

internal fun stripDesktopAudioRangeParameter(url: String): String {
    val fragmentIndex = url.indexOf('#')
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val source = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val queryIndex = source.indexOf('?')
    if (queryIndex < 0) return url
    val base = source.substring(0, queryIndex)
    val retained = source.substring(queryIndex + 1)
        .split('&')
        .filterNot { it.substringBefore('=').equals("range", ignoreCase = true) }
    val normalized = if (retained.isEmpty()) base else "$base?${retained.joinToString("&")}"
    return normalized + fragment
}

internal fun desktopRangeUrl(url: String, range: DesktopDownloadRange): String {
    val source = stripDesktopAudioRangeParameter(url)
    val host = runCatching { URI(source).host.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
    if (!host.endsWith("googlevideo.com")) return source
    val separator = if (source.contains('?')) '&' else '?'
    return "$source${separator}range=${range.start}-${range.endInclusive}"
}

internal fun isUsableDesktopRangeResponse(
    code: Int,
    bodyLength: Long,
    contentRange: String,
    range: DesktopDownloadRange,
    rangeParamApplied: Boolean
): Boolean {
    if (code == 206) {
        if (bodyLength > 0L && bodyLength != range.length) return false
        if (contentRange.isBlank()) return true
        val bounds = contentRange.substringAfter("bytes", contentRange)
            .substringBefore('/')
            .trim()
            .split('-', limit = 2)
        val start = bounds.getOrNull(0)?.trim()?.toLongOrNull()
        val end = bounds.getOrNull(1)?.trim()?.toLongOrNull()
        return start == range.start && end == range.endInclusive
    }
    if (!rangeParamApplied || code !in 200..299) return false
    if (bodyLength != range.length) return false
    if (contentRange.isBlank()) return true
    return contentRange.contains("${range.start}-${range.endInclusive}")
}
