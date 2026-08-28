package com.luc4n3x.levyra.feature.jam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamSessionCodeTest {

    @Test
    fun formattedAndDeepLinkRoundTrip() {
        val code = JamSessionCode("192.168.1.42", 42_424, "0011223344")

        assertEquals(code, JamSessionCode.parse(code.formatted()))
        assertEquals(code, JamSessionCode.parse(code.deepLink()))
        assertEquals(JamSessionCode.ENCODED_LENGTH, code.encoded().length)
    }

    @Test
    fun malformedDecorationsAreRejectedInsteadOfFilteredOut() {
        val code = JamSessionCode("10.0.0.8", 54_321, "aabbccddee")

        assertNull(JamSessionCode.parse("#${code.formatted()}!"))
        assertNull(JamSessionCode.parse("levyra://other/${code.encoded()}"))
        assertNull(JamSessionCode.parse("not-a-jam-code"))
    }

    @Test
    fun publicAddressesAndInvalidPortsAreRejected() {
        assertNull(JamSessionCode.parse(encodeRaw("8.8.8.8", 42_424, "0011223344")))
        assertNull(JamSessionCode.parse(encodeRaw("192.168.1.42", 80, "0011223344")))
        assertNull(JamSessionCode.parse(encodeRaw("192.168.1.42", 0, "0011223344")))
    }

    @Test
    fun privateAddressCatalogMatchesLanRanges() {
        assertTrue(JamSessionCode.isPrivateIpv4("10.0.0.1"))
        assertTrue(JamSessionCode.isPrivateIpv4("172.16.0.1"))
        assertTrue(JamSessionCode.isPrivateIpv4("172.31.255.254"))
        assertTrue(JamSessionCode.isPrivateIpv4("192.168.4.5"))
        assertTrue(JamSessionCode.isPrivateIpv4("169.254.1.2"))
        assertFalse(JamSessionCode.isPrivateIpv4("172.32.0.1"))
        assertFalse(JamSessionCode.isPrivateIpv4("127.0.0.1"))
        assertFalse(JamSessionCode.isPrivateIpv4("224.0.0.1"))
    }

    @Test
    fun generatedSecretsUseAllConfiguredEntropyBytes() {
        val generated = List(128) { JamSessionCode.newSecret() }

        assertTrue(generated.all { it.matches(Regex("[0-9a-f]{${JamSessionCode.SECRET_BYTES * 2}}")) })
        assertEquals(generated.size, generated.toSet().size)
        assertNotEquals(generated.first(), generated.last())
    }

    private fun encodeRaw(address: String, port: Int, secret: String): String {
        val bytes = ByteArray(JamSessionCode.TOTAL_BYTES)
        address.split('.').forEachIndexed { index, value -> bytes[index] = value.toInt().toByte() }
        bytes[JamSessionCode.ADDRESS_BYTES] = (port ushr 8).toByte()
        bytes[JamSessionCode.ADDRESS_BYTES + 1] = port.toByte()
        secret.chunked(2).forEachIndexed { index, value ->
            bytes[JamSessionCode.ADDRESS_BYTES + JamSessionCode.PORT_BYTES + index] = value.toInt(16).toByte()
        }
        val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        val output = StringBuilder()
        var buffer = 0
        var bits = 0
        bytes.forEach { byte ->
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                output.append(alphabet[(buffer ushr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) output.append(alphabet[(buffer shl (5 - bits)) and 31])
        return output.toString()
    }
}
