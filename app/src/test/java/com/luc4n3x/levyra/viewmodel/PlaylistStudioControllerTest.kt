package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.PlaylistStudioEdits
import com.luc4n3x.levyra.domain.PlaylistStudioPhoto
import com.luc4n3x.levyra.domain.Track
import java.io.IOException
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

class PlaylistStudioControllerTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val gateway = FakeGateway()
    private val controller = PlaylistStudioController(scope, gateway, Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `creating a playlist persists name tracks and cover once`() {
        controller.open(PlaylistStudioEdits.startNew()) { listOf(listOf(track("a"), track("b"))) }
        val candidates = requireNotNull(session().candidates)
        assertEquals(2, candidates.tracks.size)
        assertFalse(session().canSave)

        controller.rename("Road trip")
        controller.toggle(candidates.tracks[0])
        controller.toggle(candidates.tracks[1])
        controller.setCoverStyle(PlaylistCoverStyle.Mosaic)
        assertEquals(PlaylistStudioSaveState.Dirty, session().saveState)
        controller.save()

        assertEquals(listOf("create:Road trip:a,b"), gateway.calls.take(1))
        assertEquals("cover:new-1:Mosaic", gateway.calls[1])
        assertEquals("refresh:new-1", gateway.calls[2])
        val saved = session()
        assertEquals("new-1", saved.draft.playlistId)
        assertEquals(PlaylistStudioSaveState.Saved, saved.saveState)
        assertFalse(saved.canSave)
        assertEquals("file:///rendered-new-1.jpg", saved.draft.customCoverUrl)
    }

    @Test
    fun `new playlist with automatic cover skips cover work`() {
        controller.open(PlaylistStudioEdits.startNew()) { emptyList() }
        controller.rename("Empty")
        controller.save()
        assertEquals(listOf("create:Empty:", "refresh:new-1"), gateway.calls)
        assertEquals(PlaylistStudioSaveState.Saved, session().saveState)
    }

    @Test
    fun `editing updates the existing playlist and keeps an untouched custom cover`() {
        val draft = existing().copy(coverStyle = PlaylistCoverStyle.Current, customCoverUrl = "file:///old.jpg")
        controller.open(draft) { emptyList() }
        assertEquals(PlaylistStudioSaveState.Clean, session().saveState)
        assertFalse(session().canSave)

        controller.move(0, 2)
        controller.save()

        assertEquals(listOf("update:p1:Mix:b,c,a", "refresh:p1"), gateway.calls)
        assertEquals("file:///old.jpg", session().draft.customCoverUrl)
    }

    @Test
    fun `switching back to automatic resets the stored cover`() {
        controller.open(existing().copy(coverStyle = PlaylistCoverStyle.Current, customCoverUrl = "file:///old.jpg")) { emptyList() }
        controller.setCoverStyle(PlaylistCoverStyle.Automatic)
        controller.save()
        assertEquals(listOf("update:p1:Mix:a,b,c", "cover:p1:Automatic", "refresh:p1"), gateway.calls)
        assertEquals("", session().draft.customCoverUrl)
    }

    @Test
    fun `photo covers are committed with their crop`() {
        controller.open(existing()) { emptyList() }
        controller.setPhoto(PlaylistStudioPhoto("content://photo", 700, 2f, 1f, 1f))
        controller.save()
        assertTrue("cover:p1:Photo" in gateway.calls)
    }

    @Test
    fun `cancel discards the draft without touching storage`() {
        controller.open(existing()) { emptyList() }
        controller.rename("Changed")
        controller.remove("a")
        controller.close()
        assertNull(controller.session.value)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `remove can be undone in order`() {
        controller.open(existing()) { emptyList() }
        controller.remove("a")
        controller.remove("c")
        assertEquals(listOf("b"), ids())
        assertEquals(PlaylistStudioUndoKind.Removed, session().lastUndo?.kind)
        assertEquals("Title c", session().lastUndo?.trackTitle)

        controller.undo()
        assertEquals(listOf("a", "b", "c").filter { it != "a" }, ids())
        controller.undo()
        assertEquals(listOf("a", "b", "c"), ids())
        assertEquals(PlaylistStudioSaveState.Clean, session().saveState)
        controller.undo()
        assertEquals(listOf("a", "b", "c"), ids())
    }

    @Test
    fun `a drag gesture records a single undo step`() {
        controller.open(existing()) { emptyList() }
        controller.beginMove("a")
        controller.move(0, 1)
        controller.move(1, 2)
        assertEquals(listOf("b", "c", "a"), ids())
        assertEquals(1, session().undo.size)
        assertEquals(PlaylistStudioUndoKind.Moved, session().lastUndo?.kind)
        controller.undo()
        assertEquals(listOf("a", "b", "c"), ids())
    }

    @Test
    fun `undo history stays bounded`() {
        val tracks = (0 until 60).map { track("t$it") }
        controller.open(PlaylistStudioEdits.startNew("Big", tracks)) { emptyList() }
        repeat(50) { controller.remove("t$it") }
        assertEquals(20, session().undo.size)
        dismissAndCheckEmpty()
    }

    @Test
    fun `failed cover after creation retries without creating a duplicate`() {
        gateway.coverFailures = 1
        controller.open(PlaylistStudioEdits.startNew("Gym", listOf(track("a")))) { emptyList() }
        controller.setCoverStyle(PlaylistCoverStyle.Signal)
        controller.save()

        val failed = session()
        assertEquals(PlaylistStudioSaveState.Failed, failed.saveState)
        assertEquals("new-1", failed.draft.playlistId)
        assertTrue(failed.canSave)
        assertTrue("refresh:new-1" in gateway.calls)

        gateway.calls.clear()
        controller.retry()
        assertEquals(listOf("update:new-1:Gym:a", "cover:new-1:Signal", "refresh:new-1"), gateway.calls)
        assertEquals(PlaylistStudioSaveState.Saved, session().saveState)
        assertEquals(1, gateway.created)
    }

    @Test
    fun `a playlist deleted elsewhere surfaces a save failure`() {
        gateway.missing = true
        controller.open(existing()) { emptyList() }
        controller.rename("Renamed")
        controller.save()
        assertEquals(PlaylistStudioSaveState.Failed, session().saveState)
        assertTrue(session().draft.name == "Renamed")
    }

    @Test
    fun `edits during a save stay dirty afterwards`() {
        val gate = CompletableDeferred<Unit>()
        gateway.updateGate = gate
        controller.open(existing()) { emptyList() }
        controller.rename("First")
        controller.save()
        assertEquals(PlaylistStudioSaveState.Saving, session().saveState)
        controller.save()
        controller.rename("Second")
        gate.complete(Unit)

        assertEquals(1, gateway.calls.count { it.startsWith("update:") })
        assertEquals("First", session().baseline.name)
        assertEquals("Second", session().draft.name)
        assertEquals(PlaylistStudioSaveState.Dirty, session().saveState)
    }

    @Test
    fun `late results from a closed session are ignored`() {
        val gate = CompletableDeferred<Unit>()
        gateway.updateGate = gate
        controller.open(existing()) { emptyList() }
        controller.rename("First")
        controller.save()
        controller.close()
        controller.open(PlaylistStudioEdits.startNew("Other")) { emptyList() }
        gate.complete(Unit)

        val current = session()
        assertNull(current.draft.playlistId)
        assertEquals("Other", current.draft.name)
        assertFalse(current.saving)
        assertTrue("refresh:p1" in gateway.calls)
    }

    @Test
    fun `cover commit decisions follow what actually changed`() {
        val base = existing()
        assertFalse(coverNeedsCommit(base, base))
        assertFalse(coverNeedsCommit(base.copy(name = "Other"), base))
        val mosaic = base.copy(
            coverStyle = PlaylistCoverStyle.Mosaic,
            tracks = listOf(track("a"), track("b"), track("c"), track("d"))
        )
        assertTrue(coverNeedsCommit(mosaic, base))
        assertFalse(coverNeedsCommit(mosaic, mosaic))
        assertFalse(coverNeedsCommit(mosaic.copy(tracks = mosaic.tracks + track("z")), mosaic))
        assertTrue(coverNeedsCommit(mosaic.copy(tracks = mosaic.tracks.reversed()), mosaic))
        assertTrue(coverNeedsCommit(mosaic.copy(tracks = mosaic.tracks.drop(1)), mosaic))
        val spotlight = base.copy(coverStyle = PlaylistCoverStyle.Spotlight)
        assertTrue(coverNeedsCommit(spotlight.copy(name = "Renamed"), spotlight))
        assertFalse(coverNeedsCommit(base.copy(coverStyle = PlaylistCoverStyle.Current), base))
        val fresh = PlaylistStudioEdits.startNew("x")
        assertFalse(coverNeedsCommit(fresh, fresh))
    }

    @Test
    fun `a save still running for a closed session does not block the next one`() {
        val gate = CompletableDeferred<Unit>()
        gateway.updateGate = gate
        controller.open(existing()) { emptyList() }
        controller.rename("First")
        controller.save()
        controller.close()
        gateway.updateGate = null
        controller.open(PlaylistStudioEdits.startNew("Next")) { emptyList() }
        controller.save()
        assertEquals(PlaylistStudioSaveState.Saved, session().saveState)
        gate.complete(Unit)
        assertTrue("update:p1:First:a,b,c" in gateway.calls)
    }

    @Test
    fun `operations without a session are ignored`() {
        controller.rename("x")
        controller.remove("a")
        controller.undo()
        controller.save()
        assertNull(controller.session.value)
        assertTrue(gateway.calls.isEmpty())
    }

    private fun dismissAndCheckEmpty() {
        controller.dismissUndo()
        assertTrue(session().undo.isEmpty())
    }

    private fun session(): PlaylistStudioSession = assertNotNullSession(controller.session.value)

    private fun assertNotNullSession(value: PlaylistStudioSession?): PlaylistStudioSession {
        assertNotNull(value)
        return requireNotNull(value)
    }

    private fun ids(): List<String> = session().draft.tracks.map { it.id }

    private fun existing(): PlaylistStudioDraft = PlaylistStudioDraft(
        playlistId = "p1",
        name = "Mix",
        tracks = listOf(track("a"), track("b"), track("c"))
    )

    private class FakeGateway : PlaylistStudioGateway {
        val calls = mutableListOf<String>()
        var created = 0
        var coverFailures = 0
        var missing = false
        var updateGate: CompletableDeferred<Unit>? = null

        override suspend fun create(name: String, tracks: List<Track>): String {
            created += 1
            calls += "create:$name:${tracks.joinToString(",") { it.id }}"
            return "new-$created"
        }

        override suspend fun update(playlistId: String, name: String, tracks: List<Track>) {
            updateGate?.await()
            calls += "update:$playlistId:$name:${tracks.joinToString(",") { it.id }}"
            if (missing) throw PlaylistStudioMissingException(playlistId)
        }

        override suspend fun applyCover(playlistId: String, draft: PlaylistStudioDraft): String? {
            calls += "cover:$playlistId:${draft.coverStyle}"
            if (coverFailures > 0) {
                coverFailures -= 1
                throw IOException("disk full")
            }
            return when (draft.coverStyle) {
                PlaylistCoverStyle.Automatic -> ""
                PlaylistCoverStyle.Current -> null
                else -> "file:///rendered-$playlistId.jpg"
            }
        }

        override fun onSaved(playlistId: String) {
            calls += "refresh:$playlistId"
        }
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
