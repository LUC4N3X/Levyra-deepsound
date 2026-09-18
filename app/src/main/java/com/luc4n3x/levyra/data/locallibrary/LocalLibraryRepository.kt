package com.luc4n3x.levyra.data.locallibrary

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.LocalMediaEntity
import com.luc4n3x.levyra.data.reconcileDownloadedTracks
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

enum class LocalScanMode { Quick, Full, RebuildLevyra }

data class LocalScanResult(
    val mode: LocalScanMode,
    val added: Int = 0,
    val updated: Int = 0,
    val missing: Int = 0,
    val removed: Int = 0,
    val moved: Int = 0,
    val restoredDownloads: Int = 0,
    val skippedUnchanged: Boolean = false,
    val permissionDenied: Boolean = false,
    val failed: Boolean = false,
    val finishedAt: Long = 0L
)

data class LocalLibraryStatus(
    val permissionGranted: Boolean = false,
    val scanning: Boolean = false,
    val runningMode: LocalScanMode? = null,
    val lastResult: LocalScanResult? = null,
    val lastScanAt: Long = 0L,
    val excludedFolders: Set<String> = emptySet()
)

class LocalLibraryRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = LevyraDatabase.get(appContext)
    private val dao = database.localMediaDao()
    private val scanner = LocalMediaStoreScanner(appContext)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    private val requestLock = Any()
    private var activeJob: Job? = null
    private var pendingMode: LocalScanMode? = null
    private var loopRunning: Boolean = false
    private val _status = MutableStateFlow(
        LocalLibraryStatus(
            permissionGranted = scanner.hasPermission(),
            lastScanAt = preferences.getLong(KEY_LAST_SCAN_AT, 0L),
            excludedFolders = preferences.getStringSet(KEY_EXCLUDED_FOLDERS, emptySet()).orEmpty().toSet()
        )
    )

    val status: StateFlow<LocalLibraryStatus> = _status.asStateFlow()

    val availableMedia: Flow<List<LocalMediaEntity>> = dao.observeAvailable()

    fun refreshPermission(): Boolean {
        val granted = scanner.hasPermission()
        _status.update { it.copy(permissionGranted = granted) }
        return granted
    }

    fun requestScan(mode: LocalScanMode, force: Boolean = false): Job = synchronized(requestLock) {
        val running = activeJob
        if (running != null && running.isActive && loopRunning) {
            pendingMode = strongerLocalScanMode(pendingMode, mode)
            return running
        }
        pendingMode = null
        loopRunning = true
        scope.launch { runScanLoop(mode, force) }.also { activeJob = it }
    }

    fun setFolderExcluded(folderKey: String, excluded: Boolean) {
        val key = folderKey.trim().lowercase(Locale.ROOT)
        if (key.isEmpty()) return
        val current = _status.value.excludedFolders
        val next = if (excluded) current + key else current - key
        if (next == current) return
        preferences.edit().putStringSet(KEY_EXCLUDED_FOLDERS, next).remove(KEY_CHANGE_TOKEN).apply()
        _status.update { it.copy(excludedFolders = next) }
        requestScan(LocalScanMode.Quick, force = true)
    }

    suspend fun unavailableContentUris(uris: Collection<String>): Set<String> {
        val candidates = uris.filter { it.startsWith("content://", ignoreCase = true) }.distinct()
        if (candidates.isEmpty()) return emptySet()
        return candidates.chunked(SQL_CHUNK).flatMapTo(hashSetOf()) { dao.unavailableAmong(it) }
    }

    private suspend fun runScanLoop(initialMode: LocalScanMode, initialForce: Boolean) {
        var mode = initialMode
        var force = initialForce
        while (true) {
            runScan(mode, force)
            mode = synchronized(requestLock) {
                val next = pendingMode
                pendingMode = null
                if (next == null) loopRunning = false
                next
            } ?: return
            force = true
        }
    }

    private suspend fun runScan(mode: LocalScanMode, force: Boolean) = scanMutex.withLock {
        _status.update { it.copy(scanning = true, runningMode = mode) }
        val result = try {
            performScan(mode, force)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Local library scan failed")
            LocalScanResult(mode = mode, failed = true, finishedAt = System.currentTimeMillis())
        } finally {
            _status.update { it.copy(scanning = false, runningMode = null) }
        }
        _status.update { current ->
            current.copy(
                lastResult = result,
                lastScanAt = if (result.failed || result.permissionDenied) current.lastScanAt else result.finishedAt
            )
        }
    }

    private suspend fun performScan(mode: LocalScanMode, force: Boolean): LocalScanResult {
        val now = System.currentTimeMillis()
        if (!refreshPermission()) {
            return LocalScanResult(mode = mode, permissionDenied = true, finishedAt = now)
        }
        val volumes = scanner.mountedVolumes()
        val changeVersion = scanner.changeToken(volumes)
        val excluded = _status.value.excludedFolders
        val tokenKey = changeVersion?.let { "$it|${excluded.sorted().joinToString(",")}" }
        val previousToken = preferences.getString(KEY_CHANGE_TOKEN, null)
        if (mode == LocalScanMode.Quick && !force && tokenKey != null && tokenKey == previousToken && dao.count() > 0) {
            return LocalScanResult(mode = mode, skippedUnchanged = true, finishedAt = now)
        }
        val scanned = scanner.scan(volumes)
            ?: return LocalScanResult(
                mode = mode,
                permissionDenied = !refreshPermission(),
                failed = true,
                finishedAt = now
            )
        val downloadTrackIds = database.downloadedTracksDao().all()
            .mapNotNull { download ->
                val identityKey = mediaStoreIdentityFromUri(download.uri) ?: return@mapNotNull null
                download.trackId.takeIf { it.isNotBlank() }?.let { identityKey to it }
            }
            .toMap()
        val candidates = scanned
            .map { audio ->
                audio.toLocalMediaEntity(
                    downloadTrackIds[localIdentityKey(audio.volumeName, audio.mediaStoreId)].orEmpty(),
                    now
                )
            }
            .filter { localFolderAllowed(it.folderKey, excluded) }
        val plan = planLocalLibraryReconcile(
            existing = dao.all(),
            scanned = candidates,
            mountedVolumes = volumes,
            fullScan = mode != LocalScanMode.Quick,
            now = now
        )
        if (!plan.isEmpty) {
            dao.apply(plan.inserts, plan.updates, plan.missingIds, plan.deleteIds, now)
        }
        val restored = if (mode == LocalScanMode.RebuildLevyra) rebuildLevyraDownloads() else 0
        preferences.edit()
            .putString(KEY_CHANGE_TOKEN, tokenKey)
            .putLong(KEY_LAST_SCAN_AT, now)
            .apply()
        return LocalScanResult(
            mode = mode,
            added = plan.inserts.size,
            updated = plan.updates.size - plan.movedCount,
            missing = plan.missingIds.size,
            removed = plan.deleteIds.size,
            moved = plan.movedCount,
            restoredDownloads = restored,
            finishedAt = System.currentTimeMillis()
        )
    }

    private suspend fun rebuildLevyraDownloads(): Int {
        val levyraRows = dao.all().filter { it.available && it.isLevyraDownload }
        val identified = levyraRows.map { row ->
            if (row.levyraTrackId.isNotEmpty() || !isMp4Family(row)) {
                row
            } else {
                readLevyraTrackId(row)?.let { trackId -> row.copy(levyraTrackId = trackId) } ?: row
            }
        }
        val newlyIdentified = identified.filterIndexed { index, row ->
            row.levyraTrackId != levyraRows[index].levyraTrackId
        }
        val downloadsDao = database.downloadedTracksDao()
        val current = downloadsDao.all()
        val merged = reconcileDownloadedTracks(
            current,
            identified.filter { it.levyraTrackId.isNotBlank() }.map(::toDownloadEntity),
            ::contentReadable
        )
        val changed = merged.map(::downloadSignature).toSet() != current.map(::downloadSignature).toSet()
        if (newlyIdentified.isEmpty() && !changed) return 0
        database.withTransaction {
            newlyIdentified.chunked(SQL_CHUNK).forEach { dao.updateAll(it) }
            if (changed) downloadsDao.replaceAll(merged)
        }
        val previousUris = current.mapTo(hashSetOf()) { it.uri }
        return merged.count { it.uri !in previousUris }
    }

    private fun readLevyraTrackId(row: LocalMediaEntity): String? = runCatching {
        appContext.contentResolver.openAssetFileDescriptor(Uri.parse(row.contentUri), "r")?.use { descriptor ->
            descriptor.createInputStream().buffered(TAG_READ_BUFFER).use { input ->
                LevyraTagIdentityReader.readTrackId(input, descriptor.length)
            }
        }
    }.onFailure { Timber.d(it, "Levyra tag identity unavailable") }.getOrNull()

    private fun contentReadable(rawUri: String): Boolean {
        if (rawUri.isBlank()) return false
        return runCatching {
            val uri = Uri.parse(rawUri)
            when (uri.scheme?.lowercase(Locale.ROOT)) {
                "content" -> appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length != 0L } ?: false
                "file" -> uri.path?.let(::File)?.let { it.isFile && it.length() > 0L } == true
                else -> File(rawUri).let { it.isFile && it.length() > 0L }
            }
        }.getOrDefault(false)
    }

    companion object {
        private const val PREFERENCES_NAME = "levyra_local_library"
        private const val KEY_CHANGE_TOKEN = "change_token"
        private const val KEY_LAST_SCAN_AT = "last_scan_at"
        private const val KEY_EXCLUDED_FOLDERS = "excluded_folders"
        private const val SQL_CHUNK = 400
        private const val TAG_READ_BUFFER = 64 * 1024

        @Volatile
        private var instance: LocalLibraryRepository? = null

        fun get(context: Context): LocalLibraryRepository = instance ?: synchronized(this) {
            instance ?: LocalLibraryRepository(context.applicationContext).also { instance = it }
        }
    }
}

