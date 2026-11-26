package com.luc4n3x.levyra.desktop.core.podcast

import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import java.nio.file.Files
import java.nio.file.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PodcastLibraryStoreTest {

    private lateinit var directory: Path
    private var clock = 1_000L

    private val show = PodcastShow("MPSPabcdefgh12", "Daily Talk", "Studio", "https://img/s.jpg")

    private fun episode(id: String) = Track(id = id, title = "Episode $id", videoUrl = Track.watchUrlOf(id))

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("levyra-podcasts")
    }

    @After
    fun tearDown() {
        directory.toFile().deleteRecursively()
    }

    private fun newStore() = PodcastLibraryStore(
        JsonFileStore(directory.resolve("podcasts.json"), PodcastLibraryData.serializer(), { PodcastLibraryData() }),
        nowMillis = { clock++ }
    )

    @Test
    fun subscriptionToggleSurvivesReload() {
        newStore().toggleSubscription(show)
        val reloaded = newStore()
        assertEquals(listOf(show), reloaded.library.value.subscriptions.map { it.show })
        reloaded.toggleSubscription(show)
        assertTrue(newStore().library.value.subscriptions.isEmpty())
    }

    @Test
    fun progressIsNewestFirstDedupedAndBounded() {
        val store = newStore()
        repeat(PODCAST_HISTORY_LIMIT + 5) { store.saveProgress(episode("e$it"), 60_000L, 600_000L) }
        store.saveProgress(episode("e0"), 120_000L, 600_000L)
        val history = store.library.value.history
        assertEquals(PODCAST_HISTORY_LIMIT, history.size)
        assertEquals("e0", history.first().track.id)
        assertTrue(history.first().inProgress)
        assertEquals(120_000L, resumePositionMs(history.first()))
    }

    @Test
    fun nearEndMarksCompletedAndPlayedToggleResetsPosition() {
        val store = newStore()
        store.saveProgress(episode("a"), 590_000L, 600_000L)
        assertTrue(store.library.value.history.single().completed)
        store.setPlayed(episode("a"), false)
        val entry = store.library.value.history.single()
        assertFalse(entry.completed)
        assertEquals(0L, entry.positionMs)
        store.setPlayed(episode("b"), true)
        assertEquals(listOf("b", "a"), store.library.value.history.map { it.track.id })
        store.remove("b")
        store.clearHistory()
        assertTrue(store.library.value.history.isEmpty())
    }
}
