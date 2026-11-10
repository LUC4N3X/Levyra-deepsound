package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import com.luc4n3x.levyra.domain.LyricsFontFormat
import com.luc4n3x.levyra.domain.LyricsFontInspector
import com.luc4n3x.levyra.domain.sanitizeLyricsFontName
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

data class LyricsFontEntry(
    val id: String,
    val name: String,
    val fileName: String,
    val format: LyricsFontFormat,
    val variableWeight: IntRange?,
    val sizeBytes: Long,
    val importedAt: Long
) {
    val variable: Boolean get() = variableWeight != null
}

enum class LyricsFontError { TOO_LARGE, INVALID, LIBRARY_FULL, UNREADABLE }

class LyricsFontException(val error: LyricsFontError, message: String) : IOException(message)

class LyricsFontBackupItem(val entry: LyricsFontEntry, val bytes: ByteArray)

class LyricsFontStagedRestore internal constructor(
    internal val directory: File,
    internal val entries: List<LyricsFontEntry>
)

class LyricsFontLibrary internal constructor(
    private val directory: File,
    private val typefaceLoads: (File, IntRange?) -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<LyricsFontEntry>>(emptyList())
    @Volatile private var loaded = false

    val fonts: StateFlow<List<LyricsFontEntry>> = state.asStateFlow()

    suspend fun load(): List<LyricsFontEntry> = withContext(Dispatchers.IO) {
        mutex.withLock { ensureLoadedLocked() }
    }

    suspend fun fontFile(id: String): Pair<LyricsFontEntry, File>? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entry = ensureLoadedLocked().firstOrNull { it.id == id } ?: return@withLock null
            File(directory, entry.fileName)
                .takeIf { it.isFile && it.length() == entry.sizeBytes }
                ?.let { entry to it }
        }
    }

    suspend fun import(open: () -> InputStream?, fallbackName: String): LyricsFontEntry = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = ensureLoadedLocked()
            if (current.size >= MAX_FONTS) throw LyricsFontException(LyricsFontError.LIBRARY_FULL, "Font library is full")
            val candidate = receive(open, current.sumOf { it.sizeBytes })
            val name = candidate.familyName.ifBlank { sanitizeLyricsFontName(fallbackName).ifBlank { DEFAULT_NAME } }
            val entry = store(candidate, UUID.randomUUID().toString(), name, clock())
            persistLocked(current + entry, rollback = File(directory, entry.fileName))
            entry
        }
    }

    suspend fun replace(id: String, open: () -> InputStream?): LyricsFontEntry = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = ensureLoadedLocked()
            val previous = current.firstOrNull { it.id == id }
                ?: throw LyricsFontException(LyricsFontError.UNREADABLE, "Font not found")
            val candidate = receive(open, current.sumOf { it.sizeBytes } - previous.sizeBytes)
            val entry = store(candidate, id, previous.name, previous.importedAt)
            persistLocked(current.map { if (it.id == id) entry else it }, rollback = File(directory, entry.fileName))
            deleteQuietly(File(directory, previous.fileName))
            entry
        }
    }

    suspend fun rename(id: String, name: String): LyricsFontEntry? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val clean = sanitizeLyricsFontName(name).takeIf(String::isNotEmpty) ?: return@withLock null
            val current = ensureLoadedLocked()
            val renamed = current.firstOrNull { it.id == id }?.copy(name = clean) ?: return@withLock null
            persistLocked(current.map { if (it.id == id) renamed else it }, rollback = null)
            renamed
        }
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = ensureLoadedLocked()
            val removed = current.firstOrNull { it.id == id } ?: return@withLock false
            persistLocked(current - removed, rollback = null)
            deleteQuietly(File(directory, removed.fileName))
            true
        }
    }

    suspend fun backupItems(): List<LyricsFontBackupItem> = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoadedLocked().mapNotNull { entry ->
                runCatching { File(directory, entry.fileName).readBytes() }
                    .onFailure { Timber.w(it, "Lyrics font %s skipped from backup", entry.id) }
                    .getOrNull()
                    ?.takeIf { it.size.toLong() == entry.sizeBytes }
                    ?.let { LyricsFontBackupItem(entry, it) }
            }
        }
    }

    suspend fun stageRestore(items: List<LyricsFontBackupItem>): LyricsFontStagedRestore = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoadedLocked()
            ensureDirectory()
            val staging = File(directory, "$STAGING_PREFIX${UUID.randomUUID()}")
            if (!staging.mkdirs()) throw IOException("Unable to stage lyrics fonts")
            try {
                var total = 0L
                val staged = items.distinctBy { it.entry.id }.take(MAX_FONTS).mapNotNull { item ->
                    val size = item.bytes.size.toLong()
                    if (!ID_PATTERN.matches(item.entry.id) || size > MAX_FONT_BYTES || total + size > MAX_LIBRARY_BYTES) {
                        return@mapNotNull null
                    }
                    val file = File(staging, "${UUID.randomUUID()}.tmp")
                    file.writeBytes(item.bytes)
                    val inspection = LyricsFontInspector.inspect(file)
                    if (inspection == null || !typefaceLoads(file, inspection.variableWeight)) {
                        Timber.w("Lyrics font %s rejected during restore", item.entry.id)
                        deleteQuietly(file)
                        return@mapNotNull null
                    }
                    val fileName = "${item.entry.id}-${UUID.randomUUID()}.${inspection.format.extension}"
                    if (!file.renameTo(File(staging, fileName))) throw IOException("Unable to stage lyrics font")
                    total += size
                    item.entry.copy(
                        name = sanitizeLyricsFontName(item.entry.name).ifBlank { inspection.familyName.ifBlank { DEFAULT_NAME } },
                        fileName = fileName,
                        format = inspection.format,
                        variableWeight = inspection.variableWeight,
                        sizeBytes = size
                    )
                }
                LyricsFontStagedRestore(staging, staged)
            } catch (error: Throwable) {
                staging.deleteRecursively()
                throw error
            }
        }
    }

    suspend fun commitRestore(staged: LyricsFontStagedRestore) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val previous = ensureLoadedLocked()
                val moved = mutableListOf<File>()
                try {
                    staged.entries.forEach { entry ->
                        val target = File(directory, entry.fileName)
                        if (!File(staged.directory, entry.fileName).renameTo(target)) throw IOException("Unable to restore lyrics font")
                        moved += target
                    }
                    persistLocked(mergeRestoredFonts(staged.entries, previous), rollback = null)
                } catch (error: Throwable) {
                    moved.forEach(::deleteQuietly)
                    throw error
                }
                val kept = state.value.mapTo(HashSet()) { it.fileName }
                previous.filterNot { it.fileName in kept }.forEach { deleteQuietly(File(directory, it.fileName)) }
            } finally {
                staged.directory.deleteRecursively()
            }
        }
    }

    fun discardRestore(staged: LyricsFontStagedRestore) {
        staged.directory.deleteRecursively()
    }

    private class Candidate(
        val temporary: File,
        val familyName: String,
        val format: LyricsFontFormat,
        val variableWeight: IntRange?,
        val sizeBytes: Long
    )

    private fun store(candidate: Candidate, id: String, name: String, importedAt: Long): LyricsFontEntry {
        val fileName = "$id-${UUID.randomUUID()}.${candidate.format.extension}"
        if (!candidate.temporary.renameTo(File(directory, fileName))) {
            deleteQuietly(candidate.temporary)
            throw LyricsFontException(LyricsFontError.UNREADABLE, "Unable to store font")
        }
        return LyricsFontEntry(id, name, fileName, candidate.format, candidate.variableWeight, candidate.sizeBytes, importedAt)
    }

    private fun receive(open: () -> InputStream?, otherBytes: Long): Candidate {
        ensureDirectory()
        val temporary = File(directory, "$IMPORT_PREFIX${UUID.randomUUID()}.tmp")
        try {
            val size = try {
                val input = open() ?: throw LyricsFontException(LyricsFontError.UNREADABLE, "Unable to open font")
                input.use { copyBounded(it, temporary) }
            } catch (error: LyricsFontException) {
                throw error
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                throw LyricsFontException(LyricsFontError.UNREADABLE, error.message ?: "Unable to read font")
            }
            if (otherBytes + size > MAX_LIBRARY_BYTES) throw LyricsFontException(LyricsFontError.LIBRARY_FULL, "Font library is full")
            val inspection = LyricsFontInspector.inspect(temporary)
                ?: throw LyricsFontException(LyricsFontError.INVALID, "Not a TrueType or OpenType font")
            if (!typefaceLoads(temporary, inspection.variableWeight)) {
                throw LyricsFontException(LyricsFontError.INVALID, "Font cannot be rendered")
            }
            return Candidate(temporary, inspection.familyName, inspection.format, inspection.variableWeight, size)
        } catch (error: Throwable) {
            deleteQuietly(temporary)
            throw error
        }
    }

    private fun copyBounded(input: InputStream, target: File): Long {
        var total = 0L
        target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_FONT_BYTES) throw LyricsFontException(LyricsFontError.TOO_LARGE, "Font is too large")
                output.write(buffer, 0, read)
            }
        }
        if (total == 0L) throw LyricsFontException(LyricsFontError.INVALID, "Empty font file")
        return total
    }

    private fun ensureLoadedLocked(): List<LyricsFontEntry> {
        if (loaded) return state.value
        val index = File(directory, INDEX_FILE)
        val entries = if (index.isFile) {
            runCatching { decodeLyricsFontIndex(index.readText()) }
                .onFailure { Timber.w(it, "Lyrics font index unreadable") }
                .getOrDefault(emptyList())
                .filter { File(directory, it.fileName).isFile }
        } else {
            emptyList()
        }
        cleanupStrays(entries)
        state.value = entries
        loaded = true
        return entries
    }

    private fun cleanupStrays(entries: List<LyricsFontEntry>) {
        val known = entries.mapTo(HashSet()) { it.fileName } + INDEX_FILE
        directory.listFiles()?.forEach { file ->
            if (file.name in known) return@forEach
            if (file.isDirectory) {
                if (file.name.startsWith(STAGING_PREFIX)) file.deleteRecursively()
            } else {
                deleteQuietly(file)
            }
        }
    }

    private fun persistLocked(entries: List<LyricsFontEntry>, rollback: File?) {
        try {
            ensureDirectory()
            val temporary = File(directory, "$INDEX_FILE.tmp")
            temporary.writeText(encodeLyricsFontIndex(entries))
            val target = File(directory, INDEX_FILE)
            if (!temporary.renameTo(target)) {
                target.delete()
                if (!temporary.renameTo(target)) throw IOException("Unable to save lyrics font index")
            }
        } catch (error: Throwable) {
            rollback?.let(::deleteQuietly)
            throw error
        }
        state.value = entries
    }

    private fun ensureDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Unable to create lyrics font folder")
    }

    companion object {
        const val MAX_FONT_BYTES = 20L * 1024L * 1024L
        const val MAX_LIBRARY_BYTES = 32L * 1024L * 1024L
        const val MAX_FONTS = 12
        private const val INDEX_FILE = "index.json"
        private const val IMPORT_PREFIX = ".import-"
        private const val STAGING_PREFIX = ".restore-"
        private const val DEFAULT_NAME = "Font"
        internal val ID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

        @Volatile private var instance: LyricsFontLibrary? = null

        fun get(context: Context): LyricsFontLibrary = instance ?: synchronized(this) {
            instance ?: LyricsFontLibrary(
                File(context.applicationContext.filesDir, "lyrics_fonts"),
                ::androidTypefaceLoads
            ).also { instance = it }
        }
    }
}

