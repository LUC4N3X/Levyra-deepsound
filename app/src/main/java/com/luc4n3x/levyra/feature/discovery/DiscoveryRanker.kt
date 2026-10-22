package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.absoluteValue

data class DiscoveryInteractions(
    val dismissed: Map<String, Long> = emptyMap(),
    val saved: Map<String, Long> = emptyMap(),
    val artistDismissals: Map<String, Int> = emptyMap()
) {
    fun seen(key: String): Boolean = key in dismissed || key in saved

    fun artistPenalty(artist: String): Int {
        val count = artistDismissals[discoveryPrimaryArtistKey(artist)] ?: 0
        return if (count >= ARTIST_DISMISS_THRESHOLD) (count - ARTIST_DISMISS_THRESHOLD + 1) * ARTIST_DISMISS_PENALTY else 0
    }

    fun savedArtistBoost(artist: String): Int = if (saved.isEmpty()) 0 else {
        val key = discoveryPrimaryArtistKey(artist)
        (saved.keys.count { it.substringAfterLast('|') == key } * SAVED_ARTIST_BOOST).coerceAtMost(MAX_SAVED_ARTIST_BOOST)
    }

    companion object {
        const val ARTIST_DISMISS_THRESHOLD = 3
        private const val ARTIST_DISMISS_PENALTY = 18
        private const val SAVED_ARTIST_BOOST = 8
        private const val MAX_SAVED_ARTIST_BOOST = 24
    }
}

object DiscoveryRanker {
    const val MAX_PER_ARTIST = 2
    private const val ARTIST_SPACING = 3
    private const val MAX_UNANCHORED_FRESH_SHARE = 4
    private const val MIN_DURATION_MS = 45_000L
    private const val MAX_DURATION_MS = 12L * 60L * 1000L

    private val variantMarkers = listOf(
        "live", "en vivo", "ao vivo", "dal vivo", "remix", "rmx", "karaoke", "cover", "sped up", "speed up", "slowed",
        "reverb", "nightcore", "8d", "instrumental", "acapella", "a cappella", "bass boosted", "mashup", "reaction",
        "1 hour", "10 hours", "loop", "type beat", "fanmade", "tiktok version"
    )
    private val wordBoundary = Regex("""[^\p{L}\p{N}]+""")

    fun rank(
        candidates: List<DiscoveryCandidate>,
        profile: DiscoveryTasteProfile,
        interactions: DiscoveryInteractions,
        limit: Int,
        rotation: Long
    ): List<DiscoveryCard> {
        if (limit <= 0) return emptyList()
        val merged = LinkedHashMap<String, Pair<DiscoveryCandidate, Int>>()
        candidates.forEach { candidate ->
            if (!isEligible(candidate, profile, interactions)) return@forEach
            val key = discoveryTrackKey(candidate.track)
            val value = score(candidate, profile, interactions, rotation)
            val previous = merged[key]
            if (previous == null || value > previous.second) merged[key] = candidate to value
        }
        return diversify(merged.values.sortedByDescending { it.second }, profile, limit)
    }

    internal fun isEligible(candidate: DiscoveryCandidate, profile: DiscoveryTasteProfile, interactions: DiscoveryInteractions): Boolean {
        val track = candidate.track
        if (track.title.isBlank() || track.artist.isBlank()) return false
        val key = discoveryTrackKey(track)
        if (key in profile.knownTrackKeys || interactions.seen(key)) return false
        if (profile.excludes(track.artist)) return false
        if (track.durationMs > 0L && (track.durationMs < MIN_DURATION_MS || track.durationMs > MAX_DURATION_MS)) return false
        if (isVariant(track.title, track.album) && !isVariant(candidate.reason.seedTitle, "")) return false
        return true
    }

    internal fun isVariant(title: String, album: String): Boolean {
        if (title.isBlank()) return false
        val blob = " " + "$title $album".lowercase(Locale.ROOT).replace(wordBoundary, " ") + " "
        return variantMarkers.any { marker -> blob.contains(" $marker ") }
    }

