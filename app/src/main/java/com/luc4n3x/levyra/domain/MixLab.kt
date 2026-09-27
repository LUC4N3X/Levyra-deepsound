package com.luc4n3x.levyra.domain

import kotlin.math.abs
import kotlin.math.ceil

enum class MixLabDuration { Short, Medium, Long }

object MixLabDefaults {
    const val MinTrackCount: Int = 1
    const val MaxTrackCount: Int = 60
    const val DefaultTrackCount: Int = 20
    const val MaxCandidates: Int = 400
    const val RecentExclusionMs: Long = LevyraMixDefaults.RecentExclusionMs
    const val ClassicsHorizonYears: Float = 10f
    const val ArtistCapMinimum: Int = 2
    const val ArtistCapRatio: Float = 0.25f
    const val AlbumCapMinimum: Int = 2
    const val AlbumCapRatio: Float = 0.20f
    private const val JitterMagnitude: Float = 0.06f

    internal fun artistCap(limit: Int): Int =
        ceil(limit * ArtistCapRatio).toInt().coerceAtLeast(ArtistCapMinimum)

    internal fun albumCap(limit: Int): Int =
        ceil(limit * AlbumCapRatio).toInt().coerceAtLeast(AlbumCapMinimum)

    internal fun seedJitter(identity: String, seed: Long): Float {
        if (identity.isEmpty()) return 0f
        var hash = seed
        for (char in identity) hash = hash * 31L + char.code
        val mixed = hash xor (hash ushr 32)
        val normalized = (mixed and 0xFFFFFFL).toFloat() / 0xFFFFFFL.toFloat()
        return (normalized - 0.5f) * JitterMagnitude
    }
}

data class MixLabParams(
    val familiarity: Float = 0.5f,
    val recency: Float = 0.5f,
    val duration: MixLabDuration? = null,
    val trackCount: Int = MixLabDefaults.DefaultTrackCount,
    val genres: Set<String> = emptySet(),
    val artistKeys: Set<String> = emptySet(),
    val moodTags: Set<String> = emptySet(),
    val seed: Long = 0L
) {
    val boundedTrackCount: Int
        get() = trackCount.coerceIn(MixLabDefaults.MinTrackCount, MixLabDefaults.MaxTrackCount)

    private val normalizedGenres: Set<String> = genres.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    private val normalizedMoodTags: Set<String> = moodTags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val normalizedMoodTagCount: Int
        get() = normalizedMoodTags.size

    internal fun matchesGenre(candidateGenres: Set<String>): Boolean =
        normalizedGenres.isEmpty() || candidateGenres.any { it in normalizedGenres }

    internal fun matchesArtist(candidateBrowseIds: List<String>, candidateNameKeys: Set<String>): Boolean =
        artistKeys.isEmpty() || candidateBrowseIds.any { it in artistKeys } || candidateNameKeys.any { it in artistKeys }

    internal fun moodOverlap(candidateTags: Set<String>): Set<String> = candidateTags.intersect(normalizedMoodTags)

    internal val hasMoodPreference: Boolean get() = normalizedMoodTags.isNotEmpty()
}

data class MixLabCandidate(
    val track: Track,
    val playCount: Int = 0,
    val listenedMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val isFollowedArtist: Boolean = false,
    val isSimilarSeed: Boolean = false
)

data class MixLabResult(
    val tracks: List<Track>,
    val candidatePoolSize: Int,
    val familiarShare: Float,
    val discoveryShare: Float
) {
    val artistCount: Int get() = tracks.map(LevyraPersonalOrbit::artistKeys).flatten().toSet().size.coerceAtLeast(
        if (tracks.isEmpty()) 0 else 1
    )
}

fun interface MixCriterion {
    fun score(candidate: MixLabCandidate, familiarityValue: Float, params: MixLabParams, nowMs: Long): Float?
}

