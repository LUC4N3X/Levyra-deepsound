package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.ListenEventEntity
import com.luc4n3x.levyra.data.local.ListenLifetimeTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.LevyraSmartOfflineSettings
import com.luc4n3x.levyra.domain.ListenIdentity
import com.luc4n3x.levyra.domain.RankedSmartOfflineCandidate
import com.luc4n3x.levyra.domain.SMART_OFFLINE_ACTIVE_STATES
import com.luc4n3x.levyra.domain.SmartOfflineCandidate
import com.luc4n3x.levyra.domain.SmartOfflineStoredItem
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildSmartOfflinePlan
import com.luc4n3x.levyra.domain.normalizedSmartOfflineName
import com.luc4n3x.levyra.domain.rankSmartOfflineCandidates
import com.luc4n3x.levyra.domain.smartOfflineConstraintSpec
import com.luc4n3x.levyra.domain.shouldRunSmartOffline
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class SmartOfflineWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runMutex.withLock {
        val preferences = LevyraPreferences(applicationContext)
        val settings = preferences.smartOfflineSettings().normalized()
        if (!settings.enabled) return@withLock Result.success()
        val database = LevyraDatabase.get(applicationContext)
        val manualDownloadActive = hasActiveManualDownload(database)
        if (manualDownloadActive) return@withLock Result.success()

        val now = System.currentTimeMillis()
        val ranked = loadRankedCandidates(database, settings, now)
        if (!shouldRunSmartOffline(settings, ranked.size, manualDownloadActive)) {
            return@withLock Result.success()
        }
        val currentMediaId = PlaybackService.activePlayer?.currentMediaItem?.mediaId.orEmpty()
        val activeTasks = database.offlineDownloadTasksDao().active()
        val activeTrackKeys = activeTasks.mapTo(hashSetOf()) { it.taskKey }
        val stored = database.downloadedTracksDao().all().map { entity ->
            SmartOfflineStoredItem(
                id = entity.id,
                trackKey = ListenIdentity.trackKey(entity.trackId, entity.title, entity.artist),
                trackId = entity.trackId,
                ownership = DownloadOwnership.fromStorage(entity.ownership),
                sizeBytes = downloadedMediaSize(applicationContext, entity.uri)
            )
        }
        val plan = buildSmartOfflinePlan(
            ranked = ranked,
            stored = stored,
            storageLimitBytes = settings.storageLimitBytes,
            protectedTrackIds = setOf(currentMediaId).filterTo(hashSetOf(), String::isNotBlank),
            activeTrackKeys = activeTrackKeys,
            maxDownloads = MAX_DOWNLOADS_PER_RUN
        )

        plan.downloads.forEach { candidate ->
            if (!preferences.smartOfflineSettings().enabled || hasActiveManualDownload(database)) {
                return@forEach
            }
            download(candidate)
        }
        enforceStorageLimit(database, ranked, settings.storageLimitBytes, currentMediaId, activeTrackKeys)
        preferences.setSmartOfflineSettings(
            preferences.smartOfflineSettings().copy(lastRefreshAt = System.currentTimeMillis())
        )
        Result.success()
    }

    private suspend fun loadRankedCandidates(
        database: LevyraDatabase,
        settings: LevyraSmartOfflineSettings,
        now: Long
    ): List<RankedSmartOfflineCandidate> {
        val events = database.listenEventsDao().latest(CANDIDATE_LIMIT)
        val lifetime = database.listenLifetimeDao().topTracks(CANDIDATE_LIMIT)
        val favorites = database.favoriteTracksDao().all()
        val excludedPlaylists = settings.excludedPlaylists.mapTo(hashSetOf(), String::normalizedSmartOfflineName)
        val playlistRows = mutableListOf<com.luc4n3x.levyra.data.local.PlaylistTrackEntity>()
        val excludedPlaylistTrackKeys = hashSetOf<String>()
        database.playlistDao().allPlaylists().forEach { playlist ->
            val rows = database.playlistDao().tracksOf(playlist.id)
            val excluded = playlist.id.normalizedSmartOfflineName() in excludedPlaylists ||
                playlist.name.normalizedSmartOfflineName() in excludedPlaylists
            if (excluded) {
                rows.mapTo(excludedPlaylistTrackKeys) { row ->
                    ListenIdentity.trackKey(row.trackId, row.title, row.artist)
                }
            } else {
                playlistRows += rows
            }
        }

        val eventTracks = events.associate { event -> event.trackKey() to event.toTrack() }
        val favoriteTracks = favorites.associate { favorite ->
            val track = favorite.toTrack()
            ListenIdentity.trackKey(track.id, track.title, track.artist) to track
        }
        val playlistTracks = playlistRows.associate { row ->
            val track = row.toTrack()
            ListenIdentity.trackKey(track.id, track.title, track.artist) to track
        }
        val lifetimeByKey = lifetime.associateBy(ListenLifetimeTrackEntity::trackKey)
        val recentCutoff = now - RECENT_WINDOW_MS
        val recentCounts = events.asSequence()
            .filter { it.startedAt >= recentCutoff }
            .groupingBy(ListenEventEntity::trackKey)
            .eachCount()
        val lastPlayed = events.groupBy(ListenEventEntity::trackKey)
            .mapValues { (_, values) -> values.maxOf(ListenEventEntity::startedAt) }
        val favoriteKeys = favoriteTracks.keys
        val favoriteAdded = favorites.associate { favorite ->
            ListenIdentity.trackKey(favorite.id, favorite.title, favorite.artist) to favorite.createdAt
        }
        val playlistCount = playlistRows.groupingBy { row ->
            ListenIdentity.trackKey(row.trackId, row.title, row.artist)
        }.eachCount()
        val playlistAdded = playlistRows.groupBy { row ->
            ListenIdentity.trackKey(row.trackId, row.title, row.artist)
        }.mapValues { (_, values) -> values.maxOf { it.addedAt } }

        val keys = linkedSetOf<String>().apply {
            addAll(eventTracks.keys)
            addAll(favoriteTracks.keys)
            addAll(playlistTracks.keys)
            addAll(lifetimeByKey.keys)
        }
        val candidates = keys.mapNotNull { key ->
            if (key in excludedPlaylistTrackKeys) return@mapNotNull null
            val lifetimeTrack = lifetimeByKey[key]
            val track = favoriteTracks[key]
                ?: playlistTracks[key]
                ?: eventTracks[key]
                ?: lifetimeTrack?.toTrack()
                ?: return@mapNotNull null
            if (track.id.isBlank()) return@mapNotNull null
            SmartOfflineCandidate(
                key = key,
                track = track.copy(streamUrl = "", videoStreamUrl = ""),
                playCount = lifetimeTrack?.countedPlays ?: recentCounts[key].orZero(),
                recentPlayCount = recentCounts[key].orZero(),
                firstPlayedAt = lifetimeTrack?.firstPlayedAt ?: 0L,
                lastPlayedAt = maxOf(lifetimeTrack?.lastPlayedAt ?: 0L, lastPlayed[key] ?: 0L),
                favorite = key in favoriteKeys,
                playlistCount = playlistCount[key].orZero(),
                recentlyAddedAt = maxOf(favoriteAdded[key] ?: 0L, playlistAdded[key] ?: 0L),
                estimatedSizeBytes = estimateTrackBytes(track.durationMs)
            )
        }
        return rankSmartOfflineCandidates(candidates, settings, now)
    }

    private suspend fun download(candidate: RankedSmartOfflineCandidate) {
        val track = candidate.candidate.track
        val taskKey = candidate.candidate.key
        val workId = OfflineExportWorker.enqueue(
            context = applicationContext,
            trackId = taskKey,
            trackPayload = TrackPayloadCodec.encode(track),
            ownership = DownloadOwnership.SMART_OFFLINE
        )
        val manager = WorkManager.getInstance(applicationContext)
        try {
            while (!isStopped) {
                val info = withContext(Dispatchers.IO) { manager.getWorkInfoById(workId).get() }
                if (info == null || info.state.isFinished) {
                    if (info?.state == WorkInfo.State.FAILED) {
                        Timber.w("Smart Offline candidate failed: %s", track.id)
                    }
                    return
                }
                delay(WORK_POLL_MS)
            }
        } catch (cancelled: CancellationException) {
            val task = LevyraDatabase.get(applicationContext).offlineDownloadTasksDao().byKey(taskKey)
            if (DownloadOwnership.fromStorage(task?.ownership.orEmpty()) == DownloadOwnership.SMART_OFFLINE) {
                OfflineExportWorker.cancel(applicationContext, taskKey)
            }
            throw cancelled
        }
    }

    private suspend fun enforceStorageLimit(
        database: LevyraDatabase,
        ranked: List<RankedSmartOfflineCandidate>,
        limitBytes: Long,
        currentMediaId: String,
        activeTrackKeys: Set<String>
    ) {
        val scoreByKey = ranked.associate { it.candidate.key to it.score }
        val smartDownloads = database.downloadedTracksDao().smartOffline()
            .map { entity -> entity to downloadedMediaSize(applicationContext, entity.uri) }
        var total = smartDownloads.sumOf { (_, size) -> size.coerceAtLeast(0L) }
        if (total <= limitBytes) return
        smartDownloads.sortedWith(
            compareBy<Pair<DownloadEntity, Long>> { (entity, _) ->
                scoreByKey[ListenIdentity.trackKey(entity.trackId, entity.title, entity.artist)] ?: 0
            }.thenBy { (entity, _) -> entity.savedAt }
        ).forEach { (entity, size) ->
            if (total <= limitBytes) return
            val key = ListenIdentity.trackKey(entity.trackId, entity.title, entity.artist)
            if (entity.trackId == currentMediaId || key in activeTrackKeys) return@forEach
            val deleted = SmartOfflineOwnershipGate.withLock {
                val current = database.downloadedTracksDao().byId(entity.id)
                if (current == null || DownloadOwnership.fromStorage(current.ownership) != DownloadOwnership.SMART_OFFLINE) {
                    false
                } else if (deleteDownloadedMedia(applicationContext, current.uri)) {
                    database.downloadedTracksDao().deleteById(current.id)
                    true
                } else {
                    false
                }
            }
            if (deleted) {
                total = (total - size.coerceAtLeast(0L)).coerceAtLeast(0L)
            }
        }
    }

    private suspend fun hasActiveManualDownload(database: LevyraDatabase): Boolean =
        database.offlineDownloadTasksDao().active().any { task ->
            task.state in SMART_OFFLINE_ACTIVE_STATES &&
                DownloadOwnership.fromStorage(task.ownership) == DownloadOwnership.MANUAL
        }

    companion object {
        private val runMutex = Mutex()
        private const val CANDIDATE_LIMIT = 500
        private const val MAX_DOWNLOADS_PER_RUN = 4
        private const val WORK_POLL_MS = 750L
        private const val RECENT_WINDOW_MS = 30L * 24L * 60L * 60L * 1000L
    }
}

