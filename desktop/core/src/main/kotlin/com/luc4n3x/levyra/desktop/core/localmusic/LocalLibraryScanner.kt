package com.luc4n3x.levyra.desktop.core.localmusic

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

data class LocalScanProgress(
    val processed: Int = 0,
    val total: Int = 0,
    val currentDirectory: String = ""
)

data class LocalScanResult(
    val tracks: List<LocalTrack>,
    val added: Int,
    val updated: Int,
    val reused: Int,
    val missing: Int,
    val failed: Int
)

class LocalLibraryScanner(
    private val artwork: LocalArtworkCache,
    private val readTags: (Path) -> AudioTags = AudioTagReader::read,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    suspend fun scan(
        folders: List<LocalFolder>,
        existing: List<LocalTrack>,
        deep: Boolean = false,
        onProgress: (LocalScanProgress) -> Unit = {}
    ): LocalScanResult {
        artwork.clearDirectoryCache()
        val previous = existing.associateBy { LocalMusicIdentity.normalizePathKey(it.path) }
        val files = ArrayList<Pair<LocalFolder, Path>>()
        val seenPaths = HashSet<String>()
        folders.forEach { folder ->
            coroutineContext.ensureActive()
            collectFiles(folder, files, seenPaths)
        }
        onProgress(LocalScanProgress(processed = 0, total = files.size))

        val scanned = LinkedHashMap<String, LocalTrack>(files.size.coerceAtLeast(16))
        var added = 0
        var updated = 0
        var reused = 0
        var failed = 0
        files.forEachIndexed { index, (folder, file) ->
            coroutineContext.ensureActive()
            val key = LocalMusicIdentity.normalizePathKey(file.toString())
            val attributes = runCatching {
                Files.size(file) to Files.getLastModifiedTime(file).toMillis()
            }.getOrNull()
            if (attributes == null) {
                failed += 1
            } else {
                val (size, modified) = attributes
                val known = previous[key]
                val unchanged = !deep &&
                    known != null &&
                    known.fileSize == size &&
                    known.modifiedAtMs == modified
                if (unchanged) {
                    scanned[key] = known.copy(available = true, folderId = folder.id)
                    reused += 1
                } else {
                    val entry = runCatching { read(folder, file, size, modified, known) }.getOrNull()
                    if (entry == null) {
                        failed += 1
                    } else {
                        scanned[key] = entry
                        if (known == null) added += 1 else updated += 1
                    }
                }
            }
            if (index % PROGRESS_STEP == 0 || index == files.lastIndex) {
                onProgress(
                    LocalScanProgress(
                        processed = index + 1,
                        total = files.size,
                        currentDirectory = file.parent?.fileName?.toString().orEmpty()
                    )
                )
            }
        }

        var missing = 0
        previous.forEach { (key, track) ->
            if (scanned.containsKey(key)) return@forEach
            if (!belongsToScannedFolders(track, folders)) {
                scanned[key] = track
                return@forEach
            }
            missing += 1
            scanned[key] = track.copy(available = false)
        }

        return LocalScanResult(
            tracks = scanned.values.sortedWith(TRACK_ORDER),
            added = added,
            updated = updated,
            reused = reused,
            missing = missing,
            failed = failed
        )
    }

    private fun belongsToScannedFolders(track: LocalTrack, folders: List<LocalFolder>): Boolean =
        folders.any { folder -> LocalMusicIdentity.isWithin(track.path, folder.path) }

    private fun read(
        folder: LocalFolder,
        file: Path,
        size: Long,
        modified: Long,
        known: LocalTrack?
    ): LocalTrack {
        val tags = readTags(file)
        val artworkPath = tags.artwork?.let(artwork::store).orEmpty().ifBlank {
            file.parent?.let(artwork::folderCover).orEmpty()
        }
        return LocalTrack(
            id = LocalMusicIdentity.hashOf(file.toString()),
            path = file.toString(),
            folderId = folder.id,
            fileSize = size,
            modifiedAtMs = modified,
            title = tags.title,
            artist = tags.artist,
            albumArtist = tags.albumArtist,
            album = tags.album,
            genre = tags.genre,
            year = tags.year,
            trackNumber = tags.trackNumber,
            discNumber = tags.discNumber,
            durationMs = tags.durationMs,
            bitrateKbps = tags.bitrateKbps,
            sampleRateHz = tags.sampleRateHz,
            bitDepth = tags.bitDepth,
            channels = tags.channels,
            codec = tags.codec,
            artworkPath = artworkPath,
            available = true,
            addedAtMs = known?.addedAtMs?.takeIf { it > 0L } ?: nowMillis()
        )
    }

    private fun collectFiles(
        folder: LocalFolder,
        target: MutableList<Pair<LocalFolder, Path>>,
        seenPaths: MutableSet<String>
    ) {
        if (target.size >= MAX_FILES) return
        val root = runCatching { Path.of(folder.path) }.getOrNull() ?: return
        if (!Files.isDirectory(root)) return
        val remaining = (MAX_FILES - target.size).toLong()
        runCatching {
            Files.walk(root, MAX_DEPTH).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter(AudioTagReader::isSupported)
                    .filter { file -> seenPaths.add(LocalMusicIdentity.normalizePathKey(file.toString())) }
                    .limit(remaining)
                    .forEach { file -> target.add(folder to file) }
            }
        }
    }

    companion object {
        private const val MAX_DEPTH = 12
        private const val MAX_FILES = 200_000
        private const val PROGRESS_STEP = 25

        private val TRACK_ORDER = compareBy<LocalTrack>(
            { LocalMusicIdentity.normalizeKey(it.effectiveAlbumArtist) },
            { LocalMusicIdentity.normalizeKey(it.album) },
            { it.discNumber },
            { it.trackNumber },
            { it.title.lowercase(Locale.ROOT) }
        )
    }
}
