package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.luc4n3x.levyra.BuildConfig
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
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LevyraBackupManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val database = LevyraDatabase.get(appContext)
    private val preferences = LevyraPreferences(appContext)
    private val followedArtistsStore = FollowedArtistsStore(appContext)

    suspend fun exportTo(uri: Uri): LevyraBackupResult = withContext(Dispatchers.IO) {
        val payload = createPayload().toString().toByteArray(Charsets.UTF_8)
        val checksum = sha256(payload)
        val manifest = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("createdAt", System.currentTimeMillis())
            .put("payloadSha256", checksum)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val output = appContext.contentResolver.openOutputStream(uri, "w") ?: throw IOException("Impossibile aprire il file di backup")
        output.use { stream ->
            ZipOutputStream(stream.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifest)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(PAYLOAD_ENTRY))
                zip.write(payload)
                zip.closeEntry()
            }
        }
        LevyraBackupResult("Backup completato", checksum)
    }

    suspend fun restoreFrom(uri: Uri): LevyraBackupResult = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri) ?: throw IOException("Impossibile leggere il backup")
        val entries = mutableMapOf<String, ByteArray>()
        input.use { stream ->
            ZipInputStream(stream.buffered()).use { zip ->
                var totalBytes = 0L
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name in setOf(MANIFEST_ENTRY, PAYLOAD_ENTRY)) {
                        if (entry.size > MAX_ENTRY_BYTES) throw IOException("Backup troppo grande")
                        val bytes = readZipEntry(zip, MAX_ENTRY_BYTES)
                        totalBytes += bytes.size
                        if (totalBytes > MAX_TOTAL_BYTES) throw IOException("Backup troppo grande")
                        entries[entry.name] = bytes
                    }
                    zip.closeEntry()
                }
            }
        }
        val manifestBytes = entries[MANIFEST_ENTRY] ?: throw IOException("Manifest del backup mancante")
        val payloadBytes = entries[PAYLOAD_ENTRY] ?: throw IOException("Dati del backup mancanti")
        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val schema = manifest.optInt("schemaVersion", 0)
        if (schema !in 1..SCHEMA_VERSION) throw IOException("Versione backup non supportata: $schema")
        val expected = manifest.optString("payloadSha256")
        val actual = sha256(payloadBytes)
        if (expected.isBlank() || !expected.equals(actual, true)) throw IOException("Backup danneggiato: checksum non valido")
        restorePayload(JSONObject(payloadBytes.toString(Charsets.UTF_8)))
        LevyraBackupResult("Ripristino completato", actual)
    }

    private suspend fun createPayload(): JSONObject {
        val snapshot = preferences.snapshot()
        val favorites = database.favoriteTracksDao().all().map { it.toTrack() }
        val playlists = database.playlistDao().allPlaylists()
        val history = database.listenEventsDao().all()
        val queueItems = database.playbackQueueDao().items()
        val queueState = database.playbackQueueDao().state()
        val root = JSONObject()
        root.put("settings", settingsToJson(snapshot))
        root.put("favorites", JSONArray().apply { favorites.forEach { put(TrackJson.toJson(it)) } })
        root.put("followedArtists", followedArtistsToJson(followedArtistsStore.load()))
        root.put("playlists", JSONArray().apply {
            playlists.forEach { playlist ->
                val tracks = database.playlistDao().tracksOf(playlist.id)
                put(
                    JSONObject()
                        .put("id", playlist.id)
                        .put("name", playlist.name)
                        .put("coverUrl", playlist.coverUrl)
                        .put("createdAt", playlist.createdAt)
                        .put("updatedAt", playlist.updatedAt)
                        .put("tracks", JSONArray().apply { tracks.forEach { put(TrackJson.toJson(it.toTrack())) } })
                )
            }
        })
        root.put("history", JSONArray().apply { history.forEach { put(listenEventToJson(it)) } })
        root.put("queue", JSONObject()
            .put("items", JSONArray().apply {
                queueItems.forEach { put(JSONObject().put("position", it.position).put("payload", it.payload).put("identity", it.identity)) }
            })
            .put("state", queueState?.let(::queueStateToJson) ?: JSONObject.NULL)
        )
        return root
    }

    private suspend fun restorePayload(root: JSONObject) {
        val settings = root.optJSONObject("settings") ?: JSONObject()
        val favoriteTracks = root.optJSONArray("favorites").toTrackList()
        val followedArtists = parseFollowedArtists(root.optJSONArray("followedArtists"))
        val playlists = root.optJSONArray("playlists") ?: JSONArray()
        val history = parseHistory(root.optJSONArray("history"))
        val queue = root.optJSONObject("queue")
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.favoriteTracksDao().replaceAll(favoriteTracks.mapIndexed { index, track -> track.toFavoriteTrackEntity(now - index) })
            restorePlaylists(playlists)
            database.listenEventsDao().replaceAll(history)
            restoreQueue(queue)
        }
        restoreSettings(settings)
        followedArtistsStore.save(followedArtists)
    }

    private fun settingsToJson(snapshot: LevyraPreferencesSnapshot): JSONObject {
        return JSONObject()
            .put("onboarded", snapshot.onboarded)
            .put("tastes", JSONArray(snapshot.tastes.toList()))
            .put("userName", snapshot.userName)
            .put("languageCode", snapshot.languageCode)
            .put("animationsEnabled", snapshot.animationsEnabled)
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
            .put("recentSearches", JSONArray().apply { snapshot.recentSearches.forEach { put(TrackJson.toJson(it)) } })
            .put("personalOrbitTracks", JSONArray().apply { snapshot.personalOrbitTracks.forEach { put(TrackJson.toJson(it)) } })
            .put("lastTrack", snapshot.lastTrack?.let(TrackJson::toJson) ?: JSONObject.NULL)
            .put("lastPositionMs", snapshot.lastPositionMs)
    }

    private fun restoreSettings(json: JSONObject) {
        val tastes = json.optJSONArray("tastes").toStringSet()
        preferences.setOnboardingState(json.optBoolean("onboarded", false), tastes)
        preferences.setUserName(json.optString("userName"))
        preferences.setLanguageCode(json.optString("languageCode"))
        preferences.setAnimationsEnabled(json.optBoolean("animationsEnabled", true))
        preferences.setDynamicColor(json.optBoolean("dynamicColor", true))
        preferences.setSponsorBlock(json.optBoolean("sponsorBlock", true))
        preferences.setSkipSilence(json.optBoolean("skipSilence", false))
        preferences.setAudioQuality(json.optString("audioQuality", "Auto"))
        preferences.setAudioNormalization(json.optBoolean("audioNormalization", false))
        preferences.setLyricsTranslationEnabled(json.optBoolean("lyricsTranslationEnabled", false))
        preferences.setThemePreset(json.optString("themePreset"))
        preferences.setAudioSettings(parseAudioSettings(json.optJSONObject("audioSettings")))
        preferences.setInterfaceSettings(parseInterfaceSettings(json.optJSONObject("interfaceSettings")))
        preferences.setDownloadSettings(parseDownloadSettings(json.optJSONObject("downloadSettings")))
        preferences.saveRecentSearches(json.optJSONArray("recentSearches").toTrackList())
        preferences.savePersonalOrbitTracks(json.optJSONArray("personalOrbitTracks").toTrackList(), json.optString("languageCode"))
        val lastTrack = json.optJSONObject("lastTrack")?.let(TrackJson::fromJson)
        preferences.saveLastPlayback(lastTrack, json.optLong("lastPositionMs").coerceAtLeast(0L))
    }

    private suspend fun restorePlaylists(array: JSONArray) {
        val dao = database.playlistDao()
        dao.clearAll()
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val id = json.optString("id").trim()
            if (id.isBlank()) continue
            val createdAt = json.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = json.optLong("updatedAt", createdAt)
            dao.upsertPlaylist(PlaylistEntity(id, json.optString("name", "Playlist"), json.optString("coverUrl"), createdAt, updatedAt))
            val tracks = json.optJSONArray("tracks").toTrackList()
            if (tracks.isNotEmpty()) dao.insertTracks(tracks.mapIndexed { position, track -> track.toPlaylistTrackEntity(id, position, createdAt + position) })
        }
    }

    private suspend fun restoreQueue(json: JSONObject?) {
        val dao = database.playbackQueueDao()
        if (json == null) {
            dao.clear()
            return
        }
        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (index in 0 until itemsArray.length()) {
                val item = itemsArray.optJSONObject(index) ?: continue
                add(PlaybackQueueItemEntity(item.optInt("position"), item.optString("payload"), item.optString("identity")))
            }
        }
        val stateJson = json.optJSONObject("state")
        if (stateJson == null) {
            dao.clear()
        } else {
            dao.replace(items, parseQueueState(stateJson))
        }
    }

    private fun audioSettingsToJson(value: LevyraAudioSettings): JSONObject = JSONObject()
        .put("equalizerEnabled", value.equalizerEnabled)
        .put("presetId", value.presetId)
        .put("bandLevels", JSONArray(value.bandLevels))
        .put("bassBoost", value.bassBoost)
        .put("virtualizer", value.virtualizer)
        .put("crossfadeSeconds", value.crossfadeSeconds)
        .put("djSoftMode", value.djSoftMode)
        .put("replayGainEnabled", value.replayGainEnabled)
        .put("playbackSpeed", value.playbackSpeed.toDouble())
        .put("pitch", value.pitch.toDouble())
        .put("gaplessEnabled", value.gaplessEnabled)

    private fun parseAudioSettings(json: JSONObject?): LevyraAudioSettings {
        if (json == null) return LevyraAudioSettings()
        val levelsArray = json.optJSONArray("bandLevels") ?: JSONArray()
        val levels = buildList { for (index in 0 until levelsArray.length()) add(levelsArray.optInt(index)) }
        return LevyraAudioSettings(
            equalizerEnabled = json.optBoolean("equalizerEnabled"),
            presetId = json.optString("presetId"),
            bandLevels = levels,
            bassBoost = json.optInt("bassBoost"),
            virtualizer = json.optInt("virtualizer"),
            crossfadeSeconds = json.optInt("crossfadeSeconds"),
            djSoftMode = json.optBoolean("djSoftMode"),
            replayGainEnabled = json.optBoolean("replayGainEnabled"),
            playbackSpeed = json.optDouble("playbackSpeed", 1.0).toFloat(),
            pitch = json.optDouble("pitch", 1.0).toFloat(),
            gaplessEnabled = json.optBoolean("gaplessEnabled", true)
        ).normalized()
    }

    private fun interfaceSettingsToJson(value: LevyraInterfaceSettings): JSONObject = JSONObject()
        .put("compactHome", value.compactHome)
        .put("showPersonalOrbit", value.showPersonalOrbit)
        .put("showResonance", value.showResonance)
        .put("showNewReleases", value.showNewReleases)
        .put("showAlbumsForYou", value.showAlbumsForYou)
        .put("showTrendingArtists", value.showTrendingArtists)
        .put("showCharts", value.showCharts)
        .put("playerGesturesEnabled", value.playerGesturesEnabled)
        .put("doubleTapSeekSeconds", value.doubleTapSeekSeconds)
        .put("longPressSpeed", value.longPressSpeed.toDouble())

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
            playerGesturesEnabled = json.optBoolean("playerGesturesEnabled", true),
            doubleTapSeekSeconds = json.optInt("doubleTapSeekSeconds", 10),
            longPressSpeed = json.optDouble("longPressSpeed", 2.0).toFloat()
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

    private fun followedArtistsToJson(values: List<FollowedArtist>): JSONArray = JSONArray().apply {
        values.forEach { put(JSONObject().put("browseId", it.browseId).put("name", it.name).put("thumbnailUrl", it.thumbnailUrl).put("followedAt", it.followedAt)) }
    }

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
                add(ListenEventEntity(trackId = trackId, title = json.optString("title"), artist = json.optString("artist"), album = json.optString("album"), durationMs = json.optLong("durationMs"), videoUrl = json.optString("videoUrl"), thumbnailUrl = json.optString("thumbnailUrl"), largeThumbnailUrl = json.optString("largeThumbnailUrl"), source = json.optString("source"), listenedMs = json.optLong("listenedMs"), completed = json.optBoolean("completed"), startedAt = json.optLong("startedAt")))
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

    private fun parseQueueState(json: JSONObject): PlaybackQueueStateEntity = PlaybackQueueStateEntity(
        currentIndex = json.optInt("currentIndex", -1),
        positionMs = json.optLong("positionMs"),
        shuffleEnabled = json.optBoolean("shuffleEnabled"),
        shuffleOrder = json.optString("shuffleOrder"),
        shuffleCursor = json.optInt("shuffleCursor", -1),
        history = json.optString("history"),
        repeatMode = json.optString("repeatMode", "Off"),
        radioEnabled = json.optBoolean("radioEnabled", true),
        generation = json.optLong("generation"),
        updatedAt = json.optLong("updatedAt")
    )

    private fun JSONArray?.toTrackList(): List<com.luc4n3x.levyra.domain.Track> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) optJSONObject(index)?.let(TrackJson::fromJson)?.let(::add)
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

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.json"
        const val PAYLOAD_ENTRY = "payload.json"
        const val MAX_ENTRY_BYTES = 64L * 1024L * 1024L
        const val MAX_TOTAL_BYTES = 65L * 1024L * 1024L
    }
}

data class LevyraBackupResult(val message: String, val checksum: String)