internal fun strongerLocalScanMode(current: LocalScanMode?, requested: LocalScanMode): LocalScanMode =
    if (current == null || requested.ordinal > current.ordinal) requested else current

internal fun localFolderAllowed(folderKey: String, excludedFolders: Set<String>): Boolean =
    excludedFolders.none { excluded -> folderKey.startsWith(excluded) }

private fun isMp4Family(row: LocalMediaEntity): Boolean {
    val mime = row.mimeType.lowercase(Locale.ROOT)
    val name = row.displayName.lowercase(Locale.ROOT)
    return mime.contains("mp4") || mime.contains("m4a") || name.endsWith(".m4a") || name.endsWith(".mp4")
}

private fun toDownloadEntity(row: LocalMediaEntity) = DownloadEntity(
    trackId = row.levyraTrackId,
    title = row.title,
    artist = row.artist,
    album = row.album,
    durationMs = row.durationMs,
    fileName = row.displayName,
    uri = row.contentUri,
    mimeType = row.mimeType,
    embeddedMetadata = row.levyraTrackId.isNotEmpty(),
    downloadPreset = "",
    downloadQuality = "",
    savedAt = row.dateAddedMs
)

private fun downloadSignature(entity: DownloadEntity): String =
    "${entity.uri}|${entity.trackId}|${entity.title}|${entity.artist}|${entity.album}"
