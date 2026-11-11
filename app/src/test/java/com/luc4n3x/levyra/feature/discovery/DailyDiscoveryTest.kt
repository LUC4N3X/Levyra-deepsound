package com.luc4n3x.levyra.feature.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyDiscoveryTest {

    private fun card(id: String, artist: String, reason: DiscoveryReason) =
        DiscoveryCard(track("Song $id", artist, id), reason)

    private val similar = DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, "Seed", seedArtist = "Seed Artist")

    @Test
    fun keepsOnlyCardsWithAnExplainableReason() {
        val cards = listOf(
            card("a", "One", similar),
            card("b", "Two", DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, "Bridge")),
            card("c", "Three", DiscoveryReason(DiscoveryReasonKind.Trending)),
            card("d", "Four", DiscoveryReason(DiscoveryReasonKind.FollowedArtist))
        )
        assertEquals(listOf("a"), dailyDiscoveryCards(cards, emptySet()).map { it.track.id })
    }

    @Test
    fun skipsArtistsTheListenerAlreadyKnows() {
        val cards = listOf(card("a", "Seed Artist", similar), card("b", "Known", similar), card("c", "Fresh", similar))
        assertEquals(listOf("c"), dailyDiscoveryCards(cards, emptySet(), setOf("known")).map { it.track.id })
    }

    @Test
    fun skipsTracksShownElsewhereAndRepeatsOfAnArtist() {
        val cards = listOf(card("a", "One", similar), card("b", "One", similar), card("c", "Two", similar), card("d", "Three", similar))
        val hidden = setOf(cards[2].key)
        assertEquals(listOf("a", "d"), dailyDiscoveryCards(cards, hidden).map { it.track.id })
    }
}