internal object FamiliarityMixCriterion : MixCriterion {
    override fun score(candidate: MixLabCandidate, familiarityValue: Float, params: MixLabParams, nowMs: Long): Float =
        1f - abs(familiarityValue - params.familiarity)
}

internal object RecencyMixCriterion : MixCriterion {
    override fun score(candidate: MixLabCandidate, familiarityValue: Float, params: MixLabParams, nowMs: Long): Float? {
        val releaseEpochMs = parseReleaseEpochMs(candidate.track) ?: return null
        val ageYears = (nowMs - releaseEpochMs).coerceAtLeast(0L) / MILLIS_PER_YEAR
        val recencyValue = 1f - (ageYears / MixLabDefaults.ClassicsHorizonYears).toFloat().coerceIn(0f, 1f)
        return 1f - abs(recencyValue - params.recency)
    }

    private const val MILLIS_PER_YEAR = 365.25 * 24 * 60 * 60 * 1000

    private fun parseReleaseEpochMs(track: Track): Long? {
        val date = track.releaseDate.trim()
        if (date.length >= 4) {
            val year = date.take(4).toIntOrNull()
            if (year != null && year in 1900..2100) return yearToEpochMs(year, date)
        }
        val year = track.year.trim().take(4).toIntOrNull()
        if (year != null && year in 1900..2100) return yearToEpochMs(year, "")
        return null
    }

    private fun yearToEpochMs(year: Int, isoDate: String): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.clear()
        val month = if (isoDate.length >= 7) isoDate.substring(5, 7).toIntOrNull() ?: 1 else 1
        val day = if (isoDate.length >= 10) isoDate.substring(8, 10).toIntOrNull() ?: 1 else 1
        calendar.set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 28))
        return calendar.timeInMillis
    }
}

internal object DurationMixCriterion : MixCriterion {
    override fun score(candidate: MixLabCandidate, familiarityValue: Float, params: MixLabParams, nowMs: Long): Float? {
        val target = params.duration ?: return null
        val durationMs = candidate.track.durationMs
        if (durationMs <= 0L) return null
        val bucket = bucketOf(durationMs)
        return when {
            bucket == target -> 1f
            isAdjacent(bucket, target) -> 0.35f
            else -> 0f
        }
    }

    private fun bucketOf(durationMs: Long): MixLabDuration = when {
        durationMs < 3L * 60_000L -> MixLabDuration.Short
        durationMs > 5L * 60_000L -> MixLabDuration.Long
        else -> MixLabDuration.Medium
    }

    private fun isAdjacent(a: MixLabDuration, b: MixLabDuration): Boolean =
        a != b && (a == MixLabDuration.Medium || b == MixLabDuration.Medium)
}

internal object MoodMixCriterion : MixCriterion {
    override fun score(candidate: MixLabCandidate, familiarityValue: Float, params: MixLabParams, nowMs: Long): Float? {
        if (!params.hasMoodPreference) return null
        val candidateTags = candidate.track.moodTags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (candidateTags.isEmpty()) return null
        val overlap = params.moodOverlap(candidateTags)
        if (overlap.isEmpty()) return 0f
        return (overlap.size.toFloat() / params.normalizedMoodTagCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    }
}

private val mixLabExtensibleCriteria: List<Pair<Float, MixCriterion>> = listOf(
    1.0f to FamiliarityMixCriterion,
    0.7f to RecencyMixCriterion,
    0.5f to DurationMixCriterion,
    0.6f to MoodMixCriterion
)

object MixLabEngine {

