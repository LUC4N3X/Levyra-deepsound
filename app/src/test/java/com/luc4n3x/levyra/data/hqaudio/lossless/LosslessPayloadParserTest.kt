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

    @Test
    fun monochromeKeepsArtistObjectsAndMillisecondDurationsFromTheProviderContract() {
        val body = """{"tracks":[
            {"id":"1","title":"Song","duration":200600,"artistNames":[null,""],
             "artists":[{"name":"Artist"},{"displayName":"Guest"}],"release":{"title":"Album"},"explicit":null},
            {"id":"2","title":"Song","duration":1200000,"artistNames":["Artist"],
             "artists":[{"name":"Other Artist"}],"albumTitle":"Preferred Album","release":{"title":"Ignored"},"explicit":false}
        ]}"""

        val candidates = MonochromePayloadParser.searchPage(body)!!

        assertEquals(2, candidates.size)
        assertEquals(listOf(201, 1200), candidates.map { it.durationSeconds })
        assertEquals(listOf("Artist"), candidates[0].primaryArtists)
        assertEquals(listOf("Guest"), candidates[0].featuredArtists)
        assertEquals("Album", candidates[0].album)
        assertNull(candidates[0].explicit)
        assertEquals(listOf("Artist"), candidates[1].primaryArtists)
        assertEquals("Preferred Album", candidates[1].album)
        assertEquals(false, candidates[1].explicit)
    }

    @Test
    fun monochromeRejectsUnusableDurationsAndMissingArtists() {
        val body = """{"tracks":[
            {"id":"1","title":"Song","duration":0,"artistNames":["Artist"]},
            {"id":"2","title":"Song","duration":-1,"artistNames":["Artist"]},
            {"id":"3","title":"Song","duration":200,"artistNames":[null],"artists":[{"name":null}]},
            {"id":"4","title":"Song","duration":86401000,"artistNames":["Artist"]}
        ]}"""

        assertTrue(MonochromePayloadParser.searchPage(body)!!.isEmpty())
    }
}
