package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.local.LEVYRA_DATABASE_VERSION
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.ListenEventEntity
import com.luc4n3x.levyra.data.local.PlaybackQueueItemEntity
import com.luc4n3x.levyra.data.local.PlaybackQueueStateEntity
import com.luc4n3x.levyra.data.local.PlaylistEntity
import com.luc4n3x.levyra.data.local.toFavoriteTrackEntity
import com.luc4n3x.levyra.data.local.toPlaylistTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraBackupFrequency
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.Track
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class LevyraBackupManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val database = LevyraDatabase.get(appContext)
    private val preferences = LevyraPreferences(appContext)
    private val followedArtistsStore = FollowedArtistsStore(appContext)
    private val operationMutex = Mutex()

    suspend fun exportTo(uri: Uri): LevyraBackupResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val output = appContext.contentResolver.openOutputStream(uri, "w")
                ?: throw IOException("Impossibile aprire il file di backup")
            val result = output.use { writeArchive(it) }
            preferences.setLastBackupAt(System.currentTimeMillis())
            result
        }
    }

    suspend fun exportAutomatic(retentionCount: Int): LevyraBackupResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val directory = File(appContext.filesDir, AUTOMATIC_BACKUP_DIRECTORY)
            if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
                throw IOException("Impossibile creare la cartella dei backup automatici")
            }
            val canonicalDirectory = directory.canonicalFile
            val timestamp = System.currentTimeMillis()
            val target = File(canonicalDirectory, "$AUTOMATIC_BACKUP_PREFIX$timestamp$VAULT_EXTENSION")
            val temporary = File(canonicalDirectory, ".$AUTOMATIC_BACKUP_PREFIX$timestamp.tmp")
            checkBackupChild(canonicalDirectory, target)
            checkBackupChild(canonicalDirectory, temporary)
            val result = try {
                val archiveResult = temporary.outputStream().buffered().use { writeArchive(it) }
                if (!temporary.renameTo(target)) throw IOException("Impossibile finalizzare il backup automatico")
                archiveResult
            } finally {
                if (temporary.exists()) temporary.delete()
            }
            runCatching {
                pruneAutomaticBackups(canonicalDirectory, retentionCount.coerceIn(1, MAX_AUTOMATIC_BACKUPS))
            }.onFailure { error ->
                Timber.w(error, "Automatic backup retention cleanup failed")
            }
            preferences.setLastBackupAt(System.currentTimeMillis())
            result
        }
    }

    suspend fun inspect(uri: Uri): VaultPreview = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw IOException("Impossibile leggere il backup")
            input.use { stream ->
                ZipInputStream(stream.buffered()).use { zip ->
                    var totalBytes = 0L
                    var entryCount = 0
                    var manifestBytes: ByteArray? = null
                    var hasLegacyPayload = false
                    val sectionNames = mutableListOf<String>()
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entryCount += 1
                        if (entryCount > MAX_ZIP_ENTRIES) throw IOException("Backup non valido: troppe voci ZIP")
                        if (entry.isDirectory) throw IOException("Backup non valido: voce ZIP directory")
                        if (!vaultEntryAllowed(entry.name)) throw IOException("Backup non valido: voce ZIP inattesa ${entry.name}")
                        if (entry.size > MAX_ENTRY_BYTES) throw IOException("Backup troppo grande")
                        if (entry.size > 0L) {
                            totalBytes += entry.size
                            if (totalBytes > MAX_TOTAL_BYTES) throw IOException("Backup troppo grande")
                        }
                        when {
                            entry.name == MANIFEST_ENTRY -> manifestBytes = readZipEntry(zip, MAX_ENTRY_BYTES)
                            entry.name == LEGACY_PAYLOAD_ENTRY -> hasLegacyPayload = true
                            else -> sectionNames.add(entry.name)
                        }
                        zip.closeEntry()
                    }
                    val manifest = manifestBytes?.let { bytes ->
                        runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }.getOrNull()
                    }
                    val vaultCompatible = manifest != null &&
                        manifest.optInt("formatVersion", 0) == FORMAT_VERSION &&
                        manifest.optString("platform", "").equals(PLATFORM, ignoreCase = true) &&
                        manifest.optInt("databaseVersion", 0) <= LEVYRA_DATABASE_VERSION
                    val legacyCompatible = hasLegacyPayload &&
                        manifest?.optInt("schemaVersion", 0) == LEGACY_SCHEMA_VERSION
                    VaultPreview(
                        createdAt = manifest?.optLong("createdAt", 0L) ?: 0L,
                        appVersion = manifest?.optString("appVersion", "").orEmpty(),
                        databaseVersion = manifest?.optInt("databaseVersion", 0) ?: 0,
                        formatVersion = manifest?.optInt("formatVersion", 0) ?: 0,
                        platform = manifest?.optString("platform", "").orEmpty(),
                        sizeBytes = totalBytes,
                        sections = if (legacyCompatible) listOf(LEGACY_PAYLOAD_ENTRY) else sectionNames,
                        compatible = vaultCompatible || legacyCompatible
                    )
                }
            }
        }
    }

    suspend fun restoreFrom(uri: Uri): LevyraBackupResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val entries = readArchiveEntries(uri)
            val target = if (entries.containsKey(LEGACY_PAYLOAD_ENTRY)) {
                legacySnapshot(entries)
            } else {
                vaultSnapshot(entries)
            }
            val rollback = currentSnapshot()
            try {
                applySnapshot(target)
            } catch (restoreError: Throwable) {
                runCatching { applySnapshot(rollback) }
                    .onFailure { rollbackError -> restoreError.addSuppressed(rollbackError) }
                throw restoreError
            }
            LevyraBackupResult("Ripristino completato", "")
        }
    }

    private suspend fun writeArchive(output: OutputStream): LevyraBackupResult {
        val sections = linkedMapOf<String, SectionInfo>()
        ZipOutputStream(output.buffered()).use { zip ->
            sections[FAVORITES_ENTRY] = writeJsonSection(zip, FAVORITES_ENTRY) { writer ->
                val favorites = database.favoriteTracksDao().all().map { it.toTrack() }
                writeJsonArray(writer, favorites) { TrackJson.toJson(it).toString() }
            }
            sections[FOLLOWED_ARTISTS_ENTRY] = writeJsonSection(zip, FOLLOWED_ARTISTS_ENTRY) { writer ->
                writeJsonArray(writer, followedArtistsStore.load()) { followedArtistToJson(it).toString() }
            }
            sections[PLAYLISTS_ENTRY] = writeJsonSection(zip, PLAYLISTS_ENTRY) { writer ->
                val playlists = database.playlistDao().allPlaylists()
                writer.write("[")
                playlists.forEachIndexed { index, playlist ->
                    if (index > 0) writer.write(",")
                    val tracks = database.playlistDao().tracksOf(playlist.id)
                    val json = JSONObject()
                        .put("id", playlist.id)
                        .put("name", playlist.name)
                        .put("coverUrl", playlist.coverUrl)
                        .put("createdAt", playlist.createdAt)
                        .put("updatedAt", playlist.updatedAt)
                        .put("tracks", JSONArray().apply { tracks.forEach { put(TrackJson.toJson(it.toTrack())) } })
                        .toString()
                    writer.write(json)
                }
                writer.write("]")
            }
            sections[HISTORY_ENTRY] = writeJsonSection(zip, HISTORY_ENTRY) { writer ->
                writeJsonArray(writer, database.listenEventsDao().all()) { listenEventToJson(it).toString() }
            }
            sections[SETTINGS_ENTRY] = writeJsonSection(zip, SETTINGS_ENTRY) { writer ->
                writer.write(settingsToJson(preferences.snapshot()).toString())
            }
            sections[QUEUE_ENTRY] = writeJsonSection(zip, QUEUE_ENTRY) { writer ->
                val queueItems = database.playbackQueueDao().items()
                val queueState = database.playbackQueueDao().state()
                writer.write("{\"items\":")
                writeJsonArray(writer, queueItems) {
                    JSONObject().put("position", it.position).put("payload", it.payload).put("identity", it.identity).toString()
                }
                writer.write(",\"state\":")
                writer.write(queueState?.let(::queueStateToJson)?.toString() ?: "null")
                writer.write("}")
            }
            val manifest = JSONObject()
                .put("formatVersion", FORMAT_VERSION)
                .put("appVersion", BuildConfig.VERSION_NAME)
                .put("platform", PLATFORM)
                .put("databaseVersion", LEVYRA_DATABASE_VERSION)
                .put("createdAt", System.currentTimeMillis())
                .put("sections", JSONObject().apply {
                    sections.forEach { (name, info) ->
                        put(name, JSONObject().put("sha256", info.sha256).put("bytes", info.bytes))
                    }
                })
            sections[MANIFEST_ENTRY] = writeJsonSection(zip, MANIFEST_ENTRY) { writer ->
                writer.write(manifest.toString())
            }
        }
        return writeArchiveResult(combinedChecksum(sections))
    }

    private fun writeArchiveResult(checksum: String): LevyraBackupResult =
        LevyraBackupResult("Backup completato", checksum)

    private suspend fun writeJsonSection(
        zip: ZipOutputStream,
        name: String,
        write: suspend (OutputStreamWriter) -> Unit
    ): SectionInfo {
        zip.putNextEntry(ZipEntry(name))
        val digest = MessageDigest.getInstance("SHA-256")
        val counter = ByteCounterOutputStream(zip)
        val digestStream = DigestOutputStream(counter, digest)
        val writer = OutputStreamWriter(digestStream, Charsets.UTF_8)
        write(writer)
        writer.flush()
        zip.closeEntry()
        return SectionInfo(sha256Hex(digest.digest()), counter.count)
    }

    private fun <T> writeJsonArray(writer: OutputStreamWriter, items: List<T>, encode: (T) -> String) {
        writer.write("[")
        items.forEachIndexed { index, item ->
            if (index > 0) writer.write(",")
            writer.write(encode(item))
        }
        writer.write("]")
    }

    private fun readArchiveEntries(uri: Uri): Map<String, ByteArray> {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Impossibile leggere il backup")
        val entries = mutableMapOf<String, ByteArray>()
        input.use { stream ->
            ZipInputStream(stream.buffered()).use { zip ->
                var totalBytes = 0L
                var entryCount = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount += 1
                    if (entryCount > MAX_ZIP_ENTRIES) throw IOException("Backup non valido: troppe voci ZIP")
                    if (entry.isDirectory) throw IOException("Backup non valido: voce ZIP directory")
                    if (!vaultEntryAllowed(entry.name)) throw IOException("Backup non valido: voce ZIP inattesa ${entry.name}")
                    if (entries.containsKey(entry.name)) throw IOException("Backup non valido: voce duplicata ${entry.name}")
                    if (entry.size > MAX_ENTRY_BYTES) throw IOException("Backup troppo grande")
                    val bytes = readZipEntry(zip, MAX_ENTRY_BYTES)
                    totalBytes += bytes.size
                    if (totalBytes > MAX_TOTAL_BYTES) throw IOException("Backup troppo grande")
                    entries[entry.name] = bytes
                    zip.closeEntry()
                }
            }
        }
        return entries
    }

    private suspend fun vaultSnapshot(entries: Map<String, ByteArray>): VaultSnapshot {
        val manifestBytes = entries[MANIFEST_ENTRY] ?: throw IOException("Manifest del backup mancante")
        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val format = manifest.optInt("formatVersion", 0)
        if (format != FORMAT_VERSION) throw IOException("Versione Vault non supportata: $format")
        if (!manifest.optString("platform", "").equals(PLATFORM, ignoreCase = true)) {
            throw IOException("Backup creato per un'altra piattaforma")
        }
        val databaseVersion = manifest.optInt("databaseVersion", 0)
        if (databaseVersion > LEVYRA_DATABASE_VERSION) {
            throw IOException("Backup creato da una versione più recente dell'app")
        }
        validateSectionChecksums(entries, manifest)
        val settingsBytes = entries[SETTINGS_ENTRY] ?: throw IOException("Dati impostazioni del backup mancanti")
        val settingsJson = JSONObject(settingsBytes.toString(Charsets.UTF_8))
        return VaultSnapshot(
            settings = parseSettings(settingsJson),
            favorites = entries[FAVORITES_ENTRY].toJsonArray().toTrackList(),
            followedArtists = parseFollowedArtists(entries[FOLLOWED_ARTISTS_ENTRY].toJsonArray()),
            playlists = parsePlaylists(entries[PLAYLISTS_ENTRY].toJsonArray()),
            history = parseHistory(entries[HISTORY_ENTRY].toJsonArray()),
            queueItems = parseQueueItems(entries[QUEUE_ENTRY].toJsonObject()),
            queueState = parseQueueState(entries[QUEUE_ENTRY].toJsonObject())
        )
    }

    private suspend fun legacySnapshot(entries: Map<String, ByteArray>): VaultSnapshot {
        val manifestBytes = entries[MANIFEST_ENTRY] ?: throw IOException("Manifest del backup mancante")
        val payloadBytes = entries[LEGACY_PAYLOAD_ENTRY] ?: throw IOException("Dati del backup mancanti")
        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val schema = manifest.optInt("schemaVersion", 0)
        if (schema != LEGACY_SCHEMA_VERSION) throw IOException("Versione backup non supportata: $schema")
        val expected = manifest.optString("payloadSha256")
        val actual = sha256Hex(MessageDigest.getInstance("SHA-256").digest(payloadBytes))
        if (expected.isBlank() || !expected.equals(actual, ignoreCase = true)) {
            throw IOException("Backup danneggiato: checksum non valido")
        }
        val root = JSONObject(payloadBytes.toString(Charsets.UTF_8))
        val queue = root.optJSONObject("queue")
        return VaultSnapshot(
            settings = parseSettings(root.optJSONObject("settings") ?: JSONObject()),
            favorites = root.optJSONArray("favorites").toTrackList(),
            followedArtists = parseFollowedArtists(root.optJSONArray("followedArtists")),
            playlists = parsePlaylists(root.optJSONArray("playlists") ?: JSONArray()),
            history = parseHistory(root.optJSONArray("history")),
            queueItems = parseQueueItems(queue),
            queueState = parseQueueState(queue)
        )
    }

    private fun validateSectionChecksums(entries: Map<String, ByteArray>, manifest: JSONObject) {
        val sections = manifest.optJSONObject("sections") ?: throw IOException("Backup non valido: sezioni mancanti")
        for ((name, bytes) in entries) {
            if (name == MANIFEST_ENTRY) continue
            val section = sections.optJSONObject(name) ?: throw IOException("Backup non valido: sezione sconosciuta $name")
            val expected = section.optString("sha256")
            val declaredBytes = section.optLong("bytes", -1L)
            if (declaredBytes >= 0L && declaredBytes != bytes.size.toLong()) {
                throw IOException("Backup non valido: dimensione errata per $name")
            }
            val actual = sha256Hex(MessageDigest.getInstance("SHA-256").digest(bytes))
            if (expected.isBlank() || !expected.equals(actual, ignoreCase = true)) {
                throw IOException("Backup danneggiato: checksum non valido per $name")
            }
        }
    }

    private suspend fun currentSnapshot(): VaultSnapshot {
        val snapshot = preferences.snapshot()
        val playlists = database.playlistDao().allPlaylists().map { playlist ->
            PlaylistBackup(
                id = playlist.id,
                name = playlist.name,
                coverUrl = playlist.coverUrl,
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt,
                tracks = database.playlistDao().tracksOf(playlist.id).map { it.toTrack() }
            )
        }
        return VaultSnapshot(
            settings = snapshot,
            favorites = database.favoriteTracksDao().all().map { it.toTrack() },
            followedArtists = followedArtistsStore.load(),
            playlists = playlists,
            history = database.listenEventsDao().all(),
            queueItems = database.playbackQueueDao().items(),
            queueState = database.playbackQueueDao().state()
        )
    }

    private suspend fun applySnapshot(payload: VaultSnapshot) {
        preferences.restoreSnapshot(payload.settings)
        followedArtistsStore.saveDurable(payload.followedArtists)
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.favoriteTracksDao().replaceAll(payload.favorites.mapIndexed { index, track -> track.toFavoriteTrackEntity(now - index) })
            restorePlaylists(payload.playlists)
            database.listenEventsDao().replaceAll(payload.history)
            restoreQueue(payload.queueItems, payload.queueState)
        }
        AutomaticBackupScheduler.schedule(appContext, payload.settings.backupSettings)
    }

    private suspend fun restorePlaylists(playlists: List<PlaylistBackup>) {
        val dao = database.playlistDao()
        dao.clearAll()
        playlists.forEach { playlist ->
            if (playlist.id.isBlank()) return@forEach
            dao.upsertPlaylist(
                PlaylistEntity(
                    playlist.id,
                    playlist.name.ifBlank { "Playlist" },
                    playlist.coverUrl,
                    playlist.createdAt,
                    playlist.updatedAt
                )
            )
            if (playlist.tracks.isNotEmpty()) {
                dao.insertTracks(
                    playlist.tracks.mapIndexed { position, track ->
                        track.toPlaylistTrackEntity(playlist.id, position, playlist.createdAt + position)
                    }
                )
            }
        }
    }

    private suspend fun restoreQueue(items: List<PlaybackQueueItemEntity>, state: PlaybackQueueStateEntity?) {
        val dao = database.playbackQueueDao()
        if (state == null) {
            dao.clear()
        } else {
            dao.replace(items, state)
        }
    }

    private fun parseQueueItems(json: JSONObject?): List<PlaybackQueueItemEntity> {
        if (json == null) return emptyList()
        val itemsArray = json.optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until itemsArray.length()) {
                val item = itemsArray.optJSONObject(index) ?: continue
                add(PlaybackQueueItemEntity(item.optInt("position"), item.optString("payload"), item.optString("identity")))
            }
        }
    }

    private fun parseQueueState(json: JSONObject?): PlaybackQueueStateEntity? {
        if (json == null) return null
        val stateJson = json.optJSONObject("state") ?: return null
        return PlaybackQueueStateEntity(
            currentIndex = stateJson.optInt("currentIndex", -1),
            positionMs = stateJson.optLong("positionMs"),
            shuffleEnabled = stateJson.optBoolean("shuffleEnabled"),
            shuffleOrder = stateJson.optString("shuffleOrder"),
            shuffleCursor = stateJson.optInt("shuffleCursor", -1),
            history = stateJson.optString("history"),
            repeatMode = stateJson.optString("repeatMode", "Off"),
            radioEnabled = stateJson.optBoolean("radioEnabled", true),
            generation = stateJson.optLong("generation"),
            updatedAt = stateJson.optLong("updatedAt")
        )
    }

    private fun parsePlaylists(array: JSONArray?): List<PlaylistBackup> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val id = json.optString("id").trim()
                if (id.isBlank()) continue
                val createdAt = json.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = json.optLong("updatedAt", createdAt)
                add(
                    PlaylistBackup(
                        id = id,
                        name = json.optString("name", "Playlist"),
                        coverUrl = json.optString("coverUrl"),
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        tracks = json.optJSONArray("tracks").toTrackList()
                    )
                )
            }
        }
    }

    private fun settingsToJson(snapshot: LevyraPreferencesSnapshot): JSONObject {
        return JSONObject()
            .put("onboarded", snapshot.onboarded)
            .put("tastes", JSONArray(snapshot.tastes.toList()))
            .put("userName", snapshot.userName)
            .put("languageCode", snapshot.languageCode)
            .put("animationsEnabled", snapshot.animationsEnabled)
            .put("motionArtworkEnabled", snapshot.motionArtworkEnabled)
            .put("dynamicColor", snapshot.dynamicColor)
            .put("sponsorBlock", snapshot.sponsorBlock)
            .put("skipSilence", snapshot.skipSilence)
            .put("audioQuality", snapshot.audioQuality)
            .put("audioNormalization", snapshot.audioNormalization)
            .put("lyricsTranslationEnabled", snapshot.lyricsTranslationEnabled)
            .put("themePreset", snapshot.themePreset)
            .put("audioSettings", audioSettingsToJson(snapshot.audioSettings))
            .put("interfaceSettings", interfaceSettingsToJson(snapshot.interfaceSettings))
            .put("downloadSettings", downloadSettingsToJson(snapshot.downloadSettings))
            .put("backupSettings", backupSettingsToJson(snapshot.backupSettings))
            .put("jamDisplayName", snapshot.jamDisplayName)
            .put("recentSearches", JSONArray().apply { snapshot.recentSearches.forEach { put(TrackJson.toJson(it)) } })
            .put("personalOrbitTracks", JSONArray().apply { snapshot.personalOrbitTracks.forEach { put(TrackJson.toJson(it)) } })
            .put("lastTrack", snapshot.lastTrack?.let(TrackJson::toJson) ?: JSONObject.NULL)
            .put("lastPositionMs", snapshot.lastPositionMs)
    }

    private fun parseSettings(json: JSONObject): LevyraPreferencesSnapshot {
        return LevyraPreferencesSnapshot(
            onboarded = json.optBoolean("onboarded", false),
            tastes = json.optJSONArray("tastes").toStringSet(),
            userName = json.optString("userName"),
            languageCode = json.optString("languageCode"),
            animationsEnabled = json.optBoolean("animationsEnabled", true),
            motionArtworkEnabled = json.optBoolean("motionArtworkEnabled", true),
            dynamicColor = json.optBoolean("dynamicColor", true),
            sponsorBlock = json.optBoolean("sponsorBlock", true),
            skipSilence = json.optBoolean("skipSilence", false),
            audioQuality = json.optString("audioQuality", "Auto"),
            dismissedUpdateVersion = preferences.dismissedUpdateVersion(),
            lastTrack = json.optJSONObject("lastTrack")?.let(TrackJson::fromJson),
            lastPositionMs = json.optLong("lastPositionMs").coerceAtLeast(0L),
            recentSearches = json.optJSONArray("recentSearches").toTrackList(),
            personalOrbitTracks = json.optJSONArray("personalOrbitTracks").toTrackList(),
            audioNormalization = json.optBoolean("audioNormalization", false),
            lyricsTranslationEnabled = json.optBoolean("lyricsTranslationEnabled", false),
            themePreset = json.optString("themePreset"),
            audioSettings = parseAudioSettings(json.optJSONObject("audioSettings")),
            interfaceSettings = parseInterfaceSettings(json.optJSONObject("interfaceSettings")),
            downloadSettings = parseDownloadSettings(json.optJSONObject("downloadSettings")),
            backupSettings = parseBackupSettings(json.optJSONObject("backupSettings")),
            jamDisplayName = json.optString("jamDisplayName")
        )
    }

    private fun audioSettingsToJson(value: LevyraAudioSettings): JSONObject =
        backupAudioSettingsToJson(value)

    private fun parseAudioSettings(json: JSONObject?): LevyraAudioSettings =
        backupAudioSettingsFromJson(json)

    private fun interfaceSettingsToJson(value: LevyraInterfaceSettings): JSONObject = JSONObject()
        .put("compactHome", value.compactHome)
        .put("showPersonalOrbit", value.showPersonalOrbit)
        .put("showResonance", value.showResonance)
        .put("showNewReleases", value.showNewReleases)
        .put("showAlbumsForYou", value.showAlbumsForYou)
        .put("showTrendingArtists", value.showTrendingArtists)
        .put("showCharts", value.showCharts)
        .put("fontPreset", value.fontPreset.name)
        .put("playerGesturesEnabled", value.playerGesturesEnabled)
        .put("doubleTapSeekSeconds", value.doubleTapSeekSeconds)
        .put("longPressSpeed", value.longPressSpeed.toDouble())
        .put("pureBlack", value.pureBlack)
        .put("hapticFeedback", value.hapticFeedback)

    private fun parseInterfaceSettings(json: JSONObject?): LevyraInterfaceSettings {
        if (json == null) return LevyraInterfaceSettings()
        return LevyraInterfaceSettings(
            compactHome = json.optBoolean("compactHome"),
            showPersonalOrbit = json.optBoolean("showPersonalOrbit", true),
            showResonance = json.optBoolean("showResonance", true),
            showNewReleases = json.optBoolean("showNewReleases", true),
            showAlbumsForYou = json.optBoolean("showAlbumsForYou", true),
            showTrendingArtists = json.optBoolean("showTrendingArtists", true),
            showCharts = json.optBoolean("showCharts", true),
            fontPreset = LevyraFontPreset.from(json.optString("fontPreset")),
            playerGesturesEnabled = json.optBoolean("playerGesturesEnabled", true),
            doubleTapSeekSeconds = json.optInt("doubleTapSeekSeconds", 10),
            longPressSpeed = json.optDouble("longPressSpeed", 2.0).toFloat(),
            pureBlack = json.optBoolean("pureBlack", false),
            hapticFeedback = json.optBoolean("hapticFeedback", true)
        ).normalized()
    }

    private fun downloadSettingsToJson(value: LevyraDownloadSettings): JSONObject = JSONObject()
        .put("wifiOnly", value.wifiOnly)
        .put("chargingOnly", value.chargingOnly)
        .put("resumable", value.resumable)
        .put("maxConcurrentDownloads", value.maxConcurrentDownloads)

    private fun parseDownloadSettings(json: JSONObject?): LevyraDownloadSettings {
        if (json == null) return LevyraDownloadSettings()
        return LevyraDownloadSettings(
            wifiOnly = json.optBoolean("wifiOnly"),
            chargingOnly = json.optBoolean("chargingOnly"),
            resumable = json.optBoolean("resumable", true),
            maxConcurrentDownloads = json.optInt("maxConcurrentDownloads", 2)
        ).normalized()
    }

    private fun backupSettingsToJson(value: LevyraBackupSettings): JSONObject = JSONObject()
        .put("enabled", value.enabled)
        .put("frequency", value.frequency.name)
        .put("retentionCount", value.retentionCount)
        .put("chargingOnly", value.chargingOnly)
        .put("preUpdate", value.preUpdate)

    private fun parseBackupSettings(json: JSONObject?): LevyraBackupSettings {
        if (json == null) return LevyraBackupSettings()
        return LevyraBackupSettings(
            enabled = json.optBoolean("enabled"),
            frequency = LevyraBackupFrequency.from(json.optString("frequency")),
            retentionCount = json.optInt("retentionCount", 5),
            chargingOnly = json.optBoolean("chargingOnly", true),
            preUpdate = json.optBoolean("preUpdate", true)
        ).normalized()
    }

    private fun followedArtistToJson(value: FollowedArtist): JSONObject = JSONObject()
        .put("browseId", value.browseId)
        .put("name", value.name)
        .put("thumbnailUrl", value.thumbnailUrl)
        .put("followedAt", value.followedAt)

    private fun parseFollowedArtists(array: JSONArray?): List<FollowedArtist> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val name = json.optString("name").trim()
                if (name.isBlank()) continue
                add(FollowedArtist(json.optString("browseId"), name, json.optString("thumbnailUrl"), json.optLong("followedAt")))
            }
        }
    }

    private fun listenEventToJson(value: ListenEventEntity): JSONObject = JSONObject()
        .put("trackId", value.trackId)
        .put("title", value.title)
        .put("artist", value.artist)
        .put("album", value.album)
        .put("durationMs", value.durationMs)
        .put("videoUrl", value.videoUrl)
        .put("thumbnailUrl", value.thumbnailUrl)
        .put("largeThumbnailUrl", value.largeThumbnailUrl)
        .put("source", value.source)
        .put("artistBrowseIds", value.artistBrowseIds)
        .put("listenedMs", value.listenedMs)
        .put("completed", value.completed)
        .put("startedAt", value.startedAt)

    private fun parseHistory(array: JSONArray?): List<ListenEventEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val trackId = json.optString("trackId")
                if (trackId.isBlank()) continue
                add(
                    ListenEventEntity(
                        trackId = trackId,
                        title = json.optString("title"),
                        artist = json.optString("artist"),
                        album = json.optString("album"),
                        durationMs = json.optLong("durationMs"),
                        videoUrl = json.optString("videoUrl"),
                        thumbnailUrl = json.optString("thumbnailUrl"),
                        largeThumbnailUrl = json.optString("largeThumbnailUrl"),
                        source = json.optString("source"),
                        artistBrowseIds = json.optString("artistBrowseIds"),
                        listenedMs = json.optLong("listenedMs"),
                        completed = json.optBoolean("completed"),
                        startedAt = json.optLong("startedAt")
                    )
                )
            }
        }
    }

    private fun queueStateToJson(value: PlaybackQueueStateEntity): JSONObject = JSONObject()
        .put("currentIndex", value.currentIndex)
        .put("positionMs", value.positionMs)
        .put("shuffleEnabled", value.shuffleEnabled)
        .put("shuffleOrder", value.shuffleOrder)
        .put("shuffleCursor", value.shuffleCursor)
        .put("history", value.history)
        .put("repeatMode", value.repeatMode)
        .put("radioEnabled", value.radioEnabled)
        .put("generation", value.generation)
        .put("updatedAt", value.updatedAt)

    private fun JSONArray?.toTrackList(): List<Track> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) optJSONObject(index)?.let(TrackJson::fromJson)?.let(::add)
        }
    }

    private fun ByteArray?.toJsonArray(): JSONArray? {
        if (this == null) return null
        return try {
            JSONArray(toString(Charsets.UTF_8))
        } catch (error: org.json.JSONException) {
            throw IOException("Backup non valido: sezione JSON danneggiata")
        }
    }

    private fun ByteArray?.toJsonObject(): JSONObject? {
        if (this == null) return null
        return try {
            JSONObject(toString(Charsets.UTF_8))
        } catch (error: org.json.JSONException) {
            throw IOException("Backup non valido: sezione JSON danneggiata")
        }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet { for (index in 0 until length()) optString(index).takeIf { it.isNotBlank() }?.let(::add) }
    }

    private fun readZipEntry(zip: ZipInputStream, limit: Long): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = zip.read(chunk)
            if (read < 0) break
            total += read
            if (total > limit) throw IOException("Backup troppo grande")
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun sha256Hex(digest: ByteArray): String = digest.joinToString("") { "%02x".format(it) }

    private fun combinedChecksum(sections: Map<String, SectionInfo>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        sections.toSortedMap().forEach { (name, info) ->
            digest.update(name.toByteArray(Charsets.UTF_8))
            digest.update(info.sha256.toByteArray(Charsets.UTF_8))
        }
        return sha256Hex(digest.digest())
    }

    private fun pruneAutomaticBackups(directory: File, retentionCount: Int) {
        val candidates = directory.listFiles().orEmpty().filter { file ->
            isAutomaticBackupName(file.name) &&
                file.canonicalFile.parentFile == directory &&
                Files.isRegularFile(file.toPath()) &&
                !Files.isSymbolicLink(file.toPath())
        }
        automaticBackupFilesToDelete(candidates.map { it.name }, retentionCount).forEach { name ->
            val target = File(directory, name)
            checkBackupChild(directory, target)
            if (target.exists() && !target.delete()) throw IOException("Impossibile applicare la conservazione dei backup")
        }
    }

    private fun checkBackupChild(directory: File, file: File) {
        if (file.canonicalFile.parentFile != directory.canonicalFile) throw IOException("Percorso backup non valido")
    }

    private data class SectionInfo(val sha256: String, val bytes: Long)

    private data class PlaylistBackup(
        val id: String,
        val name: String,
        val coverUrl: String,
        val createdAt: Long,
        val updatedAt: Long,
        val tracks: List<Track>
    )

    private data class VaultSnapshot(
        val settings: LevyraPreferencesSnapshot,
        val favorites: List<Track>,
        val followedArtists: List<FollowedArtist>,
        val playlists: List<PlaylistBackup>,
        val history: List<ListenEventEntity>,
        val queueItems: List<PlaybackQueueItemEntity>,
        val queueState: PlaybackQueueStateEntity?
    )

    private class ByteCounterOutputStream(private val delegate: OutputStream) : OutputStream() {
        var count: Long = 0L
            private set

        override fun write(value: Int) {
            delegate.write(value)
            count++
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            delegate.write(buffer, offset, length)
            count += length
        }

        override fun flush() = delegate.flush()

        override fun close() = delegate.close()
    }

    internal companion object {
        const val FORMAT_VERSION = 1
        const val PLATFORM = "android"
        const val LEGACY_SCHEMA_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.json"
        const val LEGACY_PAYLOAD_ENTRY = "payload.json"
        const val SETTINGS_ENTRY = "data/settings.json"
        const val FAVORITES_ENTRY = "data/favorites.json"
        const val FOLLOWED_ARTISTS_ENTRY = "data/followed_artists.json"
        const val PLAYLISTS_ENTRY = "data/playlists.json"
        const val HISTORY_ENTRY = "data/history.json"
        const val QUEUE_ENTRY = "data/queue.json"
        const val MAX_ZIP_ENTRIES = 16
        const val MAX_ENTRY_BYTES = 64L * 1024L * 1024L
        const val MAX_TOTAL_BYTES = 192L * 1024L * 1024L
        const val MAX_AUTOMATIC_BACKUPS = 12
        const val AUTOMATIC_BACKUP_DIRECTORY = "backups"
        const val AUTOMATIC_BACKUP_PREFIX = "levyra-auto-backup-"
        const val VAULT_EXTENSION = ".levyra"
        val ALLOWED_VAULT_ENTRIES = setOf(
            MANIFEST_ENTRY,
            SETTINGS_ENTRY,
            FAVORITES_ENTRY,
            FOLLOWED_ARTISTS_ENTRY,
            PLAYLISTS_ENTRY,
            HISTORY_ENTRY,
            QUEUE_ENTRY,
            LEGACY_PAYLOAD_ENTRY
        )
    }
}

