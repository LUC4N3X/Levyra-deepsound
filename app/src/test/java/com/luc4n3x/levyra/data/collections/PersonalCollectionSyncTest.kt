package com.luc4n3x.levyra.data.collections

import com.luc4n3x.levyra.data.local.PersonalCollectionTrackEntity
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalCollectionSyncTest {

    private fun entry(key: String, title: String = key, artwork: String = "") = ParsedCollectionEntry(
        entryKey = key,
        title = title,
        artist = "Artist",
        album = "",
        albumArtist = "",
        durationMs = 1_000L,
        audioUrl = "https://cdn.example.com/$key.mp3",
        videoUrl = "",
        artworkUrl = artwork,
        trackNumber = 0,
        discNumber = 0,
        year = 0
    )

    @Test
    fun firstImportAddsEverythingWithStableTrackIds() {
        val plan = PersonalCollectionSyncPlanner.plan("c1", emptyList(), listOf(entry("a"), entry("b")), now = 10L)
        assertEquals(2, plan.added)
        assertEquals(0, plan.updated)
        assertTrue(plan.removedEntryKeys.isEmpty())
        assertEquals(listOf("pc:c1:a", "pc:c1:b"), plan.tracks.map { it.trackId })
        assertEquals(listOf(0, 1), plan.tracks.map { it.position })
        assertTrue(plan.tracks.all { isPersonalCollectionTrackId(it.trackId) && personalCollectionIdOf(it.trackId) == "c1" })
    }

    @Test
    fun refreshDetectsAddedRemovedAndUpdatedWhileKeepingHistory() {
        val initial = PersonalCollectionSyncPlanner.plan(
            "c1",
            emptyList(),
            listOf(entry("a", artwork = "https://cdn.example.com/a.jpg"), entry("b"), entry("c")),
            now = 10L
        ).tracks.map { if (it.entryKey == "a") it.copy(artworkFile = "/data/art/a.jpg") else it }
        val plan = PersonalCollectionSyncPlanner.plan(
            "c1",
            initial,
            listOf(entry("c"), entry("a", title = "A (Remastered)", artwork = "https://cdn.example.com/a.jpg"), entry("d")),
            now = 99L
        )
        assertEquals(1, plan.added)
        assertEquals(1, plan.updated)
        assertEquals(listOf("b"), plan.removedEntryKeys)
        val byKey = plan.tracks.associateBy { it.entryKey }
        assertEquals("A (Remastered)", byKey.getValue("a").title)
        assertEquals("/data/art/a.jpg", byKey.getValue("a").artworkFile)
        assertEquals(10L, byKey.getValue("a").addedAt)
        assertEquals(99L, byKey.getValue("d").addedAt)
        assertEquals(listOf("c", "a", "d"), plan.tracks.map { it.entryKey })
        assertEquals(listOf(0, 1, 2), plan.tracks.map { it.position })
    }

    @Test
    fun changedArtworkUrlInvalidatesTheCachedFile() {
        val initial = listOf(
            PersonalCollectionTrackEntity(
                "c1", "a", "pc:c1:a", 0, "a", "", "", "", 0L, "https://cdn.example.com/a.mp3", "",
                "https://cdn.example.com/old.jpg", "/data/old.jpg", 0, 0, 0, 1L
            )
        )
        val plan = PersonalCollectionSyncPlanner.plan("c1", initial, listOf(entry("a", artwork = "https://cdn.example.com/new.jpg")), 5L)
        assertEquals("", plan.tracks.single().artworkFile)
        assertEquals(1, plan.updated)
    }

    @Test
    fun reorderOnlyIsNotCountedAsAnUpdate() {
        val initial = PersonalCollectionSyncPlanner.plan("c1", emptyList(), listOf(entry("a"), entry("b")), 1L).tracks
        val plan = PersonalCollectionSyncPlanner.plan("c1", initial, listOf(entry("b"), entry("a")), 2L)
        assertEquals(0, plan.added)
        assertEquals(0, plan.updated)
        assertTrue(plan.removedEntryKeys.isEmpty())
    }

    @Test
    fun emptyServerResponseCannotWipeTheCollectionBecauseParsingRejectsIt() {
        val result = PersonalCollectionParser.parse(ByteArray(0), "https://cdn.example.com/list.m3u", "x")
        assertEquals(CollectionParseResult.Failure(CollectionParseError.Empty), result)
    }

    @Test
    fun concurrentRefreshOfTheSameCollectionRunsOnce() = runBlocking {
        val gate = PersonalCollectionSyncGate()
        val running = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val release = CompletableDeferred<Unit>()
        val first = async(Dispatchers.Default) {
            gate.tryRun("c1") {
                peak.updateAndGet { maxOf(it, running.incrementAndGet()) }
                release.await()
                running.decrementAndGet()
                "ran"
            }
        }
        while (running.get() == 0) Thread.yield()
        val contenders = (1..8).map { async(Dispatchers.Default) { gate.tryRun("c1") { "duplicate" } } }.awaitAll()
        val otherCollection = gate.tryRun("c2") { "independent" }
        release.complete(Unit)
        assertEquals("ran", first.await())
        assertTrue(contenders.all { it == null })
        assertEquals("independent", otherCollection)
        assertEquals(1, peak.get())
        assertEquals("again", gate.tryRun("c1") { "again" })
    }

    @Test
    fun deleteWaitsForAnInFlightRefresh() = runBlocking {
        val gate = PersonalCollectionSyncGate()
        val order = mutableListOf<String>()
        val inside = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val refresh = async(Dispatchers.Default) {
            gate.tryRun("c1") {
                inside.complete(Unit)
                release.await()
                synchronized(order) { order += "refresh" }
            }
        }
        inside.await()
        val delete = async(Dispatchers.Default) { gate.run("c1") { synchronized(order) { order += "delete" } } }
        release.complete(Unit)
        refresh.await()
        delete.await()
        assertEquals(listOf("refresh", "delete"), order)
    }

    @Test
    fun addressClassificationSeparatesPublicPrivateAndBlocked() {
        fun classify(host: String) = PersonalCollectionUrlPolicy.classify(InetAddress.getByName(host))
        assertEquals(CollectionAddressClass.Public, classify("93.184.216.34"))
        assertEquals(CollectionAddressClass.Private, classify("192.168.1.20"))
        assertEquals(CollectionAddressClass.Private, classify("10.0.2.2"))
        assertEquals(CollectionAddressClass.Private, classify("172.16.4.1"))
        assertEquals(CollectionAddressClass.Private, classify("100.100.1.1"))
        assertEquals(CollectionAddressClass.Private, classify("fd12::1"))
        assertEquals(CollectionAddressClass.Blocked, classify("127.0.0.1"))
        assertEquals(CollectionAddressClass.Blocked, classify("::1"))
        assertEquals(CollectionAddressClass.Blocked, classify("169.254.169.254"))
        assertEquals(CollectionAddressClass.Blocked, classify("0.0.0.0"))
        assertEquals(CollectionAddressClass.Blocked, classify("224.0.0.1"))
    }

    @Test
    fun localNetworkHostsAreRecognisedWithoutDns() {
        assertTrue(PersonalCollectionUrlPolicy.isLocalNetworkLiteral("192.168.0.10"))
        assertTrue(PersonalCollectionUrlPolicy.isLocalNetworkLiteral("nas.local"))
        assertTrue(PersonalCollectionUrlPolicy.isLocalNetworkLiteral("media.home.arpa"))
        assertTrue(PersonalCollectionUrlPolicy.isLocalNetworkLiteral("jukebox"))
        assertFalse(PersonalCollectionUrlPolicy.isLocalNetworkLiteral("music.example.com"))
        assertFalse(PersonalCollectionUrlPolicy.isLocalNetworkLiteral("8.8.8.8"))
    }

    @Test
    fun urlHelpersNormaliseIdentityAndHideCredentials() {
        assertEquals(
            "https://cdn.example.com/a.mp3?x=1",
            PersonalCollectionUrlPolicy.identityOf("https://bob:pw@CDN.Example.com/a.mp3?x=1#frag")
        )
        assertEquals("cdn.example.com:8443/music", PersonalCollectionUrlPolicy.displayUrl("https://bob:pw@cdn.example.com:8443/music/"))
        assertEquals("https://cdn.example.com:443", PersonalCollectionUrlPolicy.origin("https://cdn.example.com/x"))
        assertTrue(PersonalCollectionUrlPolicy.isCleartext("http://nas.local/a.mp3"))
        assertFalse(PersonalCollectionUrlPolicy.isCleartext("https://nas.local/a.mp3"))
        assertNull(PersonalCollectionUrlPolicy.origin("not a url"))
    }

    @Test
    fun cleartextConsentIncludesArtworkAndVideoUrls() {
        val parsed = ParsedCollection(
            format = PersonalCollectionFormat.JSON,
            name = "Collection",
            artworkUrl = "https://cdn.example.com/cover.jpg",
            entries = listOf(entry("a").copy(videoUrl = "http://media.example.com/a.mp4")),
            issues = emptyList(),
            credentials = emptyList()
        )

        assertTrue(parsed.usesCleartext())
        assertEquals(0, parsed.cleartextCount())
        assertEquals(CollectionSyncError.Cleartext, parsed.consentError("https://lists.example.com/list.json", false, false))
        assertNull(parsed.consentError("https://lists.example.com/list.json", true, false))
    }

    @Test
    fun localNetworkEntriesRequireCollectionConsent() {
        val parsed = ParsedCollection(
            format = PersonalCollectionFormat.M3U,
            name = "Home",
            artworkUrl = "",
            entries = listOf(entry("a").copy(audioUrl = "https://nas.local/a.mp3")),
            issues = emptyList(),
            credentials = emptyList()
        )

        assertEquals(CollectionSyncError.LocalNetwork, parsed.consentError("https://lists.example.com/list.m3u", false, false))
        assertNull(parsed.consentError("https://lists.example.com/list.m3u", false, true))
    }
}
