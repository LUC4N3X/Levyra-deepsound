package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetEaseLyricsTest {
    private val search = """{"result":{"songs":[
        {"id":3410510807,"name":"ЗНАК","duration":216000,"artists":[{"name":"Toxi${'$'}"},{"name":"Дора"}],"album":{"name":"ЗНАК"}},
        {"id":2,"name":"ЗНАК (Remix)","duration":150000,"artists":[{"name":"Other"}],"album":{"name":""}},
        {"id":0,"name":"broken","duration":1000,"artists":[]}
    ]},"code":200}"""

    @Test
    fun `parses search results with joined artists`() {
        val songs = parseNetEaseSongs(search)
        assertEquals(2, songs.size)
        assertEquals("Toxi$, Дора", songs.first().artist)
        assertEquals(216L, songs.first().durationSec)
    }

    @Test
    fun `shortlist keeps only plausible durations`() {
        val songs = parseNetEaseSongs(search)
        assertEquals(listOf(3410510807L), netEaseShortlist(songs, 214L).map { it.id })
        assertEquals(2, netEaseShortlist(songs, 0L).size)
    }

    @Test
    fun `synced lyrics carry chinese translation only for chinese readers`() {
        val body = """{"lrc":{"lyric":"[00:00.000] 作词 : A\n[00:26.30]Шо ж це я\n[00:30.59]Зупинитися вчасно\n"},
            "tlyric":{"lyric":"[00:26.30]为何我\n[00:30.59]及时停下脚步\n"},"code":200}"""
        val chinese = parseNetEaseLyrics(body, includeTranslation = true)!!
        assertTrue(chinese.synced)
        assertEquals("NetEase", chinese.provider)
        assertEquals("为何我", chinese.lines.first { it.text == "Шо ж це я" }.translated)
        val russian = parseNetEaseLyrics(body, includeTranslation = false)!!
        assertTrue(russian.lines.all { it.translated.isBlank() })
    }

    @Test
    fun `missing or instrumental lyrics yield nothing`() {
        assertNull(parseNetEaseLyrics("""{"nolyric":true,"code":200}""", false))
        assertNull(parseNetEaseLyrics("""{"uncollected":true,"code":200}""", false))
        assertNull(parseNetEaseLyrics("""{"lrc":{"lyric":"[00:00.00]纯音乐，请欣赏\n"},"code":200}""", false))
    }
}
