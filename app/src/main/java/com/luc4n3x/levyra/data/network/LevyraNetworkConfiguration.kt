package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraDnsMode
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraProxyMode
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.dnsoverhttps.DnsOverHttps

internal data class LevyraDnsEndpoint(
    val url: String,
    val bootstrapAddresses: List<String>
)

internal object LevyraDnsCatalog {
    val CLOUDFLARE = LevyraDnsEndpoint(
        url = "https://cloudflare-dns.com/dns-query",
        bootstrapAddresses = listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001")
    )
    val GOOGLE = LevyraDnsEndpoint(
        url = "https://dns.google/dns-query",
        bootstrapAddresses = listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844")
    )
    val ADGUARD = LevyraDnsEndpoint(
        url = "https://dns.adguard-dns.com/dns-query",
        bootstrapAddresses = listOf("94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff")
    )
    val QUAD9 = LevyraDnsEndpoint(
        url = "https://dns.quad9.net/dns-query",
        bootstrapAddresses = listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9")
    )

    fun endpointFor(settings: LevyraNetworkSettings): LevyraDnsEndpoint? = when (settings.dnsMode) {
        LevyraDnsMode.System -> null
        LevyraDnsMode.Cloudflare -> CLOUDFLARE
        LevyraDnsMode.Google -> GOOGLE
        LevyraDnsMode.AdGuard -> ADGUARD
        LevyraDnsMode.Quad9 -> QUAD9
        LevyraDnsMode.Custom -> settings.customDohUrl.trim()
            .takeIf { it.startsWith("https://", ignoreCase = true) }
            ?.let { LevyraDnsEndpoint(it, emptyList()) }
    }
}

private data class ResolvedDnsHolder(
    val dns: Dns,
    val generation: Long
)

internal object LevyraNetworkConfiguration {
    private val generationCounter = AtomicLong(0L)

    @Volatile
    private var settings: LevyraNetworkSettings = LevyraNetworkSettings()

    @Volatile
    private var proxyPassword: String = ""

    @Volatile
    private var resolvedDnsHolder: ResolvedDnsHolder = ResolvedDnsHolder(
        dns = LevyraNetworkIntelligence.dns,
        generation = -1L
    )

    val generation: Long get() = generationCounter.get()

    fun current(): LevyraNetworkSettings = settings

    fun hasProxyPassword(): Boolean = proxyPassword.isNotEmpty()

    @Synchronized
    fun apply(newSettings: LevyraNetworkSettings, newProxyPassword: String) {
        val normalized = newSettings.normalized()
        if (normalized == settings && newProxyPassword == proxyPassword) return
        settings = normalized
        proxyPassword = newProxyPassword
        generationCounter.incrementAndGet()
        resolvedDnsHolder = ResolvedDnsHolder(LevyraNetworkIntelligence.dns, -1L)
        runCatching { dohConnectionPool.evictAll() }
        runCatching { dohDispatcher.cancelAll() }
        LevyraHttpClientFactory.onConfigurationChanged()
    }

    fun dns(): Dns {
        val currentGeneration = generationCounter.get()
        val holder = resolvedDnsHolder
        if (holder.generation == currentGeneration) return holder.dns
        return synchronized(this) {
            val targetGeneration = generationCounter.get()
            val currentHolder = resolvedDnsHolder
            if (currentHolder.generation == targetGeneration) {
                currentHolder.dns
            } else {
                val built = buildDns(settings, proxyPassword)
                resolvedDnsHolder = ResolvedDnsHolder(built, targetGeneration)
                built
            }
        }
    }

    fun proxy(): Proxy? = proxyFor(settings)

    fun proxyFor(target: LevyraNetworkSettings): Proxy? {
        if (!target.usesProxy || target.proxyHost.isBlank()) return null
        val type = when (target.proxyMode) {
            LevyraProxyMode.Http -> Proxy.Type.HTTP
            LevyraProxyMode.Socks -> Proxy.Type.SOCKS
            LevyraProxyMode.Disabled -> return null
        }
        return runCatching {
            Proxy(type, InetSocketAddress.createUnresolved(target.proxyHost, target.proxyPort))
        }.getOrNull()
    }

    fun proxyAuthenticator(): Authenticator? = proxyAuthenticatorFor(settings, proxyPassword)

    fun proxyAuthenticatorFor(target: LevyraNetworkSettings, password: String): Authenticator? {
        if (!target.usesProxy || !target.proxyAuthenticationEnabled) return null
        if (target.proxyUsername.isBlank() || password.isEmpty()) return null
        val credential = Credentials.basic(target.proxyUsername, password)
        return Authenticator { _: Route?, response: Response ->
            if (response.request.header(PROXY_AUTHORIZATION) != null) return@Authenticator null
            if (responseChainLength(response) > MAX_PROXY_AUTH_ATTEMPTS) return@Authenticator null
            response.request.newBuilder().header(PROXY_AUTHORIZATION, credential).build()
        }
    }

    fun buildDns(target: LevyraNetworkSettings, targetProxyPassword: String = proxyPassword): Dns {
        val endpoint = LevyraDnsCatalog.endpointFor(target) ?: return LevyraNetworkIntelligence.dns
        val url = endpoint.url.toHttpUrlOrNull() ?: return LevyraNetworkIntelligence.dns
        if (!url.isHttps) return LevyraNetworkIntelligence.dns
        val bootstrap = endpoint.bootstrapAddresses.mapNotNull { address ->
            runCatching { InetAddress.getByName(address) }.getOrNull()
        }
        val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(DOH_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectionPool(dohConnectionPool)
            .dispatcher(dohDispatcher)
            .apply {
                proxyFor(target)?.let(::proxy)
                proxyAuthenticatorFor(target, targetProxyPassword)?.let(::proxyAuthenticator)
            }
            .build()
        return runCatching {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(url)
                .includeIPv6(true)
                .apply { if (bootstrap.isNotEmpty()) bootstrapDnsHosts(bootstrap) }
                .build()
        }.getOrElse { LevyraNetworkIntelligence.dns }
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    internal val dohConnectionPool = okhttp3.ConnectionPool(4, 5, TimeUnit.MINUTES)
    private val dohDispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 8
        maxRequestsPerHost = 8
    }

    private const val PROXY_AUTHORIZATION = "Proxy-Authorization"
    private const val MAX_PROXY_AUTH_ATTEMPTS = 2
    private const val DOH_TIMEOUT_SECONDS = 6L
    private const val DOH_CALL_TIMEOUT_SECONDS = 10L
}
