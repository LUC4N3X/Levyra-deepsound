package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.playlistimport.CatalogCandidate
import com.luc4n3x.levyra.data.playlistimport.PlaylistCandidateProvider
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportInput
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSessionStore
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSourceAdapter
import com.luc4n3x.levyra.data.playlistimport.PlaylistReadProgress
import com.luc4n3x.levyra.data.playlistimport.PlaylistSourceResult
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.DetectedPlaylistInput
import com.luc4n3x.levyra.nexus.playlistimport.ImportedPlaylistDescriptor
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.ParsedPlaylist
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportCompleteness
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportSource
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlaylistImportControllerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val executor = Executors.newSingleThreadExecutor()
    private val main = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)
    private val gateway = FakeGateway()
    private val store by lazy { PlaylistImportSessionStore(folder.newFolder("sessions")) }
    private val controller by lazy {
        PlaylistImportController(scope, listOf(FakeSpotifyAdapter()), FakeCatalog(), store, gateway, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        executor.shutdownNow()
    }

    @Test
    fun `reopening after a completed import shows a clean input and the next import is independent`() {
        act { open() }
        val reviewA = importToReview(PLAYLIST_A)
        act { confirm() }
        val summaryA = await { it.step == PlaylistImportStep.SUMMARY }
        val committedA = requireNotNull(summaryA.committedPlaylistId)
        act { fixMissing() }
        await { it.step == PlaylistImportStep.REVIEW }
        act { close() }
        val hidden = await { !it.visible }
        assertEquals(PlaylistImportStep.REVIEW, hidden.step)
        assertEquals(committedA, hidden.committedPlaylistId)

        act { open() }
        assertCleanInput(controller.state.value)

        val reviewB = importToReview(PLAYLIST_B)
        assertEquals(PLAYLIST_B_ID, reviewB.descriptor?.sourceId)
        assertNotEquals(reviewA.sessionId, reviewB.sessionId)
        assertNull(reviewB.committedPlaylistId)
        assertEquals(listOf("Song B1"), reviewB.entries.map { it.identity.title })

        act { confirm() }
        val summaryB = await { it.step == PlaylistImportStep.SUMMARY }
        val committedB = requireNotNull(summaryB.committedPlaylistId)
        assertNotEquals(committedA, committedB)
        assertFalse(requireNotNull(summaryB.summary).appended)
        assertEquals(listOf(committedA, committedB), gateway.commits.map { it.first })
        assertEquals(listOf(listOf("a1"), listOf("b1")), gateway.commits.map { it.second })
        assertTrue(gateway.appends.isEmpty())
    }

    @Test
    fun `summary reached while hidden during saving is cleared on reopen`() {
        val gate = CompletableDeferred<Unit>()
        gateway.commitGate = gate
        act { open() }
        importToReview(PLAYLIST_A)
        act { confirm() }
        await { it.step == PlaylistImportStep.SAVING }
        act { close() }
        await { !it.visible && it.busy }
        gate.complete(Unit)
        val hidden = await { it.step == PlaylistImportStep.SUMMARY }
        assertFalse(hidden.visible)

        act { open() }
        assertCleanInput(controller.state.value)
    }

    @Test
    fun `import another playlist resets the summary to a fresh input`() {
        act { open() }
        importToReview(PLAYLIST_A)
        act { confirm() }
        await { it.step == PlaylistImportStep.SUMMARY }

        act { importAnother() }
        assertCleanInput(controller.state.value)

        val reviewB = importToReview(PLAYLIST_B)
        assertEquals(PLAYLIST_B_ID, reviewB.descriptor?.sourceId)
        assertNull(reviewB.committedPlaylistId)
    }

    @Test
    fun `unfinished review survives close and reopen and stays resumable`() {
        act { open() }
        val review = importToReview(PLAYLIST_A)
        val sessionId = requireNotNull(review.sessionId)
        act { close() }
        await { !it.visible }

        act { open() }
        val reopened = controller.state.value
        assertTrue(reopened.visible)
        assertEquals(PlaylistImportStep.REVIEW, reopened.step)
        assertEquals(sessionId, reopened.sessionId)
        assertEquals(review.entries, reopened.entries)

        act { cancel() }
        val input = await { it.step == PlaylistImportStep.INPUT && it.resumable.any { session -> session.id == sessionId } }
        assertNull(input.sessionId)
        assertTrue(runBlocking { store.resumable() }.any { it.id == sessionId })

        act { resume(sessionId) }
        val resumed = await { it.step == PlaylistImportStep.REVIEW }
        assertEquals(sessionId, resumed.sessionId)
        assertEquals(PLAYLIST_A_ID, resumed.descriptor?.sourceId)
        assertTrue(gateway.commits.isEmpty())
    }

    private fun importToReview(url: String): PlaylistImportUiState {
        act {
            updateInput(url)
            start()
        }
        return await { it.step == PlaylistImportStep.REVIEW && it.descriptor?.sourceId == url.substringAfterLast('/') }
    }

    private fun assertCleanInput(state: PlaylistImportUiState) {
        assertTrue(state.visible)
        assertEquals(PlaylistImportStep.INPUT, state.step)
        assertEquals("", state.input)
        assertNull(state.descriptor)
        assertTrue(state.entries.isEmpty())
        assertNull(state.summary)
        assertNull(state.sessionId)
        assertNull(state.committedPlaylistId)
        assertNull(state.failure)
        assertEquals("", state.playlistName)
    }

    private fun act(block: PlaylistImportController.() -> Unit) = runBlocking {
        withContext(main) { controller.block() }
    }

    private fun await(predicate: (PlaylistImportUiState) -> Boolean): PlaylistImportUiState = runBlocking {
        withTimeout(TIMEOUT_MS) { controller.state.first(predicate) }
    }

    private class FakeSpotifyAdapter : PlaylistImportSourceAdapter {
        override val sources = setOf(PlaylistImportSource.SPOTIFY)

        override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
            val id = input.text.trim().substringAfterLast('/')
            val titles = if (id == PLAYLIST_A_ID) listOf("Song A1", "Missing A2") else listOf("Song B1")
            val tracks = titles.mapIndexed { index, title -> ImportedTrackIdentity(index, title, listOf("Artist"), durationMs = 200_000L) }
            val descriptor = ImportedPlaylistDescriptor(PlaylistImportSource.SPOTIFY, id, "Playlist $id", declaredTrackCount = tracks.size)
            return PlaylistSourceResult(ParsedPlaylist(descriptor, tracks, PlaylistImportCompleteness.Complete))
        }
    }

    private class FakeCatalog : PlaylistCandidateProvider {
        private val known = mapOf("Song A1" to "a1", "Song B1" to "b1")

        override suspend fun candidates(identity: ImportedTrackIdentity, broad: Boolean): List<CatalogCandidate> {
            val id = known[identity.title] ?: return emptyList()
            val candidate = MatchCandidate(id, identity.title, identity.artists, durationMs = identity.durationMs, kind = CandidateKind.SONG)
            return listOf(CatalogCandidate(candidate, track(id, identity.title)))
        }

        override suspend fun search(query: String, origin: CandidateOrigin): List<CatalogCandidate> = emptyList()
    }

    private class FakeGateway : PlaylistImportGateway {
        val commits = mutableListOf<Pair<String, List<String>>>()
        val appends = mutableListOf<Pair<String, List<String>>>()
        var commitGate: CompletableDeferred<Unit>? = null

        override fun languageCode(): String = "en"

        override suspend fun commit(name: String, tracks: List<Track>, playlistId: String): Playlist {
            commitGate?.await()
            commits += playlistId to tracks.map { it.id }
            return Playlist(playlistId, name, "", tracks, 0L, 0L)
        }

        override suspend fun append(playlistId: String, tracks: List<Track>) {
            appends += playlistId to tracks.map { it.id }
        }

        override fun playlistsChanged() = Unit

        override fun openPlaylist(playlistId: String) = Unit
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val PLAYLIST_A_ID = "37i9dQZF1DXcBWIGoYBM5M"
        const val PLAYLIST_B_ID = "37i9dQZF1DX0XUsuxWHRQd"
        const val PLAYLIST_A = "https://open.spotify.com/playlist/$PLAYLIST_A_ID"
        const val PLAYLIST_B = "https://open.spotify.com/playlist/$PLAYLIST_B_ID"

        fun track(id: String, title: String) = Track(
            id = id,
            title = title,
            artist = "Artist",
            album = "",
            durationMs = 200_000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "test",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 50,
            cacheScore = 50,
            accentStart = 0,
            accentEnd = 0
        )
    }
}
