package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

internal data class IndianAddressBlock(
    val network: String,
    val prefixLength: Int,
    val operator: String
) {
    val firstAddress: Long
        get() = ipv4ToLong(network) ?: 0L

    val size: Long
        get() = 1L shl (32 - prefixLength)

    fun contains(address: String): Boolean {
        val value = ipv4ToLong(address) ?: return false
        return value >= firstAddress && value < firstAddress + size
    }
}

internal class JioSaavnRequestProfile(
    val forwardedAddress: String,
    val operator: String,
    val userAgent: String = DEFAULT_USER_AGENT
) {
    val maskedAddress: String
        get() = forwardedAddress.split('.').take(2).joinToString(".") + ".x.x"

    fun apiHeaders(): Map<String, String> = linkedMapOf(
        "User-Agent" to userAgent,
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to ACCEPT_LANGUAGE,
        "X-Forwarded-For" to forwardedAddress,
        "X-Real-IP" to forwardedAddress,
        "Referer" to REFERER
    )

    fun mediaHeaders(): Map<String, String> = linkedMapOf(
        "User-Agent" to userAgent,
        "Accept" to "*/*",
        "Accept-Encoding" to "identity"
    )

    companion object {
        const val ACCEPT_LANGUAGE = "en-IN,en;q=0.9"
        const val REFERER = "https://www.jiosaavn.com/"
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Mobile Safari/537.36"
        private const val FIRST_HOST_OCTET = 11
        private const val HOST_OCTET_RANGE = 240

        val addressBlocks: List<IndianAddressBlock> = listOf(
            IndianAddressBlock("49.32.0.0", 12, "Reliance Jio"),
            IndianAddressBlock("152.56.0.0", 14, "Reliance Jio"),
            IndianAddressBlock("157.48.0.0", 16, "Reliance Jio"),
            IndianAddressBlock("117.96.0.0", 14, "Bharti Airtel")
        )

        fun create(random: Random = SecureRandom().asKotlinRandom()): JioSaavnRequestProfile {
            val block = addressBlocks[random.nextInt(addressBlocks.size)]
            val network = block.firstAddress + random.nextLong(block.size)
            val host = (network and 0xFFFFFF00L) or (FIRST_HOST_OCTET + random.nextInt(HOST_OCTET_RANGE)).toLong()
            return JioSaavnRequestProfile(forwardedAddress = longToIpv4(host), operator = block.operator)
        }

        fun isIndianAddress(address: String): Boolean = addressBlocks.any { it.contains(address) }
    }
}

private fun ipv4ToLong(address: String): Long? {
    val parts = address.trim().split('.')
    if (parts.size != 4) return null
    var value = 0L
    for (part in parts) {
        val octet = part.toIntOrNull() ?: return null
        if (octet !in 0..255) return null
        value = (value shl 8) or octet.toLong()
    }
    return value
}

private fun longToIpv4(value: Long): String =
    listOf(24, 16, 8, 0).joinToString(".") { shift -> ((value shr shift) and 0xFFL).toString() }
