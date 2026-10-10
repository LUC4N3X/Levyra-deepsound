package com.luc4n3x.levyra.domain

import java.util.Locale
import kotlin.math.min

enum class DownloadOwnership {
    MANUAL,
    SMART_OFFLINE;

    companion object {
        fun fromStorage(value: String): DownloadOwnership = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: MANUAL
    }
}

data class LevyraSmartOfflineSettings(
    val enabled: Boolean = false,
    val storageLimitBytes: Long = ONE_GIB,
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val preferFavorites: Boolean = true,
    val excludedArtists: Set<String> = emptySet(),
    val excludedPlaylists: Set<String> = emptySet(),
    val lastRefreshAt: Long = 0L
) {
    fun normalized(): LevyraSmartOfflineSettings = copy(
        storageLimitBytes = storageLimitBytes.coerceIn(MIN_STORAGE_BYTES, MAX_STORAGE_BYTES),
        excludedArtists = excludedArtists.normalizeExclusions(),
        excludedPlaylists = excludedPlaylists.normalizeExclusions(),
        lastRefreshAt = lastRefreshAt.coerceAtLeast(0L)
    )

    companion object {
        const val MIN_STORAGE_BYTES = 100L * 1024L * 1024L
        const val MAX_STORAGE_BYTES = 50L * 1024L * 1024L * 1024L
        const val ONE_GIB = 1024L * 1024L * 1024L
        val PRESET_BYTES = listOf(
            500L * 1024L * 1024L,
            ONE_GIB,
            2L * ONE_GIB,
            5L * ONE_GIB
        )
    }
}

data class SmartOfflineCandidate(
    val key: String,
    val track: Track,
    val playCount: Int,
    val recentPlayCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long,
    val favorite: Boolean,
    val playlistCount: Int,
    val recentlyAddedAt: Long,
    val estimatedSizeBytes: Long
)

data class RankedSmartOfflineCandidate(
    val candidate: SmartOfflineCandidate,
    val score: Int
)

data class SmartOfflineStoredItem(
    val id: Long,
    val trackKey: String,
    val trackId: String,
    val ownership: DownloadOwnership,
    val sizeBytes: Long
)

data class SmartOfflinePlan(
    val downloads: List<RankedSmartOfflineCandidate>,
    val evictions: List<SmartOfflineStoredItem>,
    val projectedBytes: Long
)

data class SmartOfflineConstraintSpec(
    val requiresUnmeteredNetwork: Boolean,
    val requiresCharging: Boolean,
    val requiresStorageNotLow: Boolean = true,
    val requiresBatteryNotLow: Boolean = true
)

fun smartOfflineConstraintSpec(settings: LevyraSmartOfflineSettings): SmartOfflineConstraintSpec =
    SmartOfflineConstraintSpec(
        requiresUnmeteredNetwork = settings.wifiOnly,
        requiresCharging = settings.chargingOnly
    )

fun shouldRunSmartOffline(
    settings: LevyraSmartOfflineSettings,
    candidateCount: Int,
    hasActiveManualDownload: Boolean
): Boolean = settings.enabled && candidateCount > 0 && !hasActiveManualDownload

fun rankSmartOfflineCandidates(
    candidates: List<SmartOfflineCandidate>,
    settings: LevyraSmartOfflineSettings,
    nowMs: Long
): List<RankedSmartOfflineCandidate> {
    val excludedArtists = settings.excludedArtists.normalizeExclusions()
    return candidates.asSequence()
        .filter { candidate ->
            candidate.key.isNotBlank() &&
                candidate.track.id.isNotBlank() &&
                candidate.track.artist.normalizedSmartOfflineName() !in excludedArtists
        }
        .distinctBy(SmartOfflineCandidate::key)
        .map { candidate -> RankedSmartOfflineCandidate(candidate, candidate.smartOfflineScore(settings, nowMs)) }
        .sortedWith(
            compareByDescending<RankedSmartOfflineCandidate> { it.score }
                .thenByDescending { it.candidate.lastPlayedAt }
                .thenBy { it.candidate.key }
        )
        .toList()
}

