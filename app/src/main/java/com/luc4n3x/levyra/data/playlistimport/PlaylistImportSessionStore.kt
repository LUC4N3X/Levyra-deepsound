package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportChoiceOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportEntry
import com.luc4n3x.levyra.nexus.playlistimport.ImportFlag
import com.luc4n3x.levyra.nexus.playlistimport.ImportedPlaylistDescriptor
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.IncompleteReason
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.MatchConfidence
import com.luc4n3x.levyra.nexus.playlistimport.MatchEvaluation
import com.luc4n3x.levyra.nexus.playlistimport.MatchReason
import com.luc4n3x.levyra.nexus.playlistimport.MatchSignal
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportCompleteness
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportSource
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

enum class PlaylistImportPhase { READ, MATCHING, REVIEW, COMMITTED }

data class PlaylistImportSession(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val inputHash: String,
    val inputLabel: String,
    val descriptor: ImportedPlaylistDescriptor,
    val completeness: PlaylistImportCompleteness,
    val phase: PlaylistImportPhase,
    val preference: ResolutionPreference,
    val playlistName: String,
    val entries: List<ImportEntry>,
    val tracks: Map<String, Track>,
    val committedPlaylistId: String? = null
)

data class PlaylistImportSessionSummary(
    val id: String,
    val title: String,
    val source: PlaylistImportSource,
    val phase: PlaylistImportPhase,
    val total: Int,
    val resolved: Int,
    val updatedAt: Long,
    val inputHash: String
)

fun playlistImportInputHash(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(text.trim().toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

class PlaylistImportSessionStore(private val directory: File) {
    private val mutex = Mutex()

    suspend fun save(session: PlaylistImportSession) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!directory.exists() && !directory.mkdirs()) throw IOException("Cannot create import session directory")
            val target = file(session.id)
            val temp = File(directory, "${session.id}.tmp")
            temp.writeText(PlaylistImportSessionCodec.encode(session).toString(), Charsets.UTF_8)
            if (!temp.renameTo(target)) {
                target.delete()
                if (!temp.renameTo(target)) throw IOException("Cannot finalize import session")
            }
        }
    }

    suspend fun load(id: String): PlaylistImportSession? = withContext(Dispatchers.IO) {
        mutex.withLock { readFile(file(id)) }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock { file(id).delete() }
    }

    suspend fun resumable(nowMs: Long = System.currentTimeMillis()): List<PlaylistImportSessionSummary> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val files = directory.listFiles { candidate -> candidate.isFile && candidate.name.endsWith(".json") }.orEmpty()
            val sessions = files.mapNotNull { stored ->
                val session = readFile(stored)
                val expired = session == null || nowMs - session.updatedAt > MAX_AGE_MS ||
                    session.phase == PlaylistImportPhase.COMMITTED && nowMs - session.updatedAt > COMMITTED_RETENTION_MS
                if (expired) {
                    stored.delete()
                    null
                } else {
                    session
                }
            }.sortedByDescending { it.updatedAt }
            val open = sessions.filter { it.phase != PlaylistImportPhase.COMMITTED }
            open.drop(MAX_SESSIONS).forEach { file(it.id).delete() }
            open.take(MAX_SESSIONS)
                .map { session ->
                    PlaylistImportSessionSummary(
                        id = session.id,
                        title = session.playlistName.ifBlank { session.descriptor.title },
                        source = session.descriptor.source,
                        phase = session.phase,
                        total = session.entries.size,
                        resolved = session.entries.count { it.resolved },
                        updatedAt = session.updatedAt,
                        inputHash = session.inputHash
                    )
                }
        }
    }

    private fun readFile(stored: File): PlaylistImportSession? {
        if (!stored.isFile || stored.length() > MAX_SESSION_BYTES) return null
        return try {
            PlaylistImportSessionCodec.decode(JSONObject(stored.readText(Charsets.UTF_8)))
        } catch (error: JSONException) {
            Timber.w(error, "Discarding unreadable playlist import session")
            stored.delete()
            null
        } catch (error: IllegalArgumentException) {
            Timber.w(error, "Discarding incompatible playlist import session")
            stored.delete()
            null
        }
    }

    private fun file(id: String): File {
        require(SESSION_ID.matches(id)) { "Invalid session id" }
        return File(directory, "$id.json")
    }

    companion object {
        private const val MAX_SESSIONS = 3
        private const val MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000
        private const val COMMITTED_RETENTION_MS = 24L * 60 * 60 * 1000
        private const val MAX_SESSION_BYTES = 48L * 1024 * 1024
        private val SESSION_ID = Regex("^[A-Za-z0-9-]{8,64}$")
    }
}

