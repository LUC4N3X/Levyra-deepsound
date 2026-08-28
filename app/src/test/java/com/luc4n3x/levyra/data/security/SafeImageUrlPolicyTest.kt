package com.luc4n3x.levyra.data.security

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeImageUrlPolicyTest {

    @Test
    fun publicHttpsUrlsAreAccepted() {
        val validUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/cover.jpg/600x600bb.jpg"
        assertEquals(validUrl, SafeImageUrlPolicy.sanitize(validUrl))

        val cdnUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg"
        assertEquals(cdnUrl, SafeImageUrlPolicy.sanitize(cdnUrl))
    }

    @Test
    fun nonHttpsAndNonStandardPortsAreRejected() {
        assertEquals("", SafeImageUrlPolicy.sanitize("http://example.com/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://example.com:8443/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("ftp://example.com/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("javascript:alert(1)"))
    }

    @Test
    fun urlsWithUserInfoAreRejected() {
        assertEquals("", SafeImageUrlPolicy.sanitize("https://user:password@example.com/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://admin@example.com/art.jpg"))
    }

    @Test
    fun privateAndLoopbackIpLiteralsAreRejected() {
        assertEquals("", SafeImageUrlPolicy.sanitize("https://127.0.0.1/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://10.0.0.1/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://172.16.0.1/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://192.168.1.1/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://169.254.169.254/latest/meta-data/"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://[::1]/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://[fe80::1]/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://localhost/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://test.localhost/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://router.local/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://service.internal/art.jpg"))
        assertEquals("", SafeImageUrlPolicy.sanitize("https://dark.onion/art.jpg"))
    }

    @Test
    fun isPublicAddressAccuratelyClassifiesIps() {
        assertTrue(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("8.8.8.8")))
        assertTrue(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("1.1.1.1")))
        assertTrue(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("2606:4700:4700::1111")))

        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("10.254.1.1")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("172.20.0.1")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("192.168.0.100")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("169.254.169.254")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("224.0.0.1")))
        assertFalse(SafeImageUrlPolicy.isPublicAddress(InetAddress.getByName("::1")))
    }

    @Test
    fun isAllowedImageMimeTypeValidatesContentTypes() {
        assertTrue(SafeImageUrlPolicy.isAllowedImageMimeType("image/jpeg"))
        assertTrue(SafeImageUrlPolicy.isAllowedImageMimeType("image/png"))
        assertTrue(SafeImageUrlPolicy.isAllowedImageMimeType("image/webp"))
        assertTrue(SafeImageUrlPolicy.isAllowedImageMimeType("image/jpeg; charset=utf-8"))

        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType("image/svg+xml"))
        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType("image/x-icon"))
        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType("text/html"))
        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType("application/json"))
        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType("application/octet-stream"))
        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType(null))
        assertFalse(SafeImageUrlPolicy.isAllowedImageMimeType(""))
    }
}
