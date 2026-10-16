package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseRadarNavigationTest {
    @Test
    fun `identifiable release opens its album page with real ids`() {
        val entry = entry(browseId = " MPREb_radar01 ")

        val album = releaseRadarAlbumHit(entry)!!

        assertEquals("MPREb_radar01", album.browseId)
        assertEquals("UC_artist", album.artistBrowseId)
        assertEquals("Night Signal", album.title)
        assertEquals("Aurora Lane", album.artist)
        assertEquals("2026", album.year)
        assertEquals("https://img.example/cover.jpg", album.thumbnailUrl)
        assertEquals("Aurora Lane Night Signal", album.query)
    }

    @Test
    fun `missing artist name falls back to release subtitle`() {
        val album = releaseRadarAlbumHit(entry(browseId = "MPREb_radar02", artistName = " "))!!

        assertEquals("Single • Aurora Lane", album.artist)
    }

    @Test
    fun `release without browse id is not matched by guesswork`() {
        val entry = entry(browseId = "  ")

        assertNull(releaseRadarAlbumHit(entry))
        assertEquals("Night Signal Aurora Lane", releaseRadarSearchQuery(entry))
    }

    private fun entry(browseId: String, artistName: String = "Aurora Lane") = ReleaseRadarEntry(
        artistName = artistName,
        artistBrowseId = "UC_artist",
        release = ArtistRelease(
            browseId = browseId,
            title = "Night Signal",
            subtitle = "Single • Aurora Lane",
            thumbnailUrl = "https://img.example/cover.jpg",
            year = "2026"
        ),
        isFresh = true
    )
}
