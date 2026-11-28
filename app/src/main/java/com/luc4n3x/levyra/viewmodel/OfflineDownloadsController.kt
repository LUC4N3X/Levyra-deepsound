package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.luc4n3x.levyra.data.SmartOfflineOwnershipGate
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.collections.isPersonalCollectionTrack
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.DownloadedTracksDao
import com.luc4n3x.levyra.data.local.OfflineDownloadTasksDao
import com.luc4n3x.levyra.data.locallibrary.downloadArtworkModel
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.BatchDownload
import com.luc4n3x.levyra.domain.BatchDownloadKind
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.DownloadQualityChoice
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.OfflineLaneProgress
import com.luc4n3x.levyra.domain.OfflineTransferState
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.activeBatchDownload
import com.luc4n3x.levyra.domain.batchDownloadKey
import com.luc4n3x.levyra.domain.batchDownloadKindOf
import com.luc4n3x.levyra.domain.batchDownloadProgress
import com.luc4n3x.levyra.domain.batchDownloadState
import com.luc4n3x.levyra.domain.shouldSkipExistingDownload
import com.luc4n3x.levyra.domain.withDownloadQualityChoice
import com.luc4n3x.levyra.feature.jam.JamRole
import com.luc4n3x.levyra.player.offline.DownloadQualityProbe
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import com.luc4n3x.levyra.player.offline.work.OfflineDownloadBatchRef
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.artistCatalogCopy
import com.luc4n3x.levyra.ui.i18n.smartOfflineCopy
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private val ACTIVE_DOWNLOAD_STATES = setOf("QUEUED", "RUNNING", "PAUSED", "RETRYING")
private const val DOWNLOAD_QUALITY_PROBE_TTL_MS = 5L * 60L * 1_000L

internal interface OfflineDownloadsHost {
    fun cleanUserError(error: Throwable): String
    fun cleanUserError(message: String?): String
    fun recordSmartDownload(track: Track)
    fun playOfflineQueue(tracks: List<Track>, startIndex: Int)
}

