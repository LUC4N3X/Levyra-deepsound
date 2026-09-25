package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueSpaceEngineTest {

    @Test
    fun restoreLoadsTheActiveSpace() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("gym", listOf(track("g1"), track("g2")), currentIndex = 1, positionMs = 42_000L))
        storage.activeId = "gym"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())

        val restored = engine.restore(emptyList(), -1, 0L)

        assertEquals("gym", restored.spaceId)
        assertEquals(listOf("g1", "g2"), restored.tracks.map { it.id })
        assertEquals(1, restored.currentIndex)
        assertEquals(42_000L, restored.positionMs)
    }

    @Test
    fun restorePreservesExactShuffleTraversalAndCursor() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        val savedOrder = listOf(2, 0, 3, 1)
        storage.put(
            persisted("shuffle", listOf(track("1"), track("2"), track("3"), track("4")), 3, 18_000L)
                .copy(
                    shuffleEnabled = true,
                    shuffleOrder = savedOrder,
                    shuffleCursor = 2,
                    repeatMode = RepeatMode.All
                )
        )
        storage.activeId = "shuffle"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())

        val restored = engine.restore(emptyList(), -1, 0L)

        assertTrue(restored.shuffleEnabled)
        assertEquals(savedOrder, restored.shuffleOrder)
        assertEquals(2, restored.shuffleCursor)
        assertEquals(3, restored.currentIndex)
        assertEquals(18_000L, restored.positionMs)
        assertEquals(RepeatMode.All, restored.repeatMode)
    }

    @Test
    fun restoreOfAnEmptyActiveSpaceDoesNotInheritFallbackQueue() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("drive", emptyList(), currentIndex = -1, positionMs = 0L))
        storage.activeId = "drive"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())

        val restored = engine.restore(listOf(track("last")), 0, 12_000L)

        assertEquals("drive", restored.spaceId)
        assertTrue(restored.tracks.isEmpty())
        assertEquals(-1, restored.currentIndex)
        assertEquals(0L, restored.positionMs)
    }

    @Test
    fun switchPersistsOutgoingPositionAndRestoresTarget() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1"), track("a2"), track("a3")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("b", listOf(track("b1"), track("b2")), currentIndex = 1, positionMs = 73_000L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)
        engine.select(2)
        val generationBefore = engine.state.value.generation

        val switched = engine.switchSpace("b", outgoingPositionMs = 95_500L)

        assertNotNull(switched)
        assertEquals("b", engine.state.value.spaceId)
        assertEquals("b2", engine.state.value.currentTrack?.id)
        assertEquals(73_000L, engine.state.value.positionMs)
        assertTrue(engine.state.value.generation > generationBefore)
        assertEquals("b", storage.activeId)
        val savedA = storage.saved.getValue("a")
        assertEquals(2, savedA.currentIndex)
        assertEquals(95_500L, savedA.positionMs)
        assertEquals(listOf("a1", "a2", "a3"), savedA.tracks.map { it.id })

        engine.switchSpace("a")

        assertEquals("a3", engine.state.value.currentTrack?.id)
        assertEquals(95_500L, engine.state.value.positionMs)
        assertEquals(73_000L, storage.saved.getValue("b").positionMs)
    }

    @Test
    fun switchingToTheActiveSpaceIsANoOp() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)
        val saves = storage.saveCount

        val result = engine.switchSpace("a", outgoingPositionMs = 10_000L)

        assertEquals("a", result?.spaceId)
        assertEquals(saves, storage.saveCount)
    }

    @Test
    fun switchToAMissingSpaceKeepsTheCurrentQueue() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        assertNull(engine.switchSpace("ghost"))
        assertEquals("a", engine.state.value.spaceId)
        assertEquals("a", storage.activeId)
    }

    @Test
    fun switchIsRefusedDuringTransientPlayback() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("b", listOf(track("b1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        val preserved = engine.restore(emptyList(), -1, 0L)
        engine.beginTransientPlayback(preserved, listOf(track("live")), 0)

        assertNull(engine.switchSpace("b"))
        assertEquals("a", storage.activeId)
    }

    @Test
    fun mutationsDuringASwitchAreSavedToTheOutgoingSpace() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val storage = FakeQueueSpaceStorage(loadGate = gate)
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("b", listOf(track("b1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        storage.loadGateEnabled = false
        engine.restore(emptyList(), -1, 0L)
        storage.loadGateEnabled = true

        val switch = async(Dispatchers.Default) { engine.switchSpace("b") }
        while (!storage.loadWaiting) yield()
        engine.addLast(track("a2"))
        gate.complete(Unit)
        switch.await()

        assertEquals("b", engine.state.value.spaceId)
        assertEquals(listOf("b1"), engine.state.value.tracks.map { it.id })
        assertEquals(listOf("a1", "a2"), storage.saved.getValue("a").tracks.map { it.id })
    }

    @Test
    fun concurrentSwitchesSettleOnAConsistentActiveSpace() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        listOf("a", "b", "c").forEach { id ->
            storage.put(persisted(id, listOf(track("${id}1"), track("${id}2")), currentIndex = 0, positionMs = 0L))
        }
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        repeat(20) { round ->
            listOf("b", "c", "a").map { id ->
                async(Dispatchers.Default) { engine.switchSpace(id, outgoingPositionMs = round * 1_000L) }
            }.awaitAll()
            val active = engine.state.value
            assertEquals(storage.activeId, active.spaceId)
            assertTrue(active.tracks.all { it.id.startsWith(active.spaceId) })
        }
        listOf("a", "b", "c").forEach { id ->
            assertTrue(storage.saved.getValue(id).tracks.all { it.id.startsWith(id) })
        }
    }

    @Test
    fun staleDeferredPositionIsNotWrittenIntoTheNewSpace() = runBlocking {
        val dispatcher = ManualDispatcher()
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("b", listOf(track("b1")), currentIndex = 0, positionMs = 9_000L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, dispatcher)
        engine.restore(emptyList(), -1, 0L)
        engine.updatePosition(61_000L)

        engine.switchSpace("b")
        dispatcher.runAll()

        assertTrue(storage.positionUpdates.none { it.first == "b" })
        assertEquals(9_000L, engine.state.value.positionMs)
        assertEquals(9_000L, storage.saved.getValue("b").positionMs)
    }

    @Test
    fun activeSpaceCannotBeDeletedButOthersCan() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("b", listOf(track("b1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        assertFalse(engine.deleteSpace("a"))
        assertTrue(engine.deleteSpace("b"))
        assertFalse(storage.saved.containsKey("b"))
        assertTrue(storage.saved.containsKey("a"))
    }

    @Test
    fun appendToInactiveSpaceDeduplicatesAndSelectsFirstTrackOfEmptySpace() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("empty", emptyList(), currentIndex = -1, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        assertTrue(engine.appendToSpace("empty", listOf(track("x"), track("y"), track("x"))))
        assertTrue(engine.appendToSpace("empty", listOf(track("y"), track("z"))))

        val saved = storage.saved.getValue("empty")
        assertEquals(listOf("x", "y", "z"), saved.tracks.map { it.id })
        assertEquals(0, saved.currentIndex)
        assertEquals(listOf("a1"), engine.state.value.tracks.map { it.id })
    }

    @Test
    fun appendToTheActiveSpaceUsesTheLiveQueue() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        assertTrue(engine.appendToSpace("a", listOf(track("a2"))))

        assertEquals(listOf("a1", "a2"), engine.state.value.tracks.map { it.id })
    }

    @Test
    fun clearingTheActiveSpacePersistsAnEmptyQueue() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1"), track("a2")), currentIndex = 1, positionMs = 5_000L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        assertTrue(engine.clearSpace("a"))

        assertTrue(engine.state.value.tracks.isEmpty())
        assertEquals(-1, engine.state.value.currentIndex)
        assertEquals("a", engine.state.value.spaceId)
        assertTrue(storage.saved.getValue("a").tracks.isEmpty())
    }

    @Test
    fun createdSpaceIsSeededWithoutActivation() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)

        val created = engine.createSpace("Gym", listOf(track("g1"), track("g1"), track("g2")))

        assertNotNull(created)
        assertEquals("a", storage.activeId)
        assertEquals(listOf("g1", "g2"), storage.saved.getValue(created!!.id).tracks.map { it.id })
        assertEquals("a", engine.state.value.spaceId)
    }

    @Test
    fun duplicatingTheActiveSpaceCapturesItsLatestState() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("a1")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)
        engine.addLast(track("a2"))

        val copy = engine.duplicateSpace("a", "Copy")

        assertNotNull(copy)
        assertEquals(listOf("a1", "a2"), storage.saved.getValue(copy!!.id).tracks.map { it.id })
    }

    @Test
    fun appendKeepsShuffleOrderCompleteForNewTracks() {
        val existing = persisted("s", listOf(track("1"), track("2")), currentIndex = 1, positionMs = 3_000L)
            .copy(shuffleEnabled = true, shuffleOrder = listOf(1, 0))
        val next = existing.tracks + track("3")

        val updated = queueSpaceAfterAppend(existing, next, now = 99L)

        assertEquals(listOf(1, 0, 2), updated.shuffleOrder)
        assertEquals(1, updated.currentIndex)
        assertEquals(3_000L, updated.positionMs)
        assertEquals(existing.generation + 1L, updated.generation)
    }

    @Test
    fun summaryValuesCountTracksAndCollectDistinctArtwork() {
        val tracks = listOf(
            track("1").copy(durationMs = 1_000L, thumbnailUrl = "a"),
            track("2").copy(durationMs = 2_000L, thumbnailUrl = "a"),
            track("3").copy(durationMs = -5L, thumbnailUrl = "", largeThumbnailUrl = "b")
        )

        val summary = queueSpaceSummaryValues(tracks, updatedAt = 7L)

        assertEquals(3, summary.trackCount)
        assertEquals(3_000L, summary.durationMs)
        assertEquals("a\nb", summary.artworkUrls)
    }

    @Test
    fun spaceNamesAreTrimmedAndBounded() {
        assertEquals("Late night drive", normalizeQueueSpaceName("  Late   night\tdrive "))
        assertEquals(48, normalizeQueueSpaceName("x".repeat(80)).length)
    }

    @Test
    fun removedAutomaticTrackIsNotReinsertedByRadioInTheSameSpace() = runBlocking {
        val engine = restoredEngine("a", listOf(track("seed")))
        engine.appendRadioTracks(listOf(track("r1"), track("r2")))
        val removedIndex = engine.state.value.tracks.indexOfFirst { it.id == "r1" }

        engine.remove(removedIndex)
        engine.appendRadioTracks(listOf(track("r1"), track("r3")))

        assertEquals(listOf("seed", "r2", "r3"), engine.state.value.tracks.map { it.id })
        assertTrue(engine.rejectedAutomaticKeys().isNotEmpty())
    }

    @Test
    fun removingAManualTrackDoesNotCreateATombstone() = runBlocking {
        val engine = restoredEngine("a", listOf(track("seed")))
        engine.addLast(track("m1"))

        engine.remove(1)
        engine.appendRadioTracks(listOf(track("m1")))

        assertEquals(listOf("seed", "m1"), engine.state.value.tracks.map { it.id })
        assertTrue(engine.rejectedAutomaticKeys().isEmpty())
    }

    @Test
    fun explicitAddClearsTheTombstoneAndIsAccepted() = runBlocking {
        val engine = restoredEngine("a", listOf(track("seed")))
        engine.appendRadioTracks(listOf(track("r1")))
        engine.remove(1)

        engine.playNext(track("r1"))

        assertEquals(listOf("seed", "r1"), engine.state.value.tracks.map { it.id })
        assertTrue(engine.rejectedAutomaticKeys().isEmpty())
    }

    @Test
    fun undoRestoresAutomaticProvenanceWithoutTombstone() = runBlocking {
        val engine = restoredEngine("a", listOf(track("seed")))
        engine.appendRadioTracks(listOf(track("r1")))
        engine.remove(1)

        engine.undoRemove()

        assertTrue(engine.rejectedAutomaticKeys().isEmpty())
        engine.remove(1)
        assertTrue(engine.rejectedAutomaticKeys().isNotEmpty())
    }

    @Test
    fun tombstoneMatchesAlternateUploadsOfTheSameSong() = runBlocking {
        val engine = restoredEngine("a", listOf(track("seed")))
        engine.appendRadioTracks(listOf(track("r1")))
        engine.remove(1)

        engine.appendRadioTracks(listOf(track("r1-alt").copy(title = "Title r1")))

        assertEquals(listOf("seed"), engine.state.value.tracks.map { it.id })
    }

    @Test
    fun tombstonesAreScopedPerSpaceAndClearedWithTheSession() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("a", listOf(track("seed")), currentIndex = 0, positionMs = 0L))
        storage.put(persisted("b", listOf(track("seed")), currentIndex = 0, positionMs = 0L))
        storage.activeId = "a"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())
        engine.restore(emptyList(), -1, 0L)
        engine.appendRadioTracks(listOf(track("r1")))
        engine.remove(1)

        engine.switchSpace("b")
        engine.appendRadioTracks(listOf(track("r1")))
        assertEquals(listOf("seed", "r1"), engine.state.value.tracks.map { it.id })

        engine.switchSpace("a")
        assertTrue(engine.rejectedAutomaticKeys().isNotEmpty())
        engine.clear()
        assertTrue(engine.rejectedAutomaticKeys().isEmpty())
    }

    @Test
    fun tombstoneMemoryIsBounded() {
        val tombstones = AutoQueueTombstones(maxKeysPerSpace = 4, maxSpaces = 2)
        val tracks = (1..10).map { track("t$it") }
        tombstones.markAutomatic("a", tracks)
        assertFalse(tombstones.isAutomatic("a", tracks.first()))
        assertTrue(tombstones.isAutomatic("a", tracks.last()))
        tombstones.recordRemoval("a", tracks.takeLast(2))
        assertTrue(tombstones.rejectedKeys("a").size <= 4)

        tombstones.markAutomatic("b", tracks.take(1))
        tombstones.markAutomatic("c", tracks.take(1))
        assertFalse(tombstones.isAutomatic("a", tracks[7]))
    }

    private suspend fun restoredEngine(spaceId: String, tracks: List<Track>): PersistentQueueEngine {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted(spaceId, tracks, currentIndex = 0, positionMs = 0L))
        storage.activeId = spaceId
        return PersistentQueueEngine(storage, ManualDispatcher()).also { it.restore(emptyList(), -1, 0L) }
    }

    private fun persisted(id: String, tracks: List<Track>, currentIndex: Int, positionMs: Long) = PersistentQueueSnapshot(
        spaceId = id,
        tracks = tracks,
        currentIndex = currentIndex,
        positionMs = positionMs,
        shuffleEnabled = false,
        shuffleOrder = emptyList(),
        shuffleCursor = -1,
        history = emptyList(),
        repeatMode = RepeatMode.Off,
        radioEnabled = false,
        generation = 3L,
        updatedAt = 1L
    )

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
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

    private class ManualDispatcher : CoroutineDispatcher() {
        private val tasks = ConcurrentLinkedQueue<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks += block
        }

        fun runAll() {
            while (true) tasks.poll()?.run() ?: return
        }
    }

    private class FakeQueueSpaceStorage(
        private val loadGate: CompletableDeferred<Unit>? = null
    ) : QueueSpaceStorage {
        val saved = ConcurrentHashMap<String, PersistentQueueSnapshot>()
        val positionUpdates = ConcurrentLinkedQueue<Pair<String, Long>>()
        @Volatile var activeId: String = ""
        @Volatile var saveCount = 0
        @Volatile var loadGateEnabled = true
        @Volatile var loadWaiting = false
        private var created = 0

        override val spaces: Flow<List<QueueSpaceSummary>> = MutableStateFlow(emptyList())

        fun put(snapshot: PersistentQueueSnapshot) {
            saved[snapshot.spaceId] = snapshot
        }

        override suspend fun activeSpaceId(): String = activeId

        override suspend fun load(spaceId: String): PersistentQueueSnapshot? {
            if (loadGate != null && loadGateEnabled) {
                loadWaiting = true
                loadGate.await()
            }
            return saved[spaceId]
        }

        override suspend fun save(snapshot: PersistentQueueSnapshot): Boolean {
            if (!saved.containsKey(snapshot.spaceId)) return false
            saveCount++
            saved[snapshot.spaceId] = snapshot
            return true
        }

        override suspend fun updatePosition(spaceId: String, positionMs: Long, updatedAt: Long) {
            positionUpdates += spaceId to positionMs
            saved[spaceId]?.let { saved[spaceId] = it.copy(positionMs = positionMs) }
        }

        override suspend fun activate(spaceId: String): Boolean {
            if (!saved.containsKey(spaceId)) return false
            activeId = spaceId
            return true
        }

        override suspend fun create(name: String): QueueSpaceSummary {
            val id = "created-${++created}"
            saved[id] = PersistentQueueSnapshot(
                id, emptyList(), -1, 0L, false, emptyList(), -1, emptyList(), RepeatMode.Off, false, 1L, 0L
            )
            return QueueSpaceSummary(id, name, 0L, 0L, 0L, false, 0, 0L, emptyList())
        }

        override suspend fun rename(spaceId: String, name: String): Boolean = saved.containsKey(spaceId)

        override suspend fun duplicate(sourceId: String, name: String): QueueSpaceSummary? {
            val source = saved[sourceId] ?: return null
            val summary = create(name)
            saved[summary.id] = source.copy(spaceId = summary.id)
            return summary
        }

        override suspend fun clear(spaceId: String) {
            saved[spaceId]?.let { saved[spaceId] = it.copy(tracks = emptyList(), currentIndex = -1) }
        }

        override suspend fun delete(spaceId: String): Boolean = saved.remove(spaceId) != null
    }
}
