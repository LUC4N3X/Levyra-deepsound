package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKeys
import com.luc4n3x.levyra.domain.primaryArtistSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val RECOMMENDATION_FEEDBACK_STORE = "levyra_recommendation_feedback"
private const val RECOMMENDATION_FEEDBACK_KEY_ENTRIES = "entries_v1"
private const val RECOMMENDATION_FEEDBACK_MAX_ENTRIES = 256
private const val RECOMMENDATION_ARTIST_AFFINITY_LIMIT = 3
private const val RECOMMENDATION_ARTIST_SCORE_STEP = 90
private const val RECOMMENDATION_PREFERRED_TRACK_BONUS = 240
private const val RECOMMENDATION_AVOIDED_TRACK_PENALTY = 480
private const val RECOMMENDATION_MAX_TRACKS_PER_ARTIST = 2

internal enum class RecommendationFeedbackKind {
    MORE_LIKE_THIS,
    LESS_LIKE_THIS,
    BLOCK_ARTIST
}

private data class RecommendationFeedbackEntry(
    val trackId: String,
    val artistKey: String,
    val kind: RecommendationFeedbackKind,
    val updatedAtMs: Long
)

internal data class RecommendationFeedbackSnapshot(
    val preferredTrackIds: Set<String> = emptySet(),
    val avoidedTrackIds: Set<String> = emptySet(),
    val blockedArtistKeys: Set<String> = emptySet(),
    val artistAffinity: Map<String, Int> = emptyMap()
)

internal class RecommendationFeedbackStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        RECOMMENDATION_FEEDBACK_STORE,
        Context.MODE_PRIVATE
    )

    fun snapshot(): RecommendationFeedbackSnapshot = processSnapshot

    suspend fun preload() {
        if (processLoaded) return
        withContext(Dispatchers.Default) {
            synchronized(writeLock) {
                ensureLoadedLocked()
            }
        }
    }

    suspend fun moreLike(track: Track) {
        record(track, RecommendationFeedbackKind.MORE_LIKE_THIS)
    }

    suspend fun lessLike(track: Track) {
        record(track, RecommendationFeedbackKind.LESS_LIKE_THIS)
    }

    suspend fun blockArtist(track: Track) {
        record(track, RecommendationFeedbackKind.BLOCK_ARTIST)
    }

    suspend fun unblockArtist(track: Track) {
        val artistKey = recommendationArtistKey(track)
        if (artistKey.isBlank()) return
        withContext(Dispatchers.Default) {
            synchronized(writeLock) {
                ensureLoadedLocked()
                persistLocked(
                    processEntries.filterNot { entry ->
                        entry.artistKey == artistKey && entry.kind == RecommendationFeedbackKind.BLOCK_ARTIST
                    }
                )
            }
        }
    }

    suspend fun isArtistBlocked(track: Track): Boolean {
        preload()
        val artistKey = recommendationArtistKey(track)
        return artistKey.isNotBlank() && artistKey in processSnapshot.blockedArtistKeys
    }

    private suspend fun record(track: Track, kind: RecommendationFeedbackKind) {
        val trackId = track.id.trim()
        val artistKey = recommendationArtistKey(track)
        if (kind == RecommendationFeedbackKind.BLOCK_ARTIST && artistKey.isBlank()) return
        if (kind != RecommendationFeedbackKind.BLOCK_ARTIST && trackId.isBlank() && artistKey.isBlank()) return

        withContext(Dispatchers.Default) {
            synchronized(writeLock) {
                ensureLoadedLocked()
                val filtered = processEntries.filterNot { entry ->
                    when (kind) {
                        RecommendationFeedbackKind.BLOCK_ARTIST -> entry.artistKey == artistKey
                        RecommendationFeedbackKind.MORE_LIKE_THIS ->
                            (trackId.isNotBlank() && entry.trackId == trackId) ||
                                (artistKey.isNotBlank() &&
                                    entry.artistKey == artistKey &&
                                    entry.kind == RecommendationFeedbackKind.BLOCK_ARTIST)
                        RecommendationFeedbackKind.LESS_LIKE_THIS ->
                            trackId.isNotBlank() && entry.trackId == trackId
                    }
                }
                val updated = buildList {
                    addAll(filtered)
                    add(
                        RecommendationFeedbackEntry(
                            trackId = if (kind == RecommendationFeedbackKind.BLOCK_ARTIST) "" else trackId,
                            artistKey = artistKey,
                            kind = kind,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    )
                }
                    .sortedByDescending(RecommendationFeedbackEntry::updatedAtMs)
                    .take(RECOMMENDATION_FEEDBACK_MAX_ENTRIES)
                persistLocked(updated)
            }
        }
    }

    private fun ensureLoadedLocked() {
        if (processLoaded) return
        val raw = runCatching {
            preferences.getString(RECOMMENDATION_FEEDBACK_KEY_ENTRIES, null)
        }.getOrNull()
        processEntries = decodeEntries(raw)
        processSnapshot = snapshotFrom(processEntries)
        processLoaded = true
    }

    private fun persistLocked(entries: List<RecommendationFeedbackEntry>) {
        val bounded = entries.take(RECOMMENDATION_FEEDBACK_MAX_ENTRIES)
        preferences.edit()
            .putString(RECOMMENDATION_FEEDBACK_KEY_ENTRIES, encodeEntries(bounded))
            .apply()
        processEntries = bounded
        processSnapshot = snapshotFrom(bounded)
        processLoaded = true
    }

    private companion object {
        val writeLock = Any()

        @Volatile
        var processLoaded = false

        @Volatile
        var processSnapshot = RecommendationFeedbackSnapshot()

        var processEntries: List<RecommendationFeedbackEntry> = emptyList()
    }
}

