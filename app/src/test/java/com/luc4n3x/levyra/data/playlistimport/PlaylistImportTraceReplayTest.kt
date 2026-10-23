package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextFormat
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextParsers
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeNotNull
import org.junit.Test

class PlaylistImportTraceReplayTest {
    private class UnrecordedQueryException(key: String) : IOException(key)

    private class TraceStore(directory: File) {
        val responses = HashMap<String, JSONObject>()
        val metadata = HashMap<Int, JSONArray>()
        val positions = HashSet<Int>()

        init {
            directory.listFiles { file -> file.name.endsWith(".jsonl") }.orEmpty().sortedBy { it.name }.forEach { file ->
                file.forEachLine { raw ->
                    if (raw.isBlank()) return@forEachLine
                    val line = JSONObject(raw)
                    positions += line.getInt("position")
                    if (line.has("error")) return@forEachLine
                    val kind = line.getString("kind")
                    if (kind == "metadata") {
                        metadata[line.getInt("position")] = line.getJSONArray("items")
                    } else {
                        val key = "$kind|${line.getString("query").lowercase()}"
                        val previous = responses[key]
                        if (previous == null || previous.optBoolean("failed") || !line.optBoolean("failed")) responses[key] = line
                    }
                }
            }
        }
    }

    private class ReplayRepository(private val store: TraceStore, private val misses: MutableSet<String>) :
        YoutubeMusicRepository(null) {
        val queries = mutableListOf<String>()

        private fun lookup(kind: String, query: String): JSONObject {
            val key = "$kind|${query.lowercase()}"
            queries += key
            return store.responses[key] ?: run {
                misses += key
                throw UnrecordedQueryException(key)
            }
        }

        override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> {
            val line = lookup("songs", query)
            return SearchPage(tracks(line.getJSONArray("items")), "", failed = line.optBoolean("failed"))
        }

        override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> =
            tracks(lookup("mixed", query).getJSONArray("items"))

        override suspend fun searchYoutubeForImport(query: String, limit: Int): List<Track> =
            tracks(lookup("youtube", query).getJSONArray("items"))

        val albumSource = object : ImportAlbumSource {
            override suspend fun albums(query: String, languageCode: String): List<AlbumHit> = replayAlbums(query)

            override suspend fun tracks(album: AlbumHit, languageCode: String): List<Track> =
                tracks(lookup("album", album.browseId.ifBlank { "${album.title}|${album.artist}" }).getJSONArray("items"))
        }

        private fun replayAlbums(query: String): List<AlbumHit> {
            val items = lookup("albums", query).getJSONArray("items")
            return (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                AlbumHit(item.getString("title"), item.optString("artist"), item.optString("year"), "", "",
                    browseId = item.optString("browseId"))
            }
        }
    }

    @Test
    fun replayRecordedPlaylistTrace() = runBlocking {
        val directory = System.getenv("LEVYRA_PLAYLIST_TRACE_DIRECTORY")
        assumeNotNull(directory)
        val root = File(requireNotNull(directory))
        val identities = PlaylistTextParsers.parse(PlaylistTextFormat.CSV, File(root, "playlist.csv").readText()).tracks
        val store = TraceStore(root)
        val rows = JSONArray()
        val totals = sortedMapOf<String, Int>()
        val incomplete = mutableListOf<Int>()
        identities.filter { it.position in store.positions }.forEach { identity ->
            val misses = linkedSetOf<String>()
            val repository = ReplayRepository(store, misses)
            val catalog = PlaylistImportCatalog(repository, { emptyList() }, { "en" },
                metadataSearch = { metadataCandidates(store, it) },
                youtubeSearch = { query -> repository.searchYoutubeForImport(query, 12) }, albumSource = repository.albumSource)
            val row = JSONObject().put("position", identity.position).put("title", identity.title)
                .put("artists", JSONArray(identity.artists)).put("album", identity.album).put("durationMs", identity.durationMs)
            val confidence = try {
                val outcome = catalog.resolve(identity, false, ResolutionPreference.SMART).outcome
                row.put("selected", outcome.selected?.candidate?.id).put("ambiguous", outcome.ambiguous)
                    .put("candidates", JSONArray().also { list ->
                        outcome.alternatives.forEach { match ->
                            list.put(JSONObject().put("id", match.candidate.id).put("title", match.candidate.title)
                                .put("artists", JSONArray(match.candidate.artists)).put("album", match.candidate.album)
                                .put("durationMs", match.candidate.durationMs).put("kind", match.candidate.kind.name)
                                .put("score", match.score).put("reasons", match.reasons.joinToString(" ") { reason ->
                                    reason.signal.name + reason.detail.takeIf(String::isNotBlank)?.let { "($it)" }.orEmpty()
                                }))
                        }
                    })
                outcome.confidence.name
            } catch (_: IOException) {
                "ERROR"
            }
            if (misses.isNotEmpty()) incomplete += identity.position
            row.put("confidence", confidence).put("queries", JSONArray(repository.queries)).put("unrecorded", JSONArray(misses))
            rows.put(row)
            totals[confidence] = (totals[confidence] ?: 0) + 1
        }
        val summary = JSONObject().put("tracks", rows.length()).put("totals", JSONObject(totals as Map<*, *>))
            .put("positionsWithUnrecordedQueries", JSONArray(incomplete))
        File(root, System.getenv("LEVYRA_PLAYLIST_TRACE_REPORT") ?: "replay-report.json")
            .writeText(JSONObject().put("summary", summary).put("rows", rows).toString(1))
        println("PLAYLIST_TRACE_REPLAY $summary")
    }

    private fun metadataCandidates(store: TraceStore, identity: ImportedTrackIdentity): List<MatchCandidate> {
        val items = store.metadata[identity.position] ?: throw IOException("metadata|${identity.position}")
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            MatchCandidate(
                id = item.getString("id"), title = item.getString("title"),
                artists = item.getJSONArray("artists").let { array -> (0 until array.length()).map(array::getString) },
                album = item.optString("album"), durationMs = item.optLong("durationMs"), isrc = item.optString("isrc"),
                explicit = if (item.optBoolean("explicit")) true else null, kind = CandidateKind.valueOf(item.getString("kind"))
            )
        }
    }

    private companion object {
        fun tracks(items: JSONArray): List<Track> = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val id = item.getString("id")
            Track(
                id = id, title = item.getString("title"), artist = item.optString("artist"), album = item.optString("album"),
                durationMs = item.optLong("durationMs"), streamUrl = "", videoUrl = "https://www.youtube.com/watch?v=$id",
                thumbnailUrl = "", largeThumbnailUrl = "", source = item.optString("source"), moodTags = emptySet(),
                energy = 50, vocal = 50, replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0,
                isrc = item.optString("isrc"), explicit = item.optBoolean("explicit"), videoType = item.optString("videoType"),
                albumBrowseId = item.optString("albumBrowseId")
            )
        }
    }
}
