package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistIdentityTest {
    @Test
    fun collaborationResolvesPrimaryArtistWhenNoExactActExists() {
        assertTrue(
            artistSearchMatchScore("Shiva & Geolier", "Shiva") >
                artistSearchMatchScore("Shiva & Geolier", "Geolier")
        )
    }

    @Test
    fun exactDuoNameBeatsPrimaryMember() {
        assertTrue(
            artistSearchMatchScore("Simon & Garfunkel", "Simon & Garfunkel") >
                artistSearchMatchScore("Simon & Garfunkel", "Simon")
        )
    }

    @Test
    fun commaSeparatedFeatureUsesLeadArtist() {
        assertEquals("Fred De Palma", primaryArtistSegment("Fred De Palma, Anitta"))
    }

    @Test
    fun localizedCollaborationUsesLeadArtist() {
        assertEquals("Shiva", primaryArtistSegment("Shiva e Geolier"))
    }

    @Test
    fun featuringSuffixUsesLeadArtist() {
        assertEquals("Guè", primaryArtistSegment("Guè feat. Marracash"))
    }

    @Test
    fun ambiguousSoloNamesRemainUnchanged() {
        assertEquals("ANNA", primaryArtistSegment("ANNA"))
        assertEquals("Ultimo", primaryArtistSegment("Ultimo"))
    }
    @Test
    fun curatorAndPlaylistNamesAreRejectedFromArtistShelf() {
        assertTrue(!isArtistShelfNameEligible("HIT CANZONI SANREMO 2026"))
        assertTrue(!isArtistShelfNameEligible("Topsify Italia"))
        assertTrue(!isArtistShelfNameEligible("Estate Mix"))
    }

    @Test
    fun realArtistNamesRemainEligible() {
        assertTrue(isArtistShelfNameEligible("Annalisa"))
        assertTrue(isArtistShelfNameEligible("Samurai Jay"))
        assertTrue(isArtistShelfNameEligible("Shiva"))
    }

}
