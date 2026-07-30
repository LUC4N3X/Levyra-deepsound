package com.luc4n3x.levyra.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicCookieParserTest {
    @Test
    fun acceptsHeaderJsonAndNetscapeExportsWithoutKeepingUnknownCookies() {
        val header = YoutubeMusicCookieParser.parse("SAPISID=abc123; SID=sid123; EVIL=secret")
        assertNotNull(header)
        assertTrue(header!!.cookieHeader.contains("SAPISID=abc123"))
        assertFalse(header.cookieHeader.contains("EVIL"))

        val json = YoutubeMusicCookieParser.parse("{\"__Secure-3PAPISID\":\"secure123\",\"PREF\":\"hl=it\"}")
        assertNotNull(json)

        val netscape = YoutubeMusicCookieParser.parse(".youtube.com\tTRUE\t/\tTRUE\t0\tSAPISID\tnetscape123")
        assertNotNull(netscape)
    }

    @Test
    fun rejectsExportsWithoutAnAuthenticationCookie() {
        assertNull(YoutubeMusicCookieParser.parse("PREF=hl=it; YSC=test"))
    }
}