internal object PlaylistImportSessionCodec {
    private const val VERSION = 1

    fun encode(session: PlaylistImportSession): JSONObject = JSONObject()
        .put("v", VERSION)
        .put("id", session.id)
        .put("createdAt", session.createdAt)
        .put("updatedAt", session.updatedAt)
        .put("inputHash", session.inputHash)
        .put("inputLabel", session.inputLabel)
        .put("phase", session.phase.name)
        .put("preference", session.preference.name)
        .put("playlistName", session.playlistName)
        .put("committedPlaylistId", session.committedPlaylistId ?: JSONObject.NULL)
        .put("descriptor", encodeDescriptor(session.descriptor))
        .put("completeness", encodeCompleteness(session.completeness))
        .put("entries", JSONArray().apply { session.entries.forEach { put(encodeEntry(it)) } })
        .put("tracks", JSONObject().apply { session.tracks.forEach { (id, track) -> put(id, encodeTrack(track)) } })

    fun decode(json: JSONObject): PlaylistImportSession {
        require(json.optInt("v") == VERSION) { "Unsupported session version" }
        val tracksJson = json.getJSONObject("tracks")
        val tracks = tracksJson.keys().asSequence().associateWith { decodeTrack(tracksJson.getJSONObject(it)) }
        val entriesJson = json.getJSONArray("entries")
        return PlaylistImportSession(
            id = json.getString("id"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            inputHash = json.getString("inputHash"),
            inputLabel = json.optString("inputLabel"),
            descriptor = decodeDescriptor(json.getJSONObject("descriptor")),
            completeness = decodeCompleteness(json.getJSONObject("completeness")),
            phase = PlaylistImportPhase.valueOf(json.getString("phase")),
            preference = ResolutionPreference.valueOf(json.getString("preference")),
            playlistName = json.optString("playlistName"),
            entries = List(entriesJson.length()) { decodeEntry(entriesJson.getJSONObject(it)) },
            tracks = tracks,
            committedPlaylistId = json.optString("committedPlaylistId").takeIf { !json.isNull("committedPlaylistId") && it.isNotBlank() }
        )
    }

    private fun encodeDescriptor(value: ImportedPlaylistDescriptor) = JSONObject()
        .put("source", value.source.name)
        .put("sourceId", value.sourceId)
        .put("title", value.title)
        .put("owner", value.owner)
        .put("description", value.description.take(2_000))
        .put("artworkUrl", value.artworkUrl)
        .put("declared", value.declaredTrackCount ?: -1)

    private fun decodeDescriptor(json: JSONObject) = ImportedPlaylistDescriptor(
        source = PlaylistImportSource.valueOf(json.getString("source")),
        sourceId = json.optString("sourceId"),
        title = json.optString("title"),
        owner = json.optString("owner"),
        description = json.optString("description"),
        artworkUrl = json.optString("artworkUrl"),
        declaredTrackCount = json.optInt("declared", -1).takeIf { it >= 0 }
    )

    private fun encodeCompleteness(value: PlaylistImportCompleteness): JSONObject = when (value) {
        PlaylistImportCompleteness.Complete -> JSONObject().put("type", "complete")
        PlaylistImportCompleteness.Unknown -> JSONObject().put("type", "unknown")
        is PlaylistImportCompleteness.Incomplete -> JSONObject()
            .put("type", "incomplete")
            .put("retrieved", value.retrieved)
            .put("declared", value.declared ?: -1)
            .put("reason", value.reason.name)
    }

    private fun decodeCompleteness(json: JSONObject): PlaylistImportCompleteness = when (json.optString("type")) {
        "complete" -> PlaylistImportCompleteness.Complete
        "incomplete" -> PlaylistImportCompleteness.Incomplete(
            json.optInt("retrieved"),
            json.optInt("declared", -1).takeIf { it >= 0 },
            IncompleteReason.valueOf(json.getString("reason"))
        )
        else -> PlaylistImportCompleteness.Unknown
    }

    private fun encodeIdentity(value: ImportedTrackIdentity) = JSONObject()
        .put("p", value.position)
        .put("t", value.title)
        .put("a", JSONArray(value.artists))
        .put("f", JSONArray(value.featuredArtists))
        .put("al", value.album)
        .put("d", value.durationMs)
        .put("isrc", value.isrc)
        .put("sid", value.sourceTrackId)
        .put("y", value.releaseYear)
        .put("e", value.explicit ?: JSONObject.NULL)
        .put("tn", value.trackNumber)
        .put("dn", value.discNumber)
        .put("art", value.artworkUrl)
        .put("url", value.originalUrl)
        .put("dir", value.directCatalogId)
        .put("loc", value.localReference)

    private fun decodeIdentity(json: JSONObject) = ImportedTrackIdentity(
        position = json.getInt("p"),
        title = json.getString("t"),
        artists = json.getJSONArray("a").strings(),
        featuredArtists = json.optJSONArray("f")?.strings().orEmpty(),
        album = json.optString("al"),
        durationMs = json.optLong("d"),
        isrc = json.optString("isrc"),
        sourceTrackId = json.optString("sid"),
        releaseYear = json.optInt("y"),
        explicit = if (json.isNull("e")) null else json.optBoolean("e"),
        trackNumber = json.optInt("tn"),
        discNumber = json.optInt("dn"),
        artworkUrl = json.optString("art"),
        originalUrl = json.optString("url"),
        directCatalogId = json.optString("dir"),
        localReference = json.optString("loc")
    )

    private fun encodeCandidate(value: MatchCandidate) = JSONObject()
        .put("id", value.id)
        .put("t", value.title)
        .put("a", JSONArray(value.artists))
        .put("al", value.album)
        .put("d", value.durationMs)
        .put("isrc", value.isrc)
        .put("e", value.explicit ?: JSONObject.NULL)
        .put("o", value.origin.name)
        .put("k", value.kind.name)
        .put("art", value.artworkUrl)
        .put("av", value.available)

    private fun decodeCandidate(json: JSONObject) = MatchCandidate(
        id = json.getString("id"),
        title = json.getString("t"),
        artists = json.getJSONArray("a").strings(),
        album = json.optString("al"),
        durationMs = json.optLong("d"),
        isrc = json.optString("isrc"),
        explicit = if (json.isNull("e")) null else json.optBoolean("e"),
        origin = CandidateOrigin.valueOf(json.getString("o")),
        kind = CandidateKind.valueOf(json.getString("k")),
        artworkUrl = json.optString("art"),
        available = json.optBoolean("av", true)
    )

    private fun encodeEvaluation(value: MatchEvaluation) = JSONObject()
        .put("c", encodeCandidate(value.candidate))
        .put("s", value.score)
        .put("r", JSONArray().apply {
            value.reasons.forEach { reason ->
                put(JSONObject().put("g", reason.signal.name).put("d", reason.deltaMs).put("t", reason.detail))
            }
        })

    private fun decodeEvaluation(json: JSONObject): MatchEvaluation {
        val reasons = json.getJSONArray("r")
        return MatchEvaluation(
            candidate = decodeCandidate(json.getJSONObject("c")),
            score = json.getInt("s"),
            reasons = List(reasons.length()) { index ->
                val reason = reasons.getJSONObject(index)
                MatchReason(MatchSignal.valueOf(reason.getString("g")), reason.optLong("d"), reason.optString("t"))
            }
        )
    }

    private fun encodeEntry(value: ImportEntry) = JSONObject()
        .put("i", encodeIdentity(value.identity))
        .put("res", value.resolved)
        .put("alt", JSONArray().apply { value.alternatives.forEach { put(encodeEvaluation(it)) } })
        .put("sel", value.selected?.let(::encodeEvaluation) ?: JSONObject.NULL)
        .put("conf", value.confidence.name)
        .put("auto", value.automaticId ?: JSONObject.NULL)
        .put("autoConf", value.automaticConfidence.name)
        .put("origin", value.origin.name)
        .put("skip", value.skipped)
        .put("flags", JSONArray(value.flags.map { it.name }))
        .put("dup", value.duplicateGroup)

    private fun decodeEntry(json: JSONObject): ImportEntry {
        val alternatives = json.getJSONArray("alt")
        return ImportEntry(
            identity = decodeIdentity(json.getJSONObject("i")),
            resolved = json.optBoolean("res"),
            alternatives = List(alternatives.length()) { decodeEvaluation(alternatives.getJSONObject(it)) },
            selected = if (json.isNull("sel")) null else decodeEvaluation(json.getJSONObject("sel")),
            confidence = MatchConfidence.valueOf(json.getString("conf")),
            automaticId = if (json.isNull("auto")) null else json.getString("auto"),
            automaticConfidence = MatchConfidence.valueOf(json.getString("autoConf")),
            origin = ImportChoiceOrigin.valueOf(json.getString("origin")),
            skipped = json.optBoolean("skip"),
            flags = json.getJSONArray("flags").strings().map(ImportFlag::valueOf).toSet(),
            duplicateGroup = json.optInt("dup", -1)
        )
    }

    private fun encodeTrack(track: Track) = JSONObject()
        .put("id", track.id)
        .put("title", track.title)
        .put("artist", track.artist)
        .put("album", track.album)
        .put("durationMs", track.durationMs)
        .put("streamUrl", if (track.streamUrl.startsWith("content://")) track.streamUrl else "")
        .put("videoUrl", track.videoUrl)
        .put("thumbnailUrl", track.thumbnailUrl)
        .put("largeThumbnailUrl", track.largeThumbnailUrl)
        .put("source", track.source)
        .put("isrc", track.isrc)
        .put("year", track.year)
        .put("explicit", track.explicit)
        .put("albumBrowseId", track.albumBrowseId)
        .put("artistBrowseIds", JSONArray(track.artistBrowseIds))
        .put("videoType", track.videoType)
        .put("trackNumber", track.trackNumber)
        .put("discNumber", track.discNumber)

    private fun decodeTrack(json: JSONObject) = Track(
        id = json.getString("id"),
        title = json.getString("title"),
        artist = json.optString("artist"),
        album = json.optString("album"),
        durationMs = json.optLong("durationMs"),
        streamUrl = json.optString("streamUrl"),
        videoUrl = json.optString("videoUrl"),
        thumbnailUrl = json.optString("thumbnailUrl"),
        largeThumbnailUrl = json.optString("largeThumbnailUrl"),
        source = json.optString("source"),
        moodTags = setOf("music", "imported"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        isrc = json.optString("isrc"),
        year = json.optString("year"),
        explicit = json.optBoolean("explicit"),
        albumBrowseId = json.optString("albumBrowseId"),
        artistBrowseIds = json.optJSONArray("artistBrowseIds")?.strings().orEmpty(),
        videoType = json.optString("videoType"),
        trackNumber = json.optInt("trackNumber"),
        discNumber = json.optInt("discNumber")
    )

    private fun JSONArray.strings(): List<String> = List(length()) { optString(it) }.filter(String::isNotEmpty)
}