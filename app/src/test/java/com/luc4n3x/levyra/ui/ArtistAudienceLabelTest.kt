package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ArtistHit
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
    fun subscriberFallbackKeepsItsOwnWording() {
        val artist = ArtistHit("VILLIAN", "4,2 тыс. подписчиков", "", 0, 0, "UCbj2", audienceIsSubscribers = true)
        assertEquals("4,2 тыс. подписчиков", artistAudienceLabel(artist, "ru", "слушателей"))
        assertEquals("", artistAudienceLabel(artist.copy(subscribers = "", audienceIsSubscribers = false), "ru", "слушателей"))
    }

    @Test
    fun unknownFormatsAreNotParsed() {
        assertNull(artistAudienceCount("Artist"))
        assertNull(artistAudienceCount("12 xyzw listeners"))
    }
}
