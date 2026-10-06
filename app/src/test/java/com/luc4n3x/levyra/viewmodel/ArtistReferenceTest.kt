package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistReferenceTest {

    @Test
    fun `track with artist browse id resolves to that exact artist`() {
        val reference = artistReferenceOf(track(artist = "Queen", browseIds = listOf(" UCiMhD4jzUqG-IgPzUmmytRQ ")))

        assertEquals(ArtistReference(name = "Queen", browseId = "UCiMhD4jzUqG-IgPzUmmytRQ"), reference)
    }

    @Test
    fun `track without browse id falls back to the artist name lookup`() {
        val reference = artistReferenceOf(track(artist = "Queen"))

        assertEquals(ArtistReference(name = "Queen", browseId = ""), reference)
    }

    @Test
    fun `multiple artists resolve to the primary credited artist and its browse id`() {
        val reference = artistReferenceOf(
            track(artist = "Luis Fonsi, Daddy Yankee", browseIds = listOf("UCfonsi", "UCyankee"))
        )

        assertEquals(ArtistReference(name = "Luis Fonsi", browseId = "UCfonsi"), reference)
    }

    @Test
    fun `secondary credited artist resolves by index`() {
        val track = track(
            artist = "Luis Fonsi, Daddy Yankee",
            browseIds = listOf("UCfonsi", "UCyankee")
        )

        assertEquals(
            ArtistReference(name = "Daddy Yankee", browseId = "UCyankee"),
            artistReferenceOf(track, 1)
        )
        assertNull(artistReferenceOf(track, 2))
    }


    @Test
    fun `invalid earlier credit does not shift later artist index`() {
        val track = track(
            artist = "X, Queen",
            browseIds = listOf("UCinvalid", "UCqueen")
        )

        assertNull(artistReferenceOf(track, 0))
        assertEquals(
            ArtistReference(name = "Queen", browseId = "UCqueen"),
            artistReferenceOf(track, 1)
        )
    }

    @Test
    fun `featuring credit resolves to the main artist`() {
        assertEquals("Dua Lipa", artistReferenceOf(track(artist = "Dua Lipa feat. DaBaby"))?.name)
    }

    @Test
    fun `missing or placeholder artist metadata never produces a destination`() {
        assertNull(artistReferenceOf(track(artist = "")))
        assertNull(artistReferenceOf(track(artist = "   ")))
        assertNull(artistReferenceOf(track(artist = "X")))
        assertNull(artistReferenceOf(track(artist = "YouTube")))
        assertNull(artistReferenceOf(track(artist = "YouTube Music", browseIds = listOf("UCyoutube"))))
    }

    @Test
    fun `live radio station metadata is never treated as an artist`() {
        val station = track(id = "live-radio:7a1c", artist = "Germany", source = "Live Radio")

        assertNull(artistReferenceOf(station))
    }

    @Test
    fun `local track keeps its tagged artist for name lookup`() {
        val local = track(artist = "Local Band", source = "Offline", streamUrl = "content://media/external/audio/media/7")

        assertEquals(ArtistReference(name = "Local Band", browseId = ""), artistReferenceOf(local))
    }

    @Test
    fun `navigable artist name rejects blanks, single characters and platform names`() {
        assertEquals(false, isNavigableArtistName(" "))
        assertEquals(false, isNavigableArtistName("a"))
        assertEquals(false, isNavigableArtistName("youtube music"))
        assertEquals(true, isNavigableArtistName("U2"))
    }

    private fun track(
        id: String = "track-1",
        artist: String,
        browseIds: List<String> = emptyList(),
        source: String = "YouTube Music",
        streamUrl: String = ""
    ) = Track(
        id = id,
        title = "Title",
        artist = artist,
        album = "Album",
        durationMs = 1_000L,
        streamUrl = streamUrl,
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = source,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        artistBrowseIds = browseIds
    )
}
