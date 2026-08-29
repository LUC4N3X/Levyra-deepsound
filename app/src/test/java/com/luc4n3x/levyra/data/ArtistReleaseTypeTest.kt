package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistReleaseTypeTest {
    @Test
    fun bareYearIsInheritedOnlyForDedicatedReleaseSections() {
        assertEquals(ReleaseType.Unknown, artistReleaseType("2020", null))
        assertEquals(ReleaseType.Album, artistReleaseType("2020", ReleaseType.Album))
        assertEquals(ReleaseType.Ep, artistReleaseType("2020", ReleaseType.Ep))
        assertEquals(ReleaseType.Compilation, artistReleaseType("2020", ReleaseType.Compilation))
    }

    @Test
    fun providerTypeWinsOverTheSectionHint() {
        assertEquals(ReleaseType.Ep, artistReleaseType("EP · 2020", ReleaseType.Album))
        assertEquals(ReleaseType.Compilation, artistReleaseType("Compilation · 2020", ReleaseType.Single))
    }

    @Test
    fun compilationAndEpCarouselTitlesMatchTheirShelves() {
        assertEquals(ReleaseType.Compilation, artistReleaseSectionType("Compilations"))
        assertEquals(ReleaseType.Ep, artistReleaseSectionType("EPs"))
    }
}
