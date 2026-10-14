package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SpotifyMetadataIndexTest {
    private fun track(id: String = "abcdefghijk", title: String = "Numb", artist: String = "Linkin Park") = Track(
        id = id, title = title, artist = artist, album = "Meteora", durationMs = 187_000L,
        streamUrl = "https://audio.example.test/song", videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "https://i.ytimg.com/vi/$id/default.jpg", largeThumbnailUrl = "",
        source = "YouTube Music", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0,
        cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    @Test
    fun preservesPlaybackIdentityAndYoutubeCountersWhenEnrichingFields() {
        val original = track().copy(audioVideoId = "abcdefghijk", youtubeViewCount = 100L)
        val spotify = track("levyra-recording").copy(
            thumbnailUrl = "https://i.scdn.co/image/album", releaseDate = "2003-03-25", trackTotal = 13
        )
        val result = withSpotifyMetadata(original, SpotifyMetadataIndex(listOf(spotify)).match(original)!!)

        assertEquals(original.id, result.id)
        assertEquals(original.streamUrl, result.streamUrl)
        assertEquals(original.videoUrl, result.videoUrl)
        assertEquals(original.audioVideoId, result.audioVideoId)
        assertEquals(100L, result.youtubeViewCount)
        assertEquals(spotify.thumbnailUrl, result.thumbnailUrl)
        assertEquals(spotify.releaseDate, result.releaseDate)
        assertEquals(13, result.trackTotal)
        assertEquals("spotify", result.metadataProvider)
    }

    @Test
    fun missingSpotifyFieldsKeepExistingMetadata() {
        val original = track().copy(upc = "existing-upc", composer = "Existing composer")
        val spotify = track("levyra-recording").copy(thumbnailUrl = "", album = "")
        val result = withSpotifyMetadata(original, spotify)

        assertEquals(original.thumbnailUrl, result.thumbnailUrl)
        assertEquals(original.album, result.album)
        assertEquals(original.upc, result.upc)
        assertEquals(original.composer, result.composer)
    }

    @Test
    fun localFilesAndCustomArtworkArePreserved() {
        val spotify = track("levyra-recording")
        val local = track("local:recording").copy(source = "Offline", streamUrl = "content://media/1")
        val custom = track().copy(thumbnailUrl = "file:///custom/cover.jpg")

        assertNull(SpotifyMetadataIndex(listOf(spotify)).match(local))
        assertNull(SpotifyMetadataIndex(listOf(spotify)).match(custom))
        assertSame(local, withSpotifyMetadata(local, spotify))
        assertSame(custom, withSpotifyMetadata(custom, spotify))
    }

    @Test
    fun wrongRecordingIsNotUsedForArtwork() {
        val original = track()
        val live = track("levyra-live", "Numb (Live)")
        val homonym = track("levyra-other", artist = "Another Artist")

        assertNull(SpotifyMetadataIndex(listOf(live, homonym)).match(original))
        assertSame(original, withSpotifyMetadata(original, live))
    }

    @Test
    fun albumArtworkDoesNotConfuseStandardAndDeluxeEditions() {
        org.junit.Assert.assertTrue(sameSpotifyAlbum("Meteora", "Meteora"))
        org.junit.Assert.assertFalse(sameSpotifyAlbum("Meteora", "Meteora (Deluxe Edition)"))
        org.junit.Assert.assertFalse(sameSpotifyAlbum("Meteora", "Hybrid Theory"))
    }

    @Test
    fun sameRecordingOnAnotherAlbumCannotReplaceKnownAlbumArtwork() {
        val original = track().copy(isrc = "USWB10300001")
        val compilation = track("levyra-compilation").copy(album = "Greatest Hits", isrc = original.isrc)
        val deluxe = compilation.copy(id = "levyra-deluxe", album = "Meteora (Deluxe Edition)")
        assertNull(SpotifyMetadataIndex(listOf(compilation, deluxe)).match(original))
        assertSame(original, withSpotifyMetadata(original, compilation))
    }

    @Test
    fun explicitSpotifyRatingCannotEnrichKnownCleanRecording() {
        val clean = track(title = "Numb (Clean)")
        val explicit = track("spotify-recording").copy(explicit = true)
        assertEquals(0.0, SpotifyYouTubeMatcher.scoreMatch(explicit, clean), 0.0)
        assertNull(SpotifyMetadataIndex(listOf(explicit)).match(clean))
    }

    @Test
    fun reusedIsrcCannotOverrideConflictingArtistOrDuration() {
        val original = track().copy(isrc = "USWB10300001")
        val wrongArtist = original.copy(artist = "Another Artist")
        val differentRecording = original.copy(durationMs = original.durationMs + 60_000L)
        assertEquals(0.0, SpotifyYouTubeMatcher.scoreMatch(original, wrongArtist), 0.0)
        assertEquals(0.0, SpotifyYouTubeMatcher.scoreMatch(original, differentRecording), 0.0)
    }

    @Test
    fun knownSpotifyIsrcAllowsStrongYoutubeMatchWhenYoutubeHasNoIsrc() {
        val spotify = track("spotify-recording").copy(isrc = "USWB10300001")
        val youtube = track().copy(isrc = "")
        org.junit.Assert.assertNotNull(SpotifyYouTubeMatcher.findBestMatch(spotify, listOf(youtube)))
    }

    @Test
    fun searchRequiresAllQueryTokensAndBoundsResults() {
        val songs = (1..30).map { track("levyra-$it") }
        val index = SpotifyMetadataIndex(songs)

        assertEquals(12, index.search("Linkin Park numb", 12).size)
        assertEquals(25, index.search("Meteora", 100).size)
        assertEquals(0, index.search("numb unrelated", 12).size)
        assertEquals(0, index.search(" ", 12).size)
    }
}
