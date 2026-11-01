package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class DiscoverySources(
    val similarToTrack: suspend (Track) -> List<Track>,
    val newMusic: suspend (market: String, language: String) -> List<Track>,
    val followedReleases: suspend () -> List<Track>,
    val similarArtistTracks: suspend (String) -> List<Track>,
    val regional: suspend (market: String) -> List<Track>,
    val editorial: suspend (language: String) -> List<Track>,
    val sceneCategories: suspend () -> List<ExploreCategory> = { emptyList() },
    val sceneTracks: suspend (params: String) -> List<Track> = { emptyList() },
    val cityTracks: suspend (city: String, rotation: Long) -> List<Track> = { _, _ -> emptyList() },
    val decadeTracks: suspend (startYear: Int, rotation: Long) -> List<Track> = { _, _ -> emptyList() },
    val distantRegions: (market: String, language: String) -> List<Pair<String, String>> = { _, _ -> emptyList() }
)

data class DiscoveryBuildResult(
    val cards: List<DiscoveryCard>,
    val failedSources: Int,
    val totalSources: Int
) {
    val allSourcesFailed: Boolean get() = totalSources > 0 && failedSources == totalSources
}

class DiscoveryEngine(
    private val sources: DiscoverySources,
    private val sourceTimeoutMs: Long = DEFAULT_SOURCE_TIMEOUT_MS
) {
    private class Outcome(val candidates: List<DiscoveryCandidate>, val failed: Boolean)

    private class FetchResult(val tracks: List<Track>, val failed: Boolean) {
        fun map(transform: (Track) -> DiscoveryCandidate): Outcome = Outcome(tracks.map(transform), failed)
    }

    suspend fun build(
        profile: DiscoveryTasteProfile,
        interactions: DiscoveryInteractions,
        limit: Int,
        rotation: Long,
        request: DiscoveryRequest = DiscoveryRequest()
    ): DiscoveryBuildResult = coroutineScope {
        val requests = when {
            request.lens != DiscoveryLens.Open -> lensRequests(request.lens, profile, rotation)
            request.level == BubbleLevel.Adventurous -> adventurousRequests(profile, rotation)
            request.level == BubbleLevel.OutOfOrbit -> outerRequests(profile, rotation)
            else -> nearRequests(profile, rotation)
        }
        val outcomes = requests.map { item -> async { item() } }.awaitAll()
        val cards = DiscoveryRanker.rank(outcomes.flatMap { it.candidates }, profile, interactions, limit, rotation, request)
        DiscoveryBuildResult(cards, outcomes.count { it.failed }, outcomes.size)
    }

    private fun nearRequests(profile: DiscoveryTasteProfile, rotation: Long): List<suspend () -> Outcome> {
        val seeds = rotate(profile.seeds, rotation).take(MAX_SEED_QUERIES)
        val similarArtists = rotate(profile.similarArtistNames, rotation).take(MAX_SIMILAR_ARTISTS)
        val requests = buildList<suspend () -> Outcome> {
            seeds.forEach { seed ->
                add {
                    fetch { sources.similarToTrack(seed) }.map {
                        DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, seed.title), confidence = 8)
                    }
                }
            }
            add {
                fetch { sources.newMusic(profile.market, profile.languageCode) }.map { track ->
                    val anchored = profile.followsAny(track.artist)
                    val kind = if (anchored) DiscoveryReasonKind.FollowedArtist else DiscoveryReasonKind.NewRelease
                    DiscoveryCandidate(track, DiscoveryReason(kind), fresh = true, confidence = 6)
                }
            }
            add {
                fetch { sources.followedReleases() }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.FollowedArtist), fresh = true, confidence = 10)
                }
            }
            similarArtists.forEach { artist ->
                add {
                    fetch { sources.similarArtistTracks(artist) }.map {
                        DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.SimilarArtist), confidence = 4)
                    }
                }
            }
            add {
                fetch { sources.regional(profile.market) }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Trending), confidence = 2)
                }
            }
            if (profile.isColdStart) {
                add {
                    fetch { sources.editorial(profile.languageCode) }.map {
                        DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Editorial), confidence = 4)
                    }
                }
            }
        }
        return requests
    }

    private fun adventurousRequests(profile: DiscoveryTasteProfile, rotation: Long): List<suspend () -> Outcome> = buildList<suspend () -> Outcome> {
        val seeds = rotate(profile.seeds, rotation).take(ADVENTURE_SEEDS)
        if (seeds.isNotEmpty()) add { secondHops(seeds, profile) }
        rotate(profile.similarArtistNames, rotation).take(MAX_SIMILAR_ARTISTS).forEach { artist ->
            add {
                fetch { sources.similarArtistTracks(artist) }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.SimilarArtist), confidence = 4)
                }
            }
        }
        add { scenes(profile, rotation, ADVENTURE_SCENES) }
    }

    private fun outerRequests(profile: DiscoveryTasteProfile, rotation: Long): List<suspend () -> Outcome> = buildList<suspend () -> Outcome> {
        rotate(sources.distantRegions(profile.market, profile.languageCode), rotation).take(OUTER_REGIONS).forEach { (regionId, label) ->
            add {
                fetch { sources.regional(regionId) }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Country, place = label), confidence = 3)
                }
            }
        }
        add { scenes(profile, rotation + 1, OUTER_SCENES) }
    }

    private fun lensRequests(lens: DiscoveryLens, profile: DiscoveryTasteProfile, rotation: Long): List<suspend () -> Outcome> = when (lens) {
        DiscoveryLens.Open -> emptyList()
        is DiscoveryLens.Country -> listOf(
            request {
                fetch { sources.regional(lens.regionId) }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Country, place = lens.label), confidence = 6)
                }
            },
            request {
                fetch { sources.newMusic(lens.regionId, profile.languageCode) }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Country, place = lens.label), fresh = true, confidence = 4)
                }
            }
        )
        is DiscoveryLens.Scene -> listOf(request {
            fetch { sources.sceneTracks(lens.params) }.map {
                DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Scene, place = lens.title), confidence = 6)
            }
        })
        is DiscoveryLens.Decade -> listOf(request {
            fetch { sources.decadeTracks(lens.startYear, rotation) }.map { DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Decade, place = "${lens.startYear}s"), confidence = 6) }
        })
        is DiscoveryLens.City -> listOf(request {
            fetch { sources.cityTracks(lens.name, rotation) }.map {
                DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.City, place = lens.name), confidence = 6)
            }
        })
    }

    private fun request(block: suspend () -> Outcome): suspend () -> Outcome = block

    private suspend fun secondHops(seeds: List<Track>, profile: DiscoveryTasteProfile): Outcome = coroutineScope {
        val firstHops = seeds.map { seed -> async { seed to fetch { sources.similarToTrack(seed) } } }.awaitAll()
        val firstHopArtists = firstHops.flatMapTo(HashSet()) { (_, result) -> result.tracks.map { discoveryPrimaryArtistKey(it.artist) } }
        val bridges = firstHops.flatMap { (_, result) ->
            result.tracks
                .filter { profile.affinityFor(it.artist) == 0 && !profile.followsAny(it.artist) && !profile.excludes(it.artist) }
                .distinctBy { discoveryPrimaryArtistKey(it.artist) }
                .take(BRIDGES_PER_SEED)
        }
        val hops = bridges.map { bridge ->
            async {
                fetch { sources.similarToTrack(bridge) }.map {
                    if (discoveryPrimaryArtistKey(it.artist) in firstHopArtists) DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, bridge.title), confidence = 5)
                    else DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.SecondHop, bridge.title), confidence = 6)
                }
            }
        }.awaitAll()
        val first = firstHops.flatMap { (seed, result) ->
            result.map { DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, seed.title), confidence = 5) }.candidates
        }
        Outcome(first + hops.flatMap { it.candidates }, firstHops.all { it.second.failed } && hops.all { it.failed })
    }

    private suspend fun scenes(profile: DiscoveryTasteProfile, rotation: Long, count: Int): Outcome = coroutineScope {
        val categories = try {
            withTimeoutOrNull(sourceTimeoutMs) { sources.sceneCategories() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        } ?: return@coroutineScope Outcome(emptyList(), failed = true)
        val unfamiliar = categories.filterNot { category -> sceneIsFamiliar(category.title, profile) }.ifEmpty { categories }
        val picked = rotate(unfamiliar, rotation).take(count)
        val outcomes = picked.map { category ->
            async {
                fetch { sources.sceneTracks(category.params) }.map {
                    DiscoveryCandidate(it, DiscoveryReason(DiscoveryReasonKind.Scene, place = category.title), confidence = 4)
                }
            }
        }.awaitAll()
        Outcome(outcomes.flatMap { it.candidates }, outcomes.isEmpty() || outcomes.all { it.failed })
    }

    private fun sceneIsFamiliar(title: String, profile: DiscoveryTasteProfile): Boolean =
        title.lowercase(Locale.ROOT).split(SCENE_WORD_SPLIT).any { it.length >= 3 && it in profile.tasteTags }

    private suspend fun fetch(block: suspend () -> List<Track>): FetchResult = try {
        val tracks = withTimeoutOrNull(sourceTimeoutMs) { block() }
        if (tracks == null) FetchResult(emptyList(), failed = true) else FetchResult(tracks.take(MAX_TRACKS_PER_SOURCE), failed = false)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        FetchResult(emptyList(), failed = true)
    }

    private fun <T> rotate(items: List<T>, rotation: Long): List<T> {
        if (items.size < 2) return items
        val offset = Math.floorMod(rotation, items.size.toLong()).toInt()
        return items.drop(offset) + items.take(offset)
    }

    companion object {
        const val DEFAULT_SOURCE_TIMEOUT_MS = 9_000L
        private const val MAX_SEED_QUERIES = 4
        private const val MAX_SIMILAR_ARTISTS = 3
        private const val MAX_TRACKS_PER_SOURCE = 30
        private const val ADVENTURE_SEEDS = 2
        private const val BRIDGES_PER_SEED = 2
        private const val ADVENTURE_SCENES = 2
        private const val OUTER_REGIONS = 3
        private const val OUTER_SCENES = 3
        private val SCENE_WORD_SPLIT = Regex("[^\\p{L}\\p{N}]+")
    }
}
