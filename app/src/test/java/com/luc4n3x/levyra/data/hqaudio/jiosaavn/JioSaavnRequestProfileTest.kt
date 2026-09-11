package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnRequestProfileTest {
    @Test
    fun apiHeadersCarryTheIndiaProfileWithOneConsistentAddress() {
        val profile = JioSaavnRequestProfile.create(Random(7))
        val headers = profile.apiHeaders()
        assertTrue(headers.getValue("Accept-Language").contains("en-IN"))
        assertEquals(JioSaavnRequestProfile.ACCEPT_LANGUAGE, headers.getValue("Accept-Language"))
        assertEquals(profile.forwardedAddress, headers.getValue("X-Forwarded-For"))
        assertEquals(profile.forwardedAddress, headers.getValue("X-Real-IP"))
        assertTrue(headers.getValue("User-Agent").startsWith("Mozilla/5.0 (Linux; Android"))
        assertTrue(JioSaavnRequestProfile.isIndianAddress(profile.forwardedAddress))
    }

    @Test
    fun generatedAddressesStayInsideIndianBlocksAndAvoidReservedHosts() {
        val random = Random(42)
        repeat(500) {
            val address = JioSaavnRequestProfile.create(random).forwardedAddress
            assertTrue(address, JioSaavnRequestProfile.isIndianAddress(address))
            val host = address.substringAfterLast('.').toInt()
            assertTrue(address, host in 11..250)
        }
    }

    @Test
    fun mediaHeadersNeverCarryTheIndiaProfile() {
        val headers = JioSaavnRequestProfile.create(Random(3)).mediaHeaders()
        assertFalse(headers.containsKey("X-Forwarded-For"))
        assertFalse(headers.containsKey("X-Real-IP"))
        assertFalse(headers.containsKey("Accept-Language"))
    }

    @Test
    fun addressClassificationRejectsNonIndianAddresses() {
        assertFalse(JioSaavnRequestProfile.isIndianAddress("8.8.8.8"))
        assertFalse(JioSaavnRequestProfile.isIndianAddress("not-an-ip"))
        assertTrue(JioSaavnRequestProfile.isIndianAddress("157.48.21.77"))
    }

    @Test
    fun maskedAddressHidesHostPart() {
        assertEquals("49.37.x.x", JioSaavnRequestProfile("49.37.160.14", "Reliance Jio").maskedAddress)
    }

    @Test
    fun indiaHeadersDoNotLeakIntoUnrelatedLevyraNetworking() {
        val sourceRoot = listOf(Path.of("src/main/java"), Path.of("app/src/main/java")).first(Files::isDirectory)
        val owners = Files.walk(sourceRoot).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }
                .filter { path ->
                    val text = Files.readString(path)
                    text.contains("X-Forwarded-For") || text.contains("X-Real-IP") || text.contains("en-IN")
                }
                .map { it.fileName.toString() }
                .toList()
        }
        assertEquals(listOf("JioSaavnRequestProfile.kt"), owners)
    }
}
