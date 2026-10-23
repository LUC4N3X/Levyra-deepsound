package com.luc4n3x.levyra.data.playlistimport

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.apple.AppleMetadataEnricher
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistInputDetector
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextFormat
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextParsers
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistImportSnapshotLiveTest {
    @Test
    fun captureRequestedPlaylist() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("playlistLive") == "true")
        val url = args.getString("playlistUrl") ?: return@runBlocking
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requests = JSONArray()
        val client = LevyraHttpClientFactory.externalIntegrations().newBuilder().addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            requests.put(JSONObject().put("host", chain.request().url.host).put("status", response.code))
            response
        }.build()
        val source = SpotifyPlaylistAdapter(PlaylistImportFetcher(client)).read(
            PlaylistImportInput(url), requireNotNull(PlaylistInputDetector.detectUrl(url)), PlaylistReadProgress { _, _ -> }
        ).playlist
        val rows = source.tracks
        fun csv(value: String) = "\"${value.replace("\"", "\"\"") }\""
        val fixture = "Track name,Artist name,Album,Playlist name,Type,ISRC,Track duration (ms),Spotify id\n" +
            rows.joinToString("\n") { row ->
                listOf(row.title, row.artistLine, row.album, "Temporary catalog validation", "Playlist", row.isrc,
                    row.durationMs.toString(), row.sourceTrackId).joinToString(",", transform = ::csv)
            }
        File(context.filesDir, "${playlistName(args)}.csv").writeText(fixture)
        val parsed = PlaylistTextParsers.parse(PlaylistTextFormat.CSV, fixture)
        assertEquals(rows.map { it.title }, parsed.tracks.map { it.title })
        assertEquals(rows.map { it.artists }, parsed.tracks.map { it.artists })
        assertEquals(rows.map { it.durationMs }, parsed.tracks.map { it.durationMs })
        assertEquals(rows.map { it.sourceTrackId }, parsed.tracks.map { it.sourceTrackId })
        File(context.filesDir, "${playlistName(args)}-access.json").writeText(
            JSONObject().put("retrieved", rows.size).put("declared", source.descriptor.declaredTrackCount)
                .put("completeness", source.completeness.toString()).put("requests", requests)
                .put("csvRoundTrip", true).toString(2)
        )
    }

    @Test
    fun resolveSnapshotBatch(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("playlistLive") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "${playlistName(args)}.csv")
        assumeTrue(file.isFile)
        val identities = PlaylistTextParsers.parse(PlaylistTextFormat.CSV, file.readText()).tracks
        val start = args.getString("playlistStart")?.toInt() ?: 0
        val count = (args.getString("playlistCount")?.toInt() ?: 25).coerceIn(1, 100)
        val reportName = args.getString("playlistReport") ?: "requested-playlist-results"
        require(reportName.matches(Regex("[A-Za-z0-9-]+")))
        val output = File(context.filesDir, "$reportName.json")
        val results = if (output.isFile) JSONArray(output.readText()) else JSONArray()
        val done = (0 until results.length()).map { results.getJSONObject(it).getInt("position") }.toSet()
        val spotify = com.luc4n3x.levyra.data.spotify.SpotifySearchClient()
        val metadata = PlaylistImportMetadata(AppleMetadataEnricher(context),
            { query -> spotify.search(query).songs }, { "US" })
        val positions = args.getString("playlistPositions")?.split(',')?.map { it.toInt() }?.toSet()
        require(positions == null || positions.size <= 100 && positions.all { it in identities.indices })
        val batch = if (positions == null) identities.drop(start).take(count) else identities.filter { it.position in positions }
        val slots = Semaphore((args.getString("playlistConcurrency")?.toInt() ?: 1).coerceIn(1, 4))
        val resultLock = Mutex()
        val trace = PlaylistTraceRecorder(File(context.filesDir, "$reportName-trace.jsonl"))
        coroutineScope {
            batch.filter { it.position !in done }.map { identity ->
                async {
                    slots.withPermit {
                        var calls = 0
                        val queries = JSONArray()
                        val repository = object : YoutubeMusicRepository(context) {
                            override suspend fun searchYoutubeForImport(query: String, limit: Int): List<Track> {
                                calls++
                                queries.put(JSONObject().put("kind", "youtube").put("query", query))
                                return trace.tracks(identity.position, "youtube", query) { super.searchYoutubeForImport(query, limit) }
                            }

                            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> {
                                calls++
                                queries.put(JSONObject().put("kind", "songs").put("query", query))
                                return trace.page(identity.position, query) { super.searchSongsPage(query, languageCode, continuation) }
                            }

                            override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> {
                                calls++
                                queries.put(JSONObject().put("kind", "mixed").put("query", query))
                                return trace.tracks(identity.position, "mixed", query) { super.searchForImport(query, limit, languageCode) }
                            }
                        }
                        var metadataCalls = 0
                        val catalog = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, metadataSearch = { identity ->
                            metadataCalls++
                            trace.metadata(identity.position) { metadata.search(identity) }
                        }, youtubeSearch = { query -> repository.searchYoutubeForImport(query, 12) },
                            albumSource = object : ImportAlbumSource {
                                override suspend fun albums(query: String, languageCode: String) =
                                    trace.albums(identity.position, query) { repository.searchAlbumsForImport(query, languageCode) }

                                override suspend fun tracks(album: com.luc4n3x.levyra.domain.AlbumHit, languageCode: String) =
                                    trace.tracks(identity.position, "album", album.browseId.ifBlank { "${album.title}|${album.artist}" }) {
                                        repository.albumTracksForImport(album, languageCode)
                                    }
                            },
                            durationLookup = { videoId, languageCode ->
                                trace.duration(identity.position, videoId) { repository.videoDurationForImport(videoId, languageCode) }
                            })
                        val before = calls
                        val beforeMetadata = metadataCalls
                        val queryStart = queries.length()
                        val started = System.nanoTime()
                        val row = JSONObject().put("position", identity.position).put("title", identity.title)
                            .put("artists", JSONArray(identity.artists)).put("spotifyId", identity.sourceTrackId)
                            .put("durationMs", identity.durationMs)
                        try {
                            val resolution = catalog.resolve(identity, false, ResolutionPreference.SMART)
                            row.put("confidence", resolution.outcome.confidence.name)
                                .put("selected", resolution.outcome.selected?.candidate?.id)
                                .put("candidates", JSONArray().also { list ->
                                    resolution.outcome.alternatives.forEach { match ->
                                        list.put(JSONObject().put("id", match.candidate.id).put("title", match.candidate.title)
                                            .put("artists", JSONArray(match.candidate.artists)).put("album", match.candidate.album)
                                            .put("durationMs", match.candidate.durationMs).put("kind", match.candidate.kind.name)
                                            .put("score", match.score).put("reasons", match.reasons.toString()))
                                    }
                                })
                        } catch (error: java.io.IOException) {
                            row.put("confidence", "ERROR").put("error", error.javaClass.simpleName)
                        }
                        row.put("calls", calls - before).put("metadataLookups", metadataCalls - beforeMetadata)
                            .put("elapsedMs", (System.nanoTime() - started) / 1_000_000)
                            .put("queries", JSONArray().also { list ->
                                (queryStart until queries.length()).forEach { list.put(queries.get(it)) }
                            })
                        resultLock.withLock {
                            results.put(row)
                            output.writeCheckpoint(results.toString(2))
                        }
                        delay(750)
                    }
                }
            }.awaitAll()
        }
    }

    @Test
    fun inspectYoutubeCandidates(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("playlistLive") == "true")
        val positions = args.getString("playlistPositions")?.split(',')?.map(String::toInt) ?: return@runBlocking
        require(positions.size <= 10)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val identities = PlaylistTextParsers.parse(PlaylistTextFormat.CSV,
            File(context.filesDir, "${playlistName(args)}.csv").readText()).tracks
        val repository = YoutubeMusicRepository(context)
        val results = JSONArray()
        positions.forEach { position ->
            val identity = identities[position]
            val query = "${identity.title} ${identity.primaryArtist}"
            val row = JSONObject().put("position", position).put("query", query)
            try {
                val candidates = repository.searchYoutubeForImport(query, 16)
                row.put("candidates", JSONArray().also { list ->
                    candidates.forEach { track ->
                        val candidate = track.toMatchCandidate().candidate
                        val evaluation = PlaylistMatchEngine.evaluate(identity, candidate)
                        list.put(JSONObject().put("id", candidate.id).put("title", candidate.title)
                            .put("artists", JSONArray(candidate.artists)).put("durationMs", candidate.durationMs)
                            .put("score", evaluation.score).put("reasons", evaluation.reasons.toString()))
                    }
                })
            } catch (error: java.io.IOException) {
                row.put("error", error.javaClass.simpleName)
            }
            results.put(row)
            File(context.filesDir, "youtube-diagnostics.json").writeCheckpoint(results.toString(2))
            delay(1_000)
        }
    }

    @Test
    fun searchSnapshotVariants() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("playlistLive") == "true")
        val positions = args.getString("playlistPositions")?.split(',')?.map { it.toInt() } ?: return@runBlocking
        require(positions.size <= 25)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val identities = PlaylistTextParsers.parse(PlaylistTextFormat.CSV,
            File(context.filesDir, "${playlistName(args)}.csv").readText()).tracks
        val catalog = PlaylistImportCatalog(YoutubeMusicRepository(context), { emptyList() }, { "en" })
        val output = File(context.filesDir, "${playlistName(args)}-diagnostics.json")
        val results = if (output.isFile) JSONArray(output.readText()) else JSONArray()
        positions.forEach { position ->
            val identity = identities[position]
            val queries = listOf("${identity.title} - ${identity.artists.joinToString(" ")}",
                "${identity.primaryArtist} ${identity.title}").distinct()
            queries.forEach { query ->
                val done = (0 until results.length()).any {
                    results.getJSONObject(it).getInt("position") == position && results.getJSONObject(it).getString("query") == query
                }
                if (!done) {
                    val found = catalog.search(query, CandidateOrigin.ONLINE)
                    val outcome = PlaylistMatchEngine.select(identity, found.map { it.candidate })
                    results.put(JSONObject().put("position", position).put("query", query)
                        .put("confidence", outcome.confidence.name).put("candidates", JSONArray().also { list ->
                            found.forEach { item ->
                                val evaluation = PlaylistMatchEngine.evaluate(identity, item.candidate)
                                list.put(JSONObject().put("id", item.candidate.id).put("title", item.candidate.title)
                                    .put("artists", JSONArray(item.candidate.artists)).put("durationMs", item.candidate.durationMs)
                                    .put("score", evaluation.score).put("reasons", evaluation.reasons.toString()))
                            }
                        }))
                    output.writeCheckpoint(results.toString(2))
                    delay(1_000)
                }
            }
        }
    }

    private fun File.writeCheckpoint(value: String) {
        val temporary = File(parentFile, "$name.tmp")
        temporary.writeText(value)
        check(temporary.renameTo(this))
    }

    private fun playlistName(args: android.os.Bundle): String =
        (args.getString("playlistName") ?: "requested-playlist").also { require(it.matches(Regex("[A-Za-z0-9-]+"))) }
}

