package com.luc4n3x.levyra.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class NewPipeRuntimeTest {
    private val limit = 32L

    @Test
    fun boundedBodyAcceptsNormalContentLength() {
        val bytes = "extractor-response".encodeToByteArray()

        val result = readBoundedBody(
            ByteArrayInputStream(bytes),
            declaredLength = bytes.size.toLong(),
            maxBytes = limit
        )

        assertArrayEquals(bytes, result)
    }

    @Test
    fun boundedBodyRejectsDeclaredLengthOverLimit() {
        assertThrows(IOException::class.java) {
            readBoundedBody(
                ByteArrayInputStream(byteArrayOf(1)),
                declaredLength = limit + 1,
                maxBytes = limit
            )
        }
    }

    @Test
    fun boundedBodyAcceptsMissingContentLength() {
        val bytes = ByteArray(limit.toInt() - 1) { it.toByte() }

        val result = readBoundedBody(
            ByteArrayInputStream(bytes),
            declaredLength = -1L,
            maxBytes = limit
        )

        assertArrayEquals(bytes, result)
    }

    @Test
    fun boundedBodyRejectsStreamThatCrossesLimit() {
        val bytes = ByteArray(limit.toInt() + 1)

        assertThrows(IOException::class.java) {
            readBoundedBody(
                ByteArrayInputStream(bytes),
                declaredLength = -1L,
                maxBytes = limit
            )
        }
    }

    @Test
    fun boundedBodyAcceptsExactlyTheLimit() {
        val bytes = ByteArray(limit.toInt()) { 7 }

        val result = readBoundedBody(
            ByteArrayInputStream(bytes),
            declaredLength = limit,
            maxBytes = limit
        )

        assertEquals(limit.toInt(), result.size)
        assertArrayEquals(bytes, result)
    }

    @Test
    fun localeConfigUsesLevyraLanguageAndMarket() {
        val french = newPipeLocaleConfig("fr-FR")
        val japanese = newPipeLocaleConfig("ja-JP")

        assertEquals("fr", french.localization.languageCode)
        assertEquals("FR", french.localization.countryCode)
        assertEquals("FR", french.contentCountry.countryCode)
        assertEquals("ja", japanese.localization.languageCode)
        assertEquals("JP", japanese.localization.countryCode)
        assertEquals("JP", japanese.contentCountry.countryCode)
    }

    @Test
    fun acceptLanguageFollowsCurrentLocalization() {
        val french = newPipeLocaleConfig("fr").localization
        val english = newPipeLocaleConfig("en").localization

        assertEquals("fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7", newPipeAcceptLanguage(french))
        assertEquals("en-US,en;q=0.9", newPipeAcceptLanguage(english))
    }
}
