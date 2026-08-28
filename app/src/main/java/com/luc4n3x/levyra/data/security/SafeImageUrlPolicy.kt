package com.luc4n3x.levyra.data.security

import java.net.InetAddress
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object SafeImageUrlPolicy {
    const val MAX_IMAGE_PAYLOAD_BYTES = 5L * 1024L * 1024L
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

    fun sanitize(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        val trimmed = rawUrl.trim()
        val httpUrl = trimmed.toHttpUrlOrNull() ?: return ""
        if (!isSyntacticallyAllowed(httpUrl)) return ""
        return httpUrl.toString()
    }

    fun isSyntacticallyAllowed(url: HttpUrl): Boolean {
        if (url.scheme != "https") return false
        if (url.port != DEFAULT_HTTPS_PORT) return false
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return false

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
        if (raw.size == 4) {
            val b0 = raw[0].toInt() and 0xFF
            val b1 = raw[1].toInt() and 0xFF
            val b2 = raw[2].toInt() and 0xFF

            if (b0 == 0) return false
            if (b0 == 10) return false
            if (b0 == 100 && b1 in 64..127) return false
            if (b0 == 127) return false
            if (b0 == 169 && b1 == 254) return false
            if (b0 == 172 && b1 in 16..31) return false
            if (b0 == 192 && b1 == 0 && b2 == 0) return false
            if (b0 == 192 && b1 == 0 && b2 == 2) return false
            if (b0 == 192 && b1 == 168) return false
            if (b0 == 198 && b1 in 18..19) return false
            if (b0 == 198 && b1 == 51 && b2 == 100) return false
            if (b0 == 203 && b1 == 0 && b2 == 113) return false
            if (b0 >= 224) return false
        } else if (raw.size == 16) {
            val b0 = raw[0].toInt() and 0xFF
            val b1 = raw[1].toInt() and 0xFF

            if (address.isLoopbackAddress || address.isAnyLocalAddress) return false
            if ((b0 and 0xFE) == 0xFC) return false
            if (b0 == 0xFE && (b1 and 0xC0) == 0x80) return false
            if (b0 == 0xFF) return false
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
