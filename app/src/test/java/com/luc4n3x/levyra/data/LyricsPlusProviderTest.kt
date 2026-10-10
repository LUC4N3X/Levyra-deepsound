package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricVocalRole
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsPlusProviderTest {
    private val provider = LyricsPlusProvider(OkHttpClient())

    @Test
    fun parsesWordSyncedMainBackgroundTranslationAndRomanization() {
        val payload = """
            {
              "type": "Word",
              "metadata": {
                "title": "Da Dio",
                "totalDuration": "03:10"
              },
              "lyrics": [
                {
                  "time": 1000,
                  "duration": 1600,
                  "text": "Non ci credevo",
                  "element": { "singer": "v1" },
                  "translation": { "lang": "en", "text": "I did not believe it" },
                  "transliteration": { "lang": "latn", "text": "Non ci credevo" },
                  "syllabus": [
                    { "time": 1000, "duration": 400, "text": "Non ", "isBackground": false },
                    { "time": 1400, "duration": 500, "text": "ci ", "isBackground": false },
                    { "time": 1900, "duration": 700, "text": "credevo", "isBackground": false },
                    { "time": 1500, "duration": 600, "text": "davvero", "isBackground": true }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = provider.parseMirrorPayload(payload, "LyricsPlus", "Fallback", "Bresh", 190L)

        assertNotNull(result)
        requireNotNull(result)
        assertTrue(result.synced)
        assertEquals(96, result.confidence)
        assertEquals("Da Dio", result.title)
        assertEquals(190L, result.durationSec)
        assertEquals(2, result.lines.size)
        val main = result.lines.first { it.role == LyricVocalRole.MAIN }
        val background = result.lines.first { it.role == LyricVocalRole.BACKGROUND }
        assertEquals("Non ci credevo", main.text)
        assertEquals("I did not believe it", main.translated)
        assertEquals(3, main.words.size)
        assertEquals("davvero", background.text)
        assertEquals(1, background.words.size)
    }

    @Test
    fun parsesPlainLyricsWithoutPretendingTheyAreSynced() {
        val payload = """
            {
              "type": "None",
              "lyrics": [
                { "text": "Prima riga" },
                { "text": "Seconda riga" }
              ]
            }
        """.trimIndent()

        val result = provider.parseMirrorPayload(payload, "LyricsPlus", "Titolo", "Artista", 0L)

        assertNotNull(result)
        requireNotNull(result)
        assertFalse(result.synced)
        assertEquals(2, result.lines.size)
        assertEquals("Prima riga", result.lines.first().text)
    }

    @Test
    fun assignsAlternatingDuetRolesWhenMultipleSingersExist() {
        val payload = """
            {
              "type": "Line",
              "lyrics": [
                { "time": 1000, "duration": 1000, "text": "Voce uno", "element": { "singer": "v1" } },
                { "time": 2000, "duration": 1000, "text": "Voce due", "element": { "singer": "v2" } }
              ]
            }
        """.trimIndent()

        val result = provider.parseMirrorPayload(payload, "LyricsPlus", "Titolo", "Artista", 0L)

        assertNotNull(result)
        requireNotNull(result)
        assertEquals(LyricVocalRole.DUET_LEFT, result.lines[0].role)
        assertEquals(LyricVocalRole.DUET_RIGHT, result.lines[1].role)
    }
}
