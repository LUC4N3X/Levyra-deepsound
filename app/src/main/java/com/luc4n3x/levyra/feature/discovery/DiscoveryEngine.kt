package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.Track
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
    val editorial: suspend (language: String) -> List<Track>
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
        rotation: Long
    ): DiscoveryBuildResult = coroutineScope {
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
        val outcomes = requests.map { request -> async { request() } }.awaitAll()
        val cards = DiscoveryRanker.rank(outcomes.flatMap { it.candidates }, profile, interactions, limit, rotation)
        DiscoveryBuildResult(cards, outcomes.count { it.failed }, outcomes.size)
    }

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
    }
}
