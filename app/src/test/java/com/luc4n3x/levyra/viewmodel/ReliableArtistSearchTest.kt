package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliableArtistSearchTest {

    @Test
    fun exactVerifiedArtistIsPlacedBeforeUnrelatedGeneralResults() {
        val hugel = artist("HUGEL", "UC-hugel")
        val result = mergeReliableArtistSearchResults(
            query = "hugel",
            exactArtist = hugel,
            verifiedArtists = listOf(
                artist("Solto (FR)", "UC-solto"),
                artist("Ultra Naté", "UC-ultra")
            )
        )

        assertEquals("HUGEL", result.first().name)
        assertTrue(result.any { it.browseId == "UC-hugel" })
    }

    @Test
    fun exactArtistIsDeduplicatedByBrowseId() {
        val exact = artist("HUGEL", "UC-hugel")
        val duplicate = exact.copy(subscribers = "2M")

        val result = mergeReliableArtistSearchResults(
            query = "HUGEL",
            exactArtist = exact,
            verifiedArtists = listOf(duplicate)
        )

        assertEquals(1, result.size)
        assertEquals("UC-hugel", result.single().browseId)
    }

    @Test
    fun unrelatedDirectLookupIsNotInjectedAsExactResult() {
        val result = mergeReliableArtistSearchResults(
            query = "HUGEL",
            exactArtist = artist("Hugel Angel", "UC-wrong"),
            verifiedArtists = listOf(artist("Ultra Naté", "UC-ultra"))
        )

        assertTrue(result.none { it.browseId == "UC-wrong" })
    }

    @Test
    fun resultLimitIsBounded() {
        val artists = (1..20).map { index -> artist("Artist $index", "UC-$index") }

        val result = mergeReliableArtistSearchResults(
            query = "missing",
            exactArtist = null,
            verifiedArtists = artists,
            limit = 8
        )

        assertEquals(8, result.size)
    }

    private fun artist(name: String, browseId: String) = ArtistHit(
        name = name,
        subscribers = "",
        thumbnailUrl = "https://example.com/$browseId.jpg",
        accentStart = 0,
        accentEnd = 0,
        browseId = browseId,
        officialArtwork = true
    )
}