internal fun rankRecommendationCandidates(
    candidates: List<Track>,
    feedback: RecommendationFeedbackSnapshot,
    excludedTrackIds: Set<String> = emptySet()
): List<Track> {
    if (candidates.isEmpty()) return emptyList()
    val excluded = excludedTrackIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    val seen = HashSet<String>()
    val scored = candidates.mapIndexedNotNull { index, track ->
        val trackId = track.id.trim()
        val identity = trackId.ifBlank {
            "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"
        }
        if (identity.isBlank() || !seen.add(identity) || trackId in excluded) return@mapIndexedNotNull null

        val artistKey = recommendationArtistKey(track)
        if (artistKey.isNotBlank() && artistKey in feedback.blockedArtistKeys) return@mapIndexedNotNull null

        var score = 0
        if (trackId in feedback.preferredTrackIds) score += RECOMMENDATION_PREFERRED_TRACK_BONUS
        if (trackId in feedback.avoidedTrackIds) score -= RECOMMENDATION_AVOIDED_TRACK_PENALTY
        score += (feedback.artistAffinity[artistKey] ?: 0) * RECOMMENDATION_ARTIST_SCORE_STEP
        RankedRecommendationCandidate(track, artistKey, score, index)
    }.sortedWith(
        compareByDescending<RankedRecommendationCandidate> { it.score }
            .thenBy { it.originalIndex }
    )

    val primary = ArrayList<Track>(scored.size)
    val overflow = ArrayList<Track>()
    val artistCounts = HashMap<String, Int>()
    scored.forEach { candidate ->
        if (candidate.artistKey.isBlank()) {
            primary += candidate.track
            return@forEach
        }
        val count = artistCounts[candidate.artistKey] ?: 0
        if (count < RECOMMENDATION_MAX_TRACKS_PER_ARTIST) {
            artistCounts[candidate.artistKey] = count + 1
            primary += candidate.track
        } else {
            overflow += candidate.track
        }
    }
    return primary + overflow
}

private data class RankedRecommendationCandidate(
    val track: Track,
    val artistKey: String,
    val score: Int,
    val originalIndex: Int
)

private fun recommendationArtistKey(track: Track): String {
    val primary = primaryArtistSegment(track.artist).ifBlank { track.artist }
    return artistIdentityKeys(primary).minByOrNull(String::length).orEmpty()
}

private fun snapshotFrom(entries: List<RecommendationFeedbackEntry>): RecommendationFeedbackSnapshot {
    val preferred = LinkedHashSet<String>()
    val avoided = LinkedHashSet<String>()
    val blocked = LinkedHashSet<String>()
    val affinity = LinkedHashMap<String, Int>()

    entries.forEach { entry ->
        when (entry.kind) {
            RecommendationFeedbackKind.MORE_LIKE_THIS -> {
                if (entry.trackId.isNotBlank()) preferred += entry.trackId
                if (entry.artistKey.isNotBlank()) {
                    affinity[entry.artistKey] = ((affinity[entry.artistKey] ?: 0) + 1)
                        .coerceAtMost(RECOMMENDATION_ARTIST_AFFINITY_LIMIT)
                }
            }
            RecommendationFeedbackKind.LESS_LIKE_THIS -> {
                if (entry.trackId.isNotBlank()) avoided += entry.trackId
                if (entry.artistKey.isNotBlank()) {
                    affinity[entry.artistKey] = ((affinity[entry.artistKey] ?: 0) - 1)
                        .coerceAtLeast(-RECOMMENDATION_ARTIST_AFFINITY_LIMIT)
                }
            }
            RecommendationFeedbackKind.BLOCK_ARTIST -> if (entry.artistKey.isNotBlank()) {
                blocked += entry.artistKey
            }
        }
    }

    return RecommendationFeedbackSnapshot(
        preferredTrackIds = preferred,
        avoidedTrackIds = avoided,
        blockedArtistKeys = blocked,
        artistAffinity = affinity.filterValues { it != 0 }
    )
}

private fun decodeEntries(raw: String?): List<RecommendationFeedbackEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val kind = runCatching {
                    RecommendationFeedbackKind.valueOf(item.optString("kind"))
                }.getOrNull() ?: continue
                add(
                    RecommendationFeedbackEntry(
                        trackId = item.optString("trackId").trim(),
                        artistKey = item.optString("artistKey").trim(),
                        kind = kind,
                        updatedAtMs = item.optLong("updatedAtMs", 0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun encodeEntries(entries: List<RecommendationFeedbackEntry>): String = JSONArray().apply {
    entries.forEach { entry ->
        put(
            JSONObject()
                .put("trackId", entry.trackId)
                .put("artistKey", entry.artistKey)
                .put("kind", entry.kind.name)
                .put("updatedAtMs", entry.updatedAtMs)
        )
    }
}.toString()