data class LevyraBackupResult(val message: String, val checksum: String)

data class VaultPreview(
    val createdAt: Long,
    val appVersion: String,
    val databaseVersion: Int,
    val formatVersion: Int,
    val platform: String,
    val sizeBytes: Long,
    val sections: List<String>,
    val compatible: Boolean
)

internal fun vaultEntryAllowed(name: String): Boolean {
    if (name.isEmpty() || name.startsWith("/") || name.contains("..") || name.contains('\\')) return false
    return name in LevyraBackupManager.ALLOWED_VAULT_ENTRIES
}

internal fun isAutomaticBackupName(name: String): Boolean {
    if (!name.startsWith("levyra-auto-backup-")) return false
    val body = name.removePrefix("levyra-auto-backup-")
    if (body.endsWith(".levyra")) {
        return body.removeSuffix(".levyra").all(Char::isDigit)
    }
    if (body.endsWith(".zip")) {
        return body.removeSuffix(".zip").all(Char::isDigit)
    }
    return false
}

internal fun levyraVaultFileName(timestamp: Long): String {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd_HHmm", java.util.Locale.US).format(java.util.Date(timestamp))
    return "Levyra_${date}.levyra"
}

internal fun automaticBackupFilesToDelete(fileNames: List<String>, retentionCount: Int): List<String> = fileNames
    .filter(::isAutomaticBackupName)
    .sortedDescending()
    .drop(retentionCount.coerceIn(1, 12))

