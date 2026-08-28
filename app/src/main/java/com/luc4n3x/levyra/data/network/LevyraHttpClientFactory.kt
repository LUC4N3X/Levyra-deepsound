package com.luc4n3x.levyra.data.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.luc4n3x.levyra.BuildConfig
import java.io.File
import java.net.Proxy
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.brotli.BrotliInterceptor

object LevyraHttpClientFactory {
    private const val FEED_CACHE_DIRECTORY = "levyra_feed_http"
    private const val FEED_CACHE_BYTES = 8L * 1024L * 1024L

    private val mediaConnectionPool = ConnectionPool(24, 5, TimeUnit.MINUTES)
    private val mediaDispatcher = Dispatcher().apply {
        maxRequests = 40
        maxRequestsPerHost = 16
    }
    private val youtubeConnectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)
    private val youtubeDispatcher = Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 24
    }
    private val downloadConnectionPool = ConnectionPool(48, 5, TimeUnit.MINUTES)
    private val downloadDispatcher = Dispatcher().apply {
        maxRequests = 128
        maxRequestsPerHost = 48
    }
    private val externalConnectionPool = ConnectionPool(8, 5, TimeUnit.MINUTES)
    private val externalDispatcher = Dispatcher().apply {
        maxRequests = 16
        maxRequestsPerHost = 8
    }
    private val sharedConnectionPools = listOf(
        mediaConnectionPool,
        youtubeConnectionPool,
        downloadConnectionPool,
        externalConnectionPool,
        LevyraNetworkConfiguration.dohConnectionPool
    )

    private val lock = Any()

    @Volatile
    private var mediaClient: OkHttpClient? = null

    @Volatile
    private var streamingClient: OkHttpClient? = null

    @Volatile
    private var youtubePlayerClient: OkHttpClient? = null

    @Volatile
    private var extractorClient: OkHttpClient? = null

    @Volatile
    private var downloadClient: OkHttpClient? = null

    @Volatile
    private var feedClient: OkHttpClient? = null

    @Volatile
    private var externalIntegrationClient: OkHttpClient? = null

    @Volatile
    private var generalClient: OkHttpClient? = null

    @Volatile
    private var feedCache: Cache? = null

    @Volatile
    private var clientGeneration: Long = LevyraNetworkConfiguration.generation

    internal fun onConfigurationChanged() {
        synchronized(lock) {
            mediaClient = null
            streamingClient = null
            youtubePlayerClient = null
            extractorClient = null
            downloadClient = null
            feedClient = null
            externalIntegrationClient = null
            generalClient = null
            clientGeneration = LevyraNetworkConfiguration.generation
        }
        sharedConnectionPools.forEach { pool -> runCatching { pool.evictAll() } }
    }

    private fun invalidateIfStale() {
        if (clientGeneration == LevyraNetworkConfiguration.generation) return
        onConfigurationChanged()
    }

    fun externalIntegrations(): OkHttpClient {
        invalidateIfStale()
        return externalIntegrationClient ?: synchronized(lock) {
            externalIntegrationClient ?: OkHttpClient.Builder()
                .connectionPool(externalConnectionPool)
                .dispatcher(externalDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .let { applyNetworkIntelligence(it, null) }
                .build()
                .also { externalIntegrationClient = it }
        }
    }

    fun feeds(context: Context): OkHttpClient {
        invalidateIfStale()
        return feedClient ?: synchronized(lock) {
            feedClient ?: media(context).newBuilder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .callTimeout(9, TimeUnit.SECONDS)
                .addInterceptor(BrotliInterceptor)
                .cache(feedCache(context))
                .build()
                .also { feedClient = it }
        }
    }

    private fun feedCache(context: Context): Cache {
        return feedCache ?: synchronized(lock) {
            feedCache ?: Cache(
                File(context.applicationContext.cacheDir, FEED_CACHE_DIRECTORY),
                FEED_CACHE_BYTES
            ).also { feedCache = it }
        }
    }

    fun media(context: Context? = null): OkHttpClient {
        invalidateIfStale()
        return mediaClient ?: synchronized(lock) {
            mediaClient ?: mediaBuilder()
                .let { applyNetworkIntelligence(it, context) }
                .let { applyDebugInterceptors(it, context) }
                .build()
                .also { mediaClient = it }
        }
    }

    fun streaming(context: Context? = null): OkHttpClient {
        invalidateIfStale()
        if (!bypassesProxyForStreams()) return media(context)
        return streamingClient ?: synchronized(lock) {
            streamingClient ?: mediaBuilder()
                .let { applyNetworkIntelligence(it, context, allowProxy = false) }
                .let { applyDebugInterceptors(it, context) }
                .build()
                .also { streamingClient = it }
        }
    }

    private fun mediaBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectionPool(mediaConnectionPool)
        .dispatcher(mediaDispatcher)
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)

    fun youtubePlayer(context: Context? = null): OkHttpClient {
        invalidateIfStale()
        return youtubePlayerClient ?: synchronized(lock) {
            youtubePlayerClient ?: OkHttpClient.Builder()
                .connectionPool(youtubeConnectionPool)
                .dispatcher(youtubeDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(YoutubeClientIdentityInterceptor)
                .addInterceptor(BrotliInterceptor)
                .retryOnConnectionFailure(true)
                .let { applyNetworkIntelligence(it, context) }
                .build()
                .also { youtubePlayerClient = it }
        }
    }

    fun extractor(): OkHttpClient {
        invalidateIfStale()
        return extractorClient ?: synchronized(lock) {
            extractorClient ?: youtubePlayer().newBuilder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(18, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
                .also { extractorClient = it }
        }
    }

    fun download(): OkHttpClient {
        invalidateIfStale()
        return downloadClient ?: synchronized(lock) {
            downloadClient ?: OkHttpClient.Builder()
                .connectionPool(downloadConnectionPool)
                .dispatcher(downloadDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .let { applyNetworkIntelligence(it, null) }
                .build()
                .also { downloadClient = it }
        }
    }

    fun general(context: Context? = null): OkHttpClient {
        invalidateIfStale()
        return generalClient ?: synchronized(lock) {
            generalClient ?: OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .let { applyNetworkIntelligence(it, context) }
                .let { applyDebugInterceptors(it, context) }
                .build()
                .also { generalClient = it }
        }
    }

    private fun bypassesProxyForStreams(): Boolean {
        val settings = LevyraNetworkConfiguration.current()
        return settings.usesProxy && settings.bypassProxyForStreams
    }

    private fun applyNetworkIntelligence(
        builder: OkHttpClient.Builder,
        context: Context?,
        allowProxy: Boolean = true
    ): OkHttpClient.Builder {
        context?.let(LevyraNetworkIntelligence::initialize)
        builder
            .dns(LevyraNetworkConfiguration.dns())
            .eventListenerFactory(LevyraNetworkIntelligence.eventListenerFactory)
        if (allowProxy) {
            LevyraNetworkConfiguration.proxy()?.let(builder::proxy)
            LevyraNetworkConfiguration.proxyAuthenticator()?.let(builder::proxyAuthenticator)
        } else {
            builder.proxy(Proxy.NO_PROXY)
        }
        return builder
    }

    private fun applyDebugInterceptors(builder: OkHttpClient.Builder, context: Context?): OkHttpClient.Builder {
        if (BuildConfig.DEBUG && context != null) {
            builder.addInterceptor(
                ChuckerInterceptor.Builder(context.applicationContext)
                    .maxContentLength(250_000L)
                    .alwaysReadResponseBody(false)
                    .build()
            )
        }
        return builder
    }
}
