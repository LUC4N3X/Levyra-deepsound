package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.radio.SavedStation
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import java.nio.file.Files
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiscoveryDeckControllerTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val monday = LocalDate.of(2026, 11, 23)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun picksKeepSimilarFirstAndDropKnownExcludedAndDuplicates() {
        val seed = track("seed", "Seed", "Someone")
        val picks = discoveryPicks(
            fresh = listOf(track("a", "Alpha", "One"), track("known", "Known", "Two"), track("a2", "alpha ", "one")),
            editorial = listOf(track("b", "Beta", "Banned Band"), track("c", "Gamma", "Three")),
            similar = listOf(DailyPick(track("d", "Delta", "Four"), seed)),
            knownIds = setOf("known")
        ) { it.artist == "Banned Band" }
        assertEquals(listOf("d", "a", "c"), picks.map { it.track.id })
        assertEquals(DiscoveryPickReason.SimilarTo, picks.first().reason)
        assertEquals(seed, picks.first().seed)
    }

    @Test
    fun saveAddsToWeeklyPlaylistAndUndoRemovesIt() {
        val library = libraryStore()
        val controller = controller(library)
        controller.open(listOf(pick("a"), pick("b")))
        controller.save()
        val weekly = library.current.playlists.single { it.id == controller.weeklyPlaylistId }
        assertEquals("levyra-discovery-2026-w48", weekly.id)
        assertEquals(listOf("a"), weekly.tracks.map { it.id })
        assertEquals("b", controller.deck.value.current?.track?.id)
        assertEquals(DiscoveryNoticeKind.Saved, controller.deck.value.notice?.kind)
        controller.undo()
        assertEquals("a", controller.deck.value.current?.track?.id)
        assertTrue(library.current.playlists.single { it.id == controller.weeklyPlaylistId }.tracks.isEmpty())
    }

    @Test
    fun dismissedAndSavedTracksAreNotShownAgain() {
        val library = libraryStore()
        val dismissedFile = temporary.root.toPath().resolve("dismissed.json")
        val first = controller(library, dismissedFile)
        first.open(listOf(pick("a"), pick("b"), pick("c")))
        first.dismiss()
        first.save()
        assertTrue(first.deck.value.canUndo)
        val reopened = controller(library, dismissedFile)
        reopened.open(listOf(pick("a"), pick("b"), pick("c")))
        assertEquals(listOf("c"), reopened.deck.value.picks.map { it.track.id })
    }

    @Test
    fun undoingDismissalShowsTheCardAgainLater() {
        val library = libraryStore()
        val dismissedFile = temporary.root.toPath().resolve("dismissed.json")
        val controller = controller(library, dismissedFile)
        controller.open(listOf(pick("a"), pick("b")))
        controller.dismiss()
        controller.undo()
        assertEquals("a", controller.deck.value.current?.track?.id)
        controller(library, dismissedFile).apply { open(listOf(pick("a"))) }.also {
            assertEquals("a", it.deck.value.current?.track?.id)
        }
    }

    @Test
    fun failedSaveKeepsTheCardAndReportsIt() {
        val paths = AppPaths(temporary.newFolder().toPath()).prepare()
        val library = LibraryStore.create(paths)
        Files.createDirectories(paths.libraryFile.resolveSibling("${paths.libraryFile.fileName}.tmp"))
        val controller = controller(library)
        controller.open(listOf(pick("a"), pick("b")))
        controller.save()
        assertEquals("a", controller.deck.value.current?.track?.id)
        assertFalse(controller.deck.value.canUndo)
        assertEquals(DiscoveryNoticeKind.SaveFailed, controller.deck.value.notice?.kind)
        assertTrue(library.current.playlists.none { it.id == controller.weeklyPlaylistId })
    }

    @Test
    fun undoRemovesFromThePlaylistUsedBySave() {
        val library = libraryStore()
        var day = monday.plusDays(6)
        val controller = controller(library, today = { day })
        controller.open(listOf(pick("a"), pick("b")))
        controller.save()
        val savedWeek = controller.weeklyPlaylistId
        day = day.plusDays(1)
        controller.undo()
        assertEquals("a", controller.deck.value.current?.track?.id)
        assertTrue(library.current.playlists.single { it.id == savedWeek }.tracks.isEmpty())
    }

    @Test
    fun swipeDecisionForAnOlderCardIsIgnored() {
        val library = libraryStore()
        val controller = controller(library)
        controller.open(listOf(pick("a"), pick("b"), pick("c")))
        val draggedKey = controller.deck.value.current!!.key
        controller.save()
        controller.decide(draggedKey, save = false)
        assertEquals("b", controller.deck.value.current?.track?.id)
        controller.decide(controller.deck.value.current!!.key, save = false)
        assertEquals("c", controller.deck.value.current?.track?.id)
    }

    @Test
    fun undoStaysAvailableAfterTheLastCard() {
        val controller = controller(libraryStore())
        controller.open(listOf(pick("a")))
        controller.dismiss()
        assertEquals(null, controller.deck.value.current)
        assertTrue(controller.deck.value.canUndo)
        controller.undo()
        assertEquals("a", controller.deck.value.current?.track?.id)
    }

    @Test
    fun appendKeepsPositionAndSkipsKnownCards() {
        val controller = controller(libraryStore())
        controller.open(listOf(pick("a"), pick("b")))
        controller.dismiss()
        controller.append(listOf(pick("b"), pick("c")))
        assertEquals(listOf("a", "b", "c"), controller.deck.value.picks.map { it.track.id })
        assertEquals("b", controller.deck.value.current?.track?.id)
    }

    @Test
    fun previewStartsInsideLongTracksAndAtZeroForShortOnes() {
        assertEquals(0L, discoveryPreviewStartMs(35_000L))
        assertEquals(63_000L, discoveryPreviewStartMs(210_000L))
    }

    @Test
    fun legacyRadioFavoritesWithoutNewFieldsStillDecode() {
        val file = temporary.root.toPath().resolve("radio.json")
        Files.writeString(file, """[{"uuid":"one","name":"One","streamUrl":"https://radio.example/one","details":"Italy · rock"}]""")
        val store = JsonFileStore(file, ListSerializer(SavedStation.serializer()), { emptyList() })
        val saved = store.read().single()
        assertEquals("One", saved.name)
        assertEquals(0L, saved.addedAt)
        assertTrue(saved.tags.isEmpty())
    }

    private fun libraryStore(): LibraryStore = LibraryStore.create(AppPaths(temporary.newFolder().toPath()).prepare())

    private fun controller(
        library: LibraryStore,
        dismissedFile: java.nio.file.Path = temporary.root.toPath().resolve("d-${System.nanoTime()}.json"),
        today: () -> LocalDate = { monday }
    ) =
        DiscoveryDeckController(
            scope = scope,
            libraryStore = library,
            dismissedStore = JsonFileStore(dismissedFile, ListSerializer(String.serializer()), { emptyList() }),
            previewUrl = { "" },
            playerFactory = { error("unused") },
            pauseMain = { false },
            resumeMain = {},
            playFullTrack = {},
            language = { "it" },
            today = today,
            io = Dispatchers.Unconfined
        )

    private fun pick(id: String) = DiscoveryPick(track(id, "Title $id", "Artist $id"), DiscoveryPickReason.Editorial)

    private fun track(id: String, title: String, artist: String) = Track(id = id, title = title, artist = artist, videoUrl = "https://example.com/$id")
}
