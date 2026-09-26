package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.Locale

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

    private val DIRECT = listOf(Proxy.NO_PROXY)

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

    fun routesThroughByeDpi(host: String?, settings: LevyraNetworkSettings, streamBypass: Boolean): Boolean {
        if (!settings.byeDpiEnabled || !isYoutubeHost(host)) return false
        if (streamBypass || LevyraNetworkConfiguration.proxyFor(settings) == null) return true
        return settings.bypassProxyForStreams && isYoutubeMediaHost(host)
    }

    fun selectProxies(uri: URI?, settings: LevyraNetworkSettings): List<Proxy> {
        if (uri == null) return DIRECT
        val externalProxy = LevyraNetworkConfiguration.proxyFor(settings) ?: return DIRECT
        val mediaBypassesExternalProxy = settings.bypassProxyForStreams && isYoutubeMediaHost(uri.host)
        return if (mediaBypassesExternalProxy) DIRECT else listOf(externalProxy)
    }

    fun createProxySelector(settings: LevyraNetworkSettings): ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> = selectProxies(uri, settings)

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) = Unit
    }
}
