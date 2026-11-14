package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.stream.YoutubeStreamNetworkPolicy
import com.luc4n3x.levyra.desktop.core.stream.YoutubeStreamIdentity
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class YoutubeLocalStreamBridge(
    private val upstreamClient: OkHttpClient = ExtractorHttp.client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
    private val upstreamUrlValidator: (HttpUrl) -> Boolean = YoutubeStreamNetworkPolicy::isAllowedUrl,
    private val upstreamAddressResolver: (String) -> List<InetAddress> = YoutubeStreamNetworkPolicy::resolveAddresses,
    private val upstreamAddressValidator: (InetAddress) -> Boolean = YoutubeStreamNetworkPolicy::isPublicInternetAddress,
    private val tokenGenerator: () -> String = ::newSessionToken,
    private val upstreamChunkBytes: Long = DEFAULT_UPSTREAM_CHUNK_BYTES,
    private val onStreamFailure: (String, String) -> Unit = { _, _ -> }
) : AutoCloseable {
    internal val bindAddress: InetAddress = InetAddress.getByName(LOOPBACK_ADDRESS)
    private val securedUpstreamClient = upstreamClient.newBuilder()
        .dns(YoutubeStreamNetworkPolicy.validatingDns(upstreamAddressResolver, upstreamAddressValidator))
        .build()
    private val executor = ThreadPoolExecutor(
        MAX_SERVER_THREADS,
        MAX_SERVER_THREADS,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_SERVER_QUEUE),
        { task -> Thread(task, "levyra-youtube-bridge").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )
    private val server = HttpServer.create(InetSocketAddress(bindAddress, 0), SERVER_BACKLOG)
    private val sessions = ConcurrentHashMap<String, StreamSession>()
    private val closed = AtomicBoolean(false)
    private val lifecycleLock = Any()
    private val requestNumber = AtomicLong()

    internal val port: Int get() = server.address.port

    init {
        require(upstreamChunkBytes > 0L)
        server.executor = executor
        server.createContext(STREAM_PATH, ::handle)
        server.start()
    }

    fun openSession(upstreamUrl: String): String = synchronized(lifecycleLock) {
        check(!closed.get()) { "Bridge already closed" }
        val parsed = upstreamUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid upstream URL")
        validateTarget(parsed)
        repeat(MAX_TOKEN_ATTEMPTS) {
            val sessionId = tokenGenerator().takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty bridge token")
            if (sessions.putIfAbsent(sessionId, StreamSession(parsed, sessionId)) == null) {
                return@synchronized "http://$LOOPBACK_ADDRESS:$port$STREAM_PATH/$sessionId"
            }
        }
        throw IllegalStateException("Unable to allocate bridge token")
    }

    fun closeSession(localUrl: String) {
        sessionIdOf(localUrl)?.let { sessionId -> sessions.remove(sessionId)?.close() }
    }

    fun failureReason(localUrl: String): String? =
        sessionIdOf(localUrl)?.let { sessions[it]?.failureReason?.get() }

    fun didOpenUpstream(localUrl: String): Boolean =
        sessionIdOf(localUrl)?.let { sessions[it]?.upstreamOpened?.get() } == true

    override fun close() {
        val closingSessions = synchronized(lifecycleLock) {
            if (!closed.compareAndSet(false, true)) return
            val current = sessions.values.toList()
            sessions.clear()
            server.stop(0)
            current
        }
        closingSessions.forEach(StreamSession::close)
        executor.shutdownNow()
    }

    private fun handle(exchange: HttpExchange) {
        try {
            val sessionId = exchange.requestURI.path
                .removePrefix("$STREAM_PATH/")
                .takeIf { it.isNotBlank() && '/' !in it }
            val session = sessionId?.let(sessions::get)
            if (session == null || session.closed.get()) {
                exchange.sendResponseHeaders(404, -1L)
                return
            }
            when (exchange.requestMethod.uppercase(Locale.ROOT)) {
                "GET" -> session.serve(exchange, headOnly = false)
                "HEAD" -> session.serve(exchange, headOnly = true)
                else -> {
                    exchange.responseHeaders.set("Allow", "GET, HEAD")
                    exchange.sendResponseHeaders(405, -1L)
                }
            }
        } catch (_: IOException) {
        } catch (_: Exception) {
            runCatching { exchange.sendResponseHeaders(502, -1L) }
        } finally {
            exchange.close()
        }
    }

    private fun validateTarget(url: HttpUrl) {
        require(upstreamUrlValidator(url)) { "Rejected upstream URL" }
    }

    private fun sessionIdOf(localUrl: String): String? {
        val url = localUrl.toHttpUrlOrNull() ?: return null
        if (url.scheme != "http" || url.host != LOOPBACK_ADDRESS || url.port != port) return null
        val segments = url.encodedPathSegments
        if (segments.size != 2 || segments[0] != STREAM_PATH.removePrefix("/")) return null
        return segments[1].takeIf { it.isNotBlank() }
    }

    private inner class StreamSession(initialUrl: HttpUrl, private val sessionId: String) : AutoCloseable {
        private val effectiveUrl = AtomicReference(initialUrl)
        private val calls = ConcurrentHashMap.newKeySet<Call>()
        private val callLock = Any()
        val failureReason = AtomicReference<String?>(null)
        private val failureContentRange = AtomicReference<String?>(null)
        val upstreamOpened = AtomicBoolean(false)
        val closed = AtomicBoolean(false)
        private val failurePublished = AtomicBoolean(false)

        private fun publishFailure() {
            val reason = failureReason.get() ?: return
            if (!closed.get() && failurePublished.compareAndSet(false, true)) {
                onStreamFailure("http://$LOOPBACK_ADDRESS:$port$STREAM_PATH/$sessionId", reason)
            }
        }

        fun serve(exchange: HttpExchange, headOnly: Boolean) {
            if (closed.get()) {
                exchange.sendResponseHeaders(410, -1L)
                return
            }
            val requested = parseRange(exchange.requestHeaders.getFirst("Range"))
            if (requested === InvalidRange) {
                exchange.responseHeaders.set("Accept-Ranges", "bytes")
                exchange.sendResponseHeaders(416, -1L)
                return
            }
            val range = requested as? ByteRange
            val start = range?.start ?: 0L
            val knownTotal = effectiveUrl.get().queryParameter("clen")?.toLongOrNull()?.takeIf { it > 0L }
            if (knownTotal != null && start >= knownTotal) {
                exchange.responseHeaders.set("Content-Range", "bytes */$knownTotal")
                exchange.sendResponseHeaders(416, -1L)
                return
            }
            val chunkEnd = start + minOf(upstreamChunkBytes - 1L, Long.MAX_VALUE - start)
            val firstEnd = minOf(range?.endInclusive ?: chunkEnd, chunkEnd, knownTotal?.minus(1L) ?: Long.MAX_VALUE)
            val first = openSlice(start, firstEnd)
            if (first == null) {
                val status = upstreamStatus(failureReason.get())
                failureContentRange.get()?.let { exchange.responseHeaders.set("Content-Range", it) }
                exchange.sendResponseHeaders(status, -1L)
                publishFailure()
                return
            }
            try {
                first.use { initial ->
                    val total = initial.totalBytes
                        ?: throw UpstreamFailure("BRIDGE_UPSTREAM_FAILED")
                    if (start >= total) {
                        failureReason.set("HTTP_416")
                        exchange.responseHeaders.set("Content-Range", "bytes */$total")
                        exchange.sendResponseHeaders(416, -1L)
                        return
                    }
                    val finalEnd = range?.endInclusive?.coerceAtMost(total - 1L) ?: total - 1L
                    val contentLength = finalEnd - start + 1L
                    exchange.responseHeaders.set("Accept-Ranges", "bytes")
                    initial.contentType?.let { exchange.responseHeaders.set("Content-Type", it) }
                    if (range != null) {
                        exchange.responseHeaders.set("Content-Range", "bytes $start-$finalEnd/$total")
                    }
                    if (headOnly) {
                        exchange.responseHeaders.set("Content-Length", contentLength.toString())
                        exchange.sendResponseHeaders(if (range == null) 200 else 206, -1L)
                        return
                    }
                    exchange.sendResponseHeaders(if (range == null) 200 else 206, contentLength)
                    exchange.responseBody.use { output ->
                        copySlice(initial, output, finalEnd)
                        var next = initial.endInclusive + 1L
                        while (next <= finalEnd && !closed.get()) {
                            val end = next + minOf(upstreamChunkBytes - 1L, finalEnd - next)
                            openSlice(next, end)?.use { slice ->
                                if (slice.start != next || slice.endInclusive > end || slice.totalBytes != total) {
                                    throw UpstreamFailure("BRIDGE_UPSTREAM_CHANGED")
                                }
                                copySlice(slice, output, finalEnd)
                                next = slice.endInclusive + 1L
                            } ?: throw UpstreamFailure(failureReason.get() ?: "BRIDGE_UPSTREAM_FAILED")
                        }
                    }
                }
            } catch (failure: UpstreamFailure) {
                failureReason.compareAndSet(null, failure.message ?: "BRIDGE_UPSTREAM_FAILED")
                System.getLogger("Levyra.StreamBridge").log(System.Logger.Level.WARNING, failureReason.get())
                publishFailure()
                throw failure
            }
        }

        private fun copySlice(slice: UpstreamSlice, output: OutputStream, finalEnd: Long) {
            val expected = (slice.endInclusive.coerceAtMost(finalEnd) - slice.start + 1L)
                .coerceAtLeast(0L)
            val copied = copyExactly(slice.response.body.byteStream(), output, expected)
            if (copied != expected) throw UpstreamFailure("BRIDGE_UPSTREAM_TRUNCATED")
        }

        private fun openSlice(start: Long, endInclusive: Long): UpstreamSlice? {
            var target = effectiveUrl.get()
            repeat(MAX_REDIRECTS + 1) { redirectCount ->
                if (closed.get()) return null
                validateTarget(target)
                val googleVideo = target.host == "googlevideo.com" || target.host.endsWith(".googlevideo.com")
                val requestUrl = if (googleVideo) target.newBuilder()
                    .setQueryParameter("rn", requestNumber.incrementAndGet().toString())
                    .setQueryParameter("range", "$start-$endInclusive")
                    .build() else target
                val identity = YoutubeStreamIdentity.forUrl(target.toString())
                val request = Request.Builder()
                    .url(requestUrl)
                    .get()
                    .apply { if (!googleVideo) header("Range", "bytes=$start-$endInclusive") }
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("User-Agent", identity?.userAgent ?: ExtractorHttp.YOUTUBE_STREAM_USER_AGENT)
                    .apply { identity?.referrer?.let { header("Referer", it) } }
                    .build()
                val call = securedUpstreamClient.newCall(request)
                if (!registerCall(call)) return null
                val response = try {
                    call.execute()
                } catch (_: IOException) {
                    calls -= call
                    if (!closed.get()) failureReason.set("BRIDGE_UPSTREAM_FAILED")
                    return null
                }
                if (response.code in 300..399) {
                    calls -= call
                    val location = response.header("Location")
                    response.close()
                    val redirected = location?.let(target::resolve)
                    if (redirected == null || redirectCount >= MAX_REDIRECTS) {
                        failureReason.set("BRIDGE_UPSTREAM_FAILED")
                        return null
                    }
                    if (runCatching { validateTarget(redirected) }.isFailure) {
                        failureReason.set("BRIDGE_UPSTREAM_FAILED")
                        return null
                    }
                    target = redirected
                    effectiveUrl.set(redirected)
                    return@repeat
                }
                if (response.code !in setOf(200, 206)) {
                    calls -= call
                    failureReason.set(httpFailure(response.code, target))
                    failureContentRange.set(response.header("Content-Range"))
                    response.close()
                    return null
                }
                val contentType = response.header("Content-Type")
                System.getLogger("Levyra.StreamBridge").log(
                    System.Logger.Level.DEBUG,
                    "range=$start-$endInclusive status=${response.code} bytes=${response.body.contentLength()} contentRange=${response.header("Content-Range").orEmpty()}"
                )
                if (isExplicitlyNonMedia(contentType)) {
                    calls -= call
                    failureReason.set("BRIDGE_UPSTREAM_FAILED")
                    response.close()
                    return null
                }
                val parsedRange = response.header("Content-Range")?.let(::parseContentRange)
                val queryTotal = if (googleVideo) target.queryParameter("clen")?.toLongOrNull() else null
                val querySlice = googleVideo && queryTotal != null && queryTotal > endInclusive &&
                    response.body.contentLength() == endInclusive - start + 1L &&
                    (parsedRange == null ||
                        parsedRange.start == start && parsedRange.endInclusive == endInclusive && parsedRange.totalBytes == queryTotal ||
                        parsedRange.start == 0L && parsedRange.endInclusive == endInclusive - start &&
                        parsedRange.totalBytes == endInclusive - start + 1L)
                val sliceStart = if (querySlice) start else parsedRange?.start ?: 0L
                val sliceEnd = (if (querySlice) endInclusive else parsedRange?.endInclusive)
                    ?: response.body.contentLength().takeIf { it > 0L }?.let { sliceStart + it - 1L }
                    ?: run {
                        calls -= call
                        failureReason.set("BRIDGE_UPSTREAM_FAILED")
                        response.close()
                        return null
                    }
                val total = (if (querySlice) queryTotal else parsedRange?.totalBytes)
                    ?: response.body.contentLength().takeIf { it >= 0L }
                if (
                    !querySlice && response.code == 206 &&
                    (parsedRange == null || sliceStart != start || sliceEnd > endInclusive) ||
                    !querySlice && response.code == 200 && start > 0L ||
                    sliceEnd - sliceStart + 1L > upstreamChunkBytes ||
                    response.body.contentLength() >= 0L && response.body.contentLength() != sliceEnd - sliceStart + 1L
                ) {
                    calls -= call
                    failureReason.set("BRIDGE_UPSTREAM_FAILED")
                    response.close()
                    return null
                }
                failureReason.set(null)
                failureContentRange.set(null)
                upstreamOpened.set(true)
                return UpstreamSlice(call, response, sliceStart, sliceEnd, total, contentType)
            }
            failureReason.set("BRIDGE_UPSTREAM_FAILED")
            return null
        }

        private fun registerCall(call: Call): Boolean = synchronized(callLock) {
            if (closed.get()) {
                call.cancel()
                false
            } else {
                calls += call
                true
            }
        }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            synchronized(callLock) {
                calls.forEach(Call::cancel)
                calls.clear()
            }
        }

        private inner class UpstreamSlice(
            private val call: Call,
            val response: Response,
            val start: Long,
            val endInclusive: Long,
            val totalBytes: Long?,
            val contentType: String?
        ) : AutoCloseable {
            override fun close() {
                response.close()
                calls -= call
            }
        }
    }

    private data class ByteRange(val start: Long, val endInclusive: Long?)
    private data object InvalidRange
    private data class ContentRange(val start: Long, val endInclusive: Long, val totalBytes: Long?)
    private class UpstreamFailure(message: String) : IOException(message)

    private companion object {
        const val LOOPBACK_ADDRESS = "127.0.0.1"
        const val STREAM_PATH = "/stream"
        const val SERVER_BACKLOG = 16
        const val MAX_SERVER_THREADS = 4
        const val MAX_SERVER_QUEUE = 32
        const val MAX_REDIRECTS = 3
        const val MAX_TOKEN_ATTEMPTS = 8
        const val DEFAULT_UPSTREAM_CHUNK_BYTES = 262_144L
        const val COPY_BUFFER_BYTES = 32 * 1_024
        val RANGE_PATTERN = Regex("bytes=(\\d+)-(\\d*)", RegexOption.IGNORE_CASE)
        val CONTENT_RANGE_PATTERN = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)", RegexOption.IGNORE_CASE)

        fun parseRange(raw: String?): Any? {
            if (raw.isNullOrBlank()) return null
            if (',' in raw) return InvalidRange
            val match = RANGE_PATTERN.matchEntire(raw.trim()) ?: return InvalidRange
            val start = match.groupValues[1].toLongOrNull() ?: return InvalidRange
            val end = match.groupValues[2].takeIf(String::isNotBlank)?.let { it.toLongOrNull() ?: return InvalidRange }
            if (end != null && end < start) return InvalidRange
            return ByteRange(start, end)
        }

        fun parseContentRange(raw: String): ContentRange? {
            val match = CONTENT_RANGE_PATTERN.matchEntire(raw.trim()) ?: return null
            val start = match.groupValues[1].toLongOrNull() ?: return null
            val end = match.groupValues[2].toLongOrNull() ?: return null
            val total = match.groupValues[3].takeUnless { it == "*" }?.toLongOrNull()
            if (end < start || (total != null && end >= total)) return null
            return ContentRange(start, end, total)
        }

        fun copyExactly(input: InputStream, output: OutputStream, limit: Long): Long {
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var remaining = limit
            var copied = 0L
            while (remaining > 0L) {
                val read = try {
                    input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                } catch (_: IOException) {
                    throw UpstreamFailure("BRIDGE_UPSTREAM_TRUNCATED")
                }
                if (read < 0) break
                output.write(buffer, 0, read)
                copied += read
                remaining -= read
            }
            return copied
        }

        fun upstreamStatus(reason: String?): Int = when (reason) {
            "HTTP_403", "STREAM_EXPIRED" -> 403
            "HTTP_410" -> 410
            "HTTP_416" -> 416
            "HTTP_429" -> 429
            else -> 502
        }

        fun httpFailure(status: Int, url: HttpUrl): String = when (status) {
            403 -> if (isExpired(url)) "STREAM_EXPIRED" else "HTTP_403"
            410 -> "HTTP_410"
            416 -> "HTTP_416"
            429 -> "HTTP_429"
            else -> "BRIDGE_UPSTREAM_FAILED"
        }

        fun isExpired(url: HttpUrl): Boolean {
            val expiresAtSeconds = url.queryParameter("expire")?.toLongOrNull() ?: return false
            return expiresAtSeconds * 1000L <= System.currentTimeMillis()
        }

        fun isExplicitlyNonMedia(contentType: String?): Boolean {
            val normalized = contentType.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
            return normalized.startsWith("text/") ||
                normalized == "application/json" ||
                normalized == "application/xml"
        }

        fun newSessionToken(): String {
            val bytes = ByteArray(32)
            SESSION_RANDOM.nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        val SESSION_RANDOM = SecureRandom()
    }
}
