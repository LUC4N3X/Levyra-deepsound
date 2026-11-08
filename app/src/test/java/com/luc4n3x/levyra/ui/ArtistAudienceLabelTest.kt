package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistAudienceLabelTest {
    @Test
    fun parsesLocalizedAudienceCounts() {
        assertEquals(231_000_000L, artistAudienceCount("231 Mln ascoltatori mensili"))
        assertEquals(133_000_000L, artistAudienceCount("133M monthly audience"))
        assertEquals(1_200_000L, artistAudienceCount("1,2 Mio. Hörer"))
        assertEquals(45_000L, artistAudienceCount("45K"))
        assertEquals(1_234_567L, artistAudienceCount("1,234,567 subscribers"))
    }

    @Test
    fun unknownFormatsAreNotParsed() {
        assertNull(artistAudienceCount("Artist"))
        assertNull(artistAudienceCount("12 xyzw listeners"))
    }
}
