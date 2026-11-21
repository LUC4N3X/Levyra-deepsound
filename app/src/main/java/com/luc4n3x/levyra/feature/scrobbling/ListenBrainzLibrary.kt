package com.luc4n3x.levyra.feature.scrobbling

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

enum class ListenBrainzPlaylistKind { TopWeek, TopMonth, CreatedFor, Own }

data class ListenBrainzTrack(val title: String, val artist: String, val recordingMbid: String = "")

data class ListenBrainzPlaylist(
    val id: String,
    val title: String,
    val kind: ListenBrainzPlaylistKind,
    val tracks: List<ListenBrainzTrack> = emptyList()
)

data class ListenBrainzExportResult(val matched: Int, val total: Int, val url: String)

class ListenBrainzLibrary(
    private val token: () -> String?,
    private val client: () -> OkHttpClient = LevyraHttpClientFactory::externalIntegrations
) {
    @Volatile private var cachedUser: Pair<String, String>? = null

    fun isConnected(): Boolean = !token().isNullOrBlank()

    suspend fun playlists(): List<ListenBrainzPlaylist> = withContext(Dispatchers.IO) {
        val user = userName() ?: return@withContext emptyList()
        coroutineScope {
            val week = async { top(user, "this_week", ListenBrainzPlaylistKind.TopWeek) }
            val month = async { top(user, "this_month", ListenBrainzPlaylistKind.TopMonth) }
            val createdFor = async { list("user/$user/playlists/createdfor", ListenBrainzPlaylistKind.CreatedFor) }
            val own = async { list("user/$user/playlists", ListenBrainzPlaylistKind.Own) }
            listOfNotNull(week.await(), month.await()) + createdFor.await() + own.await()
        }
    }

    suspend fun tracks(playlist: ListenBrainzPlaylist): List<ListenBrainzTrack> = withContext(Dispatchers.IO) {
        if (playlist.tracks.isNotEmpty()) return@withContext playlist.tracks
        get("playlist/${playlist.id}")?.let(ListenBrainzJson::playlistTracks).orEmpty()
    }

    suspend fun export(
        title: String,
        tracks: List<ListenBrainzTrack>,
        onSaved: (url: String, saved: Int) -> Unit = { _, _ -> }
    ): ListenBrainzExportResult = withContext(Dispatchers.IO) {
        val gate = Semaphore(LOOKUP_PARALLELISM)
        val mbids = coroutineScope {
            tracks.map { track ->
                async {
                    gate.withPermit {
                        track.recordingMbid.ifBlank {
                            val url = "$API/metadata/lookup/".toHttpUrl().newBuilder()
                                .addQueryParameter("artist_name", track.artist)
                                .addQueryParameter("recording_name", track.title)
                                .build().toString()
                            get(url, absolute = true)?.let(ListenBrainzJson::lookupMbid).orEmpty()
                        }
                    }
                }
            }.awaitAll()
        }.filter(String::isNotBlank).distinct()
        if (mbids.isEmpty()) throw ListenBrainzException()
        val batches = mbids.chunked(EXPORT_BATCH)
        val body = ListenBrainzJson.createPayload(title, batches.first()).toString().toRequestBody(JSON)
        val created = send(Request.Builder().url("$API/playlist/create").post(body))
        val playlistMbid = created?.let { JSONObject(it).optString("playlist_mbid") }.orEmpty()
        if (playlistMbid.isBlank()) throw ListenBrainzException()
        val url = "https://listenbrainz.org/playlist/$playlistMbid"
        var saved = batches.first().size
        onSaved(url, saved)
        batches.drop(1).forEach { batch ->
            val add = ListenBrainzJson.trackPayload(batch).toString().toRequestBody(JSON)
            send(Request.Builder().url("$API/playlist/$playlistMbid/item/add").post(add)) ?: throw ListenBrainzException()
            saved += batch.size
            onSaved(url, saved)
        }
        ListenBrainzExportResult(matched = mbids.size, total = tracks.size, url = url)
    }

    private fun top(user: String, range: String, kind: ListenBrainzPlaylistKind): ListenBrainzPlaylist? {
        val tracks = get("stats/user/$user/recordings?range=$range&count=$TOP_COUNT")?.let(ListenBrainzJson::topRecordings).orEmpty()
        return if (tracks.isEmpty()) null else ListenBrainzPlaylist(id = "${kind.name}:$range", title = "", kind = kind, tracks = tracks)
    }

    private fun list(path: String, kind: ListenBrainzPlaylistKind): List<ListenBrainzPlaylist> =
        get("$path?count=$LIST_COUNT")?.let { ListenBrainzJson.playlists(it, kind) }.orEmpty()

    private fun userName(): String? {
        val current = token()?.takeIf(String::isNotBlank) ?: return null
        cachedUser?.takeIf { it.first == current }?.let { return it.second }
        val name = get("validate-token")?.let(ListenBrainzJson::userName)?.takeIf(String::isNotBlank) ?: return null
        cachedUser = current to name
        return name
    }

    private fun get(path: String, absolute: Boolean = false): String? =
        send(Request.Builder().url(if (absolute) path else "$API/$path").get())

    private fun send(builder: Request.Builder): String? {
        val current = token()?.takeIf(String::isNotBlank) ?: return null
        return try {
            client().newCall(builder.header("Authorization", "Token $current").build()).execute().use { response ->
                if (response.isSuccessful && response.code != HTTP_NO_CONTENT) response.body.jsonPayload() else null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: java.io.IOException) {
            null
        }
    }

    private companion object {
        const val API = "https://api.listenbrainz.org/1"
        const val TOP_COUNT = 50
        const val LIST_COUNT = 25
        const val EXPORT_BATCH = 100
        const val LOOKUP_PARALLELISM = 4
        const val HTTP_NO_CONTENT = 204
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

class ListenBrainzException : Exception()

internal object ListenBrainzJson {
    private const val RECORDING_PREFIX = "https://musicbrainz.org/recording/"
    private const val PLAYLIST_PREFIX = "https://listenbrainz.org/playlist/"

    fun userName(json: String): String = JSONObject(json).optString("user_name")

    fun playlists(json: String, kind: ListenBrainzPlaylistKind): List<ListenBrainzPlaylist> {
        val items = JSONObject(json).optJSONArray("playlists") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val playlist = items.optJSONObject(index)?.optJSONObject("playlist") ?: return@mapNotNull null
            val id = playlist.optString("identifier").substringAfter(PLAYLIST_PREFIX, "").trim('/')
            val title = playlist.optString("title").trim()
            if (id.isBlank() || title.isBlank()) null else ListenBrainzPlaylist(id, title, kind)
        }
    }

    fun playlistTracks(json: String): List<ListenBrainzTrack> {
        val items = JSONObject(json).optJSONObject("playlist")?.optJSONArray("track") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val track = items.optJSONObject(index) ?: return@mapNotNull null
            val identifier = when (val raw = track.opt("identifier")) {
                is JSONArray -> raw.optString(0)
                else -> raw?.toString().orEmpty()
            }
            track(track.optString("title"), track.optString("creator"), identifier.substringAfter(RECORDING_PREFIX, ""))
        }
    }

    fun topRecordings(json: String): List<ListenBrainzTrack> {
        val items = JSONObject(json).optJSONObject("payload")?.optJSONArray("recordings") ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            track(item.optString("track_name"), item.optString("artist_name"), item.optString("recording_mbid"))
        }
    }

    fun lookupMbid(json: String): String = JSONObject(json).optString("recording_mbid").trim()

    fun createPayload(title: String, mbids: List<String>): JSONObject = JSONObject().put(
        "playlist",
        JSONObject()
            .put("title", title)
            .put("track", trackArray(mbids))
            .put("extension", JSONObject().put("https://musicbrainz.org/doc/jspf#playlist", JSONObject().put("public", false)))
    )

    fun trackPayload(mbids: List<String>): JSONObject = JSONObject().put("playlist", JSONObject().put("track", trackArray(mbids)))

    private fun trackArray(mbids: List<String>): JSONArray =
        JSONArray().apply { mbids.forEach { put(JSONObject().put("identifier", RECORDING_PREFIX + it)) } }

    fun xspf(title: String, tracks: List<ListenBrainzTrack>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n")
        append("<title>").append(escape(title)).append("</title>\n<trackList>\n")
        tracks.forEach { track ->
            append("<track><title>").append(escape(track.title)).append("</title><creator>")
                .append(escape(track.artist)).append("</creator></track>\n")
        }
        append("</trackList>\n</playlist>\n")
    }

    private fun track(title: String, artist: String, mbid: String): ListenBrainzTrack? {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        return if (cleanTitle.isBlank() || cleanArtist.isBlank()) null else ListenBrainzTrack(cleanTitle, cleanArtist, mbid.trim())
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        .filter { it == '\t' || it == '\n' || it == '\r' || it >= ' ' }
}