object SmartOfflineScheduler {
    private const val PERIODIC_WORK_NAME = "levyra_smart_offline_periodic"
    private const val REFRESH_WORK_NAME = "levyra_smart_offline_refresh"

    fun schedule(context: Context, settings: LevyraSmartOfflineSettings) {
        val manager = WorkManager.getInstance(context.applicationContext)
        val normalized = settings.normalized()
        if (!normalized.enabled) {
            manager.cancelUniqueWork(PERIODIC_WORK_NAME)
            manager.cancelUniqueWork(REFRESH_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<SmartOfflineWorker>(24L, TimeUnit.HOURS)
            .setConstraints(constraints(normalized))
            .setInitialDelay(30L, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        manager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun refresh(context: Context, settings: LevyraSmartOfflineSettings) {
        val normalized = settings.normalized()
        if (!normalized.enabled) return
        val request = OneTimeWorkRequestBuilder<SmartOfflineWorker>()
            .setConstraints(constraints(normalized))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(REFRESH_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun cancelSmartDownloads(context: Context) {
        val appContext = context.applicationContext
        val dao = LevyraDatabase.get(appContext).offlineDownloadTasksDao()
        dao.active()
            .filter { DownloadOwnership.fromStorage(it.ownership) == DownloadOwnership.SMART_OFFLINE }
            .forEach { OfflineExportWorker.cancel(appContext, it.taskKey) }
    }

    private fun constraints(settings: LevyraSmartOfflineSettings): Constraints {
        val spec = smartOfflineConstraintSpec(settings)
        return Constraints.Builder()
            .setRequiredNetworkType(if (spec.requiresUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(spec.requiresCharging)
            .setRequiresStorageNotLow(spec.requiresStorageNotLow)
            .setRequiresBatteryNotLow(spec.requiresBatteryNotLow)
            .build()
    }
}

private fun ListenEventEntity.trackKey(): String = ListenIdentity.trackKey(trackId, title, artist)

private fun ListenLifetimeTrackEntity.toTrack(): Track = Track(
    id = trackId,
    title = title,
    artist = artist,
    album = "",
    durationMs = 0L,
    streamUrl = "",
    videoUrl = "",
    thumbnailUrl = "",
    largeThumbnailUrl = "",
    source = "history",
    moodTags = setOf("music"),
    energy = 50,
    vocal = 50,
    replayScore = 60,
    cacheScore = 50,
    accentStart = 0,
    accentEnd = 0
)

private fun Int?.orZero(): Int = this ?: 0

private fun estimateTrackBytes(durationMs: Long): Long {
    val durationSeconds = (durationMs.coerceAtLeast(180_000L) + 999L) / 1_000L
    return (durationSeconds * 20_000L).coerceAtLeast(2L * 1024L * 1024L)
}

private fun downloadedMediaSize(context: Context, rawUri: String): Long {
    if (rawUri.isBlank()) return 0L
    return runCatching {
        val uri = Uri.parse(rawUri)
        when (uri.scheme?.lowercase()) {
            "content" -> context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?.use { descriptor -> descriptor.length.coerceAtLeast(0L) }
                ?: 0L
            "file" -> uri.path?.let(::File)?.takeIf(File::isFile)?.length() ?: 0L
            else -> File(rawUri).takeIf(File::isFile)?.length() ?: 0L
        }
    }.getOrDefault(0L)
}

private fun deleteDownloadedMedia(context: Context, rawUri: String): Boolean {
    if (rawUri.isBlank()) return true
    return runCatching {
        val uri = Uri.parse(rawUri)
        when (uri.scheme?.lowercase()) {
            "content" -> {
                val resolver = context.contentResolver
                resolver.delete(uri, null, null) > 0 ||
                    resolver.openAssetFileDescriptor(uri, "r")?.use { false } == null
            }
            "file" -> uri.path?.let(::File)?.let { file -> !file.exists() || file.delete() } ?: true
            else -> File(rawUri).let { file -> !file.exists() || file.delete() }
        }
    }.getOrDefault(false)
}