internal class PlaylistTraceRecorder(private val file: File) {
    suspend fun tracks(position: Int, kind: String, query: String, block: suspend () -> List<Track>): List<Track> =
        record(position, kind, query, { it.size }, { items -> JSONObject().put("items", trackArray(items)) }, block)

    suspend fun albums(
        position: Int,
        query: String,
        block: suspend () -> List<com.luc4n3x.levyra.domain.AlbumHit>
    ): List<com.luc4n3x.levyra.domain.AlbumHit> =
        record(position, "albums", query, { it.size }, { items ->
            JSONObject().put("items", JSONArray().also { list ->
                items.forEach { hit ->
                    list.put(JSONObject().put("title", hit.title).put("artist", hit.artist).put("year", hit.year)
                        .put("browseId", hit.browseId))
                }
            })
        }, block)

    suspend fun duration(position: Int, videoId: String, block: suspend () -> Long): Long =
        record(position, "duration", videoId, { 1 }, { JSONObject().put("durationMs", it) }, block)

    suspend fun page(position: Int, query: String, block: suspend () -> SearchPage<Track>): SearchPage<Track> =
        record(position, "songs", query, { it.items.size }, { page ->
            JSONObject().put("items", trackArray(page.items)).put("failed", page.failed)
        }, block)

    suspend fun metadata(
        position: Int,
        block: suspend () -> List<com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate>
    ): List<com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate> =
        record(position, "metadata", "", { it.size }, { items ->
            JSONObject().put("items", JSONArray().also { list ->
                items.forEach { item ->
                    list.put(JSONObject().put("id", item.id).put("title", item.title).put("artists", JSONArray(item.artists))
                        .put("album", item.album).put("durationMs", item.durationMs).put("isrc", item.isrc)
                        .put("explicit", item.explicit == true).put("kind", item.kind.name))
                }
            })
        }, block)

    private suspend fun <T> record(
        position: Int,
        kind: String,
        query: String,
        size: (T) -> Int,
        encode: (T) -> JSONObject,
        block: suspend () -> T
    ): T {
        val started = System.nanoTime()
        val line = JSONObject().put("position", position).put("kind", kind).put("query", query)
        return try {
            block().also { value ->
                encode(value).let { body -> body.keys().forEach { key -> line.put(key, body.get(key)) } }
                line.put("count", size(value))
            }
        } catch (error: Exception) {
            line.put("error", error.javaClass.simpleName)
            throw error
        } finally {
            line.put("elapsedMs", (System.nanoTime() - started) / 1_000_000)
            synchronized(this) { file.appendText(line.toString() + "\n") }
        }
    }

    private fun trackArray(items: List<Track>): JSONArray = JSONArray().also { list ->
        items.forEach { track ->
            list.put(JSONObject().put("id", track.id).put("title", track.title).put("artist", track.artist)
                .put("album", track.album).put("durationMs", track.durationMs).put("isrc", track.isrc)
                .put("explicit", track.explicit).put("videoType", track.videoType).put("source", track.source)
                .put("albumBrowseId", track.albumBrowseId))
        }
    }
}
