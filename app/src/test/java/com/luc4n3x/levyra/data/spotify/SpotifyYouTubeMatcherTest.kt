package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.data.mergeSpotifyAndYoutubeSongs
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyYouTubeMatcherTest {

    private fun sampleTrack(
        id: String,
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 180_000L,
        viewCount: Long = 0L,
        isrc: String = ""
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = if (id.startsWith("sp:")) "" else "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = if (id.startsWith("sp:")) "spotify" else "youtube",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0,
        youtubeViewCount = viewCount,
        isrc = isrc,
        metadataProvider = if (id.startsWith("sp:")) "spotify" else "youtube"
    )

    @Test
    fun exactMatchYieldsHighConfidence() {
        val spotifyTrack = sampleTrack("sp:1", "Blinding Lights", "The Weeknd", durationMs = 200_000L)
        val ytTrack = sampleTrack("yt:1", "Blinding Lights", "The Weeknd", durationMs = 202_000L, viewCount = 3_000_000_000L)

        val confidence = SpotifyYouTubeMatcher.scoreMatch(spotifyTrack, ytTrack)
        assertTrue("Confidence must be >= 0.85, was $confidence", confidence >= 0.85)
    }

    @Test
    fun featuredArtistInTitleMatchesArtistList() {
        val spotifyTrack = sampleTrack(
            "sp:2",
            "Chicago Freestyle",
            "Drake, Giveon",
            durationMs = 220_000L
        )
        val ytTrack = sampleTrack(
            "yt:2",
            "Chicago Freestyle (feat. Giveon)",
            "Drake",
            durationMs = 221_000L,
            viewCount = 362_000_000L
        )

        val confidence = SpotifyYouTubeMatcher.scoreMatch(spotifyTrack, ytTrack)
        assertTrue("Featured artist match confidence should be >= 0.70, was $confidence", confidence >= 0.70)
    }

    @Test
    fun unrequestedLiveOrRemixIsPenalized() {
        val originalSpotify = sampleTrack("sp:3", "Numb", "Linkin Park", durationMs = 187_000L)
        val studioYt = sampleTrack("yt:3a", "Numb", "Linkin Park", durationMs = 187_000L, viewCount = 2_000_000_000L)
        val liveYt = sampleTrack("yt:3b", "Numb (Live in Texas)", "Linkin Park", durationMs = 205_000L, viewCount = 50_000_000L)
        val remixYt = sampleTrack("yt:3c", "Numb (Sped Up Remix)", "Linkin Park", durationMs = 140_000L, viewCount = 10_000_000L)

        val studioScore = SpotifyYouTubeMatcher.scoreMatch(originalSpotify, studioYt)
        val liveScore = SpotifyYouTubeMatcher.scoreMatch(originalSpotify, liveYt)
        val remixScore = SpotifyYouTubeMatcher.scoreMatch(originalSpotify, remixYt)

        assertTrue(studioScore > liveScore)
        assertTrue(studioScore > remixScore)
        assertTrue("Live penalty should demote below 0.60, was $liveScore", liveScore < 0.60)
        assertTrue("Remix penalty should demote below 0.60, was $remixScore", remixScore < 0.60)
    }

    @Test
    fun explicitlyRequestedRemixMatchesRemixCandidate() {
        val remixSpotify = sampleTrack("sp:4", "Levitating (The Blessed Madonna Remix)", "Dua Lipa", durationMs = 250_000L)
        val remixYt = sampleTrack("yt:4", "Levitating (The Blessed Madonna Remix)", "Dua Lipa", durationMs = 251_000L)

        val confidence = SpotifyYouTubeMatcher.scoreMatch(remixSpotify, remixYt)
        assertTrue("Requested remix should match confidently, was $confidence", confidence >= 0.75)
    }

    @Test
    fun durationDeltaHeavilyPenalizesMismatch() {
        val spotifyTrack = sampleTrack("sp:5", "Midnight City", "M83", durationMs = 243_000L)
        val closeYt = sampleTrack("yt:5a", "Midnight City", "M83", durationMs = 244_000L)
        val longYt = sampleTrack("yt:5b", "Midnight City", "M83", durationMs = 450_000L)

        val closeScore = SpotifyYouTubeMatcher.scoreMatch(spotifyTrack, closeYt)
        val longScore = SpotifyYouTubeMatcher.scoreMatch(spotifyTrack, longYt)

        assertTrue(closeScore > longScore)
        assertTrue("Score for duration mismatch of >200s should be penalized, was $longScore", longScore < 0.60)
    }

    @Test
    fun identicalIsrcGuaranteesHighestConfidence() {
        val spotifyTrack = sampleTrack("sp:6", "Save Your Tears", "The Weeknd", isrc = "USUG12000658")
        val ytTrack = sampleTrack("yt:6", "Save Your Tears", "The Weeknd", isrc = "USUG12000658", viewCount = 1_200_000_000L)

        val confidence = SpotifyYouTubeMatcher.scoreMatch(spotifyTrack, ytTrack)
        assertTrue("Same ISRC should give max confidence >= 0.95, was $confidence", confidence >= 0.95)
    }

    @Test
    fun homonymDifferentArtistIsRejected() {
        val spotifyAdele = sampleTrack("sp:7", "Hello", "Adele")
        val ytLionel = sampleTrack("yt:7", "Hello", "Lionel Richie")

        val confidence = SpotifyYouTubeMatcher.scoreMatch(spotifyAdele, ytLionel)
        assertTrue("Different artists with same title must be rejected (< 0.50), was $confidence", confidence < 0.50)
    }

    @Test
    fun mergeSpotifyAndYoutubePreservesViewCountAndMetadata() {
        val spotifyTracks = listOf(
            sampleTrack("sp:drake", "Chicago Freestyle", "Drake, Giveon", album = "Dark Lane Demo Tapes", durationMs = 220_000L),
            sampleTrack("sp:weeknd", "Blinding Lights", "The Weeknd", album = "After Hours", durationMs = 200_000L)
        )

        val youtubeTracks = listOf(
            sampleTrack("ytdrake0001", "Chicago Freestyle (feat. Giveon)", "Drake", durationMs = 221_000L, viewCount = 362_000_000L),
            sampleTrack("ytvital0001", "Drake - Vital (Unreleased)", "Drake", durationMs = 195_000L, viewCount = 5_000_000L)
        )

        val merged = mergeSpotifyAndYoutubeSongs(spotifyTracks, youtubeTracks)

        assertEquals(3, merged.size)

        val chicago = merged.first { it.title == "Chicago Freestyle" }
        assertEquals(362_000_000L, chicago.youtubeViewCount)
        assertEquals("ytdrake0001", chicago.id)
        assertEquals("Dark Lane Demo Tapes", chicago.album)

        val unreleased = merged.first { it.title.contains("Vital") }
        assertEquals("ytvital0001", unreleased.id)
        assertEquals(5_000_000L, unreleased.youtubeViewCount)
    }

    @Test
    fun bestMatchSelectionPicksTrueTrackOverCoversAndKaraoke() {
        val spotifyTrack = sampleTrack("sp:10", "Someone Like You", "Adele", durationMs = 285_000L)
        val candidates = listOf(
            sampleTrack("ytcov000001", "Someone Like You", "Pop Cover Band", durationMs = 285_000L),
            sampleTrack("ytkar000001", "Someone Like You (Karaoke Version)", "Karaoke Hits", durationMs = 285_000L),
            sampleTrack("ytorig00001", "Someone Like You (Official Music Video)", "Adele", durationMs = 284_000L, viewCount = 2_100_000_000L)
        )

        val best = SpotifyYouTubeMatcher.findBestMatch(spotifyTrack, candidates, threshold = 0.55)
        assertNotNull("Must find a match", best)
        assertEquals("ytorig00001", best?.candidate?.id)
    }

    @Test
    fun spotifyEnrichmentKeepsExistingYoutubeOrderStable() {
        val youtubeMatch = sampleTrack(
            "ytorder0001",
            "Blinding Lights",
            "The Weeknd",
            durationMs = 200_000L,
            viewCount = 3_000_000_000L
        )
        val youtubeRarity = sampleTrack(
            "ytrarity001",
            "Blinding Lights Live",
            "The Weeknd",
            durationMs = 245_000L,
            viewCount = 12_000_000L
        )
        val spotify = sampleTrack(
            "sp:order",
            "Blinding Lights",
            "The Weeknd",
            album = "After Hours",
            durationMs = 200_000L
        )

        val merged = mergeSpotifyAndYoutubeSongs(
            existing = listOf(youtubeMatch, youtubeRarity),
            incoming = listOf(spotify)
        )

        assertEquals(listOf("ytorder0001", "ytrarity001"), merged.map { it.id })
        assertEquals("spotify", merged.first().metadataProvider)
        assertEquals("After Hours", merged.first().album)
    }

    @Test
    fun localTrackIsNeverUsedAsSpotifyYoutubeCounterpart() {
        val local = sampleTrack(
            "local:hello",
            "Hello",
            "Adele",
            durationMs = 295_000L
        ).copy(
            streamUrl = "content://media/external/audio/1",
            videoUrl = "",
            source = "Offline",
            metadataProvider = "spotify"
        )
        val youtube = sampleTrack(
            "ytlocal0001",
            "Hello",
            "Adele",
            durationMs = 295_000L,
            viewCount = 4_000_000_000L
        )
        val spotify = sampleTrack(
            "sp:local",
            "Hello",
            "Adele",
            album = "25",
            durationMs = 295_000L
        )

        val merged = mergeSpotifyAndYoutubeSongs(
            existing = listOf(local, youtube),
            incoming = listOf(spotify)
        )

        assertEquals("local:hello", merged.first().id)
        assertEquals("Offline", merged.first().source)
        assertEquals("ytlocal0001", merged[1].id)
        assertEquals("spotify", merged[1].metadataProvider)
        assertEquals(2, merged.size)
    }
}