    internal fun score(
        candidate: DiscoveryCandidate,
        profile: DiscoveryTasteProfile,
        interactions: DiscoveryInteractions,
        rotation: Long
    ): Int {
        val track = candidate.track
        val affinity = profile.affinityFor(track.artist).coerceAtMost(80)
        val followed = profile.followsAny(track.artist)
        var score = when (candidate.reason.kind) {
            DiscoveryReasonKind.SimilarToTrack -> 34
            DiscoveryReasonKind.FollowedArtist -> 36
            DiscoveryReasonKind.SimilarArtist -> 26
            DiscoveryReasonKind.NewRelease -> 20
            DiscoveryReasonKind.Editorial -> 16
            DiscoveryReasonKind.Trending -> 14
        }
        score += affinity / 2
        if (followed) score += 18
        if (candidate.fresh) score += if (affinity > 0 || followed) 18 else 6
        if (candidate.reason.kind == DiscoveryReasonKind.NewRelease && affinity == 0 && !followed && !profile.isColdStart) score -= 14
        score += metadataQuality(track)
        score += candidate.confidence.coerceIn(0, 12)
        score += interactions.savedArtistBoost(track.artist)
        score -= interactions.artistPenalty(track.artist)
        score += jitter(discoveryTrackKey(track), rotation)
        return score
    }

    private fun metadataQuality(track: Track): Int {
        var quality = 0
        if (track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank()) quality += 6 else quality -= 10
        if (track.album.isNotBlank()) quality += 3
        if (track.durationMs > 0L) quality += 3
        if (track.isrc.isNotBlank()) quality += 2
        return quality
    }

    private fun jitter(key: String, rotation: Long): Int = ((key.hashCode().toLong() * 31L + rotation).hashCode().absoluteValue % 9)

    private fun diversify(
        scored: List<Pair<DiscoveryCandidate, Int>>,
        profile: DiscoveryTasteProfile,
        limit: Int
    ): List<DiscoveryCard> {
        val pending = scored.map { it.first }.toMutableList()
        val result = ArrayList<DiscoveryCard>(limit)
        val perArtist = HashMap<String, Int>()
        val maxUnanchoredFresh = (limit / MAX_UNANCHORED_FRESH_SHARE).coerceAtLeast(1)
        var unanchoredFresh = 0
        fun allowed(candidate: DiscoveryCandidate): Boolean =
            (perArtist[discoveryPrimaryArtistKey(candidate.track.artist)] ?: 0) < MAX_PER_ARTIST &&
                (!isUnanchoredFresh(candidate, profile) || unanchoredFresh < maxUnanchoredFresh)
        while (result.size < limit && pending.isNotEmpty()) {
            val recentArtists = result.takeLast(ARTIST_SPACING).mapTo(HashSet()) { discoveryPrimaryArtistKey(it.track.artist) }
            val spaced = pending.indexOfFirst { allowed(it) && discoveryPrimaryArtistKey(it.track.artist) !in recentArtists }
            val pickIndex = if (spaced >= 0) spaced else pending.indexOfFirst(::allowed)
            if (pickIndex < 0) break
            val picked = pending.removeAt(pickIndex)
            val artist = discoveryPrimaryArtistKey(picked.track.artist)
            perArtist[artist] = (perArtist[artist] ?: 0) + 1
            if (isUnanchoredFresh(picked, profile)) unanchoredFresh++
            result += DiscoveryCard(picked.track, picked.reason)
        }
        return result
    }

    private fun isUnanchoredFresh(candidate: DiscoveryCandidate, profile: DiscoveryTasteProfile): Boolean =
        !profile.isColdStart && candidate.reason.kind == DiscoveryReasonKind.NewRelease &&
            profile.affinityFor(candidate.track.artist) == 0 && !profile.followsAny(candidate.track.artist)
}
