package com.luc4n3x.levyra.data.network.byedpi

import com.luc4n3x.levyra.data.network.LevyraNetworkConfiguration
import com.luc4n3x.levyra.data.network.YoutubeNetworkPolicy
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraProxyMode
import java.io.Closeable
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.net.ssl.ExtendedSSLSession
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ByeDpiRoutingTest {
    private val youtubeHost = "rr1---sn-test.googlevideo.com"
    private val jioSaavnHost = "aac.saavncdn.com"
    private val settings = LevyraNetworkSettings(byeDpiEnabled = true)
    private val loopback: InetAddress = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    private val root = HeldCertificate.Builder().certificateAuthority(0).build()
    private val closeables = mutableListOf<Closeable>()
    private val systemDnsLookups = CopyOnWriteArrayList<String>()
    private val secureDnsLookups = CopyOnWriteArrayList<String>()

    private val brokenSystemDns = Dns { hostname ->
        systemDnsLookups += hostname
        throw UnknownHostException("ISP resolver unavailable for $hostname")
    }

    private val poisonedSystemDns = Dns { hostname ->
        systemDnsLookups += hostname
        listOf(InetAddress.getByAddress(hostname, byteArrayOf(10, 10, 34, 34)))
    }

    private val secureDns = Dns { hostname ->
        secureDnsLookups += hostname
        listOf(loopback)
    }

    @Before
    fun resetState() {
        ByeDpiSupervisor.resetForTesting()
        LevyraNetworkConfiguration.apply(LevyraNetworkSettings(), "")
    }

    @After
    fun tearDown() {
        closeables.asReversed().forEach { runCatching { it.close() } }
        ByeDpiSupervisor.resetForTesting()
    }

    @Test
    fun youtubeTrafficReachesByeDpiAsNumericDestinationWithOriginalSni() {
        val tls = TlsServer(certificateFor(youtubeHost)).also(closeables::add)
        val socks = FakeSocks5Server().also(closeables::add)
        ByeDpiSupervisor.setRunningForTesting(socks.port)

        val body = get(client(brokenSystemDns), youtubeHost, tls.port)

        assertEquals("ok", body)
        val request = socks.requests.single()
        assertEquals(0x01, request.addressType)
        assertArrayEquals(byteArrayOf(127, 0, 0, 1), request.address)
        assertEquals(tls.port, request.port)
        assertEquals(listOf(youtubeHost), tls.requestedServerNames)
        assertEquals(listOf(youtubeHost), secureDnsLookups)
        assertTrue(systemDnsLookups.isEmpty())
    }

    @Test
    fun poisonedSystemDnsIsNeverConsultedForByeDpiRoute() {
        val tls = TlsServer(certificateFor(youtubeHost)).also(closeables::add)
        val socks = FakeSocks5Server().also(closeables::add)
        ByeDpiSupervisor.setRunningForTesting(socks.port)

        assertEquals("ok", get(client(poisonedSystemDns), youtubeHost, tls.port))

        assertTrue(systemDnsLookups.isEmpty())
        assertArrayEquals(byteArrayOf(127, 0, 0, 1), socks.requests.single().address)
    }

    @Test
    fun certificateHostnameVerificationStaysActiveThroughTunnel() {
        val tls = TlsServer(certificateFor("attacker.example")).also(closeables::add)
        val socks = FakeSocks5Server().also(closeables::add)
        ByeDpiSupervisor.setRunningForTesting(socks.port)

        try {
            get(client(brokenSystemDns), youtubeHost, tls.port)
            fail("Certificate issued for another hostname must be rejected")
        } catch (_: SSLPeerUnverifiedException) {
        }

        assertEquals(0x01, socks.requests.single().addressType)
        assertTrue(tls.awaitSni())
        assertEquals(listOf(youtubeHost), tls.requestedServerNames)
    }

    @Test
    fun jioSaavnTrafficNeverEntersTunnelAndKeepsDefaultResolver() {
        val tls = TlsServer(certificateFor(jioSaavnHost)).also(closeables::add)
        val socks = FakeSocks5Server().also(closeables::add)
        ByeDpiSupervisor.setRunningForTesting(socks.port)
        val defaultDns = Dns { hostname ->
            systemDnsLookups += hostname
            listOf(InetAddress.getByAddress(hostname, loopback.address))
        }

        assertEquals("ok", get(client(defaultDns), jioSaavnHost, tls.port))

        assertEquals(0, socks.connections)
        assertTrue(secureDnsLookups.isEmpty())
        assertEquals(listOf(jioSaavnHost), systemDnsLookups)
        assertEquals(listOf(jioSaavnHost), tls.requestedServerNames)
    }

    @Test
    fun stoppedByeDpiFallsBackToDirectConnectionButKeepsSecureResolution() {
        val tls = TlsServer(certificateFor(youtubeHost)).also(closeables::add)
        val socks = FakeSocks5Server().also(closeables::add)

        assertEquals("ok", get(client(brokenSystemDns), youtubeHost, tls.port))

        assertEquals(0, socks.connections)
        assertEquals(listOf(youtubeHost), secureDnsLookups)
        assertTrue(systemDnsLookups.isEmpty())
        assertEquals(listOf(youtubeHost), tls.requestedServerNames)
    }

    @Test
    fun refusedByeDpiPortCountsAgainstCircuitBreaker() {
        val closedPort = ServerSocket(0, 1, loopback).use { it.localPort }
        ByeDpiSupervisor.setRunningForTesting(closedPort)

        repeat(3) {
            ByeDpiRoutingSocket(settings, streamBypass = false).use { socket ->
                try {
                    socket.connect(youtubeDestination(), 1_000)
                    fail("Connecting to a closed ByeDPI port must fail")
                } catch (_: IOException) {
                }
            }
        }

        assertTrue(ByeDpiSupervisor.isTemporarilyDegraded())
    }

    @Test
    fun socksRejectionOfRemoteDestinationDoesNotTripCircuitBreaker() {
        val socks = FakeSocks5Server(replyCode = 0x04).also(closeables::add)
        ByeDpiSupervisor.setRunningForTesting(socks.port)

        repeat(4) {
            ByeDpiRoutingSocket(settings, streamBypass = false).use { socket ->
                try {
                    socket.connect(youtubeDestination(), 1_000)
                    fail("Rejected SOCKS5 connect must fail")
                } catch (rejected: Socks5ReplyException) {
                    assertEquals(0x04, rejected.replyCode)
                }
            }
        }

        assertFalse(ByeDpiSupervisor.isTemporarilyDegraded())
    }

    @Test
    fun byeDpiDnsKeepsHostnameOnResolvedAddressWithoutReverseLookup() {
        val dns = ByeDpiDns(settings, streamBypass = false, defaultDns = brokenSystemDns, secureResolver = secureDns)

        val resolved = dns.lookup(youtubeHost).single()

        assertArrayEquals(loopback.address, resolved.address)
        assertEquals(youtubeHost, InetSocketAddress(resolved, 443).hostString)
        assertTrue(systemDnsLookups.isEmpty())
    }

    @Test
    fun ipv6AnswersAreDroppedWithoutGlobalIpv6UnlessNoIpv4Remains() {
        val ipv4 = byteArrayOf(142.toByte(), 250.toByte(), 1, 2)
        val ipv6 = ByteArray(16) { index -> if (index == 0) 0x2a.toByte() else 1 }
        val dualStack = Dns { hostname -> listOf(InetAddress.getByAddress(hostname, ipv6), InetAddress.getByAddress(hostname, ipv4)) }
        val ipv6Only = Dns { hostname -> listOf(InetAddress.getByAddress(hostname, ipv6)) }

        val withoutIpv6 = ByeDpiDns(settings, false, brokenSystemDns, dualStack, hasGlobalIpv6 = { false })
        assertEquals(listOf(ipv4.toList()), withoutIpv6.lookup(youtubeHost).map { it.address.toList() })

        val withIpv6 = ByeDpiDns(settings, false, brokenSystemDns, dualStack, hasGlobalIpv6 = { true })
        assertEquals(2, withIpv6.lookup(youtubeHost).size)

        val onlyIpv6 = ByeDpiDns(settings, false, brokenSystemDns, ipv6Only, hasGlobalIpv6 = { false })
        assertEquals(listOf(ipv6.toList()), onlyIpv6.lookup(youtubeHost).map { it.address.toList() })
    }

    @Test
    fun byeDpiDnsDelegatesEverythingWhenFeatureIsDisabled() {
        val defaultDns = Dns { hostname ->
            systemDnsLookups += hostname
            listOf(loopback)
        }
        val dns = ByeDpiDns(LevyraNetworkSettings(byeDpiEnabled = false), false, defaultDns, secureDns)

        dns.lookup(youtubeHost)

        assertEquals(listOf(youtubeHost), systemDnsLookups)
        assertTrue(secureDnsLookups.isEmpty())
    }

    @Test
    fun concurrentTunnelsToSameDestinationAreAllAudited() {
        val destination = InetSocketAddress(InetAddress.getByAddress(youtubeHost, loopback.address), 44_301)

        ByeDpiRouteTrace.mark(destination, 1080)
        ByeDpiRouteTrace.mark(InetSocketAddress(InetAddress.getByAddress(youtubeHost, loopback.address), 44_301), 1080)

        assertEquals(1080, ByeDpiRouteTrace.consume(destination))
        assertEquals(1080, ByeDpiRouteTrace.consume(destination))
        assertEquals(null, ByeDpiRouteTrace.consume(destination))
    }

    @Test
    fun connectRequestEncodesNumericAddressTypesOnly() {
        val ipv4 = Socks5Handshake.connectRequest(InetAddress.getByAddress(byteArrayOf(142.toByte(), 250.toByte(), 1, 2)), 443)
        assertArrayEquals(byteArrayOf(5, 1, 0, 1, 142.toByte(), 250.toByte(), 1, 2, 0x01, 0xbb.toByte()), ipv4)

        val ipv6Raw = ByteArray(16) { index -> if (index == 0) 0x2a.toByte() else index.toByte() }
        val ipv6 = Socks5Handshake.connectRequest(InetAddress.getByAddress(ipv6Raw), 8443)
        assertEquals(0x04, ipv6[3].toInt())
        assertArrayEquals(ipv6Raw, ipv6.copyOfRange(4, 20))
        assertEquals(8443, ((ipv6[20].toInt() and 0xff) shl 8) or (ipv6[21].toInt() and 0xff))
    }

    @Test
    fun routingDecisionHonoursExternalProxyAndStreamBypass() {
        val withProxy = LevyraNetworkSettings(
            byeDpiEnabled = true,
            proxyMode = LevyraProxyMode.Http,
            proxyHost = "192.168.1.100",
            proxyPort = 8080,
            bypassProxyForStreams = true
        )

        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi(youtubeHost, withProxy, streamBypass = true))
        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi(youtubeHost, withProxy, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi("music.youtube.com", withProxy, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(jioSaavnHost, withProxy, streamBypass = true))
    }

    private fun youtubeDestination() = InetSocketAddress(InetAddress.getByAddress(youtubeHost, loopback.address), 443)

    private fun certificateFor(hostname: String): HeldCertificate =
        HeldCertificate.Builder()
            .addSubjectAlternativeName(hostname)
            .signedBy(root)
            .build()

    private fun client(defaultDns: Dns): OkHttpClient {
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(root.certificate).build()
        return OkHttpClient.Builder()
            .dns(ByeDpiDns(settings, streamBypass = false, defaultDns = defaultDns, secureResolver = secureDns))
            .socketFactory(ByeDpiSocketFactory(settings, streamBypass = false))
            .proxySelector(YoutubeNetworkPolicy.createProxySelector(settings))
            .sslSocketFactory(trust.sslSocketFactory(), trust.trustManager)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    private fun get(client: OkHttpClient, host: String, port: Int): String {
        val request = Request.Builder().url("https://$host:$port/videoplayback").build()
        return client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            response.body.string()
        }
    }

    private inner class TlsServer(certificate: HeldCertificate) : Closeable {
        val requestedServerNames = CopyOnWriteArrayList<String>()
        private val sniObserved = CountDownLatch(1)
        private val serverSocket: SSLServerSocket
        val port: Int get() = serverSocket.localPort

        init {
            val handshake = HandshakeCertificates.Builder()
                .heldCertificate(certificate, root.certificate)
                .build()
            serverSocket = handshake.sslContext().serverSocketFactory.createServerSocket(0, 16, loopback) as SSLServerSocket
            thread(isDaemon = true, name = "tls-test-server") {
                while (!serverSocket.isClosed) {
                    val accepted = try {
                        serverSocket.accept() as SSLSocket
                    } catch (_: IOException) {
                        break
                    }
                    thread(isDaemon = true) { serve(accepted) }
                }
            }
        }

        private fun serve(socket: SSLSocket) {
            socket.use {
                try {
                    it.startHandshake()
                    (it.session as ExtendedSSLSession).requestedServerNames
                        .filterIsInstance<SNIHostName>()
                        .forEach { name -> requestedServerNames += name.asciiName }
                    sniObserved.countDown()
                    val input = it.inputStream.bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) Unit
                    it.outputStream.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray())
                    it.outputStream.flush()
                } catch (_: IOException) {
                }
            }
        }

        fun awaitSni(): Boolean = sniObserved.await(2, TimeUnit.SECONDS)

        override fun close() = serverSocket.close()
    }

    private class SocksRequest(val addressType: Int, val address: ByteArray, val port: Int)

    private inner class FakeSocks5Server(private val replyCode: Int = 0x00) : Closeable {
        val requests = CopyOnWriteArrayList<SocksRequest>()

        @Volatile
        var connections = 0
            private set
        private val serverSocket = ServerSocket(0, 16, loopback)
        val port: Int get() = serverSocket.localPort

        init {
            thread(isDaemon = true, name = "socks-test-server") {
                while (!serverSocket.isClosed) {
                    val client = try {
                        serverSocket.accept()
                    } catch (_: IOException) {
                        break
                    }
                    connections++
                    thread(isDaemon = true) { handle(client) }
                }
            }
        }

        private fun handle(client: Socket) {
            try {
                val input = DataInputStream(client.getInputStream())
                val output = client.getOutputStream()
                input.readUnsignedByte()
                input.readFully(ByteArray(input.readUnsignedByte()))
                output.write(byteArrayOf(5, 0))
                val header = ByteArray(4).also(input::readFully)
                val addressType = header[3].toInt()
                val address = when (addressType) {
                    0x01 -> ByteArray(4)
                    0x04 -> ByteArray(16)
                    else -> ByteArray(input.readUnsignedByte())
                }.also(input::readFully)
                val port = input.readUnsignedShort()
                requests += SocksRequest(addressType, address, port)
                if (replyCode != 0x00 || addressType == 0x03) {
                    output.write(byteArrayOf(5, replyCode.coerceAtLeast(1).toByte(), 0, 1, 0, 0, 0, 0, 0, 0))
                    client.close()
                    return
                }
                val upstream = Socket(InetAddress.getByAddress(address), port)
                output.write(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0))
                output.flush()
                pipe(client.getInputStream(), upstream.getOutputStream(), client, upstream)
                pipe(upstream.getInputStream(), output, client, upstream)
            } catch (_: IOException) {
                client.close()
            }
        }

        private fun pipe(from: InputStream, to: OutputStream, first: Socket, second: Socket) {
            thread(isDaemon = true) {
                try {
                    from.copyTo(to)
                } catch (_: IOException) {
                } finally {
                    runCatching { first.close() }
                    runCatching { second.close() }
                }
            }
        }

        override fun close() = serverSocket.close()
    }
}