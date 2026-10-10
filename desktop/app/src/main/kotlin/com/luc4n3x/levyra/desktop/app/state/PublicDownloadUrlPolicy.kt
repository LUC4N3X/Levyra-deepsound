package com.luc4n3x.levyra.desktop.app.state

import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

internal fun requirePublicDownloadUrl(url: String): String {
    val parsed = url.toHttpUrlOrNull() ?: throw IOException("URL di download non valida")
    validatePublicDownloadUrl(parsed)
    return parsed.toString()
}

internal class PublicAddressDns(private val delegate: Dns) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty() || addresses.any(::isBlockedAddress)) {
            throw UnknownHostException("Destinazione di download non consentita: $hostname")
        }
        return addresses
    }
}

internal object PublicDownloadUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        validatePublicDownloadUrl(chain.request().url)
        return chain.proceed(chain.request())
    }
}

private fun validatePublicDownloadUrl(url: HttpUrl) {
    if (url.scheme != "http" && url.scheme != "https") {
        throw IOException("Protocollo di download non consentito")
    }
    val host = url.host.lowercase(Locale.ROOT)
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) {
        throw IOException("Destinazione di download locale non consentita")
    }
    if (isIpLiteral(host)) {
        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
            ?: throw IOException("Indirizzo di download non valido")
        if (isBlockedAddress(address)) {
            throw IOException("Destinazione di download privata non consentita")
        }
    }
}

private fun isIpLiteral(host: String): Boolean =
    host.contains(':') || host.all { it.isDigit() || it == '.' }

private fun isBlockedAddress(address: InetAddress): Boolean {
    if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
        address.isSiteLocalAddress || address.isMulticastAddress
    ) {
        return true
    }
    return when (address) {
        is Inet4Address -> {
            val bytes = address.address.map { it.toInt() and 0xff }
            (bytes[0] == 100 && bytes[1] in 64..127) ||
                (bytes[0] == 198 && bytes[1] in 18..19) ||
                bytes[0] == 0 || bytes[0] >= 224
        }
        is Inet6Address -> {
            val first = address.address[0].toInt() and 0xff
            (first and 0xfe) == 0xfc || first == 0xff
        }
        else -> true
    }
}