private fun deleteQuietly(file: File) {
    if (file.exists() && !file.delete()) Timber.w("Unable to delete %s", file.name)
}

internal fun androidTypefaceLoads(file: File, variableWeight: IntRange?): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        android.graphics.fonts.Font.Builder(file).build()
    }
    val builder = Typeface.Builder(file)
    variableWeight?.let { builder.setFontVariationSettings("'wght' ${700.coerceIn(it.first, it.last)}") }
    val typeface = builder.build()
    typeface != null && typeface != Typeface.DEFAULT
}.getOrDefault(false)

private val FONT_FILE_SUFFIX = Regex("[0-9a-f-]{36}\\.(ttf|otf)")

internal fun mergeRestoredFonts(restored: List<LyricsFontEntry>, local: List<LyricsFontEntry>): List<LyricsFontEntry> {
    val restoredIds = restored.mapTo(HashSet()) { it.id }
    var total = restored.sumOf { it.sizeBytes }
    val kept = local.filter { entry ->
        val fits = entry.id !in restoredIds && total + entry.sizeBytes <= LyricsFontLibrary.MAX_LIBRARY_BYTES
        if (fits) total += entry.sizeBytes
        fits
    }
    return (restored + kept).take(LyricsFontLibrary.MAX_FONTS)
}

