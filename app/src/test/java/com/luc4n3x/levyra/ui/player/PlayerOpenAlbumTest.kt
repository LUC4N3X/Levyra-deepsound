package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.feature.discovery.track
import com.luc4n3x.levyra.ui.trackAlbumHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOpenAlbumTest {
    private val base = track("Karma Police", "Radiohead")

    @Test
    fun albumWithBrowseIdOpensDirectly() {
        val current = base.copy(album = "OK Computer", albumBrowseId = "MPREb_okc")

        assertTrue(current.hasOpenableAlbum())
        val hit = trackAlbumHit(current)
        assertEquals("MPREb_okc", hit.browseId)
        assertEquals("OK Computer", hit.title)
        assertEquals("Radiohead", hit.artist)
    }

    @Test
    fun albumNameWithoutBrowseIdFallsBackToSearchQuery() {
        val current = base.copy(album = "OK Computer", albumBrowseId = "")

        assertTrue(current.hasOpenableAlbum())
        assertEquals("OK Computer Radiohead album", trackAlbumHit(current).query)
    }

    @Test
    fun missingOrPlaceholderAlbumHidesTheAction() {
        assertFalse(base.copy(album = "", albumBrowseId = "").hasOpenableAlbum())
        assertFalse(base.copy(album = "  ", albumBrowseId = "").hasOpenableAlbum())
        assertFalse(base.copy(album = "YouTube Music", albumBrowseId = "").hasOpenableAlbum())
        assertFalse(base.copy(album = "Unknown album", albumBrowseId = "").hasOpenableAlbum())
    }
}
