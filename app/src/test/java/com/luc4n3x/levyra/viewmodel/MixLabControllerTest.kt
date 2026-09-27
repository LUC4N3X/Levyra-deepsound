package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistExclusions
import com.luc4n3x.levyra.domain.MixLabCandidate
import com.luc4n3x.levyra.domain.MixLabParams
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixLabControllerTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val gateway = FakeMixLabGateway()
    private val controller = MixLabController(scope, gateway, Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
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

    private fun session() = requireNotNull(controller.session.value)

    @Test
    fun `generate produces a preview from the assembled pool`() {
        gateway.pool = listOf(MixLabCandidate(track("a")), MixLabCandidate(track("b")))
        controller.open(MixLabParams(trackCount = 2))
        controller.generate()
        assertEquals(MixLabStage.Preview, session().stage)
        assertEquals(2, session().result?.tracks?.size)
    }

    @Test
    fun `empty pool surfaces a human error instead of a blank success`() {
        gateway.pool = emptyList()
        controller.open(MixLabParams())
        controller.generate()
        assertEquals(MixLabStage.Error, session().stage)
    }

    @Test
    fun `generation failure surfaces as error and never crashes`() {
        gateway.failCandidatePool = true
        controller.open(MixLabParams())
        controller.generate()
        assertEquals(MixLabStage.Error, session().stage)
    }

    @Test
    fun `closing during generation cancels the in-flight job without crashing`() {
        val gate = CompletableDeferred<Unit>()
        gateway.pool = listOf(MixLabCandidate(track("a")))
        gateway.beforeCandidatePool = { gate.await() }
        controller.open(MixLabParams(trackCount = 1))
        controller.generate()
        assertEquals(MixLabStage.Generating, session().stage)

        controller.close()
        assertNull(controller.session.value)

        gate.complete(Unit)
        assertNull(controller.session.value)
    }

    @Test
    fun `regenerate bumps the seed and keeps the intent`() {
        gateway.pool = (1..30).map { MixLabCandidate(track("t$it")) }
        controller.open(MixLabParams(trackCount = 10, seed = 5L))
        controller.generate()
        val firstSeed = session().params.seed
        controller.regenerate()
        assertEquals(firstSeed + 1, session().params.seed)
        assertEquals(MixLabStage.Preview, session().stage)
    }

    @Test
    fun `save as playlist calls the gateway once and tags the result`() {
        gateway.pool = listOf(MixLabCandidate(track("a")), MixLabCandidate(track("b")))
        controller.open(MixLabParams(trackCount = 2))
        controller.generate()
        controller.saveAsPlaylist("My mix")
        assertEquals(listOf("save:My mix:a,b", "saved:new-1"), gateway.calls)
        assertEquals("new-1", session().savedPlaylistId)
        assertFalse(session().saving)
    }

    @Test
    fun `save failure is surfaced without crashing and can be retried`() {
        gateway.pool = listOf(MixLabCandidate(track("a")))
        gateway.failSave = true
        controller.open(MixLabParams(trackCount = 1))
        controller.generate()
        controller.saveAsPlaylist("My mix")
        assertTrue(session().saveFailed)
        assertNull(session().savedPlaylistId)
    }

    @Test
    fun `tune parameters returns to configure without losing chosen params`() {
        gateway.pool = listOf(MixLabCandidate(track("a")))
        controller.open(MixLabParams(trackCount = 1, familiarity = 0.8f))
        controller.generate()
        controller.tuneParameters()
        assertEquals(MixLabStage.Configure, session().stage)
        assertEquals(0.8f, session().params.familiarity)
        assertNotNull(session().params)
    }

    private class FakeMixLabGateway : MixLabGateway {
        var pool: List<MixLabCandidate> = emptyList()
        var failCandidatePool: Boolean = false
        var failSave: Boolean = false
        var beforeCandidatePool: (suspend () -> Unit)? = null
        val calls = mutableListOf<String>()
        private var nextId = 1

        override suspend fun candidatePool(): List<MixLabCandidate> {
            beforeCandidatePool?.invoke()
            if (failCandidatePool) throw IllegalStateException("boom")
            return pool
        }

        override suspend fun exclusions(): ArtistExclusions = ArtistExclusions.Empty

        override suspend fun canonicalSources(): List<Track> = emptyList()

        override suspend fun saveAsPlaylist(name: String, tracks: List<Track>): String {
            if (failSave) throw IllegalStateException("save boom")
            calls += "save:$name:${tracks.joinToString(",") { it.id }}"
            return "new-${nextId++}"
        }

        override fun onSaved(playlistId: String) {
            calls += "saved:$playlistId"
        }
    }
}
