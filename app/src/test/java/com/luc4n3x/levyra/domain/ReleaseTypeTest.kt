package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseTypeTest {
    @Test
    fun separatesAlbumsSinglesEpsAndCompilations() {
        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel("Album"))
        assertEquals(ReleaseType.Single, releaseTypeFromProviderLabel("Singolo"))
        assertEquals(ReleaseType.Ep, releaseTypeFromProviderLabel("EP"))
        assertEquals(ReleaseType.Compilation, releaseTypeFromProviderLabel("Compilation"))
        assertEquals(ReleaseType.Compilation, releaseTypeFromProviderLabel("Raccolta"))
        assertEquals(ReleaseType.Unknown, releaseTypeFromProviderLabel("23 Mln riproduzioni"))
    }
}
