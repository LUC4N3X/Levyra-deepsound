package com.luc4n3x.levyra.data.network.byedpi

import com.luc4n3x.levyra.data.network.LevyraNetworkIntelligence
import com.luc4n3x.levyra.data.network.YoutubeNetworkPolicy
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProtocolException
import java.net.Socket
import java.net.SocketAddress
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import okhttp3.Dns
import timber.log.Timber

internal class ByeDpiDns(
    private val settings: LevyraNetworkSettings,
    private val streamBypass: Boolean,
    private val defaultDns: Dns,
    private val secureResolver: Dns,
    private val hasGlobalIpv6: () -> Boolean = { LevyraNetworkIntelligence.activeNetworkHasIpv6Route() ?: true }
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (!YoutubeNetworkPolicy.routesThroughByeDpi(hostname, settings, streamBypass)) {
            return defaultDns.lookup(hostname)
        }
        val resolved = secureResolver.lookup(hostname)
        val usable = if (resolved.all { it is Inet4Address } || hasGlobalIpv6()) {
            resolved
        } else {
            resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved }
        }
        return usable.map { address -> InetAddress.getByAddress(hostname, address.address) }
    }
}

internal class ByeDpiSecureResolver(
    private val resolvers: List<Pair<String, Dns>>,
    private val fallback: Dns,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val clockMs: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) }
) : Dns {
    private class Answer(val addresses: List<InetAddress>, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, Answer>()

    override fun lookup(hostname: String): List<InetAddress> {
        val key = hostname.lowercase(Locale.ROOT)
        val now = clockMs()
        cache[key]?.takeIf { it.expiresAtMs > now }?.let { return it.addresses }
        val addresses = resolvers.firstNotNullOfOrNull { (name, resolver) -> query(name, resolver, hostname) }
        if (addresses == null) {
            Timber.w("[RouteAudit] dns: host=%s resolver=system-fallback", hostname)
            return fallback.lookup(hostname)
        }
        if (cache.size >= MAX_CACHED_HOSTS) cache.clear()
        cache[key] = Answer(addresses, now + ttlMs)
        return addresses
    }

    private fun query(name: String, resolver: Dns, hostname: String): List<InetAddress>? {
        val addresses = try {
            resolver.lookup(hostname).filter(::isPublicUnicast)
        } catch (failure: IOException) {
            Timber.w("[RouteAudit] dns: host=%s resolver=%s error=%s", hostname, name, failure.javaClass.simpleName)
            return null
        }
        if (addresses.isEmpty()) return null
        Timber.i("[RouteAudit] dns: host=%s resolver=%s answers=%d", hostname, name, addresses.size)
        return addresses
    }

    private companion object {
        const val DEFAULT_TTL_MS = 60_000L
        const val MAX_CACHED_HOSTS = 128

        fun isPublicUnicast(address: InetAddress): Boolean {
            if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress) return false
            if (address.isSiteLocalAddress || address.isMulticastAddress) return false
            val raw = address.address
            val first = raw[0].toInt() and 0xff
            val second = raw[1].toInt() and 0xff
            return when (raw.size) {
                4 -> !(first == 100 && second in 64..127) && first != 0
                else -> first and 0xfe != 0xfc
            }
        }
    }
}

internal class ByeDpiSocketFactory(
    private val settings: LevyraNetworkSettings,
    private val streamBypass: Boolean
) : SocketFactory() {
    override fun createSocket(): Socket = ByeDpiRoutingSocket(settings, streamBypass)

    override fun createSocket(host: String?, port: Int): Socket =
        getDefault().createSocket(host, port)

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        getDefault().createSocket(host, port, localHost, localPort)

    override fun createSocket(host: InetAddress?, port: Int): Socket =
        getDefault().createSocket(host, port)

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        getDefault().createSocket(address, port, localAddress, localPort)
}

