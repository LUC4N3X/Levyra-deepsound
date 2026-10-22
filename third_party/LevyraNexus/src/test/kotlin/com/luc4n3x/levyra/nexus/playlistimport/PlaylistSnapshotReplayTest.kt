package com.luc4n3x.levyra.nexus.playlistimport

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test

class PlaylistSnapshotReplayTest {
    @Test
    fun replayCapturedCandidatesWithoutNetwork() {
        val directory = System.getenv("LEVYRA_PLAYLIST_REPLAY_DIRECTORY")
        assumeNotNull(directory)
        val root = File(requireNotNull(directory))
        val identities = PlaylistTextParsers.parse(PlaylistTextFormat.CSV, File(root, "playlist-snapshot.csv").readText()).tracks
        val rows = BoundedJson.parse(File(root, "baseline.json").readText()).jsonArray().orEmpty()
            .map { requireNotNull(it.jsonObject()) }
        assertEquals(identities.indices.toSet(), rows.map { requireNotNull(it.long("position")).toInt() }.toSet())
        assertEquals(identities.size, rows.size)
        val report = mutableListOf("position,before,after,selectedBefore,selectedAfter,plainCsvConfidence,score,reasons")
        rows.forEach { row ->
            val position = requireNotNull(row.long("position")).toInt()
            val identity = identities[position]
            assertEquals(identity.title, row.string("title"))
            val candidates = row["candidates"].jsonArray().orEmpty().map { raw ->
                val item = requireNotNull(raw.jsonObject())
                MatchCandidate(
                    id = item.string("id"), title = item.string("title"),
                    artists = item["artists"].jsonArray().orEmpty().map { it as String },
                    album = item.string("album"), durationMs = item.long("durationMs") ?: 0L,
                    kind = CandidateKind.valueOf(item.string("kind"))
                )
            }
            val outcome = PlaylistMatchEngine.select(identity, candidates)
            val plain = PlaylistMatchEngine.select(identity.copy(durationMs = 0L), candidates)
            val before = row.string("confidence")
            if (before in setOf("EXACT", "EXCELLENT", "GOOD")) {
                assertTrue("Previously accepted position $position: $before -> ${outcome.confidence}", outcome.confidence.autoAccepted)
                assertEquals("Previously selected position $position", row.string("selected"), outcome.selected?.candidate?.id)
            }
            report += listOf(position, before, if (before == "ERROR") "ERROR" else outcome.confidence.name, row.string("selected"),
                outcome.selected?.candidate?.id.orEmpty(), if (before == "ERROR") "ERROR" else plain.confidence.name, outcome.selected?.score ?: 0,
                outcome.selected?.reasons?.joinToString("+") { it.signal.name }.orEmpty()).joinToString(",")
        }
        File(root, "replay-after.csv").writeText(report.joinToString("\n"))
        val standard = "Track name,Artist name,Album,Playlist name,Type,ISRC\n" + identities.joinToString("\n") { identity ->
            listOf(identity.title, identity.artistLine, identity.album, "Snapshot validation", "Playlist", identity.isrc)
                .joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        }
        val plainParsed = PlaylistTextParsers.parse(PlaylistTextFormat.CSV, standard)
        assertEquals(identities.map { it.title }, plainParsed.tracks.map { it.title })
        assertEquals(identities.map { it.artists }, plainParsed.tracks.map { it.artists })
        assertEquals(identities.map { it.album }, plainParsed.tracks.map { it.album })
        assertTrue(plainParsed.tracks.all { it.durationMs == 0L })
        File(root, "tunemymusic-generated.csv").writeText(standard)
    }
}
