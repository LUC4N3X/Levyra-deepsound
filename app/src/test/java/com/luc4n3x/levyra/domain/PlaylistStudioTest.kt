package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistStudioTest {

    @Test
    fun `editing an existing playlist starts from its current state`() {
        val auto = playlist(tracks = listOf(track("a"), track("b"), track("")), coverMode = PlaylistCoverMode.AUTO)
        val draft = PlaylistStudioEdits.startFrom(auto)
        assertEquals("p1", draft.playlistId)
        assertEquals(listOf("a", "b"), draft.tracks.map { it.id })
        assertEquals(PlaylistCoverStyle.Automatic, draft.coverStyle)
        assertEquals("", draft.customCoverUrl)

        val custom = PlaylistStudioEdits.startFrom(auto.copy(coverMode = PlaylistCoverMode.CUSTOM, coverUrl = "file:///cover.jpg"))
        assertEquals(PlaylistCoverStyle.Current, custom.coverStyle)
        assertEquals("file:///cover.jpg", custom.customCoverUrl)
    }

    @Test
    fun `new drafts cannot be saved without a name`() {
        val draft = PlaylistStudioEdits.startNew()
        assertTrue(draft.isNew)
        assertFalse(draft.canSave)
        assertTrue(PlaylistStudioEdits.rename(draft, "Night drive").canSave)
        assertFalse(PlaylistStudioEdits.rename(draft, "   ").canSave)
    }

    @Test
    fun `rename strips line breaks and caps length`() {
        val renamed = PlaylistStudioEdits.rename(PlaylistStudioEdits.startNew(), "Line\nBreak" + "x".repeat(400))
        assertFalse(renamed.name.contains('\n'))
        assertEquals(PLAYLIST_STUDIO_NAME_MAX_LENGTH, renamed.name.length)
    }

    @Test
    fun `add ignores duplicates and blank ids and drops stream urls`() {
        var draft = PlaylistStudioEdits.startNew()
        draft = PlaylistStudioEdits.add(draft, track("a").copy(streamUrl = "https://stream"))
        val unchanged = PlaylistStudioEdits.add(draft, track("a"))
        assertSame(draft, unchanged)
        assertSame(draft, PlaylistStudioEdits.add(draft, track("")))
        assertEquals("", draft.tracks.single().streamUrl)
        assertTrue(draft.contains("a"))
    }

    @Test
    fun `toggle adds then removes`() {
        val added = PlaylistStudioEdits.toggle(PlaylistStudioEdits.startNew(), track("a"))
        assertEquals(1, added.tracks.size)
        assertTrue(PlaylistStudioEdits.toggle(added, track("a")).tracks.isEmpty())
    }

    @Test
    fun `remove clears a cover track that no longer belongs to the playlist`() {
        var draft = PlaylistStudioEdits.startNew(seed = listOf(track("a"), track("b")))
        draft = PlaylistStudioEdits.setCoverTrack(draft, "b")
        assertEquals(PlaylistCoverStyle.Artwork, draft.coverStyle)
        val removed = PlaylistStudioEdits.remove(draft, "b")
        assertNull(removed.coverTrackId)
        assertEquals(listOf("a"), removed.tracks.map { it.id })
        assertSame(removed, PlaylistStudioEdits.remove(removed, "missing"))
        assertSame(removed, PlaylistStudioEdits.setCoverTrack(removed, "missing"))
    }

    @Test
    fun `move reorders and rejects invalid indices`() {
        val draft = PlaylistStudioEdits.startNew(seed = listOf(track("a"), track("b"), track("c")))
        assertEquals(listOf("c", "a", "b"), PlaylistStudioEdits.move(draft, 2, 0).tracks.map { it.id })
        assertEquals(listOf("b", "c", "a"), PlaylistStudioEdits.move(draft, 0, 2).tracks.map { it.id })
        assertSame(draft, PlaylistStudioEdits.move(draft, 0, 0))
        assertSame(draft, PlaylistStudioEdits.move(draft, -1, 1))
        assertSame(draft, PlaylistStudioEdits.move(draft, 1, 3))
    }

    @Test
    fun `current cover is only selectable when a custom cover exists`() {
        val draft = PlaylistStudioEdits.startNew()
        assertSame(draft, PlaylistStudioEdits.setCoverStyle(draft, PlaylistCoverStyle.Current))
        assertEquals(PlaylistCoverStyle.Signal, PlaylistStudioEdits.setCoverStyle(draft, PlaylistCoverStyle.Signal).coverStyle)
    }

    @Test
    fun `photo style needs a picked photo to be saved`() {
        val named = PlaylistStudioEdits.rename(PlaylistStudioEdits.startNew(), "Mix")
        assertFalse(named.copy(coverStyle = PlaylistCoverStyle.Photo).canSave)
        val withPhoto = PlaylistStudioEdits.setPhoto(named, PlaylistStudioPhoto("content://photo", 800, 1.2f, 3f, -4f))
        assertEquals(PlaylistCoverStyle.Photo, withPhoto.coverStyle)
        assertTrue(withPhoto.canSave)
    }

    @Test
    fun `stats count duration artists and offline tracks`() {
        val tracks = listOf(
            track("a", artist = "Daft Punk, Pharrell Williams", durationMs = 60_000),
            track("b", artist = "Pharrell Williams feat. Nile Rodgers", durationMs = 30_000),
            track("c", artist = "Björk", durationMs = -5),
            track("d", artist = "bjork", durationMs = 10_000)
        )
        val stats = playlistStudioStats(tracks, downloadedTrackIds = setOf("a", "d", "zz"))
        assertEquals(4, stats.trackCount)
        assertEquals(100_000L, stats.durationMs)
        assertEquals(4, stats.artistCount)
        assertEquals(2, stats.offlineCount)
    }

    @Test
    fun `empty playlist stats are zero`() {
        assertEquals(PlaylistStudioStats(0, 0L, 0, 0), playlistStudioStats(emptyList(), emptySet()))
    }

    @Test
    fun `search index matches title artist and album ignoring accents and case`() {
        val index = StudioSearchIndex(
            listOf(
                track("a", title = "Café del Mar"),
                track("b", artist = "Sigur Rós"),
                track("c", album = "Random Access Memories")
            )
        )
        assertEquals(listOf("a"), index.filter("cafe").map { it.id })
        assertEquals(listOf("b"), index.filter("ROS").map { it.id })
        assertEquals(listOf("c"), index.filter("access mem").map { it.id })
        assertEquals(3, index.filter("  ").size)
        assertTrue(index.filter("nothing").isEmpty())
    }

    @Test
    fun `candidate merge keeps first occurrence order and drops invalid tracks`() {
        val merged = mergeStudioCandidates(
            listOf(track("a"), track("b").copy(streamUrl = "https://x")),
            listOf(track("b"), track(""), track("c", title = ""), track("d"))
        )
        assertEquals(listOf("a", "b", "d"), merged.map { it.id })
        assertTrue(merged.all { it.streamUrl.isEmpty() })
    }

    @Test
    fun `large libraries stay responsive`() {
        val tracks = (0 until 20_000).map { track("t$it", title = "Song $it", artist = "Artist ${it % 700}") }
        val started = System.nanoTime()
        val index = StudioSearchIndex(mergeStudioCandidates(tracks, tracks))
        val hits = index.filter("song 1999")
        val stats = playlistStudioStats(tracks, emptySet())
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(11, hits.size)
        assertEquals(700, stats.artistCount)
        var draft = PlaylistStudioEdits.startNew(seed = tracks)
        draft = PlaylistStudioEdits.move(draft, 19_999, 0)
        assertEquals("t19999", draft.tracks.first().id)
        assertTrue("took ${elapsedMs}ms", elapsedMs < 5_000)
    }

    @Test
    fun `automatic and current styles produce no generated cover`() {
        val draft = PlaylistStudioEdits.startNew("Mix", listOf(track("a")))
        assertNull(buildPlaylistCoverPlan(draft))
        assertNull(buildPlaylistCoverPlan(draft.copy(coverStyle = PlaylistCoverStyle.Current)))
    }

    @Test
    fun `generated cover plans are deterministic`() {
        val tracks = (1..6).map { track("t$it", artwork = "https://img/$it", accentStart = 0xFF102030.toInt() + it) }
        val draft = PlaylistStudioEdits.startNew("Late Night", tracks).copy(coverStyle = PlaylistCoverStyle.Mosaic)
        val first = buildPlaylistCoverPlan(draft)
        val second = buildPlaylistCoverPlan(draft.copy())
        assertEquals(first, second)
        assertEquals(listOf("https://img/1", "https://img/2", "https://img/3", "https://img/4"), first?.artworkUrls)
        assertEquals(stableStudioSeed("Late Night", tracks), first?.seed)
        assertEquals(stableStudioSeed("  late night ", tracks), first?.seed)
        assertNotEquals(first?.seed, stableStudioSeed("Early Morning", tracks))
        assertEquals(stableStudioSeed("", emptyList()), stableStudioSeed("", emptyList()))
        assertNotEquals(stableStudioSeed("Late Night", tracks), stableStudioSeed("Late Night", tracks.reversed()))
    }

    @Test
    fun `mosaic uses distinct artworks only`() {
        val tracks = listOf(
            track("a", artwork = "https://img/same"),
            track("b", artwork = "https://img/same"),
            track("c", artwork = ""),
            track("d", artwork = "https://img/other")
        )
        val plan = buildPlaylistCoverPlan(PlaylistStudioEdits.startNew("x", tracks).copy(coverStyle = PlaylistCoverStyle.Mosaic))
        assertEquals(listOf("https://img/same", "https://img/other"), plan?.artworkUrls)
    }

    @Test
    fun `mosaic skips a second artwork from the same album`() {
        val tracks = listOf(
            track("a", album = "Prima", artist = "Adéla", artwork = "https://img/a"),
            track("b", album = "PRIMA", artist = "ADÉLA", artwork = "https://img/b"),
            track("c", album = "Other", artwork = "https://img/c")
        )
        assertEquals(listOf("https://img/a", "https://img/c"), distinctStudioArtworks(tracks, 4))
        assertEquals(listOf("https://img/a"), distinctStudioArtworks(tracks, 1))
        assertEquals(emptyList<String>(), distinctStudioArtworks(tracks, 0))
    }

    @Test
    fun `artwork style honours the chosen track and falls back to the first artwork`() {
        val tracks = listOf(track("a", artwork = ""), track("b", artwork = "https://img/b"), track("c", artwork = "https://img/c"))
        val base = PlaylistStudioEdits.startNew("x", tracks)
        val chosen = buildPlaylistCoverPlan(PlaylistStudioEdits.setCoverTrack(base, "c"))
        assertEquals(listOf("https://img/c"), chosen?.artworkUrls)
        val fallback = buildPlaylistCoverPlan(base.copy(coverStyle = PlaylistCoverStyle.Artwork))
        assertEquals(listOf("https://img/b"), fallback?.artworkUrls)
    }

    @Test
    fun `empty playlists still get a stable typographic cover with a levyra palette`() {
        val plan = buildPlaylistCoverPlan(PlaylistStudioEdits.startNew("Focus").copy(coverStyle = PlaylistCoverStyle.Signal))
        requireNotNull(plan)
        assertTrue(plan.artworkUrls.isEmpty())
        assertEquals("Focus", plan.title)
        assertEquals(0xFF, plan.primaryColor ushr 24)
        assertEquals(plan, buildPlaylistCoverPlan(PlaylistStudioEdits.startNew("Focus").copy(coverStyle = PlaylistCoverStyle.Signal)))
    }

    @Test
    fun `palette comes from the tracks when they carry accents`() {
        val tracks = listOf(
            track("a", accentStart = 0x112233, accentEnd = 0x445566),
            track("b", accentStart = 0x778899, accentEnd = 0)
        )
        val plan = buildPlaylistCoverPlan(PlaylistStudioEdits.startNew("x", tracks).copy(coverStyle = PlaylistCoverStyle.Signal))
        assertEquals(0xFF112233.toInt(), plan?.primaryColor)
        assertEquals(0xFF778899.toInt(), plan?.secondaryColor)
    }

    @Test
    fun `photo plans carry the crop only for the photo style`() {
        val photo = PlaylistStudioPhoto("content://p", 900, 1.5f, 10f, 20f)
        val draft = PlaylistStudioEdits.setPhoto(PlaylistStudioEdits.startNew("x"), photo)
        assertEquals(photo, buildPlaylistCoverPlan(draft)?.photo)
        assertNull(buildPlaylistCoverPlan(draft.copy(coverStyle = PlaylistCoverStyle.Signal))?.photo)
    }

    @Test
    fun `cover style names round trip`() {
        PlaylistCoverStyle.entries.forEach { assertEquals(it, PlaylistCoverStyle.from(it.name)) }
        assertEquals(PlaylistCoverStyle.Automatic, PlaylistCoverStyle.from("Holographic"))
        assertEquals(PlaylistCoverStyle.Automatic, PlaylistCoverStyle.from(null))
        assertTrue(PlaylistCoverStyle.Mosaic.rendered)
        assertFalse(PlaylistCoverStyle.Photo.rendered)
    }

    private fun playlist(tracks: List<Track>, coverMode: PlaylistCoverMode) = Playlist(
        id = "p1",
        name = "Road",
        coverUrl = "",
        tracks = tracks,
        createdAt = 1L,
        updatedAt = 2L,
        coverMode = coverMode
    )

    private fun track(
        id: String,
        title: String = "Title $id",
        artist: String = "Artist",
        album: String = "",
        durationMs: Long = 1_000L,
        artwork: String = "",
        accentStart: Int = 0,
        accentEnd: Int = 0
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = artwork,
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = accentStart,
        accentEnd = accentEnd
    )
}
