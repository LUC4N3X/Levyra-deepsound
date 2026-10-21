package com.luc4n3x.levyra.data.playlistimport

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistImportLiveTest {
    @Test
    fun capturePublicCatalogSample() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("playlistLive") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        var calls = 0
        val repository = object : YoutubeMusicRepository(context) {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> {
                calls++
                return super.searchSongsPage(query, languageCode, continuation)
            }

            override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> {
                calls++
                return super.searchForImport(query, limit, languageCode)
            }
        }
        var metadataCalls = 0
        val metadata = PlaylistImportMetadata(
            com.luc4n3x.levyra.data.apple.AppleMetadataEnricher(context),
            { query -> com.luc4n3x.levyra.data.spotify.SpotifySearchClient().search(query).songs },
            { "US" }
        )
        val catalog = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, metadataSearch = { identity ->
            metadataCalls++
            metadata.search(identity)
        })
        val results = JSONArray()
        val sample = listOf(
            "Кино" to "Группа крови",
            "Кино" to "Звезда по имени Солнце",
            "ДДТ" to "Что такое осень",
            "Би-2" to "Полковнику никто не пишет",
            "Земфира" to "Искала",
            "Король и Шут" to "Кукла колдуна",
            "Сплин" to "Выхода нет",
            "МакSим" to "Знаешь ли ты",
            "Kino" to "Gruppa krovi",
            "Viktor Tsoi" to "Zvezda po imeni Solntse",
            "Måneskin" to "Zitti e buoni",
            "Radiohead" to "No Surprises",
            "Океан Ельзи" to "Обійми",
            "방탄소년단" to "봄날",
            "宇多田ヒカル" to "First Love",
            "周杰倫" to "晴天",
            "فيروز" to "نسم علينا الهوى",
            "Björk" to "Jóga",
            "Daft Punk" to "One More Time",
            "Nils Frahm" to "Says"
        )
        val apple = com.luc4n3x.levyra.data.apple.AppleMetadataEnricher(context)
        suspend fun appleIdentity(index: Int, artist: String, title: String): ImportedTrackIdentity? {
            val plain = ImportedTrackIdentity(index, title, listOf(artist))
            return apple.searchImportMetadata(title, artist, "US").firstOrNull { item ->
                PlaylistMatchEngine.evaluate(plain, com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate(
                    id = "apple:${item.songId}", title = item.name, artists = listOf(item.artistName),
                    album = item.albumName, durationMs = item.durationMs
                )).confidence.autoAccepted
            }?.let { item ->
                ImportedTrackIdentity(index, item.name, listOf(item.artistName), album = item.albumName,
                    durationMs = item.durationMs, isrc = item.isrc)
            }
        }
        val identities = sample.mapIndexed { index, (artist, title) -> "plain" to ImportedTrackIdentity(index, title, listOf(artist)) } +
            if (args.getString("playlistApple") == "true") {
                sample.mapIndexedNotNull { index, (artist, title) -> appleIdentity(index, artist, title)?.let { "apple" to it } }
            } else emptyList()
        identities.forEach { (kind, identity) ->
            val artist = identity.primaryArtist
            val title = identity.title
            val started = System.nanoTime()
            val before = calls
            val metadataBefore = metadataCalls
            val row = JSONObject().put("source", kind).put("artist", artist).put("title", title)
                .put("album", identity.album).put("durationMs", identity.durationMs).put("isrc", identity.isrc)
            try {
                val outcome = catalog.resolve(identity, false, com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference.SMART).outcome
                row.put("confidence", outcome.confidence.name)
                row.put("candidates", JSONArray().also { items ->
                    outcome.alternatives.forEach { match ->
                        items.put(JSONObject().put("id", match.candidate.id).put("title", match.candidate.title)
                            .put("artist", match.candidate.artistLine).put("durationMs", match.candidate.durationMs)
                            .put("kind", match.candidate.kind.name).put("score", match.score))
                    }
                })
            } catch (error: Exception) {
                row.put("error", error.javaClass.simpleName)
            }
            row.put("calls", calls - before).put("metadataLookups", metadataCalls - metadataBefore)
                .put("elapsedMs", (System.nanoTime() - started) / 1_000_000)
            results.put(row)
            Log.i("PlaylistImportLive", row.toString())
            File(context.filesDir, "playlist-live.json").writeText(results.toString(2))
        }
    }
}
