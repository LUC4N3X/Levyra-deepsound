package com.luc4n3x.levyra.data.hqaudio.lossless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LosslessPayloadParserTest {
    @Test
    fun monochromeSearchKeepsPlayableTracksWithIdentity() {
        val body = """
            {"tracks":[
              {"id":"158460820404711424","trackId":"158460820404711424","title":"Billie Jean",
               "artistNames":["Michael Jackson"],"duration":294230,"isrc":"ussm19902991","explicit":false,"playable":true},
              {"id":"155142501534011392","title":"Get Lucky (feat. Pharrell Williams and Nile Rodgers)",
               "artistNames":["Daft Punk","Pharrell Williams","Nile Rodgers"],"duration":369630,"playable":true},
              {"id":"1","title":"Blocked","artistNames":["Someone"],"duration":1000,"playable":false},
              {"id":"../track","title":"Injected","artistNames":["Someone"],"duration":1000}
            ]}
        """.trimIndent()
        val candidates = MonochromePayloadParser.searchPage(body)!!
        assertEquals(listOf("158460820404711424", "155142501534011392"), candidates.map { it.providerTrackId })
        val billie = candidates.first()
        assertEquals("USSM19902991", billie.isrc)
        assertEquals(294, billie.durationSeconds)
        assertEquals(listOf("Michael Jackson"), billie.primaryArtists)
        assertEquals(listOf("Pharrell Williams", "Nile Rodgers"), candidates[1].featuredArtists)
    }

    @Test
    fun monochromeMalformedPayloadIsNotAnEmptyResult() {
        assertNull(MonochromePayloadParser.searchPage("<html>"))
        assertNull(MonochromePayloadParser.searchPage("""{"error":"beep boop"}"""))
    }

    @Test
    fun nullOptionalMetadataDoesNotBecomeAnIdentity() {
        val body = """{"tracks":[{"id":"1","trackId":null,"title":"Track","artistNames":["Artist"],
            "duration":200000,"isrc":null,"albumTitle":null,"releaseTitle":"Album"}]}"""

        val parsed = MonochromePayloadParser.searchPage(body)!!.single()

        assertEquals("1", parsed.providerTrackId)
        assertEquals("", parsed.isrc)
        assertEquals("Album", parsed.album)
    }

    @Test
    fun trackIdsAreNumericOnly() {
        assertTrue(MonochromePayloadParser.isTrackId("189121220435181568"))
        assertFalse(MonochromePayloadParser.isTrackId("1/../admin"))
    }
}
