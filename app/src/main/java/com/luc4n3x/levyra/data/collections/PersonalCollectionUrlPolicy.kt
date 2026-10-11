package com.luc4n3x.levyra.data.collections

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class CollectionAddressClass { Public, Private, Blocked }

object PersonalCollectionUrlPolicy {
    fun origin(url: HttpUrl): String = "${url.scheme}://${url.host.lowercase(Locale.ROOT)}:${url.port}"

    fun origin(url: String): String? = url.toHttpUrlOrNull()?.let(::origin)

    fun identityOf(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url.trim()
        return parsed.newBuilder()
            .username("")
            .password("")
            .fragment(null)
            .host(parsed.host.lowercase(Locale.ROOT))
            .build()
            .toString()
    }

    fun displayUrl(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return ""
        val port = if (parsed.port == HttpUrl.defaultPort(parsed.scheme)) "" else ":${parsed.port}"
        return "${parsed.host}$port${parsed.encodedPath}".trimEnd('/')
    }

    fun isCleartext(url: String): Boolean = url.toHttpUrlOrNull()?.isHttps == false

    fun classify(address: InetAddress): CollectionAddressClass = when {
        address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isMulticastAddress ->
            CollectionAddressClass.Blocked
        address is Inet4Address && isCarrierGradeNat(address) -> CollectionAddressClass.Private
        address.isSiteLocalAddress -> CollectionAddressClass.Private
        address is Inet6Address && isUniqueLocal(address) -> CollectionAddressClass.Private
        else -> CollectionAddressClass.Public
    }

    fun isLocalNetworkLiteral(host: String): Boolean {
        val clean = host.removePrefix("[").removeSuffix("]").lowercase(Locale.ROOT)
        if (clean.endsWith(".local") || clean.endsWith(".lan") || clean.endsWith(".home.arpa") || !clean.contains('.') && !clean.contains(':')) {
            return true
        }
        if (!looksLikeIpLiteral(clean)) return false
        val address = runCatching { InetAddress.getByName(clean) }.getOrNull() ?: return false
        return classify(address) != CollectionAddressClass.Public
    }

    private fun looksLikeIpLiteral(host: String): Boolean =
        host.contains(':') || host.split('.').let { parts -> parts.size == 4 && parts.all { part -> part.toIntOrNull() in 0..255 } }

    private fun isCarrierGradeNat(address: Inet4Address): Boolean {
        val bytes = address.address
        val first = bytes[0].toInt() and 0xFF
        val second = bytes[1].toInt() and 0xFF
        return first == 100 && second in 64..127
    }

    private fun isUniqueLocal(address: Inet6Address): Boolean = address.address[0].toInt() and 0xFE == 0xFC
}
