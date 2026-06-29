package com.luc4n3x.levyra.data.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.luc4n3x.levyra.BuildConfig
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

object LevyraHttpClientFactory {
    fun media(context: Context? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(28, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        return applyDebugInterceptors(builder, context).build()
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
