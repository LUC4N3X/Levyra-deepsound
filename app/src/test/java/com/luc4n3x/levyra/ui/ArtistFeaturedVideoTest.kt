package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistFeaturedVideoTest {

    @Test
    fun realVideoIsPreferredOverTopSong() {
        val profile = profile(
            topSongs = listOf(track("song-1")),
            videos = listOf(track("video-1"))
        )

        assertEquals("video-1", selectArtistFeaturedVideo(profile)?.id)
    }

    @Test
    fun topSongStandsInWhenTheArtistHasNoVideo() {
        val profile = profile(topSongs = listOf(track("song-1")), videos = emptyList())

        assertEquals("song-1", selectArtistFeaturedVideo(profile)?.id)
    }

    @Test
    fun noPreviewWithoutVideosOrSongs() {
        assertNull(selectArtistFeaturedVideo(profile(topSongs = emptyList(), videos = emptyList())))
    }

    private fun profile(topSongs: List<Track>, videos: List<Track>) = ArtistProfile(
        browseId = "UC-artist",
        name = "Artist",
        subscribers = "",
        monthlyListeners = "",
        thumbnailUrl = "",
        bannerUrl = "",
        topSongs = topSongs,
        albums = emptyList(),
        singles = emptyList(),
        accentStart = 0,
        accentEnd = 0,
        videos = videos
    )

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "Artist",
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
