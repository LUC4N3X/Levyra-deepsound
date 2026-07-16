package com.luc4n3x.levyra.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistOverlayPolicyTest {

    @Test
    fun `artist error takes priority over a partial profile`() {
        assertTrue(shouldShowArtistError(hasError = true, hasProfile = true))
    }

    @Test
    fun `missing profile is an error after loading completes`() {
        assertTrue(shouldShowArtistError(hasError = false, hasProfile = false))
    }

    @Test
    fun `complete profile without error renders normally`() {
        assertFalse(shouldShowArtistError(hasError = false, hasProfile = true))
    }
}
