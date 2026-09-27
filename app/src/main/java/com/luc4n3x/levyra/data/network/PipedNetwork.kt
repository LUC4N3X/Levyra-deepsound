package com.luc4n3x.levyra.data.network

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal class PipedPublicDns(
    private val delegate: Dns
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname).filter(::isPublicInternetAddress)
        if (addresses.isEmpty()) throw UnknownHostException("Piped host did not resolve to a public address")
        return addresses
    }
}

internal object PipedStreamUrlPolicy {
    fun isValidProxyStreamUrl(url: String, proxyUrl: String): Boolean {
        val stream = url.toHttpUrlOrNull() ?: return false
        val proxy = proxyUrl.toHttpUrlOrNull() ?: return false
        return isSafeHttps(stream) && isSafeHttps(proxy) && stream.host == proxy.host
    }

    fun isSafeHttps(url: HttpUrl): Boolean =
        url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty()
}

internal class PipedSafeRedirectInterceptor(
    private val maxRedirects: Int = 3
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val trustedHost = request.url.host
        repeat(maxRedirects + 1) { redirectCount ->
            if (!PipedStreamUrlPolicy.isSafeHttps(request.url) || request.url.host != trustedHost) {
                throw IOException("Unsafe Piped stream redirect")
            }
            val response = chain.proceed(request)
            if (response.code !in REDIRECT_CODES) return response
            if (redirectCount >= maxRedirects) {
                response.close()
                throw IOException("Too many Piped stream redirects")
            }
            val next = response.header("Location")?.let(request.url::resolve)
            response.close()
            if (next == null || next.host != trustedHost || !PipedStreamUrlPolicy.isSafeHttps(next)) {
                throw IOException("Unsafe Piped stream redirect")
            }
            request = request.newBuilder().url(next).get().build()
        }
        throw IOException("Too many Piped stream redirects")
    }

    private companion object {
        val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
    }
}

internal fun isPublicInternetAddress(address: InetAddress): Boolean {
    if (
        address.isAnyLocalAddress ||
        address.isLoopbackAddress ||
        address.isLinkLocalAddress ||
        address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) return false
    val raw = address.address
    return when (raw.size) {
        4 -> isPublicIpv4(raw)
        16 -> isPublicIpv6(raw)
        else -> false
    }
}

private fun isPublicIpv4(raw: ByteArray): Boolean {
    val first = raw[0].toInt() and 0xff
    val second = raw[1].toInt() and 0xff
    val third = raw[2].toInt() and 0xff
    return when {
        first == 0 || first == 10 || first == 127 || first >= 224 -> false
        first == 100 && second in 64..127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 0 -> false
        first == 192 && second == 168 -> false
        first == 198 && second in 18..19 -> false
        first == 198 && second == 51 && third == 100 -> false
        first == 203 && second == 0 && third == 113 -> false
        else -> true
    }
}

private fun isPublicIpv6(raw: ByteArray): Boolean {
    val first = raw[0].toInt() and 0xff
    val second = raw[1].toInt() and 0xff
    if (raw.take(12).all { it == 0.toByte() }) return false
    if (first and 0xfe == 0xfc) return false
    return !(first == 0x20 && second == 0x01 && raw[2] == 0x0d.toByte() && raw[3] == 0xb8.toByte())
}
