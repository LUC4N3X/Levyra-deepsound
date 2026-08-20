package com.luc4n3x.levyra.desktop.core.stream

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import okhttp3.Dns
import okhttp3.HttpUrl

/** Network boundary shared by the resolver probe and the local VLC bridge. */
object YoutubeStreamNetworkPolicy {
    fun isAllowedUrl(url: HttpUrl): Boolean {
        val host = url.host.lowercase(Locale.ROOT)
        return url.scheme == "https" &&
            url.username.isEmpty() &&
            url.password.isEmpty() &&
            url.port == 443 &&
            (host == "googlevideo.com" || host.endsWith(".googlevideo.com"))
    }

    fun resolveAddresses(host: String): List<InetAddress> = InetAddress.getAllByName(host).toList()

    fun isPublicInternetAddress(address: InetAddress): Boolean {
        if (
            address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) return false
        val bytes = address.address.map(Byte::toInt).map { it and 0xff }
        return when (address) {
            is Inet4Address -> !(
                bytes[0] == 0 ||
                    bytes[0] == 100 && bytes[1] in 64..127 ||
                    bytes[0] == 192 && bytes[1] == 0 && bytes[2] in setOf(0, 2) ||
                    bytes[0] == 198 && bytes[1] in 18..19 ||
                    bytes[0] == 198 && bytes[1] == 51 && bytes[2] == 100 ||
                    bytes[0] == 203 && bytes[1] == 0 && bytes[2] == 113 ||
                    bytes[0] >= 224
                )

            is Inet6Address -> !(
                bytes[0] and 0xfe == 0xfc ||
                    bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x0d && bytes[3] == 0xb8
                )

            else -> false
        }
    }

    fun validatingDns(
        resolver: (String) -> List<InetAddress> = ::resolveAddresses,
        validator: (InetAddress) -> Boolean = ::isPublicInternetAddress
    ): Dns = Dns { hostname ->
        val addresses = resolver(hostname)
        if (addresses.isEmpty() || addresses.any { !validator(it) }) {
            throw UnknownHostException("Rejected YouTube stream destination")
        }
        addresses
    }
}
