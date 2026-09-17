package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.PlaylistStudioEdits
import com.luc4n3x.levyra.domain.Track
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistStudioSaveRecoveryTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val gateway = RecoveryGateway()
    private val controller = PlaylistStudioController(scope, gateway, Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `failed cover after creation rolls the new playlist back before retry`() {
        gateway.coverFailures = 1
        controller.open(PlaylistStudioEdits.startNew("Gym", listOf(track("a")))) { emptyList() }
        controller.setCoverStyle(PlaylistCoverStyle.Signal)

        controller.save()

        assertEquals(PlaylistStudioSaveState.Failed, session().saveState)
        assertNull(session().draft.playlistId)
        assertTrue(gateway.records.isEmpty())

        controller.retry()

        assertEquals(PlaylistStudioSaveState.Saved, session().saveState)
        assertEquals("new-2", session().draft.playlistId)
        assertEquals(setOf("new-2"), gateway.records.keys)
    }

    @Test
    fun `failed cover after editing restores the previous playlist contents`() {
        val originalTracks = listOf(track("a"), track("b"), track("c"))
        gateway.records["p1"] = Record("Mix", originalTracks)
        gateway.coverFailures = 1
        controller.open(
            PlaylistStudioDraft(playlistId = "p1", name = "Mix", tracks = originalTracks)
        ) { emptyList() }
        controller.rename("Renamed")
        controller.remove("a")
        controller.setCoverStyle(PlaylistCoverStyle.Signal)

        controller.save()

        assertEquals(PlaylistStudioSaveState.Failed, session().saveState)
        assertEquals("Mix", gateway.records.getValue("p1").name)
        assertEquals(listOf("a", "b", "c"), gateway.records.getValue("p1").tracks.map { it.id })
    }

    private fun session(): PlaylistStudioSession = requireNotNull(controller.session.value)

    private data class Record(val name: String, val tracks: List<Track>)

    private data class RecoveryToken(
        val playlistId: String,
        val record: Record
    ) : PlaylistStudioRollbackToken

    private class RecoveryGateway : PlaylistStudioGateway {
        val records = linkedMapOf<String, Record>()
        var coverFailures = 0
        private var created = 0

        override suspend fun create(name: String, tracks: List<Track>): String {
            created += 1
            val id = "new-$created"
            records[id] = Record(name, tracks)
            return id
        }

        override suspend fun captureRollback(playlistId: String): PlaylistStudioRollbackToken? =
            records[playlistId]?.let { RecoveryToken(playlistId, it) }

        override suspend fun update(playlistId: String, name: String, tracks: List<Track>) {
            if (playlistId !in records) throw PlaylistStudioMissingException(playlistId)
            records[playlistId] = Record(name, tracks)
        }

        override suspend fun applyCover(playlistId: String, draft: PlaylistStudioDraft): String? {
            if (coverFailures > 0) {
                coverFailures -= 1
                throw IOException("disk full")
            }
            return if (draft.coverStyle == PlaylistCoverStyle.Automatic) "" else "file:///cover-$playlistId.jpg"
        }

        override suspend fun rollbackCreated(playlistId: String): Boolean = records.remove(playlistId) != null

        override suspend fun rollbackUpdated(
            playlistId: String,
            token: PlaylistStudioRollbackToken?
        ): Boolean {
            val rollback = token as? RecoveryToken ?: return false
            if (rollback.playlistId != playlistId) return false
            records[playlistId] = rollback.record
            return true
        }

        override fun onSaved(playlistId: String) = Unit
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "",
        durationMs = 1_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://img/$id",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
