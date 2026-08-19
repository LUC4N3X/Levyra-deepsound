package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.app.DesktopDiagnostics
import com.luc4n3x.levyra.desktop.core.localmusic.LocalArtworkCache
import com.luc4n3x.levyra.desktop.core.localmusic.LocalFolder
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryIndex
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryScanner
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryStore
import com.luc4n3x.levyra.desktop.core.localmusic.LocalScanProgress
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocalMusicUiState(
    val folders: List<LocalFolder> = emptyList(),
    val index: LocalLibraryIndex = LocalLibraryIndex.EMPTY,
    val scanning: Boolean = false,
    val progress: LocalScanProgress = LocalScanProgress(),
    val lastScanAtMs: Long = 0L,
    val unavailableCount: Int = 0
) {
    val trackCount: Int get() = index.tracks.size
    val hasFolders: Boolean get() = folders.isNotEmpty()
}

class LocalMusicController(
    private val scope: CoroutineScope,
    private val store: LocalLibraryStore,
    paths: AppPaths
) {
    private val scanner = LocalLibraryScanner(LocalArtworkCache(paths.localArtworkCache).prepare())
    private val internalState = MutableStateFlow(LocalMusicUiState())
    private val watchSignals = Channel<Unit>(Channel.CONFLATED)

    private var scanJob: Job? = null
    private var watchJob: Job? = null
    private var debounceJob: Job? = null

    val state: StateFlow<LocalMusicUiState> = internalState.asStateFlow()

    init {
        scope.launch {
            store.data.collect { data ->
                val index = withContext(Dispatchers.Default) { LocalLibraryIndex.of(data.tracks) }
                internalState.value = internalState.value.copy(
                    folders = data.folders,
                    index = index,
                    lastScanAtMs = data.lastScanAtMs,
                    unavailableCount = data.tracks.count { !it.available }
                )
            }
        }
        debounceJob = scope.launch {
            for (signal in watchSignals) {
                delay(WATCH_DEBOUNCE_MS)
                while (watchSignals.tryReceive().isSuccess) {
                    delay(WATCH_DEBOUNCE_MS)
                }
                rescan(deep = false)
            }
        }
        restartWatcher()
        scope.launch {
            delay(STARTUP_SCAN_DELAY_MS)
            if (store.current.folders.isNotEmpty()) {
                rescan(deep = false)
            }
        }
    }

    fun addFolder(path: String): Boolean {
        val folder = store.addFolder(path) ?: return false
        rescan(deep = false)
        restartWatcher()
        return folder.path.isNotEmpty()
    }

    fun removeFolder(folderId: String) {
        store.removeFolder(folderId)
        restartWatcher()
    }

    fun forgetUnavailable() {
        store.forgetUnavailable()
    }

    fun rescan(deep: Boolean) {
        if (scanJob?.isActive == true) return
        val folders = store.current.folders
        if (folders.isEmpty()) {
            store.replaceTracks(emptyList())
            return
        }
        internalState.value = internalState.value.copy(scanning = true, progress = LocalScanProgress())
        scanJob = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    scanner.scan(
                        folders = folders,
                        existing = store.current.tracks,
                        deep = deep,
                        onProgress = { progress ->
                            internalState.value = internalState.value.copy(progress = progress)
                        }
                    )
                }
                store.replaceTracks(result.tracks)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                DesktopDiagnostics.background("local library scan", error)
            } finally {
                internalState.value = internalState.value.copy(
                    scanning = false,
                    progress = LocalScanProgress()
                )
            }
        }
    }

    fun shutdown() {
        debounceJob?.cancel()
        watchJob?.cancel()
        scanJob?.cancel()
        watchSignals.close()
    }

    private fun restartWatcher() {
        watchJob?.cancel()
        val folders = store.current.folders
        if (folders.isEmpty()) return
        watchJob = scope.launch(Dispatchers.IO) {
            val service = runCatching { FileSystems.getDefault().newWatchService() }.getOrNull()
                ?: return@launch
            try {
                var registered = 0
                folders.forEach { folder ->
                    registered += register(service, runCatching { Path.of(folder.path) }.getOrNull(), registered)
                }
                while (isActive) {
                    val key = service.poll(WATCH_POLL_MS, TimeUnit.MILLISECONDS) ?: continue
                    val events = key.pollEvents()
                    key.reset()
                    if (events.isNotEmpty()) {
                        watchSignals.trySend(Unit)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                DesktopDiagnostics.background("local library watcher", error)
            } finally {
                runCatching { service.close() }
            }
        }
    }

    private fun register(service: WatchService, root: Path?, alreadyRegistered: Int): Int {
        if (root == null || !Files.isDirectory(root)) return 0
        var count = 0
        runCatching {
            Files.walk(root, MAX_WATCH_DEPTH).use { stream ->
                stream.filter { Files.isDirectory(it) }
                    .limit((MAX_WATCHED_DIRECTORIES - alreadyRegistered).toLong().coerceAtLeast(0L))
                    .forEach { directory ->
                        runCatching {
                            directory.register(
                                service,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY
                            )
                            count += 1
                        }
                    }
            }
        }
        return count
    }

    private companion object {
        const val STARTUP_SCAN_DELAY_MS = 6_000L
        const val WATCH_DEBOUNCE_MS = 2_500L
        const val WATCH_POLL_MS = 1_000L
        const val MAX_WATCH_DEPTH = 8
        const val MAX_WATCHED_DIRECTORIES = 4_000
    }
}
