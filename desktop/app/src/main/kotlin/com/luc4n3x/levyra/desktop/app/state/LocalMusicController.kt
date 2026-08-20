package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.app.DesktopDiagnostics
import com.luc4n3x.levyra.desktop.core.localmusic.AudioTagReader
import com.luc4n3x.levyra.desktop.core.localmusic.LocalArtworkCache
import com.luc4n3x.levyra.desktop.core.localmusic.LocalFolder
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryIndex
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryScanner
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryStore
import com.luc4n3x.levyra.desktop.core.localmusic.LocalMusicIdentity
import com.luc4n3x.levyra.desktop.core.localmusic.LocalScanProgress
import com.luc4n3x.levyra.desktop.core.localmusic.M3uPlaylist
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaylistTransferResult(
    val matched: Int,
    val skipped: Int
)

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
    private val scanSignals = Channel<Unit>(Channel.CONFLATED)
    private val deepScanPending = AtomicBoolean(false)

    private var scanWorkerJob: Job? = null
    private var watchJob: Job? = null
    private var debounceJob: Job? = null

    val state: StateFlow<LocalMusicUiState> = internalState.asStateFlow()

    init {
        scope.launch {
            store.data.collect { data ->
                val index = withContext(Dispatchers.Default) { LocalLibraryIndex.of(data.tracks) }
                internalState.update { state ->
                    state.copy(
                        folders = data.folders,
                        index = index,
                        lastScanAtMs = data.lastScanAtMs,
                        unavailableCount = data.tracks.count { !it.available }
                    )
                }
            }
        }
        scanWorkerJob = scope.launch {
            for (signal in scanSignals) {
                runScan(deep = deepScanPending.getAndSet(false))
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
        rescan(deep = false)
        restartWatcher()
    }

    fun forgetUnavailable() {
        store.forgetUnavailable()
    }

    fun rescan(deep: Boolean) {
        if (deep) deepScanPending.set(true)
        scanSignals.trySend(Unit)
    }

    private suspend fun runScan(deep: Boolean) {
        val folders = store.current.folders
        if (folders.isEmpty()) {
            store.replaceTracks(emptyList())
            internalState.update { it.copy(scanning = false, progress = LocalScanProgress()) }
            return
        }
        internalState.update { it.copy(scanning = true, progress = LocalScanProgress()) }
        try {
            val existing = store.current.tracks
            val result = withContext(Dispatchers.IO) {
                scanner.scan(
                    folders = folders,
                    existing = existing,
                    deep = deep,
                    onProgress = { progress ->
                        internalState.update { it.copy(progress = progress) }
                    }
                )
            }
            store.replaceTracks(result.tracks)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            DesktopDiagnostics.background("local library scan", error)
        } finally {
            internalState.update { it.copy(scanning = false, progress = LocalScanProgress()) }
        }
    }

    suspend fun readPlaylistFile(file: Path): Pair<List<Track>, PlaylistTransferResult> =
        withContext(Dispatchers.IO) {
            val content = try {
                currentCoroutineContext().ensureActive()
                Files.readString(file)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                DesktopDiagnostics.background("playlist import from $file", error)
                return@withContext emptyList<Track>() to PlaylistTransferResult(0, 0)
            }
            val baseDirectory = file.parent
            val known = internalState.value.index.tracks.associateBy {
                LocalMusicIdentity.normalizePathKey(it.path)
            }
            val tracks = ArrayList<Track>()
            var skipped = 0
            M3uPlaylist.parse(content).forEach { entry ->
                currentCoroutineContext().ensureActive()
                if (entry.isRemote) {
                    val videoId = M3uPlaylist.youtubeVideoId(entry)
                    if (videoId.isBlank()) {
                        skipped += 1
                    } else {
                        tracks.add(
                            Track(
                                id = videoId,
                                title = entry.title.ifBlank { videoId },
                                artist = entry.artist,
                                videoUrl = Track.watchUrlOf(videoId),
                                durationMs = entry.durationMs
                            )
                        )
                    }
                    return@forEach
                }
                val resolved = M3uPlaylist.resolve(entry, baseDirectory)
                if (resolved == null || !Files.isRegularFile(resolved)) {
                    skipped += 1
                    return@forEach
                }
                val indexed = known[LocalMusicIdentity.normalizePathKey(resolved.toString())]
                if (indexed != null) {
                    tracks.add(indexed.toTrack())
                    return@forEach
                }
                if (!AudioTagReader.isSupported(resolved)) {
                    skipped += 1
                    return@forEach
                }
                val tags = AudioTagReader.read(resolved)
                tracks.add(
                    Track(
                        id = LocalMusicIdentity.trackId(
                            LocalMusicIdentity.hashOf(resolved.toString())
                        ),
                        title = tags.title.ifBlank { entry.title },
                        artist = tags.artist.ifBlank { entry.artist },
                        album = tags.album,
                        videoUrl = "",
                        durationMs = if (tags.durationMs > 0L) tags.durationMs else entry.durationMs,
                        offlinePath = resolved.toString(),
                        offlineMediaLabel = tags.codec
                    )
                )
            }
            tracks to PlaylistTransferResult(matched = tracks.size, skipped = skipped)
        }

    suspend fun writePlaylistFile(file: Path, name: String, tracks: List<Track>): Boolean =
        withContext(Dispatchers.IO) {
            val fileName = file.fileName?.toString().orEmpty()
            if (fileName.isBlank()) return@withContext false
            val target = if (fileName.substringAfterLast('.', "").isBlank()) {
                file.resolveSibling("$fileName.${M3uPlaylist.EXTENSION}")
            } else {
                file
            }
            var temporary: Path? = null
            try {
                currentCoroutineContext().ensureActive()
                val parent = target.toAbsolutePath().parent ?: return@withContext false
                Files.createDirectories(parent)
                temporary = Files.createTempFile(parent, ".${target.fileName}.", ".tmp")
                Files.writeString(temporary, M3uPlaylist.render(name, tracks))
                currentCoroutineContext().ensureActive()
                try {
                    Files.move(
                        temporary,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
                }
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                DesktopDiagnostics.background("playlist export to $target", error)
                false
            } finally {
                temporary?.let { runCatching { Files.deleteIfExists(it) } }
            }
        }

    fun shutdown() {
        debounceJob?.cancel()
        watchJob?.cancel()
        scanWorkerJob?.cancel()
        watchSignals.close()
        scanSignals.close()
    }

    private fun restartWatcher() {
        watchJob?.cancel()
        val folders = store.current.folders
        if (folders.isEmpty()) return
        watchJob = scope.launch(Dispatchers.IO) {
            val service = runCatching { FileSystems.getDefault().newWatchService() }.getOrNull()
                ?: return@launch
            val watched = HashSet<String>()
            try {
                var registered = 0
                folders.forEach { folder ->
                    registered += register(
                        service = service,
                        root = runCatching { Path.of(folder.path) }.getOrNull(),
                        alreadyRegistered = registered,
                        watched = watched
                    )
                }
                while (isActive) {
                    val key = service.poll(WATCH_POLL_MS, TimeUnit.MILLISECONDS) ?: continue
                    val parent = key.watchable() as? Path
                    val events = key.pollEvents()
                    if (parent != null && registered < MAX_WATCHED_DIRECTORIES) {
                        events.forEach { event ->
                            if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE) return@forEach
                            val relative = event.context() as? Path ?: return@forEach
                            val created = parent.resolve(relative)
                            if (Files.isDirectory(created)) {
                                registered += register(
                                    service = service,
                                    root = created,
                                    alreadyRegistered = registered,
                                    watched = watched
                                )
                            }
                        }
                    }
                    val valid = key.reset()
                    if (!valid && parent != null) {
                        val removed = watched.remove(LocalMusicIdentity.normalizePathKey(parent.toString()))
                        if (removed) registered = (registered - 1).coerceAtLeast(0)
                    }
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

    private fun register(
        service: WatchService,
        root: Path?,
        alreadyRegistered: Int,
        watched: MutableSet<String>
    ): Int {
        if (root == null || !Files.isDirectory(root) || alreadyRegistered >= MAX_WATCHED_DIRECTORIES) return 0
        var count = 0
        val remaining = MAX_WATCHED_DIRECTORIES - alreadyRegistered
        runCatching {
            Files.walk(root, MAX_WATCH_DEPTH).use { stream ->
                stream.filter { Files.isDirectory(it) }
                    .filter { directory ->
                        LocalMusicIdentity.normalizePathKey(directory.toString()) !in watched
                    }
                    .limit(remaining.toLong())
                    .forEach { directory ->
                        val identity = LocalMusicIdentity.normalizePathKey(directory.toString())
                        if (!watched.add(identity)) return@forEach
                        val registered = runCatching {
                            directory.register(
                                service,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY
                            )
                            true
                        }.getOrDefault(false)
                        if (registered) {
                            count += 1
                        } else {
                            watched.remove(identity)
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
        const val MAX_WATCH_DEPTH = 12
        const val MAX_WATCHED_DIRECTORIES = 4_000
    }
}
