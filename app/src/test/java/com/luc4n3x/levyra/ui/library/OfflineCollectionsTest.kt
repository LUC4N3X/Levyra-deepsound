package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCollectionsTest {
    private val songA = track("a", "Alpha", "Queen")
    private val songB = track("b", "Bravo", "Queen feat. David Bowie")
    private val songC = track("c", "Charlie", "Muse")
    private val downloads = listOf(
        download(1L, songA, savedAt = 10L),
        download(2L, songB, savedAt = 30L),
        download(3L, songC, savedAt = 20L, ownership = DownloadOwnership.SMART_OFFLINE)
    )
    private val items = downloads.map { LibraryOfflineItem("download:${it.id}", it.toLibraryTrack(), it) }

    @Test
    fun noDownloadsMeansNoCollections() {
        assertEquals(OfflineCollections.Empty, buildOfflineCollections(emptyList(), emptyList(), listOf(songA), listOf(songA)))
    }

    @Test
    fun artistsGroupByLeadArtistAndSortBySize() {
        val artists = buildOfflineCollections(items, emptyList(), emptyList(), emptyList()).artists
        assertEquals(listOf("Queen", "Muse"), artists.map { it.title })
        assertEquals(listOf("download:1", "download:2"), artists.first().itemKeys)
    }

    @Test
    fun mixesCoverRecentFavoritesMostPlayedAndSmartOffline() {
        val mixes = buildOfflineCollections(items, emptyList(), favorites = listOf(songC), mostPlayed = listOf(songB, songA)).mixes
        val byMix = mixes.associateBy { it.mix }
        assertEquals(listOf("download:2", "download:3", "download:1"), byMix.getValue(OfflineMix.Recent).itemKeys)
        assertEquals(listOf("download:3"), byMix.getValue(OfflineMix.Favorites).itemKeys)
        assertEquals(listOf("download:2", "download:1"), byMix.getValue(OfflineMix.MostPlayed).itemKeys)
        assertEquals(listOf("download:3"), byMix.getValue(OfflineMix.SmartOffline).itemKeys)
    }

    @Test
    fun emptyMixesAreLeftOut() {
        val mixes = buildOfflineCollections(items.take(1), emptyList(), emptyList(), emptyList()).mixes
        assertEquals(listOf(OfflineMix.Recent), mixes.map { it.mix })
    }

    @Test
    fun playlistsKeepOnlyDownloadedTracksMatchedByIdOrText() {
        val renamedId = track("other-id", "Charlie", "Muse")
        val playlists = listOf(
            playlist("p1", "Road trip", listOf(songB, track("missing", "Nope", "Nobody"), renamedId)),
            playlist("p2", "Nothing offline", listOf(track("x", "X", "Y"))),
            playlist("p3", "Hidden", listOf(songA), hidden = true)
        )
        val result = buildOfflineCollections(items, playlists, emptyList(), emptyList()).playlists
        assertEquals(listOf("Road trip"), result.map { it.title })
        assertEquals(listOf("download:2", "download:3"), result.single().itemKeys)
    }

    @Test
    fun textMatchSkipsRecordingsWithAnotherLength() {
        val live = track("live", "Alpha", "Queen").copy(durationMs = 420_000L)
        val withLive = listOf(download(9L, live, savedAt = 5L)) + downloads
        val liveItems = withLive.map { LibraryOfflineItem("download:${it.id}", it.toLibraryTrack(), it) }
        val studioFavorite = track("studio-elsewhere", "Alpha", "Queen")
        val favorites = buildOfflineCollections(liveItems, emptyList(), listOf(studioFavorite), emptyList())
            .mixes.single { it.mix == OfflineMix.Favorites }
        assertEquals(listOf("download:1"), favorites.itemKeys)
    }

    @Test
    fun sameArtistSpelledTwiceIsOneRail() {
        val shouting = download(8L, track("q", "Delta", "QUEEN"), savedAt = 1L)
        val mixed = (downloads + shouting).map { LibraryOfflineItem("download:${it.id}", it.toLibraryTrack(), it) }
        val artists = buildOfflineCollections(mixed, emptyList(), emptyList(), emptyList()).artists
        assertEquals(artists.map { it.key }.distinct(), artists.map { it.key })
        assertEquals(3, artists.first { it.key == "artist:queen" }.itemKeys.size)
    }

    @Test
    fun findReturnsAnyCollectionByKey() {
        val collections = buildOfflineCollections(items, emptyList(), emptyList(), emptyList())
        assertTrue(collections.find("mix:Recent")?.mix == OfflineMix.Recent)
        assertEquals("Muse", collections.find("artist:muse")?.title)
        assertNull(collections.find(null))
        assertNull(collections.find("artist:unknown"))
    }

    private fun download(id: Long, track: Track, savedAt: Long, ownership: DownloadOwnership = DownloadOwnership.MANUAL) = DownloadedTrack(
        id = id,
        trackId = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationMs = track.durationMs,
        fileName = "${track.id}.m4a",
        uri = "content://${track.id}",
        mimeType = "audio/mp4",
        embeddedMetadata = true,
        savedAt = savedAt,
        ownership = ownership
    )

    private fun playlist(id: String, name: String, tracks: List<Track>, hidden: Boolean = false) = Playlist(
        id = id,
        name = name,
        coverUrl = "",
        tracks = tracks,
        createdAt = 0L,
        updatedAt = 0L,
        hidden = hidden
    )

    private fun track(id: String, title: String, artist: String) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
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
}
