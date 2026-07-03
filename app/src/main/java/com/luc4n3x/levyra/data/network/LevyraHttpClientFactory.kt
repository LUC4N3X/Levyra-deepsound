package com.luc4n3x.levyra.data.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.luc4n3x.levyra.BuildConfig
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.brotli.BrotliInterceptor
import java.util.concurrent.TimeUnit

object LevyraHttpClientFactory {
    private val mediaConnectionPool = ConnectionPool(24, 5, TimeUnit.MINUTES)
    private val mediaDispatcher = Dispatcher().apply {
        maxRequests = 32
        maxRequestsPerHost = 12
    }
    private val youtubeConnectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)
    private val youtubeDispatcher = Dispatcher().apply {
        maxRequests = 48
        maxRequestsPerHost = 18
    }

    @Volatile
    private var mediaClient: OkHttpClient? = null

    @Volatile
    private var youtubePlayerClient: OkHttpClient? = null

    fun media(context: Context? = null): OkHttpClient {
        return mediaClient ?: synchronized(this) {
            mediaClient ?: OkHttpClient.Builder()
                .connectionPool(mediaConnectionPool)
                .dispatcher(mediaDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(4, TimeUnit.SECONDS)
                .callTimeout(18, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .let { applyDebugInterceptors(it, context) }
                .build()
                .also { mediaClient = it }
        }
    }

    fun youtubePlayer(): OkHttpClient {
        return youtubePlayerClient ?: synchronized(this) {
            youtubePlayerClient ?: OkHttpClient.Builder()
                .connectionPool(youtubeConnectionPool)
                .dispatcher(youtubeDispatcher)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(1_500, TimeUnit.MILLISECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .writeTimeout(1_500, TimeUnit.MILLISECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(BrotliInterceptor)
                .retryOnConnectionFailure(true)
                .build()
                .also { youtubePlayerClient = it }
        }
    }

    fun general(context: Context? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
        return applyDebugInterceptors(builder, context).build()
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