internal fun encodeLyricsFontIndex(entries: List<LyricsFontEntry>): String = JSONObject()
    .put("version", 1)
    .put("fonts", JSONArray().apply { entries.forEach { put(lyricsFontEntryToJson(it)) } })
    .toString()

internal fun decodeLyricsFontIndex(raw: String): List<LyricsFontEntry> {
    val fonts = JSONObject(raw).optJSONArray("fonts") ?: return emptyList()
    return (0 until fonts.length())
        .mapNotNull { index -> fonts.optJSONObject(index)?.let(::lyricsFontEntryFromJson) }
        .distinctBy { it.id }
}

internal fun lyricsFontEntryToJson(entry: LyricsFontEntry): JSONObject = JSONObject()
    .put("id", entry.id)
    .put("name", entry.name)
    .put("file", entry.fileName)
    .put("format", entry.format.name)
    .put("minWeight", entry.variableWeight?.first ?: 0)
    .put("maxWeight", entry.variableWeight?.last ?: 0)
    .put("bytes", entry.sizeBytes)
    .put("importedAt", entry.importedAt)

internal fun lyricsFontEntryFromJson(json: JSONObject): LyricsFontEntry? {
    val id = json.optString("id")
    if (!LyricsFontLibrary.ID_PATTERN.matches(id)) return null
    val format = LyricsFontFormat.from(json.optString("format")) ?: return null
    val fileName = json.optString("file").ifBlank { "$id-00000000-0000-0000-0000-000000000000.${format.extension}" }
    val validFileName = fileName.startsWith("$id-") && fileName.endsWith(".${format.extension}") &&
        FONT_FILE_SUFFIX.matches(fileName.substring(id.length + 1))
    if (!validFileName) return null
    val minWeight = json.optInt("minWeight", 0)
    val maxWeight = json.optInt("maxWeight", 0)
    return LyricsFontEntry(
        id = id,
        name = sanitizeLyricsFontName(json.optString("name")).ifBlank { "Font" },
        fileName = fileName,
        format = format,
        variableWeight = if (minWeight in 1 until maxWeight && maxWeight <= 1000) minWeight..maxWeight else null,
        sizeBytes = json.optLong("bytes", 0L),
        importedAt = json.optLong("importedAt", 0L)
    )
}
