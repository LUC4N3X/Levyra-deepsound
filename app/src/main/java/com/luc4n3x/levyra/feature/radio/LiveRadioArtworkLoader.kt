package com.luc4n3x.levyra.feature.radio

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.io.IOException
import okhttp3.OkHttpClient

internal object LiveRadioArtworkLoader {
    @Volatile
    private var instance: ImageLoader? = null

    val client: OkHttpClient by lazy {
        LevyraHttpClientFactory.externalIntegrations().newBuilder()
            .dns(RadioUrlPolicy.publicDns)
            .followRedirects(true)
            .followSslRedirects(true)
            .addNetworkInterceptor { chain ->
                if (!RadioUrlPolicy.isAllowed(chain.request().url.toString())) {
                    throw IOException("Blocked unsafe live radio artwork URL")
                }
                chain.proceed(chain.request())
            }
            .build()
    }

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }
    }

    private fun build(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(client))
            }
            .crossfade(true)
            .build()
    }
}
