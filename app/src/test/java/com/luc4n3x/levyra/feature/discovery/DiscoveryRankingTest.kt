package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DiscoveryRankingTest {
    @Test
    fun duplicatesAcrossProvidersCollapseIntoOneCard() {
        val profile = profile(recent = listOf(track("Seed", "Artist A")))
        val cards = DiscoveryRanker.rank(
            listOf(
                candidate(track("Nuova Canzone", "Artist B", id = "yt1"), DiscoveryReasonKind.SimilarToTrack),
                candidate(track("Nuova Canzone (Official Video)", "Artist B", id = "apple1"), DiscoveryReasonKind.NewRelease, fresh = true),
                candidate(track("Nuova Canzone - Official Audio", "Artist B feat. Guest", id = "yt2"), DiscoveryReasonKind.Trending)
            ),
            profile, DiscoveryInteractions(), limit = 10, rotation = 0L
        )
        assertEquals(1, cards.size)
    }

    @Test
    fun liveRemixAndKaraokeVariantsAreFilteredUnlessTheSeedIsAVariant() {
        val profile = profile(recent = listOf(track("Seed", "Artist A")))
        val variants = listOf("Song (Live)", "Song - Remix", "Song Karaoke Version", "Song (Sped Up)", "Song Nightcore")
            .mapIndexed { index, title -> candidate(track(title, "Artist $index", id = "v$index"), DiscoveryReasonKind.SimilarToTrack) }
        assertTrue(DiscoveryRanker.rank(variants, profile, DiscoveryInteractions(), 10, 0L).isEmpty())
        val fromLiveSeed = candidate(track("Other (Live)", "Artist Z", id = "live"), DiscoveryReasonKind.SimilarToTrack).copy(
            reason = DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, "Seed (Live at Wembley)")
        )
        assertEquals(1, DiscoveryRanker.rank(listOf(fromLiveSeed), profile, DiscoveryInteractions(), 10, 0L).size)
        assertFalse(DiscoveryRanker.isVariant("Deliver", ""))
        assertFalse(DiscoveryRanker.isVariant("Olive Tree", ""))
    }

    @Test
    fun knownDismissedSavedAndExcludedTracksNeverReturn() {
        val known = track("Known", "Artist K")
        val profile = profile(recent = listOf(known), excluded = listOf("Blocked Artist"))
        val dismissed = track("Dismissed", "Artist D")
        val saved = track("Saved", "Artist S")
        val interactions = DiscoveryInteractions(
            dismissed = mapOf(discoveryTrackKey(dismissed) to 1L),
            saved = mapOf(discoveryTrackKey(saved) to 2L)
        )
        val cards = DiscoveryRanker.rank(
            listOf(known, dismissed, saved, track("Fresh", "Blocked Artist"), track("Keep", "Artist Ok"))
                .map { candidate(it, DiscoveryReasonKind.SimilarToTrack) },
            profile, interactions, 10, 0L
        )
        assertEquals(listOf("Keep"), cards.map { it.track.title })
    }

    @Test
    fun feedCapsTracksPerArtistAndSpreadsRepeats() {
        val profile = profile(recent = listOf(track("Seed", "Artist A")))
        val candidates = (1..6).map { candidate(track("Same $it", "Prolific"), DiscoveryReasonKind.SimilarToTrack) } +
            (1..4).map { candidate(track("Other $it", "Artist $it"), DiscoveryReasonKind.SimilarToTrack) }
        val cards = DiscoveryRanker.rank(candidates, profile, DiscoveryInteractions(), 10, 0L)
        val prolific = cards.indices.filter { cards[it].track.artist == "Prolific" }
        assertEquals(DiscoveryRanker.MAX_PER_ARTIST, prolific.size)
        assertTrue(prolific.zipWithNext().all { (first, second) -> second - first > 1 })
    }

    @Test
    fun freshReleasesNeedAffinityToLeadTheFeed() {
        val profile = profile(recent = listOf(track("Seed", "Loved Artist"), track("Seed 2", "Second"), track("Seed 3", "Third")))
        val unrelatedFresh = (1..8).map { candidate(track("Brand new $it", "Stranger $it"), DiscoveryReasonKind.NewRelease, fresh = true) }
        val anchoredFresh = candidate(track("New single", "Loved Artist"), DiscoveryReasonKind.NewRelease, fresh = true)
        val related = candidate(track("Deep cut", "Neighbour"), DiscoveryReasonKind.SimilarToTrack)
        val cards = DiscoveryRanker.rank(unrelatedFresh + anchoredFresh + related, profile, DiscoveryInteractions(), 8, 0L)
        assertEquals("New single", cards.first().track.title)
        val unrelated = cards.count { it.track.artist.startsWith("Stranger") }
        assertTrue("unrelated fresh share was $unrelated", unrelated <= 2)
    }

    @Test
    fun repeatedArtistDismissalsLowerThatArtist() {
        val profile = profile(recent = listOf(track("Seed", "Artist A")))
        val penalized = track("Tune", "Often Skipped")
        val neutral = track("Tune B", "Neutral")
        val base = DiscoveryInteractions()
        val skipped = DiscoveryInteractions(artistDismissals = mapOf("often skipped" to 5))
        val penalizedCandidate = candidate(penalized, DiscoveryReasonKind.SimilarToTrack)
        assertTrue(DiscoveryRanker.score(penalizedCandidate, profile, skipped, 0L) < DiscoveryRanker.score(penalizedCandidate, profile, base, 0L))
        val order = DiscoveryRanker.rank(listOf(penalizedCandidate, candidate(neutral, DiscoveryReasonKind.SimilarToTrack)), profile, skipped, 2, 0L)
        assertEquals("Tune B", order.first().track.title)
    }

    @Test
    fun rotationChangesOrderWithoutChangingMembership() {
        val profile = profile(recent = listOf(track("Seed", "Artist A")))
        val candidates = (1..12).map { candidate(track("Song $it", "Artist $it"), DiscoveryReasonKind.SimilarToTrack) }
        val first = DiscoveryRanker.rank(candidates, profile, DiscoveryInteractions(), 12, 1L)
        val second = DiscoveryRanker.rank(candidates, profile, DiscoveryInteractions(), 12, 99L)
        assertEquals(first.map { it.key }.toSet(), second.map { it.key }.toSet())
        assertTrue(first.map { it.key } != second.map { it.key })
    }

    @Test
    fun profileWeighsFollowedArtistsAndSplitsCollaborations() {
        val profile = DiscoveryTasteProfile.from(
            "it", "it",
            recentListens = listOf(track("A", "Mahmood & Blanco")),
            favorites = emptyList(),
            playlistTracks = emptyList(),
            followedArtists = listOf("Blanco"),
            excludedArtists = emptyList(),
            similarArtists = listOf("Elodie", "elodie")
        )
        assertTrue(profile.followsAny("Blanco feat. Someone"))
        assertTrue(profile.affinityFor("Blanco") > profile.affinityFor("Mahmood"))
        assertEquals(listOf("Elodie"), profile.similarArtistNames)
        assertTrue(profile.isColdStart)
    }

    @Test
    fun engineMergesEveryProviderAndUsesEditorialOnlyForNewListeners() = runBlocking {
        val calls = mutableListOf<String>()
        val sources = fakeSources(calls)
        val warm = profile(recent = (1..4).map { track("Seed $it", "Seed Artist $it") }, followed = listOf("Followed"))
        val result = DiscoveryEngine(sources).build(warm, DiscoveryInteractions(), 30, 0L)
        assertTrue(result.cards.any { it.reason.kind == DiscoveryReasonKind.SimilarToTrack })
        assertTrue(result.cards.any { it.reason.kind == DiscoveryReasonKind.FollowedArtist })
        assertTrue(result.cards.any { it.reason.kind == DiscoveryReasonKind.Trending })
        assertFalse("editorial" in calls)

        calls.clear()
        val cold = profile(recent = emptyList())
        val coldResult = DiscoveryEngine(sources).build(cold, DiscoveryInteractions(), 30, 0L)
        assertTrue("editorial" in calls)
        assertTrue(coldResult.cards.isNotEmpty())
    }

    @Test
    fun failingAndSlowProvidersDoNotBreakTheFeed() = runBlocking {
        val sources = DiscoverySources(
            similarToTrack = { throw IllegalStateException("network") },
            newMusic = { _, _ -> awaitCancellation() },
            followedReleases = { emptyList() },
            similarArtistTracks = { emptyList() },
            regional = { listOf(track("Chart hit", "Chart Artist")) },
            editorial = { emptyList() }
        )
        val result = DiscoveryEngine(sources, sourceTimeoutMs = 50L).build(profile(recent = listOf(track("Seed", "A"))), DiscoveryInteractions(), 10, 0L)
        assertEquals(listOf("Chart hit"), result.cards.map { it.track.title })
        assertTrue(result.failedSources >= 2)
        assertFalse(result.allSourcesFailed)
    }

    @Test
    fun everyProviderFailingIsReportedAsAFailure() = runBlocking {
        val boom: suspend () -> List<Track> = { throw IllegalStateException("offline") }
        val sources = DiscoverySources({ boom() }, { _, _ -> boom() }, boom, { boom() }, { boom() }, { boom() })
        val result = DiscoveryEngine(sources).build(profile(recent = emptyList()), DiscoveryInteractions(), 10, 0L)
        assertTrue(result.cards.isEmpty())
        assertTrue(result.allSourcesFailed)
    }

    @Test
    fun cancellingTheBuildPropagatesCancellation() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val sources = DiscoverySources(
            similarToTrack = { started.complete(Unit); awaitCancellation() },
            newMusic = { _, _ -> emptyList() },
            followedReleases = { emptyList() },
            similarArtistTracks = { emptyList() },
            regional = { emptyList() },
            editorial = { emptyList() }
        )
        val job = async { DiscoveryEngine(sources, sourceTimeoutMs = 60_000L).build(profile(recent = listOf(track("Seed", "A"))), DiscoveryInteractions(), 10, 0L) }
        started.await()
        yield()
        job.cancel()
        try {
            job.await()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
        }
    }

    private fun fakeSources(calls: MutableList<String>) = DiscoverySources(
        similarToTrack = { seed -> calls += "similar"; listOf(track("Like ${seed.title}", "Neighbour of ${seed.artist}")) },
        newMusic = { market, language ->
            calls += "new:$market:$language"
            listOf(track("Fresh Single", "Followed"), track("Fresh Other", "Unknown Newcomer"))
        },
        followedReleases = { calls += "radar"; listOf(track("Radar Track", "Followed")) },
        similarArtistTracks = { artist -> calls += "artist"; listOf(track("By $artist", artist)) },
        regional = { market -> calls += "regional:$market"; listOf(track("Regional Hit", "Local Star")) },
        editorial = { calls += "editorial"; listOf(track("Editorial Pick", "Curated Artist")) }
    )

    private fun profile(
        recent: List<Track>,
        followed: List<String> = emptyList(),
        excluded: List<String> = emptyList()
    ) = DiscoveryTasteProfile.from("it", "it", recent, emptyList(), emptyList(), followed, excluded, listOf("Similar One"))

    private fun candidate(track: Track, kind: DiscoveryReasonKind, fresh: Boolean = false) =
        DiscoveryCandidate(track, DiscoveryReason(kind), fresh = fresh)
}

internal fun track(title: String, artist: String, id: String = "$title|$artist", durationMs: Long = 200_000L) = Track(
    id = id,
    title = title,
    artist = artist,
    album = "Album",
    durationMs = durationMs,
    streamUrl = "",
    videoUrl = "",
    thumbnailUrl = "https://img.example/$id.jpg",
    largeThumbnailUrl = "",
    source = "test",
    moodTags = emptySet(),
    energy = 50,
    vocal = 50,
    replayScore = 50,
    cacheScore = 50,
    accentStart = 0,
    accentEnd = 0
)