fun buildSmartOfflinePlan(
    ranked: List<RankedSmartOfflineCandidate>,
    stored: List<SmartOfflineStoredItem>,
    storageLimitBytes: Long,
    protectedTrackIds: Set<String> = emptySet(),
    activeTrackKeys: Set<String> = emptySet(),
    maxDownloads: Int = 4
): SmartOfflinePlan {
    if (ranked.isEmpty() || storageLimitBytes <= 0L || maxDownloads <= 0) {
        return SmartOfflinePlan(emptyList(), emptyList(), stored.smartBytes())
    }
    val scoreByKey = ranked.associate { it.candidate.key to it.score }
    val existingTrackIds = stored.mapTo(hashSetOf()) { it.trackId }.filterTo(hashSetOf(), String::isNotBlank)
    val existingKeys = stored.mapTo(hashSetOf(), SmartOfflineStoredItem::trackKey)
    val evictable = stored.asSequence()
        .filter { it.ownership == DownloadOwnership.SMART_OFFLINE }
        .filterNot { it.trackId in protectedTrackIds || it.trackKey in activeTrackKeys }
        .sortedWith(
            compareBy<SmartOfflineStoredItem> { scoreByKey[it.trackKey] ?: 0 }
                .thenBy { it.trackKey }
                .thenBy { it.id }
        )
        .toMutableList()
    val evictions = ArrayList<SmartOfflineStoredItem>()
    var projectedBytes = stored.smartBytes()

    while (projectedBytes > storageLimitBytes && evictable.isNotEmpty()) {
        val removed = evictable.removeAt(0)
        evictions += removed
        projectedBytes = (projectedBytes - removed.sizeBytes.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    val downloads = ArrayList<RankedSmartOfflineCandidate>()
    ranked.forEach { rankedCandidate ->
        if (downloads.size >= maxDownloads) return@forEach
        val candidate = rankedCandidate.candidate
        if (candidate.track.id in existingTrackIds || candidate.key in existingKeys || candidate.key in activeTrackKeys) {
            return@forEach
        }
        val estimate = candidate.estimatedSizeBytes.coerceAtLeast(MIN_ESTIMATED_TRACK_BYTES)
        val replacementPool = evictable.filter { storedItem ->
            (scoreByKey[storedItem.trackKey] ?: 0) < rankedCandidate.score && storedItem !in evictions
        }.toMutableList()
        val replacementEvictions = ArrayList<SmartOfflineStoredItem>()
        var candidateProjection = projectedBytes + estimate
        while (candidateProjection > storageLimitBytes && replacementPool.isNotEmpty()) {
            val removed = replacementPool.removeAt(0)
            replacementEvictions += removed
            candidateProjection = (candidateProjection - removed.sizeBytes.coerceAtLeast(0L)).coerceAtLeast(0L)
        }
        if (candidateProjection > storageLimitBytes) return@forEach
        downloads += rankedCandidate
        replacementEvictions.forEach { removed ->
            evictions += removed
            evictable.remove(removed)
        }
        projectedBytes = candidateProjection
        existingTrackIds += candidate.track.id
        existingKeys += candidate.key
    }

    return SmartOfflinePlan(downloads, evictions.distinctBy(SmartOfflineStoredItem::id), projectedBytes)
}

fun canSmartOfflineEnqueue(
    ownership: DownloadOwnership,
    state: String,
    workId: String
): Boolean = ownership != DownloadOwnership.MANUAL || state !in SMART_OFFLINE_ACTIVE_STATES || workId.isBlank()

private fun SmartOfflineCandidate.smartOfflineScore(
    settings: LevyraSmartOfflineSettings,
    nowMs: Long
): Int {
    val playScore = min(playCount.coerceAtLeast(0), 50) * 4
    val frequencyScore = min(recentPlayCount.coerceAtLeast(0), 10) * 6
    val favoriteScore = if (favorite) if (settings.preferFavorites) 220 else 140 else 0
    val playlistScore = min(playlistCount.coerceAtLeast(0), 3) * 35
    val recentlyDiscoveredAt = maxOf(firstPlayedAt, recentlyAddedAt)
    val recentAddedScore = if (
        recentlyDiscoveredAt > 0L && nowMs - recentlyDiscoveredAt in 0 until THIRTY_DAYS_MS
    ) 20 else 0
    return playScore + frequencyScore + favoriteScore + playlistScore + recentListeningScore(lastPlayedAt, nowMs) + recentAddedScore
}

private fun recentListeningScore(lastPlayedAt: Long, nowMs: Long): Int {
    if (lastPlayedAt <= 0L || nowMs < lastPlayedAt) return 0
    return when (nowMs - lastPlayedAt) {
        in 0 until ONE_DAY_MS -> 80
        in ONE_DAY_MS until SEVEN_DAYS_MS -> 55
        in SEVEN_DAYS_MS until THIRTY_DAYS_MS -> 30
        in THIRTY_DAYS_MS until NINETY_DAYS_MS -> 10
        else -> 0
    }
}

private fun Collection<SmartOfflineStoredItem>.smartBytes(): Long = asSequence()
    .filter { it.ownership == DownloadOwnership.SMART_OFFLINE }
    .sumOf { it.sizeBytes.coerceAtLeast(0L) }

private fun Set<String>.normalizeExclusions(): Set<String> = asSequence()
    .map(String::normalizedSmartOfflineName)
    .filter(String::isNotBlank)
    .toSet()

fun String.normalizedSmartOfflineName(): String = trim().lowercase(Locale.ROOT)

internal val SMART_OFFLINE_ACTIVE_STATES = setOf("QUEUED", "RUNNING", "PAUSED", "RETRYING")
private const val MIN_ESTIMATED_TRACK_BYTES = 2L * 1024L * 1024L
private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
private const val SEVEN_DAYS_MS = 7L * ONE_DAY_MS
private const val THIRTY_DAYS_MS = 30L * ONE_DAY_MS
private const val NINETY_DAYS_MS = 90L * ONE_DAY_MS
