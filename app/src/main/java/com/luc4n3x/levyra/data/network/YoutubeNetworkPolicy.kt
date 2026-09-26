package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiSupervisor
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.Locale
import timber.log.Timber

object YoutubeNetworkPolicy {
    private val YOUTUBE_HOST_SUFFIXES = listOf(
        "youtube.com",
        "googlevideo.com",
        "youtubei.googleapis.com",
        "ytimg.com",
        "youtube-nocookie.com",
        "youtu.be",
        "suggestqueries.google.com"
    )

    private val JIOSAAVN_HOST_SUFFIXES = listOf(
        "jiosaavn.com",
        "saavncdn.com"
    )

    fun isYoutubeHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val clean = host.lowercase(Locale.ROOT).trimEnd('.')
        return YOUTUBE_HOST_SUFFIXES.any { suffix -> clean == suffix || clean.endsWith(".$suffix") }
    }

    fun isYoutubeMediaHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val clean = host.lowercase(Locale.ROOT).trimEnd('.')
        return clean == "googlevideo.com" || clean.endsWith(".googlevideo.com")
    }

    fun isJioSaavnHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val clean = host.lowercase(Locale.ROOT).trimEnd('.')
        return JIOSAAVN_HOST_SUFFIXES.any { suffix -> clean == suffix || clean.endsWith(".$suffix") }
    }

    fun selectProxies(uri: URI?, settings: LevyraNetworkSettings): List<Proxy> {
        if (uri == null) return listOf(Proxy.NO_PROXY)
        val host = uri.host.orEmpty().lowercase(Locale.ROOT).trimEnd('.')

        return when {
            isJioSaavnHost(host) -> selectJioSaavnProxies(settings)
            isYoutubeHost(host) -> selectYoutubeProxies(host, settings)
            else -> selectDefaultProxies(settings)
        }
    }

    private fun selectJioSaavnProxies(settings: LevyraNetworkSettings): List<Proxy> {
        val externalProxy = LevyraNetworkConfiguration.proxy()
        return if (settings.usesProxy && externalProxy != null && !settings.bypassProxyForStreams) {
            listOf(externalProxy)
        } else {
            listOf(Proxy.NO_PROXY)
        }
    }

    private fun selectYoutubeProxies(host: String, settings: LevyraNetworkSettings): List<Proxy> {
        val isMedia = isYoutubeMediaHost(host)
        if (settings.usesProxy) {
            val externalProxy = LevyraNetworkConfiguration.proxy()
            if (isMedia && settings.bypassProxyForStreams) {
                return resolveActiveByeDpiProxy(settings) ?: listOf(Proxy.NO_PROXY)
            }
            return if (externalProxy != null) listOf(externalProxy) else listOf(Proxy.NO_PROXY)
        }
        return resolveActiveByeDpiProxy(settings) ?: listOf(Proxy.NO_PROXY)
    }

    private fun selectDefaultProxies(settings: LevyraNetworkSettings): List<Proxy> {
        if (settings.usesProxy) {
            val externalProxy = LevyraNetworkConfiguration.proxy()
            if (externalProxy != null) return listOf(externalProxy)
        }
        return listOf(Proxy.NO_PROXY)
    }

    private fun resolveActiveByeDpiProxy(settings: LevyraNetworkSettings): List<Proxy>? {
        if (settings.byeDpiEnabled && ByeDpiSupervisor.isRunning() && !ByeDpiSupervisor.isTemporarilyDegraded()) {
            val byeDpi = ByeDpiSupervisor.proxy()
            if (byeDpi != null) return listOf(byeDpi)
        }
        return null
    }

    fun createProxySelector(settings: LevyraNetworkSettings): ProxySelector {
        return object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> {
                return selectProxies(uri, settings)
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                val byeDpi = ByeDpiSupervisor.proxy()
                val byeAddr = byeDpi?.address() as? InetSocketAddress
                val failedAddr = sa as? InetSocketAddress
                if (byeAddr != null && failedAddr != null && failedAddr.port == byeAddr.port) {
                    Timber.w(ioe, "Failed connecting to ByeDPI SOCKS on %s for %s", sa, uri)
                    ByeDpiSupervisor.recordConnectionFailure()
                }
            }
        }
    }
}