    fun build(
        pool: List<MixLabCandidate>,
        params: MixLabParams,
        exclusions: ArtistExclusions = ArtistExclusions.Empty,
        canonicalSources: List<Track> = emptyList(),
        nowMs: Long = System.currentTimeMillis()
    ): MixLabResult {
        val bounded = pool.take(MixLabDefaults.MaxCandidates)
        val eligible = bounded.filter { candidate ->
            val track = candidate.track
            LevyraPersonalOrbit.isReliableMusicCandidate(track) &&
                !exclusions.excludesTrack(track) &&
                params.matchesGenre(genreTagsOf(track)) &&
                params.matchesArtist(track.artistBrowseIds, LevyraPersonalOrbit.artistKeys(track))
        }
        val deduped = dedupeCandidates(eligible)
        if (deduped.isEmpty()) {
            return MixLabResult(emptyList(), candidatePoolSize = 0, familiarShare = 0f, discoveryShare = 0f)
        }

        val maxPlayCount = deduped.maxOf { it.playCount }
        val scored = deduped.map { candidate ->
            val familiarityValue = familiarityValueOf(candidate, maxPlayCount)
            ScoredCandidate(
                candidate = candidate,
                familiarityValue = familiarityValue,
                score = scoreOf(candidate, familiarityValue, params, nowMs)
            )
        }.sortedWith(
            compareByDescending<ScoredCandidate> { it.score }
                .thenBy { LevyraPersonalOrbit.identityKey(it.candidate.track) }
        )

        val selected = selectWithDiversityAndSpacing(scored, params.boundedTrackCount)
        val familiarCount = selected.count { it.familiarityValue >= 0.5f }
        val familiarShare = if (selected.isEmpty()) 0f else familiarCount.toFloat() / selected.size

        val preparedTracks = prepareMixPlaybackTracks(selected.map { it.candidate.track }, canonicalSources)
        return MixLabResult(
            tracks = preparedTracks,
            candidatePoolSize = deduped.size,
            familiarShare = familiarShare,
            discoveryShare = 1f - familiarShare
        )
    }

    private fun genreTagsOf(track: Track): Set<String> =
        track.moodTags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    private fun familiarityValueOf(candidate: MixLabCandidate, maxPlayCount: Int): Float {
        val normalizedPlayCount = if (maxPlayCount <= 0) 0f else (candidate.playCount.toFloat() / maxPlayCount).coerceIn(0f, 1f)
        return when {
            candidate.isFavorite -> maxOf(0.85f, normalizedPlayCount)
            candidate.isFollowedArtist -> maxOf(0.55f, normalizedPlayCount)
            else -> normalizedPlayCount
        }
    }

    private fun scoreOf(candidate: MixLabCandidate, familiarityValue: Float, params: MixLabParams, nowMs: Long): Float {
        val activeCriteria = mixLabExtensibleCriteria.filter { (_, criterion) ->
            criterion !== MoodMixCriterion || params.hasMoodPreference
        }
        var weightedSum = 0f
        var weightTotal = 0f
        for ((weight, criterion) in activeCriteria) {
            val value = criterion.score(candidate, familiarityValue, params, nowMs) ?: continue
            weightedSum += value.coerceIn(0f, 1f) * weight
            weightTotal += weight
        }
        val parameterMatch = if (weightTotal <= 0f) 0.5f else weightedSum / weightTotal

        var personalAffinity = 0f
        if (candidate.isFavorite) personalAffinity += 0.35f
        if (candidate.isFollowedArtist) personalAffinity += 0.15f
        if (candidate.isSimilarSeed) personalAffinity += 0.10f

        val recentlyPlayed = candidate.lastPlayedAt > 0L && nowMs - candidate.lastPlayedAt <= MixLabDefaults.RecentExclusionMs
        val recentlyPlayedPenalty = if (recentlyPlayed) (1f - params.familiarity) * 0.35f else 0f

        val identity = LevyraPersonalOrbit.identityKey(candidate.track)
        val jitter = MixLabDefaults.seedJitter(identity, params.seed)

        return personalAffinity + parameterMatch - recentlyPlayedPenalty + jitter
    }

