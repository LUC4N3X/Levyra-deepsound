package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricsTranslationState
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OnlineLyricsTextTest {
    @Test
    fun `joins translated segments and splits them back into lines`() {
        val response = JSONArray(
            """[[["I want\n","Ich will\n",null,null,3],["We are\n","Wir sind\n",null,null,3],["You asked me. ","Du hast mich gefragt. ",null,null,3],["And I said nothing","Und ich hab nichts gesagt",null,null,3]],null,"de"]"""
        )
        val joined = OnlineLyricsTextService.joinedTranslation(response)
        assertEquals(
            listOf("I want", "We are", "You asked me. And I said nothing"),
            OnlineLyricsTextService.splitLines(joined, 3)
        )
    }

    @Test
    fun `line count mismatch drops the chunk instead of misaligning lines`() {
        assertEquals(listOf(null, null), OnlineLyricsTextService.splitLines("one\ntwo\nthree", 2))
    }

    @Test
    fun `reads the source transliteration of japanese text`() {
        val response = JSONArray("""[[[null,null,null,"Dōshite ### Mata au hi made"]],null,"ja"]""")
        assertEquals("Dōshite ### Mata au hi made", OnlineLyricsTextService.sourceTransliteration(response))
        assertNull(OnlineLyricsTextService.sourceTransliteration(JSONArray("""[null,null,"en"]""")))
    }

    @Test
    fun `cleans google readings into readable romaji`() {
        assertEquals("issono koto kirihanase ta hō ga raku nanoni na", OnlineLyricsTextService.cleanReading("I~tsu Sono Koto Kirihanase Ta Hō Ga Raku Nanoni Na"))
        assertEquals("shikaku shingō, hassha! mata au hi made", OnlineLyricsTextService.cleanReading(" Shikaku Shingō , Hassha ! Mata Au Hi Made "))
        assertEquals("dōshite dōshite watashi dake?", OnlineLyricsTextService.cleanReading("Dōshite Dōshite Watashi Dake ?"))
    }

    @Test
    fun `maps app languages to endpoint codes`() {
        assertEquals("ru", OnlineLyricsTextService.onlineLanguageCode("ru"))
        assertEquals("zh-CN", OnlineLyricsTextService.onlineLanguageCode("zh"))
        assertEquals("zh-TW", OnlineLyricsTextService.onlineLanguageCode("zh-Hant"))
        assertEquals("pt", OnlineLyricsTextService.onlineLanguageCode("pt-BR"))
    }

    @Test
    fun `falls back online only when the device cannot translate`() = runBlocking {
        val online = FixedBackend(LyricsBatchTranslation.Success(listOf("a"), LyricsTranslationState.ONLINE))
        val onDevice = LyricsBatchTranslation.Success(listOf("b"))
        assertSame(onDevice, FallbackLyricsTranslationBackend(FixedBackend(onDevice), online).translate(listOf("x"), "en"))
        assertEquals(
            LyricsTranslationState.ONLINE,
            (FallbackLyricsTranslationBackend(FixedBackend(LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)), online)
                .translate(listOf("x"), "en") as LyricsBatchTranslation.Success).state
        )
        assertEquals(
            LyricsBatchTranslation.SameLanguage,
            FallbackLyricsTranslationBackend(FixedBackend(LyricsBatchTranslation.SameLanguage), online).translate(listOf("x"), "en")
        )
        assertEquals(
            LyricsTranslationState.ONLINE,
            (FallbackLyricsTranslationBackend(null, online).translate(listOf("x"), "en") as LyricsBatchTranslation.Success).state
        )
    }

    @Test
    fun `online readings upgrade an otherwise identical result`() {
        val repository = LyricsRepository()
        val plain = LyricsRepository.LyricsResult(true, listOf(com.luc4n3x.levyra.domain.LyricLine(0L, 1_000L, "また会う日まで")), "lrclib", 80, false)
        val read = plain.copy(lines = plain.lines.map { it.copy(romanized = "mata au hi made") })
        org.junit.Assert.assertTrue(repository.shouldUpgrade(plain, read))
        org.junit.Assert.assertFalse(repository.shouldUpgrade(read, plain))
    }

    private class FixedBackend(private val result: LyricsBatchTranslation) : LyricsTranslationBackend {
        override val version: String = "fixed"
        override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation = result
    }
}
