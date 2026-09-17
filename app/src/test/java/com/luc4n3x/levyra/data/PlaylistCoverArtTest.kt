package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistCoverArtTest {

    @Test
    fun `mosaic tiles cover the canvas without gaps or overlaps`() {
        (0..6).forEach { count ->
            val tiles = playlistMosaicTiles(count, 1000f, 1000f)
            assertEquals(count.coerceAtMost(4), tiles.size)
            val area = tiles.sumOf { ((it.right - it.left) * (it.bottom - it.top)).toDouble() }
            val expected = if (count == 0) 0.0 else 1_000_000.0
            assertEquals("count $count", expected, area, 0.5)
            tiles.forEach { tile ->
                assertTrue(tile.left >= 0f && tile.top >= 0f && tile.right <= 1000f && tile.bottom <= 1000f)
            }
        }
    }

    @Test
    fun `three tiles give the first artwork the tall half`() {
        val tiles = playlistMosaicTiles(3, 800f, 800f)
        assertEquals(PlaylistCoverTile(0f, 0f, 400f, 800f), tiles[0])
        assertEquals(PlaylistCoverTile(400f, 400f, 800f, 800f), tiles[2])
    }

    @Test
    fun `cover titles shrink as they grow`() {
        val width = 1024f
        val short = titleTextSize("Gym", width)
        val medium = titleTextSize("Sunday Morning Coffee", width)
        val long = titleTextSize("Songs for the drive back home", width)
        val huge = titleTextSize("x".repeat(90), width)
        assertTrue(short > medium && medium > long && long > huge)
        assertEquals(titleTextSize("  Gym  ", width), short, 0f)
        assertTrue(huge >= width * 0.06f)
    }
}