    private fun dedupeCandidates(pool: List<MixLabCandidate>): List<MixLabCandidate> {
        val order = ArrayList<MixLabCandidate>(pool.size)
        val indexByIdentity = HashMap<String, Int>(pool.size)
        for (candidate in pool) {
            val identity = LevyraPersonalOrbit.identityKey(candidate.track)
            var existingIndex = indexByIdentity[identity]
            if (existingIndex == null) {
                existingIndex = order.indices.firstOrNull { LevyraPersonalOrbit.sameRecording(order[it].track, candidate.track) }
            }
            if (existingIndex == null) {
                indexByIdentity[identity] = order.size
                order += candidate
            } else {
                val merged = mergeCandidates(order[existingIndex], candidate)
                order[existingIndex] = merged
                indexByIdentity[LevyraPersonalOrbit.identityKey(merged.track)] = existingIndex
            }
        }
        return order
    }

    private fun mergeCandidates(first: MixLabCandidate, second: MixLabCandidate): MixLabCandidate = first.copy(
        playCount = first.playCount + second.playCount,
        listenedMs = first.listenedMs + second.listenedMs,
        lastPlayedAt = maxOf(first.lastPlayedAt, second.lastPlayedAt),
        isFavorite = first.isFavorite || second.isFavorite,
        isFollowedArtist = first.isFollowedArtist || second.isFollowedArtist,
        isSimilarSeed = first.isSimilarSeed || second.isSimilarSeed
    )

    private fun selectWithDiversityAndSpacing(sorted: List<ScoredCandidate>, limit: Int): List<ScoredCandidate> {
        if (limit <= 0 || sorted.isEmpty()) return emptyList()
        val artistCap = MixLabDefaults.artistCap(limit)
        val albumCap = MixLabDefaults.albumCap(limit)
        val albumCounts = HashMap<String, Int>()

        val groups = LinkedHashMap<String, ArrayDeque<ScoredCandidate>>()
        for (item in sorted) {
            val artistKey = primaryArtistKey(item.candidate.track)
            val albumKey = primaryAlbumKey(item.candidate.track)
            val currentAlbumCount = if (albumKey.isNotEmpty()) albumCounts[albumKey] ?: 0 else 0
            if (albumKey.isNotEmpty() && currentAlbumCount >= albumCap) continue
            val queue = groups.getOrPut(artistKey) { ArrayDeque() }
            if (queue.size < artistCap) {
                queue.addLast(item)
                if (albumKey.isNotEmpty()) {
                    albumCounts[albumKey] = currentAlbumCount + 1
                }
            }
        }

        val selected = ArrayList<ScoredCandidate>(minOf(limit, sorted.size))
        var lastArtistKey: String? = null
        var lastAlbumKey: String? = null
        while (selected.size < limit) {
            val nonEmptyGroups = groups.entries.filter { it.value.isNotEmpty() }
            if (nonEmptyGroups.isEmpty()) break
            val artistSpaced = nonEmptyGroups.filter { it.key != lastArtistKey }.ifEmpty { nonEmptyGroups }
            val albumSpaced = artistSpaced.filter {
                val nextAlbum = primaryAlbumKey(it.value.first().candidate.track)
                nextAlbum.isEmpty() || nextAlbum != lastAlbumKey
            }.ifEmpty { artistSpaced }

            val nextGroup = albumSpaced.maxWithOrNull(
                compareBy<Map.Entry<String, ArrayDeque<ScoredCandidate>>> { it.value.size }
                    .thenBy { it.value.first().score }
            ) ?: break
            val chosen = nextGroup.value.removeFirst()
            selected += chosen
            lastArtistKey = nextGroup.key
            val album = primaryAlbumKey(chosen.candidate.track)
            lastAlbumKey = album.takeIf { it.isNotEmpty() }
        }
        return selected
    }

    private fun primaryArtistKey(track: Track): String =
        LevyraPersonalOrbit.artistKeys(track).firstOrNull() ?: track.artist.trim().lowercase()

    private fun primaryAlbumKey(track: Track): String =
        track.album.trim().lowercase()

    private data class ScoredCandidate(
        val candidate: MixLabCandidate,
        val familiarityValue: Float,
        val score: Float
    )
}
