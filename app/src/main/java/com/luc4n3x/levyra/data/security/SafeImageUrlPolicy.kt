package com.luc4n3x.levyra.data.security

import java.net.InetAddress
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object SafeImageUrlPolicy {
    const val MAX_IMAGE_PAYLOAD_BYTES = 5L * 1024L * 1024L // 5MB
    const val MAX_REDIRECT_HOPS = 3
    const val DEFAULT_HTTPS_PORT = 443

    private val ALLOWED_IMAGE_MIME_PREFIXES = listOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/avif",
        "image/gif",
        "image/heic",
        "image/heif"
    )

    /**
     * Non-blocking, synchronous syntactic check and normalization.
     * Enforces:
     * - HTTPS scheme only
     * - Standard port (443 or default -1)
     * - No user-info
     * - Non-blank host
     * - Rejection of private, loopback, link-local, multicast, or cloud metadata literal IP addresses.
     *
     * Returns the normalized safe URL string, or empty string if invalid.
     */
    fun sanitize(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        val trimmed = rawUrl.trim()
        val httpUrl = trimmed.toHttpUrlOrNull() ?: return ""
        if (!isSyntacticallyAllowed(httpUrl)) return ""
        return httpUrl.toString()
    }

    fun isSyntacticallyAllowed(url: HttpUrl): Boolean {
        // 1. Scheme must be HTTPS
        if (url.scheme != "https") return false

        // 2. Port must be 443 (standard HTTPS port)
        if (url.port != DEFAULT_HTTPS_PORT) return false

        // 3. Userinfo must be empty
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return false

        // 4. Host validation
        val host = url.host.trim().lowercase(Locale.ROOT)
        if (host.isEmpty() ||
            host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".onion") ||
            host.endsWith(".invalid") ||
            host.endsWith(".test")
        ) {
            return false
        }

        // Check if host is a literal IP address and reject non-public destinations
        if (!isPublicLiteralHost(host)) return false

        return true
    }

    fun isPublicLiteralHost(host: String): Boolean {
        val cleanHost = host.removePrefix("[").removeSuffix("]")
        val literalAddress = runCatching {
            if (isIpLiteral(cleanHost)) InetAddress.getByName(cleanHost) else null
        }.getOrNull()

        if (literalAddress != null) {
            return isPublicAddress(literalAddress)
        }
        return true
    }

    private fun isIpLiteral(host: String): Boolean {
        if (host.contains(':')) return true
        val parts = host.split('.')
        return parts.size == 4 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }
    }

    /**
     * Comprehensive address filter rejecting loopback, private, link-local, site-local, multicast,
     * CGNAT, ULA, documentation, and cloud metadata addresses (IPv4 & IPv6).
     */
    fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }

        val raw = address.address
        if (raw.size == 4) { // IPv4
            val b0 = raw[0].toInt() and 0xFF
            val b1 = raw[1].toInt() and 0xFF
            val b2 = raw[2].toInt() and 0xFF

            // 0.0.0.0/8 (Current network)
            if (b0 == 0) return false
            // 10.0.0.0/8 (Private)
            if (b0 == 10) return false
            // 100.64.0.0/10 (Carrier-Grade NAT)
            if (b0 == 100 && b1 in 64..127) return false
            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) return false
            // 169.254.0.0/16 (Link Local & Cloud metadata e.g. 169.254.169.254)
            if (b0 == 169 && b1 == 254) return false
            // 172.16.0.0/12 (Private)
            if (b0 == 172 && b1 in 16..31) return false
            // 192.0.0.0/24 (IETF Protocol Assignments)
            if (b0 == 192 && b1 == 0 && b2 == 0) return false
            // 192.0.2.0/24 (TEST-NET-1)
            if (b0 == 192 && b1 == 0 && b2 == 2) return false
            // 192.168.0.0/16 (Private)
            if (b0 == 192 && b1 == 168) return false
            // 198.18.0.0/15 (Network benchmark)
            if (b0 == 198 && b1 in 18..19) return false
            // 198.51.100.0/24 (TEST-NET-2)
            if (b0 == 198 && b1 == 51 && b2 == 100) return false
            // 203.0.113.0/24 (TEST-NET-3)
            if (b0 == 203 && b1 == 0 && b2 == 113) return false
            // 224.0.0.0/4 (Multicast) & 240.0.0.0/4 (Reserved) & 255.255.255.255 (Broadcast)
            if (b0 >= 224) return false
        } else if (raw.size == 16) { // IPv6
            val b0 = raw[0].toInt() and 0xFF
            val b1 = raw[1].toInt() and 0xFF

            // IPv6 loopback (::1) and unspecified (::)
            if (address.isLoopbackAddress || address.isAnyLocalAddress) return false
            // Unique Local Address (ULA) fc00::/7 (fc00... and fd00...)
            if ((b0 and 0xFE) == 0xFC) return false
            // Link-local unicast fe80::/10 (fe80... to febf...)
            if (b0 == 0xFE && (b1 and 0xC0) == 0x80) return false
            // Multicast ff00::/8
            if (b0 == 0xFF) return false
            // IPv4-mapped IPv6 ::ffff:0:0/96
            if (isIpv4MappedIpv6(raw)) {
                val mappedIpv4 = InetAddress.getByAddress(raw.copyOfRange(12, 16))
                return isPublicAddress(mappedIpv4)
            }
        }
        return true
    }

    private fun isIpv4MappedIpv6(raw: ByteArray): Boolean {
        if (raw.size != 16) return false
        for (i in 0..9) {
            if (raw[i] != 0.toByte()) return false
        }
        return raw[10] == 0xFF.toByte() && raw[11] == 0xFF.toByte()
    }

    fun isAllowedImageMimeType(contentType: String?): Boolean {
        if (contentType.isNullOrBlank()) return false
        val normalized = contentType.substringBefore(';').trim().lowercase(Locale.ROOT)
        return normalized in ALLOWED_IMAGE_MIME_PREFIXES
    }
}
