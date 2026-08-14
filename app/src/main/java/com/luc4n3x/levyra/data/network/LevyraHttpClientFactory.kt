package com.luc4n3x.levyra.data.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.luc4n3x.levyra.BuildConfig
import java.io.File
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
    private val downloadConnectionPool = ConnectionPool(16, 5, TimeUnit.MINUTES)
    private val downloadDispatcher = Dispatcher().apply {
        maxRequests = 32
        maxRequestsPerHost = 8
    }

    @Volatile
    private var mediaClient: OkHttpClient? = null

    @Volatile
    private var youtubePlayerClient: OkHttpClient? = null

    @Volatile
    private var extractorClient: OkHttpClient? = null

    @Volatile
    private var downloadClient: OkHttpClient? = null

    @Volatile
    private var feedClient: OkHttpClient? = null

    fun feeds(context: Context): OkHttpClient {
        return feedClient ?: synchronized(this) {
            feedClient ?: media(context).newBuilder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .callTimeout(9, TimeUnit.SECONDS)
                .addInterceptor(BrotliInterceptor)
                .cache(Cache(File(context.applicationContext.cacheDir, FEED_CACHE_DIRECTORY), FEED_CACHE_BYTES))
                .build()
                .also { feedClient = it }
        }
    }

    fun media(context: Context? = null): OkHttpClient {
        return mediaClient ?: synchronized(this) {
            mediaClient ?: OkHttpClient.Builder()
                .connectionPool(mediaConnectionPool)
                .dispatcher(mediaDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .let { applyNetworkIntelligence(it, context) }
                .let { applyDebugInterceptors(it, context) }
                .build()
                .also { mediaClient = it }
        }
    }

    fun youtubePlayer(context: Context? = null): OkHttpClient {
        return youtubePlayerClient ?: synchronized(this) {
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
        return extractorClient ?: synchronized(this) {
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
        return downloadClient ?: synchronized(this) {
            downloadClient ?: OkHttpClient.Builder()
                .connectionPool(downloadConnectionPool)
                .dispatcher(downloadDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
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
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .let { applyNetworkIntelligence(it, context) }
        return applyDebugInterceptors(builder, context).build()
    }

    private fun applyNetworkIntelligence(builder: OkHttpClient.Builder, context: Context?): OkHttpClient.Builder {
        context?.let(LevyraNetworkIntelligence::initialize)
        return builder
            .dns(LevyraNetworkIntelligence.dns)
            .eventListenerFactory(LevyraNetworkIntelligence.eventListenerFactory)
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
