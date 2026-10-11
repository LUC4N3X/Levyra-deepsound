package com.luc4n3x.levyra.data.collections

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PersonalCollectionEntity
import com.luc4n3x.levyra.data.local.PersonalCollectionSummaryRow
import com.luc4n3x.levyra.data.local.PersonalCollectionTrackEntity
import com.luc4n3x.levyra.data.local.PersonalCollectionTrackRow
import com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore
import com.luc4n3x.levyra.domain.Track
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

data class PersonalCollectionSummary(
    val id: String,
    val name: String,
    val sourceKind: PersonalCollectionSourceKind,
    val sourceLabel: String,
    val format: PersonalCollectionFormat,
    val artworkModel: String,
    val autoRefresh: Boolean,
    val allowCleartext: Boolean,
    val lastSyncedAt: Long,
    val lastSyncAttemptAt: Long,
    val lastSyncError: CollectionSyncError?,
    val createdAt: Long,
    val trackCount: Int,
    val durationMs: Long,
    val usesLocalNetwork: Boolean
) {
    val refreshable: Boolean get() = sourceKind == PersonalCollectionSourceKind.URL
}

sealed interface PersonalCollectionImportSource {
    data class Remote(val url: String, val username: String = "", val password: String = "") : PersonalCollectionImportSource
    data class LocalFile(val uri: Uri) : PersonalCollectionImportSource
}

data class PersonalCollectionPreview(
    val parsed: ParsedCollection,
    val sourceKind: PersonalCollectionSourceKind,
    val sourceUrl: String,
    val sourceLabel: String,
    val etag: String,
    val lastModified: String,
    val allowLocalNetwork: Boolean,
    val credentials: List<CollectionCredential>,
    val cleartextEntries: Int,
    val usesLocalNetwork: Boolean
) {
    val requiresCleartextConsent: Boolean
        get() = PersonalCollectionUrlPolicy.isCleartext(sourceUrl) || parsed.usesCleartext()
}

sealed interface PersonalCollectionPreviewResult {
    data class Ready(val preview: PersonalCollectionPreview) : PersonalCollectionPreviewResult
    data class Failed(val error: CollectionSyncError) : PersonalCollectionPreviewResult
}

data class PersonalCollectionSyncReport(
    val added: Int,
    val removed: Int,
    val updated: Int,
    val skipped: Int,
    val notModified: Boolean
)

sealed interface PersonalCollectionSyncResult {
    data class Synced(val report: PersonalCollectionSyncReport) : PersonalCollectionSyncResult
    data class Failed(val error: CollectionSyncError) : PersonalCollectionSyncResult
    data object Busy : PersonalCollectionSyncResult
}

class PersonalCollectionUnavailableException(message: String) : IOException(message)

class PersonalCollectionRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val dao = LevyraDatabase.get(appContext).personalCollectionDao()
    private val downloads = LevyraDatabase.get(appContext).downloadedTracksDao()
    private val credentialStore = AndroidKeystoreCredentialStore(appContext)
    private val policyMutex = Mutex()
    @Volatile private var policyLoaded = false
    private val syncGate = PersonalCollectionSyncGate()
    private val artworkDirectory: File
        get() = File(appContext.filesDir, ARTWORK_DIRECTORY).apply { mkdirs() }

    fun observeSummaries(): Flow<List<PersonalCollectionSummary>> =
        dao.observeSummaries().map { rows -> rows.map { it.toSummary() } }.flowOn(Dispatchers.IO)

    fun observeTracks(collectionId: String): Flow<List<Track>> =
        dao.observeTracks(collectionId).map { rows -> rows.map { it.toTrack() } }.flowOn(Dispatchers.IO)

    suspend fun preview(
        source: PersonalCollectionImportSource,
        allowCleartext: Boolean
    ): PersonalCollectionPreviewResult = withContext(Dispatchers.IO) {
        try {
            when (source) {
                is PersonalCollectionImportSource.LocalFile -> previewFile(source.uri)
                is PersonalCollectionImportSource.Remote -> previewRemote(source, allowCleartext)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w("Personal collection preview failed: %s", error.javaClass.simpleName)
            PersonalCollectionPreviewResult.Failed(CollectionSyncError.from(error))
        }
    }

    private fun previewFile(uri: Uri): PersonalCollectionPreviewResult {
        val label = displayName(uri)
        val bytes = appContext.contentResolver.openInputStream(uri)?.let {
            PersonalCollectionHttp.readBounded(it, PersonalCollectionParser.MAX_PLAYLIST_BYTES.toLong())
        } ?: return PersonalCollectionPreviewResult.Failed(CollectionSyncError.TooLarge)
        return when (val parsed = PersonalCollectionParser.parse(bytes, null, label.substringBeforeLast('.', label))) {
            is CollectionParseResult.Failure -> PersonalCollectionPreviewResult.Failed(CollectionSyncError.from(parsed.error))
            is CollectionParseResult.Success -> {
                val localNetworkUsed = usesLocalNetwork(hostsOf("", parsed.collection))
                PersonalCollectionPreviewResult.Ready(PersonalCollectionPreview(
                    parsed = parsed.collection,
                    sourceKind = PersonalCollectionSourceKind.FILE,
                    sourceUrl = "",
                    sourceLabel = label,
                    etag = "",
                    lastModified = "",
                    allowLocalNetwork = localNetworkUsed,
                    credentials = parsed.collection.credentials,
                    cleartextEntries = parsed.collection.cleartextCount(),
                    usesLocalNetwork = localNetworkUsed
                ))
            }
        }
    }

    private suspend fun previewRemote(
        source: PersonalCollectionImportSource.Remote,
        allowCleartext: Boolean
    ): PersonalCollectionPreviewResult {
        val url = source.url.trim().toHttpUrlOrNull()
            ?: return PersonalCollectionPreviewResult.Failed(CollectionSyncError.Malformed)
        val origin = PersonalCollectionUrlPolicy.origin(url)
        val username = source.username.ifBlank { url.username }
        val credentialSecret = source.password.ifBlank { url.password }
        val sourceCredential = username.takeIf(String::isNotBlank)?.let { CollectionCredential(origin, it, credentialSecret) }
        val cleanUrl = url.newBuilder().username("").password("").fragment(null).build()
        val rule = CollectionAccessRule(
            allowCleartext = allowCleartext,
            allowLocalNetwork = PersonalCollectionUrlPolicy.isLocalNetworkLiteral(cleanUrl.host),
            credentials = listOfNotNull(sourceCredential).associateBy { it.origin }
        )
        return when (val fetched = PersonalCollectionHttp.fetchPlaylist(cleanUrl.toString(), rule)) {
            PersonalCollectionHttp.PlaylistFetch.NotModified -> PersonalCollectionPreviewResult.Failed(CollectionSyncError.Server)
            is PersonalCollectionHttp.PlaylistFetch.Fetched -> {
                val fallbackName = PersonalCollectionParser.titleFromUrl(cleanUrl.toString())
                when (val parsed = PersonalCollectionParser.parse(fetched.bytes, fetched.finalUrl, fallbackName)) {
                    is CollectionParseResult.Failure -> PersonalCollectionPreviewResult.Failed(CollectionSyncError.from(parsed.error))
                    is CollectionParseResult.Success -> {
                        val credentials = (listOfNotNull(sourceCredential) + parsed.collection.credentials).distinctBy { it.origin }
                        val localNetworkUsed = usesLocalNetwork(hostsOf(cleanUrl.toString(), parsed.collection))
                        PersonalCollectionPreviewResult.Ready(
                            PersonalCollectionPreview(
                                parsed = parsed.collection,
                                sourceKind = PersonalCollectionSourceKind.URL,
                                sourceUrl = cleanUrl.toString(),
                                sourceLabel = PersonalCollectionUrlPolicy.displayUrl(cleanUrl.toString()),
                                etag = fetched.etag,
                                lastModified = fetched.lastModified,
                                allowLocalNetwork = localNetworkUsed,
                                credentials = credentials,
                                cleartextEntries = parsed.collection.cleartextCount(),
                                usesLocalNetwork = localNetworkUsed
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun save(
        preview: PersonalCollectionPreview,
        name: String,
        autoRefresh: Boolean,
        allowCleartext: Boolean
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString().replace("-", "").take(COLLECTION_ID_LENGTH)
        val plan = PersonalCollectionSyncPlanner.plan(id, emptyList(), preview.parsed.entries, now)
        val entity = PersonalCollectionEntity(
            id = id,
            name = PersonalCollectionParser.cleanText(name).ifBlank { preview.parsed.name },
            sourceKind = preview.sourceKind.name,
            sourceUrl = preview.sourceUrl,
            sourceLabel = preview.sourceLabel,
            format = preview.parsed.format.name,
            artworkUrl = preview.parsed.artworkUrl,
            artworkFile = "",
            allowCleartext = allowCleartext,
            allowLocalNetwork = preview.allowLocalNetwork,
            autoRefresh = autoRefresh && preview.sourceKind == PersonalCollectionSourceKind.URL,
            hosts = hostsOf(preview.sourceUrl, preview.parsed),
            etag = preview.etag,
            lastModified = preview.lastModified,
            lastSyncedAt = now,
            lastSyncAttemptAt = now,
            lastSyncError = "",
            createdAt = now,
            updatedAt = now
        )
        writeCredentials(id, preview.credentials)
        try {
            dao.replaceContents(entity, plan.tracks, emptyList())
        } catch (error: Throwable) {
            credentialStore.clear(credentialKey(id))
            throw error
        }
        reloadPolicy()
        cacheArtwork(id)
        PersonalCollectionSyncScheduler.update(appContext, dao.autoRefreshCount() > 0)
        id
    }

    suspend fun refresh(collectionId: String): PersonalCollectionSyncResult = withContext(Dispatchers.IO) {
        syncGate.tryRun(collectionId) { refreshLocked(collectionId) } ?: PersonalCollectionSyncResult.Busy
    }

    private suspend fun refreshLocked(collectionId: String): PersonalCollectionSyncResult {
        val collection = dao.byId(collectionId)
            ?: return PersonalCollectionSyncResult.Failed(CollectionSyncError.NotFound)
        if (collection.sourceKind != PersonalCollectionSourceKind.URL.name) {
            return PersonalCollectionSyncResult.Failed(CollectionSyncError.Unknown)
        }
        val now = System.currentTimeMillis()
        return try {
            val existing = dao.tracks(collectionId)
            val rule = ruleFor(collection)
            val validatorsUsable = existing.isNotEmpty()
            val fetched = PersonalCollectionHttp.fetchPlaylist(
                url = collection.sourceUrl,
                rule = rule,
                etag = if (validatorsUsable) collection.etag else "",
                lastModified = if (validatorsUsable) collection.lastModified else ""
            )
            when (fetched) {
                PersonalCollectionHttp.PlaylistFetch.NotModified -> {
                    dao.recordNotModified(collectionId, now)
                    PersonalCollectionSyncResult.Synced(PersonalCollectionSyncReport(0, 0, 0, 0, notModified = true))
                }
                is PersonalCollectionHttp.PlaylistFetch.Fetched -> {
                    when (val parsed = PersonalCollectionParser.parse(fetched.bytes, fetched.finalUrl, collection.name)) {
                        is CollectionParseResult.Failure -> {
                            val error = CollectionSyncError.from(parsed.error)
                            dao.recordSyncFailure(collectionId, now, error.name)
                            PersonalCollectionSyncResult.Failed(error)
                        }
                        is CollectionParseResult.Success -> {
                            val consentError = parsed.collection.consentError(
                                sourceUrl = collection.sourceUrl,
                                allowCleartext = collection.allowCleartext,
                                allowLocalNetwork = collection.allowLocalNetwork
                            )
                            if (consentError != null) {
                                dao.recordSyncFailure(collectionId, now, consentError.name)
                                PersonalCollectionSyncResult.Failed(consentError)
                            } else {
                                applySync(collection, existing, parsed.collection, fetched, now)
                            }
                        }
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val classified = CollectionSyncError.from(error)
            Timber.w("Personal collection refresh failed: %s", classified.name)
            dao.recordSyncFailure(collectionId, now, classified.name)
            PersonalCollectionSyncResult.Failed(classified)
        }
    }

    private suspend fun applySync(
        collection: PersonalCollectionEntity,
        existing: List<PersonalCollectionTrackEntity>,
        parsed: ParsedCollection,
        fetched: PersonalCollectionHttp.PlaylistFetch.Fetched,
        now: Long
    ): PersonalCollectionSyncResult {
        val plan = PersonalCollectionSyncPlanner.plan(collection.id, existing, parsed.entries, now)
        val updated = collection.copy(
            artworkUrl = parsed.artworkUrl,
            artworkFile = if (parsed.artworkUrl == collection.artworkUrl) collection.artworkFile else "",
            format = parsed.format.name,
            hosts = hostsOf(collection.sourceUrl, parsed),
            etag = fetched.etag,
            lastModified = fetched.lastModified,
            lastSyncedAt = now,
            lastSyncAttemptAt = now,
            lastSyncError = "",
            updatedAt = now
        )
        if (parsed.credentials.isNotEmpty()) {
            val known = readCredentials(collection.id)
            writeCredentials(collection.id, (known + parsed.credentials).distinctBy { it.origin })
        }
        dao.replaceContents(updated, plan.tracks, plan.removedEntryKeys)
        reloadPolicy()
        cacheArtwork(collection.id)
        pruneArtwork()
        return PersonalCollectionSyncResult.Synced(
            PersonalCollectionSyncReport(
                added = plan.added,
                removed = plan.removedEntryKeys.size,
                updated = plan.updated,
                skipped = parsed.skippedCount,
                notModified = false
            )
        )
    }

    suspend fun refreshAutomatic() {
        dao.all().filter { it.autoRefresh && it.sourceKind == PersonalCollectionSourceKind.URL.name }
            .forEach { refresh(it.id) }
    }

    suspend fun updateSettings(collectionId: String, name: String, autoRefresh: Boolean, allowCleartext: Boolean) =
        withContext(Dispatchers.IO) {
            val collection = dao.byId(collectionId) ?: return@withContext
            dao.updateSettings(
                id = collectionId,
                name = PersonalCollectionParser.cleanText(name).ifBlank { collection.name },
                autoRefresh = autoRefresh && collection.sourceKind == PersonalCollectionSourceKind.URL.name,
                allowCleartext = allowCleartext,
                now = System.currentTimeMillis()
            )
            reloadPolicy()
            PersonalCollectionSyncScheduler.update(appContext, dao.autoRefreshCount() > 0)
        }

    suspend fun delete(collectionId: String) = withContext(Dispatchers.IO) {
        syncGate.run(collectionId) {
            dao.deleteCollection(collectionId)
            credentialStore.clear(credentialKey(collectionId))
        }
        reloadPolicy()
        pruneArtwork()
        PersonalCollectionSyncScheduler.update(appContext, dao.autoRefreshCount() > 0)
    }

    suspend fun resolveForPlayback(track: Track): Track = withContext(Dispatchers.IO) {
        downloadedCopy(track)?.let { return@withContext it }
        ensurePolicy()
        val row = dao.trackById(track.id)
            ?: throw PersonalCollectionUnavailableException("Personal collection track no longer exists")
        row.toTrack(track.artworkModel()).copy(streamUrl = row.audioUrl, videoStreamUrl = "")
    }

    suspend fun resolveForDownload(track: Track): Track = withContext(Dispatchers.IO) {
        ensurePolicy()
        val row = dao.trackById(track.id)
            ?: throw PersonalCollectionUnavailableException("Personal collection track no longer exists")
        row.toTrack(track.artworkModel()).copy(streamUrl = row.audioUrl, videoStreamUrl = "")
    }

    private suspend fun downloadedCopy(track: Track): Track? {
        val download = runCatching { downloads.byTrackId(track.id) }.getOrNull() ?: return null
        val uri = Uri.parse(download.uri)
        val readable = runCatching {
            when (uri.scheme?.lowercase()) {
                "content" -> appContext.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                "file" -> File(uri.path.orEmpty()).let { it.isFile && it.length() > 0L }
                else -> false
            }
        }.getOrDefault(false)
        if (!readable) return null
        val base = dao.trackById(track.id)?.toTrack(track.artworkModel()) ?: track
        return base.copy(
            streamUrl = download.uri,
            videoStreamUrl = "",
            durationMs = base.durationMs.takeIf { it > 0L } ?: download.durationMs,
            source = "Offline"
        )
    }

    suspend fun ensurePolicy() {
        if (policyLoaded) return
        reloadPolicy()
    }

    private suspend fun reloadPolicy() = policyMutex.withLock {
        PersonalCollectionNetworkPolicy.replace(dao.all().associate { collection -> collection.id to ruleFor(collection) })
        policyLoaded = true
    }

    private fun ruleFor(collection: PersonalCollectionEntity): CollectionAccessRule = CollectionAccessRule(
        allowCleartext = collection.allowCleartext,
        allowLocalNetwork = collection.allowLocalNetwork,
        credentials = readCredentials(collection.id).associateBy { it.origin }
    )

    private suspend fun cacheArtwork(collectionId: String) {
        val collection = dao.byId(collectionId) ?: return
        val rule = ruleFor(collection)
        val rows = dao.tracks(collectionId)
        val pending = rows.filter { it.artworkUrl.isNotBlank() && (it.artworkFile.isBlank() || !File(it.artworkFile).isFile) }
        val urls = (pending.map { it.artworkUrl } + listOfNotNull(collection.artworkUrl.takeIf(String::isNotBlank)))
            .distinct()
            .take(MAX_ARTWORK_PER_SYNC)
        if (urls.isEmpty()) return
        val semaphore = Semaphore(ARTWORK_PARALLELISM)
        val stored = coroutineScope {
            urls.map { url ->
                async {
                    semaphore.withPermit { url to storeArtwork(collectionId, url, rule) }
                }
            }.awaitAll()
        }.mapNotNull { (url, file) -> file?.let { url to it.absolutePath } }.toMap()
        if (stored.isEmpty()) return
        val current = dao.byId(collectionId) ?: return
        val updatedRows = dao.tracks(collectionId).mapNotNull { row ->
            stored[row.artworkUrl]?.takeIf { it != row.artworkFile }?.let { row.copy(artworkFile = it) }
        }
        val updatedCollection = stored[current.artworkUrl]?.let { current.copy(artworkFile = it) } ?: current
        dao.replaceContents(updatedCollection, updatedRows, emptyList())
    }

    private suspend fun storeArtwork(collectionId: String, url: String, rule: CollectionAccessRule): File? {
        val target = File(artworkDirectory, "${stableHash("$collectionId\u0000$url", 32)}.jpg")
        if (target.isFile && target.length() > 0L) return target
        return try {
            val bytes = PersonalCollectionHttp.fetchArtwork(url, rule) ?: return null
            val encoded = downscale(bytes) ?: return null
            val temporary = File(artworkDirectory, "${target.name}.part")
            temporary.writeBytes(encoded)
            if (!temporary.renameTo(target)) {
                temporary.delete()
                return null
            }
            target
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d("Personal collection artwork skipped: %s", error.javaClass.simpleName)
            null
        }
    }

    private fun downscale(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_ARTWORK_PX) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_QUALITY, output)
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun pruneArtwork() {
        val referenced = (dao.referencedTrackArtworkFiles() + dao.referencedCollectionArtworkFiles())
            .mapTo(HashSet()) { File(it).name }
        artworkDirectory.listFiles()?.forEach { file ->
            if (file.name !in referenced) file.delete()
        }
    }

    private fun readCredentials(collectionId: String): List<CollectionCredential> {
        val raw = credentialStore.read(credentialKey(collectionId)) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                CollectionCredential(item.optString("o"), item.optString("u"), item.optString("p"))
                    .takeIf { it.origin.isNotBlank() && it.username.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeCredentials(collectionId: String, credentials: List<CollectionCredential>) {
        if (credentials.isEmpty()) {
            credentialStore.clear(credentialKey(collectionId))
            return
        }
        val array = JSONArray()
        credentials.forEach { credential ->
            array.put(JSONObject().put("o", credential.origin).put("u", credential.username).put("p", credential.password))
        }
        credentialStore.write(credentialKey(collectionId), array.toString())
    }

    private fun displayName(uri: Uri): String = runCatching {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.let(PersonalCollectionParser::cleanText)?.takeIf(String::isNotBlank) ?: "Playlist"

    private fun PersonalCollectionSummaryRow.toSummary(): PersonalCollectionSummary = PersonalCollectionSummary(
        id = id,
        name = name,
        sourceKind = PersonalCollectionSourceKind.entries.firstOrNull { it.name == sourceKind } ?: PersonalCollectionSourceKind.FILE,
        sourceLabel = sourceLabel,
        format = PersonalCollectionFormat.entries.firstOrNull { it.name == format } ?: PersonalCollectionFormat.M3U,
        artworkModel = artworkModel(artworkFile),
        autoRefresh = autoRefresh,
        allowCleartext = allowCleartext,
        lastSyncedAt = lastSyncedAt,
        lastSyncAttemptAt = lastSyncAttemptAt,
        lastSyncError = CollectionSyncError.fromStorage(lastSyncError),
        createdAt = createdAt,
        trackCount = trackCount,
        durationMs = durationMs,
        usesLocalNetwork = usesLocalNetwork(hosts)
    )

    companion object {
        private const val ARTWORK_DIRECTORY = "personal_collections/artwork"
        private const val COLLECTION_ID_LENGTH = 16
        private const val MAX_ARTWORK_PER_SYNC = 800
        private const val ARTWORK_PARALLELISM = 4
        private const val MAX_ARTWORK_PX = 1024
        private const val ARTWORK_QUALITY = 90

        @Volatile private var instance: PersonalCollectionRepository? = null

        fun get(context: Context): PersonalCollectionRepository =
            instance ?: synchronized(this) {
                instance ?: PersonalCollectionRepository(context).also { instance = it }
            }

        private fun credentialKey(collectionId: String) = "personal_collection.$collectionId"

        internal fun artworkModel(path: String): String =
            path.takeIf(String::isNotBlank)?.let { Uri.fromFile(File(it)).toString() }.orEmpty()

        internal fun usesLocalNetwork(hosts: String): Boolean =
            hosts.split(',').any { key ->
                key.isNotBlank() && PersonalCollectionUrlPolicy.isLocalNetworkLiteral(key.substringBeforeLast(':'))
            }

        internal fun hostsOf(sourceUrl: String, parsed: ParsedCollection): String {
            val hosts = LinkedHashSet<String>()
            PersonalCollectionNetworkPolicy.hostKey(sourceUrl)?.let(hosts::add)
            PersonalCollectionNetworkPolicy.hostKey(parsed.artworkUrl)?.let(hosts::add)
            parsed.entries.forEach { entry ->
                PersonalCollectionNetworkPolicy.hostKey(entry.audioUrl)?.let(hosts::add)
                PersonalCollectionNetworkPolicy.hostKey(entry.videoUrl)?.let(hosts::add)
                PersonalCollectionNetworkPolicy.hostKey(entry.artworkUrl)?.let(hosts::add)
            }
            return hosts.joinToString(",")
        }
    }
}

internal fun ParsedCollection.cleartextCount(): Int = entries.count { PersonalCollectionUrlPolicy.isCleartext(it.audioUrl) }

internal fun ParsedCollection.usesCleartext(): Boolean =
    PersonalCollectionUrlPolicy.isCleartext(artworkUrl) || entries.any { entry ->
        PersonalCollectionUrlPolicy.isCleartext(entry.audioUrl) ||
            PersonalCollectionUrlPolicy.isCleartext(entry.videoUrl) ||
            PersonalCollectionUrlPolicy.isCleartext(entry.artworkUrl)
    }

internal fun ParsedCollection.consentError(
    sourceUrl: String,
    allowCleartext: Boolean,
    allowLocalNetwork: Boolean
): CollectionSyncError? = when {
    usesCleartext() && !allowCleartext -> CollectionSyncError.Cleartext
    PersonalCollectionRepository.usesLocalNetwork(PersonalCollectionRepository.hostsOf(sourceUrl, this)) && !allowLocalNetwork ->
        CollectionSyncError.LocalNetwork
    else -> null
}

internal fun PersonalCollectionTrackRow.toTrack(): Track =
    track.toTrack(PersonalCollectionRepository.artworkModel(collectionArtworkFile))

internal fun PersonalCollectionTrackEntity.toTrack(fallbackArtwork: String = ""): Track {
    val artwork = PersonalCollectionRepository.artworkModel(artworkFile).ifBlank { fallbackArtwork }
    return Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        source = PERSONAL_COLLECTION_SOURCE,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        year = year.takeIf { it > 0 }?.toString().orEmpty(),
        albumArtist = albumArtist,
        trackNumber = trackNumber,
        discNumber = discNumber
    )
}

private fun Track.artworkModel(): String = largeThumbnailUrl.ifBlank { thumbnailUrl }
