package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class JioSaavnLyricsTest {
    private val search = """{"results":[
        {"id":"aRZbUYD7","title":"Tum Hi Ho","type":"song","more_info":{"duration":"262","has_lyrics":"true","album":"Aashiqui 2",
            "artistMap":{"primary_artists":[{"name":"Mithoon"},{"name":"Arijit Singh"}]}}},
        {"id":"x","title":"No lyrics","type":"song","more_info":{"duration":"262","has_lyrics":"false"}},
        {"id":"y","title":"Album","type":"album","more_info":{"has_lyrics":"true"}}
    ]}"""

    @Test
    fun `keeps only songs that have lyrics`() {
        val songs = parseJioSaavnLyricsSongs(search)
        assertEquals(listOf("aRZbUYD7"), songs.map { it.id })
        assertEquals("Mithoon, Arijit Singh", songs.single().artist)
        assertEquals(262L, songs.single().durationSec)
        assertEquals(1, jioSaavnShortlist(songs, 260L).size)
        assertEquals(0, jioSaavnShortlist(songs, 200L).size)
    }

    @Test
    fun `converts html breaks and entities into plain lines`() {
        val result = parseJioSaavnLyrics("""{"lyrics":"Hum Tere Bin<br>Tere Bina Kya Wajood Mera<br><br>Tum &amp; Main &quot;yeh&quot;"}""")!!
        assertFalse(result.synced)
        assertEquals("JioSaavn", result.provider)
        val texts = result.lines.map { it.text }.filter(String::isNotBlank)
        assertEquals(listOf("Hum Tere Bin", "Tere Bina Kya Wajood Mera", "Tum & Main \"yeh\""), texts)
        assertNull(parseJioSaavnLyrics("""{"lyrics":""}"""))
    }
}