internal fun backupAudioSettingsToJson(value: LevyraAudioSettings): JSONObject = JSONObject()
    .put("equalizerEnabled", value.equalizerEnabled)
    .put("presetId", value.presetId)
    .put("bandLevels", JSONArray(value.bandLevels))
    .put("bassBoost", value.bassBoost)
    .put("virtualizer", value.virtualizer)
    .put("preampDb", value.preampDb.toDouble())
    .put("limiterEnabled", value.limiterEnabled)
    .put("crossfadeSeconds", value.crossfadeSeconds)
    .put("djSoftMode", value.djSoftMode)
    .put("replayGainEnabled", value.replayGainEnabled)
    .put("playbackSpeed", value.playbackSpeed.toDouble())
    .put("pitch", value.pitch.toDouble())
    .put("gaplessEnabled", value.gaplessEnabled)
    .put("customPresets", JSONArray().apply { value.customPresets.forEach { put(customPresetToJson(it)) } })

internal fun backupAudioSettingsFromJson(json: JSONObject?): LevyraAudioSettings {
    if (json == null) return LevyraAudioSettings()
    val levelsArray = json.optJSONArray("bandLevels") ?: JSONArray()
    val levels = buildList { for (index in 0 until levelsArray.length()) add(levelsArray.optInt(index)) }
    val customPresetsArray = json.optJSONArray("customPresets")
    val customPresets = if (customPresetsArray == null) {
        emptyList()
    } else {
        buildList {
            for (index in 0 until customPresetsArray.length()) {
                customPresetsArray.optJSONObject(index)?.let(::customPresetFromJson)?.let(::add)
            }
        }
    }
    return LevyraAudioSettings(
        equalizerEnabled = json.optBoolean("equalizerEnabled"),
        presetId = json.optString("presetId"),
        bandLevels = levels,
        bassBoost = json.optInt("bassBoost"),
        virtualizer = json.optInt("virtualizer"),
        preampDb = json.optDouble("preampDb", 0.0).toFloat(),
        limiterEnabled = json.optBoolean("limiterEnabled", true),
        crossfadeSeconds = json.optInt("crossfadeSeconds"),
        djSoftMode = json.optBoolean("djSoftMode"),
        replayGainEnabled = json.optBoolean("replayGainEnabled"),
        playbackSpeed = json.optDouble("playbackSpeed", 1.0).toFloat(),
        pitch = json.optDouble("pitch", 1.0).toFloat(),
        gaplessEnabled = json.optBoolean("gaplessEnabled", true),
        customPresets = customPresets
    ).normalized()
}
