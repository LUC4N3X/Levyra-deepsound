package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRomanizationTest {
    @Test
    fun `keeps latin-only lyrics as a single original row`() {
        assertEquals("", LyricsRomanizer.romanize("Already Latin"))
    }

    @Test
    fun `romanizes cyrillic and greek without a dictionary`() {
        assertEquals("privet mir", LyricsRomanizer.romanize("Привет мир"))
        assertEquals("kalimera kosme", LyricsRomanizer.romanize("Καλημέρα κόσμε"))
    }

    @Test
    fun `preserves existing korean and kana romanization`() {
        assertEquals("annyeong", LyricsRomanizer.romanize("안녕"))
        assertEquals("konnichiha", LyricsRomanizer.romanize("こんにちは"))
    }

    @Test
    fun `romanizes devanagari gurmukhi and bengali syllables`() {
        assertEquals("namaste", LyricsRomanizer.romanize("नमस्ते"))
        assertEquals("sat shrii akaal", LyricsRomanizer.romanize("ਸਤ ਸ਼੍ਰੀ ਅਕਾਲ"))
        assertEquals("aami tomaake", LyricsRomanizer.romanize("আমি তোমাকে"))
    }

    @Test
    fun `romanizes arabic hebrew and georgian predictably`() {
        assertEquals("mrhba", LyricsRomanizer.romanize("مرحبا"))
        assertEquals("shlvm", LyricsRomanizer.romanize("שלום"))
        assertEquals("gamarjoba", LyricsRomanizer.romanize("გამარჯობა"))
    }

    @Test
    fun `preserves original punctuation emoji and latin fragments`() {
        val result = LyricsRomanizer.romanize("Hello — мир! 🎵")
        assertTrue(result.startsWith("hello — mir!"))
        assertTrue(result.endsWith("🎵"))
    }

    @Test
    fun `does not produce romanization for latin text with non-latin punctuation or casing differences`() {
        assertEquals("", LyricsRomanizer.romanize("Hello، WORLD"))
        assertEquals("", LyricsRomanizer.romanize("HELLO WORLD"))
        assertEquals("", LyricsRomanizer.romanize("Hello؛ WORLD"))
        assertEquals("", LyricsRomanizer.romanize("Hello · WORLD"))
        assertEquals("", LyricsRomanizer.romanize("Hello־WORLD"))
        assertEquals("", LyricsRomanizer.romanize("Hello? World!"))
    }

    @Test
    fun `handles combining marks niqqud and vocalization properly`() {
        assertEquals("shalvom", LyricsRomanizer.romanize("שָׁלוֹם"))
        assertEquals("marhabana", LyricsRomanizer.romanize("مَرْحَبًا"))
        assertEquals("kiaa", LyricsRomanizer.romanize("ਕਿਆ"))
        assertEquals("sant", LyricsRomanizer.romanize("ਸੰਤ"))
    }

    @Test
    fun `nfd and nfc inputs produce identical romanization results`() {
        val nfcGreek = "Καλημέρα κόσμε"
        val nfdGreek = java.text.Normalizer.normalize(nfcGreek, java.text.Normalizer.Form.NFD)
        org.junit.Assert.assertNotEquals(nfcGreek, nfdGreek)
        assertEquals("kalimera kosme", LyricsRomanizer.romanize(nfcGreek))
        assertEquals(LyricsRomanizer.romanize(nfcGreek), LyricsRomanizer.romanize(nfdGreek))

        val directNfc = ExtendedLyricsRomanization.transliterate(nfcGreek)
        val directNfd = ExtendedLyricsRomanization.transliterate(nfdGreek)
        assertEquals(directNfc.text, directNfd.text)
        assertEquals(directNfc.transformedCount, directNfd.transformedCount)
    }
    @Test
    fun `romanizes polytonic greek in nfc and nfd forms`() {
        val nfcGreek = "ἀ"
        val nfdGreek = java.text.Normalizer.normalize(nfcGreek, java.text.Normalizer.Form.NFD)

        org.junit.Assert.assertNotEquals(nfcGreek, nfdGreek)
        assertEquals("a", LyricsRomanizer.romanize(nfcGreek))
        assertEquals("a", LyricsRomanizer.romanize(nfdGreek))
    }

}