internal class OfflineDownloadsController(
    private val scope: CoroutineScope,
    private val context: Context,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val offlineTransfers: MutableStateFlow<OfflineTransferState>,
    private val downloadedTracksDao: DownloadedTracksDao,
    private val offlineDownloadTasksDao: OfflineDownloadTasksDao,
    private val offlineExporter: OfflineAudioExporter,
    private val downloadQualityProbe: DownloadQualityProbe,
    private val host: OfflineDownloadsHost
) {
    private val downloadedMediaSizeCache = ConcurrentHashMap<String, Long>()
    private val downloadRefreshGeneration = MutableStateFlow(0L)
    private val _downloadQualityPicker = MutableStateFlow<DownloadQualityPickerState?>(null)
    val downloadQualityPicker: StateFlow<DownloadQualityPickerState?> = _downloadQualityPicker.asStateFlow()
    private val _pendingBatchCancel = MutableStateFlow<BatchDownload?>(null)
    val pendingBatchCancel: StateFlow<BatchDownload?> = _pendingBatchCancel.asStateFlow()
    private var downloadQualityProbeJob: Job? = null
    var lastDownloadQualityProbe: CachedDownloadQualityProbe? = null
    private val batchEnqueueJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val selectionEnqueueJobs: MutableSet<Job> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val activeDownloadKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val activeDownloadTitles = ConcurrentHashMap<String, String>()
    private val activeDownloadTracks = ConcurrentHashMap<String, Track>()

    fun observeDownloadBatches() {
        scope.launch {
            offlineDownloadTasksDao.observeBatches().collectLatest { rows ->
                val batches = rows.map { row ->
                    BatchDownload(
                        key = row.batchKey,
                        kind = batchDownloadKindOf(row.batchKind),
                        title = row.batchTitle,
                        artworkUrl = row.batchArtworkUrl,
                        total = row.total,
                        completed = row.completed,
                        failed = row.failed,
                        active = row.active,
                        progress = batchDownloadProgress(row.total, row.progressSum),
                        state = batchDownloadState(row.total, row.completed, row.failed, row.active, row.progressSum)
                    )
                }
                uiState.update { it.copy(downloadBatches = batches) }
            }
        }
    }

    fun observeDownloadTasks() {
        scope.launch {
            offlineDownloadTasksDao.observeLanes().collectLatest { rows ->
                val lanes = rows.associate { row ->
                    DownloadOwnership.fromStorage(row.ownership) to OfflineLaneProgress.of(row.total, row.completed, row.progressSum)
                }
                offlineTransfers.update { it.copy(lanes = lanes) }
            }
        }
        scope.launch {
            offlineDownloadTasksDao.observeActive().collectLatest { tasks ->
                val domainTasks = tasks.map { task ->
                    OfflineDownloadTask(
                        taskKey = task.taskKey,
                        trackId = task.trackId,
                        title = task.title,
                        artist = task.artist,
                        state = task.state,
                        progress = task.progress,
                        error = task.error,
                        ownership = DownloadOwnership.fromStorage(task.ownership)
                    )
                }
                val runningTasks = domainTasks.filter { it.state in setOf("QUEUED", "RUNNING", "RETRYING") }
                val ids = runningTasks.mapTo(linkedSetOf()) { it.taskKey }
                val progress = runningTasks.associate { it.taskKey to it.progress.coerceIn(0, 99) }
                val titles = runningTasks.associate { it.taskKey to it.title.ifBlank { "brano" } }
                uiState.update {
                    it.copy(
                        downloadQueue = domainTasks,
                        offlineQueueSize = runningTasks.size,
                        isOfflineExporting = runningTasks.isNotEmpty(),
                        downloadingTrackIds = ids,
                        downloadProgressByTrackId = progress,
                        downloadTitleByTrackId = titles
                    )
                }
            }
        }
    }

    fun observeDownloads() {
        scope.launch {
            downloadRefreshGeneration.collectLatest {
                downloadedMediaSizeCache.keys.removeIf { uri ->
                    !uri.startsWith("content:", ignoreCase = true) || uri.startsWith("content://${MediaStore.AUTHORITY}/")
                }
                downloadedTracksDao.observeAll().collectLatest { entities ->
                    val mapped = withContext(Dispatchers.IO) {
                        entities.mapNotNull { entity ->
                            ensureActive()
                            downloadedMediaSize(entity.uri)?.let { size -> entity.toDownloadedTrack(size) }
                        }
                    }
                    uiState.update {
                        it.copy(
                            downloads = mapped,
                            downloadStorageBytes = mapped.sumOf { item -> item.sizeBytes.coerceAtLeast(0L) },
                            downloadedTrackIds = mapped.map { item -> item.trackId }.filter(String::isNotBlank).toSet()
                        )
                    }
                }
            }
        }
    }

    fun refreshDownloads() {
        downloadRefreshGeneration.update { it + 1 }
    }

    private fun DownloadEntity.toDownloadedTrack(sizeBytes: Long): DownloadedTrack = DownloadedTrack(
        id = id,
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        fileName = fileName,
        uri = uri,
        mimeType = mimeType,
        embeddedMetadata = embeddedMetadata,
        savedAt = savedAt,
        sizeBytes = sizeBytes,
        ownership = DownloadOwnership.fromStorage(ownership),
        artworkUrl = artworkUrl
    )

    private fun downloadedMediaSize(rawUri: String): Long? {
        if (rawUri.isBlank()) return 0L
        downloadedMediaSizeCache[rawUri]?.let { return it }
        val size = readDownloadedMediaSize(Uri.parse(rawUri), rawUri) ?: return null
        if (size > 0L && !com.luc4n3x.levyra.player.offline.isOfflineAudioStoreUri(rawUri)) downloadedMediaSizeCache[rawUri] = size
        return size
    }

    private fun readDownloadedMediaSize(uri: Uri, rawUri: String): Long? =
        try {
            when (uri.scheme?.lowercase()) {
                "content" -> context.contentResolver
                    .openAssetFileDescriptor(uri, "r")
                    ?.use { descriptor -> descriptor.length.coerceAtLeast(0L) }
                    ?: 0L
                "file" -> uri.path?.let(::File)?.let(::downloadedFileSize)
                null -> downloadedFileSize(File(rawUri))
                else -> 0L
            }
        } catch (_: FileNotFoundException) {
            if (uri.authority == MediaStore.AUTHORITY) null else 0L
        } catch (_: Exception) {
            0L
        }

    private fun downloadedFileSize(file: File): Long? = when {
        file.isFile -> file.length()
        file.isAbsolute && Files.notExists(file.toPath()) -> null
        else -> 0L
    }

    private fun deleteDownloadedMedia(download: DownloadedTrack): Boolean {
        if (download.uri.isBlank()) return true
        return runCatching {
            val uri = Uri.parse(download.uri)
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    val resolver = context.contentResolver
                    val deletedRows = try {
                        resolver.delete(uri, null, null)
                    } catch (error: SecurityException) {
                        if (uri.authority != MediaStore.AUTHORITY) throw error
                        0
                    }
                    deletedRows > 0 || if (uri.authority == MediaStore.AUTHORITY) {
                        readDownloadedMediaSize(uri, download.uri) == null
                    } else {
                        resolver.openAssetFileDescriptor(uri, "r")?.use { false } ?: true
                    }
                }
                "file" -> uri.path?.let(::File)?.let { file -> !file.exists() || file.delete() } ?: true
                else -> File(download.uri).let { file -> !file.exists() || file.delete() }
            }
        }.getOrDefault(false)
    }

    fun pauseDownload(taskKey: String) {
        scope.launch(Dispatchers.IO) {
            OfflineExportWorker.pause(context, taskKey)
        }
    }

    fun resumeDownload(taskKey: String) {
        scope.launch(Dispatchers.IO) {
            OfflineExportWorker.resume(context, taskKey)
        }
    }

    fun cancelDownload(taskKey: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                OfflineExportWorker.cancel(context, taskKey)
            }
            handleOfflineExportStopped(taskKey)
        }
    }

    fun autoDownloadFavorite(track: Track, becameFavorite: Boolean) {
        val current = uiState.value
        val eligible = com.luc4n3x.levyra.player.PlaybackAutomationPolicy.shouldAutoDownloadFavorite(
            enabled = current.automationSettings.autoDownloadFavorites,
            becameFavorite = becameFavorite,
            trackId = track.id,
            downloadedTrackIds = current.downloadedTrackIds,
            downloadingKeys = current.downloadingTrackIds,
            downloadKey = downloadKeyFor(track)
        )
        if (!eligible) return
        startTrackDownload(track, null)
    }

    fun retryBatchDownload(batchKey: String) {
        if (batchKey.isBlank()) return
        scope.launch {
            val appContext = context
            withContext(Dispatchers.IO) {
                offlineDownloadTasksDao.batchTasks(batchKey)
                    .filter { it.state == "FAILED" }
                    .forEach { task ->
                        OfflineExportWorker.enqueue(
                            context = appContext,
                            trackId = task.taskKey,
                            trackPayload = task.payload,
                            batch = OfflineDownloadBatchRef(
                                key = task.batchKey,
                                title = task.batchTitle,
                                kind = task.batchKind,
                                artworkUrl = task.batchArtworkUrl,
                                position = task.batchPosition
                            )
                        )
                    }
            }
        }
    }

    fun cancelBatchDownload(batchKey: String) {
        if (batchKey.isBlank()) return
        scope.launch {
            val appContext = context
            batchEnqueueJobs.remove(batchKey)?.cancelAndJoin()
            withContext(Dispatchers.IO) {
                offlineDownloadTasksDao.batchTasks(batchKey)
                    .filter { it.state in ACTIVE_DOWNLOAD_STATES }
                    .forEach { task ->
                        runCatchingPreservingCancellation { OfflineExportWorker.cancel(appContext, task.taskKey) }
                            .onFailure { Timber.w(it, "batch child cancel failed") }
                    }
                offlineDownloadTasksDao.deleteBatch(batchKey)
            }
        }
    }

    fun cancelAllDownloads() {
        selectionEnqueueJobs.toList().forEach { it.cancel() }
        val snapshot = uiState.value
        (snapshot.downloadBatches.filterNot { it.isFinished }.map { it.key } + batchEnqueueJobs.keys)
            .toSet()
            .forEach(::cancelBatchDownload)
        snapshot.downloadQueue
            .filter { it.state in ACTIVE_DOWNLOAD_STATES }
            .forEach { cancelDownload(it.taskKey) }
    }

    fun dismissBatchCancel() {
        _pendingBatchCancel.value = null
    }

    fun confirmBatchCancel() {
        val batch = _pendingBatchCancel.value ?: return
        _pendingBatchCancel.value = null
        if (activeBatchDownload(uiState.value.downloadBatches, batch.key) != null || batchEnqueueJobs[batch.key]?.isActive == true) {
            cancelBatchDownload(batch.key)
        }
    }

    fun startBatchDownload(
        kind: BatchDownloadKind,
        canonicalId: String,
        title: String,
        artworkUrl: String,
        tracks: List<Track>
    ) {
        val batchKey = batchDownloadKey(kind, canonicalId, title)
        if (batchKey.isBlank()) {
            exportTracksSequential(tracks, title)
            return
        }
        val currentState = uiState.value
        activeBatchDownload(currentState.downloadBatches, batchKey)?.let { running ->
            _pendingBatchCancel.value = running
            return
        }
        val pending = tracks
            .filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() || it.title.isNotBlank() }
            .distinctBy { downloadKeyFor(it) }
            .filterNot { track ->
                val existing = currentState.downloads.firstOrNull { it.trackId == track.id }
                existing?.ownership != DownloadOwnership.SMART_OFFLINE &&
                    currentState.downloadSettings.shouldSkipExistingDownload(
                    trackId = track.id,
                    downloadedTrackIds = currentState.downloadedTrackIds
                )
            }
        val strings = LevyraStrings.forCode(currentState.languageCode)
        if (pending.isEmpty()) {
            uiState.update { it.copy(offlineExportMessage = strings.alreadyOffline) }
            return
        }
        if (batchEnqueueJobs[batchKey]?.isActive == true) return
        batchEnqueueJobs[batchKey] = scope.launch {
            val appContext = context
            uiState.update { it.copy(offlineExportMessage = strings.artistCatalogCopy().downloadStartedHint) }
            pending.forEachIndexed { index, track ->
                val downloadKey = downloadKeyFor(track)
                val enqueued = runCatchingPreservingCancellation {
                    withContext(Dispatchers.IO) {
                        OfflineExportWorker.enqueue(
                            context = appContext,
                            trackId = downloadKey,
                            trackPayload = TrackPayloadCodec.encode(track),
                            batch = OfflineDownloadBatchRef(
                                key = batchKey,
                                title = title,
                                kind = kind.name,
                                artworkUrl = artworkUrl,
                                position = index
                            )
                        )
                    }
                }.onFailure { error -> Timber.e(error, "batch download enqueue failed") }
                if (enqueued.isSuccess) host.recordSmartDownload(track)
            }
        }.also { job -> job.invokeOnCompletion { batchEnqueueJobs.remove(batchKey, job) } }
    }

    fun exportTracksSequential(tracks: List<Track>, label: String) {
        val currentState = uiState.value
        val pending = tracks
            .filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() || it.title.isNotBlank() }
            .distinctBy { downloadKeyFor(it) }
            .filterNot { track ->
                val existing = currentState.downloads.firstOrNull { it.trackId == track.id }
                existing?.ownership != DownloadOwnership.SMART_OFFLINE &&
                    currentState.downloadSettings.shouldSkipExistingDownload(
                    trackId = track.id,
                    downloadedTrackIds = currentState.downloadedTrackIds
                )
            }
        if (pending.isEmpty()) {
            uiState.update { it.copy(offlineExportMessage = "Già tutto offline") }
            return
        }
        scope.launch {
            uiState.update { it.copy(offlineExportMessage = "$label in coda: ${pending.size} brani") }
            pending.forEachIndexed { index, track ->
                if (index > 0) delay(220L)
                startTrackDownload(track, null)
            }
        }.also { job ->
            selectionEnqueueJobs += job
            job.invokeOnCompletion { selectionEnqueueJobs -= job }
        }
    }

    private fun tryPromoteSmartDownload(track: Track): Boolean {
        val smartDownload = uiState.value.downloads.firstOrNull { download ->
            download.trackId == track.id && download.ownership == DownloadOwnership.SMART_OFFLINE
        } ?: return false
        scope.launch {
            val promoted = withContext(Dispatchers.IO) {
                SmartOfflineOwnershipGate.withLock {
                    downloadedTracksDao.promoteToManual(smartDownload.id) > 0
                }
            }
            if (promoted) {
                uiState.update { current ->
                    current.copy(
                        downloads = current.downloads.map { download ->
                            if (download.id == smartDownload.id) {
                                download.copy(ownership = DownloadOwnership.MANUAL)
                            } else {
                                download
                            }
                        },
                        offlineExportMessage =
                            "${LevyraStrings.forCode(current.languageCode).smartOfflineCopy().protectedManual}: ${track.title}"
                    )
                }
            }
        }
        return true
    }

    fun exportTrack(track: Track) {
        if (tryPromoteSmartDownload(track)) return
        if (track.isPersonalCollectionTrack()) {
            startTrackDownload(track, null)
            return
        }
        val downloadKey = downloadKeyFor(track)
        val currentState = uiState.value
        val strings = LevyraStrings.forCode(currentState.languageCode)
        if (downloadKey in currentState.downloadingTrackIds || downloadKey in activeDownloadKeys) {
            uiState.update { it.copy(offlineExportMessage = "${strings.downloadInProgress}: ${track.title}") }
            return
        }
        if (currentState.downloadSettings.shouldSkipExistingDownload(track.id, currentState.downloadedTrackIds)) {
            uiState.update { it.copy(offlineExportMessage = "${strings.alreadyOffline}: ${track.title}") }
            return
        }
        openDownloadQualityPicker(track, downloadKey)
    }

    private fun openDownloadQualityPicker(track: Track, downloadKey: String) {
        val open = _downloadQualityPicker.value
        if (open != null && downloadKeyFor(open.track) == downloadKey) return
        downloadQualityProbeJob?.cancel()
        val cached = lastDownloadQualityProbe?.takeIf {
            it.downloadKey == downloadKey &&
                it.snapshot.options.isNotEmpty() &&
                System.currentTimeMillis() - it.probedAtMs < DOWNLOAD_QUALITY_PROBE_TTL_MS
        }
        val picker = DownloadQualityPickerState(track)
        _downloadQualityPicker.value = cached?.let { picker.withSnapshot(it.snapshot) } ?: picker
        if (cached == null) probeDownloadQualities(track, downloadKey)
    }

    private fun probeDownloadQualities(track: Track, downloadKey: String) {
        downloadQualityProbeJob?.cancel()
        downloadQualityProbeJob = scope.launch {
            downloadQualityProbe.probe(track, uiState.value.downloadSettings).collect { snapshot ->
                _downloadQualityPicker.update { picker ->
                    picker?.takeIf { downloadKeyFor(it.track) == downloadKey }?.withSnapshot(snapshot) ?: picker
                }
                if (snapshot.pending == 0 && snapshot.failed == 0) {
                    lastDownloadQualityProbe = CachedDownloadQualityProbe(downloadKey, snapshot, System.currentTimeMillis())
                }
            }
        }
    }

    fun selectDownloadQuality(choice: DownloadQualityChoice) {
        _downloadQualityPicker.update { it?.select(choice) }
    }

    fun retryDownloadQualityProbe() {
        val picker = _downloadQualityPicker.value ?: return
        val downloadKey = downloadKeyFor(picker.track)
        lastDownloadQualityProbe = null
        _downloadQualityPicker.value = DownloadQualityPickerState(picker.track)
        probeDownloadQualities(picker.track, downloadKey)
    }

    fun dismissDownloadQualityPicker() {
        downloadQualityProbeJob?.cancel()
        downloadQualityProbeJob = null
        _downloadQualityPicker.value = null
    }

    fun confirmDownloadQuality() {
        val picker = _downloadQualityPicker.value ?: return
        val choice = picker.selected ?: return
        dismissDownloadQualityPicker()
        lastDownloadQualityProbe = null
        startTrackDownload(picker.track, choice)
    }

    private fun startTrackDownload(track: Track, qualityChoice: DownloadQualityChoice?) {
        if (tryPromoteSmartDownload(track)) return
        val downloadKey = downloadKeyFor(track)
        if (downloadKey in uiState.value.downloadingTrackIds || !activeDownloadKeys.add(downloadKey)) {
            uiState.update { it.copy(offlineExportMessage = "Download già in corso: ${track.title}") }
            return
        }
        val currentState = uiState.value
        if (currentState.downloadSettings.shouldSkipExistingDownload(track.id, currentState.downloadedTrackIds)) {
            activeDownloadKeys.remove(downloadKey)
            uiState.update { it.copy(offlineExportMessage = "Già scaricato: ${track.title}") }
            return
        }
        val downloadTitle = track.title.ifBlank { "brano" }
        activeDownloadTitles[downloadKey] = downloadTitle
        activeDownloadTracks[downloadKey] = track
        uiState.update {
            it.copy(
                isOfflineExporting = true,
                offlineExportMessage = null,
                downloadingTrackIds = it.downloadingTrackIds + downloadKey,
                downloadProgressByTrackId = it.downloadProgressByTrackId + (downloadKey to 1),
                downloadTitleByTrackId = it.downloadTitleByTrackId + (downloadKey to downloadTitle)
            )
        }
        scope.launch {
            val result = runCatching {
                val appContext = context
                val payload = withDownloadQualityChoice(TrackPayloadCodec.encode(track), qualityChoice)
                val workId = withContext(Dispatchers.IO) {
                    OfflineExportWorker.enqueue(appContext, downloadKey, payload)
                }
                val workManager = WorkManager.getInstance(appContext)
                var finished: WorkInfo? = null
                while (isActive && finished == null) {
                    val info = withContext(Dispatchers.IO) { workManager.getWorkInfoById(workId).get() }
                    if (info != null && info.state.isFinished) {
                        finished = info
                    } else {
                        info?.let { updateDownloadProgress(downloadKey, it.progress.getInt(OfflineExportWorker.KEY_PROGRESS, 0)) }
                        delay(500L)
                    }
                }
                finished ?: throw CancellationException("Offline export observation cancelled")
            }
            result.onSuccess { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> handleOfflineExportSuccess(workInfo, downloadKey)
                    WorkInfo.State.FAILED -> {
                        val reason = workInfo.outputData.getString(OfflineExportWorker.KEY_ERROR)
                        if (reason == OfflineExportWorker.ERROR_SUPERSEDED) {
                            handleOfflineExportStopped(downloadKey)
                        } else {
                            handleOfflineExportFailure(host.cleanUserError(reason), downloadKey)
                        }
                    }
                    WorkInfo.State.CANCELLED -> handleOfflineExportStopped(downloadKey)
                    else -> handleOfflineExportFailure(null, downloadKey)
                }
            }.onFailure { error ->
                if (error is CancellationException) {
                    activeDownloadKeys.remove(downloadKey)
                    activeDownloadTracks.remove(downloadKey)
                    activeDownloadTitles.remove(downloadKey)
                    uiState.update {
                        it.copy(
                            downloadingTrackIds = it.downloadingTrackIds - downloadKey,
                            downloadProgressByTrackId = it.downloadProgressByTrackId - downloadKey,
                            downloadTitleByTrackId = it.downloadTitleByTrackId - downloadKey
                        )
                    }
                    throw error
                }
                Timber.e(error, "Offline export work failed")
                handleOfflineExportFailure(host.cleanUserError(error), downloadKey)
            }
        }
    }

    fun deleteDownload(download: DownloadedTrack) {
        deleteDownloads(listOf(download))
    }

    fun deleteTrackDownload(trackId: String) {
        if (trackId.isBlank()) return
        deleteDownloads(uiState.value.downloads.filter { it.trackId == trackId })
    }

    fun deleteDownloads(downloads: List<DownloadedTrack>) {
        val uniqueDownloads = downloads.distinctBy { it.id }
        if (uniqueDownloads.isEmpty()) return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                var deleted = 0
                val failed = mutableListOf<String>()
                uniqueDownloads.forEach { download ->
                    if (deleteDownloadedMedia(download)) {
                        runCatching { downloadedTracksDao.deleteById(download.id) }
                            .onSuccess {
                                downloadedMediaSizeCache.remove(download.uri)
                                deleted++
                            }
                            .onFailure { failed += download.title }
                    } else {
                        failed += download.title
                    }
                }
                deleted to failed
            }
            val (deleted, failed) = result
            uiState.update { current ->
                current.copy(
                    offlineExportMessage = when {
                        failed.isEmpty() -> "Eliminati dal dispositivo: $deleted"
                        deleted > 0 -> "Eliminati $deleted file; ${failed.size} non rimossi"
                        else -> "Impossibile eliminare i file selezionati"
                    }
                )
            }
        }
    }

    private fun handleOfflineExportSuccess(workInfo: WorkInfo, trackId: String) {
        activeDownloadKeys.remove(trackId)
        val completedTrack = activeDownloadTracks.remove(trackId)
        activeDownloadTitles.remove(trackId)
        val fileName = workInfo.outputData.getString(OfflineExportWorker.KEY_FILE_NAME).orEmpty()
        val destinationLabel = workInfo.outputData.getString(OfflineExportWorker.KEY_DESTINATION_LABEL).orEmpty()
        val embedded = workInfo.outputData.getBoolean(OfflineExportWorker.KEY_EMBEDDED_METADATA, false)
        val strings = LevyraStrings.forCode(uiState.value.languageCode)
        completedTrack?.let { host.recordSmartDownload(it) }
        uiState.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                offlineExportMessage = strings.formatOfflineExportSaved(
                    fileName = fileName,
                    embeddedMetadata = embedded,
                    destination = destinationLabel
                ),
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId,
                downloadTitleByTrackId = it.downloadTitleByTrackId - trackId
            )
        }
    }

    private fun handleOfflineExportStopped(trackId: String) {
        activeDownloadKeys.remove(trackId)
        activeDownloadTracks.remove(trackId)
        activeDownloadTitles.remove(trackId)
        uiState.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId,
                downloadTitleByTrackId = it.downloadTitleByTrackId - trackId
            )
        }
    }

    private fun handleOfflineExportFailure(message: String?, trackId: String) {
        activeDownloadKeys.remove(trackId)
        activeDownloadTracks.remove(trackId)
        activeDownloadTitles.remove(trackId)
        uiState.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                offlineExportMessage = message ?: host.cleanUserError(null),
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId,
                downloadTitleByTrackId = it.downloadTitleByTrackId - trackId
            )
        }
    }

    private fun updateDownloadProgress(trackId: String, progress: Int) {
        uiState.update {
            if (trackId !in it.downloadingTrackIds) {
                it
            } else {
                val safeProgress = monotonicDownloadProgress(it.downloadProgressByTrackId[trackId], progress)
                it.copy(
                    downloadProgressByTrackId = it.downloadProgressByTrackId + (trackId to safeProgress)
                )
            }
        }
    }

    private fun downloadKeyFor(track: Track): String {
        val raw = track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}:${track.title}" } }
        return raw.ifBlank { "unknown-${track.hashCode()}" }
    }

    fun playDownloaded(download: DownloadedTrack) {
        if (download.uri.isBlank()) {
            uiState.update { it.copy(playerError = "File offline non disponibile") }
            return
        }
        host.playOfflineQueue(listOf(offlinePlaybackTrack(download)), 0)
    }

    fun playDownloads(downloads: List<DownloadedTrack>, shuffled: Boolean) {
        val tracks = downloads.filter { it.uri.isNotBlank() }.map(::offlinePlaybackTrack)
        if (tracks.isEmpty()) return
        if (uiState.value.jam.role == JamRole.Guest) return
        val ordered = if (shuffled) tracks.shuffled() else tracks
        host.playOfflineQueue(ordered, 0)
    }

    private fun offlinePlaybackTrack(download: DownloadedTrack): Track =
        Track(
            id = download.trackId.ifBlank { "offline-${download.id}" },
            title = download.title,
            artist = download.artist,
            album = download.album,
            durationMs = download.durationMs,
            streamUrl = download.uri,
            videoUrl = "",
            thumbnailUrl = downloadArtworkModel(download.uri, download.artworkUrl),
            largeThumbnailUrl = "",
            source = "Offline",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
}
