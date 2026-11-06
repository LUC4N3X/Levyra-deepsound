package com.luc4n3x.levyra.data.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleMintedTokenTest {
    private val jwt = "eyJhbGciOiJFUzI1NiJ9.eyJleHAiOjE5OTk5OTk5OTl9.c2lnbmF0dXJl"

    @Test
    fun readsDevTokenField() {
        assertEquals(jwt, AppleDeveloperTokenProvider.mintedTokenFrom("""{"dev_token":"$jwt","storefront_id":"us"}"""))
    }

    @Test
    fun readsPlainTokenField() {
        assertEquals(jwt, AppleDeveloperTokenProvider.mintedTokenFrom("""{ "token" : "$jwt" }"""))
    }

    @Test
    fun rejectsBodiesWithoutJwt() {
        assertNull(AppleDeveloperTokenProvider.mintedTokenFrom("""{"token":"not-a-jwt"}"""))
        assertNull(AppleDeveloperTokenProvider.mintedTokenFrom("<html>error</html>"))
    }
}
