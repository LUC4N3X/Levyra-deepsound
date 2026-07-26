package com.luc4n3x.levyra.desktop.app.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopVersionTest {
    @Test
    fun newerPatchIsDetected() {
        assertTrue(DesktopVersion.isNewer("2.3.17", "2.3.16"))
    }

    @Test
    fun sameOrOlderVersionIsRejected() {
        assertFalse(DesktopVersion.isNewer("2.3.16", "2.3.16"))
        assertFalse(DesktopVersion.isNewer("2.3.15", "2.3.16"))
    }

    @Test
    fun releaseIsNewerThanPrerelease() {
        assertTrue(DesktopVersion.isNewer("2.4.0", "2.4.0-beta.2"))
        assertFalse(DesktopVersion.isNewer("2.4.0-beta.2", "2.4.0"))
    }

    @Test
    fun numericPrereleaseIdentifiersUseNumericOrdering() {
        assertTrue(DesktopVersion.isNewer("2.4.0-beta.10", "2.4.0-beta.2"))
        assertFalse(DesktopVersion.isNewer("2.4.0-beta.2", "2.4.0-beta.10"))
    }

    @Test
    fun invalidVersionsAreRejected() {
        assertFalse(DesktopVersion.isNewer("latest", "2.3.16"))
        assertFalse(DesktopVersion.isNewer("2.3.17", "unknown"))
    }
}
