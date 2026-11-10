package com.luc4n3x.levyra.data

import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistSearchRefererTest {
    @Test
    fun nonLatinArtistNamesProduceAValidRefererHeader() {
        listOf("Нікіта Кісельов", "Beyoncé", "宇多田ヒカル", "Μαρινέλλα", "Sigur Rós & Björk").forEach { name ->
            val referer = artistSearchReferer(name)
            val headers = Headers.Builder().set("Referer", referer).build()
            assertEquals(referer, headers["Referer"])
        }
    }

    @Test
    fun keepsTheSearchQueryReadableBySpaceEncoding() {
        assertEquals("https://music.youtube.com/search?q=Pink+Floyd", artistSearchReferer("Pink Floyd"))
    }
}
