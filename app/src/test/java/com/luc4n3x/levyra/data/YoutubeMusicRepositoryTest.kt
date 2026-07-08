package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YoutubeMusicRepositoryTest {
    @Test
    fun cleanAlbumDescriptionRemovesWikipediaAttribution() {
        val raw = """
            Mediterraneo è il terzo album in studio del cantautore italiano Bresh, pubblicato il 6 giugno 2025 dalla Epic.
            L'album contiene il singolo La tana del granchio.
            Da
            Wikipedia
            (https://it.wikipedia.org/wiki/Mediterraneo)
            soggetto a Creative Commons Attribution CC-BY-SA 3.0
            (https://creativecommons.org/licenses/by-sa/3.0/)
        """.trimIndent()

        val clean = raw.cleanAlbumDescription()

        assertEquals(
            "Mediterraneo è il terzo album in studio del cantautore italiano Bresh, pubblicato il 6 giugno 2025 dalla Epic. L'album contiene il singolo La tana del granchio.",
            clean
        )
        assertFalse(clean.contains("wikipedia", ignoreCase = true))
        assertFalse(clean.contains("creative commons", ignoreCase = true))
        assertFalse(clean.contains("http", ignoreCase = true))
    }

    @Test
    fun cleanAlbumDescriptionKeepsTextBeforeInlineAttribution() {
        val clean = "Un album caldo e luminoso. Da Wikipedia (https://example.com) CC-BY-SA 3.0"
            .cleanAlbumDescription()

        assertEquals("Un album caldo e luminoso.", clean)
    }
}
