package com.luc4n3x.levyra.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.google.android.gms.net.CronetProviderInstaller
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.io.EOFException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLException
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import timber.log.Timber

@UnstableApi
object PlaybackNetworkStack {
    private const val USER_AGENT = "LevyraPlayer/1.13 Android Music"
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val READ_BUFFER_BYTES = 64 * 1024
    private const val TRANSPORT_CACHE_BYTES = 2L * 1024L * 1024L
    private const val INITIALIZATION_RETRY_MS = 5L * 60L * 1_000L
    private const val BASE_COOLDOWN_MS = 30_000L
    private const val MAX_COOLDOWN_MS = 5L * 60L * 1_000L

    private val initializationInFlight = AtomicBoolean(false)
    private val nextInitializationAttemptMs = AtomicLong(0L)
    private val disabledUntilMs = AtomicLong(0L)
    private val consecutiveFailures = AtomicInteger(0)
    private val executor: ExecutorService = Executors.newFixedThreadPool(2, PlaybackThreadFactory())

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var cronetEngine: CronetEngine? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        ensureCronetInitialization()
    }

    fun playbackFactory(context: Context): HttpDataSource.Factory {
        initialize(context)
        return ResilientHttpDataSourceFactory(UrlRequest.Builder.REQUEST_PRIORITY_HIGHEST)
    }

    fun warmupFactory(context: Context): HttpDataSource.Factory {
        initialize(context)
        return ResilientHttpDataSourceFactory(UrlRequest.Builder.REQUEST_PRIORITY_LOW)
    }

    private fun ensureCronetInitialization() {
        if (cronetEngine != null) return
        val context = appContext ?: return
        val now = SystemClock.elapsedRealtime()
        if (now < nextInitializationAttemptMs.get()) return
        if (!initializationInFlight.compareAndSet(false, true)) return
        CronetProviderInstaller.installProvider(context).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                initializationInFlight.set(false)
                nextInitializationAttemptMs.set(SystemClock.elapsedRealtime() + INITIALIZATION_RETRY_MS)
                Timber.w(task.exception, "Cronet provider unavailable; playback will use OkHttp")
                return@addOnCompleteListener
            }
            executor.execute {
                runCatching { buildCronetEngine(context) }.onSuccess { engine ->
                    cronetEngine = engine
                    consecutiveFailures.set(0)
                    disabledUntilMs.set(0L)
                    nextInitializationAttemptMs.set(Long.MAX_VALUE)
                    Timber.i("Cronet playback transport ready")
                }.onFailure { error ->
                    nextInitializationAttemptMs.set(SystemClock.elapsedRealtime() + INITIALIZATION_RETRY_MS)
                    Timber.w(error, "Cronet engine creation failed; playback will use OkHttp")
                }
                initializationInFlight.set(false)
            }
        }
    }

    private fun buildCronetEngine(context: Context): CronetEngine {
        return runCatching {
            CronetEngine.Builder(context)
                .setStoragePath(context.getDir("cronet_transport", Context.MODE_PRIVATE).absolutePath)
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, TRANSPORT_CACHE_BYTES)
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .build()
        }.getOrElse { cachedEngineError ->
            Timber.w(cachedEngineError, "Cronet transport cache unavailable; retrying without disk state")
            CronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .build()
        }
    }

    private fun createCronetFactory(requestPriority: Int): HttpDataSource.Factory? {
        ensureCronetInitialization()
        val engine = cronetEngine ?: return null
        if (SystemClock.elapsedRealtime() < disabledUntilMs.get()) return null
        return CronetDataSource.Factory(engine, executor)
            .setUserAgent(USER_AGENT)
            .setRequestPriority(requestPriority)
            .setConnectionTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setReadBufferSize(READ_BUFFER_BYTES)
            .setResetTimeoutOnRedirects(true)
    }

    private fun createOkHttpFactory(): HttpDataSource.Factory {
        val context = appContext
        return OkHttpDataSource.Factory(LevyraHttpClientFactory.media(context))
            .setUserAgent(USER_AGENT)
    }

    private fun reportCronetSuccess() {
        consecutiveFailures.set(0)
        disabledUntilMs.set(0L)
    }

    private fun reportCronetFailure(error: Throwable) {
        val failureCount = consecutiveFailures.incrementAndGet().coerceAtMost(5)
        val multiplier = 1L shl (failureCount - 1)
        val cooldown = (BASE_COOLDOWN_MS * multiplier).coerceAtMost(MAX_COOLDOWN_MS)
        disabledUntilMs.set(SystemClock.elapsedRealtime() + cooldown)
        Timber.w(error, "Cronet transport failed; OkHttp enabled for %d ms", cooldown)
    }

    private fun isTransportFailure(error: Throwable): Boolean {
        if (error is HttpDataSource.InvalidResponseCodeException) return false
        if (error is HttpDataSource.InvalidContentTypeException) return false
        var current: Throwable? = error
        while (current != null) {
            when (current) {
                is SocketTimeoutException,
                is UnknownHostException,
                is ConnectException,
                is NoRouteToHostException,
                is SocketException,
                is SSLException,
                is EOFException,
                is ProtocolException,
                is CronetException -> return true
                is InterruptedIOException -> return false
            }
            current = current.cause
        }
        val httpError = error as? HttpDataSource.HttpDataSourceException ?: return false
        return httpError.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            httpError.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    }

    private class ResilientHttpDataSourceFactory(
        private val requestPriority: Int
    ) : HttpDataSource.Factory {
        private val defaultRequestProperties = HttpDataSource.RequestProperties()

        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): HttpDataSource.Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        override fun createDataSource(): HttpDataSource {
            return ResilientHttpDataSource(requestPriority, defaultRequestProperties)
        }
    }

    private class ResilientHttpDataSource(
        private val requestPriority: Int,
        private val defaultRequestProperties: HttpDataSource.RequestProperties
    ) : HttpDataSource {
        private val requestProperties = HttpDataSource.RequestProperties()
        private val transferListeners = CopyOnWriteArrayList<TransferListener>()
        private var activeSource: HttpDataSource? = null
        private var activeTransport = Transport.NONE
        private var cronetConfirmed = false

        override fun addTransferListener(transferListener: TransferListener) {
            transferListeners.addIfAbsent(transferListener)
            activeSource?.addTransferListener(transferListener)
        }

        override fun setRequestProperty(name: String, value: String) {
            requestProperties.set(name, value)
        }

        override fun clearRequestProperty(name: String) {
            requestProperties.remove(name)
        }

        override fun clearAllRequestProperties() {
            requestProperties.clear()
        }

        override fun open(dataSpec: DataSpec): Long {
            check(activeSource == null) { "Data source is already open" }
            val cronetFactory = createCronetFactory(requestPriority)
            if (cronetFactory != null) {
                val cronetSource = prepareSource(prepareFactory(cronetFactory).createDataSource())
                activeSource = cronetSource
                activeTransport = Transport.CRONET
                try {
                    return cronetSource.open(dataSpec)
                } catch (error: HttpDataSource.HttpDataSourceException) {
                    closeActiveQuietly()
                    if (!isTransportFailure(error)) throw error
                    reportCronetFailure(error)
                }
            }
            val okHttpSource = prepareSource(prepareFactory(createOkHttpFactory()).createDataSource())
            activeSource = okHttpSource
            activeTransport = Transport.OKHTTP
            return okHttpSource.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val source = checkNotNull(activeSource) { "Data source is not open" }
            return try {
                source.read(buffer, offset, length).also { bytesRead ->
                    if (activeTransport == Transport.CRONET && bytesRead > 0 && !cronetConfirmed) {
                        cronetConfirmed = true
                        reportCronetSuccess()
                    }
                }
            } catch (error: HttpDataSource.HttpDataSourceException) {
                if (activeTransport == Transport.CRONET && isTransportFailure(error)) {
                    reportCronetFailure(error)
                }
                throw error
            }
        }

        override fun getUri(): Uri? = activeSource?.getUri()

        override fun getResponseHeaders(): Map<String, List<String>> = activeSource?.getResponseHeaders().orEmpty()

        override fun getResponseCode(): Int = activeSource?.getResponseCode() ?: -1

        override fun close() {
            val source = activeSource
            activeSource = null
            activeTransport = Transport.NONE
            cronetConfirmed = false
            source?.close()
        }

        private fun prepareFactory(factory: HttpDataSource.Factory): HttpDataSource.Factory {
            return factory.setDefaultRequestProperties(defaultRequestProperties.getSnapshot())
        }

        private fun prepareSource(source: HttpDataSource): HttpDataSource {
            for ((name, value) in requestProperties.getSnapshot()) {
                source.setRequestProperty(name, value)
            }
            for (listener in transferListeners) {
                source.addTransferListener(listener)
            }
            return source
        }

        private fun closeActiveQuietly() {
            val source = activeSource
            activeSource = null
            activeTransport = Transport.NONE
            cronetConfirmed = false
            runCatching { source?.close() }
        }
    }

    private enum class Transport {
        NONE,
        CRONET,
        OKHTTP
    }

    private class PlaybackThreadFactory : ThreadFactory {
        private val threadNumber = AtomicInteger(1)

        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "levyra-cronet-${threadNumber.getAndIncrement()}").apply {
                priority = Thread.NORM_PRIORITY + 1
            }
        }
    }
}
