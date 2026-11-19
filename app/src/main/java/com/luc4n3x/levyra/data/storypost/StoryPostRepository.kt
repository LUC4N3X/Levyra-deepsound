package com.luc4n3x.levyra.data.storypost

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import kotlin.coroutines.coroutineContext

data class StoryPost(
    val id: String,
    val kind: StoryPostKind,
    val title: String,
    val caption: String,
    val artist: String,
    val sourceName: String,
    val images: List<File>,
    val media: File,
    val cover: File?,
    val durationMs: Long,
    val createdAt: Long
) {
    val trackId: String get() = STORY_POST_TRACK_PREFIX + id
}

data class StoryPostDraft(
    val stagingId: String,
    val sourceName: String,
    val plan: StoryPostPlan,
    val captions: Map<String, String>
)

data class StoryPostSelection(
    val audioPath: String?,
    val videoPath: String?,
    val captionPath: String?
)

enum class StoryPostImportError { InvalidArchive, UnsafeArchive, TooLarge, NoMedia, NoAudio, Unreadable, NoSpace }

class StoryPostImportException(val reason: StoryPostImportError, cause: Throwable? = null) : IOException(reason.name, cause)

const val STORY_POST_TRACK_PREFIX = "story-post:"

fun Track.isStoryPost(): Boolean = id.startsWith(STORY_POST_TRACK_PREFIX)

fun StoryPost.toTrack(): Track {
    val artwork = (cover ?: images.firstOrNull())?.let { Uri.fromFile(it).toString() }.orEmpty()
    val stream = Uri.fromFile(media).toString()
    val manifest = if (kind == StoryPostKind.Video) {
        ResolvedPlaybackManifest(
            sourceVideoId = "",
            provider = STORY_POST_PROVIDER,
            resolvedAtMs = createdAt,
            expiresAtMs = 0L,
            durationMs = durationMs,
            selectedAudioUrl = stream,
            selectedVideoUrl = "",
            streams = listOf(
                PlaybackStreamDescriptor(
                    url = stream,
                    kind = PlaybackStreamKind.MUXED,
                    deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                    selected = true
                )
            )
        )
    } else {
        null
    }
    return Track(
        id = trackId,
        title = title,
        artist = artist,
        album = sourceName,
        durationMs = durationMs,
        streamUrl = stream,
        videoUrl = "",
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        source = STORY_POST_SOURCE,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        playbackManifest = manifest
    )
}

class StoryPostRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, ROOT_DIR)
    private val staging = File(appContext.cacheDir, STAGING_DIR)
    private val limits = StoryPostLimits()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _posts = MutableStateFlow<List<StoryPost>>(emptyList())

    val posts: StateFlow<List<StoryPost>> = _posts.asStateFlow()

    private val ready = scope.launch { mutex.withLock { reload() } }

    suspend fun inspectFolder(treeUri: Uri): StoryPostDraft = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val sourceName = queryDisplayName(DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId))
            ?: rootId.substringAfterLast('/').substringAfterLast(':')
        val files = mutableListOf<StoryPostSourceFile>()
        val pending = ArrayDeque(listOf(rootId to ""))
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val (parentId, prefix) = pending.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            resolver.query(children, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val name = cursor.getString(1)?.takeIf(String::isNotBlank) ?: continue
                    val path = prefix + name
                    if (isIgnoredStoryPostPath(path)) continue
                    if (cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (prefix.isEmpty()) pending.addLast(id to "$path/")
                        continue
                    }
                    if (storyPostFileKind(name) == null) continue
                    if (files.size >= limits.maxEntries) throw StoryPostImportException(StoryPostImportError.TooLarge)
                    val size = if (cursor.isNull(3)) 0L else cursor.getLong(3)
                    files += StoryPostSourceFile(path, size, DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString())
                }
            } ?: throw StoryPostImportException(StoryPostImportError.Unreadable)
        }
        draftOf(UUID.randomUUID().toString(), sourceName, files)
    }

    suspend fun inspectZip(uri: Uri): StoryPostDraft = withContext(Dispatchers.IO) {
        ready.join()
        val stagingId = UUID.randomUUID().toString()
        val dir = File(staging, stagingId)
        try {
            val sourceName = queryDisplayName(uri)?.substringBeforeLast('.') ?: "Post"
            if (!dir.mkdirs()) throw StoryPostImportException(StoryPostImportError.Unreadable)
            val archive = File(dir, ARCHIVE_NAME)
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw StoryPostImportException(StoryPostImportError.Unreadable)
            input.use { stream -> archive.outputStream().use { copyBounded(stream, it, limits.maxTotalBytes) } }
            val files = try {
                ZipFile(archive).use { zip -> extractZip(zip, dir) }
            } catch (error: ZipException) {
                throw StoryPostImportException(StoryPostImportError.InvalidArchive, error)
            } finally {
                archive.delete()
            }
            coroutineContext.ensureActive()
            draftOf(stagingId, sourceName, files)
        } catch (error: Throwable) {
            dir.deleteRecursively()
            throw error
        }
    }

    fun discard(draft: StoryPostDraft) {
        scope.launch { File(staging, draft.stagingId).deleteRecursively() }
    }

    suspend fun save(draft: StoryPostDraft, selection: StoryPostSelection): StoryPost = withContext(Dispatchers.IO) {
        mutex.withLock {
            val plan = draft.plan
            val kind = plan.kind ?: throw StoryPostImportException(StoryPostImportError.NoMedia)
            val media = when (kind) {
                StoryPostKind.Photos -> plan.audioCandidates.firstOrNull { it.path == selection.audioPath } ?: plan.defaultAudio
                StoryPostKind.Video -> plan.videoCandidates.firstOrNull { it.path == selection.videoPath } ?: plan.defaultVideo
            } ?: throw StoryPostImportException(StoryPostImportError.NoMedia)
            val images = if (kind == StoryPostKind.Photos) plan.images else emptyList()
            val needed = images.sumOf { it.sizeBytes } + media.sizeBytes
            if (needed > limits.maxTotalBytes || (images + media).any { it.sizeBytes > limits.maxEntryBytes }) {
                throw StoryPostImportException(StoryPostImportError.TooLarge)
            }
            root.mkdirs()
            if (root.usableSpace < needed + FREE_SPACE_MARGIN_BYTES) throw StoryPostImportException(StoryPostImportError.NoSpace)
            val id = UUID.randomUUID().toString()
            val partial = File(root, id + PARTIAL_SUFFIX)
            try {
                if (!partial.mkdirs()) throw StoryPostImportException(StoryPostImportError.Unreadable)
                var budget = limits.maxTotalBytes
                val imageNames = images.mapIndexed { index, source ->
                    storyPostStoredName(index, source).also { budget -= copySource(source, File(partial, it), budget) }
                }
                val mediaName = storyPostStoredName(0, media)
                copySource(media, File(partial, mediaName), budget)
                val facts = readMediaFacts(File(partial, mediaName), coverTarget = File(partial, COVER_NAME).takeIf { kind == StoryPostKind.Video })
                val caption = selection.captionPath?.let(draft.captions::get).orEmpty()
                val manifest = JSONObject()
                    .put("version", MANIFEST_VERSION)
                    .put("kind", kind.storageValue)
                    .put("title", storyPostTitle(caption, draft.sourceName))
                    .put("caption", caption)
                    .put("artist", facts.artist)
                    .put("sourceName", draft.sourceName)
                    .put("images", JSONArray(imageNames))
                    .put("media", mediaName)
                    .put("cover", if (facts.coverSaved) COVER_NAME else "")
                    .put("durationMs", facts.durationMs)
                    .put("createdAt", System.currentTimeMillis())
                File(partial, MANIFEST_NAME).writeText(manifest.toString())
                val target = File(root, id)
                if (!partial.renameTo(target)) throw StoryPostImportException(StoryPostImportError.Unreadable)
                File(staging, draft.stagingId).deleteRecursively()
                val post = readPost(target) ?: throw StoryPostImportException(StoryPostImportError.Unreadable)
                _posts.value = (listOf(post) + _posts.value).sortedByDescending { it.createdAt }
                post
            } catch (error: Throwable) {
                partial.deleteRecursively()
                throw error
            }
        }
    }

    suspend fun delete(postId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(root, postId)
            if (POST_ID.matches(postId) && dir.canonicalFile.parentFile == root.canonicalFile) dir.deleteRecursively()
            _posts.value = _posts.value.filterNot { it.id == postId }
        }
    }

    private fun reload() {
        root.listFiles()?.filter { it.name.endsWith(PARTIAL_SUFFIX) }?.forEach { it.deleteRecursively() }
        staging.deleteRecursively()
        _posts.value = root.listFiles()
            ?.filter(File::isDirectory)
            ?.mapNotNull(::readPost)
            ?.sortedByDescending { it.createdAt }
            .orEmpty()
    }

    private fun readPost(dir: File): StoryPost? = runCatching {
        val json = JSONObject(File(dir, MANIFEST_NAME).readText())
        val kind = StoryPostKind.fromStorage(json.optString("kind")) ?: return null
        val images = json.optJSONArray("images")
            ?.let { array -> (0 until array.length()).map { File(dir, File(array.getString(it)).name) } }
            .orEmpty()
        val media = File(dir, File(json.getString("media")).name)
        if (!media.isFile) return null
        StoryPost(
            id = dir.name,
            kind = kind,
            title = json.optString("title"),
            caption = json.optString("caption"),
            artist = json.optString("artist"),
            sourceName = json.optString("sourceName"),
            images = images.filter(File::isFile),
            media = media,
            cover = json.optString("cover").takeIf(String::isNotBlank)?.let { File(dir, File(it).name) }?.takeIf(File::isFile),
            durationMs = json.optLong("durationMs"),
            createdAt = json.optLong("createdAt")
        )
    }.onFailure { Timber.w(it, "Unreadable story post %s", dir.name) }.getOrNull()

    private suspend fun draftOf(stagingId: String, sourceName: String, files: List<StoryPostSourceFile>): StoryPostDraft {
        val plan = planStoryPost(files)
        when (plan.problem) {
            StoryPostPlanProblem.NoMedia -> throw StoryPostImportException(StoryPostImportError.NoMedia)
            StoryPostPlanProblem.NoAudio -> throw StoryPostImportException(StoryPostImportError.NoAudio)
            null -> Unit
        }
        val captions = plan.captionCandidates.associate { source ->
            coroutineContext.ensureActive()
            source.path to normalizeStoryPostCaption(readCaption(source))
        }
        return StoryPostDraft(stagingId, sourceName, plan, captions)
    }

    private fun extractZip(zip: ZipFile, dir: File): List<StoryPostSourceFile> {
        val check = checkStoryPostZipEntries(
            zip.entries().toList().map { StoryPostZipEntry(it.name, it.size, it.isDirectory) },
            limits
        )
        val accepted = when (check) {
            is StoryPostZipCheck.Accepted -> check.files
            StoryPostZipCheck.UnsafePath -> throw StoryPostImportException(StoryPostImportError.UnsafeArchive)
            StoryPostZipCheck.TooLarge -> throw StoryPostImportException(StoryPostImportError.TooLarge)
        }
        val canonicalDir = dir.canonicalFile
        return accepted.mapIndexed { index, entry ->
            val target = File(dir, "%04d-%s".format(index, safeFileName(entry.name.substringAfterLast('/'))))
            if (target.canonicalFile.parentFile != canonicalDir) throw StoryPostImportException(StoryPostImportError.UnsafeArchive)
            val zipEntry = zip.getEntry(entry.name) ?: throw StoryPostImportException(StoryPostImportError.InvalidArchive)
            zip.getInputStream(zipEntry).use { input -> target.outputStream().use { copyBounded(input, it, entry.declaredSize) } }
            StoryPostSourceFile(entry.name, target.length(), Uri.fromFile(target).toString())
        }
    }

    private fun copySource(source: StoryPostSourceFile, target: File, budget: Long): Long {
        val input = appContext.contentResolver.openInputStream(Uri.parse(source.previewModel))
            ?: throw StoryPostImportException(StoryPostImportError.Unreadable)
        input.use { stream -> target.outputStream().use { copyBounded(stream, it, minOf(limits.maxEntryBytes, budget)) } }
        return target.length()
    }

    private fun readCaption(source: StoryPostSourceFile): String {
        val input = appContext.contentResolver.openInputStream(Uri.parse(source.previewModel)) ?: return ""
        return input.use { String(it.readAtMost(limits.maxCaptionBytes.toInt()), Charsets.UTF_8) }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf(String::isNotBlank)

    private fun readMediaFacts(file: File, coverTarget: File?): MediaFacts {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim().orEmpty()
            val coverSaved = coverTarget != null && runCatching {
                val frame = retriever.getFrameAtTime(0L) ?: return@runCatching false
                try {
                    coverTarget.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, it) }
                } finally {
                    frame.recycle()
                }
                true
            }.getOrDefault(false)
            MediaFacts(duration, artist, coverSaved)
        } catch (error: RuntimeException) {
            Timber.w(error, "Story post metadata unavailable")
            MediaFacts(0L, "", false)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private data class MediaFacts(val durationMs: Long, val artist: String, val coverSaved: Boolean)

    companion object {
        private const val ROOT_DIR = "story_posts"
        private const val STAGING_DIR = "story_post_staging"
        private const val ARCHIVE_NAME = "source.zip"
        private const val MANIFEST_NAME = "post.json"
        private const val COVER_NAME = "cover.jpg"
        private const val PARTIAL_SUFFIX = ".partial"
        private const val MANIFEST_VERSION = 1
        private const val COVER_QUALITY = 88
        private const val FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L
        private val POST_ID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

        @Volatile
        private var instance: StoryPostRepository? = null

        fun get(context: Context): StoryPostRepository = instance ?: synchronized(this) {
            instance ?: StoryPostRepository(context).also { instance = it }
        }
    }
}

private const val STORY_POST_SOURCE = "Offline"
private const val STORY_POST_PROVIDER = "story-post"

private fun copyBounded(input: InputStream, output: OutputStream, limit: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > limit) throw StoryPostImportException(StoryPostImportError.TooLarge)
        output.write(buffer, 0, read)
    }
}

private fun InputStream.readAtMost(limit: Int): ByteArray {
    val buffer = ByteArray(limit)
    var offset = 0
    while (offset < limit) {
        val read = read(buffer, offset, limit - offset)
        if (read < 0) break
        offset += read
    }
    return buffer.copyOf(offset)
}

private fun safeFileName(name: String): String =
    name.map { if (it.isLetterOrDigit() || it == '.' || it == '-' || it == '_') it else '_' }
        .joinToString("")
        .take(120)
        .ifBlank { "file" }
