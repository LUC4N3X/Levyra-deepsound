package com.luc4n3x.levyra.update

import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateInstallerTest {
    @Test
    fun `official release asset URL is accepted`() {
        val url = "https://github.com/LUC4N3X/Levyra-deepsound/releases/download/v2.3.20/levyra-release.apk"

        assertNotNull(validateLevyraUpdateUrl(url, initial = true))
    }

    @Test
    fun `initial update URL rejects unsafe or unrelated sources`() {
        assertNull(validateLevyraUpdateUrl("http://github.com/LUC4N3X/Levyra-deepsound/releases/download/v2.3.20/app.apk", true))
        assertNull(validateLevyraUpdateUrl("https://user:pass@github.com/LUC4N3X/Levyra-deepsound/releases/download/v2.3.20/app.apk", true))
        assertNull(validateLevyraUpdateUrl("https://github.com:8443/LUC4N3X/Levyra-deepsound/releases/download/v2.3.20/app.apk", true))
        assertNull(validateLevyraUpdateUrl("https://github.com/other/project/releases/download/v1/app.apk", true))
        assertNull(validateLevyraUpdateUrl("https://example.com/LUC4N3X/Levyra-deepsound/releases/download/v2.3.20/app.apk", true))
    }

    @Test
    fun `redirects stay on GitHub managed hosts`() {
        assertNotNull(validateLevyraUpdateUrl("https://release-assets.githubusercontent.com/github-production-release-asset/file.apk", false))
        assertNotNull(validateLevyraUpdateUrl("https://github.com/LUC4N3X/Levyra-deepsound/releases/download/v2.3.20/app.apk", false))
        assertNull(validateLevyraUpdateUrl("https://example.com/app.apk", false))
    }

    @Test
    fun `APK response types are constrained`() {
        assertTrue(updateApkContentTypeAccepted("application/vnd.android.package-archive"))
        assertTrue(updateApkContentTypeAccepted("application/octet-stream"))
        assertTrue(updateApkContentTypeAccepted(null))
        assertFalse(updateApkContentTypeAccepted("text/html"))
        assertFalse(updateApkContentTypeAccepted("application/zip"))
    }

    @Test
    fun `private and special destinations are rejected`() {
        assertFalse(isPublicUpdateAddress(InetAddress.getByName("10.0.0.1")))
        assertFalse(isPublicUpdateAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(isPublicUpdateAddress(InetAddress.getByName("203.0.113.1")))
        assertFalse(isPublicUpdateAddress(InetAddress.getByName("fc00::1")))
        assertTrue(isPublicUpdateAddress(InetAddress.getByName("8.8.8.8")))
        assertTrue(isPublicUpdateAddress(InetAddress.getByName("2606:4700:4700::1111")))
    }
}