internal class ByeDpiRoutingSocket(
    private val settings: LevyraNetworkSettings,
    private val streamBypass: Boolean
) : Socket() {
    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        val target = endpoint as? InetSocketAddress
        val tunnel = target
            ?.takeIf { !it.isUnresolved && YoutubeNetworkPolicy.routesThroughByeDpi(it.hostString, settings, streamBypass) }
            ?.let { ByeDpiSupervisor.acquireTunnel() }
        if (target == null || tunnel == null) {
            super.connect(endpoint, timeout)
            return
        }
        ByeDpiRouteTrace.mark(target, tunnel.port)
        try {
            super.connect(tunnel, timeout)
        } catch (failure: IOException) {
            recordFailureUnlessCanceled()
            throw failure
        }
        val previousTimeout = soTimeout
        try {
            soTimeout = if (timeout > 0) timeout else HANDSHAKE_TIMEOUT_MS
            Socks5Handshake.connect(getInputStream(), getOutputStream(), target)
            soTimeout = previousTimeout
        } catch (rejected: Socks5ReplyException) {
            closeQuietly()
            throw rejected
        } catch (failure: IOException) {
            recordFailureUnlessCanceled()
            closeQuietly()
            throw failure
        }
        ByeDpiSupervisor.recordConnectionSuccess()
        Timber.i(
            "[RouteAudit] byedpi: host=%s dest=%s:%d atyp=%s via=127.0.0.1:%d",
            target.hostString,
            target.address.hostAddress,
            target.port,
            if (target.address.address.size == IPV4_LENGTH) "IPv4" else "IPv6",
            tunnel.port
        )
    }

    private fun recordFailureUnlessCanceled() {
        if (!isClosed) ByeDpiSupervisor.recordConnectionFailure()
    }

    private fun closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
        }
    }

    private companion object {
        const val HANDSHAKE_TIMEOUT_MS = 10_000
        const val IPV4_LENGTH = 4
    }
}

internal class Socks5ReplyException(val replyCode: Int) :
    ConnectException("ByeDPI SOCKS5 connect rejected with reply $replyCode")

internal object Socks5Handshake {
    private const val VERSION = 0x05
    private const val NO_AUTHENTICATION = 0x00
    private const val CONNECT = 0x01
    private const val ATYP_IPV4 = 0x01
    private const val ATYP_DOMAIN = 0x03
    private const val ATYP_IPV6 = 0x04
    private const val SUCCEEDED = 0x00

    fun connect(input: InputStream, output: OutputStream, target: InetSocketAddress) {
        val address = target.address ?: throw ProtocolException("ByeDPI SOCKS5 destination must be resolved")
        output.write(byteArrayOf(VERSION.toByte(), 1, NO_AUTHENTICATION.toByte()))
        output.flush()
        val method = readFully(input, 2)
        if (method[0].toInt() != VERSION || method[1].toInt() != NO_AUTHENTICATION) {
            throw ProtocolException("ByeDPI SOCKS5 method negotiation failed")
        }
        output.write(connectRequest(address, target.port))
        output.flush()
        val header = readFully(input, 4)
        if (header[0].toInt() != VERSION) throw ProtocolException("ByeDPI SOCKS5 reply version mismatch")
        val reply = header[1].toInt() and 0xff
        if (reply != SUCCEEDED) throw Socks5ReplyException(reply)
        val boundLength = when (header[3].toInt()) {
            ATYP_IPV4 -> 4
            ATYP_IPV6 -> 16
            ATYP_DOMAIN -> readFully(input, 1)[0].toInt() and 0xff
            else -> throw ProtocolException("ByeDPI SOCKS5 reply address type unsupported")
        }
        readFully(input, boundLength + 2)
    }

    internal fun connectRequest(address: InetAddress, port: Int): ByteArray {
        val raw = address.address
        return ByteArrayOutputStream(raw.size + 6).apply {
            write(VERSION)
            write(CONNECT)
            write(0)
            write(if (raw.size == 4) ATYP_IPV4 else ATYP_IPV6)
            write(raw)
            write(port ushr 8 and 0xff)
            write(port and 0xff)
        }.toByteArray()
    }

    private fun readFully(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read < 0) throw EOFException("ByeDPI SOCKS5 stream closed during handshake")
            offset += read
        }
        return buffer
    }
}

internal object ByeDpiRouteTrace {
    private const val MAX_PENDING = 256

    private class Pending(val tunnelPort: Int, val count: Int)

    private val tunneled = ConcurrentHashMap<InetSocketAddress, Pending>()

    fun mark(destination: InetSocketAddress, tunnelPort: Int) {
        if (tunneled.size >= MAX_PENDING) tunneled.clear()
        tunneled.compute(destination) { _, pending -> Pending(tunnelPort, (pending?.count ?: 0) + 1) }
    }

    fun consume(destination: InetSocketAddress): Int? {
        var tunnelPort: Int? = null
        tunneled.computeIfPresent(destination) { _, pending ->
            tunnelPort = pending.tunnelPort
            if (pending.count > 1) Pending(pending.tunnelPort, pending.count - 1) else null
        }
        return tunnelPort
    }
}
