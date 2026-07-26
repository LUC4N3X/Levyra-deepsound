package com.luc4n3x.levyra.desktop.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogMapperTest {

    @Test
    fun `playlist ids come from the list parameter`() {
        assertEquals(
            "PL123",
            CatalogMapper.collectionIdOf("https://www.youtube.com/playlist?list=PL123")
        )
        assertEquals(
            "PL123",
            CatalogMapper.collectionIdOf("https://www.youtube.com/watch?v=abc&list=PL123&index=2")
        )
    }

    @Test
    fun `channel ids come from the channel path`() {
        assertEquals(
            "UC123",
            CatalogMapper.collectionIdOf("https://www.youtube.com/channel/UC123")
        )
        assertEquals(
            "UC123",
            CatalogMapper.collectionIdOf("https://www.youtube.com/channel/UC123/videos")
        )
    }

    @Test
    fun `unknown urls are used as identifier`() {
        assertEquals("https://example.com", CatalogMapper.collectionIdOf("https://example.com"))
    }

    @Test
    fun `topic suffix is removed from artist names`() {
        assertEquals("Artista", CatalogMapper.cleanArtist("Artista - Topic"))
        assertEquals("Artista", CatalogMapper.cleanArtist("  Artista  "))
        assertEquals("", CatalogMapper.cleanArtist("   "))
    }
}
