package com.luc4n3x.levyra.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.luc4n3x.levyra.nexus.network.LevyraAddressFamily
import com.luc4n3x.levyra.nexus.network.LevyraRoute
import com.luc4n3x.levyra.nexus.network.LevyraRouteEngine
import com.luc4n3x.levyra.nexus.network.LevyraRouteFailure
import com.luc4n3x.levyra.nexus.network.LevyraTransport
import com.luc4n3x.levyra.runtime.RuntimeHooks
import com.luc4n3x.levyra.runtime.RuntimeSignal
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLException
import okhttp3.Call
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Protocol
import okhttp3.Response

internal object LevyraNetworkIntelligence {
    private val routeEngine = LevyraRouteEngine()
    private val initialized = AtomicBoolean(false)
    private val networkSignature = AtomicReference("")

    val dns: Dns = Dns { hostname ->
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.size <= 1) return@Dns addresses
        val routes = addresses.mapIndexed { index, address -> route(hostname, address, index) }
        val preferred = routeEngine.race(routes, maxRoutes = 2)
        val remaining = routeEngine.order(routes).filterNot(preferred::contains)
        val ordered = (preferred + remaining).mapNotNull { selected ->
            addresses.firstOrNull { address -> address.hostAddress == selected.id }
        }
        if (ordered.size == addresses.size) ordered else addresses
    }

    val eventListenerFactory: EventListener.Factory = EventListener.Factory { AdaptiveEventListener() }

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: run {
            initialized.set(false)
            return
        }
        runCatching {
            connectivity.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        refreshNetworkSignature(connectivity, network)
                    }

                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        refreshNetworkSignature(network, capabilities)
                    }

                    override fun onLost(network: Network) {
                        val previous = networkSignature.get()
                        if (previous.startsWith("${network.hashCode()}|") && networkSignature.compareAndSet(previous, "")) {
                            routeEngine.resetVolatileState()
                        }
                    }
                }
            )
        }.onFailure {
            initialized.set(false)
        }
    }

    internal fun diagnostics() = routeEngine.snapshot()

    private fun refreshNetworkSignature(connectivity: ConnectivityManager, network: Network) {
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return
        refreshNetworkSignature(network, capabilities)
    }

    private fun refreshNetworkSignature(network: Network, capabilities: NetworkCapabilities) {
        val transports = buildList {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("wifi")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("cellular")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ethernet")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("vpn")
        }.joinToString(",")
        val signature = buildString {
            append(network.hashCode())
            append('|').append(transports)
            append('|').append(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            append('|').append(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
        }
        val previous = networkSignature.getAndSet(signature)
        if (previous.isNotBlank() && previous != signature) routeEngine.resetVolatileState()
    }

    private fun route(host: String, address: InetAddress, priority: Int = 0): LevyraRoute = LevyraRoute(
        id = address.hostAddress.orEmpty(),
        host = host,
        transport = LevyraTransport.OKHTTP,
        addressFamily = when (address) {
            is Inet4Address -> LevyraAddressFamily.IPV4
            is Inet6Address -> LevyraAddressFamily.IPV6
            else -> LevyraAddressFamily.SYSTEM
        },
        priority = priority
    )

    private class AdaptiveEventListener : EventListener() {
        private val connectStartedAt = ConcurrentHashMap<String, Long>()
        private val connectAttempts = AtomicInteger(0)
        private val responseCount = AtomicInteger(0)
        @Volatile private var callStartedAtNanos = 0L

        override fun callStart(call: Call) {
            callStartedAtNanos = System.nanoTime()
        }

        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
            connectAttempts.incrementAndGet()
            connectStartedAt[key(inetSocketAddress)] = System.nanoTime()
        }

        override fun connectEnd(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?
        ) {
            val address = inetSocketAddress.address ?: return
            val latencyMs = elapsedMs(inetSocketAddress)
            routeEngine.recordSuccess(
                route = route(call.request().url.host, address),
                latencyMs = latencyMs
            )
            RuntimeHooks.network(
                host = call.request().url.host,
                category = RuntimeSignal.NETWORK_CONNECT,
                latencyMs = latencyMs,
                outcome = RuntimeSignal.OUTCOME_SUCCESS,
                retry = (connectAttempts.get() - 1).coerceAtLeast(0)
            )
        }

        override fun connectFailed(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
            ioe: IOException
        ) {
            val address = inetSocketAddress.address ?: return
            val latencyMs = elapsedMs(inetSocketAddress)
            routeEngine.recordFailure(
                route = route(call.request().url.host, address),
                failure = when (ioe) {
                    is SocketTimeoutException -> LevyraRouteFailure.TIMEOUT
                    is SSLException -> LevyraRouteFailure.TLS
                    is UnknownHostException -> LevyraRouteFailure.CONNECTION
                    else -> LevyraRouteFailure.CONNECTION
                },
                latencyMs = latencyMs
            )
            RuntimeHooks.network(
                host = call.request().url.host,
                category = RuntimeSignal.NETWORK_CONNECT,
                latencyMs = latencyMs,
                outcome = if (ioe is SocketTimeoutException) RuntimeSignal.OUTCOME_TIMEOUT else RuntimeSignal.OUTCOME_FAILURE,
                retry = (connectAttempts.get() - 1).coerceAtLeast(0),
                failure = if (ioe is SocketTimeoutException) RuntimeSignal.FAILURE_TIMEOUT else RuntimeSignal.FAILURE_NETWORK
            )
        }

        override fun responseHeadersEnd(call: Call, response: Response) {
            val count = responseCount.incrementAndGet()
            RuntimeHooks.network(
                host = call.request().url.host,
                category = RuntimeSignal.NETWORK_HTTP,
                latencyMs = callElapsedMs(),
                outcome = if (response.isSuccessful || response.isRedirect) {
                    RuntimeSignal.OUTCOME_SUCCESS
                } else {
                    RuntimeSignal.OUTCOME_FAILURE
                },
                statusCode = response.code,
                retry = (connectAttempts.get() - 1).coerceAtLeast(0),
                redirects = (count - 1).coerceAtLeast(0)
            )
        }

        override fun callEnd(call: Call) {
            connectStartedAt.clear()
        }

        override fun callFailed(call: Call, ioe: IOException) {
            RuntimeHooks.network(
                host = call.request().url.host,
                category = RuntimeSignal.NETWORK_HTTP,
                latencyMs = callElapsedMs(),
                outcome = if (ioe is SocketTimeoutException) RuntimeSignal.OUTCOME_TIMEOUT else RuntimeSignal.OUTCOME_FAILURE,
                retry = (connectAttempts.get() - 1).coerceAtLeast(0),
                redirects = (responseCount.get() - 1).coerceAtLeast(0),
                failure = if (ioe is SocketTimeoutException) RuntimeSignal.FAILURE_TIMEOUT else RuntimeSignal.FAILURE_NETWORK
            )
            connectStartedAt.clear()
        }

        private fun elapsedMs(address: InetSocketAddress): Long {
            val startedAt = connectStartedAt.remove(key(address)) ?: return 1L
            return ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)
        }

        private fun callElapsedMs(): Long {
            val startedAt = callStartedAtNanos
            if (startedAt <= 0L) return 1L
            return ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)
        }

        private fun key(address: InetSocketAddress): String =
            "${address.address?.hostAddress.orEmpty()}:${address.port}"
    }
}
