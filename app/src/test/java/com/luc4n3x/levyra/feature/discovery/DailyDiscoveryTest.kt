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

    @Test
    fun dailyBuildSkipsTracksTheListenerAlreadyHas() = kotlinx.coroutines.runBlocking {
        val known = track("Song k", "Other", "k")
        val sources = DiscoverySources(
            similarToTrack = { listOf(known, track("Song g", "Studio Musicians", "g"), track("Seed", "Copycat", "c"), track("Song n", "New", "n")) },
            newMusic = { _, _ -> emptyList() },
            followedReleases = { emptyList() },
            similarArtistTracks = { emptyList() },
            regional = { emptyList() },
            editorial = { emptyList() }
        )
        val profile = DiscoveryTasteProfile(
            "en", "US", listOf(track("Seed", "Seed Artist", "s")), emptyMap(), emptySet(), emptySet(), setOf(discoveryTrackKey(known)), emptyList()
        )
        val cards = DiscoveryEngine(sources).daily(profile, DiscoveryInteractions(), 0)
        assertEquals(listOf("n"), cards.map { it.track.id })
        assertEquals("Seed Artist", cards.single().reason.seedArtist)
    }

    @Test
    fun dailyBuildFiltersArtistsBeforeLimitingThePool() = kotlinx.coroutines.runBlocking {
        val radio = (1..12).map { track("Song r$it", "Known", "r$it") } + track("Song n", "New", "n")
        val sources = DiscoverySources(
            similarToTrack = { radio },
            newMusic = { _, _ -> emptyList() },
            followedReleases = { emptyList() },
            similarArtistTracks = { emptyList() },
            regional = { emptyList() },
            editorial = { emptyList() }
        )
        val profile = DiscoveryTasteProfile(
            "en", "US", listOf(track("Seed", "Seed Artist", "s")), emptyMap(), emptySet(), emptySet(), emptySet(), emptyList()
        )
        val cards = DiscoveryEngine(sources).daily(profile, DiscoveryInteractions(), 0, setOf(discoveryPrimaryArtistKey("Known")))
        assertEquals(listOf("n"), cards.map { it.track.id })
    }

    @Test
    fun alternatesSeedsSoNeighboursCarryDifferentReasons() {
        val other = DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, "Other", seedArtist = "Other Artist")
        val cards = listOf(card("a", "One", similar), card("b", "Two", similar), card("c", "Three", other), card("d", "Four", other))
        assertEquals(listOf("a", "c", "b", "d"), dailyDiscoveryCards(cards, emptySet()).map { it.track.id })
    }
}
