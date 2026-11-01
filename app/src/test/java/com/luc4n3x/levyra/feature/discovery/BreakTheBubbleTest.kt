package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.i18n.smartMusicLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.smartMusicRawEntries
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakTheBubbleTest {
    private val known = (1..6).map { track("Known $it", "Home Artist $it") }
    private val profile = DiscoveryTasteProfile.from(
        languageCode = "it",
        market = "it",
        recentListens = known,
        favorites = emptyList(),
        playlistTracks = emptyList(),
        followedArtists = listOf("Followed Star"),
        excludedArtists = emptyList(),
        similarArtists = listOf("Neighbour Band"),
        tasteTags = setOf("rap", "pop")
    )

    private fun candidates(): List<DiscoveryCandidate> = buildList {
        (1..10).forEach { add(DiscoveryCandidate(track("Close $it", "Home Artist ${it % 6 + 1}"), DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, "Known 1"))) }
        (1..10).forEach { add(DiscoveryCandidate(track("Neighbour $it", "Neighbour Band"), DiscoveryReason(DiscoveryReasonKind.SimilarArtist))) }
        (1..15).forEach { add(DiscoveryCandidate(track("Hop $it", "Hop Artist $it"), DiscoveryReason(DiscoveryReasonKind.SecondHop, "Bridge"))) }
        (1..15).forEach { add(DiscoveryCandidate(track("Scene $it", "Scene Artist $it"), DiscoveryReason(DiscoveryReasonKind.Scene, place = "Jazz"))) }
        (1..15).forEach { add(DiscoveryCandidate(track("Far $it", "Far Artist $it"), DiscoveryReason(DiscoveryReasonKind.Country, place = "日本"))) }
        add(DiscoveryCandidate(track("Followed new", "Followed Star"), DiscoveryReason(DiscoveryReasonKind.FollowedArtist), fresh = true))
    }

    private fun rank(level: BubbleLevel, lens: DiscoveryLens = DiscoveryLens.Open) =
        DiscoveryRanker.rank(candidates(), profile, DiscoveryInteractions(), 20, 0L, DiscoveryRequest(level, lens))

    @Test
    fun levelsMoveProgressivelyFurtherFromTheTasteProfile() {
        val near = rank(BubbleLevel.Near)
        val adventurous = rank(BubbleLevel.Adventurous)
        val outer = rank(BubbleLevel.OutOfOrbit)
        val meanNear = near.map { it.distance }.average()
        val meanAdventurous = adventurous.map { it.distance }.average()
        val meanOuter = outer.map { it.distance }.average()
        println("Mean distance near=%.2f adventurous=%.2f outer=%.2f".format(meanNear, meanAdventurous, meanOuter))
        assertTrue(meanNear < meanAdventurous)
        assertTrue(meanAdventurous < meanOuter)
        assertTrue(outer.all { it.distance >= BubbleLevel.OutOfOrbit.minDistance - 0.16f })
    }

    @Test
    fun outOfOrbitNeverProposesKnownFollowedOrSimilarArtists() {
        val outer = rank(BubbleLevel.OutOfOrbit)
        assertTrue(outer.isNotEmpty())
        assertTrue(outer.none { profile.affinityFor(it.track.artist) > 0 || profile.followsAny(it.track.artist) || profile.nearSimilar(it.track.artist) })
    }

    @Test
    fun adventurousAndOuterDecksAreDiverseAndDuplicateFree() {
        listOf(BubbleLevel.Adventurous, BubbleLevel.OutOfOrbit).forEach { level ->
            val deck = rank(level)
            val artists = deck.map { discoveryPrimaryArtistKey(it.track.artist) }
            assertEquals(artists.size, artists.toSet().size)
            assertEquals(deck.size, deck.map { it.key }.toSet().size)
            val kinds = deck.groupingBy { it.reason.kind }.eachCount()
            assertTrue("$level kinds $kinds", kinds.values.all { it <= 10 })
        }
    }

    @Test
    fun nearLevelKeepsTheOriginalRanking() {
        val legacy = DiscoveryRanker.rank(candidates(), profile, DiscoveryInteractions(), 20, 0L)
        assertEquals(legacy.map { it.key }, rank(BubbleLevel.Near).map { it.key })
    }

    @Test
    fun lensResultsBypassTheDistanceBandButKeepFamiliarityRules() {
        val lensDeck = rank(BubbleLevel.OutOfOrbit, DiscoveryLens.Scene("params", "Jazz"))
        assertTrue(lensDeck.none { profile.affinityFor(it.track.artist) > 0 })
        assertTrue(lensDeck.isNotEmpty())
    }

    @Test
    fun distantRegionsExcludeTheUserLanguageAndMarket() {
        val regions = listOf("it" to "Italia", "es" to "España", "mx" to "México", "jp" to "日本", "us" to "USA")
        val distant = BubbleRegions.distant(regions, "it", "it")
        assertEquals(listOf("es", "mx", "jp", "us"), distant.map { it.first })
        assertFalse(BubbleRegions.distant(regions, "mx", "es").any { it.first == "es" || it.first == "mx" })
    }

    @Test
    fun engineUsesSecondHopAndUnfamiliarScenesForAdventurousLevel() = runBlocking {
        val hopped = mutableListOf<String>()
        val scenesRequested = mutableListOf<String>()
        val sources = DiscoverySources(
            similarToTrack = { seed ->
                hopped += seed.title
                if (seed.title.startsWith("Known")) listOf(track("Bridge for ${seed.title}", "Bridge Artist ${seed.title}"))
                else listOf(track("Deep ${seed.title}", "Deep Artist ${seed.title}"), track("Echo ${seed.title}", "Bridge Artist Known 1"))
            },
            newMusic = { _, _ -> emptyList() },
            followedReleases = { emptyList() },
            similarArtistTracks = { emptyList() },
            regional = { emptyList() },
            editorial = { emptyList() },
            sceneCategories = { listOf(ExploreCategory("Pop", "pop"), ExploreCategory("Jazz", "jazz"), ExploreCategory("Folk", "folk")) },
            sceneTracks = { params ->
                scenesRequested += params
                listOf(track("Scene $params", "Scene Artist $params"))
            }
        )
        val result = DiscoveryEngine(sources).build(profile, DiscoveryInteractions(), 10, 0L, DiscoveryRequest(BubbleLevel.Adventurous))
        assertTrue(hopped.any { it.startsWith("Bridge") })
        assertFalse("familiar scene requested", "pop" in scenesRequested)
        assertTrue(result.cards.any { it.reason.kind == DiscoveryReasonKind.SecondHop })
        assertTrue(result.cards.filter { it.reason.kind == DiscoveryReasonKind.SecondHop }.none { it.track.artist.startsWith("Bridge Artist") })
    }

    @Test
    fun musicBrainzParserKeepsOnlyConfidentMatches() {
        val artists = MusicBrainzParser.artists(
            """{"artists":[
              {"name":"Bristol Band","score":100,"begin-area":{"name":"Bristol"},"life-span":{"begin":"1991"}},
              {"name":"Elsewhere","score":100,"area":{"name":"Leeds"}},
              {"name":"Weak","score":40,"area":{"name":"Bristol"}},
              {"name":"bristol band","score":95,"area":{"name":"Bristol"}}
            ]}""",
            "Bristol"
        )
        assertEquals(listOf("Bristol Band"), artists.map { it.name })
        assertEquals(1991, artists.first().beginYear)
        val releases = MusicBrainzParser.releaseGroups(
            """{"release-groups":[
              {"title":"Blue","score":100,"first-release-date":"1983-05-01","artist-credit":[{"name":"Singer"}]},
              {"title":"Late","score":100,"first-release-date":"1995","artist-credit":[{"name":"Other"}]},
              {"title":"Mix","score":100,"first-release-date":"1984","artist-credit":[{"name":"Various Artists"}]}
            ]}""",
            1980
        )
        assertEquals(listOf("Blue"), releases.map { it.title })
        assertTrue(MusicBrainzParser.artists("not json", "Bristol").isEmpty())
    }

    @Test
    fun smartMusicCopyIsCompleteInEveryLanguage() {
        val keys = SmartMusicText.entries.size
        val placeholder = Regex("\\{[a-z]+\\}")
        val english = smartMusicRawEntries("en")
        smartMusicLocalizationCodes().forEach { code ->
            val entries = smartMusicRawEntries(code)
            assertEquals(code, keys, entries.size)
            entries.forEachIndexed { index, value ->
                assertTrue("$code ${SmartMusicText.entries[index]} blank", value.isNotBlank())
                assertEquals("$code ${SmartMusicText.entries[index]}", placeholder.findAll(english[index]).map { it.value }.sorted().toList(), placeholder.findAll(value).map { it.value }.sorted().toList())
            }
        }
        assertEquals(41, smartMusicLocalizationCodes().size)
        assertEquals("Rompi la bolla", smartMusicCopy("it")[SmartMusicText.BubbleTitle])
        assertTrue(smartMusicCopy("pt-BR")[SmartMusicText.MoodTitle].isNotBlank())
    }
}
