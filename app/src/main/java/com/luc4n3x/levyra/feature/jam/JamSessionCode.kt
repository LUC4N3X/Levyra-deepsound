package com.luc4n3x.levyra.feature.jam

import java.security.SecureRandom
import java.util.Locale

data class JamSessionCode(
    val hostAddress: String,
    val port: Int,
    val secret: String
) {
    fun encoded(): String {
        require(isPrivateIpv4(hostAddress))
        require(port in MIN_PORT..MAX_PORT)
        require(secret.matches(Regex("[0-9a-fA-F]{${SECRET_BYTES * 2}}")))
        val octets = hostAddress.split('.').map(String::toInt)
        val bytes = ByteArray(TOTAL_BYTES)
        octets.take(ADDRESS_BYTES).forEachIndexed { index, value -> bytes[index] = value.toByte() }
        bytes[ADDRESS_BYTES] = ((port shr 8) and 0xFF).toByte()
        bytes[ADDRESS_BYTES + 1] = (port and 0xFF).toByte()
        decodeHex(secret).copyInto(bytes, ADDRESS_BYTES + PORT_BYTES)
        return encodeBase32(bytes)
    }

    fun formatted(): String = encoded().chunked(GROUP_SIZE).joinToString("-")

    fun deepLink(): String = "$DEEP_LINK_PREFIX${encoded()}"

    companion object {
        const val SECRET_BYTES = 5
        const val ADDRESS_BYTES = 4
        const val PORT_BYTES = 2
        const val TOTAL_BYTES = ADDRESS_BYTES + PORT_BYTES + SECRET_BYTES
        const val ENCODED_LENGTH = 18
        const val GROUP_SIZE = 6
        const val DEEP_LINK_PREFIX = "levyra://jam/"
        const val MIN_PORT = 1024
        const val MAX_PORT = 65_535

        private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        private val random = SecureRandom()

        fun newSecret(): String {
            val bytes = ByteArray(SECRET_BYTES)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun parse(raw: String): JamSessionCode? {
            val trimmed = raw.trim()
            val body = if (trimmed.startsWith(DEEP_LINK_PREFIX, ignoreCase = true)) {
                trimmed.substring(DEEP_LINK_PREFIX.length)
            } else {
                trimmed
            }
            val normalized = when {
                body.length == ENCODED_LENGTH && body.all { it.uppercaseChar() in ALPHABET } -> body
                body.length == ENCODED_LENGTH + 2 &&
                    body[GROUP_SIZE] == '-' && body[GROUP_SIZE * 2 + 1] == '-' &&
                    body.filterNot { it == '-' }.all { it.uppercaseChar() in ALPHABET } -> body.filterNot { it == '-' }
                else -> return null
            }.uppercase(Locale.ROOT)
            if (normalized.length != ENCODED_LENGTH) return null
            if (ALPHABET.indexOf(normalized.last()) and 0x03 != 0) return null
            val bytes = decodeBase32(normalized) ?: return null
            if (bytes.size < TOTAL_BYTES) return null

            val address = (0 until ADDRESS_BYTES).joinToString(".") { (bytes[it].toInt() and 0xFF).toString() }
            val port = ((bytes[ADDRESS_BYTES].toInt() and 0xFF) shl 8) or (bytes[ADDRESS_BYTES + 1].toInt() and 0xFF)
            if (port !in MIN_PORT..MAX_PORT) return null
            if (!isPrivateIpv4(address)) return null
            val secret = (ADDRESS_BYTES + PORT_BYTES until TOTAL_BYTES)
                .joinToString("") { "%02x".format(bytes[it]) }
            return JamSessionCode(address, port, secret)
        }

        fun isPrivateIpv4(address: String): Boolean {
            val parts = address.split('.')
            if (parts.size != ADDRESS_BYTES) return false
            val octets = parts.map { it.toIntOrNull() ?: return false }
            if (octets.any { it !in 0..255 }) return false
            return when {
                octets[0] == 10 -> true
                octets[0] == 172 && octets[1] in 16..31 -> true
                octets[0] == 192 && octets[1] == 168 -> true
                octets[0] == 169 && octets[1] == 254 -> true
                else -> false
            }
        }

        private fun encodeBase32(bytes: ByteArray): String {
            val builder = StringBuilder(ENCODED_LENGTH)
            var buffer = 0
            var bitsLeft = 0
            for (byte in bytes) {
                buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
                bitsLeft += 8
                while (bitsLeft >= 5) {
                    builder.append(ALPHABET[(buffer shr (bitsLeft - 5)) and 0x1F])
                    bitsLeft -= 5
                }
            }
            if (bitsLeft > 0) {
                builder.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
            }
            return builder.toString()
        }

        private fun decodeBase32(value: String): ByteArray? {
            val output = ArrayList<Byte>(TOTAL_BYTES)
            var buffer = 0
            var bitsLeft = 0
            for (character in value) {
                val index = ALPHABET.indexOf(character)
                if (index < 0) return null
                buffer = (buffer shl 5) or index
                bitsLeft += 5
                if (bitsLeft >= 8) {
                    output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                    bitsLeft -= 8
                }
            }
            return output.toByteArray()
        }

        private fun decodeHex(value: String): ByteArray {
            val clean = value.trim()
            val bytes = ByteArray(SECRET_BYTES)
            for (index in 0 until SECRET_BYTES) {
                val start = index * 2
                if (start + 2 > clean.length) break
                bytes[index] = clean.substring(start, start + 2).toIntOrNull(16)?.toByte() ?: 0
            }
            return bytes
        }
    }
}
