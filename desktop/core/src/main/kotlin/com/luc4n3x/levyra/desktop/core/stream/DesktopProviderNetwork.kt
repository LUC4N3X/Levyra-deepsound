package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.io.IOException
import java.net.InetAddress
import java.util.Locale
import okhttp3.OkHttpClient

internal object DesktopProviderNetwork {
    val generation: Long = 0L
    private val client by lazy {
        ExtractorHttp.client.newBuilder()
            .dns(YoutubeStreamNetworkPolicy.validatingDns())
            .addNetworkInterceptor { chain ->
                val address = chain.connection()?.route()?.socketAddress?.address
                if (address == null || !isPublicAddress(address)) throw IOException("Blocked provider connection")
                chain.proceed(chain.request())
            }.build()
    }
    fun externalIntegrations(): OkHttpClient = client
    fun isPublicAddress(address: InetAddress): Boolean = YoutubeStreamNetworkPolicy.isPublicInternetAddress(address)
}

internal object DesktopProviderDiagnostics {
    private val logger = System.getLogger("Levyra.HighQualityAudio")
    fun d(format: String, vararg values: Any?) {
        if (logger.isLoggable(System.Logger.Level.DEBUG)) {
            logger.log(System.Logger.Level.DEBUG, String.format(Locale.ROOT, format, *values))
        }
    }
}
