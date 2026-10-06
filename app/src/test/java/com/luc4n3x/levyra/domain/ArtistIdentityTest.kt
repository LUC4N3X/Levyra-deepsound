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
    fun displayCandidatesRecoverCommaSeparatedArtistsWithoutBrowseIds() {
        assertEquals(
            listOf("Fred De Palma", "Anitta", "Emis Killa"),
            artistDisplayCandidates("Fred De Palma, Anitta, Emis Killa")
        )
    }

    @Test
    fun displayCandidatesDoNotSplitAmpersandOnlyGroups() {
        assertTrue(artistDisplayCandidates("Simon & Garfunkel").isEmpty())
    }

    @Test
    fun structuredCreditsKeepArtistBrowseIdsAligned() {
        assertEquals(
            listOf(
                ArtistCredit("Luis Fonsi", "UCfonsi"),
                ArtistCredit("Daddy Yankee", "UCyankee")
            ),
            artistCredits(
                value = "Luis Fonsi, Daddy Yankee",
                artistBrowseIds = listOf("UCfonsi", "UCyankee")
            )
        )
    }

    @Test
    fun localizedStructuredCreditsCanBeTargetedIndividually() {
        assertEquals(
            listOf(
                ArtistCredit("Shiva", "UCshiva"),
                ArtistCredit("Geolier", "UCgeolier")
            ),
            artistCredits(
                value = "Shiva e Geolier",
                artistBrowseIds = listOf("UCshiva", "UCgeolier")
            )
        )
    }

    @Test
    fun unstructuredCreditFallsBackToPrimaryArtistOnly() {
        assertEquals(
            listOf(ArtistCredit("Dua Lipa", "")),
            artistCredits("Dua Lipa feat. DaBaby", emptyList())
        )
    }

    @Test
    fun surplusParsedNamesFallBackInsteadOfMisaligningBrowseIds() {
        assertEquals(
            listOf(ArtistCredit("Simon & Garfunkel", "UCduo")),
            artistCredits(
                value = "Simon & Garfunkel feat. Guest",
                artistBrowseIds = listOf("UCduo", "UCguest")
            )
        )
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
