package com.luc4n3x.levyra.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.luc4n3x.levyra.data.hqaudio.ConfigurableProviderPlaybackInterceptor
import com.luc4n3x.levyra.data.hqaudio.ConfigurableProviderPlaybackPolicy
import com.luc4n3x.levyra.data.hqaudio.PublicProviderDns
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.OkHttpClient

@UnstableApi
object PlaybackNetworkStack {
    private const val USER_AGENT = "LevyraPlayer/1.13 Android Music"

    fun initialize(context: Context) {
        LevyraHttpClientFactory.media(context.applicationContext)
    }

    fun playbackFactory(context: Context): HttpDataSource.Factory =
        PolicyAwareOkHttpFactory(context.applicationContext)

    fun warmupFactory(context: Context): HttpDataSource.Factory =
        PolicyAwareOkHttpFactory(context.applicationContext)

    private class PolicyAwareOkHttpFactory(context: Context) : HttpDataSource.Factory {
        private val baseClient = LevyraHttpClientFactory.streaming(context)
        private val defaultRequestProperties = HttpDataSource.RequestProperties()

        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): HttpDataSource.Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        override fun createDataSource(): HttpDataSource =
            PolicyAwareOkHttpDataSource(baseClient, defaultRequestProperties)
    }

    private class PolicyAwareOkHttpDataSource(
        private val baseClient: OkHttpClient,
        private val defaultRequestProperties: HttpDataSource.RequestProperties
    ) : HttpDataSource {
        private val requestProperties = HttpDataSource.RequestProperties()
        private val transferListeners = CopyOnWriteArrayList<TransferListener>()
        private var activeSource: HttpDataSource? = null

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
            val allowedHosts = ConfigurableProviderPlaybackPolicy.allowedHostsFor(dataSpec.uri.toString())
            val client = if (allowedHosts != null) {
                baseClient.newBuilder()
                    .dns(PublicProviderDns)
                    .addNetworkInterceptor(ConfigurableProviderPlaybackInterceptor(allowedHosts))
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            } else {
                baseClient
            }
            val factory = OkHttpDataSource.Factory(client)
                .setUserAgent(USER_AGENT)
                .setDefaultRequestProperties(defaultRequestProperties.getSnapshot())
            val source = factory.createDataSource()
            requestProperties.getSnapshot().forEach { (name, value) -> source.setRequestProperty(name, value) }
            transferListeners.forEach(source::addTransferListener)
            activeSource = source
            return source.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            checkNotNull(activeSource) { "Data source is not open" }.read(buffer, offset, length)

        override fun getUri(): Uri? = activeSource?.getUri()

        override fun getResponseHeaders(): Map<String, List<String>> = activeSource?.getResponseHeaders().orEmpty()

        override fun getResponseCode(): Int = activeSource?.getResponseCode() ?: -1

        override fun close() {
            val source = activeSource
            activeSource = null
            source?.close()
        }
    }
}
